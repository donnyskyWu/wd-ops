-- Rolling dashboard demo content: shift V39 seed rows into the last-7-day window at migrate time.
-- Anchor: V39 last publish date 2026-06-11 → aligns with Dashboard default dateRange (today - 6 days).

UPDATE oa_content
SET publish_time = DATE_ADD(publish_time, INTERVAL DATEDIFF(CURDATE(), DATE('2026-06-11')) DAY),
    updater = 'v129-seed-dashboard-rolling',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1
  AND creator = 'seed-dashboard'
  AND id BETWEEN 9401 AND 9414
  AND updater <> 'v129-seed-dashboard-rolling';
