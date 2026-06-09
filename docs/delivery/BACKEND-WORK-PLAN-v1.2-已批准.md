# 后端模块开发工作计划 v1.2（已批准）

> **文档性质**：工作计划 · **已批准生效**
> **版本**：v1.2（v1.1 + **模拟鉴权策略** + **seed-auth** + **Phase 2 外部平台 SSO**）
> **批准时间**：2026-06-09
> **生成时间**：2026-06-09
> **依据**：[`完整PRD-v9.1-开发版.md`](../../完整PRD-v9.1-开发版.md) · [`ADR-003`](../adr/ADR-003-模拟鉴权与外部平台SSO对接.md) · [`docs/delivery/`](../delivery/)
> **当前前端状态**：Mock 模式约 92% 完成；161 条 Playwright 已通过

---

## 0. 执行摘要（请先读这一段）

| 项 | 结论 |
|----|------|
| **本期范围** | **M0–M9 + M8**（10 个模块）；**M10 数据采集本期不做** |
| **M10 替代** | 分阶段 **Seed Data** 灌库（分析/大屏/报表） |
| **鉴权策略** | **模拟身份来源 + 库内真实用户/租户/权限**（见 §3.1、[ADR-003](../adr/ADR-003-模拟鉴权与外部平台SSO对接.md)） |
| **推荐总工期** | **11 周**（S0–S7） |
| **开发顺序** | S0 基建+Auth → S1–S2 基座（M4∥M8∥M9+seed-auth）→ S3–S5 核心 → S6 扩展 → S7 首页+上线 |
| **唯一硬门槛** | S1 末：**M4 五选择器 100%** |
| **横切任务** | **Seed Data Program** + **DevAuthFilter** |

**一句话排期**：

> S0 基建+Auth+Seed框架 → S1–S2 基座（M4∥M8∥M9+**seed-auth**）→ S3–S5 核心（每阶段末灌数）→ S6 扩展（M5∥M6∥M7+全量种子）→ S7 M0+联调+上线  
> **Phase 2（后期）**：M10 第三方采集 API · **外部平台 SSO/用户同步**

---

## 0.1 版本变更记录

| 版本 | 变更 |
|------|------|
| v1.0 | 初版：11 模块依赖 + 12 周排期 |
| v1.1 | M10 延后；Seed Data Program；11 周 |
| **v1.2** | **模拟鉴权（Dev Fixed Token）**；**seed-auth**；M9 收窄为「权限消费」；Phase 2 外部 SSO；[ADR-003](../adr/ADR-003-模拟鉴权与外部平台SSO对接.md) · **2026-06-09 已批准** |

---

## 0.2 两大 Phase 2 并列项（本期均不做）

| Phase 2 项 | 本期替代 | 后期对接 |
|-----------|---------|---------|
| **M10 数据采集** | Seed 时序数据 | 第三方平台 API |
| **外部平台 SSO** | DevAuthFilter + seed-auth | ExternalAuthProvider + 用户同步 |

---

## 1. 模块全景与定位

| ID | 模块 | 本期 | 迭代 | 说明 |
|----|------|------|------|------|
| **M9** | 系统管理 | ✅ 收窄 | S1–S2 | **身份表+权限消费**；不含登录页/SSO |
| **M8** | 配置管理 | ✅ | S1–S2 | 采集配置仅 CRUD 预留 |
| **M4** | 账号管理 | ✅ | S1 | P0 枢纽 |
| **M1–M3** | 运营/内容/绩效 | ✅ | S3–S5 | |
| **M5–M7** | 财务/分析/监测 | ✅ | S6 | 依赖 seed-analytics |
| **M0** | 首页 | ✅ | S7 | |
| **M10** | 数据采集 | ❌ | Phase 2 | |
| **—** | **Auth** | ✅ 横切 | S0 起 | DevAuthFilter + AuthProvider |
| **—** | **Seed Data** | ✅ 横切 | S0 起 | 含 **seed-auth** |

---

## 2. 模块依赖关系

### 2.1 分层依赖（含 Auth 横切）

```
┌─────────────────────────────────────────────────────────────────────────┐
│ L4  展示聚合     M0 首页                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│ L3  分析扩展     M5 │ M6 │ M7          数据：L2 + Seed（替代 M10）        │
├─────────────────────────────────────────────────────────────────────────┤
│ L2  核心业务     M1 → M2 → M3                                           │
├─────────────────────────────────────────────────────────────────────────┤
│ L1  资产中心     M4 账号管理                                             │
├─────────────────────────────────────────────────────────────────────────┤
│ L0  横纵基座     M8 配置 │ M9 系统（用户/角色/租户/权限 表结构）           │
├─────────────────────────────────────────────────────────────────────────┤
│ 横切 ①          ★ AuthProvider → SecurityContext（Dev / External 可切换）│
│ 横切 ②          ★ Seed Data（含 seed-auth 身份三件套）                   │
└─────────────────────────────────────────────────────────────────────────┘

  Phase 2: M10 采集 API  ·  ExternalPlatformAuthFilter + 用户同步
```

