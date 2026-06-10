# Donny · 个人任务进度表

> **角色**：产品 / 架构 / 前端验收  
> **文件域**：`ops-platform-ui-vue/**` · `docs/product/**` · Gate 报告 · OQ/ADR 决策  
> **总表**：[TASK-PROGRESS-MASTER.md](./TASK-PROGRESS-MASTER.md)  
> **最后更新**：2026-06-10 · merge S-R21/S-R25

---

## 当前进行中

| Slice | 任务 | 文件锁 | 状态 | 下一步 |
|-------|------|--------|------|--------|
| **S-R27-Donny** | B-8 M3 详情/趋势接 workflowStatus + userInfo | `PerfRecordDetail.vue` · `PerfUserTrend.vue` | ⬜ | S-R27a 后端已合入 |

---

## 已完成

| Slice | 完成日 | 交付物 | 备注 |
|-------|--------|--------|------|
| **S-R20-Donny** | 2026-06-10 | MASTER §15/§16 · S-R20 报告 | 协作机制奠基 |
| **S-R21-Donny** | 2026-06-10 | `Efficiency.vue` · `types/productivity.ts` | D-1 前端闭环 |
| **S-R25-Donny** | 2026-06-10 | `utils/index.ts` · 报表页 · `p2-modules.spec.ts` 等 | P-3/P-4 完成 |

*说明：S-R21~R24 后端、S-R27a 由 Mike 完成。*

---

## 待办队列（按优先级）

| # | Slice | 待办 ID | 任务 | 预估 | 文件锁 | 依赖 |
|---|-------|---------|------|------|--------|------|
| 1 | **S-R27-Donny** | B-8 | 绩效详情/趋势接真 VO | 0.5d | `PerfRecordDetail.vue` · `PerfUserTrend.vue` | S-R27a ✅ |
| 2 | **P-2** | P-2 | Playwright 全量复测 console 0 error | 0.5d | `tests/**` | S-R27-Donny |
| 3 | **D-2~D-6** | D-2~D-6 | M9 五页 Phase 2 决策 | 产品会 | PRD · ADR | 不阻塞 v1.0 |
| 4 | **S-R26-Donny** | — | 集成验收 + Gate 签字 | 1d | `gates/S-R26-*` | Wave-3 |

**禁止与 Mike 并行改的文件**：
- `yudao-server/**` · `V31__*.sql` · `ProductionContentController.java`

---

## Donny 自检清单（每个 slice 结束前）

- [ ] 6 字段对照表（api URL ↔ controller ↔ 前端 params）
- [ ] 浏览器 / Playwright 复测通过
- [ ] 更新 SESSION-PROGRESS + 本表 + MASTER 总表
- [ ] `git pull` → commit → `git push`

---

## 待 Donny 决策（OQ）

| ID | 问题 | 建议 |
|----|------|------|
| D-2~D-6 | M9 五页后端 | Phase 2 |

---

*下一动作：S-R27-Donny — `workflowStatus` / `userInfo` 接线，去掉审批 mock。*
