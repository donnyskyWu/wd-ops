# PRD-M10-数据采集

> **业务域**：M10 数据采集
> **功能模块**：采集任务 + 数据质量
> **详细设计章节**：5.40、5.41（v9.1 编号，对应 5.36-5.37 位置）
> **版本**：v1.1 | 2026-06-24
> **状态**：Draft（Phase 2 Channel-A MVP 已实现 · 见 §2.3）
> **全局规范**：[`docs/engineering/GLOBAL-CONVENTIONS.md`](./../engineering/GLOBAL-CONVENTIONS.md)

---

## 0. 元信息

| 字段 | 值 |
|------|---|
| 模块 | M10 数据采集 |
| 业务域 | 数据采集（COLLECT） |
| 详细设计 | `## 5.40~5.41` |

---

## 1. 概述

### 1.1 一句话描述

**采集引擎核心**：从各平台/外部源/API/手工录入采集数据，支持定时调度、质量检查、失败重试。

### 1.2 目标

| 维度 | 目标 |
|------|------|
| 自动化 | 80% 数据自动采集 |
| 准确性 | 采集准确率 ≥ 99% |
| 实时 | 关键数据实时同步（≤ 5min） |
| 质量 | 数据质量检查 100% 覆盖 |

### 1.3 术语表

| 术语 | 定义 |
|------|------|
| **采集任务** | 一次数据拉取的配置 + 调度 |
| **采集方式** | 内部采集 / 外部采集 / 第三方 API / 手工录入 |
| **采集源** | 公众号 API / 视频号 API / 抖音开放平台 / 奥创接口 / 企微 API / 个微 API |
| **采集频率** | 实时 / 每小时 / 每天 / 每周 / 每月 / 手动 |
| **数据质量** | 完整性 / 准确性 / 一致性 / 时效性 / 唯一性 |
| **质量等级** | 优 / 良 / 中 / 差 |

---

## 2. 范围

### 2.1 In Scope（2 个 FR 模块）

| FR 编号 | 名称 | 优先级 | 详细设计 |
|---------|------|--------|---------|
| FR-M10-001 | 采集任务管理 | P0 | 5.40 |
| FR-M10-002 | 数据质量检查 | P0 | 5.41 |

### 2.2 Out of Scope

1. ❌ **不实现** XXL-JOB（使用 Spring `@Scheduled`）
2. ❌ **不实现** RabbitMQ 异步（使用 Spring `@Async`）
3. ❌ **不实现** MinIO 对象存储（本地文件系统）
4. ❌ **不实现** FR-M10-002 数据质量检查（Phase 2 未启动）
5. ❌ **不实现** 奥创 Channel-B 任务执行（`WECHAT_PERSONAL` 任务 dataTypes 为空）
6. ❌ **不实现** 采集数据自动写入 `oa_content_daily`（M1 为只读桥接，见 [ADR-049](../adr/ADR-049-M10-全量采集与展示桥接.md)）

### 2.3 Phase 2 已实现（2026-06-24 · 文档同步）

> SSOT：[ADR-047](../adr/ADR-047-M4-平台账号凭证SSOT与Collector映射.md) · [ADR-048](../adr/ADR-048-M10-企微采集通道草案.md) · [ADR-049](../adr/ADR-049-M10-全量采集与展示桥接.md)

| 能力 | 说明 |
|------|------|
| Channel-A 六平台路由 | `UnifiedCollectorAdapter` + M4 collector-bind（ADR-047） |
| 平台全量采集 | `data_type` 空 → 按平台顺序执行全部 `dict_collect_data_type`（ADR-049 Q1） |
| 多平台落库 | V116/V121/V122：`oa_wechat_mp_article`、抖音/视频号/快手/小红书作品与粉丝表 |
| 任务 UX 简化 | 编辑页不暴露 method/source/dataType；展示只读「采集范围」（ADR-049 Q2） |
| 日志多类型 | `PARTIAL` + `result.typeResults[]`（ADR-049 Q3） |
| 企微 Channel-C | `WeComAdapter` + `oa_wework_daily_stats`（ADR-048） |
| M1/M4 展示桥接 | `CollectedDataQueryService` 只读合并（ADR-049 Q4） |

---

## 3. 功能需求

### FR-M10-001 采集任务管理（5.40）

#### 4.1.1 描述

管理数据采集任务：配置 + 调度 + 执行 + 监控 + 重试。

#### 4.1.2 数据项

| 字段 | 控件 | 字典/实体 |
|------|------|----------|
| `task_name` | `<Input />` | - |
| `platform_type` | `<DictSelect dict-type="dict_platform_type" />` | 字典 |
| `account_id` | `<AccountSelect />` | `oa_account`（**强关联** ⭐） |
| `method` | 后端默认 `INTERNAL`（运营 UI 不编辑） | 字典 |
| `source` | 后端按 `platformType` 默认（运营 UI 不编辑） | 字典 |
| `data_type` | 存库 `null` = 全量采集；运营 UI 不编辑 | `dict_collect_data_type` |
| `frequency` | `<DictSelect dict-type="dict_collect_frequency" />` | 字典 |
| `cron` | `<Input />`（cron 表达式） | - |
| `api_config` | `<TextArea />`（API 配置 JSON） | -（加密） |
| `status` | `<DictSelect dict-type="dict_collect_status" />` | 字典 |
| `last_run_at` | `<DateTimePicker />` | -（只读） |
| `next_run_at` | `<DateTimePicker />` | -（只读） |

