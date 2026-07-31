# P5-MIGRATE Gap Inventory（Cutover 后）

Date: 2026-07-31 · SSOT: ADR-058 §4.3 · Updated by **P-A-UNSTUB-20260731**（Dashboard/Analysis/Funnel/Report/Monitor/Metadata/Param/Message/WechatAnalysis）

## Migrated Controllers (core + B1 + B2 + UNSTUB + **P-A**)

| Controller | Path prefix | Slice |
|------------|-------------|-------|
| IpGroupController | `/ip-group` | MIGRATE-2 |
| ProductionContentController | `/content` | MIGRATE-3 |
| PlatformAccountController | `/account` | MIGRATE-4 |
| WechatMpFollowerController | `/account` mp-followers | MIGRATE-4 |
| SopTemplate/Node/Review + Task | `/sop/*` `/task` | MIGRATE-5 |
| DictController | `/dict` | MIGRATE-6 |
| FileController | `/file` | MIGRATE-6 |
| FootballOrderReadController | `/football-order` | MIGRATE-7 |
| FinanceRoiController | `/finance/roi` | MIGRATE-9 |
| AccountCostController | `/finance/cost` | MIGRATE-9 |
| OrderAttributionController | `/order-attribution` | MIGRATE-9 |
| OpsFoundationController | `/ops-foundation` | MIGRATE-1 |
| MatchController | `/match` | post-cutover（内容选择赛事）；**G-MATCH-01 Closed-Accept** 外部代理 [ADR-059](../../../adr/ADR-059-G-MATCH-01-external-proxy.md) |
| AiModelConfigController | `/config/ai-model` | post-cutover（配置管理 AI模型） |
| CompanyController | `/company` | B1 unstub |
| RealnameController + Intermediary | `/realname` | B1 unstub |
| PhoneController | `/phone` | B1 unstub |
| SimCardController | `/sim-card` | B1 unstub |
| AiContentController | `/ai-content` | B2 hotfix（generate/models） |
| ContentPlanController | `/plan` | B2 unstub |
| KnowledgeController | `/knowledge` | B2 unstub |
| LayoutStyleController | `/layout-style` | B2 unstub |
| LayoutTemplateController | `/layout-template` | B2 unstub |
| TypesettingRuleController | `/typesetting-rule` | B2 unstub |
| AuthorController + AuthorExt | `/author` `/author-ext` | UNSTUB-20260731 |
| AiPromptConfigController | `/config/ai-prompt` | UNSTUB-20260731 |
| ThresholdConfigController | `/config/threshold` | UNSTUB-20260731 |
| PerfTemplate/Record/Result | `/perf/template\|record\|result` | UNSTUB-20260731 |
| MetricController | `/metric` | UNSTUB-20260731 |
| InternalContentController | `/internal-content` | UNSTUB-20260731 |
| ProductivityReviewController | `/productivity-review` | UNSTUB-20260731 |
| PlatformAccountFanGroupController | `/account/fan-group` | UNSTUB-20260731 |
| WechatOfficialCertRenewalController | `/account/wechat-cert-renewal` | UNSTUB-20260731 |
| **HomeDashboardController** | `/dashboard/home` | **P-A-UNSTUB-20260731** |
| **DashboardController** | `/dashboard` | **P-A-UNSTUB-20260731** |
| **DashboardConfigController** | `/dashboard-config` | **P-A-UNSTUB-20260731** |
| **AccountAnalysisController** | `/account-analysis` | **P-A-UNSTUB-20260731** |
| **ContentAnalysisController** | `/content-analysis` | **P-A-UNSTUB-20260731** |
| **FollowerAnalysisController** | `/follower-analysis` | **P-A-UNSTUB-20260731** |
| **FunnelController** | `/funnel` | **P-A-UNSTUB-20260731** |
| **CustomQueryController** | `/query` | **P-A-UNSTUB-20260731** |
| **ReportController** | `/report` | **P-A-UNSTUB-20260731** |
| **MonitorController** | `/monitor` | **P-A-UNSTUB-20260731** |
| **OpsAnchorController + OpsStatsController** | `/ops-anchor` `/ops` | **P-A-UNSTUB-20260731** |
| **MetadataController** | `/metadata` | **P-A-UNSTUB-20260731** |
| **ParamController** | `/system/param` | **P-A-UNSTUB-20260731** |
| **MessageController** | `/system/message` | **P-A-UNSTUB-20260731** |
| **WechatAnalysisPersonal/Wework** | `/wechat-analysis/**` | **P-A-UNSTUB-20260731** |
| DeferredCutoverStubController | remaining deferred | MIGRATE-8 |

## Still stubbed (DeferredCutoverStubController)

### Remaining deferred

- Account ext: **CollectorBind / DouyinFollowers**（M10 OOS / Spec 薄弱）
- M10 collect OOS: `/collect/**` · `config/{external-source,external-collect,order-collect,internal-collect}`
- `/internal/**` personal/wework Controllers（DAL 已为 Analysis 迁入；CRUD Controllers 未挂 — 见阻塞清单）

