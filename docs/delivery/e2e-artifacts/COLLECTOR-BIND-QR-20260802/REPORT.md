# COLLECTOR-BIND-QR-20260802

**Status**: IMPLEMENTED · 假设 A（ADR-060 §5.1）  
**Date**: 2026-08-02

## Collector

| 项 | 值 |
|----|-----|
| 启动 | `.\scripts\start-collector.ps1 -Restart` |
| 监听 | `http://127.0.0.1:8000`（livez / docs） |
| Token | `test-key-2026`（与 `oa.unified-collector.api-token` 对齐） |
| 状态 | **UP**；直连 QR `wechat_mp` 已出码 |

## Ops restore

从 git `2a64362^` 恢复并改包为 `football.module.ops.**`：

- `CollectorAccountBindController` / `CollectorBatchBindController`
- `UnifiedCollectorAdapter`（bind-only，无 SyncService 落库）
- `UnifiedCollectorApiClient` + `CollectorQrLoginService`
- `CollectorAccountBindService*` / `CollectorCredentialBuilder` 等
- 权限：`ops:account:list` / `ops:platform-account:list`
- `DeferredCutoverStubController` 卸掉 `collector-bind` 映射
- ADR-060 §5.1 carve-out 已写

ops-server：`.\scripts\start-integration-oa.ps1 -Rebuild -Profiles "dev,dev-test-beta"` → **UP**

## Smoke（Gateway `:48080` · 账号 `1000109` WECHAT_OFFICIAL）

| 检查 | 结果 |
|------|------|
| GET `…/collector-bind` | ✅ code=0 · `data=null`（未绑定；非 stub 空页 / 非 410） |
| POST `…/qr-login/start` | ✅ code=0 · `sessionId` + `qrcodeBase64` · status=`pending` · expires=180 |
| DouyinFollowers POST（对照） | ✅ 仍 410 stub |
| collector livez | ✅ |

人工扫码 confirm / poll 落库未在本 smoke 跑通（start 已证明真实链路）。

## Residual

- SyncService 全量落库、DouyinFollowers、collect log/quality、EXTERNAL config → 仍 ADR-060 OOS
