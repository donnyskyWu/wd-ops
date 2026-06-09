-- GATE-S1 seed-assets (SEED-ASSETS-001 / SEED-ASSETS-002)
-- 固定 ID 段 9001+（tenant=1）、8001+（tenant=2），便于 SeedVerificationIT 校验

-- ========== tenant=1 公司 ×2 ==========
INSERT INTO oa_company (id, tenant_id, company_name, credit_code, industry, mp_capacity_standard, mp_registered_count, status, creator, updater)
VALUES
(9001, 1, 'SEED-种子科技A', '91110000MA0SEED001', '互联网', 20, 5, 'ENABLED', 'seed-assets', 'seed-assets'),
(9002, 1, 'SEED-种子传媒B', '91110000MA0SEED002', '传媒', 15, 3, 'ENABLED', 'seed-assets', 'seed-assets');

-- ========== tenant=1 实名人 ×5 ==========
INSERT INTO oa_realname (id, tenant_id, company_id, real_name, id_type, id_card_encrypted, phone_encrypted, gender, status, account_bound_count, creator, updater)
VALUES
(9001, 1, 9001, 'SEED-张三', 'ID_CARD', '5fob9vsaG24YRNLHpOSBRiW3YYrAa//2Av9TYkEKa3U=', 'DZbQz1jr3Ns1DyulP0v65A==', 'MALE',   'ENABLED', 2, 'seed-assets', 'seed-assets'),
(9002, 1, 9001, 'SEED-李四', 'ID_CARD', 'DtWs9c4XjFthhOUNMdWvROaSSwmdvkfmpHDNaJXKEYo=', 'pFGJ0sPnMtxOU9gy31GB2Q==', 'MALE',   'ENABLED', 2, 'seed-assets', 'seed-assets'),
(9003, 1, 9001, 'SEED-王五', 'ID_CARD', '/n67YIVxxXuTu3181Ws+RppAw+EzgReZ2qu/j1fYBn4=', 'mVavOnvOZmVKSFW5Be4RNQ==', 'MALE',   'ENABLED', 1, 'seed-assets', 'seed-assets'),
(9004, 1, 9002, 'SEED-赵六', 'ID_CARD', '26791RigxDfz7j2pnzr4lyv1oJ875/VvoX+uF/sYMPc=', 'CBbeBjcndLBxQLK+PV4KxA==', 'FEMALE', 'ENABLED', 1, 'seed-assets', 'seed-assets'),
(9005, 1, 9002, 'SEED-钱七', 'ID_CARD', 'T2OY78onBptS5XP791W5fmxEu7MiUwToaxtvE+QzPx4=', 'vVnlsER7E8Tdsk2dQOo37A==', 'FEMALE', 'ENABLED', 0, 'seed-assets', 'seed-assets');

-- ========== tenant=1 手机 ×5 ==========
INSERT INTO oa_phone (id, tenant_id, realname_id, phone_number_encrypted, phone_number_hash, phone_code, phone_model, keeper_id, status, account_bound_count, creator, updater)
VALUES
(9001, 1, 9001, 'DZbQz1jr3Ns1DyulP0v65A==', 'b0da6e942b8a0642f9cc9b50eb36dffcf183891ed237decc9efd6d6b84902116', 'SEED-PH-001', 'iPhone 15', 1001, 'ENABLED', 2, 'seed-assets', 'seed-assets'),
(9002, 1, 9002, 'pFGJ0sPnMtxOU9gy31GB2Q==', 'bb63166deebe61d290485bfe19dd2cd97a4bbbe0ed3fa62044dd595f59c0ce53', 'SEED-PH-002', 'iPhone 14', 1001, 'ENABLED', 2, 'seed-assets', 'seed-assets'),
(9003, 1, 9003, 'mVavOnvOZmVKSFW5Be4RNQ==', '161145566cebed46ad52a4ab58088495dfccbfd8881873462d1163f27ab47e0c', 'SEED-PH-003', 'Huawei P60', 1001, 'ENABLED', 1, 'seed-assets', 'seed-assets'),
(9004, 1, 9004, 'CBbeBjcndLBxQLK+PV4KxA==', 'c6379a61450e948c13460a8d5f0f656aa5cd06b2141dbee510a657bf81c135b4', 'SEED-PH-004', '小米14',   1001, 'ENABLED', 1, 'seed-assets', 'seed-assets'),
(9005, 1, 9005, 'vVnlsER7E8Tdsk2dQOo37A==', '8577eb3aa6cc35852a17fe93c50e0f3dd6ede7b133cbdec150a67bec0711c688', 'SEED-PH-005', 'OPPO Find', 1001, 'ENABLED', 0, 'seed-assets', 'seed-assets');

