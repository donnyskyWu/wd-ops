# -*- coding: utf-8 -*-
"""Generate V168 Flyway seed from docx extract."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[4]
text = (Path(__file__).parent / "docx-extract.md").read_text(encoding="utf-8")
markers = [
    ("POST_MATCH_REVIEW", "赛后复盘", r"1\.赛后复盘提示词"),
    ("PREHEAT_PREVIEW", "预热前瞻", r"2\.预热前瞻提示词"),
    ("NEW_ACCOUNT_TRAFFIC", "新号引流", r"3\.新号引流提示词"),
    ("SHORT_VIDEO_SCRIPT", "短视频文案", r"短视频文案提示词"),
    ("OFFICIAL_PLAN", "正式方案", r"5\.正式方案提示词"),
]
positions = []
for code, label, pat in markers:
    m = re.search(pat, text)
    if not m:
        raise SystemExit(f"missing {code}")
    positions.append((m.start(), code, label, m.group(0)))
positions.sort()


def normalize_placeholders(body: str) -> str:
    out = body
    reps = [
        (r"\{anchor\}", "{{author_name}}"),
        (r"\{event_info\}", "{{match_name}}"),
        (r"\{modify_info\}", "{{modify_info}}"),
        (r"\{match_result\}", "{{history_record}}"),
        (r"\{outcome_direction\}", "{{scheme_type}}"),
        (r"\{product_definition\}", "{{product_description}}"),
    ]
    for pat, repl in reps:
        out = re.sub(pat, repl, out)
    for old in (
        '{eventinfo["基本信息"]}|',
        "{eventinfo[“基本信息”]}|",
        '{eventinfo["基本信息"]}',
        "{eventinfo[“基本信息”]}",
    ):
        out = out.replace(old, "{{match_name}}")
    return out


def sql_escape(s: str) -> str:
    return s.replace("\\", "\\\\").replace("'", "''")


context_block = """【系统上下文】
【赛事信息】{{match_name}}
【作者/主播】{{author_name}}
【方案类型】{{scheme_type}}
{{#history_record}}【历史战绩】
{{history_record}}
{{/history_record}}
{{#anchor_style}}【主播风格】
{{anchor_style}}
{{/anchor_style}}
{{#product_description}}【产品定义说明】
{{product_description}}
{{/product_description}}
{{#preference_summary}}【用户偏好总结】
{{preference_summary}}
{{/preference_summary}}
{{#modify_info}}【用户修改意见】
{{modify_info}}
{{/modify_info}}

要求：仅输出正文内容，不要额外解释或客套话。

---
【文档类型写作指令】
"""

chunks = {}
for i, (start, code, label, _title) in enumerate(positions):
    end = positions[i + 1][0] if i + 1 < len(positions) else len(text)
    body = text[start:end]
    body = re.sub(r"^.*?\n", "", body, count=1).strip()
    body = normalize_placeholders(body)
    prefix = f"【文档类型提示词·{label}·{code}】\n\n"
    chunks[code] = (label, prefix + context_block + body)

out_dir = Path(__file__).parent
mapping = [
    "# Docx → dict_document_type mapping",
    "",
    "| Docx section | Code | Chars |",
    "|---|---|---|",
]
for code, label, _ in markers:
    mapping.append(f"| {label} | `{code}` | {len(chunks[code][1])} |")
(out_dir / "MAPPING.md").write_text("\n".join(mapping) + "\n", encoding="utf-8")

for code, (label, content) in chunks.items():
    (out_dir / f"prompt-{code}.txt").write_text(content, encoding="utf-8")

sql_lines = [
    "-- V168: AI_CONTENT_CHAT prompts by document_type (docx 各类文档提示词)",
    "-- Source: docs/内容生成/各类文档提示词.docx · ADR-063",
    "SET NAMES utf8mb4;",
    "",
    "SET @var_desc = '{{match_name}}=赛事; {{author_name}}=作者/主播; {{scheme_type}}=方案类型/参考方向; {{history_record}}=历史战绩/赛果; {{anchor_style}}=主播风格; {{product_description}}=产品说明; {{preference_summary}}=偏好总结; {{modify_info}}=用户修改意见';",
    "",
]
for code, label, _ in markers:
    content = chunks[code][1]
    tpl_name = f"AI内容对话-{label}"
    remark = f"ADR-063 docx {label} prompt"
    sql_lines += [
        f"-- {code}",
        "INSERT INTO oa_ai_prompt_config",
        "  (tenant_id, template_name, version, scene, content_type, document_type, prompt_content, variable_desc, temperature, status, remark)",
        "SELECT 1,",
        f"  '{sql_escape(tpl_name)}', 'v1', 'AI_CONTENT_CHAT', 'ARTICLE', '{code}',",
        f"  '{sql_escape(content)}',",
        "  @var_desc, 0.70, 'ENABLED',",
        f"  '{sql_escape(remark)}'",
        "FROM DUAL",
        "WHERE NOT EXISTS (",
        "  SELECT 1 FROM oa_ai_prompt_config",
        f"  WHERE tenant_id = 1 AND scene = 'AI_CONTENT_CHAT' AND document_type = '{code}' AND deleted = 0",
        ");",
        "",
        "UPDATE oa_ai_prompt_config SET",
        f"  template_name = '{sql_escape(tpl_name)}',",
        "  version = 'v1',",
        "  content_type = 'ARTICLE',",
        f"  prompt_content = '{sql_escape(content)}',",
        "  variable_desc = @var_desc,",
        "  temperature = 0.70,",
        "  status = 'ENABLED',",
        f"  remark = '{sql_escape(remark)}',",
        "  updater = 'system',",
        "  update_time = CURRENT_TIMESTAMP",
        f"WHERE tenant_id = 1 AND scene = 'AI_CONTENT_CHAT' AND document_type = '{code}' AND deleted = 0;",
        "",
    ]

mig = (
    ROOT
    / "football-backend-saas"
    / "football-module-ops"
    / "football-module-ops-server"
    / "src"
    / "main"
    / "resources"
    / "db"
    / "migration"
    / "V168__ai_content_chat_prompt_by_document_type.sql"
)
mig.write_text("\n".join(sql_lines) + "\n", encoding="utf-8")
summary = out_dir / "gen-summary.txt"
summary.write_text(
    "\n".join([f"{code}\t{len(chunks[code][1])}" for code, _, _ in markers] + [f"sql={mig}"]) + "\n",
    encoding="utf-8",
)
print("OK", mig.exists(), mig.stat().st_size)