/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80409 (8.4.9)
 Source Host           : localhost:3306
 Source Schema         : shenyu-pay

 Target Server Type    : MySQL
 Target Server Version : 80409 (8.4.9)
 File Encoding         : 65001

 Date: 04/07/2026 22:58:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for finance_author_account
-- ----------------------------
DROP TABLE IF EXISTS `finance_author_account`;
CREATE TABLE `finance_author_account`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `author_id` bigint NOT NULL COMMENT '作者ID',
  `account_type` tinyint NOT NULL COMMENT '类型: 1 个人账户, 2 对公账户',
  `company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公司名称',
  `account_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '姓名',
  `id_card_number` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '身份证号',
  `id_card_front_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证正面图片',
  `id_card_back_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证反面图片',
  `withdraw_account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提现账号',
  `bank_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '开户银行',
  `bank_card_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '银行卡图片',
  `branch_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支行名称',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `subject_type` tinyint NULL DEFAULT NULL COMMENT '主体类型 1 个体工商户	\\r\\n2 事业单位\\r\\n3 民办非企业组织\\r\\n4 社会团体\\r\\n5 非法人企业\\r\\n6 企业法人\\r\\n7 自然人\\r\\n8 政府机关\\r\\n9 其他',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址信息',
  `card_addr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证住址',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `id_type` tinyint NULL DEFAULT NULL COMMENT '证件类型',
  `id_valid_start` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '证件有效期开始日期',
  `id_valid_end` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '证件有效期结束日期',
  `province_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '经营地址省',
  `city_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '经营地址市',
  `area_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '经营地址区',
  `contact_phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人手机号',
  `business_license_img` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '营业执照副本照片',
  `social_credit_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '统一社会信用代码',
  `business_area` tinyint NULL DEFAULT NULL COMMENT '营业面积',
  `company_staff` tinyint NULL DEFAULT NULL COMMENT '公司员工规模',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户id',
  `profession_id` tinyint NOT NULL DEFAULT 2,
  `ease_merchant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '首易信ID',
  `audit_status` int NULL DEFAULT 0 COMMENT '审核状态：0-未提交 1-待审核 2-审核通过 3-审核拒绝',
  `last_audit_time` datetime NULL DEFAULT NULL COMMENT '上次审核通过的时间',
  `last_audit_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '上次审核通过的备注',
  `auditor_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核人 ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `IDX_AUTHOR_ID`(`author_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者账户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_author_channel
-- ----------------------------
DROP TABLE IF EXISTS `finance_author_channel`;
CREATE TABLE `finance_author_channel`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `author_id` bigint NOT NULL COMMENT '作者ID',
  `channel_id` bigint NOT NULL COMMENT '关联渠道ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者渠道关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_author_split
-- ----------------------------
DROP TABLE IF EXISTS `finance_author_split`;
CREATE TABLE `finance_author_split`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分成记录ID',
  `author_id` bigint NOT NULL COMMENT '作者ID',
  `split_ratio` decimal(5, 2) NOT NULL COMMENT '分成比例',
  `company_split_ratio` decimal(5, 2) NULL DEFAULT NULL COMMENT '公司分成比例',
  `other_split_ratio` decimal(5, 2) NULL DEFAULT NULL COMMENT '其它分成比例',
  `withdraw_fee` decimal(10, 2) NOT NULL COMMENT '提现手续费',
  `min_withdraw_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '最小提现金额',
  `max_withdraw_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '最大提现金额',
  `settlement_cycle` int NOT NULL COMMENT '可提现金额结算周期',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `channel_split_ratio` decimal(5, 2) NULL DEFAULT NULL COMMENT '渠道分成比例',
  `channel_company_split_ratio` decimal(5, 2) NULL DEFAULT NULL COMMENT '渠道公司分成比例',
  `channel_other_split_ratio` decimal(5, 2) NULL DEFAULT NULL COMMENT '渠道其它分成比例',
  `tenant_id` bigint NOT NULL COMMENT '租户id',
  `liberty_split_ratio` decimal(5, 2) NULL DEFAULT NULL COMMENT '自有流量方与自主内容创作方分成比例',
  `company_liberty_split_ratio` decimal(5, 2) NULL DEFAULT NULL COMMENT '公司分成比例',
  `author_liberty_split_ratio` decimal(5, 2) NULL DEFAULT NULL COMMENT '作者分成比例',
  `limit_remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '提现金额限制',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `IDX_AUTHOR_ID`(`author_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 28 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者分成表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_channel
