/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80409 (8.4.9)
 Source Host           : localhost:3306
 Source Schema         : shenyu-member

 Target Server Type    : MySQL
 Target Server Version : 80409 (8.4.9)
 File Encoding         : 65001

 Date: 04/07/2026 22:57:45
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for aoc_user
-- ----------------------------
DROP TABLE IF EXISTS `aoc_user`;
CREATE TABLE `aoc_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `friend_wechat_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信好友id',
  `friend_avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '好友头像',
  `wechat_friend_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信好友在工作手机系统id',
  `friend_nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '好友昵称',
  `friend_alias` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信好友微信号',
  `kf_id` bigint NULL DEFAULT NULL COMMENT '客服id',
  `wechat_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客服微信id',
  `alias` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客服微信号',
  `account_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '当前坐席（客服）Id',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '当前坐席（客服）用户名',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `bind_user_id` bigint NULL DEFAULT NULL COMMENT '绑定会员id',
  `tenant_id` bigint NULL DEFAULT 1 COMMENT '租户',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_user_id`(`bind_user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '奥创用户' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for aoc_user_bind_record
-- ----------------------------
DROP TABLE IF EXISTS `aoc_user_bind_record`;
CREATE TABLE `aoc_user_bind_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` tinyint NULL DEFAULT 0 COMMENT '1.绑定  0.解绑',
  `aoc_user` bigint NULL DEFAULT NULL COMMENT '奥创用户',
  `member` bigint NULL DEFAULT NULL COMMENT '会员id',
  `operate_type` tinyint NULL DEFAULT 0 COMMENT '0.私域  1.管理后台',
  `operate` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NULL DEFAULT 1 COMMENT '租户',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '奥创用户绑定解绑记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for article_push_config
-- ----------------------------
DROP TABLE IF EXISTS `article_push_config`;
CREATE TABLE `article_push_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `article_id` bigint NOT NULL COMMENT '文章id',
  `author_id` bigint NOT NULL COMMENT '作者id',
  `consume_status` int NOT NULL DEFAULT 0 COMMENT '消费状态 0 全部 -1 不推送 1 有条件推送',
  `consume_min_num` int NULL DEFAULT NULL COMMENT '最小消费次数',
  `consume_max_num` int NULL DEFAULT NULL COMMENT '最大消费次数',
  `consume_min_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '最小消费金额',
  `consume_max_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '最大消费金额',
  `push_time` datetime NOT NULL COMMENT '推送时间',
  `template_id` bigint NULL DEFAULT NULL COMMENT '模板id',
  `idx` int NULL DEFAULT 1 COMMENT '顺序标识',
  `push_type` tinyint(1) NOT NULL DEFAULT 1 COMMENT '推送类型：1-按条件推送 2-按分组推送 3-指定用户推送',
  `group_ids` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '选择的分组ID列表，字符串，多个以,分割',
  `user_ids` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '选择的用户ID列表，字符串，多个以,分割',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '推送标题',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  `status` tinyint NULL DEFAULT 0 COMMENT '推送状态 0.待推送 1.推送中 2.推送完成 3.推送取消 4.推送中取消',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `article_push_config_article_id_IDX`(`article_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21662 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章推送设置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_accomplishment
-- ----------------------------
DROP TABLE IF EXISTS `author_accomplishment`;
CREATE TABLE `author_accomplishment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `author_id` bigint NOT NULL COMMENT '作者id',
  `show_data` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '展示数据',
  `show_pic` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分享图片',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 89 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者战绩' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_apply
-- ----------------------------
DROP TABLE IF EXISTS `author_apply`;
CREATE TABLE `author_apply`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL DEFAULT 0 COMMENT '申请用户的ID',
  `apply_type` tinyint NULL DEFAULT 1 COMMENT '申请类型: 1-自媒体(个人), 2-企业/网媒, 3-MCN机构',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家头像URL',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家昵称',
  `intro` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家介绍',
  `real_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `id_card_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号',
  `id_card_front` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证人像面URL',
  `id_card_back` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证国徽面URL',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '审核状态: 0-审核中, 1-审核通过, 2-审核驳回',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '审批工作流实例id',
  `approver_id` bigint NULL DEFAULT NULL COMMENT '操作审核的管理员ID',
  `approver_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `approver_remark` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '审核备注/驳回原因 (后台填写, App端展示给用户)',
  `approved_at` datetime NULL DEFAULT NULL COMMENT '审核处理时间',
  `apply_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交申请时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `id_card_no_index`(`id_card_no` ASC) USING BTREE,
  INDEX `idx_userId`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '专家/作者入驻申请记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_article
-- ----------------------------
DROP TABLE IF EXISTS `author_article`;
CREATE TABLE `author_article`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `author_id` bigint NOT NULL COMMENT '作者ID',
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标题',
  `intro` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '简介',
  `free_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '免费内容',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '新闻内容',
  `schedule_publish_status` int NULL DEFAULT 1 COMMENT '定时发布状态：0关闭，1开启',
  `schedule_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '定时发布内容',
  `schedule_publish_time` datetime NULL DEFAULT NULL COMMENT '定时发布时间',
  `schedule_content_time` datetime NULL DEFAULT NULL COMMENT '定时发布时间',
  `privilege_types` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '2' COMMENT '0:同步至订阅套餐 1：同步至专栏套餐 2：不同步至套餐',
  `price` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '价格',
  `refund_type` int NOT NULL DEFAULT 0 COMMENT '0：不设置 1:不中补券 2：不中即退',
  `compensate_coupon_id` bigint NULL DEFAULT 0 COMMENT '补偿券ID',
  `win_result` int NOT NULL DEFAULT 0 COMMENT '红黑：0：未知  1：红  2：黑 3:走水 4:2中1 5:3中2 6:4中3 7:被绝杀',
  `win_result_info` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '红黑描述',
  `conclusion` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '结语',
  `win_exc` int NOT NULL DEFAULT 0 COMMENT '0:待处理 1:收款 2：退款',
  `share_pic_url` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分享图片地址',
  `top` tinyint NULL DEFAULT 0 COMMENT '是否置顶',
  `status` int NOT NULL DEFAULT -1 COMMENT ' -1:草稿， 0:已下架，1：已上架 ，2:审核中 3:预约发布 4审核不通过',
  `match_type` int NULL DEFAULT 0 COMMENT '赛事类型： 1:竞足 2:传足 3:北单 4:足球 5：临场',
  `match_scheme` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '比赛方案',
  `match_count` int UNSIGNED NULL DEFAULT 0 COMMENT '关联比赛id',
  `match_start_time` datetime NULL DEFAULT NULL COMMENT '比赛最早开始时间',
  `scheme_play` tinyint NULL DEFAULT 0 COMMENT '传足方案玩法：1=14场，2=任9',
  `issue` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '体彩期数',
  `recommend_win` int NULL DEFAULT NULL COMMENT '推荐结果',
  `recommend_win_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '推荐结果描述',
  `max_sale_count` int NULL DEFAULT -1 COMMENT '最大销售数量',
  `order_deadline_type` tinyint NULL DEFAULT 0 COMMENT '0.比赛结束立即截止 1.结束后半小时 2.结束后1小时 3.结束后2小时 4.自定义',
  `order_deadline` datetime NULL DEFAULT NULL COMMENT '截止购买时间',
  `publish_type` tinyint NULL DEFAULT 0 COMMENT ' 0.立即上架 1.结束后半小时 2.结束后1小时 3.结束后2小时 4.自定义',
  `publish_time` datetime NULL DEFAULT NULL COMMENT '定时上架时间',
  `visible_type` tinyint NULL DEFAULT 1 COMMENT '可见类型 1.全部可见 2.按消费条件可见 3.按标签可见 4.指定用户可见',
  `visible_min_num` int NULL DEFAULT 0 COMMENT '可见的最小消费次数',
  `visible_min_amt` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '可见的最小消费金额',
  `visible_tag_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `visible_user_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `view_count` int NULL DEFAULT 0 COMMENT '浏览量',
  `sort_num` int NULL DEFAULT 0,
  `rtp` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '方案的回报率',
  `creator` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `updater` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标志',
  `tenant_id` bigint NULL DEFAULT 1 COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `author_Id_index`(`author_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1000292 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_article_append
-- ----------------------------
DROP TABLE IF EXISTS `author_article_append`;
CREATE TABLE `author_article_append`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `fee_type` int NULL DEFAULT 0 COMMENT '费用类型：0：免费 1：付费',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '新闻内容',
  `status` tinyint NULL DEFAULT 0,
  `publish_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `creator` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `updater` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标志',
  `tenant_id` bigint NULL DEFAULT 1 COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_articleId`(`article_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1669 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章追加表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_article_match
-- ----------------------------
DROP TABLE IF EXISTS `author_article_match`;
CREATE TABLE `author_article_match`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `author_id` bigint NULL DEFAULT NULL,
  `article_id` bigint NOT NULL,
  `match_id` bigint NOT NULL,
  `country_id` bigint NULL DEFAULT 0,
  `country_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `class_id` bigint NULL DEFAULT NULL,
  `class_name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `class_type` bigint UNSIGNED NULL DEFAULT 0,
  `odds` decimal(10, 2) NULL DEFAULT 0.00,
  `creator` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `updater` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标志',
  `tenant_id` bigint NULL DEFAULT 1 COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_class_id`(`class_id` ASC) USING BTREE,
  INDEX `idx_article_id`(`article_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 735 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_article_pv_logs
-- ----------------------------
DROP TABLE IF EXISTS `author_article_pv_logs`;
CREATE TABLE `author_article_pv_logs`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `author_id` bigint NULL DEFAULT 0 COMMENT '作者ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户访问IP',
  `user_agent` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户浏览器版本信息',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `user_purchase_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '用户在当前作者下的消费金额',
  `user_purchase_num` int NULL DEFAULT 0 COMMENT '用户在当前作者下的消费次数',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识',
  `tenant_id` bigint NULL DEFAULT 1 COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_articleId`(`article_id` ASC) USING BTREE,
  INDEX `idx_author_id`(`author_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3900542 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章PV记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_article_tag
-- ----------------------------
DROP TABLE IF EXISTS `author_article_tag`;
CREATE TABLE `author_article_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `article_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  `creator` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `updater` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标志',
  `tenant_id` bigint NULL DEFAULT 1 COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_articleId`(`article_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 37 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_config
-- ----------------------------
DROP TABLE IF EXISTS `author_config`;
CREATE TABLE `author_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conf_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置项键名（如: auto_issue_coupon, max_draft_count）',
  `conf_value` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置项值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_conf_key`(`tenant_id` ASC, `conf_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_cost
-- ----------------------------
DROP TABLE IF EXISTS `author_cost`;
CREATE TABLE `author_cost`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `author_id` bigint NOT NULL COMMENT '作者id',
  `date` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '投入时间',
  `in_cost` decimal(10, 2) NULL DEFAULT NULL COMMENT '投入',
  `type` tinyint NULL DEFAULT NULL COMMENT '类型',
  `tenant_id` bigint NULL DEFAULT 1 COMMENT '租户ID',
  `status` tinyint NULL DEFAULT 0 COMMENT '0. 正常 1.删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者投入表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_member_tag_relation
-- ----------------------------
DROP TABLE IF EXISTS `author_member_tag_relation`;
CREATE TABLE `author_member_tag_relation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联表主键ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID（关联author_member_tag表的id字段）',
  `user_id` bigint NOT NULL COMMENT '用户ID（关联用户表的主键）',
  `author_id` bigint NOT NULL COMMENT '所属作者ID（冗余存储，便于快速筛选）',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户编号（冗余存储，与主表保持一致）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除(冗余字段，是物理删除)',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tag_id`(`tag_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_author_id`(`author_id` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 259571 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者用户标签分组-用户关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_performance
-- ----------------------------
DROP TABLE IF EXISTS `author_performance`;
CREATE TABLE `author_performance`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `author_id` bigint NOT NULL COMMENT '作者ID',
  `performance_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '战绩标签',
  `performance_value` decimal(18, 2) NOT NULL DEFAULT 0.00 COMMENT '业绩值（如3连红、命中率等）',
  `performance_type` tinyint NOT NULL DEFAULT 1 COMMENT '战绩类型：1-连红, 2-命中率, 3-近n中m',
  `optimal_num` int NULL DEFAULT 0 COMMENT '最优命中率的方案数',
  `optimal_hits` int NULL DEFAULT 0 COMMENT '最优命中率的命中数',
  `cycle_type` tinyint NOT NULL COMMENT '周期类型：1-时间周期, 2-数量周期',
  `cycle_value` int NOT NULL DEFAULT 0 COMMENT '数量周期值',
  `start_time` datetime NULL DEFAULT NULL COMMENT '时间周期开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '时间周期结束时间',
  `country_id` json NULL COMMENT '国家ID集合',
  `class_Id` json NULL COMMENT '比赛类型ID集合',
  `class_type` tinyint NULL DEFAULT 0 COMMENT '赛事范围：0-全部, 1-联赛',
  `match_type` json NULL COMMENT '方案赛事类型集合',
  `article_tag_id` json NULL COMMENT '方案标签ID集合',
  `generate_type` tinyint NOT NULL DEFAULT 1 COMMENT '生成方式：1-系统, 2-人工',
  `rule_id` bigint NOT NULL DEFAULT 0 COMMENT '关联规则ID（author_performance_rule.id）',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-有效 1-无效',
  `biz_date` date NULL DEFAULT NULL COMMENT '数据跑批日期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '创建人ID',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_author_id`(`author_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4430 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者业绩记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_performance_rule
-- ----------------------------
DROP TABLE IF EXISTS `author_performance_rule`;
CREATE TABLE `author_performance_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则名称',
  `performance_type` tinyint NOT NULL COMMENT '站绩类型：1-连红, 2-命中率, 3-近n中m',
  `class_type` tinyint NOT NULL DEFAULT 0 COMMENT '匹配范围：0-全部, 1-联赛',
  `cycle_type` tinyint NOT NULL DEFAULT 1 COMMENT '周期类型：1-时间周期, 2-方案数量',
  `cycle_value` int NOT NULL DEFAULT 0 COMMENT '周期值',
  `remark` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：1-禁用，0-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人ID',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_unique`(`performance_type` ASC, `class_type` ASC, `cycle_type` ASC, `cycle_value` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者业绩规则表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_private_data_report
-- ----------------------------
DROP TABLE IF EXISTS `author_private_data_report`;
CREATE TABLE `author_private_data_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `date` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `author_id` bigint NULL DEFAULT NULL COMMENT '作者id',
  `fans_count` int NULL DEFAULT NULL COMMENT '粉丝总数',
  `add_fans` int NULL DEFAULT NULL COMMENT '新增关注',
  `cancel_fans` int NULL DEFAULT NULL COMMENT '取消关注',
  `account_fans` int NULL DEFAULT NULL COMMENT '公众号粉丝总数',
  `account_add_fans` int NULL DEFAULT NULL COMMENT '公众号新增关注',
  `account_cancel_fans` int NULL DEFAULT NULL COMMENT '公众号取消关注',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7290 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者私域数据报表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_privilege_item
-- ----------------------------
DROP TABLE IF EXISTS `author_privilege_item`;
CREATE TABLE `author_privilege_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `privilege_id` bigint NOT NULL COMMENT '特权套餐ID',
  `article_id` bigint NOT NULL COMMENT '方案ID',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_privilege_id`(`privilege_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1001132 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '作者特权套餐关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_privilege_pv_logs
-- ----------------------------
DROP TABLE IF EXISTS `author_privilege_pv_logs`;
CREATE TABLE `author_privilege_pv_logs`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `author_id` bigint NULL DEFAULT 0 COMMENT '作者ID',
  `privilege_id` bigint NOT NULL COMMENT '文章ID',
  `ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户访问IP',
  `user_agent` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户浏览器版本信息',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识',
  `tenant_id` bigint NULL DEFAULT 1 COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_privilege_id`(`privilege_id` ASC) USING BTREE,
  INDEX `idx_author_id`(`author_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 28941 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '特权PV记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_privilege_set
-- ----------------------------
DROP TABLE IF EXISTS `author_privilege_set`;
CREATE TABLE `author_privilege_set`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `author_id` bigint NOT NULL COMMENT '作者ID',
  `type` tinyint NOT NULL DEFAULT 0 COMMENT '套餐类型：0：订阅套餐 1：专栏套餐',
  `days` int NOT NULL DEFAULT 0 COMMENT '天数',
  `numbers` int NULL DEFAULT NULL COMMENT '次数',
  `price` decimal(20, 2) NULL DEFAULT NULL COMMENT '价格',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父套餐id',
  `is_discount` tinyint NULL DEFAULT 0 COMMENT '是否是折扣套餐 0. 否  1.是',
  `is_buy` tinyint NULL DEFAULT 0 COMMENT '是否可以购买 0.是 1.否',
  `buy_num` int NULL DEFAULT NULL COMMENT 'null 为无限次',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '套餐名称',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '套餐内容',
  `date` int NULL DEFAULT NULL COMMENT '每几天',
  `num` int NULL DEFAULT NULL COMMENT '发布几篇文章',
  `top` int NULL DEFAULT NULL COMMENT '是否置顶',
  `top_time` datetime NULL DEFAULT NULL COMMENT '置顶时间',
  `consume_status` int NOT NULL DEFAULT 0 COMMENT '是否有消费条件 0 无 -1 有但是不公开 1 有条件',
  `consume_min_num` int NULL DEFAULT NULL COMMENT '最小消费次数',
  `consume_min_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '最低消费金额',
  `article_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '文章价格',
  `show_pic_url` varchar(600) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '展示图片地址',
  `fast_buy` tinyint NULL DEFAULT 0 COMMENT '快捷购买 0.否  1.是',
  `effective_rule` tinyint NULL DEFAULT 0 COMMENT '包时生效规则 0.24小时制 1.按自然日',
  `status` tinyint NULL DEFAULT 0 COMMENT '0.上架  1.下架  2.删除',
  `review_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '审核状态：0-待审核 1-已审核',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标签',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `author_id_index`(`author_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1000018 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者特权设置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_pv_log
-- ----------------------------
DROP TABLE IF EXISTS `author_pv_log`;
CREATE TABLE `author_pv_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `author_id` bigint NOT NULL COMMENT '作者ID',
  `ip` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户访问IP',
  `user_agent` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户浏览器版本信息',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识',
  `tenant_id` bigint NULL DEFAULT 1 COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `author_id_index`(`author_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2097 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者PV记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_sale_month_report
-- ----------------------------
DROP TABLE IF EXISTS `author_sale_month_report`;
CREATE TABLE `author_sale_month_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `author_id` bigint NULL DEFAULT NULL COMMENT '作者id',
  `published_plan_count` int NULL DEFAULT NULL COMMENT '发布方案数',
  `new_user_pay` int NULL DEFAULT NULL COMMENT '新粉丝付费人数',
  `old_user_pay` int NULL DEFAULT NULL COMMENT '老粉丝付费人数',
  `pay_user_count` int NULL DEFAULT NULL COMMENT '付费人数',
  `article_income` decimal(10, 2) NULL DEFAULT NULL COMMENT '方案收入',
  `privilege_income` decimal(10, 2) NULL DEFAULT NULL COMMENT '套餐收入',
  `article_refund` decimal(10, 2) NULL DEFAULT NULL COMMENT '方案退款',
  `privilege_refund` decimal(10, 2) NULL DEFAULT NULL COMMENT '套餐退款',
  `income_count` decimal(10, 2) NULL DEFAULT NULL COMMENT '总收入',
  `old_user_pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '老粉丝付费金额',
  `new_user_pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '新粉丝付费金额',
  `add_new_user` int NULL DEFAULT NULL COMMENT '新增粉丝数',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户',
  `user_count` int NULL DEFAULT NULL COMMENT '粉丝总数',
  `date` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5107 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者销售月报表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_sale_report
-- ----------------------------
DROP TABLE IF EXISTS `author_sale_report`;
CREATE TABLE `author_sale_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `author_id` bigint NULL DEFAULT NULL COMMENT '作者id',
  `published_plan_count` int NULL DEFAULT NULL COMMENT '发布方案数',
  `new_user_pay` int NULL DEFAULT NULL COMMENT '新粉丝付费人数',
  `old_user_pay` int NULL DEFAULT NULL COMMENT '老粉丝付费人数',
  `pay_user_count` int NULL DEFAULT NULL COMMENT '付费人数',
  `article_income` decimal(10, 2) NULL DEFAULT NULL COMMENT '方案收入',
  `privilege_income` decimal(10, 2) NULL DEFAULT NULL COMMENT '套餐收入',
  `article_refund` decimal(10, 2) NULL DEFAULT NULL COMMENT '方案退款',
  `privilege_refund` decimal(10, 2) NULL DEFAULT NULL COMMENT '套餐退款',
  `income_count` decimal(10, 2) NULL DEFAULT NULL COMMENT '总收入',
  `old_user_pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '老粉丝付费金额',
  `new_user_pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '新粉丝付费金额',
  `add_new_user` int NULL DEFAULT NULL COMMENT '新增粉丝数',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户',
  `user_count` int NULL DEFAULT NULL COMMENT '粉丝总数',
  `date` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6483 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者销售报表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_sale_week_report
-- ----------------------------
DROP TABLE IF EXISTS `author_sale_week_report`;
CREATE TABLE `author_sale_week_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `author_id` bigint NULL DEFAULT NULL COMMENT '作者id',
  `published_plan_count` int NULL DEFAULT NULL COMMENT '发布方案数',
  `new_user_pay` int NULL DEFAULT NULL COMMENT '新粉丝付费人数',
  `old_user_pay` int NULL DEFAULT NULL COMMENT '老粉丝付费人数',
  `pay_user_count` int NULL DEFAULT NULL COMMENT '付费人数',
  `article_income` decimal(10, 2) NULL DEFAULT NULL COMMENT '方案收入',
  `privilege_income` decimal(10, 2) NULL DEFAULT NULL COMMENT '套餐收入',
  `article_refund` decimal(10, 2) NULL DEFAULT NULL COMMENT '方案退款',
  `privilege_refund` decimal(10, 2) NULL DEFAULT NULL COMMENT '套餐退款',
  `income_count` decimal(10, 2) NULL DEFAULT NULL COMMENT '总收入',
  `old_user_pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '老粉丝付费金额',
  `new_user_pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '新粉丝付费金额',
  `add_new_user` int NULL DEFAULT NULL COMMENT '新增粉丝数',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户',
  `user_count` int NULL DEFAULT NULL COMMENT '粉丝总数',
  `date` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '周数',
  `date_range` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '周范围',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5107 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者销售周报表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_user
-- ----------------------------
DROP TABLE IF EXISTS `author_user`;
CREATE TABLE `author_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` bigint NULL DEFAULT 0,
  `user_id` bigint NULL DEFAULT NULL COMMENT '关联用户id',
  `mobile` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar_url` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像地址',
  `birthday` date NULL DEFAULT NULL COMMENT '生日',
  `sex` tinyint NULL DEFAULT 1 COMMENT '性别：0保密 1男 2女',
  `province_id` int NULL DEFAULT NULL COMMENT '省',
  `city_id` int NULL DEFAULT NULL COMMENT '城市',
  `adept_at` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '擅长赛事',
  `sort_num` int NULL DEFAULT NULL COMMENT '排序号',
  `status` int NULL DEFAULT 0 COMMENT '帐号状态 0启用 1禁用',
  `tags` json NULL COMMENT '标签',
  `welcome_msg` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '欢迎语',
  `cs_wechat` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客服微信',
  `cs_wecom` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客服企业微信',
  `recommend` int UNSIGNED NULL DEFAULT NULL COMMENT '作者推荐首页热门位置1-6位',
  `recommend_start_time` datetime NULL DEFAULT NULL COMMENT '作者推荐首页开始时间',
  `recommend_end_time` datetime NULL DEFAULT NULL COMMENT '作者推荐首页结束时间',
  `author_source` tinyint NULL DEFAULT 0 COMMENT '作者来源 0.外部作者 1.内部作者',
  `author_real_source` tinyint NULL DEFAULT 0 COMMENT '作者真实来源 0.管理后台 1.APP',
  `author_level` tinyint NULL DEFAULT 0 COMMENT '作者等级 0作者 1专家',
  `wechat_account` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信账号',
  `qq_account` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'qq账号',
  `recommend_user_id` int NULL DEFAULT NULL COMMENT '推荐人',
  `intro` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '作者简介',
  `remark` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `msg_push_threshold` tinyint NULL DEFAULT NULL COMMENT '消息推送阈值',
  `is_signed` tinyint(1) NULL DEFAULT 0 COMMENT '是否已签约：0未签约，1已签约',
  `order_ratio` decimal(5, 2) NULL DEFAULT NULL COMMENT '订单分成比例',
  `withdrawal_fee_ratio` decimal(5, 2) NULL DEFAULT NULL COMMENT '提现手续费比例',
  `min_withdrawal_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '最小提现金额',
  `max_withdrawal_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '最大提现金额',
  `withdrawal_period` int NULL DEFAULT NULL COMMENT '提现周期',
  `total_articles` int NULL DEFAULT 0 COMMENT '总方案数',
  `hit_rate` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '命中率',
  `recent_articles` int NULL DEFAULT 0 COMMENT '最近方案数',
  `recent_hits` int NULL DEFAULT 0 COMMENT '最近命中次数',
  `consecutive_hits` int NULL DEFAULT 0 COMMENT '连续命中次数',
  `last_login_ip` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后登录IP',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `register_ip` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '注册IP',
  `fans` int NULL DEFAULT 0 COMMENT '作者粉丝数',
  `on_sale_num` int NULL DEFAULT NULL COMMENT '在售方案数',
  `apply_id` bigint NULL DEFAULT 0 COMMENT '作者申请ID',
  `rtp` decimal(10, 2) NULL DEFAULT 0.00,
  `private_status` tinyint(1) NULL DEFAULT 0 COMMENT '是否开启私域',
  `performance_show` tinyint(1) NULL DEFAULT 1 COMMENT '是否显示战绩',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户',
  `is_attention` tinyint NULL DEFAULT 0 COMMENT '强制关注服务号0.是  1.否',
  `consume` tinyint NULL DEFAULT 0 COMMENT '强制关注服务号0.全部  1.未消费  2.已消费',
  `captive_push_account` bigint NULL DEFAULT NULL COMMENT '作者专属公众号',
  `report_push` tinyint NULL DEFAULT 0 COMMENT '钉钉推送 0.否  1.是',
  `alarm_account_count` int NULL DEFAULT NULL COMMENT '触发告警公众号数',
  `push_mode` tinyint NULL DEFAULT 0 COMMENT '0.旧推送  1.新推送',
  `ban_push` tinyint NULL DEFAULT 0 COMMENT '禁止作者推送 0.否 1.是',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_userId`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1000009 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_user_account
-- ----------------------------
DROP TABLE IF EXISTS `author_user_account`;
CREATE TABLE `author_user_account`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `author_id` bigint NULL DEFAULT NULL COMMENT '作者id',
  `income_count` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '余额总收益',
  `balance` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '余额收益',
  `gold` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '鱼币收益',
  `income_gold_count` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '鱼币总收益',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT 1 COMMENT '租户',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_author_id`(`author_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者账户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for author_user_account_log
-- ----------------------------
DROP TABLE IF EXISTS `author_user_account_log`;
CREATE TABLE `author_user_account_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `author_id` bigint NULL DEFAULT NULL COMMENT '作者id',
  `type` int NOT NULL COMMENT '类型 1-分成收益 2-提现 3-系统  4.退款 5.提现失败退还',
  `before_balance` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '修改前余额',
  `balance` decimal(10, 2) NOT NULL COMMENT '修改的余额',
  `after_balance` decimal(10, 2) NOT NULL COMMENT '修改余额',
  `remark` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `order_id` bigint NULL DEFAULT NULL COMMENT '订单ID',
  `before_gold` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '修改前鱼币',
  `gold` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '修改的鱼币',
  `after_gold` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '修改后鱼币',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `author_user_account_log_UN`(`type` ASC, `order_id` ASC) USING BTREE,
  INDEX `idx_author_id`(`author_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者账户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for channel_live_code
-- ----------------------------
DROP TABLE IF EXISTS `channel_live_code`;
CREATE TABLE `channel_live_code`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `channel_live_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '渠道活码名称',
  `share_token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分享令牌 (关联追踪日志表，业务唯一)',
  `author_id` bigint NULL DEFAULT NULL COMMENT '作者ID',
  `channel_group_id` int NULL DEFAULT NULL COMMENT '渠道分组ID',
  `channel_live_code_type` tinyint(1) NULL DEFAULT NULL COMMENT '渠道活码类型 1=系统活码 ,2=作者活码,3=公众号',
  `label_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '标签ID集合',
  `access_people` int NULL DEFAULT 0 COMMENT '访问人数',
  `register_people` int NULL DEFAULT 0 COMMENT '注册人数',
  `completed_order` int NULL DEFAULT 0 COMMENT '成交订单数',
  `transaction_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '成交金额',
  `channel_live_code_location_id` int NULL DEFAULT NULL COMMENT '渠道活码位置，字典配置',
  `channel_live_code_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '渠道活码地址',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态 0=正常,1=禁用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `referrer` bigint NULL DEFAULT NULL COMMENT '推广员',
  `page_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分享页面路径',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户',
  `deadline_time` datetime NULL DEFAULT NULL COMMENT '活码有效期',
  `deadline_type` tinyint(4) UNSIGNED ZEROFILL NULL DEFAULT NULL COMMENT '活码到期后跳转1-首页2-赛事页3-下载页4-个人中心',
  `article_id` bigint NULL DEFAULT NULL COMMENT '作者方案ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '渠道活码列表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for channel_live_code_group
-- ----------------------------
DROP TABLE IF EXISTS `channel_live_code_group`;
CREATE TABLE `channel_live_code_group`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `channel_group_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '渠道活码分组名称',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '渠道活码分组' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for channel_live_code_label
-- ----------------------------
DROP TABLE IF EXISTS `channel_live_code_label`;
CREATE TABLE `channel_live_code_label`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `label_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标签名称',
  `label_type` int NULL DEFAULT NULL COMMENT '标签分类',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '渠道活码标签' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for channel_live_code_scan_log
-- ----------------------------
DROP TABLE IF EXISTS `channel_live_code_scan_log`;
CREATE TABLE `channel_live_code_scan_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `live_code_id` bigint NOT NULL DEFAULT 0 COMMENT '渠道码ID',
  `share_token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联配置表',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '本次扫码会话ID (UUID，用于关联后续行为日志)',
  `user_id` bigint NULL DEFAULT 0 COMMENT '用户ID (扫码时若已登录则填充，否则为NULL)',
  `referrer_id` bigint NOT NULL COMMENT '分享发起人ID (谁生成的码)',
  `union_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '社交unionId',
  `open_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '社交openId',
  `ip_address` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'ip地址',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户浏览器信息',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `redirect_page` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '渠道活码重定向页面地址',
  `device_id` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备ID',
  `browser_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '扫码浏览器',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_session`(`trace_id` ASC) USING BTREE,
  INDEX `idx_live_code_id`(`live_code_id` ASC) USING BTREE COMMENT '渠道活码ID索引'
) ENGINE = InnoDB AUTO_INCREMENT = 78 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '渠道活码扫码流水表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for channel_referrer
-- ----------------------------
DROP TABLE IF EXISTS `channel_referrer`;
CREATE TABLE `channel_referrer`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分成记录ID',
  `user_id` bigint NOT NULL COMMENT '会员ID',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '渠道推广员' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for coupon_info
-- ----------------------------
DROP TABLE IF EXISTS `coupon_info`;
CREATE TABLE `coupon_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `author_id` bigint NOT NULL DEFAULT 0 COMMENT '生效作者',
  `coupon_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '券名称',
  `coupon_type` tinyint NOT NULL DEFAULT 0 COMMENT '券类型 0抵扣券 1折扣券 2不中补单券',
  `threshold_amount` decimal(20, 2) NOT NULL DEFAULT 0.00 COMMENT '满多少金额',
  `discount_amount` decimal(20, 2) NOT NULL DEFAULT 0.00 COMMENT '减多少金额',
  `scope` tinyint(1) NOT NULL DEFAULT 1 COMMENT '作用范围 1全部 2方案 3套餐',
  `effective_type` tinyint(1) NULL DEFAULT 0 COMMENT '生效时间 0领取立即生效 1领取后N天生效',
  `effective_days` int NULL DEFAULT 0 COMMENT '领取后N天生效',
  `expiration_days` int NULL DEFAULT 0 COMMENT 'N天失效',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '状态 0开启 1关闭',
  `cost` tinyint(1) NULL DEFAULT 0 COMMENT '成本承担 0专家',
  `max_issue_count` int NOT NULL DEFAULT 0 COMMENT '最大发行数量',
  `issued_count` int NULL DEFAULT 0 COMMENT '已发放数量',
  `used_count` int NULL DEFAULT 0 COMMENT '已使用数量',
  `desc_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述说明信息',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `updater` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标志',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `author_id_index`(`author_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代金券' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for identity_apply
-- ----------------------------
DROP TABLE IF EXISTS `identity_apply`;
CREATE TABLE `identity_apply`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL DEFAULT 0 COMMENT '申请用户的ID',
  `source` tinyint NOT NULL DEFAULT 1 COMMENT '身份：1.作者 2.合伙人',
  `apply_type` tinyint NULL DEFAULT 1 COMMENT '申请类型: 1-自媒体(个人), 2-企业/网媒, 3-MCN机构',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家头像URL',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家昵称',
  `intro` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家介绍',
  `real_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `id_card_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号',
  `id_card_front` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证人像面URL',
  `id_card_back` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证国徽面URL',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '审核状态: 1-审核中, 2-审核通过, 3-审核驳回',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '审批工作流实例id',
  `approver_id` bigint NULL DEFAULT NULL COMMENT '操作审核的管理员ID',
  `approver_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `approver_remark` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '审核备注/驳回原因 (后台填写, App端展示给用户)',
  `approved_at` datetime NULL DEFAULT NULL COMMENT '审核处理时间',
  `apply_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交申请时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `doc` json NULL COMMENT '扩展字段数据',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `id_card_no_index`(`id_card_no` ASC) USING BTREE,
  INDEX `idx_userId`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '身份入驻申请记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for kf_author
-- ----------------------------
DROP TABLE IF EXISTS `kf_author`;
CREATE TABLE `kf_author`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `kf_id` bigint NULL DEFAULT NULL COMMENT '客服id',
  `author_id` bigint NULL DEFAULT NULL COMMENT '作者id',
  `status` tinyint NULL DEFAULT 0 COMMENT '0.绑定  1.取消绑定',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT 1 COMMENT '租户',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客服绑定作者表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for kf_manage
-- ----------------------------
DROP TABLE IF EXISTS `kf_manage`;
CREATE TABLE `kf_manage`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客服名称',
  `wechat_account_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '私域id',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客服标识 自动生成不可修改',
  `status` tinyint NULL DEFAULT 0 COMMENT '0.启用  1.停用',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '上级客服',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '最后更新时间',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `author_ids` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绑定作者id',
  `tenant_id` bigint NULL DEFAULT 1 COMMENT '租户',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客服管理表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for member_activity
-- ----------------------------
DROP TABLE IF EXISTS `member_activity`;
CREATE TABLE `member_activity`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `type` tinyint NOT NULL DEFAULT 0 COMMENT '类型：1-新人活动',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动名称',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0-启用, 1-禁用',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `scope` tinyint NOT NULL DEFAULT 1 COMMENT '作用范围: 1-全平台, 2-方案, 3-套餐',
  `discount_rate` decimal(10, 2) NOT NULL DEFAULT 10.00 COMMENT '折扣',
  `available_num` int NOT NULL DEFAULT 1 COMMENT '可用次数',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '活动描述/规则说明',
  `promotion_text` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '提示文案',
  `participation_count` int NULL DEFAULT 0 COMMENT '活动参与总数',
  `coupon_id` bigint NULL DEFAULT NULL COMMENT '代金券ID',
  `author_id` bigint NULL DEFAULT 0 COMMENT '作者ID',
  `limited_num` int NULL DEFAULT 1 COMMENT '限领张数',
  `total_count` int NULL DEFAULT 1 COMMENT '发券总数',
  `issued_count` int NULL DEFAULT 0 COMMENT '已发行数量',
  `creator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updater` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1000008 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '新人活动' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_activity_logs
-- ----------------------------
DROP TABLE IF EXISTS `member_activity_logs`;
CREATE TABLE `member_activity_logs`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `order_id` bigint NULL DEFAULT 0 COMMENT '关联订单ID',
  `reward_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '优惠金额',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '参与时间',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '新人活动参与记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_app_push_setting
-- ----------------------------
DROP TABLE IF EXISTS `member_app_push_setting`;
CREATE TABLE `member_app_push_setting`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用: 0-关闭, 1-开启',
  `score_push` tinyint(1) NOT NULL DEFAULT 1 COMMENT '关注比分推送: 0-关闭, 1-开启',
  `publish_push` tinyint(1) NOT NULL DEFAULT 1 COMMENT '专家发单推送: 0-关闭, 1-开启',
  `disturb_mode` tinyint(1) NOT NULL DEFAULT 0 COMMENT '免打扰模式: 0-关闭, 1-开启',
  `disturb_start_time` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '免打扰开始时间(格式 HH:mm)',
  `disturb_end_time` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '免打扰结束时间(格式 HH:mm)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员App消息推送设置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_author_privilege
-- ----------------------------
DROP TABLE IF EXISTS `member_author_privilege`;
CREATE TABLE `member_author_privilege`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `author_id` bigint NOT NULL COMMENT '作者ID',
  `privilege_id` bigint NULL DEFAULT 0 COMMENT '特权ID',
  `parent_privilege_id` bigint NULL DEFAULT 0,
  `type` int NOT NULL COMMENT '特权类型：0：订阅套餐 1：专栏套餐 2:免费补单',
  `start_date` datetime NULL DEFAULT NULL COMMENT '生效时间',
  `end_date` datetime NULL DEFAULT NULL COMMENT '时效时间',
  `num` int NULL DEFAULT NULL COMMENT '剩余次数',
  `status` int NOT NULL COMMENT '状态 0：已过期 1：生效 2：待生效',
  `ex_num` int NULL DEFAULT 0 COMMENT '补单次数',
  `match_type` int NULL DEFAULT 0 COMMENT '玩法类型',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `updater` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_userId_authorId`(`user_id` ASC, `author_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户作者包时特权' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_customer
-- ----------------------------
DROP TABLE IF EXISTS `member_customer`;
CREATE TABLE `member_customer`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '姓名',
  `remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NOT NULL COMMENT '租户',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客服表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_customer_user
-- ----------------------------
DROP TABLE IF EXISTS `member_customer_user`;
CREATE TABLE `member_customer_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `customer_id` bigint NULL DEFAULT NULL COMMENT '客服ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NOT NULL COMMENT '租户',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客服用户关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_event_log
-- ----------------------------
DROP TABLE IF EXISTS `member_event_log`;
CREATE TABLE `member_event_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户ID',
  `ip` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `device_brand` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备品牌',
  `device_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备唯一标识',
  `device_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备型号',
  `os_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作系统名称',
  `os_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作系统版本',
  `browser_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浏览器名称',
  `browser_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浏览器版本',
  `oaid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'OAID',
  `app_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '应用版本',
  `app_wgt_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '应用资源版本',
  `device_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备类型(phone/pad/pc)',
  `system_language` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '系统语言',
  `event_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '事件编码',
  `event_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '事件名称',
  `event_attribute` json NULL COMMENT '事件属性(JSON格式)',
  `click_timestamp` bigint NOT NULL DEFAULT 0 COMMENT '上报时间戳',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12556 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '埋点事件日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_feedback
-- ----------------------------
DROP TABLE IF EXISTS `member_feedback`;
CREATE TABLE `member_feedback`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键编号',
  `user_id` bigint NULL DEFAULT NULL COMMENT '提交反馈的用户ID (若允许游客则可为空)',
  `feedback_type` tinyint NOT NULL COMMENT '问题类型: 1-支付相关, 2-内容不全, 3-系统错误, 4-资讯纠错, 5-其他',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈详情描述',
  `image_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '上传图片 多张用,号隔开',
  `contact_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '联系人姓名',
  `contact_way` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '联系方式(手机号或邮箱)',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态: 0-待处理, 1-已处理, 2-已忽略',
  `reply_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '后台回复内容',
  `reply_time` datetime NULL DEFAULT NULL COMMENT '回复时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户意见反馈表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_partner_record
-- ----------------------------
DROP TABLE IF EXISTS `member_partner_record`;
CREATE TABLE `member_partner_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分成记录ID',
  `partner_id` bigint NOT NULL COMMENT '合伙人id',
  `user_id` bigint NOT NULL COMMENT '会员ID',
  `bind_time` datetime NULL DEFAULT NULL COMMENT '绑定时间',
  `unbind_time` datetime NULL DEFAULT NULL COMMENT '解绑时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员合伙人关系记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_privilege_log
-- ----------------------------
DROP TABLE IF EXISTS `member_privilege_log`;
CREATE TABLE `member_privilege_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `privilege_id` bigint NOT NULL COMMENT '特权id',
  `type` tinyint(1) NULL DEFAULT NULL COMMENT '0.激活 1.使用',
  `order_id` bigint NULL DEFAULT NULL COMMENT '使用的订单id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户id',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '价格',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标题',
  `num` int NULL DEFAULT NULL COMMENT '剩余次数',
  `tenant_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_userId_privilegeId`(`user_id` ASC, `privilege_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户特权使用记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_risk_level
-- ----------------------------
DROP TABLE IF EXISTS `member_risk_level`;
CREATE TABLE `member_risk_level`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `level_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '风险等级类型编码',
  `level_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '风险等级名称',
  `min_score` int NOT NULL DEFAULT 0 COMMENT '最小分值',
  `max_score` int NOT NULL DEFAULT 0 COMMENT '最大分值',
  `desc_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述信息',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：1-已删除，0-未删除',
  `tenant_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '风险等级配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_risk_log
-- ----------------------------
DROP TABLE IF EXISTS `member_risk_log`;
CREATE TABLE `member_risk_log`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户ID',
  `score` int NOT NULL COMMENT '本次变动分数',
  `desc_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '风险描述/变动原因',
  `account_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公众号ID',
  `account_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公众号名称',
  `rule_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '关联规则ID',
  `source_type` tinyint UNSIGNED NULL DEFAULT 1 COMMENT '来源：1-系统自动，2-人工调整',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除：1-已删除，0-未删除',
  `tenant_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员风险变动日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_risk_score_rule
-- ----------------------------
DROP TABLE IF EXISTS `member_risk_score_rule`;
CREATE TABLE `member_risk_score_rule`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则编号',
  `score_element` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '计分要素编码',
  `element_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '计分要素名称',
  `trigger_info` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '触发条件描述',
  `single_score` int NOT NULL DEFAULT 0 COMMENT '单次得分',
  `max_accumulated` int NOT NULL DEFAULT 0 COMMENT '累计上限',
  `cycle_days` int NULL DEFAULT NULL COMMENT '计算周期天数',
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1-停用，0-启用',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：1-已删除，0-未删除',
  `tenant_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_rule_code`(`rule_code` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '风险等级计分规则表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_tag
-- ----------------------------
DROP TABLE IF EXISTS `member_tag`;
CREATE TABLE `member_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tag_name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态',
  `tag_type` tinyint NULL DEFAULT NULL COMMENT '1:粉丝标签 2:方案标签 ',
  `sort` tinyint NULL DEFAULT 0 COMMENT '排序',
  `author_id` bigint NULL DEFAULT 0 COMMENT '作者id',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户id',
  `sys_default` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否系统默认：0=是，1=否',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1000042 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '会员标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_user
-- ----------------------------
DROP TABLE IF EXISTS `member_user`;
CREATE TABLE `member_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录密码',
  `correlation_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公众号粉丝表关联ID',
  `mobile` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `sex` tinyint(1) NULL DEFAULT NULL COMMENT '性别 0-男 1-女',
  `avatar_url` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像地址',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '帐号状态 0-正常 1-封禁 后台使用此字段',
  `cancel` tinyint(1) NULL DEFAULT 0 COMMENT '账号注销 0-正常 1-注销 app使用此字段',
  `register_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '注册 IP',
  `last_login_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录IP',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '登录时间',
  `origin` int NOT NULL DEFAULT 0 COMMENT '来源 0 公众号 1 作者推荐',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `wx_account` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绑定微信号',
  `im_online` int NULL DEFAULT NULL COMMENT 'IM在线状态 0 登录 1 登出 2 离线',
  `im_register` int NULL DEFAULT 0 COMMENT 'IM注册状态 1 是 0 否',
  `identity` int NULL DEFAULT 0 COMMENT '身份 0=普通用户, 1=推广员',
  `live_code_id` bigint NULL DEFAULT 0 COMMENT '渠推广码',
  `referrer` bigint NULL DEFAULT NULL COMMENT '推荐人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户',
  `kf_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客服code',
  `invite_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邀请码',
  `partner_time` datetime NULL DEFAULT NULL COMMENT '合伙人加入时间',
  `partner_id` bigint NOT NULL DEFAULT 0 COMMENT '上级合伙人ID',
  `lock_fans_id` bigint NOT NULL DEFAULT 0 COMMENT '锁粉id',
  `lock_fans_time` datetime NULL DEFAULT NULL COMMENT '锁粉时间',
  `lock_fans_day` tinyint NOT NULL DEFAULT 30 COMMENT '锁粉有效期/天',
  `risk_score` int NULL DEFAULT 0 COMMENT '风险分数',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_mobile_tenant_id`(`mobile` ASC, `tenant_id` ASC) USING BTREE,
  UNIQUE INDEX `uq_inviteCode`(`invite_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1000824 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户端-用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_user_account
-- ----------------------------
DROP TABLE IF EXISTS `member_user_account`;
CREATE TABLE `member_user_account`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `balance` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
  `welfare` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '赠送金额',
  `total_recharge_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '总充值金额',
  `consume_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '消费金额',
  `order_num` int NOT NULL DEFAULT 0 COMMENT '订单数量',
  `refund_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '退费金额',
  `refund_num` int NULL DEFAULT 0 COMMENT '退款次数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1000806 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员账户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_user_account_logs
-- ----------------------------
DROP TABLE IF EXISTS `member_user_account_logs`;
CREATE TABLE `member_user_account_logs`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `type` int NOT NULL COMMENT '类型 1-鱼币充值 2-购买方案 3-购买套餐',
  `before_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '修改前金币',
  `amount` decimal(10, 2) NOT NULL COMMENT '修改的金币',
  `after_amount` decimal(10, 2) NOT NULL COMMENT '修改后金币',
  `remark` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `order_id` bigint NULL DEFAULT NULL COMMENT '订单ID',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `before_give_gold` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '修改前赠送鱼币',
  `give_gold` decimal(10, 2) NOT NULL COMMENT '修改的赠送鱼币',
  `after_give_gold` decimal(10, 2) NOT NULL COMMENT '修改后赠送鱼币',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '最后更新时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1000607 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户鱼币变更记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_user_blacklist
-- ----------------------------
DROP TABLE IF EXISTS `member_user_blacklist`;
CREATE TABLE `member_user_blacklist`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `author_id` bigint NOT NULL DEFAULT 0,
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户ID',
  `mobile` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `source_type` tinyint UNSIGNED NOT NULL DEFAULT 2 COMMENT '拉黑来源：1-系统自动(高风险)，2-人工手动',
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '拉黑原因描述',
  `risk_score` int NULL DEFAULT NULL COMMENT '关联风险分(仅系统自动时记录)',
  `block_push` bit(1) NOT NULL DEFAULT b'1' COMMENT '限制推送：1-拦截推送，0-正常',
  `block_access` bit(1) NOT NULL DEFAULT b'1' COMMENT '限制访问：1-禁止登录/访问，0-正常',
  `status` bit(1) NOT NULL DEFAULT b'0' COMMENT '状态：0-生效中，1-已解除/失效',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人/操作人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间/拉黑时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除：1-已删除，0-未删除',
  `tenant_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC, `author_id` ASC) USING BTREE COMMENT '确保一个用户只有一条有效黑名单记录',
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 98 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户黑名单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_user_coupon
-- ----------------------------
DROP TABLE IF EXISTS `member_user_coupon`;
CREATE TABLE `member_user_coupon`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `coupon_id` bigint NOT NULL COMMENT '代金券id',
  `author_id` bigint NULL DEFAULT 0 COMMENT '作者id',
  `order_id` bigint NULL DEFAULT 0 COMMENT '订单id',
  `order_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '订单号',
  `article_id` bigint NULL DEFAULT 0 COMMENT '文章id',
  `privilege_id` bigint NULL DEFAULT 0 COMMENT '套餐ID',
  `numbers` int NOT NULL DEFAULT 1 COMMENT '领取数量',
  `effective_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生效时间',
  `expiration_time` datetime NOT NULL COMMENT '过期时间',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态 0未使用 1已使用 2已过期 3作废',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `updater` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标志',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `coupon_id_index`(`coupon_id` ASC) USING BTREE,
  INDEX `user_id_index`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 71 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户代金券领取记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_user_coupon_usage
-- ----------------------------
DROP TABLE IF EXISTS `member_user_coupon_usage`;
CREATE TABLE `member_user_coupon_usage`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_coupon_id` bigint NULL DEFAULT NULL COMMENT '用户券ID,member_user_coupon.id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `coupon_id` bigint NOT NULL COMMENT '代金券id',
  `author_id` bigint NULL DEFAULT 0 COMMENT '作者id',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `order_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `article_id` bigint NULL DEFAULT 0 COMMENT '文章id',
  `privilege_id` bigint NULL DEFAULT 0 COMMENT '套餐ID',
  `numbers` int NULL DEFAULT 1 COMMENT '使用数量',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `updater` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标志',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `coupon_id_index`(`coupon_id` ASC) USING BTREE,
  INDEX `user_id_index`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户代金券使用记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_user_follow
-- ----------------------------
DROP TABLE IF EXISTS `member_user_follow`;
CREATE TABLE `member_user_follow`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `target_type` tinyint NULL DEFAULT NULL COMMENT '目标类型: 1-作者 2-文章 3-比赛',
  `target_id` bigint NOT NULL COMMENT '目标ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户编号',
  `user_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '用户状态 1-正常 2-封禁',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '状态：0关注，1取消关注',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_author`(`user_id` ASC, `target_id` ASC, `target_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1000195 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户关注作者表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for member_user_gold_logs
-- ----------------------------
DROP TABLE IF EXISTS `member_user_gold_logs`;
CREATE TABLE `member_user_gold_logs`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `type` int NOT NULL COMMENT '类型 1-鱼币充值 2-购买方案 3-购买套餐',
  `before_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '修改前金币',
  `amount` decimal(10, 2) NOT NULL COMMENT '修改的金币',
  `after_amount` decimal(10, 2) NOT NULL COMMENT '修改后金币',
  `remark` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `order_id` bigint NULL DEFAULT NULL COMMENT '订单ID',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `before_give_gold` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '修改前赠送鱼币',
  `give_gold` decimal(10, 2) NOT NULL COMMENT '修改的赠送鱼币',
  `after_give_gold` decimal(10, 2) NOT NULL COMMENT '修改后赠送鱼币',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '最后更新时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_order_id_type`(`order_id` ASC, `type` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户鱼币变更记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for platform_statistics_month_report
-- ----------------------------
DROP TABLE IF EXISTS `platform_statistics_month_report`;
CREATE TABLE `platform_statistics_month_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `date` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '报表日期',
  `active_user_count` int NULL DEFAULT 0 COMMENT '平台活跃人数',
  `new_user_count` int NULL DEFAULT 0 COMMENT '平台新增人数',
  `pay_user_count` int NULL DEFAULT 0 COMMENT '平台付费人数',
  `published_plan_count` int NULL DEFAULT 0 COMMENT '发布方案数',
  `receipt_amount` decimal(18, 2) NULL DEFAULT 0.00 COMMENT '收款金额',
  `refund_amount` decimal(18, 2) NULL DEFAULT 0.00 COMMENT '退款金额',
  `income_amount` decimal(18, 2) NULL DEFAULT 0.00 COMMENT '收入金额',
  `new_user_pay_total` decimal(18, 2) NULL DEFAULT 0.00 COMMENT '平台新人付费总额',
  `new_user_pay_count` int NULL DEFAULT 0 COMMENT '平台新人付费总数',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 138 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '平台运营月报表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for platform_statistics_report
-- ----------------------------
DROP TABLE IF EXISTS `platform_statistics_report`;
CREATE TABLE `platform_statistics_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `date` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '报表日期',
  `active_user_count` int NULL DEFAULT 0 COMMENT '平台活跃人数',
  `new_user_count` int NULL DEFAULT 0 COMMENT '平台新增人数',
  `pay_user_count` int NULL DEFAULT 0 COMMENT '平台付费人数',
  `published_plan_count` int NULL DEFAULT 0 COMMENT '发布方案数',
  `receipt_amount` decimal(18, 2) NULL DEFAULT 0.00 COMMENT '收款金额',
  `refund_amount` decimal(18, 2) NULL DEFAULT 0.00 COMMENT '退款金额',
  `income_amount` decimal(18, 2) NULL DEFAULT 0.00 COMMENT '收入金额',
  `new_user_pay_total` decimal(18, 2) NULL DEFAULT 0.00 COMMENT '平台新人付费总额',
  `new_user_pay_count` int NULL DEFAULT 0 COMMENT '平台新人付费总数',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 146 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '平台运营报表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for platform_statistics_week_report
-- ----------------------------
DROP TABLE IF EXISTS `platform_statistics_week_report`;
CREATE TABLE `platform_statistics_week_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `date` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '报表日期',
  `active_user_count` int NULL DEFAULT 0 COMMENT '平台活跃人数',
  `new_user_count` int NULL DEFAULT 0 COMMENT '平台新增人数',
  `pay_user_count` int NULL DEFAULT 0 COMMENT '平台付费人数',
  `published_plan_count` int NULL DEFAULT 0 COMMENT '发布方案数',
  `receipt_amount` decimal(18, 2) NULL DEFAULT 0.00 COMMENT '收款金额',
  `refund_amount` decimal(18, 2) NULL DEFAULT 0.00 COMMENT '退款金额',
  `income_amount` decimal(18, 2) NULL DEFAULT 0.00 COMMENT '收入金额',
  `new_user_pay_total` decimal(18, 2) NULL DEFAULT 0.00 COMMENT '平台新人付费总额',
  `new_user_pay_count` int NULL DEFAULT 0 COMMENT '平台新人付费总数',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
  `date_range` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '对应时间范围',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 138 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '平台运营报表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for privilege_push_config
-- ----------------------------
DROP TABLE IF EXISTS `privilege_push_config`;
CREATE TABLE `privilege_push_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `privilege_id` bigint NOT NULL COMMENT '套餐id',
  `author_id` bigint NOT NULL COMMENT '作者id',
  `consume_status` int NOT NULL DEFAULT 0 COMMENT '消费状态 0 全部 -1 不推送 1 有条件推送',
  `consume_min_num` int NULL DEFAULT NULL COMMENT '最小消费次数',
  `consume_max_num` int NULL DEFAULT NULL COMMENT '最大消费次数',
  `consume_min_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '最小消费金额',
  `consume_max_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '最大消费金额',
  `push_time` datetime NOT NULL COMMENT '推送时间',
  `template_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '推送模板id',
  `idx` int NULL DEFAULT 1 COMMENT '标识位',
  `push_type` tinyint(1) NOT NULL DEFAULT 1 COMMENT '推送类型：1-按条件推送 2-按分组推送 3-指定用户推送',
  `group_ids` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '选择的分组ID列表，字符串，多个以,分割',
  `user_ids` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '选择的用户ID列表，字符串，多个以,分割',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '套餐推送设置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for share_live_code
-- ----------------------------
DROP TABLE IF EXISTS `share_live_code`;
CREATE TABLE `share_live_code`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID (自增长)',
  `share_token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分享令牌 (关联追踪日志表，业务唯一)',
  `user_id` bigint NULL DEFAULT 0 COMMENT '分享用户ID (谁生成的码)',
  `target_type` tinyint NOT NULL COMMENT '分享类型：1-文章, 2-作者 (可扩展)',
  `target_id` bigint NOT NULL COMMENT '目标对象ID (作者ID/文章ID)',
  `force_follow_mp` tinyint NOT NULL DEFAULT 2 COMMENT '是否强制关注公众号：1-开启, 0-关闭',
  `author_id` bigint NULL DEFAULT 0 COMMENT '归属作者ID (KOL ID)',
  `page_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分享页面路径',
  `tenant_id` bigint NULL DEFAULT 0 COMMENT '租户ID',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建者账号/姓名',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新者账号/姓名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除',
  `share_push` tinyint NULL DEFAULT 0 COMMENT '0.分享   1.推送',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 437 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '分享活表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for share_scan_log
-- ----------------------------
DROP TABLE IF EXISTS `share_scan_log`;
CREATE TABLE `share_scan_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `share_token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联配置表',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '本次扫码会话ID (UUID，用于关联后续行为日志)',
  `user_id` bigint NULL DEFAULT 0 COMMENT '用户ID (扫码时若已登录则填充，否则为NULL)',
  `referrer_id` bigint NOT NULL COMMENT '分享发起人ID (谁生成的码)',
  `union_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '社交unionId',
  `open_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '社交openId',
  `ip_address` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'ip地址',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户浏览器信息',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除',
  `tenant_id` bigint NOT NULL COMMENT '租户ID (冗余，方便分区或查询)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_session`(`trace_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1539 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '扫码流水表 (归因实例)' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
