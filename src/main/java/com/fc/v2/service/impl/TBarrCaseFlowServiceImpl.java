package com.fc.v2.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.mapper.auto.TBarrCaseFlowMapper;
import com.fc.v2.model.auto.TBarrCaseFlow;
import com.fc.v2.service.ITBarrCaseFlowService;

/**
 * 一户一档改造卷宗 Service业务层处理（state-machine 形状：单据流转）
 *
 * 流转口径（定死）：
 * 1. 一次只挪一格（0核卷→1评估→2验收→3结清），不得跨档；
 * 2. 挪到最后一格（结清）即封住（status=2），已封住的卷宗一律拒动：不能再顶、不能退、不能改、不能撤；
 * 3. 回退只退一格，且不动 content——先前各格的记录保留（留痕），重走与首走可按格次分辨；
 * 4. 早年纸质簿抄进来的老卷宗，空步数按 0 格起步、空情形按在办看，不抛错、不当成已封住误拦；
 * 5. 挪格带当前格次做条件更新，并发下后到的请求落空，按被拒处理。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TBarrCaseFlowServiceImpl implements ITBarrCaseFlowService {

    private static final int MAX_STAGE = 3;
    private static final int STATUS_NOT_START = 0;
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
        TBarrCaseFlow r = id == null ? null : this.barrCaseFlowMapper.selectById(id);
        if (r == null || isDeleted(r) || isTerminal(r)) {
            return null;
        }
        int st = r.getStage() == null ? 0 : r.getStage();
        if (st >= MAX_STAGE) {
            return null;
        }
        int next = st + 1;
        int nextStatus = next >= MAX_STAGE ? STATUS_TERMINAL : STATUS_ACTIVE;
        UpdateWrapper<TBarrCaseFlow> uw = new UpdateWrapper<TBarrCaseFlow>()
                .eq("id", id)
                .eq("del_flag", 0)
                .set("stage", next)
                .set("status", nextStatus)
                .set("last_action", remark)
                .set("update_time", new Date());
        applyStageCond(uw, r.getStage(), st);
        if (this.barrCaseFlowMapper.update(null, uw) == 0) {
            return null;
        }
        r.setStage(next);
        r.setStatus(nextStatus);
        r.setLastAction(remark);
        return r;
    }

    @Override
    public TBarrCaseFlow rollback(Long id, String remark) {
        TBarrCaseFlow r = id == null ? null : this.barrCaseFlowMapper.selectById(id);
        if (r == null || isDeleted(r) || isTerminal(r)) {
            return null;
        }
        int st = r.getStage() == null ? 0 : r.getStage();
        if (st <= 0) {
            return null;
        }
        int next = st - 1;
        int nextStatus = next == 0 ? STATUS_NOT_START : STATUS_ACTIVE;
        UpdateWrapper<TBarrCaseFlow> uw = new UpdateWrapper<TBarrCaseFlow>()
                .eq("id", id)
                .eq("del_flag", 0)
                .set("stage", next)
                .set("status", nextStatus)
                .set("last_action", remark)
                .set("update_time", new Date());
        applyStageCond(uw, r.getStage(), st);
        if (this.barrCaseFlowMapper.update(null, uw) == 0) {
            return null;
        }
        // content 不动：退回一格不抹掉先前核齐那笔，留痕不断
        r.setStage(next);
        r.setStatus(nextStatus);
        r.setLastAction(remark);
        return r;
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        TBarrCaseFlow r = id == null ? null : this.barrCaseFlowMapper.selectById(id);
        if (r == null || isDeleted(r) || isTerminal(r)) {
            return false;
        }
        r.setContent(remark);
        return this.barrCaseFlowMapper.updateById(r) > 0;
    }

    @Override
    public boolean remove(Long id) {
        TBarrCaseFlow r = id == null ? null : this.barrCaseFlowMapper.selectById(id);
        if (r == null || isDeleted(r) || isTerminal(r)) {
            return false;
        }
        return this.barrCaseFlowMapper.deleteById(id) > 0;
    }

    /** 已封住（收口）即终态；空情形的老卷宗不按终态拦 */
    private boolean isTerminal(TBarrCaseFlow r) {
        return r.getStatus() != null && r.getStatus().intValue() == STATUS_TERMINAL;
    }

    private boolean isDeleted(TBarrCaseFlow r) {
        return r.getDelFlag() != null && r.getDelFlag().intValue() == 1;
    }

    /** 并发条件：库里的格次必须还是读到的那个（空步数按 is null 对），否则本次挪格落空 */
    private void applyStageCond(UpdateWrapper<TBarrCaseFlow> uw, Integer dbStage, int stage) {
        if (dbStage == null) {
            uw.isNull("stage");
        } else {
            uw.eq("stage", stage);
        }
    }

}
