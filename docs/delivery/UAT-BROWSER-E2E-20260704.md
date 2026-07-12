# UAT Browser E2E — Ops Standalone (2026-07-04)

> **Tool**: Playwright · **Stack**: `start-ops-standalone.ps1` → UI :3000 · API :8080 · Dev Token
> **Script**: `scripts/run-uat-browser-e2e.ps1` · **Spec**: `ops-platform-ui-vue/tests/uat-browser-gap.spec.ts`

## Summary

| Item | Value |
|------|-------|
| Scope | 内容生产 remaining + 运营管理 + 账号管理 |
| Total | 15 |
| **PASS** | **15/15** |
| Failed | 0 |
| Generated | 2026-07-04T00:48:09.383Z |

## Pages

| Group | Route | Title | Result | API | Notes |
|-------|-------|-------|--------|-----|-------|
| 内容生产 | `/content` | 内容管理 | PASS | HTTP 200 code=0 | — |
| 内容生产 | `/knowledge` | 内容知识库 | PASS | HTTP 200 code=0 | — |
| 内容生产 | `/layout-template` | 公推模板库 | PASS | HTTP 200 code=0 | — |
| 内容生产 | `/sop` | SOP管理 | PASS | — | — |
| 内容生产 | `/sop/review` | SOP审核 | PASS | — | — |
| 内容生产 | `/task` | 任务管理 | PASS | — | — |
| 运营管理 | `/account-analysis` | 账号分析 | PASS | HTTP 200 code=0 | — |
| 运营管理 | `/author` | 作者管理 | PASS | HTTP 200 code=0 | — |
| 运营管理 | `/efficiency` | 人效盘点 | PASS | — | — |
| 运营管理 | `/fans-analysis` | 粉丝分析 | PASS | — | — |
| 运营管理 | `/internal-content` | 内部作品分析 | PASS | HTTP 200 code=0 | — |
| 账号管理 | `/company` | 公司管理 | PASS | HTTP 200 code=0 | — |
| 账号管理 | `/phone` | 手机管理 | PASS | HTTP 200 code=0 | — |
| 账号管理 | `/realname` | 实名人管理 | PASS | HTTP 200 code=0 | — |
| 账号管理 | `/simcard` | 手机卡管理 | PASS | — | — |

## Re-run

```powershell
.\scripts\start-ops-standalone.ps1
.\scripts\run-uat-browser-e2e.ps1
```

JSON: `docs/delivery/uat-browser-e2e-20260704-probe.json`
