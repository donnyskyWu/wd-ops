# Production Environment Variables — ops-server

**Version:** 2026-08-24  
**Profile:** `prod` (`application-prod.yaml`)

---

## Required

| Variable | Example | Used by | Notes |
|----------|---------|---------|-------|
| `NACOS_SERVER_ADDR` | `nacos.prod.internal:8848` | Spring Cloud Nacos | Service discovery |
| `NACOS_PASSWORD` | *(secret)* | Nacos auth | |
| `OPS_DB_HOST` | `mysql.prod.internal` | Datasource | Ops schema host |
| `OPS_DB_PORT` | `3306` | Datasource | Default 3306 |
| `OPS_DB_NAME` | `shenyu-ops` | Datasource | |
| `OPS_DB_USER` | `shenyu-ops` | Datasource | |
| `OPS_DB_PASSWORD` | *(secret)* | Datasource | |
| `REDIS_HOST` | `redis.prod.internal` | Spring Redis + Football Redis | Session / cache |
| `REDIS_PASSWORD` | *(secret)* | Redis | |
| `OA_AES_KEY` | Base64 32-byte key | `oa.crypto.aes-key` | Must match existing prod key for encrypted fields |
| `COLLECTOR_BASE_URL` | `https://ai.shenyu.com/` | Unified collector | M10 external collect |
| `ADMIN_UI_URL` | `https://admin.shenyu.com` | `football.web.admin-ui.url` | Notification links |

---

## Optional (with defaults)

| Variable | Default | Notes |
|----------|---------|-------|
| `NACOS_NAMESPACE` | `prod` | Nacos discovery/config namespace |
| `NACOS_GROUP` | `DEFAULT_GROUP` | |
| `NACOS_USERNAME` | `nacos` | |
| `REDIS_PORT` | `6379` | |
| `REDIS_DATABASE` | `0` | Beta uses `1`; confirm prod |
| `FLYWAY_ENABLED` | `true` | Set `false` if DBA applies SQL manually |
| `COLLECTOR_API_TOKEN` | empty | Required if collector auth enabled |
| `XXL_JOB_ENABLED` | `true` | Set `false` to disable all XXL handlers |
| `XXL_JOB_ADMIN_ADDRESSES` | `http://127.0.0.1:9090/xxl-job-admin` | **Override for prod** — shared mp admin per ADR-070 |
| `XXL_JOB_ACCESS_TOKEN` | `a1b2c3d4e5f67890` | Must match xxl-job-admin token |

---

## YAML-only (not env-interpolated in base prod file)

Set via Nacos config overlay or `--spring.config.additional-location`:

| Key | Purpose | Prod guidance |
|-----|---------|---------------|
| `football.ai.scheme-generate-url` | jingcai async article generate | e.g. `http://ai.author.shenyu.com/api/v1/tasks` |
| `football.ai.scheme-generate-api-key` | AI API key | Secret — from jingcai platform |
| `football.ai.scheme-get-url` | Poll task result | Same host as generate |
| `football.ai.model` | LLM model id | Default `deepseek-v4-flash` |
| `oa.match.internal-base-url` | Match proxy target | Nacos `match-server` or literal prod URL (port 48088) |
| `oa.work-task.win-prediction.enabled` | Enable 红黑 Job | `true` in prod |
| `oa.work-task.win-prediction.match-end-buffer-minutes` | Post-match wait | Default `120` |
| `oa.work-task.win-prediction.scheduled-fallback-enabled` | @Scheduled fallback | **`false` in prod** — use xxl-job only |

### Private domain report (M6 MVP)

No new env vars. Requires **member-server** registered in Nacos prod namespace for Feign:

- Author nickname lookup
- Member user / order aggregation (monthly achievement, weekly funnel)

Verify: `GET /admin-api/ops/private-domain-report/authors` returns code=0 after deploy.

---

## System DB (manual scripts only)

Not consumed by ops-server JVM; for DBA when running menu/dict SQL:

| Placeholder | Description |
|-------------|-------------|
| `{{SYSTEM_DB_HOST}}` | shenyu-system MySQL host |
| `{{SYSTEM_DB_NAME}}` | Usually `shenyu-system` |
| `{{SYSTEM_DB_USER}}` | DBA user with INSERT on system_menu, system_dict_* |

---

## Work-task sys_param placeholders (SQL scripts)

| Placeholder | Default seed | Verify query |
|-------------|--------------|--------------|
| `{{WORK_TASK_DEFAULT_TEMPLATE_ID}}` | `9402` | `SELECT id FROM oa_sop_template WHERE ...` |
| `{{WORK_TASK_DEFAULT_NODE_ID}}` | `9404` | `SELECT id FROM oa_sop_node WHERE node_type='CONTENT_GENERATION'` |

---

## Example systemd / Docker env block

```bash
export SPRING_PROFILES_ACTIVE=prod
export NACOS_SERVER_ADDR=nacos.prod.internal:8848
export NACOS_PASSWORD=***
export OPS_DB_HOST=mysql.prod.internal
export OPS_DB_NAME=shenyu-ops
export OPS_DB_USER=shenyu-ops
export OPS_DB_PASSWORD=***
export REDIS_HOST=redis.prod.internal
export REDIS_PASSWORD=***
export OA_AES_KEY=***
export COLLECTOR_BASE_URL=https://ai.shenyu.com/
export ADMIN_UI_URL=https://admin.shenyu.com
export XXL_JOB_ENABLED=true
export XXL_JOB_ADMIN_ADDRESSES=http://xxl-job.prod.internal:9090/xxl-job-admin
export XXL_JOB_ACCESS_TOKEN=a1b2c3d4e5f67890
```

---

## Security notes

- Never commit real passwords to git
- `OA_AES_KEY` rotation requires re-encrypting sensitive columns — do not change casually
- `XXL_JOB_ACCESS_TOKEN` shared across executors (ADR-070 Q3) — treat as trusted-network secret
