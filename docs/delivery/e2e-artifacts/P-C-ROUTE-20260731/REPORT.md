# P-C 路径对齐证据 — 2026-07-31

| 字段 | 值 |
|------|---|
| Slice | **P-C** Controller `/admin-api/oa` → `/admin-api/ops` + 移除 Gateway Rewrite |
| ADR | ADR-058 D4 / 终态缺口计划 §P-C |
| 决策 | **全切**；不保留 Controllers 双路由别名；旧 `/admin-api/oa/**` 业务码 404 |
| 库 | 本地 `football-ops`（非 Beta） |
| 权限码 | **未改**（仍 `oa:*`，属 P-D） |

## 变更摘要

1. **Controllers / helpers**（62 文件）：`/admin-api/oa` → `/admin-api/ops`（含 stub `DeferredCutoverStubController`、`ImageKeyHelper`、`PhoneServiceImpl`、`OpsFoundationController`）
2. **Gateway**：移除 `RewritePath=/admin-api/ops/(.*) → /admin-api/oa/$1`
   - `football-gateway/.../application.yaml`
   - `scripts/integration-config/gateway-integration-local.yaml`
   - `scripts/integration-config/gateway-integration-beta.yaml`
3. knife4j ops 仍挂 `/admin-api/ops/v3/api-docs`（仅 api-docs Rewrite）

## Rewrite 已移除证据

见 [REWRITE-REMOVED.json](./REWRITE-REMOVED.json)：三处源配置均无 `ops→oa` RewritePath。

## Smoke（login admin / tenant 1）

| Check | URL | http | code | 结果 |
|-------|-----|------|------|------|
| GW account/list | `/admin-api/ops/account/list` | 200 | 0 | ✅ |
| GW content/list | `/admin-api/ops/content/list` | 200 | 0 | ✅ |
| GW ip-group/tree | `/admin-api/ops/ip-group/tree` | 200 | 0 | ✅ |
| GW task/list | `/admin-api/ops/task/list` | 200 | 0 | ✅ |
| Direct account/list | `:48094/admin-api/ops/account/list` | 200 | 0 | ✅ |
| Direct content/list | `:48094/admin-api/ops/content/list` | 200 | 0 | ✅ |
| Direct 旧 oa | `:48094/admin-api/oa/account/list` | 200 | **404** | ✅ breaking（无别名） |
| GW 旧 oa | `:48080/admin-api/oa/account/list` | 200 | **404** | ✅ 无路由/无映射 |

汇总：[RESULTS.json](./RESULTS.json) — **8/8 PASS**

## 未做（同切片禁止）

- P-B 包改名
- P-D `oa:*` → `ops:*`
- P-E / P-F / P-G

## 备注

- 历史 `scripts/probe-*` / UAT 脚本仍可能硬编码 `/admin-api/oa`（非生产路径）；生产 FE/GW 已 ops。
- 旧 oa 返回 **业务 code=404**（HTTP 仍 200），符合「无 Controller 映射」的全局错误封装，非兼容别名。
