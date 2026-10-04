package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TBarrSiteCard;

import java.util.List;

/**
 * 改造进度卡 Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITBarrSiteCardService {

    /** 按主键查询 */
    TBarrSiteCard selectTBarrSiteCardById(Long id);

    /** 按条件查询列表（分页由调用方统一处理） */
    List<TBarrSiteCard> selectTBarrSiteCardList(Wrapper<TBarrSiteCard> queryWrapper);

    /** 新增 */
    int insertTBarrSiteCard(TBarrSiteCard record);

    /** 修改 */
    int updateTBarrSiteCard(TBarrSiteCard record);

    /** 批量删除 */
    int deleteTBarrSiteCardByIds(String ids);

    /** 推进一态（待核对→已核对→已冻住） */
    int advanceTBarrSiteCard(Long id);

    /** 退回一态（已核对→待核对；旧记录沉底） */
    int revertTBarrSiteCard(Long id);

    /** 按主键删除 */
    int deleteTBarrSiteCardById(Long id);
}
