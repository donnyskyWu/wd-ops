# 双人协作 · 总任务进度表（Mike + Donny）

> **性质**：协作 SSOT · 汇总 Mike / Donny 个人表，防文件冲突  
> **维护**：每完成 1 个 slice → 更新本表 + 对应个人表 + `SESSION-PROGRESS.md`  
> **个人表**：[Mike](./TASK-PROGRESS-MIKE.md) · [Donny](./TASK-PROGRESS-DONNY.md)  
> **最后更新**：2026-06-10 · S-R21~R24 批次

---

## 0. 协作机制（必读）

### 0.1 角色

| 成员 | 角色 | 主责文件域 | 禁止擅自改动 |
|------|------|-----------|-------------|
| **Mike** | 后端 TL + 实现 | `yudao-server/**` · 后端 IT · Flyway · ADR（技术向）| Donny 负责的 PRD 决策结论未定时改 spec |
| **Donny** | 产品 / 架构 / 验收 | `ops-platform-ui-vue/**` · `docs/product/**` · Gate 签字 · OQ 决策 | Mike 进行中的 migration / Controller 未合并前改同文件 |

### 0.2 每日流程

```
1. git pull origin main
2. 各看个人表「进行中」slice，确认文件范围无重叠
3. 开发 → 自测（Mike: mvn test / Donny: Playwright 或走查）
4. 更新 TASK-PROGRESS-{NAME}.md + 本表 + SESSION-PROGRESS.md
5. commit → push（message: S-R{N}-{Name}: 简述）
6. 另一方 pull；若有冲突 → 按 §0.4 处理并 @对方
```

### 0.3 Slice 命名

`S-R{N}-{Mike|Donny}` — 文件范围写在个人表「文件锁」列。

### 0.4 冲突处理

| 情况 | 处理 |
|------|------|
| 同文件两人改 | 先 push 者保留；后 pull 者 rebase/merge，**禁止 force push main** |
| 后端新增字段 | Mike 先合 migration + API → Donny 再改 `types` + `api` + `.vue` |
| 产品决策变更 | Donny 更新 ADR/PRD → Mike 按 ADR 实现 |
| push 失败 | 在 SESSION-PROGRESS 记阻塞，双方对齐后再推 |

---

## 1. 阶段总览

| 阶段 | 目标 | 状态 | 目标日 |
|------|------|------|--------|
| **Wave-1** | D-1 / D-7 / B-6 / B-7 补强 | ✅ 完成 | 2026-06-10 |
| **Wave-2** | D-1 前端闭环 + 工程债 P-3/P-4 | ✅ 完成 → P-2 | D+3 |
| **Wave-3** | B-8 详情页字段 + P-2 全量 PW | ⬜ 待开始 | D+5 |
| **Wave-4** | S-R26 集成回归 + 上线决策 | ⬜ 待开始 | D+7 |
| **Phase-2** | D-2~D-6 M9 系统页后端 | ⬜ 待 Donny 决策 | TBD |

---

## 2. Slice 总表（防冲突）

| Slice | 负责人 | 待办 ID | 简述 | 文件锁（不可重叠） | 状态 | 依赖 |
|-------|--------|---------|------|-------------------|------|------|
| S-R21-Mike | Mike | D-1 | `oa_content.author_id` + 人效 KPI 聚合 | `V31__*.sql` · `ContentDO` · `ContentMapper` · `ProductivityReview*` · `M1ContentAuthorIdIT` | ✅ | ADR-008 |
| S-R22-Mike | Mike | D-7 | Content DELETE + IT | `ProductionContentController/Service*` · `M2ContentDeleteIT` · `api/content.ts`（删除） | ✅ | — |
| S-R23-Mike | Mike | B-6 | M9 `/oa/system/*` 双路径 | `*Controller.java`(system) · `system-*.ts` · `M9*IT` · ADR-009 | ✅ | — |
| S-R24-Mike | Mike | B-7 | 5 页 exportToExcel + P2-#14 | 5 个 `.vue` · `ux-p0-p1-p2-regression.spec.ts` | ✅ | — |
| **S-R25-Donny** | Donny | P-3/P-4 | `as any` 清理 + PW skip 补全 | `ops-platform-ui-vue/src/**`（不含 `yudao-server`） | ✅ | — |
| **S-R21-Donny** | Donny | D-1/B-3 | 效率页去 ADR 占位 + 真 KPI 展示 | `Efficiency.vue` · `types/productivity.ts` | ✅ | S-R21-Mike ✅ |
| **S-R27-Mike** | Mike | B-8 | 详情页 assignee/userOwner 字段 | `yudao-server` M3/M4 相关 VO/Mapper/IT | ⬜ | — |
| **S-R26** | Mike+Donny | — | 集成回归 mvn verify + PW 全量 | 全库 | ⬜ | Wave-2/3 ✅ |
| **D-2~D-6** | Donny | D-2~D-6 | M9 五页是否 Phase 2 | PRD · ADR · MASTER §15 | ⬜ 待决策 | 产品会 |

