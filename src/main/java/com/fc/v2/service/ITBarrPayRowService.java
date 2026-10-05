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
     * 同一批次重复提交不得重复入库；行数超过单批上限返回 -1 且整批不入库；空批次返回 0。
     *
     * 口径（定死，不得再各写各的）：
     * 1. 册码以入参 batchNo 为准，入参缺省才落回首行册码；
     * 2. 行合法口径：卡代号非空、请领额&gt;0、验收合格处数&gt;0（零数、负数一律进失败明细）；
     * 3. 同一册码只入库一次，重复提交返回 0 且不再挂行（结存只减不增）；
     * 4. 应收户数以改造点位台账（改造进度卡）为准，已销户数以本表已收下的核销行为准；
     *    两边对不上时以本表实际收下行为准核销，台账不反向改写，差额留待对账。
     */
    int submitBatch(String batchNo, List<TBarrPayRow> rows);

    /** 该批次的失败行明细（按行号升序） */
    List<TBarrPayRow> listErrors(String batchNo);
}
