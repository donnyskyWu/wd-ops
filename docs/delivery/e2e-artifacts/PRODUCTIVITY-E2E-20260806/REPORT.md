# PRODUCTIVITY E2E — 2026-08-06

- Gateway: `http://127.0.0.1:48080` | Ops health: OK
- Login: OK (admin / tenant 1)

## Feature summary

| Feature | Result |
|---------|--------|
| productivity_review | PASS |
| layout_template | PASS |
| order_attribution | PASS |
| order_attribution_roi_api | PASS |
| order_attribution_roi_ui_width | SKIP (manual visual) |

## Checks

- **PASS** `productivity-list-week` http=200 code=0 — ok
- **PASS** `productivity-list-month` http=200 code=0 — ok
- **PASS** `productivity-list-daterange` http=200 code=0 — ok
- **PASS** `productivity-tab-wechat` http=200 code=0 — ok
- **PASS** `productivity-tab-video` http=200 code=0 — ok
- **PASS** `layout-template-list` http=200 code=0 — ok
- **PASS** `football-order-list` http=200 code=0 — ok
- **PASS** `order-attribution-list` http=200 code=0 — ok
- **PASS** `order-attribution-roi` http=200 code=0 — ok
- **PASS** `productivity-detail-user` http=200 code=0 — ok

## tabType wechat vs video

```json
{
  "wechat_rows": 5,
  "video_rows": 5,
  "sample_diffs": {
    "contentOutput": {
      "wechat": 0,
      "video": 0
    },
    "avgRead": {
      "wechat": 0,
      "video": 0
    },
    "avgPlay": {
      "wechat": 0,
      "video": 0
    },
    "hitCount": {
      "wechat": 0,
      "video": 0
    }
  },
  "same_schema": true
}
```

## Playwright

- **ux-routes-smoke** (/ops/efficiency, /ops/order-attribution): PASS (legacy alias paths; see menu SSOT below).
- **Browser spot-check** (seedFootballAuth + menu routes): PASS
  - /ops/operations/efficiency — tabs clicked (≥2)
  - /ops/production/layout-template — list page rendered
  - /ops/performance/order-attribution — page rendered
- Screenshots: rowser-efficiency.png, rowser-layout-template.png, rowser-order-attribution.png

## Menu route SSOT (tenant 1 admin)

| Page | Path |
|------|------|
| 人效盘点 | /ops/operations/efficiency |
| 公推模板 | /ops/production/layout-template |
| 订单归因 | /ops/performance/order-attribution |

## Order attribution ROI UI width

Skipped (manual visual check only).
