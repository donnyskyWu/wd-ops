# ADR-003：模拟鉴权与外部平台 SSO 对接

| 字段 | 值 |
|------|---|
| 编号 | ADR-003 |
| 标题 | 模拟鉴权（Dev Fixed Token）与 Phase 2 外部平台 SSO 对接 |
| 状态 | ✅ Accepted |
| 日期 | 2026-06-09 |
| 决策人 | 产品 + 后端架构（待项目签字） |
| 关联 PRD | `完整PRD-v9.1-开发版.md` · M9 系统管理 |
| 关联计划 | [`BACKEND-WORK-PLAN-v1.2-已批准.md`](../delivery/BACKEND-WORK-PLAN-v1.2-已批准.md) §3.1 |
| 关联规范 | [`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md) 铁律三 · [`TECH-CONSTRAINTS.md`](../engineering/TECH-CONSTRAINTS.md) §5.1 |

---

## 1. 背景

1. 运营数据平台后期将**接入另一个已有平台**，该平台的用户管理、权限体系、租户体系与本项目设计的 `sys_user` / `sys_role` / `sys_tenant` **数据结构一致**。
2. 本期目标是尽快完成 M0–M9 业务模块，**不应重复开发完整登录/SSO 产品**。
3. 业务模块仍须真实走通：**多租户隔离（1504）**、**接口权限（@PreAuthorize）**、**四级数据范围（BR-006）**——这些不能 Mock 掉。
4. 与 ADR-001 思路一致：本期用**可替换适配层**解决「身份来源」，业务层保持稳定。

---

## 2. 决策

### 2.1 本期（Phase 1）：模拟鉴权 + 库内真实身份数据

| 项 | 做法 |
|----|------|
| **身份来源** | Dev 环境：`Authorization: Bearer {dev-token}` + `X-Tenant-Id`；由 `DevAuthFilter` 解析 |
| **用户/租户/角色/权限** | **必须写入数据库**（`seed-auth`），Filter 从 DB 加载，写入统一 `SecurityContext` |
| **禁止** | 代码中写死 `userId=1`、跳过 `@PreAuthorize`、Header 有租户但 SQL 无 `tenant_id` 过滤 |
| **登录页** | 本期不做；前端配置固定 Token + TenantId（`.env.development`） |
| **M9 范围** | 身份**数据模型** + 权限**消费** + 日志/参数；不含 SSO 回调、注册、找回密码 |

### 2.2 后期（Phase 2）：外部平台 SSO 对接

| 项 | 做法 |
|----|------|
| **身份来源** | 替换为 `ExternalPlatformAuthFilter`：校验外部平台 JWT/Token |
| **不变** | `SecurityContext` 结构、`@PreAuthorize`、租户拦截器、业务 Service |
| **新增** | 用户/租户/角色**同步**（定时或事件）；权限码映射表（外部码 ↔ `oa:*`） |
| **前端** | 跳转外部登录或嵌入 SSO；Token 写入方式变更，请求头格式不变 |

### 2.3 AuthProvider 抽象

```text
AuthProvider（接口）
├── DevAuthProvider          # Phase 1：固定 Token → 查 DB
└── ExternalAuthProvider     # Phase 2：验外部 Token → 查/同步 DB

统一出口：LoginUser { userId, tenantId, authorities, dataScope }
```

配置切换：`oa.auth.mode=dev-fixed | external`（`application-{profile}.yml`）

---

## 3. 配置约定（Phase 1）

### 3.1 后端 `application-dev.yml`

```yaml
oa:
  auth:
    mode: dev-fixed
    default-token: dev-token-oa-default    # 与 seed-auth、前端 .env 一致
    # 可选：多用户 Token 映射见 seed-auth 文档
```

### 3.2 前端 `.env.development`

```env
VITE_USE_MOCK=false
VITE_API_TOKEN=dev-token-oa-default
VITE_TENANT_ID=1
```

### 3.3 请求头（与 API 文档一致）

| Header | 必填 | 说明 |
|--------|------|------|
| `Authorization` | ✅ | `Bearer {token}` |
| `X-Tenant-Id` | ✅ | 租户 ID，与 `sys_tenant.id` 对应 |

---

## 4. seed-auth 约定

| 表 | 规模建议 | 用途 |
|----|---------|------|
| `sys_tenant` | 2（default + tenant-b） | 跨租户 1504 |
| `sys_user` | 5–8（管理员/组长/运营/财务/分析） | 多角色联调 |
| `sys_role` + 权限关联 | 与 PRD 角色对齐 | `@PreAuthorize` 真实校验 |
| 用户-IP 组数据范围 | 与 `seed-ops` 联动 | BR-006 |

预置 Token 示例（写入 seed 或配置文档，**不得提交生产密钥**）：

| Token | userId | tenantId | 角色 |
|-------|--------|----------|------|
| `dev-token-oa-admin` | 1001 | 1 | 系统管理员 |
| `dev-token-oa-leader` | 1002 | 1 | 运营组长 |
| `dev-token-oa-operator` | 1003 | 1 | 运营人员 |
| `dev-token-oa-tenantb` | 2001 | 2 | 租户 B 管理员 |

---

## 5. 影响范围

### 5.1 受益

- 业务模块（M1–M8）**不依赖**本期实现完整登录，开发并行度提高
- Phase 2 对接外部平台时，**仅需替换 Auth Filter + 用户同步**，业务代码基本不动
- 权限/租户测试与生产行为一致（数据来自 DB）

### 5.2 代价与约束

- Dev Token **仅限 dev/test 环境**；生产必须 `oa.auth.mode=external`
- 须维护 seed-auth 与前端 Token 配置**同步文档**
- Phase 2 前须完成**权限码映射 ADR 补充**（若外部平台权限命名与 `oa:*` 不同）

### 5.3 不受影响

- 三大铁律（选择器、字典、多租户）
- 错误码 1500–1504
- AES 敏感字段加密
- 审计日志 AOP

---

## 6. 验收标准

### Phase 1

- [ ] `DevAuthFilter` + `AuthProvider` 接口就位
- [ ] `seed-auth` 灌入后，换 Token 可切换角色，权限差异可测（403）
- [ ] 换 `X-Tenant-Id` 跨租户访问返回 **1504**
- [ ] 业务代码无硬编码 `userId` / `tenantId`（静态扫描或 Code Review）
- [ ] 前端 `request.ts` 注入 Authorization + X-Tenant-Id

### Phase 2（启动条件）

- [ ] 外部平台 SSO 沙箱就绪
- [ ] `ExternalAuthProvider` 实现 + 用户同步 Job
- [ ] 权限映射表评审通过
- [ ] E2E：外部 Token 登录 → 业务 API 全绿

---

## 7. 相关决策

- **ADR-001**：中间件简化（单体 + MySQL，无 Redis Session）
- **v1.1 计划**：M10 延后 + Seed Data Program（身份数据同属 Seed 体系）
- **Phase 2 并列项**：M10 第三方采集 API、外部平台 SSO（可并行迭代，互不阻塞）

---

*Accepted 后作为后端鉴权实现的唯一架构决策来源；实现细节见工作计划 §3.1。*
