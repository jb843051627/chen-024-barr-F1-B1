package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TBarrSiteCard;
import com.fc.v2.service.ITBarrSiteCardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 改造进度卡 Controller
 *
 * @author fuce
 * @date 2026-09-12
 */
@Api(value = "改造进度卡")
@Controller
@RequestMapping("/BarrSiteCardController")
public class BarrSiteCardController extends BaseController {

    private final String prefix = "admin/barrSiteCard";

    @Autowired
    private ITBarrSiteCardService barrSiteCardService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("barr:barrSiteCard:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "改造进度卡集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("barr:barrSiteCard:list")
    @ResponseBody
    public ResultTable list(TBarrSiteCard record) {
        QueryWrapper<TBarrSiteCard> queryWrapper = new QueryWrapper<TBarrSiteCard>();
        startPage();
        com.github.pagehelper.PageInfo<TBarrSiteCard> page =
                new com.github.pagehelper.PageInfo<TBarrSiteCard>(barrSiteCardService.selectTBarrSiteCardList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "改造进度卡新增", action = "add")
    @ApiOperation(value = "新增", notes = "新增")
    @PostMapping("/add")
    @RequiresPermissions("barr:barrSiteCard:add")
    @ResponseBody
    public AjaxResult add(TBarrSiteCard record) {
        return toAjax(barrSiteCardService.insertTBarrSiteCard(record));
    }

    @Log(title = "改造进度卡修改", action = "edit")
    @ApiOperation(value = "修改保存", notes = "修改保存")
    @PostMapping("/edit")
    @RequiresPermissions("barr:barrSiteCard:edit")
    @ResponseBody
    public AjaxResult editSave(TBarrSiteCard record) {
        return toAjax(barrSiteCardService.updateTBarrSiteCard(record));
    }

    @Log(title = "改造进度卡删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @RequiresPermissions("barr:barrSiteCard:remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(barrSiteCardService.deleteTBarrSiteCardByIds(ids));
    }
}
