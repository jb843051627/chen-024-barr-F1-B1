package com.fc.v2.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TBarrSiteCardMapper;
import com.fc.v2.mapper.auto.TBarrArchMapper;
import com.fc.v2.model.auto.TBarrSiteCard;
import com.fc.v2.model.auto.TBarrArch;
import com.fc.v2.service.ITBarrSiteCardService;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 改造进度卡Service业务层处理
 *
 * 口径（定死）：
 * 1. 进度情形按状态机走：0新录→1已改齐→2已冻住（收口，终态）；推进/退回一次一档，
 *    已冻住的卡不再顶、不再退、不再就地改、不再撤出；
 * 2. 还差几处（lack_num）一律由 应改-已改 轧出来，不手填；
 * 3. 早年纸质簿抄进来的老卡，空状态按新录起、空处数按 0 起，不抛错、不当成已冻住误拦；
 * 4. 署名（create_by）由填充器统一写当前登录名，与操作日志同口径；
 * 5. 同一处点位（site_no）已有未收口的卡，不再叠第二张；卡代号撞唯一约束按拒收处理。
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TBarrSiteCardServiceImpl extends ServiceImpl<TBarrSiteCardMapper, TBarrSiteCard> implements ITBarrSiteCardService {

    /** 进度情形终态：2已冻住（收口） */
    private static final int STATUS_FROZEN = 2;

    @Autowired
    private TBarrArchMapper barrArchMapper;

    @Override
    public TBarrSiteCard selectTBarrSiteCardById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TBarrSiteCard>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    @Override
    public List<TBarrSiteCard> selectTBarrSiteCardList(Wrapper<TBarrSiteCard> queryWrapper) {
        // 条件与分页统一由调用方处理；此处不再私设页码、不再私加状态过滤，
        // 屏上折出来的数才与册子上记的那份对得齐
        return this.baseMapper.selectList(queryWrapper);
    }

    @Override
    public int insertTBarrSiteCard(TBarrSiteCard record) {
        if (record == null) {
            return 0;
        }

        // 署名交给填充器写当前登录名，不得再拿卡代号充数
        TBarrArch refArch = record.getArchId() == null ? null : barrArchMapper.selectOne(new QueryWrapper<TBarrArch>()
                .eq("id", record.getArchId()).eq("del_flag", 0));
        if (refArch == null) {
            return 0;
        }
        if (refArch.getStatus() != null && refArch.getStatus() == 1) {
            return 0;
        }
        record.setArchNo(refArch.getArchNo());

        // 处数轧账：空按 0 起（老卡空值不抛错），负数不收；还差几处轧出来，不手填
        int need = record.getNeedNum() == null ? 0 : record.getNeedNum();
        int done = record.getDoneNum() == null ? 0 : record.getDoneNum();
        if (need < 0 || done < 0) {
            return 0;
        }
        record.setNeedNum(need);
        record.setDoneNum(done);
        record.setLackNum(need - done);

        if (StringUtils.isNotEmpty(record.getCardNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TBarrSiteCard>()
                    .eq("card_no", record.getCardNo()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }
        // 同一处点位已有未收口（未冻住）的卡，不再叠第二张——两家街道同日报同一处也只收头一张
        if (StringUtils.isNotEmpty(record.getSiteNo())) {
            Integer openCnt = this.baseMapper.selectCount(new QueryWrapper<TBarrSiteCard>()
                    .eq("site_no", record.getSiteNo())
                    .eq("del_flag", 0)
                    .and(w -> w.isNull("status").or().ne("status", STATUS_FROZEN)));
            if (openCnt != null && openCnt > 0) {
                return 0;
            }
        }
        record.setDelFlag(0);
        if (record.getStatus() == null) {
            record.setStatus(0);
        }
        try {
            return this.baseMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发下同代号撞库级唯一约束（uk_card_no），按拒收处理
            return 0;
        }
    }

    @Override
    public int updateTBarrSiteCard(TBarrSiteCard record) {
        if (record == null || record.getId() == null) {
            return 0;
        }
        TBarrSiteCard cur = this.baseMapper.selectOne(new QueryWrapper<TBarrSiteCard>()
                .eq("id", record.getId()).eq("del_flag", 0));
        if (cur == null) {
            return 0;
        }
        if (cur.getStatus() != null && cur.getStatus().intValue() == STATUS_FROZEN) {
            // 已冻住（收口）的卡不得就地改
            return 0;
        }

        if (StringUtils.isNotEmpty(record.getCardNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TBarrSiteCard>()
                    .eq("card_no", record.getCardNo()).ne("id", record.getId()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }

        // 进度情形只能走推进/退回，编辑口不直接改状态
        record.setStatus(null);
        // 处数轧账：现值与本次改动合并后重轧还差几处，负数不收
        int need = record.getNeedNum() != null ? record.getNeedNum()
                : (cur.getNeedNum() == null ? 0 : cur.getNeedNum());
        int done = record.getDoneNum() != null ? record.getDoneNum()
                : (cur.getDoneNum() == null ? 0 : cur.getDoneNum());
        if (need < 0 || done < 0) {
            return 0;
        }
        record.setNeedNum(need);
        record.setDoneNum(done);
        record.setLackNum(need - done);

        record.setUpdateTime(new Date());
        return this.baseMapper.update(record, new UpdateWrapper<TBarrSiteCard>()
                .eq("id", record.getId())
                .eq("del_flag", 0));
    }

    @Override
    public int advanceTBarrSiteCard(Long id) {
        if (id == null) {
            return 0;
        }
        TBarrSiteCard cur = this.baseMapper.selectById(id);
        if (cur == null || cur.getDelFlag() == null || cur.getDelFlag().intValue() == 1) {
            return 0;
        }
        // 空状态的老卡按新录起，不抛错
        int st = cur.getStatus() == null ? 0 : cur.getStatus().intValue();
        if (st >= STATUS_FROZEN) {
            // 已冻住（收口）不再往上顶
            return 0;
        }
        int next = st + 1;
        UpdateWrapper<TBarrSiteCard> uw = new UpdateWrapper<TBarrSiteCard>()
                .eq("id", id).eq("del_flag", 0);
        uw.set("status", next);
        uw.set("update_time", new Date());
        applyStatusCond(uw, cur.getStatus(), st);
        return this.baseMapper.update(null, uw);
    }

    @Override
    public int revertTBarrSiteCard(Long id) {
        if (id == null) {
            return 0;
        }
        TBarrSiteCard cur = this.baseMapper.selectById(id);
        if (cur == null || cur.getDelFlag() == null || cur.getDelFlag().intValue() == 1) {
            return 0;
        }
        int st = cur.getStatus() == null ? 0 : cur.getStatus().intValue();
        if (cur.getStatus() != null && st >= STATUS_FROZEN) {
            // 已冻住（收口）不得退回
            return 0;
        }
        if (st <= 0) {
            return 0;
        }
        int next = st - 1;
        UpdateWrapper<TBarrSiteCard> uw = new UpdateWrapper<TBarrSiteCard>()
                .eq("id", id).eq("del_flag", 0);
        uw.set("status", next);
        // content 不动：退回一步不抹平先前核齐那笔，留痕不断
        uw.set("update_time", new Date());
        applyStatusCond(uw, cur.getStatus(), st);
        return this.baseMapper.update(null, uw);
    }

    @Override
    public int deleteTBarrSiteCardByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        if (idArr == null || idArr.length == 0) {
            return 0;
        }
        // 已冻住（收口）的卡不得撤出：整批里有一张就整批拒删
        Integer frozenCnt = this.baseMapper.selectCount(new QueryWrapper<TBarrSiteCard>()
                .in("id", Arrays.asList(idArr))
                .eq("del_flag", 0)
                .eq("status", STATUS_FROZEN));
        if (frozenCnt != null && frozenCnt > 0) {
            return 0;
        }
        return this.baseMapper.deleteBatchIds(Arrays.asList(idArr));
    }

    @Override
    public int deleteTBarrSiteCardById(Long id) {
        if (id == null) {
            return 0;
        }
        TBarrSiteCard cur = this.baseMapper.selectById(id);
        if (cur == null || cur.getDelFlag() == null || cur.getDelFlag().intValue() == 1) {
            return 0;
        }
        if (cur.getStatus() != null && cur.getStatus().intValue() == STATUS_FROZEN) {
            return 0;
        }
        return this.baseMapper.deleteById(id);
    }

    /** 并发条件：库里的情形必须还是读到的那个（空状态按 is null 对），否则本次挪档落空 */
    private void applyStatusCond(UpdateWrapper<TBarrSiteCard> uw, Integer dbStatus, int status) {
        if (dbStatus == null) {
            uw.isNull("status");
        } else {
            uw.eq("status", status);
        }
    }
}
