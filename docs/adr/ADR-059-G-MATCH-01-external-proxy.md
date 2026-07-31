# ADR-059：G-MATCH-01 OPS Match 接受外部 HTTP 代理终态

| 字段 | 值 |
|------|-----|
| 编号 | ADR-059 |
| 标题 | G-MATCH-01 赛事选择器继续外部 HTTP 代理，不切 match-server Feign |
| 状态 | **Accepted / Closed**（2026-07-31，执行计划 **P-F 路径 A**） |
| 日期 | 2026-07-31 |
| 决策人 | 架构 / 产品（确认既有 Spec，无新 Feign 要求） |
| 关联 | [ADR-016 §2.7](./ADR-016-M2-节点类型与任务内容关联.md) · PRD-M2 **BLK-M2-004** · [API-M2-计划管理 §11](../engineering/API-M2-计划管理.md) · [FULL-MERGE G-MATCH-01](../delivery/OPS-FOOTBALL-FULL-MERGE-RPC-ANALYSIS.md) · [终态缺口 P-F](../delivery/OPS-FOOTBALL-终态缺口执行计划-20260731.md) |
| Supersedes（缺口态） | FULL-MERGE **G-MATCH-01**「确认 Feign 或继续封装」→ **Closed-Accept 外部代理** |

---

## 1. 背景

FULL-MERGE 将 **G-MATCH-01**（赛事选择器）标为「确认走 match Feign 还是继续 OPS 封装」，状态未闭环。

既有 Spec **已决**外部代理，**未**要求复用 `match-server` RPC：

| Spec | 结论 |
|------|------|
| PRD-M2 **BLK-M2-004**（2026-06-12） | 后端代理赛事 list + `MatchSelectDialog`；`competitionId` = 外部 `scheduleId` |
| **ADR-016 §2.7** | 外部源 `https://h5.shenyu.com/app-api/match`；平台契约经 OPS 转发 |
| **API-M2-计划管理 §11** | `GET …/match/list` · `…/match/leagues`；配置 `oa.match.api-base-url` |

运行时（`football-module-ops`）已按此落地，**无** Match*Api Feign：

- Controller：`MatchController` → `@RequestMapping("/admin-api/ops/match")`（`/list` · `/leagues`）
- 服务：`MatchProxyService` → Hutool `HttpRequest` 转发配置基址下的 `/list`、`/filter/competitions/flat`
- 配置：`oa.match.api-base-url` 默认 `https://h5.shenyu.com/app-api/match`（可覆盖）

MUST-HAVE / ADR-050-REV1 **白名单无** Match Feign 条目；切 `match-server` 属新产品/Spec 变更，非本期缺口强制项。

---

## 2. 决策

| # | 决策 | 说明 |
|---|------|------|
| D1 | **接受外部 HTTP 代理为终态** | OPS Match = `MatchProxyService` → 配置 `api-base-url`（默认 h5 `/app-api/match`），**不是** `match-server` Feign |
| D2 | **关闭 G-MATCH-01** | 状态 **Accepted / Closed**（Closed-Accept）；FULL-MERGE / 执行计划 G3-MATCH 勾销 |
| D3 | **本期不做 Feign 重写** | Spec 未强制；路径 B（Vendor Match*Api）仅当新产品/Spec 明文要求复用 match-server 后再开 Slice |
| D4 | **对外路径** | 平台契约前缀 `/admin-api/ops/match/**`（P-C 后；原 Spec 文档中的 `/admin-api/oa/match` 为历史路径） |
| D5 | **与 Std3 关系** | Match **不算**「跨 football 服务 Feign 缺口」；属已决外部依赖封装，不阻塞四标准终态收口 |

---

## 3. 后果

- 无代码变更；计划页赛事选择器继续走现有代理
- G-MATCH-01 不再出现在「待确认」缺口表
- 若未来产品要求 match-server RPC：另开 Slice + ADR，契约对表后再替换 `MatchProxyService` 实现

---

## 4. 运行时证据（路径 A DoD）

| 项 | 位置 |
|----|------|
| Controller | `football-module-ops-server/.../controller/match/MatchController.java` · `/admin-api/ops/match` |
| Proxy | `.../service/match/MatchProxyService.java` · `HttpRequest.get(apiBaseUrl + path)` |
| Config | `application.yaml` · `oa.match.api-base-url` |
| Spec | ADR-016 §2.7 · API-M2 §11 · BLK-M2-004 |

---

## 5. 变更记录

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-07-31 | Agent | 初稿；P-F 路径 A；关闭 G-MATCH-01 |