### Already 410 in old module — stubbed same

- User / Role / Dept / Permission / Tenant / SystemDict / DingTalkDev

### Intentionally not migrated (Phase 2 / Out of Scope)

- Collect* (M10 collector) — stubs only; no M10 implementation
- Demo / Hello

### Notes

- Author **基础 CRUD**（create/update/delete）仍为 `AUTHOR_CRUD_DEPRECATED`（Football member 作者管理 SSOT）；list/page/dashboard/ops-list + **author-ext PUT** 已可用
- HomeDashboard 已迁；缓存刷新 `POST /dashboard/home/refresh` 可用

## Smoke evidence (2026-07-31 P-A)

Artifacts: `docs/delivery/e2e-artifacts/P-A-UNSTUB-20260731/`（ops-server :48094 直连；Gateway/Nacos 本机未起）

| Path | code | total / note |
|------|------|----------------|
| `/dashboard/home/metrics` | 0 | ok（需 member Feign/mock） |
| `/dashboard/home/todos` | 0 | total **9** |
| `/dashboard-config/list` | 0 | total **3** |
| `/account-analysis/list` | 0 | total **183** |
| `/content-analysis/list` | 0 | total **133** |
| `/follower-analysis/list` | 0 | total **0**（空库可接受） |
| `/funnel/list` | 0 | total **5** |
| `/query/list` | 0 | total **4** |
| `/report/unified-account/list` | 0 | total **21** |
| `/monitor/external/list` | 0 | ok |
| `/ops-anchor/list` | 0 | ok |
| `/metadata/list` | 0 | total **10** |
| `/system/param/list` | 0 | total **11** |
| `/system/message/unread-count` | 403 | Controller 已挂；薄 login-user 缺权限 |
| `/wechat-analysis/wework/list` | 0 | ok |
| `/collector-bind` GET | 0 | stub 空页（控制组） |
| `/collector-bind` POST | 410 | stub deferred（控制组） |

## Migrated in P5-MIGRATE-9 (was stub)

- FinanceRoi `/finance/roi/**` — analysis / trend / breakdown / export
- AccountCost `/finance/cost/**` — list / create / update / delete
- OrderAttribution `/order-attribution/**` — list / roi / export（wd `oa_order_attribution`；列表 UI 主路径仍可走 football-order）

## Migrated post-cutover (was stub)

- MatchController `/match/**` — 外部赛事代理（内容选择赛事）；P-F / ADR-059 接受为终态，非 match-server Feign
- AiModelConfigController `/config/ai-model/**` — list / stats / create / update / delete / test-connection / set-default
- AiContentController `/ai-content/**` — generate / models / preference / conversation / adopt（含真实 `AiLlmInvokeSupport`）
- M4 master: Company / Realname / Phone / SimCard
- Content ext: Plan / Knowledge / LayoutStyle / LayoutTemplate / TypesettingRule
- **UNSTUB-20260731**: Author / AiPrompt / Threshold / Perf* / Metric / InternalContent / ProductivityReview / FanGroup / WechatCertRenewal
- **P-A-UNSTUB-20260731**: Dashboard/Home/Analysis/Funnel/Query/Report/Monitor/OpsAnchor/Metadata/Param/Message/WechatAnalysis

## Cutover decision

**Full cutover on :48094** with deferred stubs — core menus + ROI/账号成本/订单归因 + M4 主数据 + AI 内容生成 + 作者/配置/绩效/指标 + **P-A 分析/大屏/报表/元数据/参数** covered by real Controllers; remaining = M10 OOS + parallel system 410. Permissions **`ops:*`（P-D ✅ 2026-07-31）**.

## ADR-058 CLEANUP (2026-07-31)

- **`ops-platform-server/` deleted** — do not start legacy module; `-UseLegacyOa` fail-fast.
- **Flyway SSOT** = `football-module-ops-server/src/main/resources/db/migration/` (162 V*.sql; `spring.flyway.enabled=true`).
- ~~**Unmigrated source + legacy IT** under `legacy-archive/`~~ → **P-G ✅ 2026-07-31**：`git rm -r` 已删（580 files；仅 git 历史）。回滚：`git -C football-backend-saas checkout 7e5f1b709 -- football-module-ops/football-module-ops-server/legacy-archive`。从未在 Maven classpath。证据：[P-G-LEGACY-ARCHIVE-20260731](../P-G-LEGACY-ARCHIVE-20260731/REPORT.md)。
- P-A 已消费 archive 中 Dashboard/Analysis/Report/Monitor/Metadata/Message/Param/WechatAnalysis 等；archive 本体已由 P-G 清除。

## 阻塞问题清单（P-A）

1. **DouyinFollowers** — Spec 沉默/薄弱 → 保留 stub，不发明 API。
2. **`/internal/**` Controllers** — API-M4 有 Spec，但奥创 sync = M10 依赖；本 Slice 未挂 CRUD Controllers。
3. **Gateway 全栈冒烟** — 本机 Docker/Nacos 未起；P-A 冒烟 = ops 直连。正式 Gate 需 Nacos+Gateway。