### 2.2 M10 延后后的数据替代（摘要）

| 模块 | v1.2 数据来源 |
|------|-------------|
| M6 报表/大屏 | `seed-analytics`（90 天时序） |
| M7 监测 | `seed-monitor` |
| M3/M5 订单 | `seed-perf` / `oa_order` |
| M0/M1 分析 | 业务表 + 种子时序 |

（完整表见 v1.1 §2.2，此处不重复。）

---

## 3. 分阶段开发计划

### 3.1 ★ 鉴权策略（Auth Program）— v1.2 新增

> **决策文档**：[ADR-003 模拟鉴权与外部平台 SSO 对接](../adr/ADR-003-模拟鉴权与外部平台SSO对接.md)

#### 3.1.1 原则

| 模拟什么 | 不模拟什么 |
|---------|-----------|
| Token **从哪来**（Dev 固定 Bearer） | 权限校验逻辑（`@PreAuthorize` 必须生效） |
| 登录页 / SSO 回调 | 租户隔离（`tenant_id` + 拦截器 → 1504） |
| 外部平台 Token 验签 | 用户/角色/权限 **必须从 DB 读取** |

#### 3.1.2 架构

```text
Phase 1                          Phase 2
DevAuthFilter                    ExternalPlatformAuthFilter
       ↓                                    ↓
              AuthProvider（接口，可配置切换）
                         ↓
              LoginUser → SecurityContext
              (userId, tenantId, authorities, dataScope)
                         ↓
              M1–M8 业务 Service（本期起即按此编写，不再改动）
```

#### 3.1.3 S0 交付（Auth 基础设施）

| 工作项 | 交付物 |
|--------|--------|
| AuthProvider 接口 | `com.xxx.oa.framework.auth.AuthProvider` |
| Dev 实现 | `DevAuthProvider` + `DevAuthFilter` |
| 配置 | `application-dev.yml` → `oa.auth.mode=dev-fixed` |
| 统一上下文 | `LoginUser` / `TenantContextHolder`（业务只读此出口） |
| 前端联调 | `request.ts` 注入 `Authorization` + `X-Tenant-Id` |
| 静态门禁 | 禁止业务代码硬编码 userId（Review 清单） |

#### 3.1.4 seed-auth（S2 末，与 M9 验收同步）

| 表 | 规模 | 用途 |
|----|------|------|
| `sys_tenant` | 2 | 跨租户 1504 |
| `sys_user` | 5–8 | 多角色联调 |
| `sys_role` + 权限关联 | PRD 角色对齐 | 403 / 菜单可见性 |
| 用户-IP 组范围 | 与 seed-ops 联动 | BR-006 四级过滤 |

**预置 Dev Token**（详见 ADR-003 §4）：

| Token | 角色示意 |
|-------|---------|
| `dev-token-oa-admin` | 系统管理员 · tenant=1 |
| `dev-token-oa-leader` | 运营组长 · tenant=1 |
| `dev-token-oa-operator` | 运营人员 · tenant=1 |
| `dev-token-oa-tenantb` | 租户 B 管理员 · tenant=2 |

#### 3.1.5 M9 本期范围（收窄）

| ✅ 本期做 | ❌ 本期不做（Phase 2） |
|----------|---------------------|
| `sys_user/role/tenant` 表结构与 CRUD API | 登录页 / 注册 / 找回密码 |
| 权限点 `oa:{resource}:{action}` 注册 | 外部 SSO 回调 |
| 操作/登录日志表写入 | 外部平台用户同步 Job |
| 角色-权限-用户关联查询 | 外部 JWT 验签 |
| seed-auth 灌数 | 权限码双向映射（预留 ADR 附录） |

#### 3.1.6 Auth 验收用例

| ID | 场景 | 预期 |
|----|------|------|
| AUTH-001 | Bearer `dev-token-oa-admin` + `X-Tenant-Id:1` | 200，拥有管理员权限集 |
| AUTH-002 | 同用户访问无权限接口 | 403 |
| AUTH-003 | `dev-token-oa-tenantb` + `X-Tenant-Id:2` 访问 tenant=1 数据 | 1504 |
| AUTH-004 | 无 Authorization | 401 |
| AUTH-005 | 换 operator Token，IP 组数据范围变窄 | 列表条数符合 BR-006 |

---

### 3.0 ★ 测试数据填充计划（Seed Data Program）

