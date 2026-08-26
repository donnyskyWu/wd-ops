# 私域报表 MVP 联调 + E2E 验证报告

**日期** 2026-08-21 · **模块** M6 数据分析 · **环境** Gate `:5777` / Gateway `:48080` / Ops `:48094` · admin/admin123 tenant=1 · **本地 localhost multidb（ops profile=local）**

## 一、结论（一句话）

**API smoke ALL OK + Playwright E2E 3/3 通过**。FirstRun 后 ops 曾以 `dev-test-beta` 连远程 `110.42.49.224`（无 V184 表），改为 **local profile** 重启 ops 后 weekly/feedback 恢复正常。

## 二、Pre-flight

| 步骤 | 期望 | 实际 | 结果 |
|------|------|------|------|
| Gateway :48080 | 可登录 | 200 | ✅ |
| Ops :48094 health | UP | UP | ✅ |
| Ops Spring profile | `local` → 127.0.0.1/shenyu-ops | local（重启后） | ✅ |
| Flyway V184 | `oa_report_weekly_feedback` | 本地库已存在 | ✅ |
| member-server :48087 | Feign 可达 | monthly/weekly code=0 | ✅ |
| football-front :5777 | Vite ready | ready | ✅ |

## 三、API 联调（Gateway :48080）

脚本：`python scripts/integration-config/smoke_private_domain_report_api.py docs/delivery/e2e-artifacts/PRIVATE-DOMAIN-REPORT-E2E-20260821`

| 端点 | 方法 | 最终结果 | 说明 |
|------|------|----------|------|
| `/ops/private-domain-report/authors` | GET | code=0 · **4** 作者 | `oa_ip_group_anchor_rel` + AuthorApi |
| `/ops/private-domain-report/monthly-achievement` | GET | code=0 · **4** 行 | MVP 列有值（如 `newMemberRegisterCount`） |
| `/ops/private-domain-report/weekly-funnel` | GET | code=0 · **12** 行 | 4 作者 × 3 channel |
| `/ops/private-domain-report/weekly-feedback` | PUT/GET | code=0 | U 列读写一致 |

**阻塞复盘（已解决）**

| 现象 | 根因 | 修复 |
|------|------|------|
| weekly/feedback **500** · `Table 'shenyu-ops.oa_report_weekly_feedback' doesn't exist` | FirstRun 启动 ops 时 profile=`dev-test-beta`，默认连 **110.42.49.224**，远程库无 V184 表；且 beta profile **Flyway disabled** | `.\scripts\start-integration-oa.ps1 -Profiles "dev,dev-nacos,dev-nacos-local,dev-local-multidb"` 重启为 **local** |
| monthly 曾 **1400** 会员读服务不可用 | 全栈重启后 member-server 未就绪 / 与 beta DB 混用 | FirstRun 重建 member-server + local ops 后恢复 |

## 四、UI E2E（Playwright :5777）

Spec：`football-front/apps/web-ele/tests/private-domain-report-e2e.spec.ts`

```powershell
cd football-front/apps/web-ele
npx playwright test tests/private-domain-report-e2e.spec.ts --project=chromium
```

| # | 用例 | 路径 | 结果 |
|---|------|------|------|
| 1 | 报表中心含私域入口卡片 | `/ops/analysis/data-report` | ✅ 10.8s |
| 2 | 月达成页 MVP 表头 + 查询 | `/ops/analysis/report/monthly-achievement` | ✅ 11.7s |
| 3 | 周度转化页 + U 列反馈 | `/ops/analysis/report/weekly-funnel` | ✅ 12.1s |

**3 passed (36.1s)**

截图：`02-report-center.png` · `03-monthly-achievement.png` · `04-weekly-funnel.png`

## 五、验证中修复项（累计）

| 项 | 修复 |
|----|------|
| ops-server 旧 JAR 无新 Controller | `start-integration-oa.ps1 -Rebuild` |
| `/ops/data-report` 404 | 新增 `ops.ts` 路由 `/ops/analysis/data-report` → `DataReport.vue` |
| 报表中心无私域卡片 | `DataReport.vue` 增加月达成/周转化两张卡片 |
| E2E 路由清单 | `ux-routes.ts` 补充两报表路由 |
| FirstRun 后 weekly 500 | ops 改 **local profile**（勿在本地 E2E 使用未设 `OPS_TEST_DB_HOST=127.0.0.1` 的 beta 模式） |

## 六、MVP 设计内限制（非缺陷）

- E~K / S~T 等列前端显示 `—`
- 抖音/快手/汇总三行暂共用 author 级指标
- 新粉数 = `getNewUserIds ∩ getAuthorUserIdList` 近似

## 七、本地日常启动建议

- 默认：`.\scripts\start-ops-dev.ps1`（local multidb）
- 若 ops 进程命令行含 `--spring.profiles.active=dev-test-beta` 且未指向本地库，weekly 会连远程库失败
- 快速修复：`.\scripts\start-integration-oa.ps1`（不带 `-Beta` / 不含 `dev-test-beta` 的 Profiles）
