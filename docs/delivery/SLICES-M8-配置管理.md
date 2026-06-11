# SLICES-M8-配置管理

> **版本**：v2.1 | 2026-06-11
> **PRD**：[`PRD-M8-配置管理.md`](../product/PRD-M8-配置管理.md)

| Slice | 目标 | FR | CFG |
|-------|------|----|-----|
| S-M8-01 | 内部采集 + 奥创 | FR-M8-001 | CFG-001~009 |
| S-M8-02 | 外部采集 + 关键词 + 导入 | FR-M8-002 | CFG-010~013 |
| S-M8-03 | 外部数据 + 订单采集 | FR-M8-003,004 | CFG-014~019 |
| S-M8-04 | 阈值四 Tab | FR-M8-005 | CFG-020~024 |
| S-M8-05 | AI 模型 + 提示词 | FR-M8-006,007 | CFG-025~036 |

## 全局规范

- `platformType` → `<DictSelect dict-type="dict_platform_type" />`（内部平台 Tab）
- 外部账号 `platformType` → `dict_third_platform`
- 内部平台 Tab `accountId` → `<AccountSelect />`（关联 M4 `oa_account`）
- `overrideAccountId` → `<AccountSelect />`
- `status` → `dict_config_status`
- 凭证 AES + 脱敏

## AC 映射

| Slice | 验收 |
|-------|------|
| S-M8-01 | 7 平台 Tab；平台类 AccountSelect+CRUD；企微=WeworkAppConfigPanel；个微=仅奥创；V50/V51 种子 |
| S-M8-02 | 双 Tab；CSV 导入 |
| S-M8-03 | 订单 DB 连接测试 |
| S-M8-04 | 4 类阈值 CRUD |
| S-M8-05 | 默认模型；提示词版本 |
