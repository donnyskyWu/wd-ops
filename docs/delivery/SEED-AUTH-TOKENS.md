# SEED-AUTH · Dev Token 对表

> 与 [`ADR-003`](../adr/ADR-003-模拟鉴权与外部平台SSO对接.md) §4、`V15__seed_auth.sql` 保持一致。  
> 前端配置见 `ops-platform-ui-vue/.env.development`。

## 预置 Token

| Token | userId | tenantId | 角色 | 数据范围 | 用途 |
|-------|--------|----------|------|---------|------|
| `dev-token-oa-admin` | 1001 | 1 | OA_ADMIN | ALL | 全权限联调 |
| `dev-token-oa-leader` | 1002 | 1 | OPS_LEADER | ALL | 运营组长 |
| `dev-token-oa-operator` | 1003 | 1 | OPS_OPERATOR | IP_GROUP(9001) | AUTH-005 数据范围 |
| `dev-token-oa-finance` | 1004 | 1 | FINANCE | ALL | 财务只读用户 |
| `dev-token-oa-analyst` | 1005 | 1 | OPS_LEADER | ALL | 数据分析 |
| `dev-token-oa-tenantb` | 2001 | 2 | TENANT_ADMIN | ALL | 跨租户 1504 |

## 前端切换示例

```env
# 管理员（默认）
VITE_API_TOKEN=dev-token-oa-admin
VITE_TENANT_ID=1

# 运营专员（IP 组 9001，仅 5 条 SEED 账号）
# VITE_API_TOKEN=dev-token-oa-operator
# VITE_TENANT_ID=1
```

## AUTH 验收映射

| ID | 验证方式 |
|----|---------|
| AUTH-001 | `GateS2AuthIT#adminHasUserListPermission` |
| AUTH-002 | `GateS2AuthIT#operatorCannotCreateTenant` → 403 |
| AUTH-003 | `GateS2AuthIT#tenantBTokenCannotAccessTenant1Data` → 1504 |
| AUTH-004 | `GateS2AuthIT#missingTokenReturns401` |
| AUTH-005 | `GateS2AuthIT#operatorIpGroupDataScope` admin=10 / operator=5 |
