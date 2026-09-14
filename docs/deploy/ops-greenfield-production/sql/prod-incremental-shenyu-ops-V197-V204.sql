-- =============================================================================
-- Production incremental deploy: shenyu-ops database
--
-- Purpose:
--   Apply OPS schema + seed changes for 9/7+ release (S-21a AI 排版 / ADR-028).
--   Flyway SSOT: football-module-ops-server/src/main/resources/db/migration/
--
-- Versions included (execution order):
--   V197 — FR-M2-005: oa_wechat_layout_template.style_css → LONGTEXT（MHTML 大 CSS）
--   V198 — S-21a: 决策扫读版 + 情报分析版 PRESET + AI_TYPESET_SEMANTIC prompt
--   V199 — S-21a: footballTemplate / maxWidth enrich for built-in AI presets
--   V200 — S-21a: paragraph text-indent + styleHints prompt append
--   V201 — S-21a: decision-scan marketing paragraph spacing + image placeholder prompt
--   V202 — S-21a: CTA_BANNER / HIGHLIGHT_LIST marketing segment types prompt
--   V203 — S-21a: full AI_TYPESET_SEMANTIC prompt rewrite (rich components SSOT)
--   V204 — S-21a: 竞彩营销版 + 简洁通读版 quick typeset PRESET
--
-- Pre-requisite:
--   Production shenyu-ops baseline at V196（V192–V196 已执行，见 prod-incremental-shenyu-ops-V192-V196.sql）。
--   若 V192–V196 尚未执行，请先跑 9/7 前批次脚本（见 README-prod-incremental-20260914.md）。
--
-- Companion script:
--   无 shenyu-system 跨库 DML（V197–V204 全部在 shenyu-ops 内）。
--
-- Date: 2026-09-14
--
-- Execution:
--   mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < prod-incremental-shenyu-ops-V197-V204.sql
--
-- Idempotency (best effort, no Flyway check required):
--   V197 — 检测 information_schema 列类型，已为 LONGTEXT 则跳过 ALTER
--   V198/V204 — INSERT ... WHERE NOT EXISTS
--   V199 — UPDATE 重复执行结果一致
--   V200/V201/V202 — UPDATE 带 NOT LIKE 守卫，已追加则跳过
--   V203 — 全量 REPLACE prompt，重复执行结果一致
--   若某段已手工应用，可整段跳过；重复执行不应破坏数据。
--
-- Verify after run (expect all OK):
--   SELECT DATA_TYPE FROM information_schema.COLUMNS
--     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_wechat_layout_template' AND COLUMN_NAME='style_css';
--   -- expect: longtext
--
--   SELECT template_name, tags, status FROM oa_wechat_layout_template
--     WHERE tenant_id=1 AND source_type='PRESET' AND tags LIKE '%football-ai:%' AND deleted=0
--     ORDER BY template_name;
--   -- expect 4 rows: 决策扫读版, 情报分析版, 竞彩营销版, 简洁通读版
--
--   SELECT template_name, scene, status FROM oa_ai_prompt_config
--     WHERE tenant_id=1 AND scene='AI_TYPESET_SEMANTIC' AND deleted=0;
--   -- expect 1 row ENABLED, prompt_content 含 segmentType 白名单与 STAT_BAR
-- =============================================================================

-- ---------------------------------------------------------------------------
-- V197: FR-M2-005 — style_css LONGTEXT for MHTML bundled CSS
-- Skip if already applied (column type already longtext).
-- ---------------------------------------------------------------------------
SET NAMES utf8mb4;

SET @needs_v197 := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_wechat_layout_template'
      AND COLUMN_NAME = 'style_css'
      AND DATA_TYPE <> 'longtext'
);

SET @sql_v197 := IF(@needs_v197 > 0,
    'ALTER TABLE oa_wechat_layout_template MODIFY COLUMN style_css LONGTEXT NULL COMMENT ''Imported template CSS, including MHTML bundled stylesheets''',
    'SELECT ''V197 skip: style_css already LONGTEXT'' AS notice'
);

PREPARE stmt_v197 FROM @sql_v197;
EXECUTE stmt_v197;
DEALLOCATE PREPARE stmt_v197;

