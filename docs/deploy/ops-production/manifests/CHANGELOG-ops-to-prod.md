# Changelog — Ops to Production Deploy Pack



**Pack date:** 2026-08-24  

**Target:** ops-server production  

**Flyway ceiling:** V189



---



## Features included



### FR-M2-010 工作任务管理 (S-16 ~ S-19)



| Slice | Deliverable | DB / Config |

|-------|-------------|-------------|

| S-16 基础 | `oa_work_task_sheet` + `oa_work_task_assignment` tables; Controller skeleton | V181 |

| S-17 登记 | get-or-create / save / confirm / withdraw; Tab1 UI | V181 + V182 sys_param |

| S-18 矩阵 | matrix / summary API; Tab2 matrix table | — |

| S-19 红黑 Job | `workTaskWinPredictionJobHandler`; AI extract; refresh API | V181 AI prompt + xxl-job |

| V188 营销计划 | `LIVE_DRAIN` 直播引流 dict option | V188 system dict |



**Business capabilities:**



- IP 组长按日登记「赛事 × 作者 × 营销计划」

- 确认后轻量生成 `oa_task`（无 `oa_content_plan`，ADR-071）

- 矩阵视图展示红黑预测

- 赛后 hourly Job AI 抽取预测 vs 实际赛果（ADR-072）



### M6 私域报表 MVP (2026-08-21 E2E verified)



| Report | Route | Persisted data |

|--------|-------|----------------|

| IP业务月达成 | `/ops/analysis/report/monthly-achievement` | None (Feign aggregation) |

| 周度私域转化 | `/ops/analysis/report/weekly-funnel` | None (Feign aggregation) |

| 销售反馈 U 列 | inline on weekly page | `oa_report_weekly_feedback` (V184) |



**Dependencies:** member-server (author/order/user APIs), `oa_ip_group_anchor_rel`, menu 6126 `oa:report:list`



---



## Database migrations



| Version | Description | Ops DB | System DB |

|---------|-------------|--------|-----------|

| **V181** | Work task foundation — tables, oa_task column, AI prompt, sys_param (§2/§5 deprecated) | Yes | No |

| **V182** | Backfill `work_task.default_template_id` / `default_node_id` → 9402/9404 | Yes | No |

| **V183** | Menu ID fix 6194–6196; dict SSOT to shenyu-system; ip_group_leader role_menu | Flyway record | Yes |

| **V184** | `oa_report_weekly_feedback` for weekly U-column persistence | Yes | No |

| **V185–V187** | Match pool (deferred — dropped by V189) | Yes | No |

| **V188** | LIVE_DRAIN dict — Flyway no-op on ops | No | Yes (manual) |

| **V189** | DROP match pool tables | Yes | No |



---



## ADR references



| ADR | Title | Prod impact |

|-----|-------|-------------|

| [ADR-070](../../adr/ADR-070-Ops抓取统一XXL-JOB调度.md) | XXL-JOB unified scheduling | Executor `football-ops-executor`; collect/monitor handlers planned |

| [ADR-071](../../adr/ADR-071-工作任务登记轻量Task生成.md) | Light oa_task generation | `oa_task.work_task_assignment_id`; confirm without plan |

| [ADR-072](../../adr/ADR-072-工作任务红黑判定与AI提示词.md) | Win/loss prediction | `WORK_TASK_WIN_PREDICTION` prompt; hourly Job |



---



## Match proxy (no schema change)



- `MatchProxyService` replaces legacy `oa.match.api-base-url`

- Config: `oa.match.internal-base-url` → match-server admin API

- Used by: work-task 赛事选择, 红黑赛果 `getFinishedMatchResult`

- Deploy: config only; ensure match-server in Nacos prod namespace



---



## Menus & permissions (system DB)



| ID | Name | Permission | Roles |

|----|------|------------|-------|

| 6126 | 数据报表 | `oa:report:list` | *(existing)* — Report Center entry for 私域报表 |

