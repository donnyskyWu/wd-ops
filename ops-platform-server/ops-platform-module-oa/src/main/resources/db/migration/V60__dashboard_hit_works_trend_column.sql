-- 内部运营大屏：爆款预警列表补充「趋势」列（layout_json 增量更新）

UPDATE oa_dashboard
SET layout_json = REPLACE(
    layout_json,
    '"columns":[{"key":"rank","label":"排名"},{"key":"title","label":"作品标题"},{"key":"read_count","label":"阅读量"}]',
    '"columns":[{"key":"rank","label":"排名"},{"key":"title","label":"作品标题"},{"key":"read_count","label":"阅读量"},{"key":"trend_pct","label":"趋势"}]'
),
    updater = 'seed-data-screen'
WHERE id = 98601
  AND layout_json LIKE '%hit_works_24h%'
  AND layout_json NOT LIKE '%trend_pct%';
