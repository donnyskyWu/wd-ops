-- Restore system_users.nickname corrupted by non-utf8 import (literal '?' stored as 0x3F).
-- SSOT: shenyu-system.system_users (ADR-056). Values aligned with import-football-system-tables.sql.
-- Idempotent: only updates rows whose nickname is all question marks.

SET NAMES utf8mb4;

UPDATE system_users SET nickname = '测试号' WHERE username = 'test' AND nickname REGEXP '^[?？]+$';
UPDATE system_users SET nickname = '测试号02' WHERE username = 'admin123' AND nickname REGEXP '^[?？]';
UPDATE system_users SET nickname = '狗蛋' WHERE username = 'goudan' AND nickname REGEXP '^[?？]+$';
UPDATE system_users SET nickname = '小秃头' WHERE username = 'wwbwwb' AND nickname REGEXP '^[?？]+$';
UPDATE system_users SET nickname = '芋道源码' WHERE username = 'admin' AND nickname REGEXP '^[?？]+$';
UPDATE system_users SET nickname = '张武' WHERE username = 'zhangwu' AND (nickname REGEXP '^[?？]+$' OR nickname = 'zw');
