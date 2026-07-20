# OPS 粉丝数据路径调整（2026-07-18）

## 背景

- **公众号粉丝明细**：Football SSOT = `shenyu-mp.mp_user`（非 OPS `oa_wechat_mp_follower`）
- **采集**：公众号/抖音账号采集仅保留**粉丝总数**，不再同步粉丝明细列表

## 架构决策

| 域 | 旧路径 | 新路径 |
|---|---|---|
| 公众号粉丝列表读 | `oa_wechat_mp_follower` | `@DS("mp")` → `mp_user` |
| 公众号/抖音粉丝总数 | `oa_account_status_log.follower_count` | 不变（`MP_FOLLOWER_STATS` / `FOLLOWER_STATS`） |
| 采集 MP_FOLLOWER_LIST | 写 `oa_wechat_mp_follower` | 兼容转调 `MP_FOLLOWER_STATS`，不写明细 |
| 采集 DOUYIN_FOLLOWER_LIST | 写 `oa_douyin_follower` | 兼容转调 `FOLLOWER_STATS`，不写明细 |
| 全量采集默认顺序 | 含 LIST 步骤 | 已移除 LIST，仅 STATS + 作品类 |

## 变更文件

### 后端（oa-server）

- `MpUserDO` / `MpUserMapper` / `MpUserDataService` — Football 粉丝跨库读
- `WechatOfficialAccountResolver.resolveMpAccountId()` — 解析 `mp_account.id`
- `WechatMpFollowerQueryServiceImpl` — 改读 `mp_user`
- `WechatMpFollowerSyncService` / `DouyinFollowerSyncService` — 明细同步停用，转调 stats
- `CollectPlatformDefaults` — 全量采集移除 LIST 类型
- `CollectExecutionService` / `CollectLogResultBuilder` — LIST 任务路由与日志摘要

### 前端（ops-platform-ui-vue）

- `AccountAnalysisDetail.vue` — 移除抖音粉丝列表 Tab；公众号空态文案更新
- `PlatformAccountDetail.vue` — 同上

### 测试

- `M4AccountMpFollowerIT` — 改 seed `mp_user`
- `M10ApiCollectorExecS05IT` / `M10ApiCollectorExecDouyinListIT` — 期望改为 stats
- `WechatMpFollowerSyncServiceTest` — 委托 stats 单测

## 未改动 / 保留

- `oa_wechat_mp_follower` / `oa_douyin_follower` 表与 Mapper **保留**（历史数据、迁移期只读）
- `dict_collect_data_type` 中 `MP_FOLLOWER_LIST` / `DOUYIN_FOLLOWER_LIST` **保留**（旧任务兼容路由）
- Football `football-backend-saas` **未修改**
- Football 菜单「粉丝管理」(MpUser, menu 2099) **未隐藏** — 仍为 SSOT 维护入口

## 阻塞 / Spec 缺口

1. **Spec 未同步**：`API-M4-账号管理.md` §mp-followers 仍写 `oa_wechat_mp_follower`；`API-M10-数据采集.md` 仍列 LIST 落库表 — 需产品/工程 Spec 修订
2. **Legacy `oa_account` 与 `mp_account` ID 不一致**：`resolveMpAccountId` 按 `app_id` 关联；无 `app_id` 时 fallback 为同 ID（仅测试/过渡期）
3. **`M8MetadataIT`** 仍断言 `oa_wechat_mp_follower` 元数据 — 表未删，可后续改描述

## 手工验证

1. 启动 oa-server（`dev-local-multidb` profile），确认 `@DS("mp")` 可连 `shenyu-mp`
2. 打开公众号账号详情 →「粉丝列表」Tab，应展示 `mp_user` 数据（非采集 Tab 引导）
3. 执行采集任务 `MP_FOLLOWER_STATS` / 全量采集：检查 `oa_account_status_log.follower_count` 有更新，`oa_wechat_mp_follower` 无新增
4. 执行旧任务 `MP_FOLLOWER_LIST`：应成功且仅更新粉丝总数，不写明细
5. 抖音账号：确认无「粉丝列表」Tab；`FOLLOWER_STATS` 采集更新 `oa_account_status_log`
6. Football 粉丝管理页（菜单 2099）仍可 CRUD `mp_user`
