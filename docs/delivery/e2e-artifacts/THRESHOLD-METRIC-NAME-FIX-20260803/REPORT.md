# THRESHOLD-METRIC-NAME-FIX (2026-08-03)

## Root cause

**Missing `dict_threshold_metric` entries in shenyu-system SSOT + frontend not using dict lookup.**

1. Backend `@InDict("dict_threshold_metric")` on `metricType`; seed data (V43/V57) stores codes like `PLAY_COUNT`, `FAN_GROWTH`, `ENGAGEMENT` in `metric_type` / `metric_name`.
2. V14 seeded only 3 codes (`HIT_THRESHOLD`, `LOW_SCORE`, `FAN_ALERT`) into archived `sys_dict_data`; `@InDict` / `DictSelect` read **shenyu-system.system_dict_data** (V163+ SSOT).
3. Frontend **预警规则列表** rendered raw `metricName` (often a code) without `DictLabel` → users saw garbled codes or `?` when labels were absent/corrupt.

## Fix

| Layer | Change |
|-------|--------|
| Flyway V176 | Idempotent insert `dict_threshold_metric` type + 19 data rows into `shenyu-system` |
| apply script | `scripts/integration-config/apply_v176_threshold_metric.py` for beta (Flyway off / no cross-DB) |
| Frontend | `ThresholdConfig.vue`: list columns use `DictLabel dict_threshold_metric`; forms use `DictSelect` on `metricType` |
| Mock | `mock/ops/dict.ts` aligned with V176 codes |

## Dict type / codes

**Type:** `dict_threshold_metric`

| Code | Label |
|------|-------|
| PLAY_COUNT | 播放量 |
| LIKE_COUNT | 点赞数 |
| COMMENT_COUNT | 评论数 |
| SHARE_COUNT | 转发数 |
| READ_COUNT | 阅读量 |
| FAN_GROWTH | 粉丝增长 |
| FAN_COUNT / FOLLOWER | 粉丝数 |
| ENGAGEMENT | 互动率 |
| CONVERSION | 转化率 |
| LIVE_ONLINE | 直播在线人数 |
| NEGATIVE_RATE | 负面情绪比例 |
| POST_FREQUENCY | 发布频率 |
| HIT_THRESHOLD | 爆款阈值 |
| LOW_SCORE | 低分阈值 |
| FAN_ALERT | 粉丝预警 |
| GMV | GMV |
| VIEW_DROP | 阅读量骤降 |
| PLAY_DROP | 播放量骤降 |

## Files changed

- `football-backend-saas/.../V176__dict_threshold_metric.sql`
- `scripts/integration-config/apply_v176_threshold_metric.py`
- `football-front/apps/web-ele/src/views/ops/config/ThresholdConfig.vue`
- `football-front/apps/web-ele/src/mock/ops/dict.ts`

## Manual verification

1. **Local (Flyway on):** restart ops-server → V176 applies → open `#/ops/config-threshold` → tab **预警阈值** → **指标名称** shows 播放量 / 粉丝增长 etc. (not `PLAY_COUNT` or `?`).
2. **Beta remote:** `python scripts/integration-config/apply_v176_threshold_metric.py` then hard-refresh frontend.
3. **API spot-check:** `GET /admin-api/ops/dict/data?type=dict_threshold_metric` returns 19 items with UTF-8 labels.
4. **Create rule:** 新增规则 → 指标名称 dropdown lists dict items → save → list shows Chinese label.
5. **Regression:** 粉丝阈值 / 作品阈值 / 账号覆盖 tabs still load; platform/status DictLabels unchanged.

## Notes

- Legacy rows with Chinese `metric_name` and empty `metric_type` still display via `fallback`.
- New/edited rules write `metricType` (+ mirror code in `metricName` for search compat).
