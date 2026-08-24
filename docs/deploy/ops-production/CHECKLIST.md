# Ops Production Deploy Checklist



**Version:** 2026-08-24  

**Service:** ops-server · **Pack:** [README.md](./README.md)



---



## Pre-deploy



### Infrastructure



- [ ] MySQL `shenyu-ops` reachable from ops-server host

- [ ] MySQL `shenyu-system` reachable (menus/dicts if manual apply)

- [ ] Redis (Football session) reachable

- [ ] Nacos `prod` namespace configured

- [ ] xxl-job-admin reachable (shared with mp per ADR-070)

- [ ] match-server registered in Nacos (for `oa.match.internal-base-url`)

- [ ] **member-server** registered in Nacos (for private-domain report Feign)

- [ ] jingcai AI endpoint reachable (`football.ai.scheme-generate-url`)



### Secrets & env



- [ ] `OPS_DB_*` credentials provisioned (least privilege)

- [ ] `REDIS_PASSWORD` set

- [ ] `OA_AES_KEY` matches existing prod encryption key

- [ ] `NACOS_USERNAME` / `NACOS_PASSWORD` set

- [ ] `XXL_JOB_ACCESS_TOKEN` matches admin (default `a1b2c3d4e5f67890` — confirm with ops)

- [ ] `COLLECTOR_BASE_URL` + `COLLECTOR_API_TOKEN` if M10 collect enabled

- [ ] `ADMIN_UI_URL` points to prod Football admin UI



### Database prep



- [ ] Full backup `shenyu-ops` + `shenyu-system` completed

- [ ] Confirmed Flyway strategy: `FLYWAY_ENABLED=true` (auto) vs manual scripts

- [ ] Verified prod SOP template/node IDs for `work_task.default_*` params

- [ ] Checked menu ID 6194–6196 not occupied in prod `system_menu`

- [ ] Checked no duplicate `ops:work-task:*` permissions already exist

- [ ] Confirmed `oa_ip_group_anchor_rel` has anchor data for report authors



### Code / artifact



- [ ] JAR built from branch containing V181–V189 migrations

- [ ] Frontend deployed with routes:

  - `ops/production/work-task/index` (WorkTask)

  - `ops/analysis/report/monthly-achievement`

  - `ops/analysis/report/weekly-funnel`

- [ ] Review [CHANGELOG-ops-to-prod.md](./manifests/CHANGELOG-ops-to-prod.md)



---



## Deploy execution



### Database — Work task (V181–V183, V188–V189)



- [ ] **Ops DB:** V181 schema applied (`oa_work_task_sheet`, `oa_work_task_assignment`, `oa_task.work_task_assignment_id`)

- [ ] **Ops DB:** V189 drops match pool tables if V185–V187 were previously applied

- [ ] **Ops DB:** AI prompt `WORK_TASK_WIN_PREDICTION` seeded

- [ ] **Ops DB:** `work_task.default_template_id` / `work_task.default_node_id` set to valid prod IDs

- [ ] **System DB:** Menus 6194–6196 inserted (V183)

- [ ] **System DB:** 4 dict types + 11 dict data rows inserted (V183)

- [ ] **System DB:** `LIVE_DRAIN` marketing plan dict (V188)

- [ ] Flyway history records 181–189 (if using Flyway)



### Database — Private domain report (V184)



- [ ] **Ops DB:** `oa_report_weekly_feedback` table exists (V184)



### Application



- [ ] ops-server started with `--spring.profiles.active=prod`

- [ ] Health check `GET /actuator/health` → UP

- [ ] Nacos shows `ops-server` + `member-server` instances healthy

- [ ] Logs: no Flyway migration errors

- [ ] Logs: `XxlJobExecutor` initialized (if `XXL_JOB_ENABLED=true`)

- [ ] `oa.auth.dev-token.enabled=false` confirmed



### XXL-JOB



- [ ] Executor `football-ops-executor` registered in admin

- [ ] Job `workTaskWinPredictionJobHandler` created, cron `0 0 * * * ?`

- [ ] Job enabled; test trigger once → success in admin log

- [ ] `oa.work-task.win-prediction.scheduled-fallback-enabled=false` in prod



### Config spot-check



- [ ] `oa.match.internal-base-url` → prod match-server (Nacos or env)

- [ ] `football.ai.scheme-generate-url` / `scheme-generate-api-key` set for prod jingcai

- [ ] `oa.work-task.win-prediction.enabled=true`

- [ ] `oa.work-task.win-prediction.match-end-buffer-minutes=120`



---



## Post-deploy verification



### Smoke — Work task API



- [ ] Login as admin → menu「工作任务管理」visible under 内容生产

- [ ] Login as `ip_group_leader` → same menu visible

- [ ] `GET /admin-api/ops/work-task/sheet/get-or-create?ipGroupId=&workDate=` → 200

- [ ] Dict APIs return work-task enums (marketing plan incl. LIVE_DRAIN, sales platform, win prediction, sheet status)

- [ ] Match proxy: `GET /admin-api/ops/match/...` returns leagues/matches (no 502)

- [ ] `GET /admin-api/ops/work-task/matrix` → 200 (no 1146 table missing)



### Smoke — Private domain report API



- [ ] `GET /admin-api/ops/private-domain-report/authors` → code=0

- [ ] `GET /admin-api/ops/private-domain-report/monthly-achievement?month=YYYY-MM` → code=0

- [ ] `GET /admin-api/ops/private-domain-report/weekly-funnel?weekStart=&weekEnd=` → code=0

- [ ] `PUT/GET /admin-api/ops/private-domain-report/weekly-feedback` → code=0 (U 列读写)



### Smoke — UI (P0)



- [ ] Report Center (`/ops/analysis/data-report`) shows 月达成 + 周转化 cards

- [ ] 月达成 page loads table with MVP columns

- [ ] 周度转化 page loads + feedback save works

- [ ] Create/save work-task sheet (Tab1 登记)

- [ ] Confirm sheet → `oa_task` rows created with `work_task_assignment_id` set

- [ ] Matrix view (Tab2) renders author columns



### Monitoring



- [ ] xxl-job execution log path writable: `${user.home}/logs/xxl-job/ops-server`

- [ ] Application log no repeated `MatchProxy` connection errors

- [ ] No 1501/1504 errors in work-task save (user/tenant validation)



---



## Rollback triggers



Initiate rollback per [database/ROLLBACK-NOTES.md](./database/ROLLBACK-NOTES.md) if:



- Confirm flow fails for all users (sys_param template/node invalid)

- Menu permission 403 for all roles

- Flyway migration partial failure

- Match proxy down blocks all work-task registration

- Private-domain report 500 on weekly-feedback (missing V184 table)



---



## Sign-off



| Role | Name | Date | OK |

|------|------|------|-----|

| DBA | | | |

| Ops / SRE | | | |

| Dev lead | | | |

| Product | | | |


