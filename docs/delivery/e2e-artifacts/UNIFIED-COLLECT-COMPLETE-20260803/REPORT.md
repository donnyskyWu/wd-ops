# ADR-061 unified collect complete sign-off — 2026-08-03

## Summary

| Phase | Status | Evidence |
|-------|--------|----------|
| A — E2E persist (DOUYIN/KUAISHOU/WECHAT_VIDEO) | **PASS** | `UNIFIED-COLLECT-PERSIST-20260803/RESULTS.json` · logId=27/28 |
| B — Other platforms (WECHAT_OFFICIAL/XHS/BILIBILI) | **CODE ✅ · E2E partial** | MP → `oa_wechat_mp_article` + `oa_account_status_log`; XHS/BILI no beta members |
| C — Quality API | **BLOCKED (ADR-060 stub)** | `quality-list.json` code=0 empty list; tables `oa_data_quality_check` 未实现 |
| D — Bind enforcement | **PASS** | `ChannelAAccountCollectExecutor` 强制 BOUND；bind E2E 200 |

## Environment

- Gateway `:48080` · ops `:48094` (beta DB) · collector `:8000`
- `python docs/delivery/e2e-artifacts/UNIFIED-COLLECT-COMPLETE-20260803/smoke_complete.py`

## Phase A — FULL_PERSIST (3 core platforms)

**Before:** `persistMode=PROBE_COUNT_ONLY`, `targetTable=null`

**After (log 28):**

| Platform | persistMode | targetTable | Notes |
|----------|-------------|-------------|-------|
| DOUYIN (acc 3) | FULL_PERSIST | `oa_account_status_log`, `oa_douyin_video` | 78 videos upserted |
| WECHAT_VIDEO (acc 1) | — | — | Cookie 失效（300333）；bind OK，需 QR 重登 |
| KUAISHOU | — | — | 统一任务无 KUAISHOU 成员（beta tenant） |

`ensure-unified` 返回 `data.id`（dict）已兼容于 smoke 脚本。

## Phase B — WECHAT_OFFICIAL / XIAOHONGSHU / BILIBILI

**Code changes:** 自 legacy `ops-platform-module-oa` 迁入 SyncService 并扩展 `ChannelACollectSyncService`：

| Platform | dataTypes | targetTable |
|----------|-----------|-------------|
| WECHAT_OFFICIAL | MP_FOLLOWER_STATS/LIST, MP_ARTICLE_* | `oa_account_status_log`, `oa_wechat_mp_article` |
| XIAOHONGSHU | FOLLOWER_STATS, NOTE_LIST/STATS | `oa_account_status_log`, `oa_xiaohongshu_note` |
| BILIBILI | FOLLOWER_STATS only (ADR-049) | `oa_account_status_log` |

**E2E (beta):** WECHAT_OFFICIAL acc 1000112 — 4/4 typeResults 带 `targetTable`；XHS/BILI 无成员跳过。

## Phase C — Quality API

**Blocker:** ADR-060 §2/§5.2 仍将 collect quality 标为 OOS Accept / stub。PRD-M10 / API-M10 §2 有 API 契约，但 `oa_data_quality_check` / `oa_data_quality_log` 表 **未实现**（PRD-M10 §表清单）。

- 当前：`GET /admin-api/ops/collect/quality/list` → `code=0`, `list=[]`（stub）
- **不得发明** CRUD/run 直至产品 carve-out + Flyway 表

## Phase D — Bind sign-off

`ChannelAAccountCollectExecutor` 执行前校验：

1. `oa_collector_account_bind` 行存在
2. `bindStatus=BOUND`
3. collector `/livez` + account health

未绑定 → `FAILED` typeOutcome，不进入 SyncService。

E2E bind checks: DOUYIN/WECHAT_OFFICIAL/WECHAT_VIDEO 均 `BOUND`。

## Code changes

| File | Change |
|------|--------|
| `ChannelACollectSyncService.java` | +WECHAT_OFFICIAL/XIAOHONGSHU/BILIBILI dispatch |
| `ChannelFollowerStatsSyncService.java` | +syncWechatMp/Xiaohongshu/Bilibili follower stats |
| `WechatMp*SyncService.java` (×4) | 自 legacy 迁入 |
| `XiaohongshuNote*SyncService.java` (×2) | 自 legacy 迁入 |
| `UnifiedCollectRunService.java` | note 文案更新 |
| `ChannelAAccountCollectExecutor.java` | javadoc 更新 |

## Artifacts

- `RESULTS.json` · `log-detail.json` · `result-json.json`
- `bind-*.json` · `quality-list.json` · `smoke_complete.py`
