# ADR-049：Ops 与 Football 数据归属与松耦合集成

> **编号说明**：仓库内已有 [ADR-049-M10](./ADR-049-M10-全量采集与展示桥接.md)（M10 采集桥接）。本 ADR 使用后缀区分主题，文件名为 `ADR-049-Ops与Football数据归属与松耦合集成.md`。

| 字段 | 值 |
|------|---|
| 编号 | ADR-049-INT-DATA |
| 标题 | Ops 与 Football 数据归属与松耦合集成 |
| 状态 | **Accepted**（§已确认决策 2026-07-04） |
| 日期 | 2026-07-04 |
| 决策人 | 架构 / 产品 |
| 关联 | [ADR-047](./ADR-047-Football-Ops平台集成决策.md) · [OPS-FOOTBALL-DATA-OWNERSHIP-ANALYSIS](../delivery/OPS-FOOTBALL-DATA-OWNERSHIP-ANALYSIS.md) |

---

## 背景

Football × Ops 集成 S4 前端挂载（58/58）已满足联调；S3 后端 sibling 工程迁移需先明确单库 `wd` 内表归属与跨项目业务边界，避免错误合并或双写。

详细证据与分析见 [OPS-FOOTBALL-DATA-OWNERSHIP-ANALYSIS](../delivery/OPS-FOOTBALL-DATA-OWNERSHIP-ANALYSIS.md)。

---

## 决策

| # | 决策 | 说明 |
|---|------|------|
| D1 | **S3 延期** | `football-module-oa` sibling 工程迁移 **⏸ Deferred**；待表归属与用户 ID 迁移范围确认后再启动 |
| D2 | **`sys_dict_*` = Ops SSOT** | 业务字典（`dict_*` 类型）由 Ops 维护；**不**合并至 Football `system_dict_*`；类型命名空间并存 |
| D3 | **`sys_param` = Ops-only** | Ops 业务调参；Football/infra 运行时键保留 `infra_config` |
| D4 | **M9 `sys_user` 等废弃** | 身份以 Football `system_users` / `system_menu` 为准；`sys_user`、`sys_role`、`sys_tenant`、`sys_permission` 停止写入，只读过渡 |
| D5 | **订单只读跨查** | Ops **不写入** Football 订单；同库 `wd` 内经 oa-server **只读 Mapper** 读取 Football **`pay_all_order`**（本部署无 `trade_order`/`pay_order` DDL）；`GET /admin-api/oa/football-order/list`；**禁止 ETL** 至 `oa_order`、禁止双写 |
| D6 | **独立 UI = 开发入口** | `ops-platform-ui-vue`（:3000）+ oa-server dev（:8080）保留为 dev/QA harness；生产路径仍为 Football 5777 + Gateway 48080（S5 再评估下线） |
| D7 | **页面模块归属** | Ops-only `sys_*` 维护页 **保留在 Ops 壳**：`ops/system/*`（字典/参数/日志/消息，菜单 6137–6141）及 `ops/config/MetadataManage`（6165）；M9 身份页（user/role/tenant）**仅 Football 原生菜单**，不迁入 Ops |

---

## 已确认决策（2026-07-04）

| # | 主题 | 决策 |
|---|------|------|
| 1 | **用户** | 选择器（`UserSelect` 等）统一读 Football **`system_users`**（技术债：改 API，非 `oa_author`）；**`oa_author` 保持独立业务实体**，作者与用户引用字段迁移 **不在本期** |
| 2 | **订单** | Ops **同库只读** **`pay_all_order`**（+ 按需 `pay_gold_order`）；**不写订单、不做 ETL**；P2b：`FootballPayAllOrderReadMapper` + `/admin-api/oa/football-order/list`（2026-07-04 ✅） |
| 3 | **粉丝** | **不合并** — `oa_*` 运营粉丝域 vs Football `member_*` C 端域，用途与 schema 不同 |
| 4 | **平台账号 / 计划 / M4 资产** | **保持分离**；试用后再优化，本期不合并 |
| 5 | **Ops-only `sys_*` 页面** | **是** — 对应维护页留在 Ops（见 D7）；Football 仅保留 M9 身份与平台字典 |

---

## 后果

- S3 启动条件：`sys_dict_*` 归属已签字；P2a ✅；P2b ✅（`pay_all_order` 只读 Mapper + 集成 curl 绿）。
- 单库共存：`sys_*` 与 `system_*` 并行，靠类型命名空间与文档约束隔离。
- `oa_author` 与 `author_channel_sales` 映射、粉丝合并、用户 FK 批量迁移 — **延期**（见 INTEGRATION-PROGRESS §17）。

---

## 待决事项

1. `sys_dict_*` 是否重命名为 `oa_dict_*`（S3 可选，非必须）。
2. `oa_author.user_id`、`oa_ip_group_member.user_id` 等字段迁移至 `system_users.id` 的时间表（**非本期**）。
3. ~~订单只读 Mapper spike~~ → **P2b 已落地**；待产品：`oa_order_attribution.order_id` FK 语义切换、`author_id` 映射表。

> **本地调试场景**（2026-07-04）：**Ops standalone only** 使用 `start-ops-standalone.ps1`（:3000 + :8080 · Dev Token），**不涉及** member mock / Gateway / system-server；member 真服替换仅 **Football 全栈集成 / S5** 待办（见 [INTEGRATION-PROGRESS §20–§21](../delivery/INTEGRATION-PROGRESS.md)）。
