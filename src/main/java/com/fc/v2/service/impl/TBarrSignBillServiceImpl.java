package com.fc.v2.service.impl;

import org.springframework.stereotype.Service;

import com.fc.v2.mapper.auto.TBarrSignBillMapper;
import com.fc.v2.model.auto.TBarrSignBill;
import com.fc.v2.service.ITBarrSignBillService;

/**
 * 联户验收签认单 Service业务层处理（approval-chain 形状：多阶段签批）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TBarrSignBillServiceImpl implements ITBarrSignBillService {

    private static final int MAX_NODE = 2;
    private static final int MODE_OR = 0;
    private static final int MODE_AND = 1;
    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_PASS = 1;
    private static final int STATUS_VETO = 2;

    @javax.annotation.Resource
    private TBarrSignBillMapper barrSignBillMapper;

    /** 老单字段可能为空：签认情形按 0（在签）、所在口按 0、已签按 0 起算，不再一沾就抛错、不误拦 */
    private static int statusOf(TBarrSignBill r) {
        return r.getStatus() == null ? STATUS_RUNNING : r.getStatus().intValue();
    }

    /** 本口应签人数：以 need_count 为准；缺空按算齐办法推（0一名即签→1，1两名须同判→2），再缺按 1 */
    private static int needOf(TBarrSignBill r) {
        if (r.getNeedCount() != null && r.getNeedCount() > 0) {
            return r.getNeedCount().intValue();
        }
        if (r.getSignMode() != null && r.getSignMode() == MODE_AND) {
            return 2;
        }
        return 1;
    }

    @Override
    public TBarrSignBill selectTBarrSignBillById(Long id) {
        return this.barrSignBillMapper.selectById(id);
    }

    @Override
    public TBarrSignBill approve(Long id, String approver, String comment) {
        TBarrSignBill r = this.barrSignBillMapper.selectById(id);
        if (r == null || approver == null || approver.trim().isEmpty()) {
            return null;
        }
        // 仅在签可签：已点齐（核齐）不得再随意改，已压回不得再签
        if (statusOf(r) != STATUS_RUNNING) {
            return null;
        }
        int need = needOf(r);
        int signed = (r.getSignCount() == null ? 0 : r.getSignCount().intValue()) + 1;
        int node = r.getNodeNo() == null ? 0 : r.getNodeNo().intValue();
        if (signed >= need) {
            // 本口算齐才挪口，已签清零重计；末口点齐即收口
            node = node + 1;
            signed = 0;
            if (node > MAX_NODE) {
                r.setStatus(Integer.valueOf(STATUS_PASS));
            }
        }
        r.setNodeNo(Integer.valueOf(node));
        r.setSignCount(Integer.valueOf(signed));
        this.barrSignBillMapper.updateById(r);
        return r;
    }

    @Override
    public TBarrSignBill reject(Long id, String approver, String comment) {
        TBarrSignBill r = this.barrSignBillMapper.selectById(id);
        if (r == null) {
            return null;
        }
        // 仅在签可压回；压回要真正落 2（已压回），不再空转
        if (statusOf(r) != STATUS_RUNNING) {
            return null;
        }
        r.setStatus(Integer.valueOf(STATUS_VETO));
        this.barrSignBillMapper.updateById(r);
        return r;
    }

    @Override
    public TBarrSignBill rollback(Long id, String comment) {
        TBarrSignBill r = this.barrSignBillMapper.selectById(id);
        if (r == null) {
            return null;
        }
        // 已点齐（核齐）不得再退；其余退回上一环节，本口已签清零、状态回到在签，
        // 重走一遍按同一口径重算，串不再越走越走样
        if (statusOf(r) == STATUS_PASS) {
            return null;
        }
        int node = r.getNodeNo() == null ? 0 : r.getNodeNo().intValue();
        r.setNodeNo(Integer.valueOf(Math.max(0, node - 1)));
        r.setSignCount(Integer.valueOf(0));
        r.setStatus(Integer.valueOf(STATUS_RUNNING));
        this.barrSignBillMapper.updateById(r);
        return r;
    }
}
