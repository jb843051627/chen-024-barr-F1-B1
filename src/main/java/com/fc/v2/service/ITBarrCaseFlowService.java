package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TBarrCaseFlow;

/**
 * 一户一档改造卷宗 Service接口（state-machine 形状：单据流转，不提供增删改查全套）
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITBarrCaseFlowService {

    /** 按主键查询单据 */
    TBarrCaseFlow selectTBarrCaseFlowById(Long id);

    /** 列表查询（流转台账） */
    List<TBarrCaseFlow> selectTBarrCaseFlowList(QueryWrapper<TBarrCaseFlow> queryWrapper);

    /** 推进一档：一次只挪一格，挪到结清即封住（status=2）；已封住即拒；被拒返回 null */
    TBarrCaseFlow advance(Long id, String remark);

    /** 回退一档：只退一格，不动 content（留痕）；已封住即拒；被拒返回 null */
    TBarrCaseFlow rollback(Long id, String remark);

    /** 修改内容：已办结不得再改，被拒返回 false */
    boolean updateContent(Long id, String remark);

    /** 删除单据：已办结不得删除，被拒返回 false */
    boolean remove(Long id);
}
