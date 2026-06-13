-- ADR-016 §2.2: 计划步骤分配赛事 + 任务继承

ALTER TABLE oa_content_plan_step ADD COLUMN competition_id VARCHAR(64) NULL COMMENT '外部赛事 scheduleId（计划赛事池）';
ALTER TABLE oa_content_plan_step ADD COLUMN competition_name VARCHAR(200) NULL COMMENT '赛事展示名快照';
ALTER TABLE oa_task ADD COLUMN competition_id VARCHAR(64) NULL COMMENT '继承计划步骤赛事';
