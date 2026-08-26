# ADR-070：Ops 抓取统一 XXL-JOB 调度（撤销 ADR-001 §2/§4 XXL-JOB 禁令）

| 字段 | 值 |
|------|---|
| 编号 | ADR-070 |
| 标题 | Ops 抓取统一 XXL-JOB 调度（撤销 ADR-001 §2/§4 XXL-JOB 禁令） |
| 状态 | **Accepted**（§6 待决议项已关闭 · 2026-08-17） |
| 日期 | 2026-08-17 |
| 决策人 | 架构 / Ops Owner |
| 关联 | [ADR-001](./ADR-001-中间件简化.md) · [ADR-047](./ADR-047-Football-Ops平台集成决策.md) · [ADR-049](./ADR-049-M10-全量采集与展示桥接.md) · [ADR-056](./ADR-056-Football用户身份SSOT.md) · [ADR-061](./ADR-061-租户级统一采集任务.md) · [ADR-068](./ADR-068-M10-统一外部数据采集任务.md) · [ADR-069](./ADR-069-采集完成阈值预警触发.md) · [PRD-M10 §2.2 / §4.1.3](../product/PRD-M10-数据采集.md) · [CHECKLIST-M10 §3](../delivery/CHECKLIST-M10-数据采集.md) · [MASTER-EXECUTION-TRACKER §1 / §13](./../delivery/MASTER-EXECUTION-TRACKER.md) |
| 触发 | Ops 多租户 cron 调度与 Football 同源 7 个 service（member/mp/pay/system/wecom/live/match）不一致；@Scheduled 改一次必须走 release；多租户靠 Service 内部手动 `selectList` + `TenantContextHolder.set` 循环，重复模板散落 2 处 |

---

## 1. 背景

Ops standalone 服务（`football-module-ops-server`，`spring.application.name=ops-server`）当前有 **2 处** `org.springframework.scheduling.annotation.Scheduled` cron 触发，分别承担「采集任务扫描」与「采集完成阈值兜底」：

| # | 类 | 方法 | cron | 触发语义 |
|---|----|------|------|----------|
| 1 | `football.module.ops.service.collect.CollectCronScheduler` | `scanDueTasks()` | `${oa.collect.schedule.scan-cron:0 * * * * ?}` | 每分钟扫描 RUNNING + `next_run_at` 到期的统一任务行（ADR-061），逐租户手动 `TenantContextHolder.setTenantId(...)` → `UnifiedCollectRunService.run(taskId)` |
| 2 | `football.module.ops.service.monitor.MonitorAlertScanner` | `scanCurrentTenant()` | `0 0/30 * * * ?` | ADR-069 P1 stub：每 30 分钟兜底扫描当前租户阈值（`CollectThresholdTriggerService.evaluateTenantIncremental(tenantId)`） |

### 1.1 现状依赖

- `football-module-ops-server/pom.xml` **未**引入 `football-spring-boot-starter-job`
- `application.yaml` 中无 `xxl.job.*` 配置（仅 `mp` 模块的 `application.yaml` 保留了 `xxl.job.executor.appname=${spring.application.name}` 模板，可作参考）
- 当前依赖为 Spring Boot 3.x `@Scheduled(cron=...)` + 内置 `TaskScheduler`
- Football 同源其它 7 个 service（member/mp/pay/system/wecom/live/match）均通过 `football-spring-boot-starter-job` + `@XxlJob("xxx")` + `@TenantJob` 标准模板接入 xxl-job-admin，多租户由 `TenantJobAspect` AOP 自动循环（已有参考实现：`football.module.member.job.ArticleJobHandler#batchUpdateOnSaleNum`）

### 1.2 痛点