-- ========== tenant=1 手机卡 ×3（供选择器联调） ==========
INSERT INTO oa_sim_card (id, tenant_id, phone_id, phone_number_encrypted, phone_number_hash, is_primary, operator, assigned_user_id, iccid_encrypted, iccid_hash, package_name, status, account_bound_count, creator, updater)
VALUES
(9001, 1, 9001, 'DZbQz1jr3Ns1DyulP0v65A==', 'b0da6e942b8a0642f9cc9b50eb36dffcf183891ed237decc9efd6d6b84902116', 'YES', 'MOBILE',  1001, 'DZbQz1jr3Ns1DyulP0v65A==', 'b0da6e942b8a0642f9cc9b50eb36dffcf183891ed237decc9efd6d6b84902116', '5G畅享', 'ENABLED', 1, 'seed-assets', 'seed-assets'),
(9002, 1, 9002, 'pFGJ0sPnMtxOU9gy31GB2Q==', 'bb63166deebe61d290485bfe19dd2cd97a4bbbe0ed3fa62044dd595f59c0ce53', 'YES', 'UNICOM',  1001, 'pFGJ0sPnMtxOU9gy31GB2Q==', 'bb63166deebe61d290485bfe19dd2cd97a4bbbe0ed3fa62044dd595f59c0ce53', '冰激凌', 'ENABLED', 1, 'seed-assets', 'seed-assets'),
(9003, 1, 9003, 'mVavOnvOZmVKSFW5Be4RNQ==', '161145566cebed46ad52a4ab58088495dfccbfd8881873462d1163f27ab47e0c', 'YES', 'TELECOM', 1001, 'mVavOnvOZmVKSFW5Be4RNQ==', '161145566cebed46ad52a4ab58088495dfccbfd8881873462d1163f27ab47e0c', '星卡',   'ENABLED', 0, 'seed-assets', 'seed-assets');

-- ========== tenant=1 平台账号 ×10 ==========
INSERT INTO oa_account (id, tenant_id, platform_type, account_type, account_name, external_account_id, company_id, realname_id, phone_id, sim_card_id, phone_number_hash, status, creator, updater)
VALUES
(9001, 1, 'WECHAT_OFFICIAL', 'OFFICIAL_ACCOUNT', 'SEED-公众号A1', 'seed_mp_a1', 9001, 9001, 9001, 9001, 'b0da6e942b8a0642f9cc9b50eb36dffcf183891ed237decc9efd6d6b84902116', 'NORMAL', 'seed-assets', 'seed-assets'),
(9002, 1, 'WECHAT_OFFICIAL', 'OFFICIAL_ACCOUNT', 'SEED-公众号A2', 'seed_mp_a2', 9001, 9002, 9002, 9002, 'bb63166deebe61d290485bfe19dd2cd97a4bbbe0ed3fa62044dd595f59c0ce53', 'NORMAL', 'seed-assets', 'seed-assets'),
(9003, 1, 'WECHAT_OFFICIAL', 'SERVICE_ACCOUNT',  'SEED-服务号A3', 'seed_mp_a3', 9001, 9003, 9003, NULL, '161145566cebed46ad52a4ab58088495dfccbfd8881873462d1163f27ab47e0c', 'NORMAL', 'seed-assets', 'seed-assets'),
(9004, 1, 'WECHAT_VIDEO',    'PERSONAL_ACCOUNT', 'SEED-视频号B1', 'seed_v_b1',  9002, 9004, 9004, NULL, 'c6379a61450e948c13460a8d5f0f656aa5cd06b2141dbee510a657bf81c135b4', 'NORMAL', 'seed-assets', 'seed-assets'),
(9005, 1, 'WECHAT_VIDEO',    'PERSONAL_ACCOUNT', 'SEED-视频号B2', 'seed_v_b2',  9002, 9004, 9004, NULL, 'c6379a61450e948c13460a8d5f0f656aa5cd06b2141dbee510a657bf81c135b4', 'NORMAL', 'seed-assets', 'seed-assets'),
(9006, 1, 'DOUYIN',          'PERSONAL_ACCOUNT', 'SEED-抖音号1',  'seed_dy_1',  9001, 9001, 9001, NULL, 'b0da6e942b8a0642f9cc9b50eb36dffcf183891ed237decc9efd6d6b84902116', 'NORMAL', 'seed-assets', 'seed-assets'),
(9007, 1, 'DOUYIN',          'PERSONAL_ACCOUNT', 'SEED-抖音号2',  'seed_dy_2',  9001, 9002, 9002, NULL, 'bb63166deebe61d290485bfe19dd2cd97a4bbbe0ed3fa62044dd595f59c0ce53', 'NORMAL', 'seed-assets', 'seed-assets'),
(9008, 1, 'KUAISHOU',        'PERSONAL_ACCOUNT', 'SEED-快手号1',  'seed_ks_1',  9002, 9003, 9003, NULL, '161145566cebed46ad52a4ab58088495dfccbfd8881873462d1163f27ab47e0c', 'NORMAL', 'seed-assets', 'seed-assets'),
(9009, 1, 'XIAOHONGSHU',     'PERSONAL_ACCOUNT', 'SEED-小红书1',  'seed_xhs1',  9002, 9005, 9005, NULL, '8577eb3aa6cc35852a17fe93c50e0f3dd6ede7b133cbdec150a67bec0711c688', 'NORMAL', 'seed-assets', 'seed-assets'),
(9010, 1, 'WECHAT_OFFICIAL', 'OFFICIAL_ACCOUNT', 'SEED-公众号B1', 'seed_mp_b1', 9002, 9005, 9005, NULL, '8577eb3aa6cc35852a17fe93c50e0f3dd6ede7b133cbdec150a67bec0711c688', 'NORMAL', 'seed-assets', 'seed-assets');

