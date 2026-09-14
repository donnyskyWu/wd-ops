# Production 增量 SQL 索引（2026-09-14）

手工生产部署脚本，按数据库拆分。**不包含 shenyu-member**（用户数据从 prod 同步）。

Flyway SSOT：`football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/`

**无需检查 Flyway 版本**；脚本按最佳努力幂等设计，可安全重复执行或部分已应用后再跑。

---

## 执行顺序总览

| 顺序 | 文件 | 目标库 | 版本范围 | 说明 |
|------|------|--------|----------|------|
| 1 | `prod-incremental-shenyu-ops-V192-V196.sql` | **shenyu-ops** | V192, V194, V195, V196（OPS 表） | 9/7 前批次；ADR-074/075/076/077 表结构 |
| 2 | `prod-incremental-shenyu-system-V193-V196.sql` | **shenyu-system** | V193, V196（字典） | 9/7 前批次；`dict_marketing_plan_type` + `dict_ai_generate_status` |
| 3 | `prod-incremental-shenyu-ops-V197-V204.sql` | **shenyu-ops** | V197–V204 | **9/7 后批次**；S-21a AI 排版 / ADR-028 |

> **仅部署 9/7 后增量**：若 V192–V196 已在 prod 执行，只需跑第 3 步。  
> **全量 V192→V204**：按 1 → 2 → 3 顺序执行。

---

## 各文件详情

### 1. prod-incremental-shenyu-ops-V192-V196.sql

- **前置**：prod Flyway baseline ≤ V191
- **内容**：营销计划字段、多赛事 JSON、assignment↔task 关联表、execution_group_id、match_scheme、document_type、ai_generate_status 列
- **执行**：
  ```bash
  mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < prod-incremental-shenyu-ops-V192-V196.sql
  ```
- **验证**：`oa_sop_template.marketing_plan`、`oa_work_task_assignment_task` 表、`oa_production_content.match_scheme_json` 等列存在

### 2. prod-incremental-shenyu-system-V193-V196.sql

- **前置**：prod Flyway baseline ≤ V191；`dict_marketing_plan_type` 类型已存在
- **内容**：营销计划三值字典替换；`dict_ai_generate_status` 类型 + 四值 seed
- **执行**：
  ```bash
  mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-system < prod-incremental-shenyu-system-V193-V196.sql
  ```
- **验证**：
  ```sql
  SELECT value, label FROM system_dict_data
    WHERE dict_type='dict_marketing_plan_type' AND deleted=0 ORDER BY sort;
  -- 期望：KUAISHOU_PAID_COURSE, COMPANY_PAID, FREE_PUBLIC

  SELECT value, label FROM system_dict_data
    WHERE dict_type='dict_ai_generate_status' AND deleted=0 ORDER BY sort;
  -- 期望：QUEUED, GENERATING, SUCCESS, FAILED
  ```

### 3. prod-incremental-shenyu-ops-V197-V204.sql（NEW）

- **前置**：V196 已应用（见文件 1）
- **内容**：
  - V197：`oa_wechat_layout_template.style_css` → LONGTEXT
  - V198–V204：AI 排版 PRESET seed（决策扫读版、情报分析版、竞彩营销版、简洁通读版）+ `AI_TYPESET_SEMANTIC` prompt 演进
- **无 shenyu-system 变更**：V197–V204 不涉及跨库字典
- **执行**：
  ```bash
  mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < prod-incremental-shenyu-ops-V197-V204.sql
  ```
- **验证**（脚本末尾含自动校验 SELECT，期望全部 `OK`）：
  ```sql
  -- style_css 类型
  SELECT DATA_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_wechat_layout_template' AND COLUMN_NAME='style_css';
  -- 期望：longtext

  -- 4 款 football-ai PRESET
  SELECT template_name, tags FROM oa_wechat_layout_template
    WHERE tenant_id=1 AND source_type='PRESET' AND tags LIKE '%football-ai:%' AND deleted=0;
  -- 期望：决策扫读版、情报分析版、竞彩营销版、简洁通读版

  -- AI 语义排版 prompt
  SELECT scene, status, LEFT(prompt_content, 80) FROM oa_ai_prompt_config
    WHERE tenant_id=1 AND scene='AI_TYPESET_SEMANTIC' AND deleted=0;
  -- 期望：1 行 ENABLED，prompt 含 STAT_BAR / ODDS_TABLE
  ```

---

## 幂等性说明

| 类型 | 策略 |
|------|------|
| DDL（V197） | 检测 `information_schema.COLUMNS` 列类型，已为 LONGTEXT 则跳过 |
| INSERT seed | `WHERE NOT EXISTS` |
| UPDATE enrich | 重复执行结果一致；V200/V201/V202 带 `NOT LIKE` 守卫防重复追加 |
| V203 prompt | 全量 REPLACE 至目标版本，重复执行安全 |

ALTER 类（V192–V196）重复执行可能报 Duplicate column，需人工确认后跳过对应段。

---

## 不包含

- **shenyu-member**：用户/会员库从 prod 同步，本索引无相关脚本
- **ADR-080**：任务节点名称与执行页登记备注，无 SQL 变更
- **Flyway 写入**：各文件末尾有注释掉的 `flyway_schema_history` INSERT，按需取消注释

---

## 相关文档

- Greenfield 全量：`01-shenyu-ops-schema.sql`、`02a-shenyu-system-dicts.sql`
- Schema 验证：`verify-schema.sql`
- Slice：`docs/delivery/SLICES-M2-S21a-AI排版.md`
