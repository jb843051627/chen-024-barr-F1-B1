package com.fc.v2.service.impl;

import java.math.BigDecimal;
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
import org.springframework.stereotype.Service;

/**
 * 改造进度卡Service业务层处理
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TBarrSiteCardServiceImpl extends ServiceImpl<TBarrSiteCardMapper, TBarrSiteCard> implements ITBarrSiteCardService {

    /** 进度情形：0待核对（新录） */
    private static final int STATUS_NEW = 0;
    /** 进度情形：2已冻住（收口），收口后不许再顶、再退、再改、再撤 */
    private static final int STATUS_FROZEN = 2;

    @Autowired
    private TBarrArchMapper barrArchMapper;

    /** 老卡步数（status）可能为空：一律按 0（待核对）取值，两边口径一致 */
    private static int statusOf(TBarrSiteCard r) {
        return r.getStatus() == null ? STATUS_NEW : r.getStatus().intValue();
    }

    /** 还差几处一律轧出来（应改-已改，负差按 0），不手填 */
    private static int lackOf(Integer needNum, Integer doneNum) {
        int need = needNum == null ? 0 : needNum.intValue();
        int done = doneNum == null ? 0 : doneNum.intValue();
        return Math.max(need - done, 0);
    }

    @Override
    public TBarrSiteCard selectTBarrSiteCardById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TBarrSiteCard>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    @Override
    public List<TBarrSiteCard> selectTBarrSiteCardList(Wrapper<TBarrSiteCard> queryWrapper) {
        // 按调用方条件查，分页由调用方统一处理；不再在此硬编码状态过滤与分页，
        // 屏上折出来的数才与册子上记的一致
        return this.baseMapper.selectList(queryWrapper);
    }

    @Override
    public int insertTBarrSiteCard(TBarrSiteCard record) {
        if (record == null) {
            return 0;
        }

        // 署名交给 MybatisPlus 自动填充（当前登录人），不得再写成卡代号
        TBarrArch refArch = barrArchMapper.selectOne(new QueryWrapper<TBarrArch>()
                .eq("id", record.getArchId()).eq("del_flag", 0));
        if (refArch == null) {
            return 0;
        }
        if (refArch.getStatus() != null && refArch.getStatus() == 1) {
            return 0;
        }
        record.setArchNo(refArch.getArchNo());
        if (StringUtils.isNotEmpty(record.getCardNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TBarrSiteCard>()
                    .eq("card_no", record.getCardNo()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }
        // 同一处点位（同底册同点位）已有未收口的卡在挂，不许再压一张上来叠着
        if (record.getArchId() != null && StringUtils.isNotEmpty(record.getSiteNo())) {
            Integer hangCnt = this.baseMapper.selectCount(new QueryWrapper<TBarrSiteCard>()
                    .eq("arch_id", record.getArchId())
                    .eq("site_no", record.getSiteNo())
                    .eq("del_flag", 0)
                    .and(w -> w.isNull("status").or().lt("status", STATUS_FROZEN)));
            if (hangCnt != null && hangCnt > 0) {
                return 0;
            }
        }
        // 应改/已改不许塞负数；还差几处轧出来，不手填
        if ((record.getNeedNum() != null && record.getNeedNum() < 0)
                || (record.getDoneNum() != null && record.getDoneNum() < 0)) {
            return 0;
        }
        record.setLackNum(lackOf(record.getNeedNum(), record.getDoneNum()));
        if (record.getStatus() == null) {
            record.setStatus(STATUS_NEW);
        }
        record.setDelFlag(0);
        return this.baseMapper.insert(record);
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
        // 已冻住（收口）的卡内容不得再就地改
        if (statusOf(cur) >= STATUS_FROZEN) {
            return 0;
        }

        if (StringUtils.isNotEmpty(record.getCardNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TBarrSiteCard>()
                    .eq("card_no", record.getCardNo()).ne("id", record.getId()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }

        // 底册代号冗余自底册：换了挂靠底册就从底册重取，两处不各写各的
        if (record.getArchId() != null) {
            TBarrArch refArch = barrArchMapper.selectOne(new QueryWrapper<TBarrArch>()
                    .eq("id", record.getArchId()).eq("del_flag", 0));
            if (refArch == null || (refArch.getStatus() != null && refArch.getStatus() == 1)) {
                return 0;
            }
            record.setArchNo(refArch.getArchNo());
        }

        // 应改/已改不许塞负数；还差几处按（新值优先，缺省取账面）重轧
        Integer need = record.getNeedNum() != null ? record.getNeedNum() : cur.getNeedNum();
        Integer done = record.getDoneNum() != null ? record.getDoneNum() : cur.getDoneNum();
        if ((need != null && need < 0) || (done != null && done < 0)) {
            return 0;
        }
        record.setLackNum(lackOf(need, done));

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
        // 老卡步数为空的按 0 起算，不再一沾就抛错
        int st = statusOf(cur);
        // 已冻住（收口）不许再往上顶；逐档推进，一次只挪一态
        if (st >= STATUS_FROZEN) {
            return 0;
        }
        int next = st + 1;
        UpdateWrapper<TBarrSiteCard> uw = new UpdateWrapper<TBarrSiteCard>()
                .eq("id", id).eq("del_flag", 0);
        uw.set("status", next);
        uw.set("update_time", new Date());
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
        int st = statusOf(cur);
        // 已冻住（收口）不许再退回来；待核对已到底，无可再退
        if (st >= STATUS_FROZEN || st <= STATUS_NEW) {
            return 0;
        }
        int next = st - 1;
        UpdateWrapper<TBarrSiteCard> uw = new UpdateWrapper<TBarrSiteCard>()
                .eq("id", id).eq("del_flag", 0);
        uw.set("status", next);
        // 退回只挪状态，先前核齐记下的要件保留（留痕），不再就地抹平
        uw.set("update_time", new Date());
        return this.baseMapper.update(null, uw);
    }

    @Override
    public int deleteTBarrSiteCardByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        int ok = 0;
        for (Long id : idArr) {
            ok += this.deleteTBarrSiteCardById(id);
        }
        return ok;
    }

    @Override
    public int deleteTBarrSiteCardById(Long id) {
        if (id == null) {
            return 0;
        }
        TBarrSiteCard cur = this.baseMapper.selectById(id);
        if (cur == null) {
            return 0;
        }
        // 已冻住（收口）的卡不许整张撤出
        if (statusOf(cur) >= STATUS_FROZEN) {
            return 0;
        }
        return this.baseMapper.deleteById(id);
    }
}
