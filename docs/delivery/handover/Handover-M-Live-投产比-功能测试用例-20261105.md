# M-Live 抖音投产比 · 功能测试用例（团队测试交接）

| 项 | 值 |
|---|---|
| 模块 | **M-Live** |
| 文档日期 | 2026-11-05 |
| 关联 PRD 摘要 | [Handover-M-Live-投产比-PRD-需求摘要-20261105.md](./Handover-M-Live-投产比-PRD-需求摘要-20261105.md) |
| 用例约定 | **P0** = 发布阻断/核心闭环；**P1** = 重要体验与边界 |

**编写说明**

- 前置条件默认：Gate 环境已启动（见 §测试环境）；租户 **1**；测试账号具备对应 **bootstrap capabilities**。
- 步骤中的 URL 以 Hash 路由为准：`http://localhost:5777/#/ops/roi/...`。
- API 冒烟路径：**浏览器经 48080** → ops-server **48094**（ADR-084）；示例：`GET /admin-api/ops/roi/bootstrap`。
- 不得使用真实钉钉审批通过、真实付款、生产人员 ACL 写入完成验收（handoff 01 §9）。

---

## 目录

1. [鉴权与环境](#1-鉴权与环境)
2. [投产分析 · 总 ROI 三周期](#2-投产分析--总-roi-三周期)
3. [投产分析 · 投入产出报表](#3-投产分析--投入产出报表)
4. [投产分析 · 作者 / 运营 ROI](#4-投产分析--作者--运营-roi)
5. [投产分析 · 直播场次 Tab](#5-投产分析--直播场次-tab)
6. [直播作业 · 平台列与录入](#6-直播作业--平台列与录入)
7. [直播作业 · OCR + AI 核对](#7-直播作业--ocr--ai-核对)
8. [筛选链 · 作者→运营→账号→场次](#8-筛选链--作者运营账号场次)
9. [资金与成本 / 治理（冒烟）](#9-资金与成本--治理冒烟)
10. [边界与异常](#10-边界与异常)
11. [handoff P01–P18 映射](#11-handoff-p01p18-映射)
12. [M5 边界](#12-m5-边界)
13. [测试环境](#测试环境)

---

## 1. 鉴权与环境

| 编号 | 模块 | 优先级 | 前置条件 | 步骤 | 期望结果 |
|------|------|--------|----------|------|----------|
| TC-MLive-ENV-01 | 运行时 | P0 | `start-ops-dev.ps1` 健康表 Gateway、ops-server UP | 带登录 Token 调 `GET /admin-api/ops/roi/bootstrap` | `code` 业务成功；**无需** :48180 监听（ADR-084） |
| TC-MLive-ENV-02 | 租户 | P0 | 已登录 admin 租户 1 | DevTools 或接口请求带 **X-Tenant-Id: 1** 访问 `/admin-api/ops/roi/report` | 仅返回租户 1 数据；换租户无交叉（1504 类拒绝） |
| TC-MLive-AUTH-01 | bootstrap | P0 | 无 ROI 能力账号 | 打开 `#/ops/roi/insight` | 空态或 Tab 不可用；AC-M-Live-P001 |
| TC-MLive-AUTH-02 | M5 隔离 | P0 | 仅有 `oa:roi:list`（M5）账号 | 打开 `#/ops/finance/roi-analysis` 与 `#/ops/roi/insight` | M5 可开；M-Live **不**自动出现（AC-M-Live-P003） |
| TC-MLive-AUTH-03 | 深链 | P1 | 具备 `auditRead` | 访问 `#/ops/roi/governance?workbench=audit` 或旧 `#/ops/roi?workbench=audit` | 进入操作日志；标题正确（AC-M-Live-P002） |

---

## 2. 投产分析 · 总 ROI 三周期

| 编号 | 模块 | 优先级 | 前置条件 | 步骤 | 期望结果 |
|------|------|--------|----------|------|----------|
| TC-MLive-INS-01 | 总 ROI | P0 | `report` 或 `readAll` | `#/ops/roi/insight?tab=company` → 开启「独立营收/成本/注册周期」→ 分别选日期 → 查询 | 请求含 `revenueStart/End`、`costStart/End`、`registerStart/End`（AC-M-Live-004-3） |
| TC-MLive-INS-02 | 总 ROI | P0 | 同上，有种子作者数据 | 查询后查看汇总卡片 | 展示有效现金收入、已确认唯一成本、费用后余额、现金回收倍数；零成本或待核对为 **—** |
| TC-MLive-INS-03 | 总 ROI | P1 | 同上 | 切换筛选未点查询 | 提示「筛选已修改，查询后生效」或等价；旧结果不被 stale 响应覆盖（UI UX 规范） |

---

## 3. 投产分析 · 投入产出报表

| 编号 | 模块 | 优先级 | 前置条件 | 步骤 | 期望结果 |
|------|------|--------|----------|------|----------|
| TC-MLive-RPT-01 | 报表 | P0 | `tab=report` | 分别设置营收/成本/注册三周期 → 查询 | `GET /admin-api/ops/roi/report` 成功；**汇总**表含营收、成本、投入产出比、成本收益率、现金回收倍数（FR-M-Live-003） |
| TC-MLive-RPT-02 | 报表 | P0 | 有场次数据 | 查看 **明细** 表 | 场次编号、开播日期、账号、作者、场次营收/成本；点击场次打开直播详情抽屉 |
| TC-MLive-RPT-03 | 报表 | P0 | 营收周期 ≥2 天 | 查看 **趋势** 图 | 按日折线；成本日不在成本周期内则成本为 0（v20261105 趋势规则） |
| TC-MLive-RPT-04 | 报表 | P1 | 分母为 0 的合成/边界数据 | 查询汇总 | 三比例字段显示 **—**，非 Infinity（PRD §6.1） |
| TC-MLive-RPT-05 | 报表 | P1 | 查询失败 | 断网或非法日期 → 查询 | 错误提示；**不**展示上一次成功范围的数据（report-query loader） |

---

## 4. 投产分析 · 作者 / 运营 ROI

| 编号 | 模块 | 优先级 | 前置条件 | 步骤 | 期望结果 |
|------|------|--------|----------|------|----------|
| TC-MLive-ANA-01 | 作者 ROI | P0 | `tab=author` | 选营收/成本周期 → 查询 | `GET /admin-api/ops/roi/analysis?dimension=author` 返回 summary/rows/trend；无数据时空态「当前范围没有可核验数据」（AC-M-Live-004-1） |
| TC-MLive-ANA-02 | 作者排行 | P0 | 作者行 `liveSessionCount` > 0 | 点击 **直播场次** 数字 | 打开抽屉；列表场次与计数一致；按开播时间倒序（v20261105） |
| TC-MLive-ANA-03 | 作者排行 | P1 | `liveSessionCount` = 0 | 点击场次数字 | 不可点或无可打开 |
| TC-MLive-ANA-04 | 运营 ROI | P0 | `tab=operator` | 查询 → 「查看责任区间」 | 调 responsibility 或 analysis details kind=responsibilities（AC-M-Live-004-2） |
| TC-MLive-ANA-05 | 分析 | P1 | 有 `report` | 点击可钻取指标 | 明细与汇总同条件；导出 CSV 同口径（handoff P11） |

---

## 5. 投产分析 · 直播场次 Tab

| 编号 | 模块 | 优先级 | 前置条件 | 步骤 | 期望结果 |
|------|------|--------|----------|------|----------|
| TC-MLive-SES-01 | 场次列表 | P0 | `tab=session` | 选近 7 天 / 作者 → 查询 | 汇总场次数、成本、营收；表格可进 **直播详情** |
| TC-MLive-SES-02 | 场次列表 | P1 | 列表 >50 条 | 翻页 | 前端分页 pageSize=50 正常 |

---

## 6. 直播作业 · 平台列与录入

| 编号 | 模块 | 优先级 | 前置条件 | 步骤 | 期望结果 |
|------|------|--------|----------|------|----------|
| TC-MLive-OPS-01 | 平台列 | P0 | MEDIA 或 account 权限 | `#/ops/roi/operations?workbench=session` 打开场次列表 | 存在 **平台** 列（抖音/快手）；账号解析展示抖音号/账号名（platformAccounts） |
| TC-MLive-OPS-02 | 账号列表 | P1 | account 权限 | `workbench=account` | **平台**、**平台账号 ID** 列可见；快手/抖音区分 |
| TC-MLive-OPS-03 | 录入直播数据 | P0 | MEDIA、已存在场次 | 点击 **录入直播数据** | 打开抽屉（非整单编辑）；可填下播指标分组字段 |
| TC-MLive-OPS-04 | 录入直播数据 | P0 | 同上 | 修改指标 → 保存 | `POST/GET /admin-api/ops/roi/records` kind=session 成功；列表刷新 |
| TC-MLive-OPS-05 | 录入 | P1 | 未填最高在线/鱼塘 | 查看真实在线预览 | 提示待录入，不报错崩溃 |

---

## 7. 直播作业 · OCR + AI 核对

| 编号 | 模块 | 优先级 | 前置条件 | 步骤 | 期望结果 |
|------|------|--------|----------|------|----------|
| TC-MLive-OCR-01 | OCR | P0 | AI 模型已配置；场次已传下播截图附件 | 录入抽屉 → 选附件 → **识别下播截图** | `POST /admin-api/ops/roi/ocr/recognize` 成功；弹出 **核对截图识别结果** |
| TC-MLive-OCR-02 | 核对两列 | P0 | 识别成功 | 查看对话框 | **两列表格**展示字段；含原始识别、核对值、置信度、勾选采用 |
| TC-MLive-OCR-03 | OCR | P0 | 同上 | 勾选字段 → 确认并带入 → **保存场次** | 指标写入场次；`metricSource` 为 ocr 确认路径；未保存前不入库 |
| TC-MLive-OCR-04 | OCR | P0 | **未**配置 AI 模型 API | 发起识别 | 明确错误：「AI 模型 API 地址未配置，请在「配置管理 → AI 模型」中完善」 |
| TC-MLive-OCR-05 | OCR | P1 | 非图片/PDF | 识别 | 拒绝并提示上传截图图片 |
| TC-MLive-OCR-06 | OCR | P1 | 未选主播 | 识别 | 前端 warning「请先选择本场主播」 |

---

## 8. 筛选链 · 作者→运营→账号→场次

| 编号 | 模块 | 优先级 | 前置条件 | 步骤 | 期望结果 |
|------|------|--------|----------|------|----------|
| TC-MLive-FLT-01 | 报表筛选 | P0 | insight `tab=report` | 仅选 **运营人员** → 查询 | 作者下拉仅含该运营责任区间内作者；账号/场次同步收窄 |
| TC-MLive-FLT-02 | 报表筛选 | P0 | 同上 | 选作者 → 账号 → 场次 → 查询 | 请求带 `authorId`、`operatorUserId`、`accountId`、`sessionId` |
| TC-MLive-FLT-03 | 责任分页 | P1 | 某运营责任行 >200 条 | 选运营触发 responsibility 拉取 | 前端按 **pageSize=200** 翻页直至取全 authorId（与 `RoiResponsibilityService.page` 一致） |
| TC-MLive-FLT-04 | 账号依赖 | P1 | 未选作者 | 账号/场次下拉 | 账号需先选作者（disabled 或清空） |

---

## 9. 资金与成本 / 治理（冒烟）

| 编号 | 模块 | 优先级 | 前置条件 | 步骤 | 期望结果 |
|------|------|--------|----------|------|----------|
| TC-MLive-FIN-01 | 财务 | P1 | FINANCE | `#/ops/roi/finance?workbench=finance` | OA 财务域嵌入加载；无 session 写权限时不出现出纳登记入口混淆 |
| TC-MLive-GOV-01 | 责任 | P1 | MANAGER/ACCOUNT | `workbench=responsibility` 提交重叠区间 | 服务端拒绝冲突（handoff P04） |
| TC-MLive-GOV-02 | 审计 | P1 | auditRead | `workbench=audit` | 仅 GET；无增删改按钮（FR-M-Live-011） |

---

## 10. 边界与异常

| 编号 | 模块 | 优先级 | 前置条件 | 步骤 | 期望结果 |
|------|------|--------|----------|------|----------|
| TC-MLive-BND-01 | 退款 | P0 | 存在已退款订单 | 作者 ROI 查询含该支付日 | 订单**不计**有效现金收入；重查后口径与 handoff 02 §3 一致 |
| TC-MLive-BND-02 | 场次 12h 窗 | P0 | 同作者相邻两场；订单 pay_time 在 [开播, min(下一场,+12h)) | 查看场次归因/报表场次营收 | 订单归属符合 12 小时窗与「更近场次」规则；等距不归任何场 |
| TC-MLive-BND-03 | 分页 200 | P1 | 责任记录 >200 | API `GET .../responsibility/page?pageSize=201` | 服务端限制 pageSize ≤ **200**（或返回校验错误） |
| TC-MLive-BND-04 | AI 未配置 | P0 | 清空 AI API 配置 | OCR 识别 | 同 TC-MLive-OCR-04 |
| TC-MLive-BND-05 | 零成本 | P1 | 作者有收入无确认成本 | 查作者 ROI | 现金回收倍数为 **—**；quality 有说明（handoff P09） |
| TC-MLive-BND-06 | 数据覆盖 | P1 | 查询超出独立库 historyEnd | 页面 quality / 提示 | 展示数据覆盖区间；不把空值解释成「无交易」（handoff P12） |
| TC-MLive-BND-07 | 性能 | P2 | 全作者大范围 | 分析查询 | 允许 10s 级等待；不宣称秒级 BI（handoff 02 §9） |
| TC-MLive-BND-08 | 原生范围 | P0 | 未配置 native 边界账号 | 访问 authorized-data 或窄范围 API | 明确 403/业务拒绝，无全站泄漏（handoff P17） |

---

## 11. handoff P01–P18 映射

| handoff | 场景摘要 | 对应用例 / FR |
|---------|----------|----------------|
| P01 | 多账号同批订单不重复计收入 | TC-MLive-ANA-01 + 数据核对 |
| P02 | 跨作者同客户按订单作者归集 | TC-MLive-ANA-01 |
| P03 | 交接时刻半开区间 | TC-MLive-ANA-04 |
| P04 | 重叠责任拒绝 | TC-MLive-GOV-01 |
| P05 | 无历史责任 → 未分配 | TC-MLive-ANA-04 |
| P06 | 同日交接费用仅日期 | 财务/责任联调（P1 手工） |
| P07 | 同源费用只计一次 | 资金 cost-ledger（P1） |
| P08 | 来源金额变更冻结 | 资金 cost-ledger（P1） |
| P09 | 零成本/资料缺失 → — | TC-MLive-BND-05 |
| P10 | 首购复购重叠标签 | TC-MLive-ANA-01 明细 |
| P11 | 列表/导出同条件 | TC-MLive-ANA-05 |
| P12 | 超覆盖期提示 | TC-MLive-BND-06 |
| P13 | 跨业务线客户隔离 | TC-MLive-BND-08 |
| P14 | MEDIA 费用草稿 | 直播作业 expense 抽屉（P1） |
| P15 | 只读下级不可写 | media-delegations（P1） |
| P16 | 撤权下次请求失效 | media-delegations（P1） |
| P17 | 未配置原生范围拒绝 | TC-MLive-BND-08 |
| P18 | 旧页未适配拒绝 | 非 ROI 核心路径（P2 记录） |

---

## 12. M5 边界

| 编号 | 模块 | 优先级 | 前置条件 | 步骤 | 期望结果 |
|------|------|--------|----------|------|----------|
| TC-MLive-M5-01 | 路由 | P0 | 任意 | 对比 `#/ops/finance/roi-analysis` 与 `#/ops/roi/insight` | 不同页面、不同公式文案（TESTCASES-M5-002 vs FR-M-Live-003） |
| TC-MLive-M5-02 | 菜单 | P1 | 财务角色 | 侧栏 | 「ROI 分析」在 **财务管理**；「抖音投产管理」在 **直播** |

---

## 测试环境

| 项 | 值 |
|----|-----|
| 启动脚本 | `.\scripts\start-ops-dev.ps1`（仓库根目录） |
| Gateway | **http://localhost:48080** |
| ops-server | **http://localhost:48094**（ROI API 实际处理） |
| 前端 | **http://localhost:5777** |
| 登录 | **admin** / **admin123**，租户 **1** |
| 请求头 | 联调时 **X-Tenant-Id: 1**（与 [OPS-DEV-DEPLOY-GUIDE.md](../OPS-DEV-DEPLOY-GUIDE.md) 一致） |
| AI OCR | 管理端 **配置管理 → AI 模型**：配置 Vision 可用模型后再测 TC-MLive-OCR-* |
| 参考文档 | [OPS-DEV-DEPLOY-GUIDE.md](../OPS-DEV-DEPLOY-GUIDE.md) · [ADR-084](../../adr/ADR-084-ROI-unify-ops-server.md) |

---

## 变更记录

| 日期 | 说明 |
|------|------|
| 2026-11-05 | 首版：整合 FR-M-Live、handoff P01–P18、ADR-084、20261105 insight/场次/OCR/筛选/平台列/12h 窗 |

---

*执行用例时使用合成或授权测试数据；禁止真实付款、审批、生产 ACL 写入。*
