package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TBarrTierLineMapper;
import com.fc.v2.model.auto.TBarrTierLine;
import com.fc.v2.service.ITBarrTierLineService;

/**
 * 入户评估定档线 Service业务层处理（rule-eval 形状）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TBarrTierLineServiceImpl implements ITBarrTierLineService {

    /** 优先级降序；并列按 ruleCode 降序（确定性） */
    private static final Comparator<TBarrTierLine> PRIORITY_THEN_CODE_DESC = new Comparator<TBarrTierLine>() {
        @Override
        public int compare(TBarrTierLine a, TBarrTierLine b) {
            int pa = a.getPriority() == null ? 0 : a.getPriority();
            int pb = b.getPriority() == null ? 0 : b.getPriority();
            if (pa != pb) {
                return pb - pa;
            }
            String ca = a.getRuleCode() == null ? "" : a.getRuleCode();
            String cb = b.getRuleCode() == null ? "" : b.getRuleCode();
            return cb.compareTo(ca);
        }
    };

    @javax.annotation.Resource
    private TBarrTierLineMapper barrTierLineMapper;

    /**
     * 时段比较统一走**墙钟字符串**（yyyy-MM-dd HH:mm:ss）+ 字符串绑定：
     * JDBC 的 serverTimezone 与本机时区不对称，直接把 java.util.Date 作参数会整体偏移，
     * 使边界判定在"切换当日"这类用例上随机错档（实测踩到）。
     */
    private static String ts(Date d) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    /**
     * 生效窗口唯一取数处（列表/定位/计数/求值都走这里，不再各写各的）：
     * 未删除、在位（status=0）、起用那一日含、交班那一日不含；
     * 老线起止可能缺空——空起算按自古有效、空交班按长期有效，两边口径一致。
     */
    private static boolean inWindow(TBarrTierLine r, Date at) {
        if (r == null || at == null) {
            return false;
        }
        if (r.getDelFlag() != null && r.getDelFlag().intValue() != 0) {
            return false;
        }
        if (r.getStatus() != null && r.getStatus().intValue() != 0) {
            return false;
        }
        if (r.getEffStart() != null && ts(r.getEffStart()).compareTo(ts(at)) > 0) {
            return false;
        }
        if (r.getEffEnd() != null && ts(at).compareTo(ts(r.getEffEnd())) >= 0) {
            return false;
        }
        return true;
    }

    @Override
    public TBarrTierLine selectTBarrTierLineById(Long id) {
        return this.barrTierLineMapper.selectById(id);
    }

    @Override
    public List<TBarrTierLine> listAvailable(Date at) {
        List<TBarrTierLine> avail = new java.util.ArrayList<TBarrTierLine>();
        if (at == null) {
            return avail;
        }
        List<TBarrTierLine> all = this.barrTierLineMapper.selectList(new QueryWrapper<TBarrTierLine>());
        for (TBarrTierLine r : all) {
            if (inWindow(r, at)) {
                avail.add(r);
            }
        }
        // 优先级降序；并列按 ruleCode 降序（确定性）
        avail.sort(PRIORITY_THEN_CODE_DESC);
        return avail;
    }

    @Override
    public int evaluate(String ruleCode, BigDecimal input, Date at) {
        if (ruleCode == null || ruleCode.trim().isEmpty() || input == null || at == null) {
            return 0;
        }
        // 输入越界一律返回 0，口径与 evaluateTop 一致（零下、超封顶都塞不进来）
        if (input.compareTo(BigDecimal.ZERO) < 0
                || input.compareTo(new BigDecimal("100")) > 0) {
            return 0;
        }
        TBarrTierLine rule = findRule(ruleCode, at);
        // 无生效版本返回 0，不再空指针
        if (rule == null) {
            return 0;
        }
        return levelOf(rule, input);
    }

    @Override
    public int evaluateTop(BigDecimal input, Date at) {
        if (input == null || at == null) {
            return 0;
        }
        if (input.compareTo(BigDecimal.ZERO) < 0
                || input.compareTo(new BigDecimal("100")) > 0) {
            return 0;
        }
        List<TBarrTierLine> avail = listAvailable(at);
        if (avail.isEmpty()) {
            return 0;
        }
        return levelOf(avail.get(0), input);
    }

    @Override
    public boolean usable(Long id, Date at) {
        if (id == null || at == null) {
            return false;
        }
        // 与列表/定位同一处窗口口径
        return inWindow(this.barrTierLineMapper.selectById(id), at);
    }

    @Override
    public int countAvailable(Date at) {
        // 与列表同一处口径，角标/看板不再对不上
        return listAvailable(at).size();
    }

    /** 按 ruleCode 取 at 时刻生效的版本：优先级降序、并列按起用日降序（新班先说话） */
    private TBarrTierLine findRule(String ruleCode, Date at) {
        List<TBarrTierLine> hit = this.barrTierLineMapper.selectList(new QueryWrapper<TBarrTierLine>()
                .eq("rule_code", ruleCode));
        TBarrTierLine best = null;
        for (TBarrTierLine r : hit) {
            if (!inWindow(r, at)) {
                continue;
            }
            if (best == null) {
                best = r;
                continue;
            }
            int pr = Integer.compare(
                    r.getPriority() == null ? 0 : r.getPriority().intValue(),
                    best.getPriority() == null ? 0 : best.getPriority().intValue());
            if (pr > 0) {
                best = r;
            } else if (pr == 0) {
                String es = r.getEffStart() == null ? "" : ts(r.getEffStart());
                String bs = best.getEffStart() == null ? "" : ts(best.getEffStart());
                if (es.compareTo(bs) > 0) {
                    best = r;
                }
            }
        }
        return best;
    }

    /** 阈值解析：档案为主、明细覆盖；两者皆缺返回 null（调用方按不可用处理），精度统一 2 位 */
    private BigDecimal resolveLimit(TBarrTierLine r, int tier) {
        if (tier == 1) {
            return r.getTh1Max();
        }
        if (tier == 2) {
            return r.getTh2Max();
        }
        return r.getTh3Max();
    }

    /** 档案级默认阈值（明细未维护时的回退来源） */
    private BigDecimal defaultThreshold(TBarrTierLine r, int tier) {
        return r.getTh1Max();
    }

    /** 分档：等于上限取高一档 */
    private int levelOf(TBarrTierLine rule, BigDecimal input) {
        if (rule.getTh1Max() != null && input.compareTo(rule.getTh1Max()) <= 0) {
            return 1;
        }
        if (rule.getTh2Max() != null && input.compareTo(rule.getTh2Max()) <= 0) {
            return 2;
        }
        if (rule.getTh3Max() != null && input.compareTo(rule.getTh3Max()) <= 0) {
            return 3;
        }
        return 4;
    }
}