| # | 痛点 | 后果 |
|---|------|------|
| P1 | **多租户靠手动遍历**：`CollectCronScheduler.scanDueTasks` 用 `LambdaQueryWrapper` 查 200 条 `RUNNING` 任务行 → 在循环里手工 `TenantContextHolder.setTenantId(task.getTenantId())` → finally `clear()`；`MonitorAlertScanner.scanCurrentTenant` 干脆只扫当前租户（依赖外层调用方保证 `TenantContextHolder` 已注入） | 重复模板散落 2 处；任何漏写 `clear()` 即跨租户污染；`scanCurrentTenant` 必须由其它带租户上下文的入口触发才能生效，P1 stub 的全租户扫描语义无法兑现 |
| P2 | **cron 改一次要走 release**：`@Scheduled(cron=...)` 编译期硬编码（即便 `${oa.collect.schedule.scan-cron}` 可走 sys_param，目前也只在 `oa.collect.schedule.*` 单一入口配置；`MonitorAlertScanner.scanCurrentTenant` 的 `0 0/30 * * * ?` 完全硬编码） | 调频次 / 启停必须发版；运营/架构无 dashboard 可观测 |
| P3 | **与 Football 其他 7 个 service 不一致**：member/mp/pay/system/wecom/live/match 均走 xxl-job-admin + `@TenantJob`；Ops 单独用 `@Scheduled` | Ops 工程师必须掌握两套调度栈；多服务统一调度策略、依赖追踪、失败告警、并发限流无法共享 |
| P4 | **失败重试与告警能力缺失**：xxl-job-admin 内置失败重试 / 阻塞告警 / 执行日志，`@Scheduled` 只靠 try/catch + 日志 | 失败任务无独立重试轨迹；值班无 dashboard 入口 |
| P5 | **多租户 cron 不能按租户差异化**：ADR-069 P1 stub 设计是「按租户 cron」来自 `sys_param`（`MonitorScanParamSupport#PARAM_KEY`），但 `@Scheduled` 只能写一个固定 cron | P1 stub 兑现需要先把每个租户的 cron 全部迁移到 xxl-job-admin 才能按租户调度 |

### 1.3 ADR-001 §2 / §4 现状冲突

[ADR-001](./ADR-001-中间件简化.md) §2 决策矩阵明确「XXL-JOB 分布式调度 → Spring `@Scheduled` + 数据库任务表」；§4「🔴 不引入 XXL-JOB：调度用 `@Scheduled` + 任务表」。同时 ADR-001 §3.3 留口子「必要时引入 XXL-JOB（分布式调度）— 每次引入需新开 ADR」。

本 ADR 即兑现该口子，针对 **Ops 抓取**范围撤销禁令、引入 XXL-JOB。

---

## 2. 决策（**Accepted** · §6 全部决议项已关闭）

| # | 决策 |
|---|------|
| **D1** | **撤销 [ADR-001 §2 / §4](./ADR-001-中间件简化.md) 中关于「不引入 XXL-JOB」的禁令**，但**仅限** Ops 抓取调度（§3 范围）。Redis / RabbitMQ / MinIO 三条禁令继续生效。 |
| **D2** | Ops 抓取类调度（采集 cron 扫描 + 阈值兜底扫描）**统一**走 `football-spring-boot-starter-job`（`com.xxl.job.core.handler.annotation.XxlJob` + `football.framework.tenant.core.job.TenantJob`）。 |
| **D3** | 多租户语义由 **`@TenantJob` AOP**（`TenantJobAspect.around`）自动循环注入 `TenantContextHolder` 触发；**禁止** JobHandler 内部手工 `TenantContextHolder.set/clear`，与 `football.module.member.job.ArticleJobHandler` 同模板。 |
| **D4** | 保留 ADR-069 阈值兜底语义不变（cron `0 0/30 * * * ?`、FANS/WORK 双轨阈值判定、`evaluateTenantIncremental` 主路径）；仅替换触发方式（`@Scheduled` → `@XxlJob("monitorAlertScanJobHandler")` + `@TenantJob`）。 |
| **D5** | 触发频次映射：原 `0 * * * * ?` 与 `0 0/30 * * * ?` 直接迁移到 xxl-job-admin 的 cron 表达式配置（不改频率）；调度中心的 cron 调整走 xxl-job-admin 操作面板，**不**改代码。 |
| **D6** | 现有 cron 来源分两类处理：`oa.collect.schedule.scan-cron`（`sys_param`）保留作为 SSOT 的 fallback；首次注册 JobHandler 时若 xxl-job-admin 无 cron 配置，则按此 fallback 注册。`MonitorAlertScanner` 改走 xxl-job-admin 后，每租户 cron 来自 `MonitorScanParamSupport`（ADR-069 P1 全量实现）按租户独立注册到 xxl-job-admin。 |
| **D7** | 不在 ops-server 引入新独立 xxl-job-admin；executor appname 与 admin 地址见 §6 待决议项。 |

### 2.1 不在决策范围（保持现状）

