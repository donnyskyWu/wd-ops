# B-WP4-ARCHIVE 表归档签收表

> **硬规则**：未获本表书面签收 → **禁止**对候选对象执行 `DROP` / `RENAME`（见 [WORK-PLAN B-WP4-ARCHIVE](../OPS-FOOTBALL-MERGE-WORK-PLAN.md#阻塞问题清单b-wp4-archive--c-wp7-phys-2026-07-30)）。  
> **本表状态（2026-07-31）**：**已签收**；归档执行见 `docs/delivery/e2e-artifacts/B-WP4-ARCHIVE-20260731/`。

| 项 | 填写 |
|----|------|
| **标题** | B-WP4-ARCHIVE 表归档范围与执行条件签收 |
| **日期** | 2026年07月31日 |
| **签收人（产品）** | 用户/产品 |
| **签收人（DBA）** | 用户/产品（兼） |
| **签收人（OPS 工程）** | 用户/产品（兼） |
| **签收人（运维）** | 用户/产品（兼） |
| **结论** | ☑ 签收　☐ 拒签　☐ 有条件签收（条件见文末「附加条件」） |

**SSOT 依据**

- [OPS-FOOTBALL-MERGE-WORK-PLAN.md](../OPS-FOOTBALL-MERGE-WORK-PLAN.md) § B-WP4 / 阻塞问题清单 B-WP4-ARCHIVE **Q1–Q5**
- [OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md](../OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md) **§3.1** / **§3.4**（候选，**非**已批准删除清单）

---

## Q1 — 归档范围（产品 + DBA）

对每一行勾选**唯一**处置；「保留期限」仅在选择停写只读 / RENAME archive 时填写。  
*斜体为建议默认，非正式决定；决定列请自行勾选。*

> **签收决定（Q1）**：**采纳建议**（按下表「建议」勾选处置）。

| # | 对象（CLEANUP / WORK-PLAN 候选） | 建议（非正式） | 处置（勾选一项） | 保留期限 | 备注 |
|---|----------------------------------|---------------|------------------|----------|------|
| 1 | `wd.sys_user` | *建议：停写只读；harness 退役后再 RENAME/DROP* | ☑ 停写只读　☐ RENAME archive　☐ DROP　☐ 暂不纳入 | harness 退役后再物理处置 | 采纳建议 |
| 2 | `wd.sys_user_token` | *建议：停写只读；与 IT seed（Q2）联动后再物理处置* | ☑ 停写只读　☐ RENAME archive　☐ DROP　☐ 暂不纳入 | 与 Q2 联动后再物理处置 | 采纳建议；本 Slice **不**删 seed |
| 3 | legacy 角色相关表（§3.1 组：与 `sys_user*` 同批；含历史上的 `sys_role` / `sys_user_role` / `sys_role_permission` / `sys_permission` 等 **wd 侧废弃身份表**） | *建议：停写只读；与 `sys_user*` 同窗* | ☑ 停写只读　☐ RENAME archive　☐ DROP　☐ 暂不纳入 | 与 `sys_user*` 同窗 | 采纳建议 |
| 4 | `wd.sys_operation_log` | *建议：确认无读后 RENAME archive；勿盲目 DROP* | ☐ 停写只读　☑ RENAME archive　☐ DROP　☐ 暂不纳入 | 备份可 restore；无强制回滚窗口（Q3） | 采纳建议；代码无读依赖（C-WP0 no-op） |
| 5 | `wd.sys_dict_*`（已 merge → `shenyu-system.system_dict_*`，V152/V158） | *建议：停写只读过渡；确认无读后再 RENAME* | ☑ 停写只读　☐ RENAME archive　☐ DROP　☐ 暂不纳入 | 确认无读后再 RENAME | 采纳建议；读仍可能 fallback |
| 6 | §3.4 桥接列/桥（组）：业务表中仅服务「wd userId ↔ football userId」normalize 的过渡列/缓存；`sync_status` 等跨库 Saga 字段 | *建议：先停写/停桥接逻辑；列 **暂不纳入** DROP；`sync_status` 可能保留* | ☐ 停写只读　☐ RENAME archive　☐ DROP　☑ 暂不纳入 | — | 采纳建议 |
| 7 | `wd.system_users` overlay（§3.1：若仍作 overlay 写入；SSOT = `shenyu-system.system_users`，ADR-056） | *开放项 — 须显式选择；建议：可立即停写；物理删与 Q4 Mapper 解耦* | ☑ 停写只读　☐ RENAME archive　☐ DROP　☐ 暂不纳入 | ADR-056 全量切轨后再物理处置 | 采纳建议；与 Q4 Mapper 解耦 |

**Q1 附加说明（可选）**

```
环境范围：仅 localhost:3306/wd（integration 本地库）。禁止对本 Slice 执行远程/生产 DROP。
执行产物：docs/delivery/e2e-artifacts/B-WP4-ARCHIVE-20260731/
#4 RENAME → archive_wd.sys_operation_log（非 DROP）
#1–3/#5/#7：写阻断 trigger 落地停写只读；表仍可读
#6：本 Slice 不改列/不 DROP
```

---

## Q2 — H2 IT / SeedVerificationIT 与 `sys_user_token` seed（OPS 工程）

> WORK-PLAN：当前 IT 仍依赖 `dev-token-*` + Flyway seed（`oa.auth.dev-token.enabled=true` 仅 test）。

| 项 | 建议 | 决定 |
|----|------|------|
| 是否 **改写 IT / SeedVerificationIT 后再删** `sys_user_token` seed？ | *建议：**是** — 先改写再删，避免破 IT* | ☑ Yes　☐ No |

**备注**

```
采纳建议：Yes — 若未来物理删 seed，须先改写 IT。
本 Slice Q1 #2 处置为「停写只读」→ 不删 sys_user_token seed / 不改 IT。
物理删 seed 延后至 harness 退役 + IT 改写完成之后。
```

---

## Q3 — 备份介质与回滚窗口（运维）

> WORK-PLAN 建议回滚窗口 **≥7 天**；介质未在 Spec 中指定，由运维填写。

| 项 | 建议 | 填写 |
|----|------|------|
| 备份介质 | *库级 dump 和/或 表级 rename+archive schema（二选一或组合）* | 表级 mysqldump（候选表）+ archive schema RENAME（#4） |
| 存放位置 | *（由运维规范指定）* | `docs/delivery/e2e-artifacts/B-WP4-ARCHIVE-20260731/backup/` |
| 回滚窗口（天） | *≥7* | **0 天**（用户决定：可备份 SQL 后执行；**不要回滚窗口**） |
| restore 责任人 | *（运维具名）* | 用户/产品（本 Slice 本地 integration） |

**备份完成确认（执行前勾选）**

- [x] 备份已完成并可 restore 演练（或书面确认可 restore）
- [x] 备份校验记录路径 / ticket：`docs/delivery/e2e-artifacts/B-WP4-ARCHIVE-20260731/backup/` + `REPORT.md`

---

## Q4 — `FootballOAuth2MasterTokenMapper`（`@DS("master")` overlay）是否纳入本 Slice

> WORK-PLAN Q4 / CLEANUP §1.2：ADR-056 全量切轨后删 — **非本 Slice**。  
> *建议：否（不随 B-WP4 一并下线）。*

| 项 | 建议 | 决定 |
|----|------|------|
| 是否随 B-WP4 一并下线 `FootballOAuth2MasterTokenMapper`？ | *否* | ☐ Yes　☑ No |

**备注**

```
用户决定：测试没问题则可在 ADR-056 全量切轨后删 — 本 B-WP4 Slice 不删 Mapper。
```

---

## Q5 — 执行窗口（排期）

> WORK-PLAN：代码 PHYS 已完；**表物理删另会话**，禁止与业务发布同窗。

| 项 | 填写 |
|----|------|
| 计划执行日期 | 2026年07月31日 |
| 计划时间段 | 立即执行（签收后同会话） |
| 是否与业务发布同窗 | ☑ 否（符合要求）　☐ 是（**拒签条件** — 须改期） |
| 执行环境 | ☐ 生产　☐ 预发/UAT　☑ 其他：localhost integration `wd` |

---

## 签收签署

| 角色 | 姓名 | 签字/确认 | 日期 |
|------|------|-----------|------|
| 产品 | 用户/产品 | 已确认（Q1–Q5 采纳/执行） | 2026-07-31 |
| DBA | 用户/产品（兼） | 已确认 | 2026-07-31 |
| OPS 工程 | 用户/产品（兼） | 已确认 | 2026-07-31 |
| 运维 | 用户/产品（兼） | 已确认 | 2026-07-31 |

**结论复述**（与文首一致）：☑ 签收　☐ 拒签　☐ 有条件签收

**附加条件**（有条件签收时必填）

```
（无）
```

**拒签原因**（拒签时必填）

```
（无）
```

---

## 签收后下一步（Checklist · 仍禁止未签收 DROP/RENAME）

> 顺序对齐 WORK-PLAN B-WP4 任务清单：备份 → 归档 → 只读探测；完成后复评 Phase C 整包 GO。

- [x] **备份**已按 Q3 完成并确认可 restore
- [x] 另开 **归档 SQL Slice**（按本表 Q1 勾选范围编写 `RENAME`/`DROP` 与回滚说明；**本签收表不含 SQL**）→ 产物目录 `e2e-artifacts/B-WP4-ARCHIVE-20260731/`
- [x] 归档执行后做 **应用只读探测**（确认无生产读依赖）
- [ ] 更新 `docs/sql` / schema 导出（若团队在用）— 本 Slice 未强制；见 REPORT
- [x] 复评 **Phase C 整包 GO**（MASTER / FEIGN-CHECKLIST / WORK-PLAN 已更新为 **GO** · localhost；远程另窗）

---

## 链接

| 文档 | 用途 |
|------|------|
| [WORK-PLAN · B-WP4](../OPS-FOOTBALL-MERGE-WORK-PLAN.md#b-wp4rpc-cutover-后的归档删除排在-phase-c-验收后) | 目标 / 前置 / 验收 / Q1–Q5 |
| [CLEANUP-INVENTORY §3.1](../OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md) | 停止新写 / 逐步废弃候选表 |
| [CLEANUP-INVENTORY §3.4](../OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md) | RPC cutover 后可废弃列/桥（示例） |
| [FEIGN-CHECKLIST](../OPS-FOOTBALL-INTEGRATION-FEIGN-CHECKLIST.md) | Phase C 整包门禁（B-WP4 未完 → NO-GO） |
| [执行产物 B-WP4-ARCHIVE-20260731](../e2e-artifacts/B-WP4-ARCHIVE-20260731/REPORT.md) | 备份 / SQL / 探测结果 |
