package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TBarrPayRowMapper;
import com.fc.v2.model.auto.TBarrPayRow;
import com.fc.v2.service.ITBarrPayRowService;

/**
 * 补助核销行 Service业务层处理（batch-process 形状：整批提交）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TBarrPayRowServiceImpl implements ITBarrPayRowService {

    private static final int MAX_ROWS = 500;
    private static final int STATUS_OK = 1;
    private static final int STATUS_FAIL = 2;

    @javax.annotation.Resource
    private TBarrPayRowMapper barrPayRowMapper;

    @Override
    public TBarrPayRow selectTBarrPayRowById(Long id) {
        return this.barrPayRowMapper.selectById(id);
    }

    @Override
    public int submitBatch(String batchNo, List<TBarrPayRow> rows) {
        // 空批次返回 0，不再一沾就抛错
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        // 行数超过单批上限返回 -1 且整批不入库
        if (rows.size() > MAX_ROWS) {
            return -1;
        }
        // 册码以入参为准；入参缺省才落回首行册码
        String no = com.fc.v2.util.StringUtils.isNotEmpty(batchNo) ? batchNo : rows.get(0).getBatchNo();
        if (!com.fc.v2.util.StringUtils.isNotEmpty(no)) {
            return 0;
        }
        // 同一批次重复提交不得重复入库：册码下已有在账行即视为已报过，
        // 结存只减不增，不再重复挂行把数抬上去
        Integer reported = this.barrPayRowMapper.selectCount(new QueryWrapper<TBarrPayRow>()
                .eq("batch_no", no).eq("del_flag", 0));
        if (reported != null && reported > 0) {
            return 0;
        }
        int ok = 0;
        int seq = 0;
        for (TBarrPayRow r : rows) {
            seq++;
            // 逐行校验：卡代号非空、请领额>0、验收合格处数>0（零数、负数塞不进来）
            boolean valid = r.getItemCode() != null && !r.getItemCode().trim().isEmpty()
                    && r.getQty() != null && r.getQty().compareTo(java.math.BigDecimal.ZERO) > 0
                    && r.getQualiNum() != null && r.getQualiNum().compareTo(java.math.BigDecimal.ZERO) > 0;
            r.setBatchNo(no);
            r.setDelFlag(0);
            if (valid) {
                r.setStatus(STATUS_OK);
                ok++;
            } else {
                // 非法行记入失败明细：保留原始行次；老数据行次缺空的按提交顺序补，便于对号
                r.setStatus(STATUS_FAIL);
                if (r.getRowNo() == null) {
                    r.setRowNo(Integer.valueOf(seq));
                }
            }
            this.barrPayRowMapper.insert(r);
        }
        return ok;
    }

    @Override
    public List<TBarrPayRow> listErrors(String batchNo) {
        return this.barrPayRowMapper.selectList(new QueryWrapper<TBarrPayRow>()
                .eq("batch_no", batchNo).eq("status", STATUS_FAIL).eq("del_flag", 0)
                .orderByAsc("row_no"));
    }
}