- Redis / RabbitMQ / MinIO 三条禁令（ADR-001 §2 / §4）继续生效
- 其它 Ops 模块（M1/M2/M3/M4/M5/M6/M7/M8/M9）现有 `@Scheduled` 不在本 ADR 范围
- 不动 `football-spring-boot-starter-job` / `football-spring-boot-starter-biz-tenant` framework 任何代码
- 不动 mp/wecom/member/match/live/infra/pay/system 任一服务

---

## 3. 适用范围

| 维度 | 范围 |
|------|------|
| **服务** | `football-module-ops-server`（`spring.application.name=ops-server`，运行端口 `48094`） |
| **改造类** | 仅 `CollectCronScheduler` + `MonitorAlertScanner` 两处 JobHandler |
| **未触碰** | `football-module-mp` / `football-module-wecom` / `football-module-member` / `football-module-match` / `football-module-live` / `football-module-infra` / `football-module-pay` / `football-module-system` 现有 `@Scheduled` / `@XxlJob` 全部不动 |
| **依赖范围** | `football-module-ops-server/pom.xml` 新增 `football-spring-boot-starter-job` 依赖 |
| **配置范围** | `football-module-ops-server/src/main/resources/application.yaml` 新增 `xxl.job.*` 配置块（executor appname / admin address / accessToken / logPath） |
| **数据库** | 无 schema 变更 |
| **前端** | 无变更 |

---

## 4. 影响面

### 4.1 代码层改动范围（**仅列清单 · 本 ADR 不实施 · 实施须开 Slice**）

| # | 文件 | 改动类型 | 备注 |
|---|------|---------|------|
| 1 | `football-module-ops-server/pom.xml` | 新增依赖 | `<dependency><groupId>football.framework</groupId><artifactId>football-spring-boot-starter-job</artifactId></dependency>` |
| 2 | `football-module-ops-server/src/main/resources/application.yaml` | 新增配置块 | `xxl.job.executor.{appname,address,ip,port,logpath,logretentiondays}` + `xxl.job.admin.{addresses,accessToken}` + `xxl.job.enabled: true`；**`xxl.job.executor.appname` 必须显式写死为 `football-ops-executor`**（与 mp 模板 `${spring.application.name}` **不同** — ops-server 的 `spring.application.name=ops-server` 与 executor appname 解耦），**禁止**引用 `${spring.application.name}`，避免与 mp / member / pay 等其它 executor 路由串扰 |
| 3 | `football.module.ops.service.collect.CollectCronScheduler` | `@Scheduled` → `@XxlJob("collectCronScanJobHandler")` + `@TenantJob`；保留现有 `scanDueTasks()` 方法签名（多租户循环由 `@TenantJob` AOP 替代手写 `TenantContextHolder.set`） | 移除 `@Scheduled` import；新增 `com.xxl.job.core.handler.annotation.XxlJob` + `football.framework.tenant.core.job.TenantJob` |
| 4 | `football.module.ops.service.monitor.MonitorAlertScanner` | 同上 → `@XxlJob("monitorAlertScanJobHandler")` + `@TenantJob` | 移除 `@Scheduled`；`scanCurrentTenant()` 内 `TenantContextHolder.getTenantId()` 检查随之移除（`@TenantJob` AOP 保证） |

> **未触碰**：`CollectProperties.schedule`（保留 `enabled` / `scan-cron` 配置语义，作为 xxl-job-admin 未注册时的 fallback）、`UnifiedCollectRunService`、`CollectThresholdTriggerService`、`MonitorScanParamSupport`、`M10ColCollectScheduleS02IT` 等所有 IT（须同步迁移到 `XxlJobExecutor` 测试模式，见 §5.2）。

### 4.2 数据 / 接口影响

| 维度 | 影响 |
|------|------|
| **REST API** | **无变更**。本 ADR 不新增 / 修改任何 Controller 或 Service API |
| **数据库 schema** | **无变更**。无 Flyway 迁移 |
| **数据字典** | **无变更** |
| **行为差异** | JobHandler 实际执行语义不变（采集 cron 扫描 + 阈值兜底扫描）；仅替换触发源；多租户语义由 `@TenantJob` AOP 接管 |

### 4.3 关联 Spec / 文档同步（**本 ADR 同步落地**）

