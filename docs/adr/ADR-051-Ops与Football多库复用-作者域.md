# ADR-051：Ops × Football 多库复用 — 作者域

> **编号说明**：仓库内已有 [ADR-050-M4](./ADR-050-M4-采集Tab扫码登录.md)（M4 采集）。本 ADR 使用后缀 `-INT-AUTHOR` 标识作者域多库集成。总体方向见 [ADR-050](./ADR-050-Ops与Football多库复用总纲.md)。

| 字段 | 值 |
|------|---|
| 编号 | ADR-051-INT-AUTHOR |
| 标题 | 作者双表：`author_user`（Football SSOT）+ `oa_author_ext`（Ops 扩展） |
| 状态 | **Accepted**（2026-07-05，用户确认；**2026-07-05 修订**：测试数据可弃路径） |
| 日期 | 2026-07-05 |
| 决策人 | 架构 / 产品 |
| 关联 | [ADR-050](./ADR-050-Ops与Football多库复用总纲.md) · [ADR-047](./ADR-047-Football-Ops平台集成决策.md) · [ADR-049](./ADR-049-Ops与Football数据归属与松耦合集成.md) · [OPS-FOOTBALL-MULTI-DB-REUSE-ANALYSIS](../delivery/OPS-FOOTBALL-MULTI-DB-REUSE-ANALYSIS.md) §N.1 |

---

## 1. 背景

Football 生产分库下，作者 SSOT 为 `shenyu-member.author_user`（35 行，ID 68028–1000008）。Ops `wd.oa_author`（8 行 seed）与 Football **零 ID 重叠**。用户 2026-07-05 确认 wd 测试数据可 TRUNCATE，**无需** 8↔35 历史映射。

ADR-049 曾决策「`oa_author` 保持独立业务实体」；本 ADR **局部修订**：作者身份/财务/推送走 Football；Ops 运营维度走扩展表。

---

## 2. 决策

| # | 决策 | 说明 |
|---|------|------|
| D1 | **`author_user` = 作者 SSOT** | 昵称、头像、粉丝、分成、推送等以 `shenyu-member.author_user` 为准；Ops 经 `@DS("member")` 只读或 member-server 写入 |
| D2 | **`oa_author_ext` PK = `author_user_id`** | **非** `oa_author.id` 锚点；ext 为 Ops 运营扩展面（IP 组、类型、主推公号） |
| D3 | **停写 / 弃用 `oa_author`** | S0 TRUNCATE 后即停写；S4 DROP（若仍存在）；**禁止**新建 `oa_author` 行 |
| D4 | **`author_id` 语义 = `author_user.id`** | `oa_content` / `oa_task` / `oa_production_content` / `oa_order_attribution` 的 `author_id` 直接存 Football 作者 ID（V131 COMMENT） |
| D5 | **跨库无事务** | member 写 + wd 写由应用层 Saga；失败时 `sync_status='ERROR'` + `sync_error` |
| D6 | **Supersedes ADR-049 §已确认 #1 作者部分** | 用户选择器仍读 `system_users`（不变） |
| D7 | **全部改造在 Ops 侧** | Java/Flyway 变更仅限 **`oa-server` + `wd`**；**不改** member-server 等业务代码；读 `author_user` 用 `@DS("member")` 或 Feign 既有 API（ADR-050 §3.1） |

---

## 3. 字段归属

### 3.1 `shenyu-member.author_user`（Football SSOT）

| 字段类 | 代表列 | 读写 |
|--------|--------|------|
| 身份 | `nickname`, `avatar_url`, `mobile`, `sex`, `status` | member 写 / Ops 读 |
| 用户关联 | `user_id` → 管理端用户 | member 写 |
| 财务/分成 | `order_ratio`, `withdrawal_fee_ratio`, `min/max_withdrawal_amount` | member 写 / Ops 读 |
| 粉丝/战绩 | `fans`, `total_articles`, `hit_rate`, `recent_*` | member 写 / Ops 读 |
| 推送/私域 | `captive_push_account`, `push_mode`, `ban_push`, `private_status` | member 写 |
| 租户 | `tenant_id` | member |

### 3.2 `wd.oa_author_ext`（Ops 扩展）

