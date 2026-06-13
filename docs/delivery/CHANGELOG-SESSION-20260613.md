# CHANGELOG-SESSION-20260613 · 文档同步

> **会话**：DOC-SYNC-20260613  
> **日期**：2026-06-13  
> **范围**：M2 / M4 / M5 / M6 / M8 / M9 · 代码 ↔ Spec 对齐  
> **依据**：工作区未提交实现（0612 下午会话 + 近期走查补丁）

---

## 1. 已同步（代码 → Spec）

### M2 内容 / 计划 / 任务

| 变更 | 文档 |
|------|------|
| 计划步骤 **多赛事** + **单执行人**；详情含 **tasks** 表 | PRD-M2 § FR-M2-009 · PRD-M2-S09 · API-M2-计划管理 v1.5 · UX-M2 §8 |
| 任务列表 **「我的任务」默认 Tab**；展示 scheduled 起止 | UX-M2 §4 · CHECKLIST-M2 |
| 任务执行 **ContentEditDialog**；**提交审核**（非确认）；待审只读 | PRD-M2 · UX-M2 · ADR-017 |
| 内容：**弹窗**创作；**无封面**；平台/账号 **可选多选**；IP 组 **必填**；LONGTEXT body | PRD-M2 FR-M2-003 · API-M2 §3.2 · UX-M2 §5 |
| 内容审核：**可配置二级**（M9 参数）；一级/二级 Tab；IP 组长范围；流程 steps | ADR-017 · STATE-M2 §2 · UX-M2 §6 · API-M2 §3.2.4/3.4/3.5 |
| AI 生成：**真实 LLM** + M8 模型/提示词选择 | API-M2 §3.7 · PRD-M8 CFG-026/034 |
| 移除侧栏「内容创作」菜单；**统一 Layout 面包屑** | UX-M2 §5 · PRD-M2 §9 |
| 内容列表 **exportToExcel** | ADR-018 · CHECKLIST-M2 |

### M8 配置

| 变更 | 文档 |
|------|------|
| `dict_ai_model_type` 扩展 10 值（V69） | GLOBAL-CONVENTIONS · PRD-M8 §3.6 |
| 提示词 `content_type` / `document_type` 字段（V69） | PRD-M8 §3.7 · API 引用 |

### M1 / M9

| 变更 | 文档 |
|------|------|
| IP 组成员 **岗位** `dict_position` | UX-M1 · V69 迁移 |
| 用户 **多角色** `roleIds` 多选 | PRD-M9 § FR-M9-001 |
| 系统参数 Tab「**内容审核**」+ 角色下拉 | PRD-M9 · ADR-017 · GLOBAL §11.5 |

### M4 内部管理

| 变更 | 文档 |
|------|------|
| 6 页 CSV 导出（公司/实名人/手机/手机卡/平台账号/个人账号） | UX-M4 §2 · ADR-018 |

### M5 / M6 分析

| 变更 | 文档 |
|------|------|
| 多分析页 `exportToExcel` | ADR-018 · UX-M6 §4 |
| 自定义查询 **QueryResultPanel 分页** | UX-M6 §6.2 |
| 筛选区 **TableSearch 边框** 统一 | UX-M6 §4 |
| 人效页 **移除岗位筛选项**（保留表格列） | 实现确认 · 待 PRD-M1 细项（见 §2） |
| 指标分析 Tab 顺序：明细 → 趋势 | 实现确认 · 待 UX-M6 细项（见 §2） |

---

## 2. 仍存在的 Spec 缺口 / 代码偏离

| 编号 | 现象 | 建议 |
|------|------|------|
| GAP-M2-001 | PRD 原「三级审核」文案部分 AC 未逐条改写 | 下一批更新 TESTCASES-M2 AC-M2-003-2~5 |
| GAP-M2-002 | `POST /content/{id}/confirm` 与 `COMPLETED` 仍存在于 API/IT | ADR-017 §4 BLK-M2-006-R：待产品决定是否废弃 |
| GAP-M2-003 | 任务完成门禁 IT 仍测 `COMPLETED`；实现可能已改为 submit-review | 需对齐 `M2ContentS13IT` 与 Product |
| GAP-M2-004 | 短视频 AI 视频 URL / `final_video_url` 仍为占位 | BLK-M2-010 仍阻塞 |
| GAP-M2-005 | M8 提示词按赛事+类型的 **自动匹配规则** 未完整 Spec | BLK-M2-005 仍阻塞 |
| GAP-M1-001 | 人效/指标 Tab 等 UI 微调未写入 PRD-M1 正文 | 可选 UX-M1 v1.1 补丁 |
| GAP-EXPORT-001 | Spec 写「Excel」实际为 **CSV** | 已用 ADR-018 澄清；PRD 按钮文案未改 |

---

## 3. 新增 ADR

| ADR | 标题 |
|-----|------|
| [ADR-017](../adr/ADR-017-M2-可配置二级内容审核.md) | M2 可配置二级内容审核与 IP 组审核范围 |
| [ADR-018](../adr/ADR-018-前端CSV导出模式.md) | 分析/管理页前端 CSV 导出 |

---

## 4. 更新的文档文件清单

**ADR**：ADR-017 · ADR-018  

**Product**：PRD-M2-内容生产 · PRD-M2-S09-计划管理 · PRD-M8-配置管理 · PRD-M9-系统管理  

**UX**：UX-M2-内容生产 · UX-M4-账号管理 · UX-M6-数据分析 · UX-M1-运营管理（成员岗位）  

**Engineering**：API-M2-内容生产 · API-M2-计划管理 · STATE-M2-内容生产 · GLOBAL-CONVENTIONS  

**Delivery**：CHECKLIST-M2-内容生产 · 本 CHANGELOG · MASTER-EXECUTION-TRACKER §1.1  

---

## 5. 未改动的模块文档（无需同步或已对齐）

- M3 / M7 / M0：本次会话无实现变更
- M5 PRD：导出行为已由 ADR-018 横切覆盖；未单独改 PRD-M5 正文
- SLICES-M2 / TESTCASES-M2：部分条目仍引用「三级审核/确认」— 建议后续 Slice 专项同步
