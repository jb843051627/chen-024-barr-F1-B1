package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TBarrPayRowMapper;
import com.fc.v2.mapper.auto.TBarrSiteCardMapper;
import com.fc.v2.model.auto.TBarrPayRow;
import com.fc.v2.model.auto.TBarrSiteCard;
import com.fc.v2.service.ITBarrPayRowService;

/**
 * 补助核销行 Service业务层处理（batch-process 形状：整批提交）
 *
 * 口径（定死）：
 * 1. 册码以入参 batchNo 为准，整批只认这一个册码（合成一处）；
 * 2. 逐行校验：卡代号空、请领额为零或负、验收合格处数（核定次数）为零或负，整行判负；
 * 3. 应收/已销对不上时，以改造进度卡（台账）为准：item_code 必须对得上一张在册进度卡，
 *    且本行验收合格处数不得超出台账上轧出来的还差处数，超出整行判负；
 * 4. 合法的入库（已收下）、非法的记入失败明细（已送回，保留原始行号），返回成功行数；
 * 5. 同一批次已有收下行的重复提交整批拒收（返回 0），纯失败明细允许重报；
 *    行数超过单批上限返回 -1 且整批不入库；空批次返回 0；
 * 6. 早年纸质簿抄进来的老行，空行次按本次序号补上，不抛错。
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

    @javax.annotation.Resource
    private TBarrSiteCardMapper barrSiteCardMapper;

    @Override
    public TBarrPayRow selectTBarrPayRowById(Long id) {
        return this.barrPayRowMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitBatch(String batchNo, List<TBarrPayRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        if (batchNo == null || batchNo.trim().isEmpty()) {
            return 0;
        }
        String no = batchNo.trim();
        if (rows.size() > MAX_ROWS) {
            return -1;
        }
        // 同一批次重复提交不得重复入库：已有收下（status=1）行即整批拒收
        Integer okCnt = this.barrPayRowMapper.selectCount(new QueryWrapper<TBarrPayRow>()
                .eq("batch_no", no).eq("del_flag", 0).eq("status", STATUS_OK));
        if (okCnt != null && okCnt > 0) {
            return 0;
        }
        int ok = 0;
        int seq = 0;
        for (TBarrPayRow r : rows) {
            seq++;
            if (r == null) {
                continue;
            }
            r.setId(null);
            r.setBatchNo(no);
            if (r.getRowNo() == null) {
                // 保留原始行号；老行空行次补本次序号
                r.setRowNo(seq);
            }
            r.setDelFlag(0);
            String err = validateRow(r);
            if (err == null) {
                r.setStatus(STATUS_OK);
                ok++;
            } else {
                r.setStatus(STATUS_FAIL);
                r.setRemark(err);
            }
            this.barrPayRowMapper.insert(r);
        }
        return ok;
    }

    @Override
    public List<TBarrPayRow> listErrors(String batchNo) {
        return this.barrPayRowMapper.selectList(new QueryWrapper<TBarrPayRow>()
                .eq("batch_no", batchNo).eq("del_flag", 0).eq("status", STATUS_FAIL)
                .orderByAsc("row_no"));
    }

    /** 逐行校验：返回 null 为合法，否则为判负原因（记入备注） */
    private String validateRow(TBarrPayRow r) {
        if (r.getItemCode() == null || r.getItemCode().trim().isEmpty()) {
            return "改造进度卡代号空";
        }
        if (r.getQty() == null || r.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            return "请领额为零或负";
        }
        if (r.getQualiNum() == null || r.getQualiNum().compareTo(BigDecimal.ZERO) <= 0) {
            return "验收合格处数（核定次数）为零或负";
        }
        // 应收/已销对不上时以改造进度卡（台账）为准
        TBarrSiteCard card = this.barrSiteCardMapper.selectOne(new QueryWrapper<TBarrSiteCard>()
                .eq("card_no", r.getItemCode().trim()).eq("del_flag", 0));
        if (card == null) {
            return "对不上改造进度卡（台账）";
        }
        int need = card.getNeedNum() == null ? 0 : card.getNeedNum();
        int done = card.getDoneNum() == null ? 0 : card.getDoneNum();
        if (r.getQualiNum().compareTo(BigDecimal.valueOf(need - done)) > 0) {
            return "已销超出台账还差处数";
        }
        return null;
    }
}
