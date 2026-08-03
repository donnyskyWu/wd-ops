# G-PAY-01 跟进（2026-07-30 · 假设 B 落地）

| 字段 | 值 |
|------|---|
| 关联 | [REPORT.md](./REPORT.md) · FEIGN-CHECKLIST §3 G-PAY-01 · [ADR-057](../../../adr/ADR-057-G-PAY-01-page-for-ops.md) |
| 环境 | pay :48085 UP · oa :48094 UP · Gateway :48080（无 -Beta） |
| 结论 | **Pass** — `page-for-ops` RPC + Gateway `football-order/list` 均 code=0（有数据） |

---

## 1. 拍板

用户明确 **按假设 B**：废止 D-G-PAY-01 选项 A（复用 Admin `getOrderPage`），按 MUST-HAVE §7.8 实现 `POST /rpc-api/pay/order/page-for-ops`。

决策落档：[ADR-057](../../../adr/ADR-057-G-PAY-01-page-for-ops.md) · [MERGE-DECISIONS D-G-PAY-01 REV1](../../OPS-FOOTBALL-MERGE-DECISIONS.md)

---

## 2. 实现摘要

| 层 | 变更 |
|----|------|
| Football | `PayOrderApi.pageForOps` + `AllOrderService.getOrderPageForOps`：租户 + `[startTime,endTime)` + 可选 authorId/status；**投影 OPS 10 列**；无 permitted-ids / finance_channel 富化 |
| 时间入参 | `OrderOpsLocalDateTimeDeserializer`：兼容 ISO / `yyyy-MM-dd HH:mm:ss` / epoch 毫秒（覆盖 Football 全局 Timestamp 反序列化） |
| OPS | `PayOrderApi.pageForOps`；`AllOrderRespDTO.payTime/createTime` 用 **Long epoch millis**（与 MpUserDTO 同模式） |

---

## 3. 复验快照（假设 B 后）

| 步骤 | 结果 | 证据文件 |
|------|------|----------|
| RPC `page-for-ops` + `tenant-id`（无 Authorization） | code=**0** total≈**183485** | `G-PAY-01-rpc-page-for-ops-tenant-only.json` |
| RPC `page-for-ops` + Bearer | code=**0** total≈183485 | `G-PAY-01-rpc-page-for-ops-with-auth.json` |
| Gateway `GET .../football-order/list?startDate=2020-01-01&endDate=today` | code=**0** total≈**183485** | `G-PAY-01-ops-football-order-list-page-for-ops.json` |

旧 Admin path（对照，非目标）：

| 步骤 | 结果 |
|------|------|
| `POST .../order/page` + Bearer | 仍可能 500（富化 schema）— **不再作为 G-PAY-01 验收路径** |

---

## 4. 历史阻塞（已关闭）

| 原问题 | 处置 |
|--------|------|
| permitted-ids / Authorization | page-for-ops **不**绑定；OPS 仍可透传 Token |
| finance_channel* 富化 schema | 跳过富化 + SELECT 仅 10 列 |
| LocalDateTime 字符串→1970 | 专用 Deserializer |
| Feign 响应 epoch millis 解码失败 | OPS DTO 用 Long |
