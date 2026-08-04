# EXTERNAL-COLLECT-20260803 · ADR-068 Channel-D 统一外部采集

> 日期：2026-08-03 · Slice：统一外部数据采集任务 + `collect_enabled`

## 交付摘要

| 区域 | 内容 |
|------|------|
| ADR | `docs/adr/ADR-068-M10-统一外部数据采集任务.md` |
| Flyway | `V175__m10_external_unified_collect_task.sql` |
| Beta 应用 | `scripts/integration-config/apply_v175_external_collect.py` |
| Backend | `ExternalUnifiedCollectTaskService(Impl)` · `ExternalCollectRunService` · `ExternalCollectScheduleParamSupport` · `ExternalCollectorApiClient` · `ExternalAccountCollectExecutor` · `TenantCollectorCredentialResolver` · `CollectTaskConfig/Keyword DO+Mapper` |
| API | `collectEnabled` on external account/keyword CRUD · `POST .../ensure-external-unified` · `GET .../{id}/external-members` · `run` 路由 `is_unified=2` |
| Frontend | `ExternalCollectConfig.vue` 是否采集 Switch · `task.vue` 外部统一任务标签/成员/确保按钮 |
| Cron | `sys_param.collect.external.unified.cron` 默认 `0 0 22 * * ?` |

## 续作修复（子 agent 完成）

| 问题 | 修复 |
|------|------|
| `CollectTaskService.java` 缺 import | 补 `CollectTaskExternalMemberVO` |
| `ExternalCollectorApiClient` `StrUtil.urlEncode` 不存在 | 改 `URLEncoder.encode` |
| `UnifiedCollectorApiClient` 缺 ADR-067 live 方法 | 补 `getDouyinLive*` / `getWechatVideoLive*` + stub |
| Beta DB 无 `sys_dict_data` | V175 DDL/参数已应用；dict 插入跳过（非阻塞） |
| ops-server 未重载新代码 | `start-integration-oa.ps1 -Rebuild` 重启 :48094 |

## 平台实现状态

| 平台 | 账号采集 | 关键词 | Gap |
|------|----------|--------|-----|
| KUAISHOU | ✅ user-videos → `oa_external_work` | ❌ 无 search API | collector keyword search |
| WECHAT_OFFICIAL | ✅ search + article-collect | ✅ search + article | 须 `oa_tenant_collector_credential` |
| DOUYIN | 🟡 parse-video（URL only） | ❌ | user-videos P2 |
| WECHAT_VIDEO | ❌ stub | ❌ stub | collector P3 |

## 验证结果（2026-08-03）

### Compile

```text
mvn -pl football-module-ops/football-module-ops-server -am compile
→ BUILD SUCCESS
```

### Beta schema (apply_v175)

```text
collect_enabled(cfg/kw): True/True
oa_collect_task_config: True
oa_collect_task_keyword: True
keyword_config_id on oa_external_work: True
cron param collect.external.unified.cron: True
flyway V175 row: recorded
```

### Smoke (`smoke_external_collect.py`)

| 检查 | 结果 |
|------|------|
| toggle-collect-enabled | ✅ |
| ensure-external-unified | ✅ taskId=9, cron=`0 0 22 * * ?` |
| external-members | ✅ memberCount=1 |
| task-run | ✅ API 200 |
| log-written | ✅ status=FAILED（见下） |

**整体 pass: true**

执行日志 `FAILED` 原因：beta 首条外部账号 id=42 为 **DOUYIN** 且 `account_identifier` 非视频 URL，符合 ADR-068 限制（仅 parse-video）。链路（成员同步 → run → 写 log → typeResults）已验证。

## 冒烟步骤

1. `python scripts/integration-config/apply_v175_external_collect.py`（beta Flyway 关闭时）
2. `.\scripts\start-integration-oa.ps1 -Profiles ...dev-test-beta -Rebuild`
3. `python docs/delivery/e2e-artifacts/EXTERNAL-COLLECT-20260803/smoke_external_collect.py`
4. 浏览器：外部采集配置 → 开启「是否采集」→ 采集任务 → 确保外部统一任务 → 外部成员 → 立即执行 → 日志

## 已知限制

- Channel-D **不**使用 `oa_collector_account_bind`（ADR-052）
- 租户凭账号 CRUD UI 未在本 Slice（P1 follow-up）
- `oa.unified-collector.stub=true` 时快手/公众号可走 stub；DOUYIN 账号须 video URL 才能 SUCCESS
- V175 中 `sys_dict_data` 插入在 beta `shenyu-ops` 无该表时跳过
