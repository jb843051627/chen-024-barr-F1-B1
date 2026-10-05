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

    /** 按条件查询列表（分页与条件由调用方统一处理，实现侧不私设页码、不私加状态过滤） */
    List<TBarrSiteCard> selectTBarrSiteCardList(Wrapper<TBarrSiteCard> queryWrapper);

    /** 新增（还差几处由 应改-已改 轧出；同点位有未收口的卡即拒；卡代号撞唯一约束即拒） */
    int insertTBarrSiteCard(TBarrSiteCard record);

    /** 修改（已冻住即拒；进度情形不在此改，只能走推进/退回；还差几处重轧） */
    int updateTBarrSiteCard(TBarrSiteCard record);

    /** 批量删除（已冻住的卡不得撤出，整批里有一张即整批拒删） */
    int deleteTBarrSiteCardByIds(String ids);

    /** 推进一态（待核对→已核对→已冻住；一次一档，已冻住即拒；空状态老卡按待核对起） */
    int advanceTBarrSiteCard(Long id);

    /** 退回一态（已核对→待核对；一次一档，已冻住即拒；不清内容，旧记录沉底留痕） */
    int revertTBarrSiteCard(Long id);

    /** 按主键删除（已冻住即拒） */
    int deleteTBarrSiteCardById(Long id);
}
