# Donny · 个人任务进度表

> **角色**：产品 / 架构 / 前端验收  
> **文件域**：`ops-platform-ui-vue/**` · `docs/product/**` · Gate 报告 · OQ/ADR 决策  
> **总表**：[TASK-PROGRESS-MASTER.md](./TASK-PROGRESS-MASTER.md)  
> **最后更新**：2026-06-10

---

## 当前进行中

| Slice | 任务 | 文件锁 | 状态 | 下一步 |
|-------|------|--------|------|--------|
| **S-R27-Donny** | B-8 前端接线（M3 详情/趋势） | `PerfRecordDetail.vue` · `PerfUserTrend.vue` | ⬜ | 依赖 S-R27a-Mike ✅ |

---

## 已完成

| Slice | 完成日 | 交付物 | 备注 |
|-------|--------|--------|------|
| **S-R20-Donny** | 2026-06-10 | MASTER §15/§16 · S-R20 报告 · 24 项待办盘点 | 协作机制奠基 |

*说明：S-R21~R24 实现由 Mike 在本会话完成；Donny 侧 D-1 前端闭环与 S-R25 仍待办。*

---

## 待办队列（按优先级）

| # | Slice | 待办 ID | 任务 | 预估 | 文件锁 | 依赖 |
|---|-------|---------|------|------|--------|------|
| 1 | **S-R27-Donny** | B-8 | 绩效详情/趋势接真 VO（workflowStatus/userInfo） | 0.5d | `PerfRecordDetail.vue` · `PerfUserTrend.vue` | S-R27a ✅ |
| 2 | **S-R21-Donny** | D-1 / B-3 | `Efficiency.vue` 去掉 ADR-008 占位；真 KPI | 0.5~1d | `Efficiency.vue` | S-R21-Mike ✅ |
| 3 | **S-R25-Donny** | P-3 | 清理高频 `as any`（enum-alias / api 响应） | 1d | `src/views/**` · `src/api/**` | — |
| 4 | **S-R25-Donny** | P-4 | Playwright 3 个 skip 测试补全 | 0.5d | `tests/**` | — |
| 5 | **P-2** | P-2 | Playwright 全量复测 console 0 error | 0.5d | `tests/**` | S-R25 |
| 6 | **D-2~D-6** | D-2~D-6 | M9 五页 Phase 2 决策 | 产品会 | PRD · ADR | 不阻塞 v1.0 |
| 7 | **S-R26-Donny** | — | 集成验收 + Gate 签字 | 1d | `gates/S-R26-*` | Mike S-R26 |

**禁止与 Mike 并行改的文件**（Mike 已合入 main 前勿动）：
- `yudao-server/**`
- `V31__*.sql`
- `ProductionContentController.java`

---

## Donny 自检清单（每个 slice 结束前）

- [ ] 6 字段对照表（api URL ↔ controller ↔ 前端 params）
- [ ] 浏览器 / Playwright 复测通过
- [ ] PRD / UX spec 与实现一致
- [ ] 更新 SESSION-PROGRESS + 本表 + MASTER 总表
- [ ] `git pull` → commit → `git push`

---

## 待 Donny 决策（OQ）

| ID | 问题 | 选项 | 建议 |
|----|------|------|------|
| D-2 | ParamManage 后端 | 本期 / Phase 2 | Phase 2 |
| D-3 | DictManage CRUD | 本期 / Phase 2 | Phase 2 |
| D-4 | LogManage | 本期 / Phase 2 | Phase 2 |
| D-5 | LoginLog | 本期 / Phase 2 | Phase 2 |
| D-6 | MessageManage | 本期 / Phase 2 | Phase 2 |

---

## 阻塞 / 风险

| 日期 | 项 | 状态 |
|------|-----|------|
| — | 无 | — |

---

*下一动作：S-R21-Donny — 效率页 KPI UI 与 Mike 后端 author_id 对齐验收。*
