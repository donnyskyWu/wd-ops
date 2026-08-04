# ADR-067: M10 直播采集（DOUYIN · WECHAT_VIDEO）

| 字段 | 值 |
|------|---|
| 状态 | **Accepted** |
| 日期 | **2026-08-03** |
| 关联 | ADR-049 · ADR-061 · ADR-047 · PRD-M10 · API-M10 |
| 触发 | 产品授权本 Slice：抖音/视频号账号级「采集直播数据」开关 + 统一任务落库 + 内部作品分析展示 |

## 1. 背景

ADR-049 全量 Channel-A 采集覆盖粉丝与短视频/图文，**不含直播场次**。unify-collector-api 已提供：

| 平台 | live-list | live-stats |
|------|-----------|------------|
| DOUYIN | `GET /api/v1/internal/douyin-live/live-list?account_id=` | `GET /api/v1/internal/douyin-live/live-stats?account_id=&live_id=` |
| WECHAT_VIDEO | `GET /api/v1/internal/wechat-channels/live-list?account_id=` | `GET /api/v1/internal/wechat-channels/live-stats?account_id=&live_id=` |

运营需按账号可选开启直播采集，并在 **内部作品分析** 与短视频一并查看。

## 2. 决策

| # | 决策 |
|---|------|
| Q1 | 账号开关字段名 **`collect_live_enabled`**（`oa_account`，TINYINT 0/1，默认 0） |
| Q2 | 仅 **`DOUYIN`**、**`WECHAT_VIDEO`** 可设为 1；其它平台写 true → **1503** |
| Q3 | 开关与 **`collect_enabled` 独立**：须先开启「是否采集」加入统一任务；直播类型仅在 `collect_live_enabled=1` 时追加执行 |
| Q4 | 追加 `dict_collect_data_type`（非替换 ADR-049 基线顺序） |

### 2.1 dataType 顺序（在平台基线之后追加）

| `platform_type` | 追加 dataTypes（仅 `collect_live_enabled=1`） |
|-----------------|---------------------------------------------|
| DOUYIN | `DOUYIN_LIVE_LIST` → `DOUYIN_LIVE_STATS` |
| WECHAT_VIDEO | `WECHAT_VIDEO_LIVE_LIST` → `WECHAT_VIDEO_LIVE_STATS` |

### 2.2 落库表（Flyway V173）

| 表 | dataType | UK |
|----|----------|-----|
| `oa_douyin_live` | DOUYIN_LIVE_* | tenant + account + live_id |
| `oa_wechat_video_live` | WECHAT_VIDEO_LIVE_* | tenant + account + live_id |

字段（两表对称）：`live_id`、`title`、`cover_url`、`live_url`、`started_at`、`ended_at`、`duration_sec`、`viewer_count`、`peak_viewer_count`、`like_count`、`comment_count`、`share_count`、`synced_at`、`stats_synced_at`。

### 2.3 内部作品分析桥接（ADR-049 Q4 扩展）

| 消费方 | 映射 |
|--------|------|
| `CollectedDataQueryService.pageInternalContents` | `oa_douyin_live` / `oa_wechat_video_live` → `contentType=LIVE`（`dict_content_type`） |
| 筛选 | `contentType=LIVE` 仅直播；`SHORT_VIDEO` 不含直播；空=合并 |
| `dataSource` | `COLLECT` |

## 3. API Delta

账号 create/update/list/detail 增加 **`collectLiveEnabled`**（Boolean）。校验见 Q2。

统一任务 run 日志 `typeResults[]` 在开关开启时出现 `targetTable=oa_douyin_live` / `oa_wechat_video_live`。

## 4. Out of Scope

- 快手/B 站/公众号直播
- 直播实时调度（仍随统一任务 cron）
- 采集数据写入 `oa_content_daily`

## 5. 变更记录

| 日期 | 说明 |
|------|------|
| 2026-08-03 | Accepted；V173 + SyncService + 账号 UI + 展示桥接 |
