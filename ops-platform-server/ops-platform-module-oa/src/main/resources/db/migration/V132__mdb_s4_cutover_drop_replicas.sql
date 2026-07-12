-- V132: S4 cutover — DROP deprecated oa_author + wd Football replica tables (ADR-050/051)
-- oa-server reads member/mp/pay/system via @DS; these wd copies are obsolete on localhost.
-- NOTE: system_* tables in remote wd remain for system-server until remote cutover (see GATE-MDB-S4).

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS oa_author;

DROP TABLE IF EXISTS author_channel_sales;
DROP TABLE IF EXISTS author_user;
DROP TABLE IF EXISTS pay_gold_order;
DROP TABLE IF EXISTS pay_all_order;

SET FOREIGN_KEY_CHECKS = 1;