#### 4.1.3 业务规则

- 调度：Spring `@Scheduled(cron = "...")`，不依赖 XXL-JOB
- 异步：Spring `@Async`，不依赖 RabbitMQ
- 失败重试：3 次指数退避
- 凭证：Channel-A 凭证 SSOT 在 M4 `oa_account`（ADR-047）；任务 `apiConfig` 仍支持 AES-256 加密 JSON
- **全量采集**：`data_type` 为空时，`CollectPlatformDefaults` 按平台顺序串行执行全部 dataType（ADR-049）
- **日志状态**：多类型执行时，部分成功 → `PARTIAL`；全部成功 → `SUCCESS`；全部失败 → `FAILED`
- **企微任务**：`platform_type=WEWORK` 时 `account_id` → `oa_wework_account.id`（ADR-048）

#### 4.1.4 验收标准

**AC-M10-001-1**（CRUD）

**AC-M10-001-2**（字典校验）
- `platformType` / `method` / `source` / `frequency` / `status` 均 `@InDict`

**AC-M10-001-3**（强关联）
- `accountId` 用 `<AccountSelect />`

**AC-M10-001-4**（定时调度）
- 启动后 Spring 调度生效

**AC-M10-001-5**（失败重试）
- 失败 3 次后状态 = FAILED

**AC-M10-001-6**（凭证加密）
- `apiConfig` 数据库存储为密文

---

### FR-M10-002 数据质量检查（5.41）

#### 4.2.1 描述

对采集的数据进行质量检查（完整性/准确性/一致性/时效性/唯一性）。

#### 4.2.2 数据项

| 字段 | 控件 | 字典/实体 |
|------|------|----------|
| `check_name` | `<Input />` | - |
| `check_type` | `<DictSelect dict-type="dict_quality_check_type" />` | 字典 |
| `target_table` | `<Input />` | - |
| `target_field` | `<Input />` | - |
| `rule` | `<TextArea />`（规则表达式） | - |
| `level` | `<DictSelect dict-type="dict_quality_level" />` | 字典 |

#### 4.2.3 业务规则

- **完整性**：必填字段非空率
- **准确性**：数据值在合理范围
- **一致性**：跨表数据一致
- **时效性**：数据新鲜度（采集时间 - 数据时间）
- **唯一性**：主键/唯一键不重复

#### 4.2.4 验收标准

**AC-M10-002-1**（创建检查）

**AC-M10-002-2**（字典校验）

**AC-M10-002-3**（质量等级）
- 优（≥95%）、良（80-94%）、中（60-79%）、差（<60%）

---

## 4. 集成与数据

### 4.1 核心实体

| 实体 | 用途 | 关联 |
|------|------|------|
| `oa_collect_task` | 采集任务 | `oa_account` 或 `oa_wework_account`（企微） |
| `oa_collect_log` | 采集日志（含 `result_json`） | `oa_collect_task` |
| `oa_collector_account_bind` | Channel-A 双 ID 映射 | `oa_account`（ADR-047） |
| `oa_wechat_mp_article` / `oa_wechat_mp_follower` | 公众号采集快照 | `oa_account` |
| `oa_douyin_follower` / `oa_douyin_video` | 抖音采集快照 | `oa_account` |
| `oa_wechat_video_work` | 视频号作品 | `oa_account` |
| `oa_kuaishou_video` | 快手作品 | `oa_account` |
| `oa_xiaohongshu_note` | 小红书笔记 | `oa_account` |
| `oa_wework_daily_stats` | 企微日聚合 | `oa_wework_account`（ADR-048） |
| `oa_data_quality_check` | 质量检查（未实现） | - |
| `oa_data_quality_log` | 质量日志（未实现） | `oa_data_quality_check` |

### 4.2 关联属性

| 字段 | 选择器 |
|------|--------|
| `accountId` | `<AccountSelect />` |
| `platformType` | `<DictSelect dict-type="dict_platform_type" />` |
| `method` | `<DictSelect dict-type="dict_collect_method" />` |
| `source` | `<DictSelect dict-type="dict_collect_source" />` |
| `frequency` | `<DictSelect dict-type="dict_collect_frequency" />` |
| `status` | `<DictSelect dict-type="dict_collect_status" />` |
| `checkType` | `<DictSelect dict-type="dict_quality_check_type" />` |
| `level` | `<DictSelect dict-type="dict_quality_level" />` |

---

## 5. 决策记录

| 编号 | 问题 | 决策 | 原因 |
|------|------|------|------|
| ADR-M10-001 | 任务调度依赖 XXL-JOB 吗？ | 不依赖，Spring `@Scheduled` | 中间件简化（ADR-001） |
| ADR-M10-002 | 异步处理依赖 RabbitMQ 吗？ | 不依赖，Spring `@Async` | 中间件简化（ADR-001） |
| ADR-M10-003 | 凭证存储？ | 本地 + AES-256 | 中间件简化（ADR-001） |
| ADR-047 | Channel-A 凭证 SSOT？ | M4 `oa_account` + bind 表 | 消除 M8 双 SSOT |
| ADR-048 | 企微采集？ | `WeComAdapter` 直连 | 不经 collector bind |
| ADR-049 | 单任务多 dataType？ | 空 data_type = 全量顺序执行 | 降低运营配置成本 |

---

*下一步：UX / API / STATE / SLICES / CHECKLIST / TESTCASES。*