> 与 v1.1 相同，**v1.2 在种子包清单中增加 seed-auth**。

#### 3.0.1 种子包清单

| 包 ID | 灌入时机 | 主要表/数据 | 消费模块 |
|-------|---------|------------|---------|
| **seed-base** | S0 | `init_dict.sql` | 全模块 |
| **seed-auth** | **S2 末（M9 验收后）** | `sys_tenant` / `sys_user` / `sys_role` / 权限关联 / 数据范围 | **全模块鉴权+BR-006** |
| **seed-assets** | S1 末 | 公司/实名人/手机/卡/账号 | M1–M7 |
| **seed-ops** | S3 末 | IP 组/作者/粉丝日表 | M0/M1/M6 |
| **seed-content** | S4 末 | SOP/任务/内容 | M1/M3/M6/M7 |
| **seed-perf** | S5 末 | 绩效/订单/归因 | M3/M5/M6 |
| **seed-analytics** | **S6 前硬门槛** | 90 天时序/报表/大屏 | M6/M0 |
| **seed-monitor** | S6 前 | 外部账号/告警 | M7/M0 |
| **seed-wechat** / **seed-industry** | S6 前 | 微信/行业 | M6/M7 |

#### 3.0.2–3.0.3 规范与阶段验收

同 v1.1；**S2 末追加**：
- [ ] `seed-auth` 灌入；AUTH-001～005 通过
- [ ] 前端 `.env` Token 与 seed 用户一致

---

### 阶段 0 · S0（第 1 周）

| 工作项 | 交付物 |
|--------|--------|
| 工程 + Flyway | `yudao-module-oa` |
| 多租户拦截器 | `tenant_id` 自动注入（与 Auth 配合） |
| AES / 错误码 / 审计 | 同前 |
| 字典基线 | `db/init_dict.sql` |
| **Auth 骨架** | `AuthProvider` + `DevAuthFilter` + `application-dev.yml` |
| 种子框架 | `db/seed/` + `SeedVerificationIT` |
| HelloWorld | 带 **Dev Token** 的一条 API 链路 |

**S0 退出**：
- [ ] HelloWorld + Dev Token 200
- [ ] 无 Token → 401
- [ ] `@InDict` → 1503；跨租户 → 1504（可先空库测拦截器）

---

### 阶段 1 · S1–S2（第 2–4 周）

- **S1**：M4 + `seed-assets`（同 v1.1）
- **S2**：M8 + **M9（收窄）** + **`seed-auth`**

**阶段 1 硬门槛**：
- [ ] M4 五选择器 100%
- [ ] **AUTH-001～005 通过**
- [ ] seed-auth 与前端 Token 配置文档同步

---

### 阶段 2 · S3–S5（第 5–8 周）

同 v1.1；联调时 **`VITE_USE_MOCK=false` + Dev Token**（非 Mock 用户对象）。

| 迭代 | 模块 | 种子包 |
|------|------|--------|
| S3 | M1 | seed-ops |
| S4 | M2 | seed-content |
| S5 | M3 | seed-perf |

---

### 阶段 3 · S6（第 9–10 周）

同 v1.1（M5∥M6∥M7 + seed-analytics 硬门槛）。

---

### 阶段 4 · S7（第 11 周）

| 工作项 | 说明 |
|--------|------|
| M0 首页 | 聚合读 |
| Auth 加固 | tenant=2 全链路 1504 回归 |
| 9 条 E2E | §5.3 |
| 压测 + UAT | 同前 |

**本期不做**：M10 API · 登录页 · 外部 SSO

---

## 4. 各模块工作计划（节选）

### M9 系统管理（S2 · 收窄版）

| 项 | 内容 |
|----|------|
| API | 用户/角色/租户 CRUD；权限点查询；日志 |
| 不做 | 登录/SSO/同步（Phase 2） |
| 前置 | S0 Auth 骨架 |
| 验收 | CHECKLIST-M9 · TESTCASES-M9 · **AUTH-001～005** |
| ADR | [ADR-003](../adr/ADR-003-模拟鉴权与外部平台SSO对接.md) |

### M10 — ❌ 本期不做（Phase 2-A：采集）

同 v1.1 §4。

### Phase 2-B：外部平台 SSO（本期不做）

| 项 | 内容 |
|----|------|
| 触发 | 外部平台 SSO 沙箱就绪 |
| 实现 | `ExternalAuthProvider` + 用户/租户同步 + 权限映射 |
| 不变 | 业务 Service、`@PreAuthorize`、表结构 |
| 估时 | 1–2 周（可与 M10 并行） |
| 文档 | ADR-003 §Phase 2 验收 |

---

## 5. 验收测试方法

### 5.3 S7 九条 E2E