-- ----------------------------
DROP TABLE IF EXISTS `finance_channel`;
CREATE TABLE `finance_channel`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '渠道ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道名称',
  `is_default` tinyint(1) NULL DEFAULT 0 COMMENT '是否默认支付渠道(0:否,1:是)',
  `contract_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '签约费率',
  `withdraw_mode` tinyint(1) NULL DEFAULT NULL COMMENT '提现模式 1 分账提现 2 线下提现 0其它',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态(0:停用,1:启用)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '应用ID',
  `mch_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商户ID',
  `private_key` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API私钥',
  `wx_pay` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '是否开通微信 1 是 0 否',
  `ali_pay` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '是否开通支付宝 1 是 0 否',
  `wx_app_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信APPID',
  `mini_pay` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '小程序支付 1 是 0 否',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1002 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '支付渠道表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_channel_sub
-- ----------------------------
DROP TABLE IF EXISTS `finance_channel_sub`;
CREATE TABLE `finance_channel_sub`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `channel_id` bigint NOT NULL COMMENT '关联渠道ID',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `mch_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商户ID',
  `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '应用ID',
  `private_key` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'API私钥',
  `store_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '门店ID',
  `contract_rate` decimal(5, 2) NOT NULL COMMENT '签约费率',
  `wx_app_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '微信APPID',
  `wx_pay` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '是否开通微信 1 是 0 否',
  `ali_pay` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '是否开通支付宝 1 是 0 否',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态(0:停用,1:启用)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `mini_pay` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '小程序支付 1 是 0 否',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1005 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '支付渠道子表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_cooperation_apply
-- ----------------------------
DROP TABLE IF EXISTS `finance_cooperation_apply`;
CREATE TABLE `finance_cooperation_apply`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `author_id` bigint NOT NULL COMMENT '作者ID',
  `source` tinyint NOT NULL DEFAULT 1 COMMENT '来源：1.作者 2.合伙人 3.租户',
  `user_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名称',
  `sharing_role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分账角色',
  `organization` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属机构',
  `channel_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '支付渠道ID',
  `payment_channel` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '支付渠道',
  `channel_rate` decimal(5, 2) NOT NULL COMMENT '支付渠道费率(如0.006表示0.6%)',
  `withdraw_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提现方式',
  `account_type` tinyint NOT NULL COMMENT '类型: 1 个人账户, 2 对公账户',
  `company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公司名称',
  `account_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '姓名',
  `id_card_number` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '身份证号',
  `id_card_front_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证正面图片',
  `id_card_back_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证反面图片',
  `business_license_image` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '营业执照副本照片',
  `withdraw_account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提现账号',
  `bank_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '开户银行',
  `withdraw_account_image` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '提现账号照片(银行卡/开户许可证)',
  `branch_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支行名称',
  `submit_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '状态 0 待审核 1 通过 2 拒绝 3 失效',
  `remarks` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '申请备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `audit_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核备注/反馈',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址信息',
  `card_addr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证住址',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `contact_phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人手机号',
  `id_type` tinyint NULL DEFAULT NULL COMMENT '证件类型',
  `id_valid_start` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '证件有效期开始日期',
  `id_valid_end` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '证件有效期结束日期',
  `province_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '经营地址省',
  `city_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '经营地址市',
  `area_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '经营地址区',
  `social_credit_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '统一社会信用代码',
  `business_area` tinyint NULL DEFAULT NULL COMMENT '营业面积',
  `company_staff` tinyint NULL DEFAULT NULL COMMENT '公司员工规模',
  `subject_type` tinyint NULL DEFAULT NULL COMMENT '主体类型 1 个体工商户	\\\\r\\\\n2 事业单位\\\\r\\\\n3 民办非企业组织\\\\r\\\\n4 社会团体\\\\r\\\\n5 非法人企业\\\\r\\\\n6 企业法人\\\\r\\\\n7 自然人\\\\r\\\\n8 政府机关\\\\r\\\\n9 其他',
  `legal_person_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '法人姓名',
  `profession_id` tinyint NULL DEFAULT NULL COMMENT '职业ID',
  `tenant_id` tinyint NULL DEFAULT NULL COMMENT '租户ID',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '审核失败原因',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 62 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商户申请表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_log
-- ----------------------------
DROP TABLE IF EXISTS `finance_log`;
CREATE TABLE `finance_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operation_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作ID',
  `operation_item` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作项',
  `operation_result` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作结果',
  `remarks` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `operation_ip` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作IP',
  `operation_type` tinyint(1) NOT NULL COMMENT '状态 1 商户操作 2提现审核操作',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人昵称',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 93 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商户操作记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_merchant_account
-- ----------------------------
DROP TABLE IF EXISTS `finance_merchant_account`;
CREATE TABLE `finance_merchant_account`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_id` bigint NOT NULL COMMENT '账户ID',
  `account_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '账户名称',
  `account_type` tinyint NOT NULL COMMENT '类型 1 个人账户, 2 对公账户',
  `channel` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属通道',
  `total_balance` decimal(15, 2) NOT NULL DEFAULT 0.00 COMMENT '总余额',
  `frozen_amount` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '冻结金额',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NORMAL' COMMENT '状态 1 正常 2 冻结',
  `freeze_reason` tinyint NULL DEFAULT NULL COMMENT '冻结原因 1 交易异常/涉嫌套现 2 司法冻结 3 其它',
  `freeze_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '冻结说明',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT ' 租户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 54 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商户账户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_merchant_account_detail
-- ----------------------------
DROP TABLE IF EXISTS `finance_merchant_account_detail`;
CREATE TABLE `finance_merchant_account_detail`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_id` bigint NOT NULL COMMENT '账户ID',
  `account_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账户名称',
  `operation_action` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作动作',
  `operation_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '单号',
  `amount` decimal(15, 2) NOT NULL COMMENT '金额',
  `balance` decimal(15, 2) NOT NULL COMMENT '余额',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 544 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '资金账号流水明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_merchant_account_sub
