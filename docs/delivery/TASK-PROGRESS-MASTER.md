# 任务进度总表（单人全栈执行）

> **性质**：任务 SSOT · 前后端由同一人按 slice 顺序交付，不再分人/分文件域  
> **维护**：每完成 1 个 slice → 更新本表 + `SESSION-PROGRESS.md` + `MASTER-EXECUTION-TRACKER.md` §1.1  
> **历史**：原 Mike/Donny 双人表已归档 → [Mike](./TASK-PROGRESS-MIKE.md) · [Donny](./TASK-PROGRESS-DONNY.md)（只读）  
> **最后更新**：2026-06-18 · Phase 2 待办登记（M10 + 微信发布）

---

## 0. 执行机制（单人版）

### 0.1 原则

| 项 | 说明 |
|----|------|
| **一人全栈** | 每个 slice 内：**后端 → 前端 → IT/Playwright → 文档**，一气呵成 |
| **一片一会话** | 仍遵守 PHASE-DEV-METHOD：不跨 slice 混改 |
| **无文件域锁** | 不再需要「Mike 域 / Donny 域」；改 API 时同步 types + vue |
| **命名** | 新 slice 用 `S-R{N}`（不再加 `-Mike`/`-Donny`）；历史 slice 保留旧名便于追溯 |

### 0.2 每个 slice 标准流程

```
1. git pull origin main
2. 读本表「当前 slice」+ Spec（@ PRD/API/SLICES，不粘贴长文）
3. 五段式实现：DTO → Mapper → Service → Controller → IT
4. 前端接线：api → types → .vue → Playwright（如涉及页面）
5. 自测：mvn test（相关 IT）+ Playwright 子集/走查
6. 更新本表 + SESSION-PROGRESS + MASTER §1.1/§15
7. commit → push（message: S-R{N}: 简述）
```

### 0.3 自检清单（每个 slice 结束前）

- [ ] 6 字段对照表（api URL ↔ controller ↔ 前端 params）
- [ ] `OaErrorCodes` 无重复 · Flyway 版本递增
- [ ] IT 自包含 seed · 相关 Playwright 绿
- [ ] PRD/ADR 与实现一致（有决策写 ADR）
- [ ] PowerShell 批量改中文文件 → 用 Python UTF-8 或 StrReplace

---

## 1. 阶段总览

| 阶段 | 目标 | 状态 | 说明 |
|------|------|------|------|
| **补强 Wave-1** | D-1 / D-7 / B-6 / B-7 | ✅ | 原 S-R21~R24（前后端均已合入） |
| **补强 Wave-2** | D-1 前端 + P-3/P-4 | ✅ | 原 S-R21-Donny + S-R25 |
| **补强 Wave-3** | B-8 详情页 + P-2 全量 PW | 🟡 P-2 ✅ · S-R27 进行中 | **当前焦点** |
| **补强 Wave-4** | S-R26 集成回归 + 上线决策 | ⬜ | Wave-3 完成后 |
| **Phase-2** | M10 三通道采集 + M2 微信发布 + D-2~D-6 | ⬜ 待 Gate 立项 | 见 §3 · [ADR-045](../adr/ADR-045-M10-奥创多账号与设备同步.md) · [M10-三通道采集计划](./M10-三通道采集计划.md) |

---

## 2. 当前进行中

### S-R27 · B-8 详情页字段补全（全栈）

| 子项 | 范围 | 文件 | 状态 |
|------|------|------|------|
| 27a | M3 绩效详情 + 趋势 **后端** | `PerfRecord*` · `PerfTrendVO` · `M3PerfDetailEnrichIT` | ✅ |
| 27b | M3 绩效详情 + 趋势 **前端** | `PerfRecordDetail.vue` · `PerfUserTrend.vue` · `api/perf*` | ⬜ **下一刀** |
| 27c | M4 公司详情 + 扩容历史 **全栈** | `CompanyController` · 详情 `.vue` · IT | ⬜ |
| 27d | M4 平台账号统计字段 **全栈** | `AccountRespVO` · 账号详情 `.vue` · IT | ⬜ |

**27b 交付标准**：`workflowStatus` / `userInfo` / `metricCode`+`target` 真值展示；去掉审批 mock。

---

## 3. 待办队列（按优先级）

| # | Slice | 待办 ID | 任务 | 预估 | 依赖 |
|---|-------|---------|------|------|------|
| 1 | **S-R27** | B-8 | 详情页字段（见 §2 子项 27b→27d） | 2d | 27a ✅ |
| 2 | ~~P-2~~ | P-2 | ~~Playwright 全量复测~~ | — | ✅ 2026-06-10 · 183/183 |
| 3 | **S-R26** | — | `mvn verify` + PW 全量 + Gate 签字 | 1d | P-2 |
| 4 | **D-2~D-6** | D-2~D-6 | M9 五页（Param/Dict/Log/LoginLog/Message）Phase 2 决策 | 产品会 | 不阻塞 v1.0 |

### 3.1 Phase 2 待办（Gate 立项后执行）

