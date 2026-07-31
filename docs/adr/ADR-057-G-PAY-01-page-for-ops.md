# ADR-057：G-PAY-01 订单运营列表改用 `page-for-ops`

| 字段 | 值 |
|------|-----|
| 编号 | ADR-057 |
| 标题 | G-PAY-01 OPS 订单列表走 MUST-HAVE §7.8 `page-for-ops` |
| 状态 | **Accepted**（2026-07-30，产品/架构拍板 **假设 B**） |
| 日期 | 2026-07-30 |
| 决策人 | 产品 / 架构 |
| Supersedes | [D-G-PAY-01 选项 A](../delivery/OPS-FOOTBALL-MERGE-DECISIONS.md#d-g-pay-01订单列表ops-何时切-getorderpage-feign)（2026-07-28：复用 Admin `getOrderPage`） |
| 关联 | [MUST-HAVE §7.8](../delivery/OPS-FOOTBALL-RPC-MUST-HAVE.md) · [ADR-050-REV1](./ADR-050-REV1-Football-G-RPC-Supersede.md) 白名单 G-PAY-01 · [WORK-PLAN §8.8](../delivery/OPS-FOOTBALL-MERGE-WORK-PLAN.md) · [G-PAY-01-FIX](../delivery/e2e-artifacts/G-STAR-HANDVERIFY-20260730/G-PAY-01-FIX.md) |

---

## 1. 背景

D-G-PAY-01（2026-07-28）拍板 **选项 A**：OPS Feign 复用 Football Admin 同源 `PayOrderApi.getOrderPage`（`POST /rpc-api/pay/order/page`）。

Integration 手验（2026-07-30）证实该路径绑定：

1. `authorApi.getPermittedIds()`（需登录用户上下文）
2. `permissionApi.getUserDataList`（system schema）
3. Admin **富化**：`finance_channel` / `finance_channel_sub` / 会员 / 作者 / 活码等

Local Integration DB schema 与 Admin 富化列不完全一致时，裸 RPC 持续 500（schema whack-a-mole）；OPS 列表在空结果早退时「偶然绿」，有数据则爆炸。与 MUST-HAVE §7.8 提案（轻量 `page-for-ops`、仅 tenant-id + 时间窗）冲突。

阻塞清单见 [G-PAY-01-FIX §4](../delivery/e2e-artifacts/G-STAR-HANDVERIFY-20260730/G-PAY-01-FIX.md)；用户拍板 **假设 B**。

---

## 2. 决策

| # | 决策 | 说明 |
|---|------|------|
| D1 | **采用 MUST-HAVE §7.8** | Football 新增 `POST /rpc-api/pay/order/page-for-ops`（`PayOrderApi.pageForOps`） |
| D2 | **入参** | `startTime` / `endTime`（必填；`createTime` 半开区间 `[start, end)`）+ 可选 `authorId` / `status` + `pageNo` / `pageSize`；Header `tenant-id`（铁律 1504）。时间 JSON 兼容 ISO / `yyyy-MM-dd HH:mm:ss` / epoch 毫秒（覆盖 Football 全局 Timestamp 反序列化） |
| D3 | **出参** | OPS 10 列：`id, orderNo, userId, authorId, amount, payAmount, status, orderType, payTime, createTime`（可复用 `AllOrderRespDTO` 子集） |
| D4 | **禁止绑定 Admin 语义** | **不**调用 `getPermittedIds` / `getUserDataList`；**不**加载 `finance_channel*`、会员昵称、作者昵称、活码等富化 |
| D5 | **OPS 切轨** | `FootballOrderReadService` Feign 从 `getOrderPage` 改为 `pageForOps`；Authorization 可继续透传，但 RPC **不以** Admin 作者权限为前置条件 |
| D6 | **Supersede D-G-PAY-01-A** | 2026-07-28「复用 getOrderPage」决议废止；字段对表签字（§8.8）仍有效，契约 path 改为 `page-for-ops` |

---

## 3. 后果

- Football `ops` 分支白名单 G-PAY-01 合法扩 API（ADR-050-REV1）
- Integration 不再依赖 Admin 渠道表列对齐即可绿 G-PAY-01
- Admin `/order/page` 行为不变；OPS 与 Admin 列表权限模型分离（OPS 侧仍走自身菜单/数据权限）

---

## 4. 变更记录

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-07-30 | Agent | 初稿；用户明确「按假设 B」 |
