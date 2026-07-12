-- Football TenantDO extensions (not in stock ruoyi-vue-pro.sql)
-- Idempotent: adds columns only when missing.

SET @db = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@db AND table_name='system_tenant' AND column_name='org_id') = 0,
  'ALTER TABLE `system_tenant`
    ADD COLUMN `org_id` bigint NULL DEFAULT NULL COMMENT ''组织 ID'' AFTER `account_count`,
    ADD COLUMN `abbreviation` varchar(64) NULL DEFAULT NULL COMMENT ''简称'' AFTER `org_id`,
    ADD COLUMN `category` int NULL DEFAULT NULL COMMENT ''机构类型'' AFTER `abbreviation`,
    ADD COLUMN `credit_code` varchar(32) NULL DEFAULT NULL COMMENT ''统一社会信用代码'' AFTER `category`,
    ADD COLUMN `code` varchar(64) NULL DEFAULT NULL COMMENT ''编码'' AFTER `credit_code`,
    ADD COLUMN `start_date` date NULL DEFAULT NULL COMMENT ''开始日期'' AFTER `code`,
    ADD COLUMN `end_date` date NULL DEFAULT NULL COMMENT ''结束日期'' AFTER `start_date`,
    ADD COLUMN `description` varchar(512) NULL DEFAULT NULL COMMENT ''描述'' AFTER `end_date`,
    ADD COLUMN `payment_channel` varchar(128) NULL DEFAULT NULL COMMENT ''支付渠道'' AFTER `description`,
    ADD COLUMN `payment_fee_rate` decimal(10,4) NULL DEFAULT NULL COMMENT ''支付手续费率'' AFTER `payment_channel`,
    ADD COLUMN `company_name` bigint NULL DEFAULT NULL COMMENT ''公司名称(字典/ID)'' AFTER `payment_fee_rate`',
  'SELECT ''system_tenant patch already applied'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
