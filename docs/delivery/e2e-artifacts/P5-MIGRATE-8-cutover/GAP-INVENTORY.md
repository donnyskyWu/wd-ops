# P5-MIGRATE Gap Inventory（Cutover 后）

Date: 2026-07-31 · SSOT: ADR-058 §4.3 · Updated by **UNSTUB-20260731（author/config/perf/metric/content-ext）**

## Migrated Controllers (core + B1 + B2 + UNSTUB-20260731)

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
| MatchController | `/match` | post-cutover（内容选择赛事） |
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
| **AuthorController + AuthorExt** | `/author` `/author-ext` | **UNSTUB-20260731** |
| **AiPromptConfigController** | `/config/ai-prompt` | **UNSTUB-20260731** |
| **ThresholdConfigController** | `/config/threshold` | **UNSTUB-20260731** |
| **PerfTemplate/Record/Result** | `/perf/template\|record\|result` | **UNSTUB-20260731** |
| **MetricController** | `/metric` | **UNSTUB-20260731** |
| **InternalContentController** | `/internal-content` | **UNSTUB-20260731** |
| **ProductivityReviewController** | `/productivity-review` | **UNSTUB-20260731** |
| **PlatformAccountFanGroupController** | `/account/fan-group` | **UNSTUB-20260731** |
| **WechatOfficialCertRenewalController** | `/account/wechat-cert-renewal` | **UNSTUB-20260731** |
| DeferredCutoverStubController | remaining deferred | MIGRATE-8 |

## Still stubbed (DeferredCutoverStubController)

### Gate / main-menu risk — stubbed (GET empty / write 410)

- Analytics / Screen: Dashboard / HomeDashboard / DashboardConfig / Funnel / Report / Monitor / OpsStats / CustomQuery
- Analysis: AccountAnalysis / ContentAnalysis / FollowerAnalysis / WechatAnalysis*
- Account ext remaining: **CollectorBind / DouyinFollowers only**（fan-group / wechat-cert 已迁）
- System ext: Message / Param / Metadata / config/{external-source,external-collect,order-collect,internal-collect} / collect/* / OpsAnchor / internal/*

### Already 410 in old module — stubbed same

- User / Role / Dept / Permission / Tenant / SystemDict / DingTalkDev

### Intentionally not migrated (Phase 2 / Out of Scope)

- Collect* (M10 collector) — stubs only; no M10 implementation
- Demo / Hello

### Notes

- Author **基础 CRUD**（create/update/delete）仍为 `AUTHOR_CRUD_DEPRECATED`（Football member 作者管理 SSOT）；list/page/dashboard/ops-list + **author-ext PUT** 已可用
- HomeDashboard 缓存刷新仍 deferred（`TodoReminderSupport` 仅 dismiss 通知，不依赖未迁 HomeDashboardService）

## Smoke evidence (2026-07-31 UNSTUB)

Artifacts: `docs/delivery/e2e-artifacts/UNSTUB-20260731/`

| Path | code | total / note |
|------|------|----------------|
| `/author/list` | 0 | total **29**（非 stub） |
| `/author/page` | 0 | ok（FE alias） |
| `/config/ai-prompt/list` | 0 | total **11** |
| `/config/threshold/list` | 0 | total **10** |
| `/perf/template/list` | 0 | total **3** |
| `/perf/record/list` | 0 | total **6** |
| `/metric/list` | 0 | total **32** |
| `/internal-content/list` | 0 | total **135** |
| `/productivity-review/list` | 0 | total **5** |
| `/config/ai-prompt/create` | 400 | 字典校验（非 stub 410） |
| `/dashboard/create` | 410 | 仍 stub（控制组） |
| `/collector-bind/create` | 410 | 仍 stub（控制组） |
| `/account/wechat-cert-renewal/list` | 0 | ok（空列表） |
| `/author-ext/{id}` | 0 | ok（真实扩展） |
| `/account/fan-group/list` | — | Controller 已挂载；对非粉丝群适用账号返回业务「账号不存在」（非 stub 410） |

## Migrated in P5-MIGRATE-9 (was stub)

- FinanceRoi `/finance/roi/**` — analysis / trend / breakdown / export
- AccountCost `/finance/cost/**` — list / create / update / delete
- OrderAttribution `/order-attribution/**` — list / roi / export（wd `oa_order_attribution`；列表 UI 主路径仍可走 football-order）

## Migrated post-cutover (was stub)

- MatchController `/match/**` — 外部赛事代理（内容选择赛事）
- AiModelConfigController `/config/ai-model/**` — list / stats / create / update / delete / test-connection / set-default
- AiContentController `/ai-content/**` — generate / models / preference / conversation / adopt（含真实 `AiLlmInvokeSupport`）
- M4 master: Company / Realname / Phone / SimCard
- Content ext: Plan / Knowledge / LayoutStyle / LayoutTemplate / TypesettingRule
- **UNSTUB-20260731**: Author / AiPrompt / Threshold / Perf* / Metric / InternalContent / ProductivityReview / FanGroup / WechatCertRenewal

## Cutover decision

**Full cutover on :48094** with deferred stubs — core menus + ROI/账号成本/订单归因 + M4 主数据 + AI 内容生成 + 作者/配置/绩效/指标/内容分析扩展 covered by real Controllers; remaining menus get safe empty GET / 410 write. Permissions remain `oa:*` (P6 later).

## ADR-058 CLEANUP (2026-07-31)

- **`ops-platform-server/` deleted** — do not start legacy module; `-UseLegacyOa` fail-fast.
- **Flyway SSOT** = `football-module-ops-server/src/main/resources/db/migration/` (162 V*.sql; `spring.flyway.enabled=true`).
- **Unmigrated source + legacy IT** preserved under `football-module-ops-server/legacy-archive/` (not on Maven classpath). Runtime SSOT = Gate E2E + monorepo Controllers/stubs.
- Still stubbed (unchanged list above): Dashboard/Screen/Analysis, collector-bind/douyin-followers, message/metadata/collect OOS, parallel system CRUD 410.

## Likely user hit (2026-07-31 diagnose)

Ops access log（PID 20592 / 39704，重启前）高频命中 stub 域：

1. **`/admin-api/oa/author/list|page`** — 带真实 `ipGroupId`（9000–9003），FE 选择器路径
2. `/admin-api/oa/config/threshold/list`、`/config/ai-prompt/list`
3. `/admin-api/oa/perf/template/list`、`/dashboard/home/*`、`/account-analysis/list`

写操作暴露的文案 `ADR-058 P5 deferred` 仅来自 stub write；与上述域一致。本轮优先迁 **author + ai-prompt + threshold**，并批量迁 perf/metric/internal-content/productivity-review/fan-group/wechat-cert。
