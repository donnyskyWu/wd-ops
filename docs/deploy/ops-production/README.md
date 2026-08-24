# Ops Server — Production Deploy Guide



**Pack version:** 2026-08-24  

**Service:** `ops-server` (`football-module-ops-server`, port `48094`)  

**Flyway SSOT:** `football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/` (latest: **V189**)



---



## 1. What this pack contains



| Area | Contents |

|------|----------|

| **Ops DB** (`shenyu-ops`) | V181–V182 work-task tables/seeds · V184 weekly feedback · V189 drops deferred match pool |

| **System DB** (`shenyu-system`) | Menus 6194–6196 · work-task dicts + V188 LIVE_DRAIN · role_menu |

| **Config** | `application-prod` snippet, env vars, xxl-job registration |

| **Features** | FR-M2-010 工作任务管理 · M6 私域报表 MVP · ADR-070/071/072 |



---



## 2. Database targets



Replace placeholders before execution:



| Placeholder | Example (prod) | Description |

|-------------|----------------|-------------|

| `{{OPS_DB_HOST}}` | `prod-mysql.internal` | Ops MySQL host |

| `{{OPS_DB_NAME}}` | `shenyu-ops` | Ops schema |

| `{{SYSTEM_DB_HOST}}` | same or separate | System MySQL host |

| `{{SYSTEM_DB_NAME}}` | `shenyu-system` | Football system schema |

| `{{WORK_TASK_DEFAULT_TEMPLATE_ID}}` | `9402` | SOP template with CONTENT_GENERATION node — **verify in prod** |

| `{{WORK_TASK_DEFAULT_NODE_ID}}` | `9404` | CONTENT_GENERATION node ID — **verify in prod** |



---



## 3. Deploy order of operations



### Phase A — Pre-deploy (no downtime)



1. **Review** [CHECKLIST.md](./CHECKLIST.md) pre-deploy section

2. **Backup** both `shenyu-ops` and `shenyu-system` (full dump or point-in-time)

3. **Resolve prod template/node IDs:**

   ```sql

   -- On shenyu-ops

   SELECT id, name FROM oa_sop_template WHERE deleted=0 AND tenant_id=1;

   SELECT n.id, n.name, n.node_type, n.template_id

   FROM oa_sop_node n

   WHERE n.node_type='CONTENT_GENERATION' AND n.deleted=0 AND n.tenant_id=1;

   ```

4. Edit `database/04-ops-seeds/002_v181_v182_sys_param.sql` placeholders

5. Confirm **member-server** reachable (private-domain report Feign dependency)



### Phase B — Database (maintenance window or online if Flyway)



**Option 1 — Flyway (recommended when `FLYWAY_ENABLED=true`):**



Deploy new JAR; Flyway applies V181→V188 automatically on ops DB.  

Then **still run** system DB scripts (menus/dicts target `shenyu-system`, not ops Flyway):



```bash

mysql -h {{SYSTEM_DB_HOST}} -u {{SYSTEM_DB_USER}} -p {{SYSTEM_DB_NAME}} \

  < database/02-system-menus/001_v183_work_task_menus.sql

mysql -h {{SYSTEM_DB_HOST}} -u {{SYSTEM_DB_USER}} -p {{SYSTEM_DB_NAME}} \

  < database/03-system-dicts/001_v183_work_task_dicts.sql

mysql -h {{SYSTEM_DB_HOST}} -u {{SYSTEM_DB_USER}} -p {{SYSTEM_DB_NAME}} \

  < database/03-system-dicts/002_v188_work_task_live_drain_dict.sql

```



**Option 2 — Manual DBA (Flyway disabled, e.g. beta pattern):**



Execute in order:



| Step | Script | Database |

|------|--------|----------|

| 1 | `database/01-ops-schema/001_v181_work_task_tables.sql` | shenyu-ops |

| 2 | `database/05-report-schema/001_v184_weekly_feedback.sql` | shenyu-ops |

| 3 | `database/04-ops-seeds/001_v181_ai_prompt.sql` | shenyu-ops |

| 4 | `database/04-ops-seeds/002_v181_v182_sys_param.sql` | shenyu-ops |

| 5 | `database/02-system-menus/001_v183_work_task_menus.sql` | shenyu-system |

| 6 | `database/03-system-dicts/001_v183_work_task_dicts.sql` | shenyu-system |

| 7 | `database/03-system-dicts/002_v188_work_task_live_drain_dict.sql` | shenyu-system |



**Automation reference:**



```bash

# Ops DB (V181+V182)

python scripts/integration-config/apply_v181_work_task.py --target prod  # adapt credentials



# System DB (V183 menus+dicts)

python scripts/integration-config/apply_v183_work_task_fix.py --target prod



# Private-domain report API smoke (post-deploy)

python scripts/integration-config/smoke_private_domain_report_api.py /tmp/prod-verify



# sys_param patch only

python scripts/integration-config/patch_v182_work_task_params.py --target prod

```



> Scripts default to `local`/`test`; create prod env file or pass credentials before use.



**Do NOT apply** V181 menu section (IDs 6176–6178) or V181 §2 wd dict INSERTs. Production SSOT is V183 (6194–6196) on shenyu-system. Match pool (V185–V187) is dropped by V189 — do not deploy match pool scripts.



### Phase C — Application deploy



1. Build/deploy `football-module-ops-server` JAR with `--spring.profiles.active=prod`

2. Set env vars per [config/env-variables.md](./config/env-variables.md)

