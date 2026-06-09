-- seed-content: SOP 模板 / 任务 / 生产内容（ID 段 9401+）

-- ========== SOP 模板 ×2 ==========
INSERT INTO oa_sop_template (id, tenant_id, template_name, content_type, platform_type, description, status, creator, updater) VALUES
(9401, 1, 'SEED-短视频生产流程', 'SHORT_VIDEO', 'DOUYIN', 'seed-content 抖音短视频 3 节点 DAG', 1, 'seed-content', 'seed-content'),
(9402, 1, 'SEED-公众号推文流程', 'ARTICLE', 'WECHAT_OFFICIAL', 'seed-content 公众号推文 3 节点 DAG', 1, 'seed-content', 'seed-content');

-- ========== SOP 节点（各 3 节点 DAG） ==========
INSERT INTO oa_sop_node (id, template_id, node_name, node_order, executor_role, need_review, reviewer_role, predecessors_json, parallel_group, sla_hours, creator, updater) VALUES
(9401, 9401, 'SEED-选题策划', 1, 'OPERATOR', 0, NULL, '[]', NULL, 24, 'seed-content', 'seed-content'),
(9402, 9401, 'SEED-拍摄剪辑', 2, 'OPERATOR', 0, NULL, '[9401]', NULL, 48, 'seed-content', 'seed-content'),
(9403, 9401, 'SEED-发布审核', 3, 'OPS_LEADER', 1, 'OPS_LEADER', '[9402]', NULL, 12, 'seed-content', 'seed-content'),
(9404, 9402, 'SEED-选题撰写', 1, 'EDITOR', 0, NULL, '[]', NULL, 24, 'seed-content', 'seed-content'),
(9405, 9402, 'SEED-排版配图', 2, 'EDITOR', 0, NULL, '[9404]', NULL, 24, 'seed-content', 'seed-content'),
(9406, 9402, 'SEED-组长审核', 3, 'OPS_LEADER', 1, 'OPS_LEADER', '[9405]', NULL, 12, 'seed-content', 'seed-content');

-- ========== 任务 ×10（多状态，关联 M1 作者 9101+ / IP组 9001+） ==========
INSERT INTO oa_task (id, tenant_id, template_id, node_id, plan_name, assignee_id, ip_group_id, author_id, status, need_review, sla_deadline, deliverables, start_time, complete_time, creator, updater) VALUES
(9411, 1, 9401, 9401, 'SEED-6月八卦计划-A', 1003, 9001, 9101, 'PENDING', 0, '2026-06-15 18:00:00', NULL, NULL, NULL, 'seed-content', 'seed-content'),
(9412, 1, 9401, 9402, 'SEED-6月八卦计划-B', 1003, 9001, 9101, 'IN_PROGRESS', 0, '2026-06-16 18:00:00', NULL, '2026-06-08 10:00:00', NULL, 'seed-content', 'seed-content'),
(9413, 1, 9401, 9403, 'SEED-6月八卦计划-C', 1003, 9001, 9102, 'PENDING_REVIEW', 1, '2026-06-17 18:00:00', '草稿链接', '2026-06-07 09:00:00', '2026-06-08 11:00:00', 'seed-content', 'seed-content'),
(9414, 1, 9401, 9403, 'SEED-6月八卦计划-D', 1002, 9001, 9102, 'DONE', 1, '2026-06-10 18:00:00', '已发布', '2026-06-05 09:00:00', '2026-06-06 15:00:00', 'seed-content', 'seed-content'),
(9415, 1, 9402, 9404, 'SEED-6月推文计划-A', 1005, 9002, 9104, 'PENDING', 0, '2026-06-18 18:00:00', NULL, NULL, NULL, 'seed-content', 'seed-content'),
(9416, 1, 9402, 9405, 'SEED-6月推文计划-B', 1005, 9002, 9104, 'IN_PROGRESS', 0, '2026-06-19 18:00:00', NULL, '2026-06-08 08:00:00', NULL, 'seed-content', 'seed-content'),
(9417, 1, 9402, 9406, 'SEED-6月推文计划-C', 1003, 9001, 9103, 'PENDING_REVIEW', 1, '2026-06-20 18:00:00', '推文初稿', '2026-06-07 14:00:00', '2026-06-08 16:00:00', 'seed-content', 'seed-content'),
(9418, 1, 9402, 9406, 'SEED-6月推文计划-D', 1003, 9001, 9103, 'DONE', 1, '2026-06-12 18:00:00', '已排版', '2026-06-04 10:00:00', '2026-06-05 12:00:00', 'seed-content', 'seed-content'),
(9419, 1, 9401, 9401, 'SEED-6月娱乐计划-E', 1002, 9002, 9104, 'PENDING', 0, '2026-06-21 18:00:00', NULL, NULL, NULL, 'seed-content', 'seed-content'),
(9420, 1, 9401, 9402, 'SEED-6月娱乐计划-F', 1002, 9002, 9105, 'IN_PROGRESS', 0, '2026-06-22 18:00:00', NULL, '2026-06-08 12:00:00', NULL, 'seed-content', 'seed-content');

-- SOP 审核记录（待审核任务）
INSERT INTO oa_sop_review (id, tenant_id, task_id, reviewer_id, reviewer_role, status, comment, creator, updater) VALUES
(9451, 1, 9413, 1002, 'OPS_LEADER', 'PENDING', NULL, 'seed-content', 'seed-content'),
(9452, 1, 9417, 1002, 'OPS_LEADER', 'PENDING', NULL, 'seed-content', 'seed-content');

-- ========== 生产内容（多状态，关联 M4 账号 9001+） ==========
INSERT INTO oa_production_content (id, tenant_id, title, body, cover_image, creator_user_id, account_id, platform_type, content_type, status, ai_generated, creator, updater) VALUES
(9431, 1, 'SEED-草稿短视频', 'seed 草稿正文', NULL, 1003, 9006, 'DOUYIN', 'SHORT_VIDEO', 'DRAFT', 0, 'seed-content', 'seed-content'),
(9432, 1, 'SEED-待初审推文', 'seed 待初审正文', NULL, 1003, 9001, 'WECHAT_OFFICIAL', 'ARTICLE', 'PENDING_FIRST_REVIEW', 0, 'seed-content', 'seed-content'),
(9433, 1, 'SEED-待复审推文', 'seed 待复审正文', NULL, 1003, 9002, 'WECHAT_OFFICIAL', 'ARTICLE', 'PENDING_SECOND_REVIEW', 0, 'seed-content', 'seed-content'),
(9434, 1, 'SEED-已发布短视频', 'seed 已发布正文', 'https://example.com/cover.jpg', 1003, 9006, 'DOUYIN', 'SHORT_VIDEO', 'PUBLISHED', 0, 'seed-content', 'seed-content'),
(9435, 1, 'SEED-已驳回推文', 'seed 驳回正文', NULL, 1003, 9003, 'WECHAT_OFFICIAL', 'ARTICLE', 'REJECTED', 0, 'seed-content', 'seed-content');

-- 知识库样本
INSERT INTO oa_knowledge_base (id, tenant_id, title, content, category, tags, is_public, status, creator, updater) VALUES
(9461, 1, 'SEED-SOP编写指南', 'seed 知识库：SOP 模板编写规范', 'TEMPLATE_LIB', 'SOP,运营', 1, 1, 'seed-content', 'seed-content');
