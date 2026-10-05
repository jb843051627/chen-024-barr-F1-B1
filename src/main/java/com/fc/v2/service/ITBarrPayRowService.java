package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TBarrPayRow;

/**
 * 补助核销行 Service接口（batch-process 形状：整批提交，无单条增删改）
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITBarrPayRowService {

    /** 按主键回查单行 */
    TBarrPayRow selectTBarrPayRowById(Long id);

    /**
     * 整批提交：逐行校验，合法的入库、非法的记入失败明细（保留原始行号），返回**成功行数**。
     * 册码以入参 batchNo 为准（同一批只认这一个册码）；
     * 逐行校验卡代号空、请领额零/负、验收合格处数（核定次数）零/负，整行判负；
     * 应收/已销对不上时**以改造进度卡（台账）为准**：item_code 必须对得上在册进度卡，
     * 且验收合格处数不得超出台账轧出的还差处数，超出整行判负；
     * 同一批次已有收下行的重复提交不得重复入库（整批拒收返回 0，纯失败明细允许重报）；
     * 行数超过单批上限返回 -1 且整批不入库；空批次返回 0。
     */
    int submitBatch(String batchNo, List<TBarrPayRow> rows);

    /** 该批次的失败行明细（按行号升序） */
    List<TBarrPayRow> listErrors(String batchNo);
}