-- ---------------------------------------------------------------------------
-- V198: S-21a / ADR-028 — tenant-safe AI semantic typeset presets and M8 prompt
-- ---------------------------------------------------------------------------
SET NAMES utf8mb4;

SET @decision_styles = '{"heading1":{"fontSize":"22px","fontWeight":"700","color":"#1a2a4a","lineHeight":"1.35","borderLeft":"4px solid #c0392b","paddingLeft":"14px"},"heading2":{"fontSize":"16px","fontWeight":"700","color":"#1a2a4a","lineHeight":"1.4"},"heading3":{"fontSize":"15px","fontWeight":"700","color":"#1a2a4a","lineHeight":"1.4"},"paragraph":{"fontSize":"16px","color":"#2a2a3e","lineHeight":"1.95","marginBottom":"10px"},"quote":{"fontSize":"15px","color":"#5a5a72","borderLeft":"2px solid #c0392b","paddingLeft":"16px","lineHeight":"2"},"list":{"fontSize":"15px","color":"#2a2a3e","lineHeight":"2","border":"1.5px solid #c0392b","backgroundColor":"#fdf6f6","padding":"18px 16px"},"image":{"fontSize":"14px","color":"#6a7a96"}}';
SET @analysis_styles = '{"heading1":{"fontSize":"24px","fontWeight":"700","color":"#ffffff","lineHeight":"1.4","backgroundColor":"#1a1a2e","padding":"30px"},"heading2":{"fontSize":"18px","fontWeight":"700","color":"#1a1a2e","lineHeight":"1.4","borderLeft":"4px solid #e94560","paddingLeft":"12px"},"heading3":{"fontSize":"16px","fontWeight":"700","color":"#1a1a2e","lineHeight":"1.4"},"paragraph":{"fontSize":"15px","color":"#444444","lineHeight":"1.8","marginBottom":"12px"},"quote":{"fontSize":"15px","color":"#444444","backgroundColor":"#fff5f5","borderLeft":"4px solid #e94560","padding":"16px"},"list":{"fontSize":"15px","color":"#ffffff","lineHeight":"1.8","backgroundColor":"#1a1a2e","padding":"20px"},"image":{"fontSize":"14px","color":"#888888"}}';

INSERT INTO oa_wechat_layout_template
(tenant_id, template_name, description, content_type, document_type, layout_json, layout_schema,
 schema_version, layout_html, preview_html, source_type, status, creator_user_id, tags, default_params,
 creator, updater)
SELECT 1, '决策扫读版', 'S-21a 内置 AI 排版模板：快速定位比赛、对阵与推荐', 'ARTICLE', NULL,
       '{"version":2,"blocks":[]}',
       JSON_OBJECT('version', 2, 'footballTemplate', 'decision-scan', 'maxWidth', '620px', 'globalStyles', CAST(@decision_styles AS JSON), 'blocks', JSON_ARRAY(
         JSON_OBJECT('id','decision-title','type','heading','level',1,'styleRef','heading1','slotKind','heading'),
         JSON_OBJECT('id','decision-match','type','heading','level',2,'styleRef','heading2','slotKind','heading','repeat',true),
         JSON_OBJECT('id','decision-subheading','type','slot','slotKind','heading','level',3,'styleRef','heading3','repeat',true),
         JSON_OBJECT('id','decision-recommendation','type','slot','slotKind','list','styleRef','list','repeat',true),
         JSON_OBJECT('id','decision-quote','type','slot','slotKind','quote','styleRef','quote','repeat',true),
         JSON_OBJECT('id','decision-paragraph-repeat','type','slot','slotKind','paragraph','styleRef','paragraph','repeat',true)
       )),
       2, NULL, NULL, 'PRESET', 'ENABLED', 1, 'football-ai:decision-scan',
       '{"maxWidth":"620px","paragraph_font_size":16,"paragraph_line_height":1.95}',
       'system', 'system'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_wechat_layout_template
  WHERE tenant_id=1 AND source_type='PRESET' AND tags LIKE '%football-ai:decision-scan%' AND deleted=0
);

INSERT INTO oa_wechat_layout_template
(tenant_id, template_name, description, content_type, document_type, layout_json, layout_schema,
 schema_version, layout_html, preview_html, source_type, status, creator_user_id, tags, default_params,
 creator, updater)