| 列 | 类型 | 说明 |
|----|------|------|
| **`author_user_id`** | BIGINT **PK** | → `author_user.id`；**必填** |
| `tenant_id` | BIGINT NOT NULL | 租户隔离 |
| `ip_group_id` | BIGINT NOT NULL | → `oa_ip_group.id`（小 IP 组，group_type=2） |
| `author_type` | VARCHAR(32) | `dict_author_type` |
| `primary_mp_account_id` | BIGINT NULL | → `mp_account.id`（取代 legacy `primary_account_id` → `oa_account`） |
| `status` | TINYINT | Ops 侧启用/停用 |
| `remark` | VARCHAR(200) | Ops 备注 |
| `sync_status` | VARCHAR(32) | **`SYNCED` / `ERROR` only**（无 `PENDING_MAP`） |
| `sync_error` | VARCHAR(512) | 双写失败原因 |
| audit | creator/create_time/updater/update_time/deleted | 标准 Ops 审计 |

### 3.3 `wd.oa_author`（**弃用**）

| 状态 | 说明 |
|------|------|
| S0 | TRUNCATE + 停写 |
| S1+ | 代码不再 INSERT/UPDATE |
| S4 | DROP TABLE（若仍存） |

---

## 4. 新建 / 编辑规则

### 4.1 新建作者（无 `oa_author`）

```
1. @DS("member") INSERT author_user（nickname, user_id, status, tenant_id, …）
   → 得到 author_user_id
2. @DS("master") INSERT oa_author_ext（author_user_id, ip_group_id, …）
   sync_status = 'SYNCED'
3. oa_content / oa_task 等 author_id 直接存 author_user_id
4. 任一步失败：已写步骤标记 sync_status='ERROR' + sync_error；无跨库回滚
```

### 4.2 编辑作者

| 变更字段 | 写入目标 |
|----------|----------|
| `nickname`, 粉丝/分成/推送等 Football 域 | `@DS("member")` UPDATE `author_user` |
| `ip_group_id`, `author_type`, `primary_mp_account_id`, `remark`, `status` | `@DS("master")` UPDATE `oa_author_ext` |

### 4.3 删除

- 逻辑删除 `oa_author_ext`；**不**物理删除 `author_user`（Football 域可能有订单/公号绑定）
- 可选：member 侧 `author_user.deleted=1`（经 member-server）

### 4.4 列表 / 详情 API（S1 目标）

```
@DS("member") SELECT author_user …
  LEFT JOIN（内存）@DS("master") oa_author_ext ON ext.author_user_id = author_user.id
  补 ip_group_name / primary_mp_account_name
```

---

## 5. ID 策略（修订后）

| 方案 | 结论 |
|------|------|
| A. ext PK = `author_user_id` | ✅ **采纳** — 零映射；FK 语义统一 |
| B. ext.id = oa_author.id 锚点 | ❌ **Superseded**（V130 过渡方案，V131 修订） |
| C. 强制 oa_author.id = author_user.id | ❌ 与历史 seed 冲突且已弃 |

**无 PENDING_MAP**：测试数据已 TRUNCATE，不存在待映射历史行。

---

## 6. 后果

- Flyway：`V130` 过渡建表 → **`V131`** 改 PK + 建 `oa_account_ext`
- Java：`OaAuthorExtDO` / `AuthorServiceImpl` 待 S1 改造（PK 字段变更）— **均在 oa-server + wd Flyway，不改 member-server**
- `oa_order_attribution.author_id` = `author_user_id`（与 Football `pay_all_order.author_id` 同空间）

---

## 7. 不在本期

- DROP `oa_author`（S4）
- member-server Feign 封装（PoC 可用 oa-server 直连 `@DS("member")`）— **仍不改 member-server 业务代码**
- 批量修复已清空测试内容的 author_id

---

## 8. 变更记录

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-07-05 | Agent | 初稿；Option A（author_user + oa_author_ext） |
| 2026-07-05 | Agent | **修订**：PK=`author_user_id`；DROP oa_author 路径；删除 PENDING_MAP / 8 行映射 |
| 2026-07-05 | Agent | D7：全部改造限定 oa-server + wd；不改 Football 业务代码（ADR-050 §3.1） |
