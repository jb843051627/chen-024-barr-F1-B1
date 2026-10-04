package com.fc.v2.service.impl;

import java.math.BigDecimal;
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
import org.springframework.stereotype.Service;

/**
 * 改造进度卡Service业务层处理
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TBarrSiteCardServiceImpl extends ServiceImpl<TBarrSiteCardMapper, TBarrSiteCard> implements ITBarrSiteCardService {

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
        QueryWrapper<TBarrSiteCard> wrapper = new QueryWrapper<TBarrSiteCard>();
        com.github.pagehelper.PageHelper.startPage(1, 10);
        wrapper.eq("status", 0);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public int insertTBarrSiteCard(TBarrSiteCard record) {
        if (record == null) {
            return 0;
        }

        record.setCreateBy(record.getCardNo());
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
        record.setDelFlag(0);
        return this.baseMapper.insert(record);
    }

    @Override
    public int updateTBarrSiteCard(TBarrSiteCard record) {
        if (record == null || record.getId() == null) {
            return 0;
        }

        if (record.getId() != null && StringUtils.isNotEmpty(record.getCardNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TBarrSiteCard>()
                    .eq("card_no", record.getCardNo()).ne("id", record.getId()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }

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
        Integer st = cur.getStatus();
        int next = st.intValue() + 1;
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
        Integer st = cur.getStatus();
        if (st == null) {
            return 0;
        }
        int next = st.intValue() - 1;
        UpdateWrapper<TBarrSiteCard> uw = new UpdateWrapper<TBarrSiteCard>()
                .eq("id", id).eq("del_flag", 0);
        uw.set("status", next);
        uw.set("content", null);
        uw.set("update_time", new Date());
        return this.baseMapper.update(null, uw);
    }

    @Override
    public int deleteTBarrSiteCardByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        return this.baseMapper.deleteBatchIds(Arrays.asList(idArr));
    }

    @Override
    public int deleteTBarrSiteCardById(Long id) {
        return this.baseMapper.deleteById(id);
    }
}