| 文档 | 章节 | 改动 |
|------|------|------|
| [PRD-M10](../product/PRD-M10-数据采集.md) | §2.2 Out of Scope §1「不实现 XXL-JOB」 | **删除**，原句改为 `XXL-JOB 已引入（ADR-070）` |
| [PRD-M10](../product/PRD-M10-数据采集.md) | §4.1.3 业务规则第 1 条「调度：Spring `@Scheduled`」 | 改为 `调度：xxl-job + @TenantJob（ADR-070）` |
| [PRD-M10](../product/PRD-M10-数据采集.md) | §4.1.4 AC-M10-001-4「启动后 Spring 调度生效」 | 改为 `启动后 ops-server 在 xxl-job-admin 注册成功，`@TenantJob` 多租户循环生效` |
| [PRD-M10](../product/PRD-M10-数据采集.md) | §5 决策表 ADR-M10-001 行 | 改为 `XXL-JOB + @TenantJob 多租户循环（ADR-070 撤销 ADR-001 §2/§4 禁令）` |
| [CHECKLIST-M10](../delivery/CHECKLIST-M10-数据采集.md) | §3「不依赖 XXL-JOB → 用 Spring `@Scheduled`」 | **删除 / 替换**为 xxl-job 注册 + `@XxlJob` + `@TenantJob` 生效 3 条 |
| [MASTER-EXECUTION-TRACKER](./../delivery/MASTER-EXECUTION-TRACKER.md) | §13 阻塞表 | 新增 1 行「ADR-070 待批准」 |
| [MASTER-EXECUTION-TRACKER](./../delivery/MASTER-EXECUTION-TRACKER.md) | §17.1 M10 三通道采集 P2-M10-A 行 | 追加 `ADR-070` 链接（约束本 ADR 已采纳即生效） |
| [TECH-CONSTRAINTS](../engineering/TECH-CONSTRAINTS.md §1.1) | 后续 Slice 实施时 | 新增条目「Ops 抓取调度走 xxl-job + @TenantJob（ADR-070）」，与 Redis/RabbitMQ/MinIO 三条禁令共存 |

### 4.4 ADR-069 P1 stub 同步改造（Q6 · 2026-08-17 决议）

承接 ADR-070 §6 Q6 决议，本次 Slice 在「采集 cron 扫描」迁移的同时，**一并把 ADR-069 §3.2 / §4 P1 stub「全租户定时扫描」**同步迁移到 xxl-job + `@TenantJob`，不再走 `@Scheduled`。理由：

1. 原 P1 stub 设计语义为「全租户扫描」，但 `@Scheduled` + `scanCurrentTenant()` 单租户内串行实现，无法兑现全租户遍历；
2. 若本次只迁移 `CollectCronScheduler`、保留 `MonitorAlertScanner` 仍走 `@Scheduled`，会留下「Ops 抓取一半走 xxl-job、一半走 `@Scheduled`」的双轨状态，与 ADR-070 D3 / §1.2 P3「统一栈」目标冲突；
3. 多租户兜底语义对 `@TenantJob` AOP 的依赖，与 `CollectCronScheduler` 完全一致 — 一起迁移避免后续返工。

#### 4.4.1 P1 stub 范围与文件清单

> 注：当前仓库 `football-backend-saas/football-module-ops/football-module-ops-server` 实际 Java 源树中尚未包含 `MonitorAlertScanner` / `CollectCronScheduler` 实现（ADR-069 P0 仅完成 Hook + judge + notify，`MonitorAlertScanner` 与 `scanCurrentTenant()` 方法仍属 ADR-069 P1 计划新建 + ADR-070 Slice 实施期新建）。下列「最终路径」即为 ADR-070 Slice 落地后该类在仓库中的预期位置。SSOT 引用来自 ADR-069 §3.2：

