-- 内容关联赛事展示名快照（MatchSelectDialog 选择后写入）
ALTER TABLE oa_production_content
    ADD COLUMN competition_name VARCHAR(200) NULL COMMENT '赛事展示名快照' AFTER competition_id;
