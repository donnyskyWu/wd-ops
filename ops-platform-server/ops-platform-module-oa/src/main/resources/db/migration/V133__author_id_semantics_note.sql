-- V133: §23 author_id SSOT reminder (ADR-051) — comment-only, idempotent
-- oa_content / oa_production_content / oa_task / oa_order_attribution.author_id -> member.author_user.id

ALTER TABLE oa_production_content
    MODIFY COLUMN author_id BIGINT NULL COMMENT '-> shenyu-member.author_user.id (ADR-050/051 §23)';
