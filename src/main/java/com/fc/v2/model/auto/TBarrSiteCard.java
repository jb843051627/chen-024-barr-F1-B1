package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 改造进度卡对象 t_barr_site_card
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_barr_site_card")
@ApiModel(value = "TBarrSiteCard", description = "改造进度卡")
public class TBarrSiteCard implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 改造进度卡代号 */
    @TableField("card_no")
    @ApiModelProperty(value = "改造进度卡代号")
    private String cardNo;

    /** 挂在哪一处底册名下 */
    @TableField("arch_id")
    @ApiModelProperty(value = "挂在哪一处底册名下")
    private Integer archId;

    /** 底册代号（冗余自底册） */
    @TableField("arch_no")
    @ApiModelProperty(value = "底册代号（冗余自底册）")
    private String archNo;

    /** 点位或住户代号 */
    @TableField("site_no")
    @ApiModelProperty(value = "点位或住户代号")
    private String siteNo;

    /** 应改处数 */
    @TableField("need_num")
    @ApiModelProperty(value = "应改处数")
    private Integer needNum;

    /** 已改处数 */
    @TableField("done_num")
    @ApiModelProperty(value = "已改处数")
    private Integer doneNum;

    /** 还差几处（轧出来，不手填） */
    @TableField("lack_num")
    @ApiModelProperty(value = "还差几处（轧出来，不手填）")
    private Integer lackNum;

    /** 随卡交来的改造要件 */
    @TableField("content")
    @ApiModelProperty(value = "随卡交来的改造要件")
    private String content;

    /** 进度情形 0新录 1已改齐 2缺项 */
    @TableField("status")
    @ApiModelProperty(value = "进度情形 0新录 1已改齐 2缺项")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public Integer getArchId() {
        return archId;
    }

    public void setArchId(Integer archId) {
        this.archId = archId;
    }

    public String getArchNo() {
        return archNo;
    }

    public void setArchNo(String archNo) {
        this.archNo = archNo;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public Integer getNeedNum() {
        return needNum;
    }

    public void setNeedNum(Integer needNum) {
        this.needNum = needNum;
    }

    public Integer getDoneNum() {
        return doneNum;
    }

    public void setDoneNum(Integer doneNum) {
        this.doneNum = doneNum;
    }

    public Integer getLackNum() {
        return lackNum;
    }

    public void setLackNum(Integer lackNum) {
        this.lackNum = lackNum;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
