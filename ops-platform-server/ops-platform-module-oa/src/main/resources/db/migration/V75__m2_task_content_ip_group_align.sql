-- 任务关联内容 ip_group_id 与 oa_task 对齐（修复历史错误写入 / getMyIpGroups 回退）
UPDATE oa_production_content c
    INNER JOIN oa_task t
        ON t.id = c.task_id AND t.tenant_id = c.tenant_id AND t.deleted = 0
SET c.ip_group_id = t.ip_group_id,
    c.updater     = 'migration-v75',
    c.update_time = NOW()
WHERE c.deleted = 0
  AND c.task_id IS NOT NULL
  AND t.ip_group_id IS NOT NULL
  AND (c.ip_group_id IS NULL OR c.ip_group_id <> t.ip_group_id);