| 6194 | 工作任务管理 | `ops:work-task:list` | admin, ip_group_leader |

| 6195 | 工作任务登记 | `ops:work-task:register` | admin, ip_group_leader |

| 6196 | 工作任务管理矩阵 | `ops:work-task:manage` | admin, ip_group_leader |



Parent menus: **6102 内容生产** (work-task) · **6103 数据分析** (report center via 6126)



> **ID collision note:** Beta may use 6176–6178 for work-task; prod SSOT is 6194–6196 per V183.



---



## Dictionaries (system DB)



| Type | Values |

|------|--------|

| `dict_marketing_plan_type` | LIVE_PUBLIC, PAID_SALES, **LIVE_DRAIN** (V188) |

| `dict_sales_platform` | PRIVATE, KUAISHOU, DOUYIN, NONE |

| `dict_win_prediction` | UNKNOWN, RED, BLACK |

| `dict_work_task_sheet_status` | DRAFT, CONFIRMED |



---



## Seeds (ops DB)



| Object | Key / Scene | Notes |

|--------|-------------|-------|

| `oa_ai_prompt_config` | scene=`WORK_TASK_WIN_PREDICTION` | ADR-072 AI extract template |

| `sys_param` | `work_task.default_template_id` | Must point to prod CONTENT_GENERATION SOP |

| `sys_param` | `work_task.default_node_id` | Must be CONTENT_GENERATION node type |



**No report seeds** — see `database/06-report-seeds/README.md`



---



## XXL-JOB handlers



| Handler | Status | Cron |

|---------|--------|------|

| `workTaskWinPredictionJobHandler` | **Implemented** | `0 0 * * * ?` |

| `collectCronScanJobHandler` | ADR-070 planned | `0 * * * * ?` |

| `monitorAlertScanJobHandler` | ADR-070 planned | `0 0/30 * * * ?` |



---



## Out of scope (this pack)



- M10 full collect Slice implementation (handlers not in current Java tree)

- Frontend build/deploy (assumes routes already in prod admin UI bundle)

- shenyu-system schema migrations unrelated to work-task / reports

- Login page / external SSO (Phase 2 out of scope per project rules)

- Dedicated system_menu rows for 月达成/周转化 (use Report Center cards + `oa:report:list`)



---



## Source files (SSOT)



```

football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/

  V181__m2_work_task_foundation.sql

  V182__m2_work_task_default_params.sql

  V183__m2_work_task_menu_dict_fix.sql

  V184__m6_private_domain_report_mvp.sql

  V185__m2_work_task_match_pool.sql          ← deferred; dropped by V189

  V186__m2_work_task_match_pool_per_day.sql

  V187__m2_work_task_match_pool_unique_fix.sql

  V188__m2_work_task_marketing_live_drain.sql

  V189__drop_work_task_match_pool.sql



scripts/integration-config/

  apply_v181_work_task.py

  apply_v183_work_task_fix.py

  patch_v182_work_task_params.py

  smoke_work_task_api.py

  smoke_private_domain_report_api.py

  fix_local_work_task_menu.sql          ← local dev parity (6194–6196 + dicts)

```



---



## Verification artifacts



| Feature | E2E evidence |

|---------|--------------|

| Work task | `docs/delivery/e2e-artifacts/WORK-TASK-E2E-20260819/` |

| Private domain report | `docs/delivery/e2e-artifacts/PRIVATE-DOMAIN-REPORT-E2E-20260821/` |



Checklist: `docs/delivery/CHECKLIST-M2-内容生产.md` § FR-M2-010



---



## Pack update 2026-08-24



Added to `docs/deploy/ops-production/`:



- `database/05-report-schema/001_v184_weekly_feedback.sql`

- `database/03-system-dicts/002_v188_work_task_live_drain_dict.sql`

- `database/06-report-seeds/README.md`



Updated: README, CHECKLIST, ROLLBACK-NOTES, env-variables


