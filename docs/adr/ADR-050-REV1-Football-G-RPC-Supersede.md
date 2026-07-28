# ADR-050-REV1：Football G-* RPC 有限 Supersede §3.1

| 字段 | 值 |
|------|-----|
| 编号 | ADR-050-REV1 |
| 标题 | Football G-* RPC 有限 Supersede ADR-050 §3.1 |
| 状态 | **Accepted**（2026-07-28，产品/架构拍板） |
| 日期 | 2026-07-28 |
| 决策人 | 产品 / 架构 |
| Supersedes | [ADR-050-Ops与Football多库复用总纲](./ADR-050-Ops与Football多库复用总纲.md) **§3.1 部分**（D10 字面「禁止改 Football 业务代码」） |
| 关联 | [OPS-FOOTBALL-RPC-MUST-HAVE §7](../delivery/OPS-FOOTBALL-RPC-MUST-HAVE.md) · [OPS-FOOTBALL-MERGE-DECISIONS D-ADR-050](../delivery/OPS-FOOTBALL-MERGE-DECISIONS.md) · [OPS-FOOTBALL-MERGE-WORK-PLAN §8.6](../delivery/OPS-FOOTBALL-MERGE-WORK-PLAN.md) · [ADR-056](./ADR-056-Football用户身份SSOT.md) |

---

## 1. 背景

[ADR-050 §3.1](./ADR-050-Ops与Football多库复用总纲.md#31-硬约束不改-football-业务代码与逻辑)（2026-07-05）规定：**禁止**修改 `football-backend-saas` 业务代码；OPS 只能用 `@DS` 只读跨库或调用**既有** API，不得要求 Football 新增/改接口。

合并目标态（[MUST-HAVE §1](../delivery/OPS-FOOTBALL-RPC-MUST-HAVE.md#1-原则结论)）要求 OPS **只访问 `wd`**，跨库一律 Feign/RPC。G-SYS / DICT / MEM / MP / PAY / DING 等 must-have 能力需 Football 侧 **新增或扩展 `/rpc-api/*` 契约**（见 MUST-HAVE §7），与 §3.1 字面冲突。

2026-07-28 产品/架构拍板 **选项 C — 有限 Supersede**（见 [D-ADR-050](../delivery/OPS-FOOTBALL-MERGE-DECISIONS.md#d-adr-050supersede-31允许-football-为-ops-增-feign)）。

---

## 2. 决策

| # | 决策 | 说明 |
|---|------|------|
| R1 | **有限 Supersede §3.1** | 允许 Football 在 **`football-backend-saas` `ops` 分支**为 OPS 合并 **扩展或新增** MUST-HAVE §7 白名单 G-* RPC |
| R2 | **白名单范围** | 仅下列 G-* 编号及其 §7 子节所列接口/DTO 扩展；**超出白名单的 Football 业务改动仍禁止** |
| R3 | **OPS Phase C Feign 切轨合法** | OPS 在 G-* 交付（或 Football `ops` 分支已有实码）后，可 vendored `*-api` + `@FeignClient` 双跑并 cutover；**不**构成违反 ADR-050 |
| R4 | **其余 §3.1 约束保留** | Gateway 集成基建仍冻结；Football 原生 Admin 页面/Store 逻辑不改；非白名单 member/mp/pay/system 业务重构仍禁止 |
| R5 | **非整包 GO** | 本 REV **仅解除 B-ADR-050 流程阻塞**；Phase C 整包 cutover 仍须各 G-* Integration 验收 + 删 `@DS`/multidb（见 WORK-PLAN §8.6） |

### 2.1 白名单（MUST-HAVE §7）

| G-* | 域 | 说明 |
|-----|-----|------|
| G-SYS-01 | system | 用户 simple-list Feign（§7.1） |
| G-SYS-02 | system | 用户/角色校验 RPC（§7.2）；含 `hasAnyRoles`、roleCode 列用户等 |
| G-DICT-01 | system | 字典 list-by-type / @InDict RPC（§7.3） |
| G-INF-01 | infra | FileApi 契约对齐（§7.4） |
| G-MEM-01 | member | 作者 DTO `authorLevel`（§7.5） |
| G-MEM-02 | member | 作者只读 Feign（§7.5；可选，D-G-MEM-02 已拍板可后置） |
| G-MEM-03 | member | 文章写 Feign（§7.6） |
| G-MP-01 | mp | 公众号 page/写 Feign（§7.7） |
| G-PAY-01 | pay | 订单运营列表（§7.8） |
| G-DING-01 | system/钉钉 | 通用工作通知（§7.9；D-G-DING 已拍板**延后**，不阻塞主路径） |

**不在白名单**：Football Admin UI 改造、非 OPS 合并需求的业务重构、通讯录 sync（D-DING-02 仍不做）、M10 采集等 Phase 2 范围。

### 2.2 交付与分支约定

- Football 合入轨：**Gitee `ops` 分支**（见 [FOOTBALL-OPS-BRANCH.md](../delivery/FOOTBALL-OPS-BRANCH.md)）
- 禁止将 G-* 改动 push/merge 到 `master`，除非另开架构决议
- OPS 每接一项 G-*：Feign 双跑 → Integration 手验 → 再删对应 `@DS`（单片 cutover，禁止整包抢跑）

---

## 3. 与 ADR-050 原文关系

| ADR-050 条款 | REV1 后状态 |
|--------------|-------------|
| §3.1「禁止改 Football 业务代码」 | **部分 Superseded** — 仅白名单 G-* RPC 扩展例外 |
| D10 | **部分 Superseded** — 与 §3.1 同范围 |
| D1–D9、§3.2 验收、P1–P3 数据原则 | **不变** |
| §8「不在本期 — Football 业务微服务代码修改」 | **修订** — 白名单 G-* 除外 |

---

## 4. 后果

- **解除**：WORK-PLAN **B-ADR-050** 流程阻塞；Football 扩 G-* API 有架构依据；`origin/ops` push 与 OPS Feign 联调合法
- **未解除**：Phase C **整包 NO-GO** 直至各 G-* OPS 集成 + Integration 验收 + 物理删 `@DS`/multidb
- **后续**：Football 按 MUST-HAVE §7 排期；OPS 按 WORK-PLAN C-WP2–C-WP7 单片切轨；每 cutover 更新 §8.6 阻塞表

---

## 5. 变更记录

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-07-28 | Agent | 初稿；D-ADR-050 选项 C 拍板；有限 Supersede §3.1 |
