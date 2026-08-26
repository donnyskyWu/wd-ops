# Gateway — Ops API 路由（Production）

**版本:** 2026-08-25  
**SSOT:** `football-gateway` JAR 内置 `application.yaml`（ADR-058 D4）

---

## 终态路由

| 属性 | 值 |
|------|-----|
| 路径前缀 | `/admin-api/ops/**` |
| 负载均衡 | `grayLb://ops-server` |
| 服务名 | `ops-server`（Nacos `spring.application.name`） |
| 端口 | `48094` |
| 响应超时 | **300s**（AI 内容生成长请求） |

> **已废除:** `/admin-api/oa/**` → RewritePath（P-C 2026-07-31）。Prod 仅暴露 `/admin-api/ops/**`。

---

## Nacos 注册要求

| 服务 | namespace | 说明 |
|------|-----------|------|
| `ops-server` | `{{NACOS_NAMESPACE:prod}}` | 本 pack 主服务 |
| `system-server` | 同 namespace | Feign DictDataApi / PermissionApi |
| `member-server` | 同 namespace | 私域报表 Feign |
| `match-server` | 同 namespace | 工作任务赛事 proxy（可选直连 URL） |

Gateway 通过 Nacos 发现 `ops-server`；**无需**在 prod overlay 写死 `http://host:48094`（beta/local overlay 除外）。

---

## Gateway JAR 参考片段

以下为 ADR-058 终态示意（实际以部署的 gateway JAR 版本为准）：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: ops-admin-api
              uri: grayLb://ops-server
              predicates:
                - Path=/admin-api/ops/**
              filters:
                - RewritePath=/admin-api/ops/v3/api-docs, /v3/api-docs
              metadata:
                response-timeout: 300000
          httpclient:
            response-timeout: 300s
```

Knife4j 聚合文档：`service-name: ops-server`，api-docs 路径 `/admin-api/ops/v3/api-docs`。

---

## 部署检查

```bash
# 1. Nacos 实例
curl -s "http://{{NACOS_SERVER_ADDR}}/nacos/v1/ns/instance/list?serviceName=ops-server&namespaceId={{NACOS_NAMESPACE}}"

# 2. 经 Gateway 冒烟（需 Football 登录 Token）
curl -H "Authorization: Bearer {{TOKEN}}" \
  "https://{{GATEWAY_HOST}}/admin-api/ops/ip-group/tree"

# 3. 工作任务
curl -H "Authorization: Bearer {{TOKEN}}" \
  "https://{{GATEWAY_HOST}}/admin-api/ops/work-task/sheet/get-or-create?ipGroupId=1&workDate=2026-08-25"
```

期望：`code=0`；502/503 检查 Nacos 注册与 namespace。

---

## 前端 API 基址

Football Admin UI 请求走 Gateway，axios `baseURL` 通常为 `/admin-api`（相对路径）。Ops 页面 API 前缀：`/admin-api/ops/...`。

详见 [manifests/frontend-deploy.md](../manifests/frontend-deploy.md)。

---

## 相关文档

- [ADR-058](../../adr/ADR-058-OPS后端单仓与football-module-ops命名.md) §D4 Gateway 路径终态
- [OPS-MENU-ROUTE-INDEX.md](../../delivery/OPS-MENU-ROUTE-INDEX.md) 菜单 → 路由
- E2E: `docs/delivery/e2e-artifacts/P-C-ROUTE-20260731/`
