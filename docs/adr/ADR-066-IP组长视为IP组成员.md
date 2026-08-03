# ADR-066：IP 组长视为所属 IP 组成员

| 字段 | 值 |
|------|---|
| 编号 | ADR-066 |
| 标题 | 担任组长即视为 IP 组成员（内容新建等成员校验） |
| 状态 | **Accepted**（产品确认 2026-08-02） |
| 日期 | 2026-08-02 |
| 关联 Spec | PRD-M1/M2（未显式写明「组长⊂成员」）；ADR-064（`ledIpGroupIds` / `leader_user_id`） |
| 关联工程 | `IpGroupAccessSupport.isMemberOfIpGroup` · `ProductionContentServiceImpl.assertUserMemberOfIpGroup` |

---

## 1. 背景

IP 组长落在 `oa_ip_group.leader_user_id`（或成员行 `is_leader=1`），**未必**有 `oa_ip_group_member` 行。内容新建（非任务驱动）调用 `isMemberOfIpGroup`，仅查成员表时，`opsleader` 选自己管辖的 IP 组会被拒：「当前用户不属于所选 IP 组」。

PRD-M1/M2 对此未写明；产品确认：**担任组长即视为该组成员**，不应被拒。

---

## 2. 决策

`IpGroupAccessSupport.isMemberOfIpGroup` 判定为 true 当：

1. 租户管理员（无限制）——既有；或
2. 当前用户（含 ID 桥接）在 `oa_ip_group_member` 中属于该组——既有；或
3. 该组 ∈ 当前用户 `resolveLedIpGroupIds`（`leader_user_id` 或 `is_leader=1`）——**新增**。

与账号绑定侧已有的 `resolveAccessibleIpGroupIds`（成员 ∪ 组长）语义对齐。

---

## 3. 非目标

- 不强制写入 `oa_ip_group_member` 行
- 不放宽非组长、非成员用户的内容新建权限
