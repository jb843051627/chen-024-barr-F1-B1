package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TBarrCaseFlowMapper;
import com.fc.v2.model.auto.TBarrCaseFlow;
import com.fc.v2.service.ITBarrCaseFlowService;

/**
 * 一户一档改造卷宗 Service业务层处理（state-machine 形状：单据流转）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TBarrCaseFlowServiceImpl implements ITBarrCaseFlowService {

    private static final int MAX_STAGE = 3;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_TERMINAL = 2;

    @javax.annotation.Resource
    private TBarrCaseFlowMapper barrCaseFlowMapper;

    @Override
    public TBarrCaseFlow selectTBarrCaseFlowById(Long id) {
        return this.barrCaseFlowMapper.selectById(id);
    }

    @Override
    public List<TBarrCaseFlow> selectTBarrCaseFlowList(QueryWrapper<TBarrCaseFlow> queryWrapper) {
        return this.barrCaseFlowMapper.selectList(queryWrapper);
    }

    @Override
    public TBarrCaseFlow advance(Long id, String remark) {
        TBarrCaseFlow r = this.barrCaseFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        // 已封住（收口）的卷宗不许再往上顶
        if (r.getStatus() != null && r.getStatus() == STATUS_TERMINAL) {
            return null;
        }
        int st = r.getStage() == null ? 0 : r.getStage();
        // 逐档推进，一次只挪一格
        int next = Math.min(st + 1, MAX_STAGE);
        r.setStage(next);
        // 挪到结清格即封住收口，挂卡户数随之松坑
        r.setStatus(next >= MAX_STAGE ? STATUS_TERMINAL : STATUS_ACTIVE);
        r.setLastAction(remark);
        this.barrCaseFlowMapper.updateById(r);
        return r;
    }

    @Override
    public TBarrCaseFlow rollback(Long id, String remark) {
        TBarrCaseFlow r = this.barrCaseFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        // 已封住（收口）的卷宗不许再退回来
        if (r.getStatus() != null && r.getStatus() == STATUS_TERMINAL) {
            return null;
        }
        int st = r.getStage() == null ? 0 : r.getStage();
        // 只回退一档，先前已核齐的格次与记录保留（留痕），不再一抹到 0
        r.setStage(Math.max(st - 1, 0));
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.barrCaseFlowMapper.updateById(r);
        return r;
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        TBarrCaseFlow r = this.barrCaseFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        // 已封住（收口）的卷宗内容不得再就地改
        if (r.getStatus() != null && r.getStatus() == STATUS_TERMINAL) {
            return false;
        }
        r.setContent(remark);
        return this.barrCaseFlowMapper.updateById(r) > 0;
    }

    @Override
    public boolean remove(Long id) {
        TBarrCaseFlow r = this.barrCaseFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        // 已封住（收口）的卷宗不许整张撤出
        if (r.getStatus() != null && r.getStatus() == STATUS_TERMINAL) {
            return false;
        }
        return this.barrCaseFlowMapper.deleteById(id) > 0;
    }

}