---

## 3. 待办 ID 进度（§15 对照）

| ID | 任务 | 负责人 | 状态 |
|----|------|--------|------|
| D-1 | oa_content.author_id | Mike 后端 ✅ · Donny 前端 ✅ | ✅ |
| D-2~D-6 | M9 系统页后端 | Donny 决策 | ⬜ |
| D-7 | Content delete | Mike | ✅ |
| B-6 | M9 路径 prefix | Mike | ✅ |
| B-7 | exportToExcel 5 页 | Mike | ✅ |
| B-8 | 详情页 assignee/userOwner | Mike | ⬜ |
| P-2 | PW 全量 0 error | Donny | ⬜ |
| P-3 | `as any` 清理 | Donny | ✅ |
| P-4 | PW skip 补全 | Donny | ✅ |
| X-1~X-5 | 横切合规 | 已完成 | ✅ |

---

## 4. 并行排期（Wave-2 起）

| 时段 | Mike | Donny | 同步点 |
|------|------|-------|--------|
| **现在** | S-R27-Mike 调研 B-8 | S-R21-Donny 效率页 KPI UI | 每日 stand-up |
| **+1d** | S-R27 实现 + IT | S-R25-Donny P-3 第一批 | Mike push 后 Donny pull |
| **+2d** | mvn verify 子集 | S-R25 P-4 + PW 局部 | — |
| **+3d** | — | P-2 PW 全量复测 | 记 SESSION-PROGRESS |
| **+5d** | **S-R26** 后端回归 | **S-R26** 前端回归 | Donny Gate 签字 |
| **+7d** | — | 上线决策会 | OVERVIEW 更新 |

---

## 5. Git 同步日志

| 日期 | 操作人 | 动作 | 结果 | 备注 |
|------|--------|------|------|------|
| 2026-06-10 | Mike | `git pull` | ✅ Already up to date | 无冲突 |
| 2026-06-10 | Mike | commit `be5594f` S-R21~R24 + 任务表 | ✅ 本地已提交 | 44 files |
| 2026-06-10 | Mike | `git push origin main` | ⏸ 待人工 push | 环境拦截 protected main；Donny 本地 `git pull` 后 push |

---

## 6. 文档索引

| 文档 | 用途 |
|------|------|
| [TASK-PROGRESS-MIKE.md](./TASK-PROGRESS-MIKE.md) | Mike 个人任务（仅后端域为主） |
| [TASK-PROGRESS-DONNY.md](./TASK-PROGRESS-DONNY.md) | Donny 个人任务（前端+产品） |
| [MASTER-EXECUTION-TRACKER.md](./MASTER-EXECUTION-TRACKER.md) | Gate + §15 待办 SSOT |
| [SESSION-PROGRESS.md](./SESSION-PROGRESS.md) | 会话级变更日志 |
| [deliverable-OVERVIEW-20260610.md](../deliverable-OVERVIEW-20260610.md) | 总览与方法论 |

---

*维护：完成 slice 后 10 分钟内更新 §2 状态 + 个人表 + §5 Git 日志。*
