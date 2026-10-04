-- barr 无障碍环境建设与适老化改造补助监督 -- schema (chen-024)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_024

CREATE TABLE IF NOT EXISTS t_barr_arch (
  id bigint NOT NULL COMMENT '主键',
  arch_no varchar(64) DEFAULT NULL COMMENT '改造点位底册代号',
  arch_name varchar(128) DEFAULT NULL COMMENT '底册全称',
  arch_kind varchar(32) DEFAULT NULL COMMENT '底册类别',
  road_name varchar(128) DEFAULT NULL COMMENT '属地那一路(省—市—区—街道)',
  status int DEFAULT NULL COMMENT '底册情形 0在册 1已停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='改造点位底册';

CREATE TABLE IF NOT EXISTS t_barr_case_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '一户一档改造卷宗号',
  stage int DEFAULT NULL COMMENT '当前格次 0..3（核卷/评估/验收/结清）',
  status int DEFAULT NULL COMMENT '卷宗走到哪一格 0未起 1在办 2已封住',
  content varchar(255) DEFAULT NULL COMMENT '一格一记',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次挪格动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='一户一档改造卷宗';

CREATE TABLE IF NOT EXISTS t_barr_pay_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '月度补助核销册码',
  row_no int DEFAULT NULL COMMENT '原报行次',
  item_code varchar(64) DEFAULT NULL COMMENT '被对上的改造进度卡代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行请领补助额',
  quali_num decimal(12,2) DEFAULT NULL COMMENT '本行验收合格处数',
  pay_kind varchar(32) DEFAULT NULL COMMENT '拨付口径（区级／市级两条来路）',
  status int DEFAULT NULL COMMENT '行落地情形 0未勾 1已收下 2已送回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='补助核销行';

CREATE TABLE IF NOT EXISTS t_barr_sign_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '联户验收签认单',
  node_no int DEFAULT NULL COMMENT '当前所在口 0..2（住户/到场两方/两级残联）',
  sign_mode int DEFAULT NULL COMMENT '本口算齐办法 0一名即签 1两名须同判',
  need_count int DEFAULT NULL COMMENT '本口应签人数',
  sign_count int DEFAULT NULL COMMENT '本口已签人数',
  status int DEFAULT NULL COMMENT '签认情形 0在签 1已点齐 2已压回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='联户验收签认单';

CREATE TABLE IF NOT EXISTS t_barr_site_card (
  id bigint NOT NULL COMMENT '主键',
  card_no varchar(64) DEFAULT NULL COMMENT '改造进度卡代号',
  arch_id int DEFAULT NULL COMMENT '挂在哪一处底册名下',
  arch_no varchar(64) DEFAULT NULL COMMENT '底册代号（冗余自底册）',
  site_no varchar(64) DEFAULT NULL COMMENT '点位或住户代号',
  need_num int DEFAULT NULL COMMENT '应改处数',
  done_num int DEFAULT NULL COMMENT '已改处数',
  lack_num int DEFAULT NULL COMMENT '还差几处（轧出来，不手填）',
  content varchar(255) DEFAULT NULL COMMENT '随卡交来的改造要件',
  status int DEFAULT NULL COMMENT '进度情形 0新录 1已改齐 2缺项',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='改造进度卡';

CREATE TABLE IF NOT EXISTS t_barr_tier_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '入户评估定档线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '线名',
  scene_kind varchar(32) DEFAULT NULL COMMENT '这条线管公共点位还是居家适老',
  th1_max decimal(12,3) DEFAULT NULL COMMENT '起算分',
  th2_max decimal(12,3) DEFAULT NULL COMMENT '够线分',
  th3_max decimal(12,3) DEFAULT NULL COMMENT '封顶分',
  eff_start datetime DEFAULT NULL COMMENT '起用那一日',
  eff_end datetime DEFAULT NULL COMMENT '交班那一日(不含)',
  priority int DEFAULT NULL COMMENT '交班次序(数值大的先说话)',
  status int DEFAULT NULL COMMENT '线的情形 0在位 1已卸下',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='入户评估定档线';

CREATE TABLE IF NOT EXISTS t_barr_visit_item (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '质保与养护复检条目代号',
  due_at datetime DEFAULT NULL COMMENT '该上门那一日的止点时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '最多许提前几日',
  content varchar(255) DEFAULT NULL COMMENT '事由与所对住户的记要',
  status int DEFAULT NULL COMMENT '条目情形 0搁着 1已递出 2递不动',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='质保与养护复检条目';

-- 初始底册档案数据：在册一条（DC00·小区楼组类）、已停用一条（DC01·公共点位类）
INSERT IGNORE INTO t_barr_arch (id, arch_no, arch_name, arch_kind, road_name, status, del_flag, create_by, create_time)
VALUES (0, 'DC00', '浈县区白沙街道·祥和楼组片区（小区楼组类，在册）', '小区楼组', '岫岭省—柏州市—浈县区—白沙街道', 0, 0, 'seed', NOW()),
       (1, 'DC01', '梅坂区藕塘街道·沿河南岸公共通道（已并走停用）', '公共点位', '岫岭省—柏州市—梅坂区—藕塘街道', 1, 0, 'seed', NOW());