| # | 最终路径 | 类型 | 来源 ADR | 改动 |
|---|----------|------|----------|------|
| 1 | `football-backend-saas/football-module-ops/football-module-ops-server/src/main/java/football/module/ops/service/monitor/MonitorAlertScanner.java` | 新建 Java 类 | [ADR-069 §3.2](../adr/ADR-069-采集完成阈值预警触发.md) P1 | `@Scheduled("0 0/30 * * * ?")` → `@XxlJob("monitorAlertScanJobHandler")` + `@TenantJob`；保留 `scanCurrentTenant()` 方法签名（多租户循环由 `@TenantJob` AOP 自动接管） |
| 2 | 同上文件，类级别 | 注解 | ADR-070 §2 D3 | 新增 `import com.xxl.job.core.handler.annotation.XxlJob; import football.framework.tenant.core.job.TenantJob;`，移除 `org.springframework.scheduling.annotation.Scheduled` |
| 3 | 同上文件，方法 `scanCurrentTenant()` 内 | 行 11-13（建议锚点） | ADR-070 §2 D3 | 删除手动 `TenantContextHolder.getTenantId()` 的「单租户假定」检查 — `@TenantJob` AOP 保证每租户触发一次 |
| 4 | `football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/application.yaml` | yaml `xxl.job.*` 配置块 | ADR-070 §4.1 row 2 | 同步加上 `monitorAlertScanJobHandler` 在 xxl-job-admin 端的 cron 配置（默认 `0 0/30 * * * ?`，与 §6 Q4 logPath / §5.2 S4 一致） |
| 5 | `football-backend-saas/football-module-ops/football-module-ops-server/pom.xml` | 新增依赖 | ADR-070 §4.1 row 1 | `<dependency><groupId>football.framework</groupId><artifactId>football-spring-boot-starter-job</artifactId></dependency>`（与 `CollectCronScheduler` 共享同一依赖，**不**重复新增） |

未触碰（继承自 §4.1 「未触碰」段）：

- `CollectThresholdTriggerService` / `ThresholdJudgeService` / `ThresholdMetricResolver` / `ThresholdAlertRecipientResolver`（ADR-069 §3.2 新增组件）— 业务判定逻辑不变
- `MonitorScanParamSupport`（ADR-069 P1 stub 的按租户 cron SSOT 解析器） — 仅在 §2 D6 「按租户独立注册」路径上保留作 `MonitorAlertScanner` 内的 fallback
- `MonitorServiceImpl` / `NotificationServiceImpl` — 通知通道与去重不变

#### 4.4.2 验收点（与 §5.2 联动）

- 与 §5.2 T1~T8 并列：新增 **T9 ADR-069 P1 多租户遍历**：tenant=1 DISABLED + tenant=2 + tenant=3 各 1 条账户，`monitorAlertScanJobHandler` 触发 → tenant=1 跳过 0 次；tenant=2 / tenant=3 各 `evaluateTenantIncremental` 1 次；与「`@TenantJob` 跳过 `DISABLED` 租户」语义一致
- 验收记录须在 M10-COL-Phase2-A Gate Sign-off 报告（路径 `docs/delivery/gates/GATE-M10-Phase2-A-报告-{YYYYMMDD}.md`，待 ADR-070 Slice 启动时落地）中勾选「P1 stub 多租户触发 ✓」

> **风险备注**：若 ADR-070 Slice 实施期间发现 `MonitorAlertScanner` 因 M10 P1 整体推迟而尚未新建，§4.4.1 row 1 即视为「本 Slice 一并新建 + 迁移」的合并工作；不可拆分为独立 ADR，避免双轨态。

---

## 5. 实施路径（**待批准后开 Slice · 本 ADR 不实施**）

### 5.1 步骤（5 步 · 与现有 M10 Slice 模板对齐，**S5 为 ADR-069 P1 同步改造**）

| 步骤 | 内容 | 验证 |
|------|------|------|
| **S1** | `pom.xml` 加依赖 + `mvn dependency:tree` 确认 `football-spring-boot-starter-job` 与 starter-biz-tenant 已传递引入 | `mvn -pl football-module-ops-server dependency:tree` 包含 `football-spring-boot-starter-job`、`football.framework:tenant` |
| **S2** | `application.yaml` 新增 `xxl.job.*` 配置块；executor appname / admin address 见 §6（**`appname=football-ops-executor` 显式写死**，见 §6 Q2） | ops-server 启动日志打印 `XxlJobExecutor` 初始化成功；本地联调 xxl-job-admin「执行器管理」可见 appname `football-ops-executor` |
| **S3** | `CollectCronScheduler` + `MonitorAlertScanner` 改 `@XxlJob + @TenantJob`；IT `M10ColCollectScheduleS02IT` 等同步迁移到 `XxlJobExecutor` 单元 / 集成测试模式 | `mvn -pl football-module-ops-server test` 全绿；`TenantJobAspectIT`（新增）覆盖多租户循环 |
| **S4** | 本地启动 ops-server + 本地 xxl-job-admin → 在 admin「任务管理」注册 `collectCronScanJobHandler`（cron `0 * * * * ?`）与 `monitorAlertScanJobHandler`（cron `0 0/30 * * * ?`，cron 注册时配置 **retry=3, retry-interval=1/5/15min**，见 §6 Q5）→ 触发一次 → 看执行日志 + 多租户遍历 | 租户 1 / 租户 2 各触发一次；`oa_collect_log` 增量写入；`@TenantJob` AOP 切到对应租户上下文 |
| **S5** | **ADR-069 P1 stub 同步迁移**（承接 §4.4）：若 `MonitorAlertScanner.java` 在 S3 时尚未存在（ADR-069 P1 未实施），本步合并「新建 + 迁移」一次性完成 → `@XxlJob("monitorAlertScanJobHandler")` + `@TenantJob`；IT `MonitorAlertScannerIT`（新增）覆盖「DISABLED 租户 0 次 + ENABLED 租户各 1 次」（见 §5.2 Q6 行） | `mvn -pl football-module-ops-server test -Dtest=MonitorAlertScannerIT` 全绿；本地 admin 「调度日志」可见禁用租户被 AOP 自动跳过 |