SELECT 1, '情报分析版', 'S-21a 内置 AI 排版模板：多维数据、战术与赔率深度报告', 'ARTICLE', NULL,
       '{"version":2,"blocks":[]}',
       JSON_OBJECT('version', 2, 'footballTemplate', 'analysis-report', 'maxWidth', '720px', 'globalStyles', CAST(@analysis_styles AS JSON), 'blocks', JSON_ARRAY(
         JSON_OBJECT('id','analysis-title','type','heading','level',1,'styleRef','heading1','slotKind','heading'),
         JSON_OBJECT('id','analysis-section','type','heading','level',2,'styleRef','heading2','slotKind','heading','repeat',true),
         JSON_OBJECT('id','analysis-subheading','type','slot','slotKind','heading','level',3,'styleRef','heading3','repeat',true),
         JSON_OBJECT('id','analysis-quote','type','slot','slotKind','quote','styleRef','quote','repeat',true),
         JSON_OBJECT('id','analysis-recommendation','type','slot','slotKind','list','styleRef','list','repeat',true),
         JSON_OBJECT('id','analysis-paragraph-repeat','type','slot','slotKind','paragraph','styleRef','paragraph','repeat',true)
       )),
       2, NULL, NULL, 'PRESET', 'ENABLED', 1, 'football-ai:analysis-report',
       '{"maxWidth":"720px","paragraph_font_size":15,"paragraph_line_height":1.8}',
       'system', 'system'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_wechat_layout_template
  WHERE tenant_id=1 AND source_type='PRESET' AND tags LIKE '%football-ai:analysis-report%' AND deleted=0
);

INSERT INTO oa_ai_prompt_config
(tenant_id, template_name, version, scene, content_type, document_type, prompt_content,
 variable_desc, temperature, status, remark)
SELECT 1, 'AI排版语义分段', 'v1', 'AI_TYPESET_SEMANTIC', 'ARTICLE', NULL,
'将正文解析为 JSON SemanticSegment 数组。
仅允许 segmentType：ARTICLE_TITLE、MATCH_HEADER、TEAM_VS、MATCH_TIME、MATCH_METADATA、SUBHEADING、ANALYSIS_PARAGRAPH、RECOMMENDATION_LIST、IMAGE_PLACEHOLDER、DIVIDER、DISCLAIMER、QUOTE、ORDERED_LIST、DECORATIVE_SLOGAN、PLAIN_PARAGRAPH。
index 必须从 0 连续递增。每个 text 必须是原文连续子串，所有 text 按顺序直接拼接后必须与原文逐字符相同。不得增删、改写、纠错、重排或输出 HTML。推荐三行合并为一个 RECOMMENDATION_LIST，但 text 必须保留原换行。只输出 JSON 数组。',
'输入由服务端追加 layout_schema 摘要与正文；输出 SemanticSegment[]',
0.10, 'ENABLED', 'S-21a · ADR-028 AI 语义排版'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_ai_prompt_config
  WHERE tenant_id=1 AND scene='AI_TYPESET_SEMANTIC' AND deleted=0
);

-- ---------------------------------------------------------------------------
-- V199: S-21a — footballTemplate tag enrich for built-in AI presets
-- ---------------------------------------------------------------------------
SET NAMES utf8mb4;

UPDATE oa_wechat_layout_template
SET layout_schema = JSON_SET(
        layout_schema,
        '$.footballTemplate', 'decision-scan',
        '$.maxWidth', '620px'),
    default_params = JSON_SET(
        COALESCE(default_params, JSON_OBJECT()),
        '$.maxWidth', '620px',
        '$.paragraph_font_size', 16,
        '$.paragraph_line_height', 1.95),
    updater = 'system'
WHERE tenant_id = 1
  AND source_type = 'PRESET'
  AND tags LIKE '%football-ai:decision-scan%'
  AND deleted = 0;

UPDATE oa_wechat_layout_template
SET layout_schema = JSON_SET(
        layout_schema,
        '$.footballTemplate', 'analysis-report',
        '$.maxWidth', '720px'),
    default_params = JSON_SET(
        COALESCE(default_params, JSON_OBJECT()),
        '$.maxWidth', '720px',
        '$.paragraph_font_size', 15,
        '$.paragraph_line_height', 1.8),
    updater = 'system'