-- ----------------------------
DROP TABLE IF EXISTS `finance_merchant_account_sub`;
CREATE TABLE `finance_merchant_account_sub`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `channel_id` bigint NULL DEFAULT NULL COMMENT '渠道ID',
  `total_balance` decimal(10, 2) NULL DEFAULT NULL COMMENT '账户可提现金额',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
  `account_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `account_type` tinyint(1) NULL DEFAULT NULL COMMENT '账户类型 1 作者 2 租户',
  `last_total_balance` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '上次可提现金额',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '账户渠道关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_partner_account
-- ----------------------------
DROP TABLE IF EXISTS `finance_partner_account`;
CREATE TABLE `finance_partner_account`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '会员ID',
  `account_type` tinyint NOT NULL COMMENT '类型: 1 个人账户, 2 对公账户',
  `company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公司名称',
  `account_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '姓名',
  `id_card_number` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '身份证号',
  `id_card_front_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证正面图片',
  `id_card_back_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证反面图片',
  `withdraw_account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提现账号',
  `bank_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '开户银行',
  `bank_card_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '银行卡图片',
  `branch_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支行名称',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `subject_type` tinyint NULL DEFAULT NULL COMMENT '主体类型 1 个体工商户	\\r\\n2 事业单位\\r\\n3 民办非企业组织\\r\\n4 社会团体\\r\\n5 非法人企业\\r\\n6 企业法人\\r\\n7 自然人\\r\\n8 政府机关\\r\\n9 其他',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址信息',
  `card_addr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证住址',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `id_type` tinyint NULL DEFAULT NULL COMMENT '证件类型',
  `id_valid_start` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '证件有效期开始日期',
  `id_valid_end` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '证件有效期结束日期',
  `province_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '经营地址省',
  `city_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '经营地址市',
  `area_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '经营地址区',
  `contact_phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人手机号',
  `business_license_img` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '营业执照副本照片',
  `social_credit_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '统一社会信用代码',
  `business_area` tinyint NULL DEFAULT NULL COMMENT '营业面积',
  `company_staff` tinyint NULL DEFAULT NULL COMMENT '公司员工规模',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户id',
  `profession_id` tinyint NOT NULL DEFAULT 2,
  `ease_merchant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '首易信ID',
  `audit_status` int NULL DEFAULT 0 COMMENT '审核状态：0-未提交 1-待审核 2-审核通过 3-审核拒绝',
  `last_audit_time` datetime NULL DEFAULT NULL COMMENT '上次审核通过的时间',
  `last_audit_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '上次审核通过的备注',
  `auditor_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核人 ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_userId`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '合伙人账户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_partner_split
-- ----------------------------
DROP TABLE IF EXISTS `finance_partner_split`;
CREATE TABLE `finance_partner_split`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分成记录ID',
  `user_id` bigint NOT NULL COMMENT '会员ID(一级合伙人)',
  `child_user_id` bigint NOT NULL COMMENT '会员ID(二级合伙人)',
  `child_split_ratio` decimal(5, 2) NULL DEFAULT NULL COMMENT '二级合伙人分成比例',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注名',
  `settlement_cycle` int NOT NULL DEFAULT 0 COMMENT '可提现金额结算周期',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_userId_childUserId`(`user_id` ASC, `child_user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '合伙人分成表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_tenant_channel
-- ----------------------------
DROP TABLE IF EXISTS `finance_tenant_channel`;
CREATE TABLE `finance_tenant_channel`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `channel_id` bigint NOT NULL COMMENT '关联渠道ID',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户id',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户渠道关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_transaction_detail
-- ----------------------------
DROP TABLE IF EXISTS `finance_transaction_detail`;
CREATE TABLE `finance_transaction_detail`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sharing_serial_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分账流水号',
  `order_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sharing_role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分账角色',
  `payee_id` bigint NULL DEFAULT NULL COMMENT '收款方ID',
  `payee` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收款方',
  `order_amount` decimal(15, 2) NULL DEFAULT NULL COMMENT '订单金额',
  `sharing_amount` decimal(15, 2) NOT NULL COMMENT '分账金额',
  `accounting_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '动账时间',
  `payment_time` datetime NULL DEFAULT NULL COMMENT '付款时间',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态 1 处理中 2 成功 3 失败',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `sharing_type` tinyint(1) NULL DEFAULT NULL COMMENT '分账类型  1:渠道支付费率,2:内容生成商 3:作者 4:渠道分销商 5:推广员 6:平台 7:支付费率差额',
  `tenant_id` bigint NOT NULL COMMENT ' 租户ID',
  `pay_status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态: 0 未支付 1支付成功  2 支付部分',
  `pay_amount` decimal(15, 2) NULL DEFAULT NULL COMMENT '支付部分，填可支付金额',
  `sharing_ratio` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '分账比例(%)',
  `split_type` tinyint(1) NOT NULL DEFAULT 0 COMMENT '分账状态 0 未分账 1 成功分账 2 分账成功 3 分账中',
  `is_refund` tinyint(1) NOT NULL DEFAULT 0 COMMENT '默认 0， 0 不退款 1 退款',
  `is_calculate` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否核算 默认0  0 未核算 1 已核算（到账户了）',
  `calculate_time` datetime NULL DEFAULT NULL COMMENT '已核算',
  `withdraw_method` tinyint(1) NULL DEFAULT 1 COMMENT '提现方式 1 分账提现 2 线下提现',
  `mch_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商户ID',
  `mch_id_sub` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '子商户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `IDX_ORDERNO_PAYEEID_SHARINGTYPE`(`order_no` ASC, `sharing_type` ASC, `is_refund` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1366 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '分账流水明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_transaction_detail_20260514
-- ----------------------------
DROP TABLE IF EXISTS `finance_transaction_detail_20260514`;
CREATE TABLE `finance_transaction_detail_20260514`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sharing_serial_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分账流水号',
  `order_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sharing_role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分账角色',
  `payee_id` bigint NULL DEFAULT NULL COMMENT '收款方ID',
  `payee` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收款方',
  `order_amount` decimal(15, 2) NULL DEFAULT NULL COMMENT '订单金额',
  `sharing_amount` decimal(15, 2) NOT NULL COMMENT '分账金额',
  `accounting_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '动账时间',
  `payment_time` datetime NULL DEFAULT NULL COMMENT '付款时间',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态 1 处理中 2 成功 3 失败',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `sharing_type` tinyint(1) NULL DEFAULT NULL COMMENT '分账类型  1:渠道支付费率,2:内容生成商 3:作者 4:渠道分销商 5:推广员 6:平台 7:支付费率差额',
  `tenant_id` bigint NOT NULL COMMENT ' 租户ID',
  `pay_status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态: 0 未支付 1支付成功  2 支付部分',
  `pay_amount` decimal(15, 2) NULL DEFAULT NULL COMMENT '支付部分，填可支付金额',
  `sharing_ratio` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '分账比例(%)',
  `split_type` tinyint(1) NOT NULL DEFAULT 0 COMMENT '分账状态 0 未分账 1 成功分账 2 分账成功 3 分账中',
  `is_refund` tinyint(1) NOT NULL DEFAULT 0 COMMENT '默认 0， 0 不退款 1 退款',
  `is_calculate` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否核算 默认0  0 未核算 1 已核算（到账户了）',
  `calculate_time` datetime NULL DEFAULT NULL COMMENT '已核算',
  `withdraw_method` tinyint(1) NULL DEFAULT 1 COMMENT '提现方式 1 分账提现 2 线下提现',
  `mch_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商户ID',
  `mch_id_sub` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '子商户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `IDX_ORDERNO_PAYEEID_SHARINGTYPE`(`order_no` ASC, `sharing_type` ASC, `is_refund` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 822 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '分账流水明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_transaction_detail_20260515
-- ----------------------------
DROP TABLE IF EXISTS `finance_transaction_detail_20260515`;
CREATE TABLE `finance_transaction_detail_20260515`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sharing_serial_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分账流水号',
  `order_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sharing_role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分账角色',
  `payee_id` bigint NULL DEFAULT NULL COMMENT '收款方ID',
  `payee` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收款方',
  `order_amount` decimal(15, 2) NULL DEFAULT NULL COMMENT '订单金额',
  `sharing_amount` decimal(15, 2) NOT NULL COMMENT '分账金额',
  `accounting_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '动账时间',
  `payment_time` datetime NULL DEFAULT NULL COMMENT '付款时间',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态 1 处理中 2 成功 3 失败',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `sharing_type` tinyint(1) NULL DEFAULT NULL COMMENT '分账类型  1:渠道支付费率,2:内容生成商 3:作者 4:渠道分销商 5:推广员 6:平台 7:支付费率差额',
  `tenant_id` bigint NOT NULL COMMENT ' 租户ID',
  `pay_status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态: 0 未支付 1支付成功  2 支付部分',
  `pay_amount` decimal(15, 2) NULL DEFAULT NULL COMMENT '支付部分，填可支付金额',
  `sharing_ratio` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '分账比例(%)',
  `split_type` tinyint(1) NOT NULL DEFAULT 0 COMMENT '分账状态 0 未分账 1 成功分账 2 分账成功 3 分账中',
  `is_refund` tinyint(1) NOT NULL DEFAULT 0 COMMENT '默认 0， 0 不退款 1 退款',
  `is_calculate` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否核算 默认0  0 未核算 1 已核算（到账户了）',
  `calculate_time` datetime NULL DEFAULT NULL COMMENT '已核算',
  `withdraw_method` tinyint(1) NULL DEFAULT 1 COMMENT '提现方式 1 分账提现 2 线下提现',
  `mch_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商户ID',
  `mch_id_sub` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '子商户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `IDX_ORDERNO_PAYEEID_SHARINGTYPE`(`order_no` ASC, `sharing_type` ASC, `is_refund` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 886 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '分账流水明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_withdraw_record
-- ----------------------------
DROP TABLE IF EXISTS `finance_withdraw_record`;
CREATE TABLE `finance_withdraw_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `withdraw_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提现单号',
  `applicant_id` bigint NOT NULL COMMENT '申请人ID',
  `applicant` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申请人',
  `applicant_role` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申请人角色',
  `account_type` tinyint(1) NOT NULL COMMENT '账户类型：1-个人账户 2-对公账户',
  `id_card_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号',
  `bank_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '银行名称',
  `bank_branch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支行全称',
  `audit_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核备注/反馈',
  `individual_legal_person` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '个人/法人',
  `company_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公司名称',
  `withdraw_account` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提现账号',
  `withdraw_amount` decimal(15, 2) NOT NULL COMMENT '提现金额',
  `withdraw_fee` decimal(15, 2) NOT NULL DEFAULT 0.00 COMMENT '提现手续费',
  `actual_arrival_amount` decimal(15, 2) NULL DEFAULT NULL COMMENT '实际到账金额',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `accounting_time` datetime NULL DEFAULT NULL COMMENT '动账时间',
  `withdraw_status` tinyint(1) NULL DEFAULT 0 COMMENT '提现状态 0 提交处理中 1 分账处理中 2 打款成功 3 失败',
  `payment_screenshot` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '打款截图',
  `withdraw_type` tinyint NOT NULL DEFAULT 1 COMMENT '提现类型 1 分账提现 2 线下提现',
  `remarks` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `thrid_order_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方ID',
  `tenant_id` bigint NOT NULL COMMENT ' 租户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 258 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提现记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_withdraw_record_260514
-- ----------------------------
DROP TABLE IF EXISTS `finance_withdraw_record_260514`;
CREATE TABLE `finance_withdraw_record_260514`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `withdraw_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提现单号',
  `applicant_id` bigint NOT NULL COMMENT '申请人ID',
  `applicant` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申请人',
  `applicant_role` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申请人角色',
  `account_type` tinyint(1) NOT NULL COMMENT '账户类型：1-个人账户 2-对公账户',
  `id_card_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号',
  `bank_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '银行名称',
  `bank_branch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支行全称',
  `audit_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核备注/反馈',
  `individual_legal_person` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '个人/法人',
  `company_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公司名称',
  `withdraw_account` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提现账号',
  `withdraw_amount` decimal(15, 2) NOT NULL COMMENT '提现金额',
  `withdraw_fee` decimal(15, 2) NOT NULL DEFAULT 0.00 COMMENT '提现手续费',
  `actual_arrival_amount` decimal(15, 2) NULL DEFAULT NULL COMMENT '实际到账金额',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `accounting_time` datetime NULL DEFAULT NULL COMMENT '动账时间',
  `withdraw_status` tinyint(1) NULL DEFAULT 0 COMMENT '提现状态 0 提交处理中 1 分账处理中 2 打款成功 3 失败',
  `payment_screenshot` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '打款截图',
  `withdraw_type` tinyint NOT NULL DEFAULT 1 COMMENT '提现类型 1 分账提现 2 线下提现',
  `remarks` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `thrid_order_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方ID',
  `tenant_id` bigint NOT NULL COMMENT ' 租户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 257 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提现记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_withdraw_record_order
-- ----------------------------
DROP TABLE IF EXISTS `finance_withdraw_record_order`;
CREATE TABLE `finance_withdraw_record_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `withdraw_record_id` bigint NOT NULL COMMENT '提现表主键',
  `sharing_serial_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分账流水号',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联订单号',
  `amount` decimal(15, 2) NOT NULL COMMENT '提现金额(分账明细贡献金额)',
  `sharing_role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分账角色',
  `payee` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '收款方',
  `order_total_amount` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '订单实收金额',
  `sharing_ratio` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '分账比例(%)',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `user_nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `user_mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户手机号',
  `order_pay_time` datetime NULL DEFAULT NULL COMMENT '订单付款时间',
  `order_pay_status` tinyint NULL DEFAULT 1 COMMENT '支付状态: 1支付成功 2已退款',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '提现状态 1 全部 2 部分',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NOT NULL COMMENT ' 租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_withdraw_record_id`(`withdraw_record_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 95 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提现审核关联订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_withdraw_record_order_260514
-- ----------------------------
DROP TABLE IF EXISTS `finance_withdraw_record_order_260514`;
CREATE TABLE `finance_withdraw_record_order_260514`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `withdraw_record_id` bigint NOT NULL COMMENT '提现表主键',
  `sharing_serial_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分账流水号',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联订单号',
  `amount` decimal(15, 2) NOT NULL COMMENT '提现金额(分账明细贡献金额)',
  `sharing_role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分账角色',
  `payee` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '收款方',
  `order_total_amount` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '订单实收金额',
  `sharing_ratio` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '分账比例(%)',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `user_nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `user_mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户手机号',
  `order_pay_time` datetime NULL DEFAULT NULL COMMENT '订单付款时间',
  `order_pay_status` tinyint NULL DEFAULT 1 COMMENT '支付状态: 1支付成功 2已退款',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '提现状态 1 全部 2 部分',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NOT NULL COMMENT ' 租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_withdraw_record_id`(`withdraw_record_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 91 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提现审核关联订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_withdraw_record_sub
-- ----------------------------
DROP TABLE IF EXISTS `finance_withdraw_record_sub`;
CREATE TABLE `finance_withdraw_record_sub`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `withdraw_record_id` bigint NOT NULL COMMENT '提现表主键',
  `sub_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '子单号',
  `account_type` tinyint(1) NOT NULL COMMENT '账户类型：1-个人账户 2-对公账户',
  `id_card_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号',
  `bank_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '银行名称',
  `bank_branch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支行全称',
  `audit_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核备注/反馈',
  `individual_legal_person` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '个人/法人',
  `company_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公司名称',
  `withdraw_account` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提现账号',
  `withdraw_amount` decimal(15, 2) NOT NULL COMMENT '提现金额',
  `withdraw_fee` decimal(15, 2) NOT NULL DEFAULT 0.00 COMMENT '提现手续费',
  `actual_arrival_amount` decimal(15, 2) NULL DEFAULT NULL COMMENT '实际到账金额',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `accounting_time` datetime NULL DEFAULT NULL COMMENT '动账时间',
  `withdraw_status` tinyint(1) NULL DEFAULT 0 COMMENT '提现状态 0 提交处理中 1 分账处理中 2 打款成功 3 失败',
  `payment_screenshot` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '打款截图',
  `withdraw_type` tinyint NOT NULL DEFAULT 1 COMMENT '提现类型 1 分账提现 2 线下提现',
  `remarks` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `thrid_order_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方ID',
  `tenant_id` bigint NOT NULL COMMENT ' 租户ID',
  `channel_id` bigint NULL DEFAULT NULL COMMENT '渠道ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 254 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提现记录子表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for finance_withdraw_record_sub_260514
-- ----------------------------
DROP TABLE IF EXISTS `finance_withdraw_record_sub_260514`;
CREATE TABLE `finance_withdraw_record_sub_260514`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `withdraw_record_id` bigint NOT NULL COMMENT '提现表主键',
  `sub_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '子单号',
  `account_type` tinyint(1) NOT NULL COMMENT '账户类型：1-个人账户 2-对公账户',
  `id_card_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号',
  `bank_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '银行名称',
  `bank_branch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支行全称',
  `audit_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核备注/反馈',
  `individual_legal_person` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '个人/法人',
  `company_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公司名称',
  `withdraw_account` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提现账号',
  `withdraw_amount` decimal(15, 2) NOT NULL COMMENT '提现金额',
  `withdraw_fee` decimal(15, 2) NOT NULL DEFAULT 0.00 COMMENT '提现手续费',
  `actual_arrival_amount` decimal(15, 2) NULL DEFAULT NULL COMMENT '实际到账金额',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `accounting_time` datetime NULL DEFAULT NULL COMMENT '动账时间',
  `withdraw_status` tinyint(1) NULL DEFAULT 0 COMMENT '提现状态 0 提交处理中 1 分账处理中 2 打款成功 3 失败',
  `payment_screenshot` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '打款截图',
  `withdraw_type` tinyint NOT NULL DEFAULT 1 COMMENT '提现类型 1 分账提现 2 线下提现',
  `remarks` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `thrid_order_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方ID',
  `tenant_id` bigint NOT NULL COMMENT ' 租户ID',
  `channel_id` bigint NULL DEFAULT NULL COMMENT '渠道ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 253 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提现记录子表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for partner_config
-- ----------------------------
DROP TABLE IF EXISTS `partner_config`;
CREATE TABLE `partner_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `lock_fans_days` int NOT NULL DEFAULT 30 COMMENT '锁粉天数',
  `partner_apply_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '合伙人申请图片地址',
  `settlement_cycle` int NOT NULL COMMENT '到账周期',
  `withdrawal_limit` decimal(10, 2) NOT NULL COMMENT '最低提现',
  `withdrawal_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '提现手续费',
  `withdrawal_frequency` int NOT NULL COMMENT '每日提现次数',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态(0:停用,1:启用)',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '最后更新时间',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `updater` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '合伙人设置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pay_all_order
-- ----------------------------
DROP TABLE IF EXISTS `pay_all_order`;
CREATE TABLE `pay_all_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `user_id` bigint NOT NULL COMMENT '用户id',
  `article_id` bigint NULL DEFAULT 0 COMMENT '文章id',
  `privilege_id` bigint NULL DEFAULT 0 COMMENT '套餐ID',
  `author_id` bigint NOT NULL COMMENT '作者id',
  `order_source` tinyint NULL DEFAULT 1 COMMENT '订单来源：1.H5 2.APP  3 直播',
  `order_type` tinyint NULL DEFAULT 0 COMMENT '订单类型 0.方案订单 1.订阅套餐 2.专栏套餐',
  `amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
  `pay_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
  `coupon_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '优惠券金额',
  `pay_gold` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '支付鱼币',
  `author_divide` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '作者分成',
  `sharer_divide` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '分享者分成',
  `partner_divide` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '合作伙伴分成',
  `pay_type` int NOT NULL DEFAULT 0 COMMENT '支付方式 0：金币支付 1：支付宝 2：微信 3:补单特权支付 4:订阅特权支付 5:专栏特权支付',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态 0.待支付 1.支付成功  2.支付失败',
  `is_refund` tinyint NULL DEFAULT 0 COMMENT '是否退款 1：是 0：否',
  `refund_time` datetime NULL DEFAULT NULL COMMENT '退款时间',
  `third_party_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方支付单号',
  `channel_order_sn` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '通道订单号，微信订单号、支付宝订单号等',
  `ins_order_sn` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '机构订单号（显示在微信/支付宝支付凭证的订单号）',
  `remark` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `from_article_id` bigint NULL DEFAULT NULL COMMENT '补单的文章id',
  `pay_app_id` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付渠道app_id',
  `user_privilege_id` bigint NULL DEFAULT 0 COMMENT '用户使用特权购买文章套餐id',
  `pay_app_type` int NULL DEFAULT 0 COMMENT '支付渠道类型 默认 0 付呗 1 收钱吧',
  `real_divide_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '实际分成总金额',
  `kf_id` bigint NULL DEFAULT NULL COMMENT '所属客服id',
  `son_privilege_id` bigint NULL DEFAULT 0 COMMENT '购买的子套餐id',
  `give_gold` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '赠送鱼币支付',
  `author_gold_divide` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '作者鱼币分成',
  `is_activity_order` tinyint(1) NULL DEFAULT 0 COMMENT '活动订单 0.否  1.是',
  `is_first_order` tinyint(1) NULL DEFAULT 0 COMMENT '是否首单：0复购，1首单',
  `is_redo` tinyint(1) NULL DEFAULT 0 COMMENT '0.未补单  1.已补单',
  `live_code_id` bigint NULL DEFAULT 0 COMMENT '渠推广码',
  `referrer` bigint NULL DEFAULT NULL COMMENT '推荐人',
  `referrer_type` tinyint NULL DEFAULT 0 COMMENT '推荐人类型：1.作者 2.合伙人',
  `senior_partner_id` bigint NULL DEFAULT 0 COMMENT '上级合伙人ID',
  `trace_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '分享追踪编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户id',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `open_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'openId 只有小程序支付需要',
  `match_type` int NULL DEFAULT 0 COMMENT '方案赛事类型 旧订单都没有',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `scheme_order_status_IDX`(`status` ASC, `create_time` ASC) USING BTREE,
  INDEX `scheme_order_author_id_IDX`(`author_id` ASC) USING BTREE,
  INDEX `article_id_idx`(`article_id` ASC) USING BTREE,
  INDEX `idx_sonPrivilegeId`(`son_privilege_id` ASC) USING BTREE,
  INDEX `idx_privilegeId`(`privilege_id` ASC) USING BTREE,
  INDEX `idx_live_code_id`(`live_code_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1002301 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pay_all_order_20260514
-- ----------------------------
DROP TABLE IF EXISTS `pay_all_order_20260514`;
CREATE TABLE `pay_all_order_20260514`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `user_id` bigint NOT NULL COMMENT '用户id',
  `article_id` bigint NULL DEFAULT 0 COMMENT '文章id',
  `privilege_id` bigint NULL DEFAULT 0 COMMENT '套餐ID',
  `author_id` bigint NOT NULL COMMENT '作者id',
  `order_source` tinyint NULL DEFAULT 1 COMMENT '订单来源：1.H5 2.APP  3 直播',
  `order_type` tinyint NULL DEFAULT 0 COMMENT '订单类型 0.方案订单 1.订阅套餐 2.专栏套餐',
  `amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
  `pay_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
  `coupon_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '优惠券金额',
  `pay_gold` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '支付鱼币',
  `author_divide` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '作者分成',
  `sharer_divide` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '分享者分成',
  `partner_divide` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '合作伙伴分成',
  `pay_type` int NOT NULL DEFAULT 0 COMMENT '支付方式 0：金币支付 1：支付宝 2：微信 3:补单特权支付 4:订阅特权支付 5:专栏特权支付',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态 0.待支付 1.支付成功  2.支付失败',
  `is_refund` tinyint NULL DEFAULT 0 COMMENT '是否退款 1：是 0：否',
  `refund_time` datetime NULL DEFAULT NULL COMMENT '退款时间',
  `third_party_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方支付单号',
  `channel_order_sn` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '通道订单号，微信订单号、支付宝订单号等',
  `ins_order_sn` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '机构订单号（显示在微信/支付宝支付凭证的订单号）',
  `remark` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `from_article_id` bigint NULL DEFAULT NULL COMMENT '补单的文章id',
  `pay_app_id` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付渠道app_id',
  `user_privilege_id` bigint NULL DEFAULT 0 COMMENT '用户使用特权购买文章套餐id',
  `pay_app_type` int NULL DEFAULT 0 COMMENT '支付渠道类型 默认 0 付呗 1 收钱吧',
  `real_divide_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '实际分成总金额',
  `kf_id` bigint NULL DEFAULT NULL COMMENT '所属客服id',
  `son_privilege_id` bigint NULL DEFAULT 0 COMMENT '购买的子套餐id',
  `give_gold` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '赠送鱼币支付',
  `author_gold_divide` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '作者鱼币分成',
  `is_activity_order` tinyint(1) NULL DEFAULT 0 COMMENT '活动订单 0.否  1.是',
  `is_first_order` tinyint(1) NULL DEFAULT 0 COMMENT '是否首单：0复购，1首单',
  `is_redo` tinyint(1) NULL DEFAULT 0 COMMENT '0.未补单  1.已补单',
  `live_code_id` bigint NULL DEFAULT 0 COMMENT '渠推广码',
  `referrer` bigint NULL DEFAULT NULL COMMENT '推荐人',
  `referrer_type` tinyint NULL DEFAULT 0 COMMENT '推荐人类型：1.作者 2.合伙人',
  `senior_partner_id` bigint NULL DEFAULT 0 COMMENT '上级合伙人ID',
  `trace_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '分享追踪编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户id',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `open_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'openId 只有小程序支付需要',
  `match_type` int NULL DEFAULT 0 COMMENT '方案赛事类型 旧订单都没有',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `scheme_order_status_IDX`(`status` ASC, `create_time` ASC) USING BTREE,
  INDEX `scheme_order_author_id_IDX`(`author_id` ASC) USING BTREE,
  INDEX `article_id_idx`(`article_id` ASC) USING BTREE,
  INDEX `idx_sonPrivilegeId`(`son_privilege_id` ASC) USING BTREE,
  INDEX `idx_privilegeId`(`privilege_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1000575 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pay_gold_order
-- ----------------------------
DROP TABLE IF EXISTS `pay_gold_order`;
CREATE TABLE `pay_gold_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '订单号',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户id',
  `amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '订单金额',
  `pay_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '支付金额',
  `commission` decimal(10, 2) NULL DEFAULT NULL COMMENT '手续费',
  `gold_num` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '金币数',
  `pay_type` tinyint NULL DEFAULT NULL COMMENT '支付方式 1.支付宝  2.微信 3.人工充值 8.苹果支付',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `status` tinyint NULL DEFAULT 0 COMMENT '支付状态 0.待支付 1.支付成功  2.支付失败',
  `third_party_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方订单号',
  `channel_order_sn` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '通道订单号，微信订单号、支付宝订单号等',
  `ins_order_sn` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '机构订单号（显示在微信/支付宝支付凭证的订单号）',
  `client_ip` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '下单ip地址',
  `present_gold` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '赠送金币',
  `pay_app_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付渠道app_id',
  `pay_app_type` int NULL DEFAULT 0 COMMENT '支付渠道类型 默认 0 付呗 1 收钱吧',
  `is_refund` int NULL DEFAULT 0 COMMENT '是否退款 1：是 0：否',
  `refund_time` datetime NULL DEFAULT NULL COMMENT '退款时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `order_source` tinyint NULL DEFAULT 1 COMMENT '订单来源：1.H5 2.APP',
  `is_activity_order` tinyint NULL DEFAULT 0 COMMENT '活动订单 0.否  1.是',
  `apple_receipt` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '苹果支付票据',
  `apple_product_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '苹果商品ID',
  `apple_transaction_id` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '苹果交易ID',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `update_time` datetime NULL DEFAULT NULL COMMENT '最后更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户id',
  `split_status` tinyint NOT NULL DEFAULT 0 COMMENT '分账状态： 0 未分 1 已分完  2 分部分',
  `split_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '分账部分，填可支付金额',
  `open_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'openId 只有小程序支付需要',
  `kf_id` bigint NULL DEFAULT NULL COMMENT '所属客服ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1000189 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鱼币订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pay_order_gold_relation
-- ----------------------------
DROP TABLE IF EXISTS `pay_order_gold_relation`;
CREATE TABLE `pay_order_gold_relation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `serial_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流水号',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `gold_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鱼币号',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态 0 未完成 1 完成 2 部分',
  `amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '金额',
  `balance` decimal(10, 2) NULL DEFAULT NULL COMMENT '余额',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 28 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单鱼币关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pay_order_gold_serial
-- ----------------------------
DROP TABLE IF EXISTS `pay_order_gold_serial`;
CREATE TABLE `pay_order_gold_serial`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `serial_no` varchar(13) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成唯一ID（首信易只支付13位）',
  `transaction_detail_id` bigint NULL DEFAULT NULL COMMENT '流水明细ID',
  `order_gold_id` bigint NULL DEFAULT NULL COMMENT '订单鱼币关联表ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `IDX_SERIAL_NO`(`serial_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单鱼币流水表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pay_order_redo_log
-- ----------------------------
DROP TABLE IF EXISTS `pay_order_redo_log`;
CREATE TABLE `pay_order_redo_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `order_id` bigint NOT NULL DEFAULT 0 COMMENT '订单ID',
  `user_id` bigint NOT NULL DEFAULT 0 COMMENT '用户ID',
  `redo_type` tinyint NOT NULL DEFAULT 0 COMMENT '补单类型：0.优惠券补单  1.特权补单',
  `coupon_id` bigint NULL DEFAULT 0 COMMENT '优惠券ID',
  `privilege_id` bigint NULL DEFAULT 0 COMMENT '用户权益ID',
  `privilege_num` int NULL DEFAULT 1 COMMENT '权益数量',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '补单日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pay_refund_order
-- ----------------------------
DROP TABLE IF EXISTS `pay_refund_order`;
CREATE TABLE `pay_refund_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL DEFAULT 0 COMMENT '用户ID',
  `author_id` bigint NOT NULL DEFAULT 0 COMMENT '作者ID',
  `order_type` int NOT NULL COMMENT '订单类型 0.方案订单 1.订阅套餐 2.专栏套餐 3.金币订单',
  `order_id` bigint NOT NULL COMMENT '原订单ID',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原单号',
  `order_amount` decimal(10, 2) NULL DEFAULT 0.00,
  `amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
  `refund_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '历史退款单号',
  `refund_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '实退金额',
  `refund_remark` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '退款备注',
  `refund_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '手续费',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态：0：退款中 1：退款成功 2:退款失败 3 处理中',
  `result_message` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '三方支付返还说明',
  `third_party_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方退款订单号',
  `pay_app_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付渠道APPID',
  `finish_time` datetime NULL DEFAULT NULL COMMENT '退款完成时间',
  `kf_id` bigint NULL DEFAULT NULL COMMENT '所属客服id',
  `live_code_id` bigint NULL DEFAULT 0 COMMENT '渠推广码',
  `referrer` bigint NULL DEFAULT NULL COMMENT '推荐人',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL COMMENT '租户id',
  `refund_gold` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '退款鱼币',
  `refund_present_gold` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '退款赠送鱼币',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_orderId`(`order_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2291 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '退款订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for third_party_callback
-- ----------------------------
DROP TABLE IF EXISTS `third_party_callback`;
CREATE TABLE `third_party_callback`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `channel_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道ID',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `status` tinyint(1) NOT NULL COMMENT '状态 1.支付成功  2.支付失败 3 取消支付',
  `complete_date_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `result_object` json NULL COMMENT '对象JSON格式',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `creator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `updater` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者，目前使用 SysUser 的 id 编号 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1896 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '第三方数据回调表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
