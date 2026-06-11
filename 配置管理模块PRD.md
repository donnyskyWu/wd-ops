# 配置管理模块 PRD 产品需求文档

> **⚠️ 本文档已合并至 SSOT，请勿在此单独维护。**
>
> **Canonical**：[`docs/product/PRD-M8-配置管理.md`](docs/product/PRD-M8-配置管理.md) v2.2
>
> **关联**：[`ADR-014`](docs/adr/ADR-014-M8-配置管理数据模型.md) · [`API-M8`](docs/engineering/API-M8-配置管理.md) · [`UX-M8`](docs/product/UX-M8-配置管理.md)
>
> **合并日期**：2026-06-11

---

原 v1.0 全文（CFG-001~036、架构图、验收标准、待确认问题）已并入 `docs/product/PRD-M8-配置管理.md`，并叠加以下实现对齐：

| 变更 | 说明 |
|------|------|
| 账号统一 | 平台 Tab 使用 `AccountSelect` → `oa_account.account_id` |
| 企微 Tab | `WeworkAppConfigPanel` + `/oa/internal/wework/*` |
| 个微 Tab | 仅奥创接口，无采集列表 |
| 外部账号 | `subType=account`，平台 `dict_third_platform` |
| 数据模型 | `config_*` → `oa_*`（ADR-014） |
| API 路径 | `/api/config/*` → `/admin-api/oa/config/*` |

如需编辑需求，请直接修改 **`docs/product/PRD-M8-配置管理.md`**。
