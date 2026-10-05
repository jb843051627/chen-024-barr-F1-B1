package com.fc.v2.service;

import com.fc.v2.model.auto.TBarrSignBill;

/**
 * 联户验收签认单 Service接口（approval-chain 形状：多阶段签批，无增删改查入口）
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITBarrSignBillService {

    /** 按主键回查单据 */
    TBarrSignBill selectTBarrSignBillById(Long id);

    /** 签批一票：按 sign_mode 计票（0一名即签、1两名须同判签够 need_count），
     *  本口算齐才挪口，末口算齐即已点齐；只有在签的单可签，被拒返回 null */
    TBarrSignBill approve(Long id, String approver, String comment);

    /** 否决：落已压回；只有在签的单可否，被拒返回 null */
    TBarrSignBill reject(Long id, String approver, String comment);

    /** 退回上一环节：只退一口、本口票数清零重计；已点齐/已压回即拒，被拒返回 null */
    TBarrSignBill rollback(Long id, String comment);
}