WHERE tenant_id = 1
  AND source_type = 'PRESET'
  AND tags LIKE '%football-ai:analysis-report%'
  AND deleted = 0;

-- ---------------------------------------------------------------------------
-- V200: S-21a — paragraph first-line indent + styleHints prompt append
-- Skip prompt append if styleHints already present.
-- ---------------------------------------------------------------------------
SET NAMES utf8mb4;

UPDATE oa_wechat_layout_template
SET layout_schema = JSON_SET(
        layout_schema,
        '$.globalStyles.paragraph.textIndent', '2em'),
    default_params = JSON_SET(
        COALESCE(default_params, JSON_OBJECT()),
        '$.paragraph_text_indent', '2em'),
    updater = 'system'
WHERE tenant_id = 1
  AND source_type = 'PRESET'
  AND tags LIKE '%football-ai:%'
  AND deleted = 0;

UPDATE oa_ai_prompt_config
SET prompt_content = CONCAT(
        prompt_content,
        '\n\n可选 styleHints：仅用于 ANALYSIS_PARAGRAPH/PLAIN_PARAGRAPH；'
        '字段 start/end/hintType(TEAM_NAME|SCORE|EMPHASIS|KEYWORD|CONCLUSION)/'
        'renderAs(bold+accent|bold+primary|accent+bg)；'
        '标注队名、比分、赔率、推荐/结论等关键词，不得改变 text 拼接结果。'),
    updater = 'system'
WHERE tenant_id = 1
  AND scene = 'AI_TYPESET_SEMANTIC'
  AND deleted = 0
  AND prompt_content NOT LIKE '%styleHints%';

-- ---------------------------------------------------------------------------
-- V201: S-21a — decision-scan marketing paragraph spacing
-- Skip prompt append if metadata.imageUrl already documented.
-- ---------------------------------------------------------------------------
SET NAMES utf8mb4;

UPDATE oa_wechat_layout_template
SET layout_schema = JSON_SET(
        layout_schema,
        '$.globalStyles.paragraph.textIndent', '0',
        '$.globalStyles.paragraph.marginBottom', '16px'),
    default_params = JSON_SET(
        COALESCE(default_params, JSON_OBJECT()),
        '$.paragraph_text_indent', '0',
        '$.paragraph_margin_bottom', 16),
    updater = 'system'
WHERE tenant_id = 1
  AND source_type = 'PRESET'
  AND tags LIKE '%football-ai:decision-scan%'
  AND deleted = 0;

UPDATE oa_ai_prompt_config
SET prompt_content = CONCAT(
        prompt_content,
        '\n\n决策扫读版段落：不使用首行缩进（text-indent:0），段间距约 16px。'
        'IMAGE_PLACEHOLDER/IMAGE 若 metadata.imageUrl 或 text 含 http(s) 图片地址则渲染全宽推广图，'
        '否则保留灰色占位文案。'),
    updater = 'system'
WHERE tenant_id = 1
  AND scene = 'AI_TYPESET_SEMANTIC'
  AND deleted = 0
  AND prompt_content NOT LIKE '%metadata.imageUrl%';

-- ---------------------------------------------------------------------------
-- V202: S-21a — CTA_BANNER / HIGHLIGHT_LIST marketing segment types
-- Skip if CTA_BANNER already in prompt.
-- ---------------------------------------------------------------------------
SET NAMES utf8mb4;

UPDATE oa_ai_prompt_config
SET prompt_content = CONCAT(
        prompt_content,
        '\n\n决策扫读版营销组件（可选）：'
        'CTA_BANNER=短促行动号召行（如含点击/立即/跟紧），可选 metadata.emphasisPhrase 标注金色尾句；'
        'HIGHLIGHT_LIST=要点列表，text 多行或 metadata.items[]；'
        'HIGHLIGHT 可带 metadata.variant=CTA_BANNER 或 metadata.items[] 触发同等渲染。'
        '不得增删改正文字符。'),
    updater = 'system'
WHERE tenant_id = 1
  AND scene = 'AI_TYPESET_SEMANTIC'
  AND deleted = 0
  AND prompt_content NOT LIKE '%CTA_BANNER%';