> SSOT：[ADR-045](../adr/ADR-045-M10-奥创多账号与设备同步.md) · [M10-三通道采集计划](./M10-三通道采集计划.md) · MASTER [§17](./MASTER-EXECUTION-TRACKER.md#17-phase-2-backlog待-gate-立项)

| # | Slice | 待办 ID | 任务 | 预估 | 依赖 |
|---|-------|---------|------|------|------|
| 5 | **M10-COL** | P2-M10-00 | 采集任务壳 M10-COL-S-01~S-03 | 3d | Phase 2 Gate |
| 6 | **M10-API** | P2-M10-A | api.json 通道 M10-API-S-01~S-04 | 5d | P2-M10-00 |
| 7 | **M10-AO** | P2-M10-B | 奥创通道 M10-AO-S-00~S-07（下一切片 **S-00**） | 10d | P2-M10-00 · ADR-045 |
| 8 | **M10-WECOM** | P2-M10-C | 企微通道 M10-WECOM-S-01~S-04 | 5d | P2-M10-00 |
| 9 | **M10-闭环** | P2-M10-04 | COL-S-04 + AO-S-07 + API-S-05~08 + WECOM-S-05 | 5d | P2-M10-A/B/C |
| 10 | **M2-PUB** | P2-M2-PUB-00~03 | 公众号发布：凭证 ADR → Adapter → draft/add → freepublish | 6d | 产品决策 · 与 M10 分离 |

---

## 4. 已完成（合并 Mike + Donny 历史）

| Slice | 完成日 | 待办 | 交付摘要 |
|-------|--------|------|----------|
| S-R19 | 2026-06-10 | 横切 | 4 错码修复 · 250/250 IT |
| S-R20 | 2026-06-10 | 规划 | §15 待办盘点 · 原双人协作机制（已废止） |
| S-R21 | 2026-06-10 | D-1 | author_id 迁移 + KPI 聚合 + Efficiency 真 KPI UI |
| S-R22 | 2026-06-10 | D-7 | Content DELETE + IT + 前端真删除 |
| S-R23 | 2026-06-10 | B-6 | M9 `/oa/system/*` 双路径 + ADR-009 |
| S-R24 | 2026-06-10 | B-7 | 5 页 exportToExcel + P2-#14 |
| S-R25 | 2026-06-10 | P-3/P-4 | unwrapApiData + SYSTEM-001~003 PW 3/3 绿 |

*历史命名对照：S-R21-Mike/S-R21-Donny → 合并记为 S-R21；S-R27a-Mike → S-R27·27a。*

---

## 5. 待办 ID 进度（§15 对照）

| ID | 任务 | 状态 |
|----|------|------|
| D-1 | oa_content.author_id | ✅ |
| D-2~D-6 | M9 系统页后端 | ⬜ 待决策 |
| D-7 | Content delete | ✅ |
| B-6 | M9 路径 prefix | ✅ |
| B-7 | exportToExcel 5 页 | ✅ |
| B-8 | 详情页字段补全 | 🟡 27a ✅ · 27b~27d 待办 |
| P-2 | PW 全量 0 error | ✅ 183/183 |
| P-3 | `as any` 清理（首批） | ✅ |
| P-4 | PW skip 补全 | ✅ |
| X-1~X-5 | 横切合规 | ✅ |

---

## 6. 排期（单人顺序）

| 顺序 | 日 | 任务 |
|------|-----|------|
| **现在** | D0 | S-R27·27b 绩效详情前端接线 |
| +0.5d | D0.5 | S-R27·27c 公司详情全栈 |
| +1d | D1 | S-R27·27d 平台账号统计全栈 |
| +1.5d | D1.5 | P-2 Playwright 全量 |
| +2.5d | D2.5 | S-R26 集成回归 + Gate |

---

## 7. Git 同步日志

| 日期 | 动作 | 结果 | 备注 |
|------|------|------|------|
| 2026-06-10 | Donny `b41a89b` S-R21/S-R25 前端 | ✅ remote | |
| 2026-06-10 | Mike merge + S-R27a `01ac81f` | ✅ remote | 双人最后一次并行合入 |
| 2026-06-10 | 协作模式 → **单人全栈** | 📝 文档 | 本表重写 |

---

## 8. 文档索引

| 文档 | 用途 |
|------|------|
| **本表** | 唯一任务 SSOT |
| [MASTER-EXECUTION-TRACKER.md](./MASTER-EXECUTION-TRACKER.md) | Gate + §15 待办 + **§17 Phase 2 Backlog** |
| [ADR-045](../adr/ADR-045-M10-奥创多账号与设备同步.md) | M10 奥创多账号 · AO-S-00~S-07 |
| [M10-三通道采集计划](./M10-三通道采集计划.md) | 三通道架构 · COL/API/WECOM 切片 |
| [SESSION-PROGRESS.md](./SESSION-PROGRESS.md) | 会话级变更日志 |
| [TASK-PROGRESS-MIKE.md](./TASK-PROGRESS-MIKE.md) | 归档（只读） |
| [TASK-PROGRESS-DONNY.md](./TASK-PROGRESS-DONNY.md) | 归档（只读） |

---

*下一动作：**S-R27·27b** — `PerfRecordDetail` / `PerfUserTrend` 接 S-R27a 真 VO。*
