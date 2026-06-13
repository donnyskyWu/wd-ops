-- 计划步骤支持多赛事选择（每节点 competition_ids JSON）

ALTER TABLE oa_content_plan_step
    ADD COLUMN competition_ids_json VARCHAR(2000) NULL COMMENT '步骤关联赛事 scheduleId 列表 JSON';

UPDATE oa_content_plan_step
SET competition_ids_json = CONCAT('["', competition_id, '"]')
WHERE competition_id IS NOT NULL
  AND (competition_ids_json IS NULL OR competition_ids_json = '');
