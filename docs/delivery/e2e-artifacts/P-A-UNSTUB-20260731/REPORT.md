# P-A Unstub Report（2026-07-31）

Slice: **P-A only**（终态缺口计划）· No P-B/C/D

## Migrated domains（from `legacy-archive` → compile path）

| Domain | Controllers | Spec | Smoke list |
|--------|-------------|------|------------|
| HomeDashboard | `HomeDashboardController` | API-M0 | metrics/todos/todo-list code=0 |
| Dashboard / DashboardConfig | `DashboardController` `DashboardConfigController` | API-M6 | config/list total=3；create 非 stub |
| Account / Content / Follower Analysis | `*AnalysisController` | API-M1 | list code=0（account total=183 / content 133） |
| Funnel / CustomQuery | `FunnelController` `CustomQueryController` | API-M6 | funnel total=5；query total=4 |
| Report | `ReportController` | API-M6 | unified-account total=21 |
| Monitor | `MonitorController` | API-M7 | external/list code=0 |
| OpsAnchor / OpsStats | `OpsAnchorController` `OpsStatsController` | API-M1 | ops-anchor/list code=0 |
| Metadata | `MetadataController` | API-M8 | list total=10 |
| Param | `ParamController` + recovered CRUD `ParamService*` | ADR-047 / CLEANUP | list total=11 |
| Message | `MessageController` | API-M9 | Controller 挂载；unread-count 403（薄 login-user 缺 `oa:message:*`） |
| WechatAnalysis | `WechatAnalysis*Controller` | ADR-048 | wework/personal list code=0 |

Artifacts: `RESULTS.json` · `smoke.py` · per-endpoint JSON

## Remaining stubs（`DeferredCutoverStubController`）

| Prefix | Reason |
|--------|--------|
| `/collector-bind/**` | M10 / Phase 2 OOS（API-M10） |
| `/account/douyin-followers/**` | Spec 薄弱 → **阻塞问题**（见下） |
| `/collect/**` | M10 OOS |
| `/config/external-*` `/order-collect` `/internal-collect` | M10 collect config OOS |
| `/internal/**` | 个微/企微 CRUD Controllers 未本 Slice 挂载（DAL 已为 Analysis 依赖迁入） |
| Parallel system user/role/dept/permission/tenant/dict/dev | 既有 410 |

## 阻塞问题清单

1. **DouyinFollowers**：GAP 候选但无独立 API Spec 强制条款 → 本 Slice **保留 stub**，不发明 API。
2. **`/internal/**` personal/wework Controllers**：API-M4 有 Spec，但依赖奥创/M10 sync 服务；本 Slice 仅迁 Analysis 所需 DAL/轻量 Service，**未**挂 `PersonalWechatAccountController` / `Wework*Controller`。
3. **Gateway 冒烟**：本机 Docker/Nacos 未起，冒烟走 **ops-server :48094 直连 + login-user**（对外路径仍经 Gateway Rewrite ops→oa，P-C 另做）。

## Next session

**P-B** 包名 `cn.iocoder.yudao.module.oa` → `football.module.ops`（勿混 P-C）。