1. **Dev Token 注入**（替代登录页）→ 切 `X-Tenant-Id` → 建公司 → …（后续同 v1.1）
2. 建 IP 组 → 建作者
3. SOP → 三审 → 发布
4. 考核 → 得分
5. 归因 → ROI
6. 报表 + 大屏非空
7. 告警 → 消息
8. **tenant-b Token + 跨租户数据 → 1504**
9. 首页 KPI + 待办

### 5.5 Seed 专项（含 Auth）

| ID | 预期 |
|----|------|
| SEED-001～005 | 同 v1.1 |
| **AUTH-001～005** | §3.1.6 |

### 5.6 前端联调配置

| 阶段 | 模式 |
|------|------|
| S0–S2 | Mock 或 Dev Token 混用 |
| S3 起 | `VITE_USE_MOCK=false` + `VITE_API_TOKEN` + `VITE_TENANT_ID` |
| Phase 2 | 外部 SSO 发 Token，**Header 格式不变** |

---

## 6. 资源与排期

### 6.2 甘特（11 周）

```
周次  1    2    3    4    5    6    7    8    9   10   11
S0   ████  Auth+Seed框架
S1        ████████  M4 + seed-assets
S2             ████  M8+M9+seed-auth ★
S3                  ████████  M1 + seed-ops
S4                       ████  M2 + seed-content
S5                            ████  M3 + seed-perf
S6                                 ████████  M5∥M6∥M7
S7                                           ████  M0+E2E

Phase 2-A: M10 采集    Phase 2-B: 外部 SSO
```

---

## 7. 风险与假设

| 风险 | 缓解 |
|------|------|
| Dev Token 泄漏到生产 | `oa.auth.mode` 生产必为 `external`；CI 检查 |
| 业务硬编码 userId | Code Review + AUTH 用例 |
| 外部权限码与 `oa:*` 不一致 | Phase 2 前建映射表 ADR 附录 |
| seed-auth 与前端 Token 不一致 | S2  checklist 强制对表 |

**假设（v1.2 追加）**：
- 外部平台 **租户/用户表结构与本项目一致**，Phase 2 以同步为主而非改表
- 本期 **不做登录 UI**；默认已登录 = Dev Token 已配置

---

## 8. 确认事项（已全部批准）

| # | 确认项 | 结论 | 状态 |
|---|--------|------|------|
| C1 | 工期 11 周 | 同意 | ☑ |
| C2 | 开发顺序 | 同意 | ☑ |
| C3 | M4 五选择器硬门槛 | 同意 | ☑ |
| C9–C12 | M10 延后 + Seed（v1.1） | 同意 | ☑ |
| C13 | 本期模拟鉴权（Dev Token），不做登录/SSO | 同意 | ☑ |
| C14 | 用户/租户/权限走 DB（seed-auth），权限逻辑不 Mock | 同意 | ☑ |
| C15 | Phase 2 仅换 AuthProvider + 用户同步，业务不改 | 同意 | ☑ |
| C16 | ADR-003 作为鉴权 SSOT | 同意 | ☑ |

---

## 9. Phase 2 预告

### 9.1 M10 数据采集（Phase 2-A）

同 v1.1 §9。

### 9.2 外部平台 SSO（Phase 2-B）— v1.2 新增

| 项 | 内容 |
|----|------|
| 范围 | ExternalAuthFilter · 用户/租户/角色同步 · 权限码映射 |
| 不变 | `sys_*` 表结构（一致时）· 业务 API · 请求头格式 |
| 前端 | 外部登录页或 SSO 跳转 |
| 估时 | 1–2 周 |
| 文档 | [ADR-003](../adr/ADR-003-模拟鉴权与外部平台SSO对接.md) |

---

## 10. 附录

| 类型 | 路径 |
|------|------|
| **阶段方法 SSOT** | [`PHASE-DEV-METHOD.md`](../engineering/PHASE-DEV-METHOD.md) |
| **执行进度 SSOT** | [`MASTER-EXECUTION-TRACKER.md`](./MASTER-EXECUTION-TRACKER.md) |
| Gate 报告模板 | [`gates/GATE-报告模板.md`](./gates/GATE-报告模板.md) |
| 鉴权 ADR | [`docs/adr/ADR-003-模拟鉴权与外部平台SSO对接.md`](../adr/ADR-003-模拟鉴权与外部平台SSO对接.md) |
| 模块 API/TESTCASES | `docs/engineering/API-M*` · `docs/delivery/TESTCASES-M*` |
| 前端联调 | [`INTEGRATION-TESTING.md`](./INTEGRATION-TESTING.md) |

---

*版本：v1.2-已批准 · 2026-06-09 确认 · SSOT：本文 + ADR-003*
