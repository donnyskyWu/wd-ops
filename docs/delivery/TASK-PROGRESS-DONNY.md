# Donny · 个人任务进度表

> **角色**：产品 / 架构 / 前端验收  
> **文件域**：`ops-platform-ui-vue/**` · `docs/product/**` · Gate 报告 · OQ/ADR 决策  
> **总表**：[TASK-PROGRESS-MASTER.md](./TASK-PROGRESS-MASTER.md)  
> **最后更新**：2026-06-10

---

## 当前进行中

| Slice | 任务 | 文件锁 | 状态 | 下一步 |
|-------|------|--------|------|--------|
| — | 无进行中 | — | — | 见待办队列 #4 P-2 PW 全量 |

---

## 已完成

| Slice | 完成日 | 交付物 | 备注 |
|-------|--------|--------|------|
| **S-R20-Donny** | 2026-06-10 | MASTER §15/§16 · S-R20 报告 · 24 项待办盘点 | 协作机制奠基 |
| **S-R21-Donny** | 2026-06-10 | `Efficiency.vue` · `types/productivity.ts` | D-1 前端闭环：去 ADR-008 占位 + 内容 KPI 真值展示 + 导出列补全 |
| **S-R25-Donny** | 2026-06-10 | `utils/index.ts` · 5 报表页 · `Efficiency` · `sop` · `Perf*` · `p2-modules.spec.ts` | P-3 `unwrapApiData` + 高频 `as any` 清理；P-4 SYSTEM-001~003 3/3 绿 |

*说明：S-R21~R24 后端由 Mike 完成。*

---

## 待办队列（按优先级）

| # | Slice | 待办 ID | 任务 | 预估 | 文件锁 | 依赖 |
|---|-------|---------|------|------|--------|------|
| 1 | ~~S-R21-Donny~~ | D-1 / B-3 | ~~效率页 KPI UI 闭环~~ | — | — | ✅ 2026-06-10 |
| 2 | ~~S-R25-Donny~~ | P-3 | ~~清理高频 `as any`（enum-alias / api 响应）~~ | — | — | ✅ 2026-06-10 |
| 3 | ~~S-R25-Donny~~ | P-4 | ~~Playwright 3 个 skip 测试补全~~ | — | — | ✅ 2026-06-10 |
| 4 | **P-2** | P-2 | Playwright 全量复测 console 0 error | 0.5d | `tests/**` | — |
| 5 | **D-2~D-6** | D-2~D-6 | M9 五页（Param/Dict/Log/LoginLog/Message）Phase 2 决策 | 产品会 | PRD · ADR | 不阻塞 v1.0 |
| 6 | **S-R26-Donny** | — | 集成验收 + Gate 签字 | 1d | `gates/S-R26-*` | Mike S-R26 |

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

*下一动作：P-2 — Playwright 全量复测（183 用例 console 0 error）。*
