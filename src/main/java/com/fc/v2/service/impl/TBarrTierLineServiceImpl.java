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
 * 口径（定死）：
 * 1. 参与定档的线必须"在位"（status=0）且当下落在效期内（起用日<=当下<交班日），
 *    已卸下的、过了交班日的老线一律不再说话；空边界按不限算（老线空日期不拦）；
 * 2. 多条同代号线并存时按交班次序取：priority 数值大的先说话，并列按 ruleCode 降序；
 * 3. 分档边界：等于上限取高一档（严格小于才落本档），空阈值跳过该档；
 * 4. 效期内找不到线，按不可用处理（返回 0），不抛错。
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

    @Override
    public TBarrTierLine selectTBarrTierLineById(Long id) {
        return this.barrTierLineMapper.selectById(id);
    }

    @Override
    public List<TBarrTierLine> listAvailable(Date at) {
        if (at == null) {
            return new java.util.ArrayList<TBarrTierLine>();
        }
        String now = ts(at);
        List<TBarrTierLine> list = this.barrTierLineMapper.selectList(new QueryWrapper<TBarrTierLine>()
                .eq("del_flag", 0)
                .and(w -> w.isNull("status").or().eq("status", 0))
                .and(w -> w.isNull("eff_start").or().le("eff_start", now))
                .and(w -> w.isNull("eff_end").or().gt("eff_end", now)));
        list.sort(PRIORITY_THEN_CODE_DESC);
        return list;
    }

    @Override
    public int evaluate(String ruleCode, BigDecimal input, Date at) {
        if (ruleCode == null || ruleCode.trim().isEmpty() || input == null || at == null) {
            return 0;
        }
        TBarrTierLine rule = null;
        for (TBarrTierLine r : listAvailable(at)) {
            if (ruleCode.equals(r.getRuleCode())) {
                rule = r;
                break;
            }
        }
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
        TBarrTierLine r = this.barrTierLineMapper.selectById(id);
        if (r == null || (r.getDelFlag() != null && r.getDelFlag().intValue() == 1)) {
            return false;
        }
        if (r.getStatus() != null && r.getStatus().intValue() != 0) {
            return false;
        }
        String now = ts(at);
        if (r.getEffStart() != null && ts(r.getEffStart()).compareTo(now) > 0) {
            return false;
        }
        if (r.getEffEnd() != null && ts(r.getEffEnd()).compareTo(now) <= 0) {
            return false;
        }
        return true;
    }

    @Override
    public int countAvailable(Date at) {
        if (at == null) {
            return 0;
        }
        return listAvailable(at).size();
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

    /** 分档：等于上限取高一档（严格小于才落本档）；空阈值跳过该档 */
    private int levelOf(TBarrTierLine rule, BigDecimal input) {
        if (rule == null || input == null) {
            return 0;
        }
        if (rule.getTh1Max() != null && input.compareTo(rule.getTh1Max()) < 0) {
            return 1;
        }
        if (rule.getTh2Max() != null && input.compareTo(rule.getTh2Max()) < 0) {
            return 2;
        }
        if (rule.getTh3Max() != null && input.compareTo(rule.getTh3Max()) < 0) {
            return 3;
        }
        return 4;
    }
}