-- ---------------------------------------------------------------------------
-- V203: S-21a — full AI_TYPESET_SEMANTIC prompt rewrite (rich components SSOT)
-- Idempotent: replaces entire prompt_content to target version each run.
-- ---------------------------------------------------------------------------
SET NAMES utf8mb4;

UPDATE oa_ai_prompt_config
SET prompt_content = '将正文解析为 JSON SemanticSegment 数组（SSOT: docs/football-layout/shared/semantic-parser.md）。

segmentType 白名单：
ARTICLE_TITLE、MATCH_HEADER、TEAM_VS、MATCH_TIME、MATCH_METADATA、SUBHEADING、ANALYSIS_PARAGRAPH、
RECOMMENDATION_LIST、STAT_BAR、ODDS_TABLE、HIGHLIGHT、HIGHLIGHT_LIST、CTA_BANNER、
IMAGE_PLACEHOLDER、DIVIDER、DISCLAIMER、QUOTE、ORDERED_LIST、DECORATIVE_SLOGAN、PLAIN_PARAGRAPH。

分段规则（优先识别富组件）：
1. 首行短标题(<30字) → ARTICLE_TITLE；含作者品牌名时保留原文。
2. 「周X### 联赛」→ MATCH_HEADER；「队A VS 队B」独立行 → TEAM_VS。
3. 「比赛时间：…」→ MATCH_TIME；多行「联赛：…\\n轮次：…」→ MATCH_METADATA。
4. 小标题（以「：」结尾且<24字，或「1. 基本面」类短节名）→ SUBHEADING。
5. 「45% | 55%」「控球率 45% vs 55%」→ STAT_BAR（保留原文换行）。
6. 含「机构|胜|平|负」等管道符表格 ≥2 行 → ODDS_TABLE。
7. 「竞彩推荐/比分推荐/指数推荐」三行相邻 → 合并为一个 RECOMMENDATION_LIST（text 保留换行）。
8. 「【重点】…」「重点：…」→ HIGHLIGHT；短 CTA「点击…/立即…」→ CTA_BANNER。
9. 其余分析正文 → ANALYSIS_PARAGRAPH；无法分类 → PLAIN_PARAGRAPH。

铁律：index 从 0 连续递增；每个 text 必须是原文连续子串；所有 text 按序拼接后与原文逐字符相同。
不得增删、改写、纠错、重排或输出 HTML。只输出 JSON 数组。',
    updater = 'system'
WHERE tenant_id = 1
  AND scene = 'AI_TYPESET_SEMANTIC'
  AND deleted = 0;

-- ---------------------------------------------------------------------------
-- V204: S-21a quick typeset — 竞彩营销版 + 简洁通读版 presets
-- ---------------------------------------------------------------------------
SET NAMES utf8mb4;

SET @marketing_styles = '{"heading1":{"fontSize":"24px","fontWeight":"700","color":"#ffffff","lineHeight":"1.4","backgroundColor":"#1a1a2e","padding":"30px"},"heading2":{"fontSize":"18px","fontWeight":"700","color":"#1a1a2e","lineHeight":"1.4","borderLeft":"4px solid #e94560","paddingLeft":"12px"},"heading3":{"fontSize":"16px","fontWeight":"700","color":"#1a1a2e","lineHeight":"1.4"},"paragraph":{"fontSize":"15px","color":"#444444","lineHeight":"1.8","marginBottom":"16px","textIndent":"0"},"quote":{"fontSize":"15px","color":"#444444","backgroundColor":"#fff5f5","borderLeft":"4px solid #e94560","padding":"16px"},"list":{"fontSize":"15px","color":"#ffffff","lineHeight":"1.8","backgroundColor":"#1a1a2e","padding":"20px"},"image":{"fontSize":"14px","color":"#888888"}}';
SET @clean_styles = '{"heading1":{"fontSize":"20px","fontWeight":"700","color":"#262626","lineHeight":"1.4","borderLeft":"3px solid #434343","paddingLeft":"12px"},"heading2":{"fontSize":"17px","fontWeight":"700","color":"#262626","lineHeight":"1.4","borderLeft":"3px solid #434343","paddingLeft":"12px"},"heading3":{"fontSize":"16px","fontWeight":"700","color":"#333333","lineHeight":"1.4"},"paragraph":{"fontSize":"16px","color":"#333333","lineHeight":"1.85","marginBottom":"14px","textIndent":"0"},"quote":{"fontSize":"15px","color":"#555555","borderLeft":"3px solid #434343","paddingLeft":"14px","lineHeight":"1.85"},"list":{"fontSize":"15px","color":"#333333","lineHeight":"1.85","paddingLeft":"18px"},"image":{"fontSize":"14px","color":"#888888"}}';