### 5.2 回归测试点

| # | 维度 | 验收 |
|---|------|------|
| T1 | **单租户 happy path** | tenant=1 启用 1 条统一任务 → `collectCronScanJobHandler` 触发 → `UnifiedCollectRunService.run` 成功 → `oa_collect_log` 1 行 `SUCCESS` |
| T2 | **多租户遍历** | tenant=1 + tenant=2 各 1 条统一任务 → `collectCronScanJobHandler` 触发 → 两条任务均被 run；`@TenantJob` AOP 注入 `tenant_id=1`、`tenant_id=2` 各一次；无跨租户污染 |
| T3 | **失败重试** | `UnifiedCollectRunService.run` 抛异常 → xxl-job-admin 失败重试 N 次（默认 0，需在 cron 配置页调整）；`oa_collect_log` 写入 FAILED；与原 `@Scheduled` 失败语义一致 |
| T4 | **禁用租户跳过** | tenant=1 任务 `status=DISABLED` → `collectCronScanJobHandler` 不进入 run 路径（保留现有 `LambdaQueryWrapper eq status=RUNNING` 过滤） |
| T5 | **频次等价** | 旧 `@Scheduled(cron="0 * * * * ?")` 与新 `@XxlJob("...cron=0 * * * * ?")` 在 1 小时窗口内的触发次数差异 ≤ 1（容忍时钟漂移） |
| T6 | **阈值兜底等价** | `monitorAlertScanJobHandler` 触发后 `CollectThresholdTriggerService.evaluateTenantIncremental(tenantId)` 行为不变；`oa_notification_event` 去重（ADR-026）不受影响 |
| T7 | **mvn verify** | ops-server 模块 `mvn verify` 全绿；JaCoCo 覆盖率持平或上升 |
| T8 | **租户禁用回退** | 关 `xxl.job.enabled=true` → ops-server 不向 xxl-job-admin 注册 → JobHandler 不被触发（与回滚到 `@Scheduled` 等价，见 §5.3） |
| **Q6 / T9** | **ADR-069 P1 stub 多租户触发** | 配置 tenant=1 `status=DISABLED` + tenant=2 / tenant=3 各 1 条账户（含阈值规则） → 触发 `monitorAlertScanJobHandler` → 断言 `CollectThresholdTriggerService.evaluateTenantIncremental` 在 tenant=1 调度次数为 **0**，在 tenant=2 / tenant=3 各为 **1**；`@TenantJob` AOP 按 `TenantDO.status=ENABLED` 自动跳过禁用租户；与 ADR-069 §3.2 P1 stub「全租户遍历」语义一致 |

### 5.3 Rollback

| 步骤 | 动作 |
|------|------|
| R1 | `application.yaml` `xxl.job.enabled: false` |
| R2 | `CollectCronScheduler` + `MonitorAlertScanner` 还原 `@Scheduled(cron=...)` 注解 + 移除 `@XxlJob` / `@TenantJob` |
| R3 | `pom.xml` 移除 `football-spring-boot-starter-job` 依赖 |
| R4 | ops-server 重启 → 行为与本 ADR 实施前等价（已生产数据无 schema 变更，无须迁移脚本） |

> **风险**：R2 还原 `@Scheduled` 后，多租户遍历语义从「`@TenantJob` AOP 自动循环」回退到「Service 内部手写 `TenantContextHolder.set/clear`」；若实施期已重构掉手写模板，需同步还原。

---

