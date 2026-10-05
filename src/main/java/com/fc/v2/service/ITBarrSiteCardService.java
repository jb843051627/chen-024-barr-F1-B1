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

    /** 按条件查询列表（分页由调用方统一处理；服务内不再另设状态过滤与分页，屏上与册子同一份数） */
    List<TBarrSiteCard> selectTBarrSiteCardList(Wrapper<TBarrSiteCard> queryWrapper);

    /**
     * 新增。口径（定死）：
     * 1. 署名由自动填充写当前登录人，不得写成卡代号；
     * 2. 同一处点位（同底册同点位代号）已有未冻住的卡在挂，不许再压一张叠着；
     * 3. 应改/已改不许为负；还差几处一律按（应改-已改）轧出，不手填；
     * 4. 老卡步数（status）缺空按 0（待核对）起算。
     */
    int insertTBarrSiteCard(TBarrSiteCard record);

    /** 修改：已冻住（收口）不得再就地改；换了挂靠底册时底册代号从底册重取，两处同一处取值 */
    int updateTBarrSiteCard(TBarrSiteCard record);

    /** 批量删除：已冻住（收口）的跳过不删，返回实际删除条数 */
    int deleteTBarrSiteCardByIds(String ids);

    /** 推进一态（待核对→已核对→已冻住），一次只挪一态；已冻住不许再往上顶 */
    int advanceTBarrSiteCard(Long id);

    /** 退回一态（已核对→待核对；旧记录沉底留痕，要件不抹平）；已冻住不许退 */
    int revertTBarrSiteCard(Long id);

    /** 按主键删除：已冻住（收口）不许整张撤出 */
    int deleteTBarrSiteCardById(Long id);
}