INSERT INTO oa_wechat_layout_template
(tenant_id, template_name, description, content_type, document_type, layout_json, layout_schema,
 schema_version, layout_html, preview_html, source_type, status, creator_user_id, tags, default_params,
 creator, updater)
SELECT 1, '竞彩营销版', '一键排版：竞彩推广/截图风，圆角分析卡 + 无首行缩进 + #e94560 强调', 'ARTICLE', NULL,
       '{"version":2,"blocks":[]}',
       JSON_OBJECT('version', 2, 'footballTemplate', 'marketing', 'maxWidth', '720px', 'globalStyles', CAST(@marketing_styles AS JSON), 'blocks', JSON_ARRAY(
         JSON_OBJECT('id','marketing-title','type','heading','level',1,'styleRef','heading1','slotKind','heading'),
         JSON_OBJECT('id','marketing-section','type','heading','level',2,'styleRef','heading2','slotKind','heading','repeat',true),
         JSON_OBJECT('id','marketing-subheading','type','slot','slotKind','heading','level',3,'styleRef','heading3','repeat',true),
         JSON_OBJECT('id','marketing-quote','type','slot','slotKind','quote','styleRef','quote','repeat',true),
         JSON_OBJECT('id','marketing-recommendation','type','slot','slotKind','list','styleRef','list','repeat',true),
         JSON_OBJECT('id','marketing-paragraph-repeat','type','slot','slotKind','paragraph','styleRef','paragraph','repeat',true)
       )),
       2, NULL, NULL, 'PRESET', 'ENABLED', 1, 'football-ai:marketing',
       '{"maxWidth":"720px","paragraph_font_size":15,"paragraph_line_height":1.8,"paragraph_text_indent":"0"}',
       'system', 'system'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_wechat_layout_template
  WHERE tenant_id=1 AND source_type='PRESET' AND tags LIKE '%football-ai:marketing%' AND deleted=0
);

INSERT INTO oa_wechat_layout_template
(tenant_id, template_name, description, content_type, document_type, layout_json, layout_schema,
 schema_version, layout_html, preview_html, source_type, status, creator_user_id, tags, default_params,
 creator, updater)
SELECT 1, '简洁通读版', '一键排版：清爽通读，无头图/卡片，适合导语 + 长文', 'ARTICLE', NULL,
       '{"version":2,"blocks":[]}',
       JSON_OBJECT('version', 2, 'footballTemplate', 'clean-read', 'maxWidth', '680px', 'globalStyles', CAST(@clean_styles AS JSON), 'blocks', JSON_ARRAY(
         JSON_OBJECT('id','clean-title','type','heading','level',1,'styleRef','heading1','slotKind','heading'),
         JSON_OBJECT('id','clean-section','type','heading','level',2,'styleRef','heading2','slotKind','heading','repeat',true),
         JSON_OBJECT('id','clean-quote','type','slot','slotKind','quote','styleRef','quote','repeat',true),
         JSON_OBJECT('id','clean-paragraph-repeat','type','slot','slotKind','paragraph','styleRef','paragraph','repeat',true)
       )),
       2, NULL, NULL, 'PRESET', 'ENABLED', 1, 'football-ai:clean-read',
       '{"maxWidth":"680px","paragraph_font_size":16,"paragraph_line_height":1.85,"paragraph_text_indent":"0"}',
       'system', 'system'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_wechat_layout_template
  WHERE tenant_id=1 AND source_type='PRESET' AND tags LIKE '%football-ai:clean-read%' AND deleted=0
);

-- =============================================================================
-- Post-run verification (optional — run manually, expect all OK)
-- =============================================================================
SELECT 'v197_style_css_longtext' AS check_name,
       CASE WHEN DATA_TYPE = 'longtext' THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'oa_wechat_layout_template'
  AND COLUMN_NAME = 'style_css';