## 6. 决议项（**2026-08-17 全部关闭 · 转 Accepted**）

> 用户决议已合并；下列为最终值，取代 Proposed 阶段的「推荐 / 风险」列。实施期 Slice 直接引用。

| # | 决议项 | ✅ 最终决议（2026-08-17） | 关联 |
|---|--------|-------------------------|------|
| **Q1** | xxl-job-admin 调度中心地址 | **A) 复用 `football-module-mp` 现有 admin** — 最小改动 + 与 mp 共用 | `application.yaml` `xxl.job.admin.addresses` 与 `xxl.job.admin.accessToken` 字段直接对齐 mp 模块；运维 / 架构确认 mp admin 已支持多 executor appname 注册（xxl-job-admin 标准能力） |
| **Q2** | executor appname 命名 | **C) `football-ops-executor`** — **与 mp 模板 `${spring.application.name}` 不同**；`application.yaml` `xxl.job.executor.appname` 必须**显式写死**为字面量 `football-ops-executor`，**禁止**引用 `${spring.application.name}`（ops-server 的 `spring.application.name=ops-server` 与 executor appname 在本项目刻意解耦）；避免与 mp / member / pay / system 等其它 executor 在 xxl-job-admin「执行器管理」页面路由串扰 | §4.1 row 2 · §4.4.1 row 4 |
| **Q3** | admin accessToken | 沿用 mp admin 默认 token **`a1b2c3d4e5f67890`**；**待运维 / 架构确认 token 泄露面**（与其它 executor 共享同一 token，等同 token 信任域扩大）；后续如需轮换须开 ADR-XXX（不在本 ADR 范围） | `application.yaml` `xxl.job.admin.accessToken` |
| **Q4** | logPath | `${user.home}/logs/xxl-job/ops-server` — 与 mp 模板对齐 | `application.yaml` `xxl.job.executor.logpath` |
| **Q5** | 失败重试策略 | **Ops 抓取历史「3 次指数退避」1/5/15min** — 在 xxl-job-admin 「任务管理」cron 注册时设 `retry-count=3, retry-interval=` 「1 分钟 → 5 分钟 → 15 分钟」梯度；覆盖 `collectCronScanJobHandler` 与 `monitorAlertScanJobHandler` 两个 JobHandler | §5.1 S4 · CHECKLIST-M10 §3 · PRD-M10 §4.1.4 AC-M10-001-5 |
| **Q6** | ADR-069 P1 stub 全租户 cron | **本 ADR 一并处理**：ADR-069 P1 stub「全租户遍历」**同步**切到 xxl-job + `@TenantJob`，删除「本 ADR 范围不实施 P1」的最初表述；详见 §4.4「ADR-069 P1 同步改造」；验收点见 §5.2 T9 与 §5.1 S5 | §4.4 / §5.1 S5 / §5.2 T9 |

---

## 7. 关联 ADR 引用

- [ADR-001 §2 / §4](./ADR-001-中间件简化.md) — 本 ADR 撤销范围（仅 §2/§4 XXL-JOB 禁令，Redis/RabbitMQ/MinIO 三条禁令继续生效）
- [ADR-049](./ADR-049-M10-全量采集与展示桥接.md) — 采集任务全量 dataType 顺序；本 ADR 不修改
- [ADR-061](./ADR-061-租户级统一采集任务.md) — 统一任务行 `is_unified=1` + cron 来自 `sys_param.collect.schedule.cron`；本 ADR 不修改 schema，但 cron SSOT 迁移到 xxl-job-admin 后语义对齐
- [ADR-068](./ADR-068-M10-统一外部数据采集任务.md) — 外部竞品 Channel-D 同样走 `CollectCronScheduler` 触发；本 ADR 适用范围一并覆盖
- [ADR-069](./ADR-069-采集完成阈值预警触发.md) — `MonitorAlertScanner.scanCurrentTenant` + `CollectThresholdTriggerService.evaluateTenantIncremental` 阈值兜底；本 ADR 仅替换 `@Scheduled` 触发方式，业务语义保留

---

## 8. 变更记录

| 日期 | 说明 |
|------|------|
| 2026-08-17 | Draft（Proposed · 待批准 · §6 待决议项关闭后落地） |
| 2026-08-17 | **Accepted** · §6 Q1–Q6 全部关闭 · 新增 §4.4「ADR-069 P1 同步改造」 · 状态由 Proposed 升级为 Accepted |