-- ========== tenant=1 个微/企微样本（S-08 联调） ==========
INSERT INTO oa_personal_wechat_account (id, tenant_id, account_name, wechat_id, phone_id, status, creator, updater)
VALUES (9001, 1, 'SEED-个微张三', 'seed_wx_zhangsan', 9001, 'ENABLED', 'seed-assets', 'seed-assets');

INSERT INTO oa_wework_account (id, tenant_id, account_name, corp_id, agent_id, secret_encrypted, status, creator, updater)
VALUES (9001, 1, 'SEED-企微A', 'seed_corp_a', 'seed_agent_a', 'DZbQz1jr3Ns1DyulP0v65A==', 'ENABLED', 'seed-assets', 'seed-assets');

-- ========== tenant=2 隔离样本（SEED-ASSETS-002） ==========
INSERT INTO oa_company (id, tenant_id, company_name, credit_code, industry, mp_capacity_standard, mp_registered_count, status, creator, updater)
VALUES (8001, 2, 'SEED-T2-隔离公司', '91110000MA0SEEDT02', '测试', 5, 0, 'ENABLED', 'seed-assets', 'seed-assets');

INSERT INTO oa_realname (id, tenant_id, company_id, real_name, id_type, id_card_encrypted, phone_encrypted, status, account_bound_count, creator, updater)
VALUES (8001, 2, 8001, 'SEED-T2-王隔离', 'ID_CARD', '5fob9vsaG24YRNLHpOSBRiW3YYrAa//2Av9TYkEKa3U=', 'pRcCZ/+LToO3hYsGZEECUg==', 'ENABLED', 1, 'seed-assets', 'seed-assets');

INSERT INTO oa_phone (id, tenant_id, realname_id, phone_number_encrypted, phone_number_hash, phone_code, phone_model, keeper_id, status, account_bound_count, creator, updater)
VALUES (8001, 2, 8001, 'pRcCZ/+LToO3hYsGZEECUg==', 'b3864671715334e2733029e0f934e3d6065ea2fa7a29dc7362289fb09398325d', 'SEED-T2-PH', '隔离测试机', 2001, 'ENABLED', 1, 'seed-assets', 'seed-assets');

INSERT INTO oa_account (id, tenant_id, platform_type, account_type, account_name, external_account_id, company_id, realname_id, phone_id, phone_number_hash, status, creator, updater)
VALUES (8001, 2, 'WECHAT_OFFICIAL', 'OFFICIAL_ACCOUNT', 'SEED-T2-公众号', 'seed_t2_mp1', 8001, 8001, 8001, 'b3864671715334e2733029e0f934e3d6065ea2fa7a29dc7362289fb09398325d', 'NORMAL', 'seed-assets', 'seed-assets');
