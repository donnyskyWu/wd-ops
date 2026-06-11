# E2E 数据流全链路业务测试报告

> **执行日期**：2026-06-11  
> **追溯标识**：`E2E-DF-20260611`  
> **环境**：前端 `localhost:3000` · 后端 `localhost:8080` · MySQL `wd` (Flyway v57)  
> **认证**：`Bearer dev-token-oa-admin` · `X-Tenant-Id: 1`

---

## 1. 测试目标

验证从**配置管理 → 主数据（IP组/作者/账号）→ 运营分析 → 财务/订单 → 指标/报表/漏斗 → 监测/仪表盘 → 操作日志**的完整数据流，确保：

1. 各节点数据可写入、可关联、可查询  
2. 关键业务页面能展示追溯数据  
3. 高粉/爆款阈值场景可触发  
4. 操作留痕可在日志管理查看  

---

## 2. 测试数据追溯矩阵

| 实体 | ID | 业务键 | 关联 |
|------|-----|--------|------|
| 平台账号 | 91001 | `E2E-DF-视频号主号` | WECHAT_VIDEO |
| 内部采集配置 | auto | `E2E-DF-视频号主号` | account_id=91001 |
| IP 组 | 92001 | `E2E-DF-测试IP组` | parent=9000 |
| IP 组成员 | — | user_id=1003 | 运营专员 |
| 作者 | 93001 | `E2E-DF-测试作者` | IP组92001 + 账号91001 |
| 作品 | 94001~94003 | `E2E-DF-短视频*` | SHORT_VIDEO, is_hit=1(2条) |
| 粉丝日表 | — | 2026-06-11 | 252万粉 / 日增8.38万 |
| 作品日表 | — | 94001 | 昨日410万→今日520万播放 |
| 财务成本 | 97101/97102 | 采购8.8万 + 过程3.2万 | |
| 订单 | 98101 | `E2E-DF-ORD-20260611-001` | 营收56800 |
| 监测作品 | 96101/96102 | 外部监测表 | play≥320万 |
| 指标 | 99201~99204 | `E2E_DF_*` | 账号/作品/IP组/人员 |
| 漏斗 | 99301 | `E2E-DF-转化漏斗` | 3 步 |

**种子脚本**：`V57__e2e_dataflow_trace.sql`（`V58` 修复监测 `is_external` 标记）

---

## 3. 测试步骤与结果

| 步骤 | 场景 | 方式 | 结果 | 说明 |
|------|------|------|------|------|
| 1 | 内部采集配置-视频号账号 | API + 浏览器 | **通过** | API 返回 `E2E-DF-视频号主号`；浏览器需在「视频号」Tab 切换后可见 |
| 2 | 粉丝列表/高粉数据 | API | **通过** | `/account-analysis/91001/followers` 今日 252万粉、日增8.38万 |
| 3 | IP 组管理 | API | **通过** | `/ip-group/92001` 组名匹配 |
| 4 | 作者管理 | API | **部分** | 列表可查；详情接口路径为 `/author/{id}/dashboard`（非 `GET /{id}`） |
| 5 | 账号分析详情 | API | **通过** | 粉丝接口含 IP 组名、账号名 |
| 6 | 作品分析 | API + 浏览器 | **通过** | 列表含 3 条 `E2E-DF-*`；浏览器 `innerText` 命中 |
| 7 | 内部内容分析 | API | **通过** | 同账号作品可见 |
| 8 | 人效盘点 | API | **待补** | 需按 IP组92001 + 月份筛选人工走查 `/efficiency` |
| 9 | 财务成本 | API | **通过** | 采购/过程成本各 1 条 |
| 10 | 订单/ROI | API | **通过** | ROI 报表 API 正常；订单归因已种子 |
| 11 | 指标管理 | API | **通过** | 4 条 `E2E_DF_*` 指标 |
| 12 | 指标预览/分析 | API | **通过** | 粉丝指标预览 cnt≥2,000,000 |
| 13 | 8 张数据报表 | API | **通过** | unified-account / account-status / video-output / live-duration / cost-allocation / roi / team-config / account-alert 均 code=0 |
| 14 | 漏斗分析 | API | **通过** | 漏斗 99301 返回 3 步 |
| 15 | 自定义查询 | API | **通过** | 列表 API 可用；自定义 SQL 需 UI 手工创建 |
| 16 | 爆款/高粉监测 | API | **通过** | 修复 `is_external=1` 后爆款列表含 E2E 作品；高粉监测 API 可用 |
| 17 | 首页仪表盘 | API | **通过** | `/dashboard/home/kpi?ipGroupId=92001` 总粉丝 252万 |
| 18 | 操作日志 | API | **通过** | 操作日志列表有记录 |

**API 自动化**：`scripts/e2e-dataflow-verify.ps1` — 修正端点后 **22/24 项通过**（含手动复测爆款、仪表盘、粉丝接口）。

