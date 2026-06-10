# Mike · 个人任务进度表

> **角色**：后端 TL（实现）  
> **文件域**：`yudao-server/yudao-module-oa/**` · Flyway · 后端 IT · 技术 ADR  
> **总表**：[TASK-PROGRESS-MASTER.md](./TASK-PROGRESS-MASTER.md)  
> **最后更新**：2026-06-10

---

## 当前进行中

| Slice | 任务 | 文件锁 | 状态 | 下一步 |
|-------|------|--------|------|--------|
| **S-R27a-Mike** | M3 详情 + 趋势 userInfo | `PerfRecord*` · `PerfTrendVO` · `M3PerfDetailEnrichIT` | ✅ | 待 Donny S-R27 前端接线 |

---

## 已完成

| Slice | 完成日 | 待办 ID | 交付物 | 验证 |
|-------|--------|---------|--------|------|
| **S-R21-Mike** | 2026-06-10 | D-1 | `V31__m1_oa_content_author_id.sql` · `ContentDO.authorId` · `ContentMapper.sumByUser` · `ProductivityReviewServiceImpl` · `M1ContentAuthorIdIT` · ADR-008→已实施 | IT 已写（待 `mvn test` 环境验证） |
| **S-R22-Mike** | 2026-06-10 | D-7 | `DELETE /production/content/{id}` · `M2ContentDeleteIT` · `api/content.ts` delete · `content/index.vue` 真删除 | IT 3 用例 |
| **S-R23-Mike** | 2026-06-10 | B-6 | M9 双路径 `/oa/system/*` + legacy · `M9SystemPathPrefixIT` · ADR-009 | IT + 前端 path 迁移 |
| **S-R24-Mike** | 2026-06-10 | B-7 | 5 页 + FansAnalysis `exportToExcel` · P2-#14 `missing===0` | Playwright 2 passed |
| **S-R27a-Mike** | 2026-06-10 | B-8 子集 | `PerfRecordDetailVO` / `PerfTrendVO` enrich · `M3PerfDetailEnrichIT` 3 用例 | mvn test |

---

## 待办队列（按优先级）

| # | Slice | 待办 | 预估 | 文件锁 | 依赖 |
|---|-------|------|------|--------|------|
| 1 | **S-R27b-Mike** | B-8 公司详情 + 扩容历史端点 | 0.5d | `CompanyController` · `M4Company*IT` | — |
| 2 | **S-R27c-Mike** | B-8 平台账号 followerCount/workCount | 0.5d | `AccountRespVO` · `PlatformAccount*` | — |
| 3 | **S-R26-Mike** | 全量 `mvn -pl yudao-module-oa test` | 0.5d | 全后端 | Wave-2/3 完成 |
| 3 | **联调** | 配合 S-R21-Donny 验收 author_id KPI | 0.5d | 只读 Efficiency API | Donny S-R21 |

**不进入 Mike 队列（Donny 负责）**：S-R25 前端 · D-2~D-6 产品决策 · P-2 Playwright 全量

---

## Mike 自检清单（每个 slice 结束前）

- [ ] Flyway 版本号递增且无冲突
- [ ] `OaErrorCodes` 无重复 code
- [ ] IT 自包含 seed（JdbcTemplate）
- [ ] API-M*.md / ADR 已同步
- [ ] 更新 MASTER §1.1 + 本表 + MASTER 总表
- [ ] `git pull` → commit → `git push`

---

## 阻塞 / 风险

| 日期 | 项 | 状态 |
|------|-----|------|
| 2026-06-10 | 本地 shell `mvn` 未在 PATH，IT 未实跑 | 待 CI 或本机补跑 |
| 2026-06-10 | `git pull` 网络 reset | push 前重试 |

---

*下一动作：S-R27b-Mike（公司详情）或协助 Donny 接 S-R27-Donny 前端。*
