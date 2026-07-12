-- Football pay module tables for wd integration (ADR-049 P2b)
-- Source: football-backend-saas/football-module-pay
--   AllOrderDO  -> pay_all_order
--   GoldOrderDO -> pay_gold_order
-- No DDL in sql/mysql/ruoyi-vue-pro.sql — schema derived from DO + BaseDO/TenantBaseDO.
-- Idempotent: CREATE TABLE IF NOT EXISTS. Does NOT touch sys_* / oa_* tables.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `pay_all_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '订单号',
  `user_id` bigint NOT NULL DEFAULT 0 COMMENT '用户id',
  `article_id` bigint NOT NULL DEFAULT 0 COMMENT '文章id',
  `privilege_id` bigint NOT NULL DEFAULT 0 COMMENT '父套餐id',
  `son_privilege_id` bigint NOT NULL DEFAULT 0 COMMENT '子套餐id',
  `author_id` bigint NOT NULL DEFAULT 0 COMMENT '作者id',
  `order_source` int NOT NULL DEFAULT 1 COMMENT '订单来源 1.H5 2.APP',
  `order_type` int NULL DEFAULT NULL COMMENT '订单类型 0.文章订单 1.订阅套餐 2.专栏套餐',
  `match_type` int NULL DEFAULT NULL COMMENT '比赛类型',
  `amount` decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '订单金额',
  `random_reduce_amount` decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '随机立减金额',
  `pay_amount` decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '支付金额',
  `coupon_amount` decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '优惠金额',
  `pay_gold` decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '支付鱼币',
  `give_gold` decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '赠送鱼币支付',
  `author_divide` decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '作者鱼币分成',
  `author_gold_divide` decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '作者鱼币分成',
  `sharer_divide` decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '分享者分成',
  `partner_divide` decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '合伙人分成',
  `pay_type` int NULL DEFAULT NULL COMMENT '支付方式',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `status` int NOT NULL DEFAULT 0 COMMENT '支付状态 0待支付 1成功 2失败 3取消',
  `is_refund` int NOT NULL DEFAULT 0 COMMENT '是否退款 1是 0否',
  `refund_time` datetime NULL DEFAULT NULL COMMENT '退款时间',
  `third_party_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '第三方订单号',
  `channel_order_sn` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通道订单号',
  `ins_order_sn` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '机构订单号',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `from_article_id` bigint NOT NULL DEFAULT 0 COMMENT '来源文章id',
  `pay_app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '支付渠道app_id',
  `user_privilege_id` bigint NOT NULL DEFAULT 0 COMMENT '用户授权套餐id',
  `pay_app_type` int NULL DEFAULT NULL COMMENT '支付渠道类型',
  `real_divide_amount` decimal(20, 4) NULL DEFAULT NULL COMMENT '实际分成总金额',
  `is_activity_order` int NOT NULL DEFAULT 0 COMMENT '活动订单 0否 1是',
  `is_first_order` int NOT NULL DEFAULT 0 COMMENT '是否首单',
  `is_redo` int NOT NULL DEFAULT 0 COMMENT '是否补单',
  `referrer` bigint NOT NULL DEFAULT 0 COMMENT '推荐人ID',
  `referrer_type` int NULL DEFAULT NULL COMMENT '推荐人类型',
  `senior_partner_id` bigint NOT NULL DEFAULT 0 COMMENT '上级合伙人ID',
  `trace_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '扫码追踪参数',
  `live_code_id` bigint NULL DEFAULT NULL COMMENT '活码渠道ID',
  `open_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'openId',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_pay_all_order_tenant_create` (`tenant_id`, `create_time`) USING BTREE,
  KEY `idx_pay_all_order_tenant_author` (`tenant_id`, `author_id`) USING BTREE,
  KEY `idx_pay_all_order_order_no` (`order_no`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Football 业务订单';

CREATE TABLE IF NOT EXISTS `pay_gold_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '订单号',
  `third_party_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '第三方订单号',
  `user_id` bigint NOT NULL DEFAULT 0 COMMENT '用户id',
  `amount` decimal(20, 4) NULL DEFAULT NULL COMMENT '订单金额',
  `pay_amount` decimal(20, 4) NULL DEFAULT NULL COMMENT '支付金额',
  `commission` decimal(20, 4) NULL DEFAULT NULL COMMENT '手续费',
  `gold_num` decimal(20, 4) NULL DEFAULT NULL COMMENT '鱼币数',
  `pay_type` int NULL DEFAULT NULL COMMENT '支付方式',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `status` int NOT NULL DEFAULT 0 COMMENT '支付状态',
  `client_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '客户端IP',
  `channel_order_sn` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通道订单号',
  `ins_order_sn` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '机构订单号',
  `present_gold` decimal(20, 4) NULL DEFAULT NULL COMMENT '充值赠送鱼币',
  `pay_app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '支付渠道app_id',
  `pay_app_type` int NULL DEFAULT NULL COMMENT '支付渠道类型',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `order_source` int NULL DEFAULT NULL COMMENT '订单来源',
  `is_refund` int NOT NULL DEFAULT 0 COMMENT '是否退款',
  `refund_time` datetime NULL DEFAULT NULL COMMENT '退款时间',
  `is_activity_order` int NOT NULL DEFAULT 0 COMMENT '活动订单',
  `apple_receipt` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '苹果支付票据',
  `apple_product_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '苹果商品ID',
  `apple_transaction_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '苹果交易ID',
  `split_status` int NULL DEFAULT NULL COMMENT '分账状态',
  `split_amount` decimal(20, 4) NULL DEFAULT NULL COMMENT '分账金额',
  `open_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'openId',
  `kf_id` bigint NULL DEFAULT NULL COMMENT '客服id',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_pay_gold_order_tenant_create` (`tenant_id`, `create_time`) USING BTREE,
  KEY `idx_pay_gold_order_order_no` (`order_no`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Football 鱼币充值订单';

-- Minimal seed for P2b curl smoke (tenant_id=1, deleted=0, create_time in probe range)
INSERT INTO `pay_all_order` (
  `id`, `order_no`, `user_id`, `author_id`, `order_type`, `amount`, `pay_amount`,
  `status`, `pay_time`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  900001, 'P2B-SEED-001', 10001, 20001, 0, 99.0000, 88.0000,
  1, '2026-03-15 10:00:00', 1, 'integration-seed', '2026-03-15 10:00:00', 'integration-seed', '2026-03-15 10:00:00', b'0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `pay_all_order` WHERE `id` = 900001);

INSERT INTO `pay_all_order` (
  `id`, `order_no`, `user_id`, `author_id`, `order_type`, `amount`, `pay_amount`,
  `status`, `pay_time`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  900002, 'P2B-SEED-002', 10002, 20001, 1, 199.0000, 199.0000,
  1, '2026-04-01 12:30:00', 1, 'integration-seed', '2026-04-01 12:30:00', 'integration-seed', '2026-04-01 12:30:00', b'0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `pay_all_order` WHERE `id` = 900002);

INSERT INTO `pay_gold_order` (
  `id`, `order_no`, `user_id`, `amount`, `pay_amount`, `gold_num`, `status`, `pay_time`,
  `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  800001, 'GOLD-P2B-001', 10001, 50.0000, 50.0000, 500.0000, 1, '2026-03-20 09:00:00',
  1, 'integration-seed', '2026-03-20 09:00:00', 'integration-seed', '2026-03-20 09:00:00', b'0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `pay_gold_order` WHERE `id` = 800001);

SET FOREIGN_KEY_CHECKS = 1;
