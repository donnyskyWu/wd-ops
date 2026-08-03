# COLLECTOR-PATH-404-FIX-20260802

## Root cause

ADR-061 `PROBE_COUNT_ONLY` 路径在 `UnifiedCollectorProbeClient#resolveProbePath` 臆造了 **嵌套** URL：

- `/api/v1/internal/wechat-mp/{accountId}/followers/stats`
- `/api/v1/internal/wechat-mp/{accountId}/articles?...`
- `/api/v1/internal/douyin/{accountId}/videos?...`

unify-collector-api OpenAPI **没有**这些路由（FastAPI `404 Not Found`）。

真实路由（与 `UnifiedCollectorApiClient` 一致）为 flat query：

| dataType | Correct path |
|---|---|
| MP_FOLLOWER_STATS | `GET /api/v1/internal/wechat-mp/follower-stats?account_id=` |
| MP_ARTICLE_* | `GET /api/v1/internal/wechat-mp/publish-list?account_id=&begin=0&end=1`（探测；`article-list` 另需 fakeid） |
| FOLLOWER_STATS / DOUYIN | `GET /api/v1/internal/douyin/follower-stats?account_id=` |
| DOUYIN_VIDEO_* | `GET /api/v1/internal/douyin/video-list?account_id=&cursor=&page_size=` |
| WECHAT_VIDEO | `GET /api/v1/internal/wechat-channels/follower-stats?account_id=` |
| … | 同理 kuaishou / xiaohongshu `follower-stats` / `video-list` |
| BILIBILI | `GET /api/v1/internal/bilibili/user/me` + `X-Account-Id` |

## WECHAT_VIDEO（非路径问题）

`account:1/WECHAT_VIDEO` → bind `acc_wechat_channels_2e205365ffd7266c`，collector health `alive=false` / `status=relogin_needed`。需用户重新扫码登录，**不是** URL 映射错误。活跃视频号示例：`acc_wechat_channels_044cb00401af77e2`。

## Fix

- File: `football-module-ops-server/.../unified/UnifiedCollectorProbeClient.java`
- Align probe paths with `UnifiedCollectorApiClient` + live OpenAPI
- Rebuild + restart ops-server `:48094`

## Smoke

### Collector path contrast

| Probe | HTTP | Note |
|---|---|---|
| nested `.../wechat-mp/{id}/followers/stats` | 404 | FastAPI Not Found |
| flat `.../wechat-mp/follower-stats?account_id=` | 200 | real account OK |
| nested `.../douyin/{id}/videos?...` | 404 | FastAPI Not Found |
| flat `.../douyin/video-list?account_id=` | 200 | real account OK |

### Unified task run (`taskId=8`)

| | Before (log 22) | After (log 23) |
|---|---|---|
| status | FAILED | **PARTIAL** |
| recordCount | 0 | **7** |
| error | MP/Douyin nested path HTTP 404… | only `account:1/WECHAT_VIDEO` relogin_needed |

Artifacts: `RESULTS.json` · `COMPARE.json` · `log-page.json` · `log-detail.json` · `smoke_path_fix.py`
