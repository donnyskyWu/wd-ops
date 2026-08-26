# COLLECTOR-TOKEN-ROTATE-20260807

**Status**: PASS  
**Date**: 2026-08-07

## Scope

Rotate unify-collector `API_TOKEN` / OPS `COLLECTOR_API_TOKEN` for Beta (`start-ops-dev.ps1 -Beta`), then E2E-verify auth.

## Files updated

| File | Change |
|------|--------|
| `scripts/integration-config/ops-test-remote.env` (gitignored) | `COLLECTOR_API_TOKEN` set to new 64-char token |
| Tracked yaml (`application-dev-test-beta.yaml` / `application-beta.yaml`) | **unchanged** — still `${COLLECTOR_API_TOKEN:}` |
| `unify-collector-api/.env` | not present in workspace |

## ops-server restart

**Yes** — `.\scripts\start-integration-oa.ps1 -Profiles "…,dev-test-beta"` after env update.

- Profile: `dev-test-beta`
- Health: UP (`:48094`)
- Process env confirmed: `COLLECTOR_BASE_URL=http://ai.shenyu.com/`, `COLLECTOR_API_TOKEN` matches new value (prefix/suffix check)

## E2E results

### A. Direct collector (`http://ai.shenyu.com`)

| Step | Expect | Result |
|------|--------|--------|
| `GET /livez` | 200 | PASS |
| `GET /api/v1/accounts` (no Authorization) | 401 | PASS |
| `GET /api/v1/accounts` + `Authorization: Bearer <token>` | 200 | PASS |
| `GET /api/v1/accounts/health` + Bearer | 200 | PASS |

No `Token 无效` with the new token.

### B. OPS → collector (after restart)

| Step | Result |
|------|--------|
| Login `admin` @ system-server | PASS (`code=0`) |
| `POST /admin-api/ops/account/1000112/collector-bind/test-connection` | PASS `code=0` — reached collector (business `DISCONNECTED` / account credential issue, **not** 401 auth) |
| `POST …/collector-bind/qr-login/start` | PASS `code=0` · `status=pending` · `sessionId` + QR present |

## Secrets

- Real token lives only in **gitignored** `ops-test-remote.env`.
- Do **not** commit that file or paste the token into tracked yaml/docs.
- Example placeholder remains `CHANGE_ME` in `ops-test-remote.env.example`.