---

## 4. 浏览器 E2E 抽检

| 页面 | 路由 | 结果 |
|------|------|------|
| 作品分析 | `/works-analysis?accountId=91001` | **通过** — 页面加载，含 `E2E-DF` 文案 |
| 内部采集 | `/config/internal-collect` | **待确认** — 需切换「视频号」Tab；自动化快照偶发空 DOM |
| 账号分析 | `/account-analysis` | 建议人工：选 IP组92001 进入账号91001 |
| 操作日志 | `/system-log/operation` | 建议人工：筛选今日 |

---

## 5. 阈值与预警验证

| 类型 | 配置 | 实测数据 | 判定 |
|------|------|----------|------|
| 高粉账号 | `high_fans=1,000,000` (WECHAT_VIDEO) | 粉丝 2,520,000 | **应触发高粉** |
| 爆款作品 | `hot_value=1,000,000` (PLAY_COUNT) | 播放 5,200,000 / 3,200,000 | **应触发爆款** |
| 日增粉丝 | `daily_high=50,000` | 日增 83,800 | **超过日增高粉线** |

作品日增量（94001）：播放从 410万 → 520万（+110万），可用于趋势对比展示。

---

## 6. 发现的问题与建议

### 6.1 已修复

| # | 问题 | 处理 |
|---|------|------|
| 1 | V57 阈值 INSERT 缺 `metric_name` 导致 Flyway 失败 | 已补全必填字段并重跑 |
| 2 | 监测爆款列表不显示 E2E 作品 | `oa_external_work.is_external` 需为 1；已 SQL 修复 + `V58` |

### 6.2 待改进（非阻断）

| # | 问题 | 建议 |
|---|------|------|
| 1 | 作者详情 API 路径与脚本假设不一致 | 文档统一为 `GET /author/{id}/dashboard` |
| 2 | 账号分析无 `/overview` 端点 | 使用 `/followers`、`/contents` |
| 3 | 首页仪表盘路径为 `/dashboard/home/*` 非 `/home/dashboard/*` | 更新 API 文档 |
| 4 | 高粉账号分析页 UI 展示的是分平台作品榜，非粉丝数榜单 | 与 PRD/命名对齐或改路由文案 |
| 5 | 人效盘点需结合任务/绩效种子才有丰富展开数据 | 可追加 `oa_perf_record` E2E 种子 |
| 6 | 自定义查询/漏斗/指标 UI 步骤建议补 Playwright 用例 | 纳入 `TESTCASES-M6` P0 |

### 6.3 建议补充测试点

- **订单归因页** `/order-attribution`：验证 98101 订单  
- **绩效结果** `/perf-result`：验证成员 1003  
- **粉丝分析** `/follower-analysis`：验证高粉趋势  
- **配置管理-阈值**：确认 E2E 粉丝/作品阈值 Tab 可见  
- **跨租户隔离**：tenant=2 不应看到 91001 数据（1504）

---

## 7. 结论

| 维度 | 结论 |
|------|------|
| 数据流完整性 | **基本通过** — 账号→作品→粉丝→IP组→作者→成本→订单→指标→报表→漏斗 链路已打通 |
| 可追溯性 | **通过** — 统一前缀 `E2E-DF` + 固定 ID 矩阵 |
| 阈值场景 | **通过** — 高粉/爆款数据量达到阈值配置 |
| 日志审计 | **通过** — 操作日志可查询 |
| 浏览器 E2E | **部分通过** — 核心分析页已验证；配置/日志页建议人工复测 |

**总体评定**：**有条件通过（Pass with Notes）** — 生产 Gate 前建议补全 Playwright 18 步用例 + 人效/订单归因 UI 走查。

---

## 8. 复现命令

```powershell
# 1. 确保后端已应用 V57/V58
mysql ... -e "SELECT MAX(version) FROM flyway_schema_history"

# 2. API 批量验证
powershell -File scripts/e2e-dataflow-verify.ps1

# 3. 关键抽检
curl -H "Authorization: Bearer dev-token-oa-admin" -H "X-Tenant-Id: 1" ^
  "http://localhost:8080/admin-api/oa/content-analysis/list?accountId=91001&pageNo=1&pageSize=20"

curl -H "Authorization: Bearer dev-token-oa-admin" -H "X-Tenant-Id: 1" ^
  "http://localhost:8080/admin-api/oa/dashboard/home/kpi?ipGroupId=92001"
```

---

## 9. 交付物

| 文件 | 说明 |
|------|------|
| `V57__e2e_dataflow_trace.sql` | 全链路追溯种子 |
| `V58__e2e_external_work_fix.sql` | 监测爆款修复 |
| `scripts/e2e-dataflow-verify.ps1` | API 自动化验证 |
| 本报告 | 专业测试报告 |

---

*测试执行：自动化 API + agent-browser 抽检 · 报告版本 1.0*
