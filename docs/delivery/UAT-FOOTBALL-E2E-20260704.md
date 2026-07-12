# UAT Browser E2E — Football Full-Stack (2026-07-04)

> **Tool**: Playwright · **Stack**: `start-integration-all.ps1` → UI :5777 · Gateway :48080
> **Login**: `admin` / `admin123` · tenant **1** · hash `#/ops/...`
> **Script**: `scripts/run-uat-football-e2e.ps1` · **Spec**: `football-front/apps/web-ele/tests/uat-football-ops-login.spec.ts`

## Summary

| Item | Value |
|------|-------|
| Scope | all 58 visible Ops menu routes (oa-menu-permission-map.csv) |
| Total | 58 |
| **PASS** | **58/58** |
| Failed | 0 |
| Generated | 2026-07-07T03:23:30.588Z |

## Pages

| Group | Hash | Title | Result | API | Notes |
|-------|------|-------|--------|-----|-------|
| 作品监测 | `#/ops/external-account` | 外部账号分析 | PASS | — | — |
| 作品监测 | `#/ops/high-fans-account` | 高粉账号分析 | PASS | — | — |
| 作品监测 | `#/ops/hot-works` | 爆款作品分析 | PASS | — | — |
| 作品监测 | `#/ops/ip-theme` | IP主题数据 | PASS | — | — |
| 作品监测 | `#/ops/low-fans-account` | 低粉账号分析 | PASS | — | — |
| 作品监测 | `#/ops/low-score` | 低分作品分析 | PASS | — | — |
| 内容生产 | `#/ops/content` | 内容管理 | PASS | — | — |
| 内容生产 | `#/ops/content/review` | 内容审核 | PASS | — | — |
| 内容生产 | `#/ops/knowledge` | 内容知识库 | PASS | — | — |
| 内容生产 | `#/ops/layout-template` | 公推模板库 | PASS | — | — |
| 内容生产 | `#/ops/plan` | 计划管理 | PASS | — | — |
| 内容生产 | `#/ops/sop` | SOP管理 | PASS | — | — |
| 内容生产 | `#/ops/sop/review` | SOP审核 | PASS | — | — |
| 内容生产 | `#/ops/task` | 任务管理 | PASS | — | — |
| 数据分析 | `#/ops/custom-query` | 自定义查询 | PASS | — | — |
| 数据分析 | `#/ops/data-report` | 数据报表 | PASS | — | — |
| 数据分析 | `#/ops/financial-analysis` | 总体财务分析 | PASS | — | — |
| 数据分析 | `#/ops/funnel-analysis` | 漏斗分析 | PASS | — | — |
| 数据分析 | `#/ops/metric` | 指标管理 | PASS | — | — |
| 数据分析 | `#/ops/metric-analysis` | 指标分析 | PASS | — | — |
| 数据分析 | `#/ops/screen` | 数据大屏 | PASS | — | — |
| 数据分析 | `#/ops/screen-config` | 大屏配置 | PASS | — | — |
| 数据采集 | `#/ops/collect/log` | 采集日志 | PASS | — | — |
| 数据采集 | `#/ops/collect/private-domain-bridge` | 私域桥接 | PASS | — | — |
| 数据采集 | `#/ops/collect/quality` | 数据质量 | PASS | — | — |
| 数据采集 | `#/ops/collect/task` | 采集任务 | PASS | — | — |
| 系统管理(OA) | `#/ops/system-dict` | 字典配置 | PASS | — | — |
| 系统管理(OA) | `#/ops/system-log/login` | 登录日志 | PASS | — | — |
| 系统管理(OA) | `#/ops/system-log/operation` | 操作日志 | PASS | — | — |
| 系统管理(OA) | `#/ops/system-message` | 消息管理 | PASS | — | — |
| 系统管理(OA) | `#/ops/system-param` | 系统参数 | PASS | — | — |
| 绩效核算 | `#/ops/order-attribution` | 订单归因分析 | PASS | — | — |
| 绩效核算 | `#/ops/perf-execution` | 考核执行 | PASS | — | — |
| 绩效核算 | `#/ops/perf-result` | 绩效结果 | PASS | — | — |
| 绩效核算 | `#/ops/perf-template` | 考核模板 | PASS | — | — |
| 财务管理 | `#/ops/account-cost` | 账号成本管理 | PASS | — | — |
| 财务管理 | `#/ops/roi-analysis` | ROI分析 | PASS | — | — |
| 账号管理 | `#/ops/company` | 公司管理 | PASS | — | — |
| 账号管理 | `#/ops/internal-account` | 平台账号管理 | PASS | — | — |
| 账号管理 | `#/ops/personal-account` | 个人账号管理 | PASS | — | — |
| 账号管理 | `#/ops/phone` | 手机管理 | PASS | — | — |
| 账号管理 | `#/ops/realname` | 实名人管理 | PASS | — | — |
| 账号管理 | `#/ops/simcard` | 手机卡管理 | PASS | — | — |
| 运营管理 | `#/ops/account-analysis` | 账号分析 | PASS | — | — |
| 运营管理 | `#/ops/author` | 作者管理 | PASS | — | — |
| 运营管理 | `#/ops/efficiency` | 人效盘点 | PASS | — | — |
| 运营管理 | `#/ops/fans-analysis` | 粉丝分析 | PASS | — | — |
| 运营管理 | `#/ops/internal-content` | 内部作品分析 | PASS | — | — |
| 运营管理 | `#/ops/ip-group` | IP组管理 | PASS | — | — |
| 配置管理 | `#/ops/config-ai-model` | AI模型 | PASS | — | — |
| 配置管理 | `#/ops/config-ai-prompt` | AI提示词 | PASS | — | — |
| 配置管理 | `#/ops/config-external-collect` | 外部采集配置 | PASS | — | — |
| 配置管理 | `#/ops/config-external-data` | 外部数据配置 | PASS | — | — |
| 配置管理 | `#/ops/config-internal-collect` | 内部采集配置 | PASS | — | — |
| 配置管理 | `#/ops/config-metadata` | 元数据维护 | PASS | — | — |
| 配置管理 | `#/ops/config-order-collect` | 订单采集配置 | PASS | — | — |
| 配置管理 | `#/ops/config-threshold` | 阈值规则配置 | PASS | — | — |
| 首页 | `#/ops/dashboard` | 首页仪表盘 | PASS | — | — |

## Re-run

```powershell
.\scripts\start-integration-all.ps1 -SkipBuild
.\scripts\run-uat-football-e2e.ps1
```

JSON: `docs/delivery/uat-football-e2e-20260704-probe.json`