SELECT 'v198_decision_scan_preset' AS check_name,
       CASE WHEN COUNT(*) >= 1 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM oa_wechat_layout_template
WHERE tenant_id = 1 AND source_type = 'PRESET'
  AND tags LIKE '%football-ai:decision-scan%' AND deleted = 0;

SELECT 'v198_analysis_report_preset' AS check_name,
       CASE WHEN COUNT(*) >= 1 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM oa_wechat_layout_template
WHERE tenant_id = 1 AND source_type = 'PRESET'
  AND tags LIKE '%football-ai:analysis-report%' AND deleted = 0;

SELECT 'v198_ai_typeset_semantic_prompt' AS check_name,
       CASE WHEN COUNT(*) >= 1 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM oa_ai_prompt_config
WHERE tenant_id = 1 AND scene = 'AI_TYPESET_SEMANTIC' AND deleted = 0;

SELECT 'v204_marketing_preset' AS check_name,
       CASE WHEN COUNT(*) >= 1 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM oa_wechat_layout_template
WHERE tenant_id = 1 AND source_type = 'PRESET'
  AND tags LIKE '%football-ai:marketing%' AND deleted = 0;

SELECT 'v204_clean_read_preset' AS check_name,
       CASE WHEN COUNT(*) >= 1 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM oa_wechat_layout_template
WHERE tenant_id = 1 AND source_type = 'PRESET'
  AND tags LIKE '%football-ai:clean-read%' AND deleted = 0;

-- =============================================================================
-- Optional: Flyway schema history records (manual deploy only; uncomment if needed)
-- Assumes flyway_schema_history table exists in shenyu-ops and versions not yet recorded.
-- =============================================================================
-- INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
-- VALUES
--   ((SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history fsh), '197', 'm2 layout template mhtml style css longtext', 'SQL', 'V197__m2_layout_template_mhtml_style_css_longtext.sql', NULL, 'manual-prod', NOW(), 0, 1),
--   ((SELECT COALESCE(MAX(installed_rank), 0) + 2 FROM flyway_schema_history fsh), '198', 'm2 ai semantic typeset seed', 'SQL', 'V198__m2_ai_semantic_typeset_seed.sql', NULL, 'manual-prod', NOW(), 0, 1),
--   ((SELECT COALESCE(MAX(installed_rank), 0) + 3 FROM flyway_schema_history fsh), '199', 'm2 ai football layout template enrich', 'SQL', 'V199__m2_ai_football_layout_template_enrich.sql', NULL, 'manual-prod', NOW(), 0, 1),
--   ((SELECT COALESCE(MAX(installed_rank), 0) + 4 FROM flyway_schema_history fsh), '200', 'm2 ai typeset paragraph indent prompt', 'SQL', 'V200__m2_ai_typeset_paragraph_indent_prompt.sql', NULL, 'manual-prod', NOW(), 0, 1),
--   ((SELECT COALESCE(MAX(installed_rank), 0) + 5 FROM flyway_schema_history fsh), '201', 'm2 ai typeset marketing paragraph spacing', 'SQL', 'V201__m2_ai_typeset_marketing_paragraph_spacing.sql', NULL, 'manual-prod', NOW(), 0, 1),
--   ((SELECT COALESCE(MAX(installed_rank), 0) + 6 FROM flyway_schema_history fsh), '202', 'm2 ai typeset marketing segment types', 'SQL', 'V202__m2_ai_typeset_marketing_segment_types.sql', NULL, 'manual-prod', NOW(), 0, 1),
--   ((SELECT COALESCE(MAX(installed_rank), 0) + 7 FROM flyway_schema_history fsh), '203', 'm2 ai typeset rich segment prompt', 'SQL', 'V203__m2_ai_typeset_rich_segment_prompt.sql', NULL, 'manual-prod', NOW(), 0, 1),
--   ((SELECT COALESCE(MAX(installed_rank), 0) + 8 FROM flyway_schema_history fsh), '204', 'm2 quick typeset marketing clean presets', 'SQL', 'V204__m2_quick_typeset_marketing_clean_presets.sql', NULL, 'manual-prod', NOW(), 0, 1);