3. Overlay [config/application-prod.snippet.yaml](./config/application-prod.snippet.yaml) values (Nacos or external config)

4. Verify Nacos registration: `ops-server` + `member-server` in namespace `prod`



### Phase D — XXL-JOB registration



Follow [config/xxl-job-admin-register.md](./config/xxl-job-admin-register.md):



1. Confirm executor `football-ops-executor` appears after ops-server start

2. Register `workTaskWinPredictionJobHandler` cron `0 0 * * * ?`



### Phase E — Post-deploy verification



See [CHECKLIST.md](./CHECKLIST.md) post-deploy section.



---



## 4. Migration version map



| Version | File | Ops DB | System DB |

|---------|------|--------|-----------|

| V181 | `V181__m2_work_task_foundation.sql` | Tables, oa_task column, AI prompt, sys_param (§2/§5 deprecated) | — |

| V182 | `V182__m2_work_task_default_params.sql` | sys_param backfill 9402/9404 | — |

| V183 | `V183__m2_work_task_menu_dict_fix.sql` | Flyway record only | Menus 6194–6196, dicts, role_menu |

| V184 | `V184__m6_private_domain_report_mvp.sql` | `oa_report_weekly_feedback` | — |

| V185–V187 | Match pool (deferred) | Created then dropped by V189 | — |

| V188 | `V188__m2_work_task_marketing_live_drain.sql` | Flyway no-op | `LIVE_DRAIN` dict via manual script |

| V189 | `V189__drop_work_task_match_pool.sql` | DROP match pool tables | — |



**Flyway categorization (V181+):**



| Category | Versions | Notes |

|----------|----------|-------|

| Work task core | V181–V182 | Sheet/assignment tables, sys_param, AI prompt |

| Work task RBAC | V183, V188 dict | System DB manual; avoid Flyway cross-DB writes |

| Work task match pool | V185–V187 → V189 drop | Deferred feature — tables removed |

| Private domain report | V184 | MVP weekly feedback only; monthly/weekly are read APIs |



---



## 5. Key dependencies



| Dependency | Config key | Required for |

|------------|------------|--------------|

| match-server | `oa.match.internal-base-url` | Work-task 赛事选择 + 红黑赛果 |

| member-server | Nacos discovery | 私域报表 MVP（作者/订单/用户 Feign） |

| jingcai AI | `football.ai.scheme-generate-url` | AI 内容生成（任务正文） |

| xxl-job-admin | `xxl.job.admin.addresses` | 红黑 hourly Job |

| shenyu-system Redis | `oa.auth.football-redis.*` | Football 登录态 |

| Nacos | `NACOS_SERVER_ADDR` | 服务发现 |



---



## 6. Feature notes



### M6 私域报表 MVP



| Report | Route | API | DB |

|--------|-------|-----|-----|

| IP业务月达成 | `/ops/analysis/report/monthly-achievement` | `GET /ops/private-domain-report/monthly-achievement` | No extra DDL |

| 周度私域转化 | `/ops/analysis/report/weekly-funnel` | `GET /ops/private-domain-report/weekly-funnel` | No extra DDL |

| 销售反馈 U 列 | (inline on weekly page) | `PUT/GET /ops/private-domain-report/weekly-feedback` | `oa_report_weekly_feedback` (V184) |



- UI entry: **6126 数据报表** → Report Center cards (`oa:report:list`); no new system_menu rows

- Authors source: `oa_ip_group_anchor_rel` + member AuthorApi



### FR-M2-010 工作任务（V183+ updates）



| Menu ID | Name | Permission | Roles |

|---------|------|------------|-------|

| 6194 | 工作任务管理 | `ops:work-task:list` | admin, ip_group_leader |

| 6195 | 工作任务登记 | `ops:work-task:register` | admin, ip_group_leader |

| 6196 | 工作任务管理矩阵 | `ops:work-task:manage` | admin, ip_group_leader |



> **Menu ID note:** Beta env may have occupied 6176–6190; prod uses 6194–6196. See `fix_local_work_task_menu.sql` for local dev parity.



---



## 7. Related ADRs / Spec



- [ADR-070](../../adr/ADR-070-Ops抓取统一XXL-JOB调度.md) — XXL-JOB executor `football-ops-executor`

- [ADR-071](../../adr/ADR-071-工作任务登记轻量Task生成.md) — confirm → oa_task

- [ADR-072](../../adr/ADR-072-工作任务红黑判定与AI提示词.md) — WIN_PREDICTION Job + AI prompt

- [CHECKLIST-M2](../../delivery/CHECKLIST-M2-内容生产.md) § FR-M2-010

- E2E: `docs/delivery/e2e-artifacts/PRIVATE-DOMAIN-REPORT-E2E-20260821/`



---



## 8. Folder layout



```

docs/deploy/ops-production/

├── README.md                 ← this file

├── CHECKLIST.md

├── database/

│   ├── 01-ops-schema/        ← V181 work-task tables

│   ├── 02-system-menus/      ← V183 (6194–6196)

│   ├── 03-system-dicts/      ← V183 work-task + V188 LIVE_DRAIN

│   ├── 04-ops-seeds/

│   ├── 05-report-schema/     ← V184 weekly feedback

│   ├── 06-report-seeds/      ← README (no seeds)

│   └── ROLLBACK-NOTES.md

├── config/

│   ├── application-prod.snippet.yaml

│   ├── env-variables.md

│   └── xxl-job-admin-register.md

└── manifests/

    └── CHANGELOG-ops-to-prod.md

```


