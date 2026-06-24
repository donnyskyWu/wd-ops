# CHANGELOG-IMPL-20260624 · M10 Phase 2 实现同步

> **会话**：DOC-SYNC-20260624  
> **日期**：2026-06-24  
> **范围**：M10 Channel-A 多平台 · M1/M4 采集展示桥接 · 奥创签名 · Bug 修复  
> **依据**：工作区未提交实现（V121–V122 迁移 + 前后端联调补丁）

---

## 1. 已同步（代码 → Spec）

### M10 数据采集

| 变更 | 文档 | ADR |
|------|------|-----|
| 平台全量 dataType 串行执行（`data_type` 空/ALL） | PRD-M10 §2.3 · API-M10 §5 · UX-M10 §3 | [ADR-049](../adr/ADR-049-M10-全量采集与展示桥接.md) |
| 任务编辑隐藏 method/source/dataType；只读「采集范围」 | UX-M10 §3 · PRD-M10 FR-M10-001 | ADR-049 Q2 |
| 日志 PARTIAL + `typeResults` 多类型详情 | API-M10 §1.7 · UX-M10 §4 · PRD-M10 | ADR-049 Q3 |
| V121/V122 多平台落库表 + `dict_collect_data_type` 扩展 | API-M10 §6 · PRD-M10 §4.1 | ADR-049 |
| Collector bind / Channel-A 路由（既有） | API-M10 §3–4 · CHECKLIST-M10 §8 | ADR-047 |
| 企微 Channel-C S-01~S-04 | CHECKLIST-M10 §9 · API-M10（引用 ADR-048） | ADR-048 |

### M1 / M4 展示桥接

| 变更 | 文档 | ADR |
|------|------|-----|
| `CollectedDataQueryService` 合并 M10 表至分析 API | PRD-M1 §4.3.5 · API-M1 §桥接 · PRD-M4 §4.5.6 | ADR-049 Q4 |
| 平台账号详情内容趋势 / 作品数 | PRD-M4 §4.5.6 | ADR-049 |

### M8 奥创（Channel-B 基建）

| 变更 | 文档 | ADR |
|------|------|-----|
| `AochuangSignatureHelper` SHA1 · Signature-Version 2.0 | ADR-045 §4.1 | ADR-045 增补 |
| 端点：`/api/v1/wechatAccounts`、`wechatFriends`、`wechatMessages` | ADR-045 §4 | ADR-045 增补 |

### 运维脚本

| 变更 | 文档 |
|------|------|
| `scripts/restart-all.ps1` / `.sh` 一键重启 backend + collector + UI | `ops-platform-server/README.md`（已有联调步骤） |

### Bug 修复（行为记录，无新 FR）

| 现象 | 修复 | 验证 |
|------|------|------|
| 采集任务保存账号丢失 | `CollectTaskServiceImpl` 账号校验/写入 | IT |
| 立即执行长时间 loading | 前端 timeout / 异步反馈 | 手工 |
| 多类型执行误标 FAILED | `CollectRunService` → `PARTIAL` | `M10ApiCollectorExecAllTypesIT` |
| 抖音 rate limit / video-stats | collector + adapter 重试 | IT / 联调 |
| 公众号 article `msgid_itemidx` 复合键 | 同步逻辑 | `M10ApiCollectorExecMpArticleIT` |
| 快手 Playwright Windows | unify-collector `run.py` | 联调 |
| 小红书 cookie registry / guest 检测 | collector 侧 | 联调 |

---

## 2. 新增 ADR

| ADR | 标题 |
|-----|------|
| [ADR-049](../adr/ADR-049-M10-全量采集与展示桥接.md) | M10 全量 dataType 任务与采集数据展示桥接 |

ADR-045 增补 §4.1 奥创 OpenAPI 签名（Apifox doc-8348826）。

---

## 3. CHECKLIST 更新

| 文档 | 新增勾选 |
|------|----------|
| CHECKLIST-M10 §10 | 全量 dataType · V121/V122 · 日志 typeResults · 展示桥接 IT |
| CHECKLIST-M1 §Phase2-M10 | `M10CollectedDataDisplayIT` |

---

## 4. 仍 Out of Scope / 缺口

| 编号 | 项 | 说明 |
|------|-----|------|
| GAP-M10-001 | FR-M10-002 数据质量 | 未实现 |
| GAP-M10-002 | M10-API Gate 正式签收 | CHECKLIST §8 待人工 |
| GAP-M10-003 | 全平台生产 collector 联调 | 需真实凭证 |
| GAP-M10-004 | M10-AO-S-00~S-07 奥创采集任务 | Channel-B 任务壳未接 |
| GAP-M10-005 | Bilibili 作品列表/明细 | MVP 仅 `FOLLOWER_STATS` |
| GAP-M10-006 | 采集 → `oa_content_daily` 自动同步 | 未做；展示为只读桥接 |
| GAP-M10-007 | AppSecret OpenAPI 混合 | ADR-047 Phase 2 |

---

## 5. 迁移清单（本会话相关）

| 版本 | 说明 |
|------|------|
| V120 | `oa_collect_log.result_json` |
| V121 | 抖音粉丝/作品表 + dataType |
| V122 | 视频号/快手/小红书表 + 公众号文章扩展列 |
