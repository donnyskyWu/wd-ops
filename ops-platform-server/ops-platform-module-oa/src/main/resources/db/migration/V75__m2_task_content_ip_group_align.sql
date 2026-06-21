-- 任务关联内容 ip_group_id 与 oa_task 对齐（修复历史错误写入 / getMyIpGroups 回退）

UPDATE oa_production_content c
SET c.ip_group_id = (
        SELECT t.ip_group_id
        FROM oa_task t
        WHERE t.id = c.task_id
          AND t.tenant_id = c.tenant_id
          AND t.deleted = 0
          AND t.ip_group_id IS NOT NULL
        LIMIT 1
    ),
    c.updater = 'migration-v75',
    c.update_time = NOW()
WHERE c.deleted = 0
  AND c.task_id IS NOT NULL
  AND EXISTS (
        SELECT 1
        FROM oa_task t
        WHERE t.id = c.task_id
          AND t.tenant_id = c.tenant_id
          AND t.deleted = 0
          AND t.ip_group_id IS NOT NULL
          AND (c.ip_group_id IS NULL OR c.ip_group_id <> t.ip_group_id)
    );
