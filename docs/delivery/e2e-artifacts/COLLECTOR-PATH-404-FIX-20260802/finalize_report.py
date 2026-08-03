# -*- coding: utf-8 -*-
import json
import urllib.request
from pathlib import Path

ART = Path(__file__).resolve().parent


def http(method, url, headers=None, data=None, timeout=60):
    import urllib.error

    h = dict(headers or {})
    body = None
    if data is not None:
        body = json.dumps(data).encode()
        h.setdefault("Content-Type", "application/json")
    req = urllib.request.Request(url, data=body, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


_, login_raw = http(
    "POST",
    "http://127.0.0.1:48080/admin-api/system/auth/login",
    {"tenant-id": "1", "X-Tenant-Id": "1"},
    {"username": "admin", "password": "admin123"},
)
(ART / "00-login.json").write_text(login_raw, encoding="utf-8")
token = json.loads(login_raw)["data"]["accessToken"]
h = {
    "Authorization": f"Bearer {token}",
    "tenant-id": "1",
    "X-Tenant-Id": "1",
    "Content-Type": "application/json",
}


def get(url: str) -> str:
    _, body = http("GET", url, h)
    return body


body = get("http://127.0.0.1:48080/admin-api/ops/collect/log/23")
(ART / "log-detail.json").write_text(body, encoding="utf-8")
detail = json.loads(body)
print("detail code", detail.get("code"))
data = detail.get("data") or {}
rj = data.get("resultJson")
if isinstance(rj, str) and rj.strip().startswith("{"):
    parsed = json.loads(rj)
    (ART / "result-json.json").write_text(
        json.dumps(parsed, ensure_ascii=False, indent=2), encoding="utf-8"
    )
elif isinstance(rj, dict):
    parsed = rj
    (ART / "result-json.json").write_text(
        json.dumps(parsed, ensure_ascii=False, indent=2), encoding="utf-8"
    )
else:
    parsed = None

page = json.loads((ART / "log-page.json").read_text(encoding="utf-8"))
latest = page["data"]["list"][0]
prev = page["data"]["list"][1]
err = latest.get("errorMessage") or ""
prev_err = prev.get("errorMessage") or ""
summary = {
    "after": {
        "id": latest["id"],
        "status": latest["status"],
        "recordCount": latest["recordCount"],
        "durationMs": latest["durationMs"],
        "errorMessage": err,
    },
    "before": {
        "id": prev["id"],
        "status": prev["status"],
        "recordCount": prev["recordCount"],
        "errorMessage": prev_err[:600],
    },
    "path404_gone": (
        "/followers/stats" not in err
        and "articles?offset" not in err
        and "/videos?cursor" not in err
    ),
    "only_relogin_partial": ("需重新登录" in err) or ("凭证失效" in err),
}
(ART / "COMPARE.json").write_text(
    json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8"
)
print(json.dumps(summary, ensure_ascii=False, indent=2))

# extract type outcomes if present
outcomes = []
if isinstance(parsed, dict):
    def walk(o):
        if isinstance(o, dict):
            if "label" in o and ("ok" in o or "success" in o or "error" in o or "message" in o):
                outcomes.append(o)
            for v in o.values():
                walk(v)
        elif isinstance(o, list):
            for v in o:
                walk(v)
    walk(parsed)

res = json.loads((ART / "RESULTS.json").read_text(encoding="utf-8"))
res["task"]["latestLog"] = summary["after"]
res["task"]["previousFailedLog"] = summary["before"]
res["task"]["outcomesSample"] = outcomes[:30]
res["verdict"]["run_status_after"] = latest["status"]
res["verdict"]["recordCount"] = latest["recordCount"]
res["verdict"]["mp_douyin_path404_gone"] = summary["path404_gone"]
res["verdict"]["wechat_video_relogin_only_remaining"] = summary["only_relogin_partial"]
(ART / "RESULTS.json").write_text(
    json.dumps(res, ensure_ascii=False, indent=2), encoding="utf-8"
)

report = f"""# COLLECTOR-PATH-404-FIX-20260802

## Root cause

ADR-061 `PROBE_COUNT_ONLY` 路径在 `UnifiedCollectorProbeClient#resolveProbePath` 臆造了 **嵌套** URL：

- `/api/v1/internal/wechat-mp/{{accountId}}/followers/stats`
- `/api/v1/internal/wechat-mp/{{accountId}}/articles?...`
- `/api/v1/internal/douyin/{{accountId}}/videos?...`

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
| nested `.../wechat-mp/{{id}}/followers/stats` | 404 | FastAPI Not Found |
| flat `.../wechat-mp/follower-stats?account_id=` | 200 | real account OK |
| nested `.../douyin/{{id}}/videos?...` | 404 | FastAPI Not Found |
| flat `.../douyin/video-list?account_id=` | 200 | real account OK |

### Unified task run (`taskId=8`)

| | Before (log 22) | After (log 23) |
|---|---|---|
| status | FAILED | **PARTIAL** |
| recordCount | 0 | **7** |
| error | MP/Douyin nested path HTTP 404… | only `account:1/WECHAT_VIDEO` relogin_needed |

Artifacts: `RESULTS.json` · `COMPARE.json` · `log-page.json` · `log-detail.json` · `smoke_path_fix.py`
"""
(ART / "REPORT.md").write_text(report, encoding="utf-8")
print("REPORT written")
