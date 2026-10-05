package com.fc.v2.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TBarrVisitItemMapper;
import com.fc.v2.model.auto.TBarrVisitItem;
import com.fc.v2.service.ITBarrVisitItemService;

/**
 * 质保与养护复检条目 Service业务层处理（scheduling-job 形状：周期执行）
 *
 * 口径（定死）：
 * 1. 只办"搁着"（status=0）的到期条目；递出即落已递出（status=1），
 *    出库一笔销一笔，下一轮不再重复计数；
 * 2. 最多许提前几日（amount）为空或为负的条目递不动（status=2），不抛错；
 * 3. 空情形的老条目按搁着看，不当成已递出误拦。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TBarrVisitItemServiceImpl implements ITBarrVisitItemService {

    private static final int STATUS_WAIT = 0;
    private static final int STATUS_DONE = 1;
    private static final int STATUS_FAIL = 2;

    @javax.annotation.Resource
    private TBarrVisitItemMapper barrVisitItemMapper;

    @Override
    public TBarrVisitItem selectTBarrVisitItemById(Long id) {
        return this.barrVisitItemMapper.selectById(id);
    }

    /**
     * 时段比较统一走**墙钟字符串**（yyyy-MM-dd HH:mm:ss）：JDBC 的 serverTimezone 与本机
     * 时区不对称，直接把 java.util.Date 作参数会整体偏移，使"恰好到期"这类边界用例错判。
     */
    private static String ts(Date d) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    @Override
    public List<TBarrVisitItem> listDue(Date at) {
        List<TBarrVisitItem> due = new java.util.ArrayList<TBarrVisitItem>();
        if (at == null) {
            return due;
        }
        List<TBarrVisitItem> all = this.barrVisitItemMapper.selectList(new QueryWrapper<TBarrVisitItem>()
                .eq("del_flag", 0));
        for (TBarrVisitItem r : all) {
            if (r.getDueAt() != null && ts(r.getDueAt()).compareTo(ts(at)) < 0) {
                due.add(r);
            }
        }
        return due;
    }

    @Override
    public int runOnce(Date at) {
        List<TBarrVisitItem> due = listDue(at);
        if (due.isEmpty()) {
            return 0;
        }
        int ok = 0;
        for (TBarrVisitItem r : due) {
            if (r.getStatus() != null && r.getStatus().intValue() != STATUS_WAIT) {
                continue;
            }
            if (r.getAmount() != null && r.getAmount().signum() >= 0) {
                r.setStatus(STATUS_DONE);
                ok++;
            } else {
                r.setStatus(STATUS_FAIL);
            }
            this.barrVisitItemMapper.updateById(r);
        }
        return ok;
    }
}
