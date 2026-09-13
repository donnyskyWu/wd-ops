-- shenyu-member.author_article schema drift fix (2026-09-12)
-- Symptom: OPS save → Football sync returns "系统异常"
-- Cause: member-server ArticleDO expects columns missing from older author_article dumps
-- Apply on: local shenyu-member AND beta 110.42.49.224 if sync fails with Unknown column

USE `shenyu-member`;

ALTER TABLE `author_article`
    ADD COLUMN `visible_max_amt` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '可见的最大消费金额' AFTER `visible_min_amt`;

ALTER TABLE `author_article`
    ADD COLUMN `free_preview_imgs` varchar(500) NULL COMMENT '免费预览图片' AFTER `has_free_code`;

ALTER TABLE `author_article`
    ADD COLUMN `content_preview_imgs` varchar(500) NULL COMMENT '付费预览图片' AFTER `free_preview_imgs`;
