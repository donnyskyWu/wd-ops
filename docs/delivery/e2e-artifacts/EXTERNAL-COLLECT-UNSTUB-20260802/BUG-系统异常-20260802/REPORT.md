# Bug: 外部采集配置页「系统异常」

**Date**: 2026-08-02 · **Status**: RESOLVED（无业务代码缺陷）

## Root cause

用户报障时刻（~20:54）与 **ops-server 重打包/重启** 重合：

| 事件 | 时间 |
|------|------|
| `football-module-ops-server.jar` LastWriteTime | 20:54:43 |
| ops-server PID 40480 CreationDate | 20:54:44 |

Gateway `GlobalExceptionHandler` 在下游不可达时兜底返回 `{code:500, msg:"系统异常"}`（`GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR`）。

ops 日志中 **无** `/admin-api/ops/config/external-collect/**` 的 ERROR / SQL / NPE 堆栈；卸 stub 后的 Controller 已在运行 JAR 内。

## Fix

无需改业务代码。确认 ops-server `:48094` UP（本次重启后已 UP）。

## Smoke

| API / UI | 结果 |
|----------|------|
| `GET …/external-collect/list?subType=account` | code=0 · total=4 |
| `GET …/external-collect/keyword/list` | code=0 · total=5 |
| `GET …/dict/data?type=dict_platform_type` | code=0 |
| 浏览器 `/ops/config/config-external-collect` | 外部账号 4 行 · 关键词 5 行 · 无「系统异常」toast |

> 注意：`/ops/config-external-collect`（缺 `config/` 段）为菜单路径外 404，与本 bug 无关。正确菜单路径为 `/ops/config/config-external-collect`。
