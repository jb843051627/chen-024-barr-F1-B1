package com.fc.v2.service.impl;

import org.springframework.stereotype.Service;

import com.fc.v2.mapper.auto.TBarrSignBillMapper;
import com.fc.v2.model.auto.TBarrSignBill;
import com.fc.v2.service.ITBarrSignBillService;

/**
 * 联户验收签认单 Service业务层处理（approval-chain 形状：多阶段签批）
 *
 * 签批口径（定死）：
 * 1. 签批一票只累一票（sign_count+1）；本口算齐办法看 sign_mode：
 *    0一名即签（一票即齐）、1两名须同判（须签够 need_count，空按 2 计）；
 * 2. 本口算齐才挪口（node_no+1），挪口后票数清零重计；末口（两级残联）算齐即已点齐；
 * 3. 只有在签（status=0）的单可签、可否、可退；已点齐、已压回的一律拒动；
 * 4. 否决落已压回（status=2）；退回只退一口，本口票数清零，
 *    重走一趟与头一遭同口径，新旧两串接得上；
 * 5. 早年纸质簿抄进来的老单，空口次按首口起、空情形按在签看，不抛错、不当成已点齐误拦。
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

    @Override
    public TBarrSignBill selectTBarrSignBillById(Long id) {
        return this.barrSignBillMapper.selectById(id);
    }

    @Override
    public TBarrSignBill approve(Long id, String approver, String comment) {
        TBarrSignBill r = id == null ? null : this.barrSignBillMapper.selectById(id);
        if (r == null || approver == null || approver.trim().isEmpty() || !isRunning(r)) {
            return null;
        }
        int node = r.getNodeNo() == null ? 0 : r.getNodeNo();
        if (node > MAX_NODE) {
            return null;
        }
        int signed = (r.getSignCount() == null ? 0 : r.getSignCount()) + 1;
        int mode = r.getSignMode() == null ? MODE_OR : r.getSignMode();
        int need = mode == MODE_AND ? (r.getNeedCount() == null ? 2 : r.getNeedCount()) : 1;
        if (signed < need) {
            // 本口未算齐：只累票，不挪口
            r.setSignCount(signed);
            this.barrSignBillMapper.updateById(r);
            return r;
        }
        // 本口算齐
        if (node >= MAX_NODE) {
            r.setStatus(STATUS_PASS);
            r.setSignCount(signed);
        } else {
            r.setNodeNo(node + 1);
            r.setSignCount(0);
        }
        this.barrSignBillMapper.updateById(r);
        return r;
    }

    @Override
    public TBarrSignBill reject(Long id, String approver, String comment) {
        TBarrSignBill r = id == null ? null : this.barrSignBillMapper.selectById(id);
        if (r == null || !isRunning(r)) {
            return null;
        }
        r.setStatus(STATUS_VETO);
        this.barrSignBillMapper.updateById(r);
        return r;
    }

    @Override
    public TBarrSignBill rollback(Long id, String comment) {
        TBarrSignBill r = id == null ? null : this.barrSignBillMapper.selectById(id);
        if (r == null || !isRunning(r)) {
            return null;
        }
        int node = r.getNodeNo() == null ? 0 : r.getNodeNo();
        if (node <= 0) {
            return null;
        }
        r.setNodeNo(node - 1);
        r.setSignCount(0);
        this.barrSignBillMapper.updateById(r);
        return r;
    }

    /** 只有在签的单可动；空情形的老单按在签看，不当成已点齐误拦 */
    private boolean isRunning(TBarrSignBill r) {
        if (r.getDelFlag() != null && r.getDelFlag().intValue() == 1) {
            return false;
        }
        return r.getStatus() == null || r.getStatus().intValue() == STATUS_RUNNING;
    }
}
