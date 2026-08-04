# EXTERNAL-COLLECT · Douyin user-videos (2026-08-03)

## Root cause

Ops `ExternalAccountCollectExecutor.collectDouyinLimited` rejected any `account_identifier` that was not an `http` video URL, returning:

> 抖音 external 仅支持 parse-video（account_identifier 须为视频 URL）；user-videos 待 collector P2

Beta config **id=42** (`毒舌电影`) stores a **sec_uid** (`MS4wLjABAAAA_py8TGmFe6t8KDY04LU0JH9Yr9ml54dCjRFi0mc1lwI`), not a video URL — so the run failed before calling collector.

**Collector gap was overstated**: `unify-collector-api` already exposes:

- `GET /api/v1/external/douyin/user-profile?sec_uid=`
- `GET /api/v1/external/douyin/user-videos?sec_uid=&cursor=&page_size=`

Only Ops wiring was missing.

## Implementation

| Layer | Change |
|-------|--------|
| `ExternalCollectorApiClient` | Added `getDouyinUserProfile`, `getDouyinUserVideos` (+ stub responses) |
| `ExternalAccountCollectExecutor` | Replaced `collectDouyinLimited` with `collectDouyin` routing: video URL → `parse-video`; user id → profile + paginated `user-videos` → `oa_external_account` / `oa_external_work` |
| `ExternalAccountCollectExecutor.fillWorkFields` | Truncate `title` (200) / `work_url` (500) to match schema |
| `ExternalCollectConfig.vue` | DOUYIN placeholder + hint for sec_uid / profile URL / video URL |

## Identifier detection contract

| Input | Route | Collector API |
|-------|-------|-----------------|
| `http…` + `/video/` or `v.douyin.com` or `/share/video/` | Single video | `GET …/douyin/parse-video?video_url=` |
| Raw `MS4wLjAB…` (≥32 chars) | User + videos | `user-profile` + `user-videos` |
| Profile URL containing `sec_uid=` | User + videos | same (extract param) |
| Other non-HTTP string | User + videos | passed as `sec_uid` to collector `resolve_sec_uid` |
| HTTP URL without video path and without `sec_uid=` | **Error** | unsupported format message |
| Blank | **Error** | account_identifier 为空 |

## Verification

### Compile

```text
mvn -pl football-module-ops/football-module-ops-server -am compile -DskipTests  → OK
```

### Collector direct (config 42 sec_uid)

```text
user-profile → code=0, aweme_count=524
user-videos  → code=0, videos=2, has_more=True (page_size=3)
```

### Ops E2E (beta, task id=9, config id=42)

After ops-server rebuild + restart:

```text
logStatus: SUCCESS
recordCount: 200 (20/page × PAGE_LIMIT 20 pages max)
typeResults: EXT_DOUYIN_USER_PROFILE + EXT_DOUYIN_USER_VIDEOS
```

Artifacts copied from `EXTERNAL-COLLECT-20260803/smoke_external_collect.py` run (latest log-detail.json).

## Manual test steps

1. Ensure collector `:8000` live and `DOUYIN_COOKIE` configured (optional; public profile/post often works without).
2. M8 外部采集配置 → 抖音 → `account_identifier` = sec_uid or profile URL.
3. Toggle **是否采集** → 统一外部任务 → **立即执行**.
4. 采集日志应 SUCCESS；`oa_external_work` 有 DOUYIN 行。

## Spec gaps (non-blocking)

- `EXT_DOUYIN_USER_PROFILE` / `EXT_DOUYIN_USER_VIDEOS` dict entries not in Flyway (V175 only has `EXT_DOUYIN_PARSE_VIDEO`); typeResults use string codes regardless.
- P2-1 `oa_external_follower_daily` follower snapshot not implemented (profile fetched but follower daily not persisted).
- Douyin **keyword search** (`search-user` / `search-video`) still ADR-068 gap for keyword members.
- `unique_id` (抖音号) without prior search cache may fail at collector unless resolved to sec_uid.
