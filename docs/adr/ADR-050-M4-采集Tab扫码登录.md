# ADR-050: M4 采集 Tab 扫码登录 UX 扩展

> **状态**: 已采纳 | **日期**: 2026-06-24
> **决策人**: 产品 + 开发团队
> **关联**: [ADR-047](./ADR-047-M4-平台账号凭证SSOT与Collector映射.md) · [API-M10](../engineering/API-M10-数据采集.md)

## 背景

ADR-047 已约定 Channel-A 平台可通过 Collector 扫码获取凭证，但 M4「采集」Tab 初版仅支持手动粘贴 Cookie/Token，运营成本高。

`unify-collector-api` 已提供 **统一扫码 API**（`POST /api/v1/auth/qrcode` + `GET /api/v1/auth/poll`），支持 `wechat_mp`、`wechat_channels`、`douyin`、`kuaishou`、`xiaohongshu`（见仓库根 `api.json` · `QrCodeRequest`）。

## 决策

1. **OA 代理 Collector QR**：前端不持有 `API_TOKEN`；由 OA 后端代理统一 QR 三接口。
2. **凭证 SSOT 不变**：扫码 `confirmed` 后，OA 将 poll 返回的 `credential`（cookie / token / auth_token）**AES 写入 `oa_account`**，再写 `oa_collector_account_bind` 或触发 `bind` import。
3. **支持平台（Channel-A MVP）**：`WECHAT_OFFICIAL`、`WECHAT_VIDEO`、`DOUYIN`、`KUAISHOU`、`XIAOHONGSHU`。
4. **Cookie 粘贴保留**：扫码与手动粘贴并存；抖音/快手服务端 QR 可能受 CDN 风控，UI 提示可用 `tools/local_qr_login.py` 或 Cookie 粘贴兜底。
5. **Bilibili**：Collector 支持统一 QR，但不在本期 M4 Tab 按钮范围（仍 Cookie + bind）。

## OA API（新增）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/admin-api/oa/account/{id}/collector-bind/qr-login/start` | 启动扫码 |
| GET | `/admin-api/oa/account/{id}/collector-bind/qr-login/poll?sessionId=` | 轮询状态 |
| DELETE | `/admin-api/oa/account/{id}/collector-bind/qr-login/cancel?sessionId=` | 取消会话 |

权限：`oa:account:list`；租户隔离与账号归属校验沿用 M4。

## 验收（AC 摘要）

- **AC-QR-01**：五平台账号详情「采集」Tab 展示「扫码登录」按钮（Bilibili 除外）。
- **AC-QR-02**：start 返回 base64 QR；poll `confirmed` 后 `hasCookie` 为真且 bind 为 BOUND（或提示手动 bind）。
- **AC-QR-03**：前端不暴露 collector token / 明文 cookie。

## 非目标

- 不在 OA 内实现 Playwright 扫码（SSOT 仍为 collector）。
- 不替代企微 / 奥创通道登录（ADR-045）。
