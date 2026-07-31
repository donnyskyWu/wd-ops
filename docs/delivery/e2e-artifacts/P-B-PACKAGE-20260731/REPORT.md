# P-B 包改名证据 — 2026-07-31

| 字段 | 值 |
|------|---|
| Slice | **P-B** `cn.iocoder.yudao.module.oa.**` → `football.module.ops.**` |
| ADR | ADR-058 §2.1 · 终态缺口计划 §P-B |
| 范围 | `football-module-ops-server` 生产源码（**不含** `legacy-archive`） |
| HTTP 路径 | **未改**（仍 `/admin-api/ops/**`，P-C 已完成） |
| 权限码 | **未改**（仍 `oa:*`，属 P-D） |

## 变更摘要

1. **业务包**：`cn.iocoder.yudao.module.oa.**` → `football.module.ops.**`（703 Java 文件）
2. **ops-local framework 副本**：`cn.iocoder.yudao.framework.**` → `football.module.ops.framework.**`（Feign API / CommonResult / Tenant* 等）
3. **`OpsServerApplication`**：`scanBasePackages = "football.module.ops"`；`@MapperScan("football.module.ops.dal.mysql")`
4. **`application.yaml`**：logging 包名对齐；`type-aliases-package` / `football.info.base-package` 已是终态
5. **`legacy-archive/`**：保留旧包（P-G 再删）

## 构建

```text
mvn -pl football-module-ops/football-module-ops-server -am package -DskipTests
→ BUILD SUCCESS
```

## Smoke（login admin / tenant 1）

| Check | URL | http | code | 结果 |
|-------|-----|------|------|------|
| GW account/list | `/admin-api/ops/account/list` | 200 | 0 | ✅ |
| GW content/list | `/admin-api/ops/content/list` | 200 | 0 | ✅ |
| GW ip-group/tree | `/admin-api/ops/ip-group/tree` | 200 | 0 | ✅ |
| GW task/list | `/admin-api/ops/task/list` | 200 | 0 | ✅ |
| GW football-order/list | `/admin-api/ops/football-order/list` | 200 | 0 | ✅ |
| Direct account/list | `:48094/admin-api/ops/account/list` | 200 | 0 | ✅ |
| Direct content/list | `:48094/admin-api/ops/content/list` | 200 | 0 | ✅ |

汇总：[RESULTS.json](./RESULTS.json) — **7/7 PASS**

## 未做（同切片禁止）

- P-D `oa:*` → `ops:*`
- P-E / P-F / P-G（legacy-archive 未删）
