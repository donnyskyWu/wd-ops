-- =============================================================================
-- Ops DB (shenyu-ops) — AI prompt seed WORK_TASK_WIN_PREDICTION (ADR-072 D2)
-- Source: V181__m2_work_task_foundation.sql (§3)
-- Version: 2026-08-19
-- =============================================================================
SET NAMES utf8mb4;

INSERT INTO oa_ai_prompt_config
  (tenant_id, template_name, version, scene, content_type, prompt_content, variable_desc, temperature, status, remark)
SELECT 1,
  '工作任务红黑预测抽取', 'v1', 'WORK_TASK_WIN_PREDICTION', 'ARTICLE',
'你是一位专业的足球赛果分析助手。请从以下任务正文中**抽取且仅抽取一条**全场胜负预测 outcome。

【赛事】{{match_name}}（competition_id={{competition_id}}）
【正文】
{{content_body}}

输出要求：
1. 仅输出一个 outcome 枚举值：HOME_WIN（主胜）/ DRAW（平局）/ AWAY_WIN（客胜）
2. 若正文无法判断明确单场预测，输出 UNKNOWN
3. 不要输出解释、标点或其他文字',
'{{match_name}}=赛事名称; {{competition_id}}=赛事ID; {{content_body}}=任务关联正文',
0.20, 'ENABLED', 'FR-M2-010 S-16 · ADR-072 赛后 Job 抽取预测'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_ai_prompt_config
  WHERE tenant_id = 1 AND scene = 'WORK_TASK_WIN_PREDICTION' AND deleted = 0
);
