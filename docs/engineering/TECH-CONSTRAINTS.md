# TECH-CONSTRAINTS（技术约束）

> **项目级技术约束**：所有 AI 实现 / 研发活动**必须**遵守
> **版本**：v1.0 | 2026-06-07
> **关联方法论**：`AI驱动产品开发方法论-产品经理指南.md § 5.4.3`
> **关联 PRD**：`完整PRD-v9.1-开发版.md`
> **约束强度**：🔴 硬性（违反 → 阻断合并） / 🟡 软性（违反 → 评审警告）

---

## 1. 技术栈

### 1.1 后端

| 维度 | 选择 | 约束 |
|------|------|------|
| 语言 | Java | 17（最低 11） |
| 框架 | Spring Boot | 3.x |
| 持久化 | MyBatis Plus | 3.5.x |
| 数据库 | MySQL | 8.0+ |
| 数据库连接池 | HikariCP | 默认 |
| 缓存 | ConcurrentHashMap（**不引入 Redis**） | 🔴 |
| 消息队列 | **不引入 RabbitMQ**；用 `@Async` + 本地任务表 | 🔴 |
| 任务调度 | **不引入 XXL-JOB**；用 Spring `@Scheduled` + 任务表 | 🔴 |
| 对象存储 | **不引入 MinIO**；本地文件 + Nginx 静态代理 | 🔴 |
| 状态机 | Spring State Machine | 复杂状态机使用 |
| 工具 | Hutool / Lombok | 推荐 |
| 单元测试 | JUnit 5 + Mockito | 覆盖率 ≥ 80% |
| 集成测试 | Testcontainers (MySQL) | 可选 |

### 1.2 前端

| 维度 | 选择 | 约束 |
|------|------|------|
| 语言 | TypeScript | 必选，禁用 JS |
| 框架 | Vue 3 | Composition API + `<script setup>` |
| UI 库 | Element Plus | 2.x |
| 图表 | ECharts | 5.x |
| 状态管理 | Pinia | 替代 Vuex |
| 路由 | Vue Router | 4.x |
| HTTP | Axios | 推荐封装 |
| 工具 | VueUse | 推荐 |
| 单元测试 | Vitest | - |

---

## 2. 目录结构

### 2.1 后端（Spring Boot）

```
yudao-server/
├── yudao-module-oa/                  # OA 模块（运营数据分析平台）
│   ├── src/main/java/cn/iocoder/yudao/module/oa/
│   │   ├── controller/                # REST 控制器，按业务域分包
│   │   │   ├── ip/                    # FR-M1-001 IP 组
│   │   │   ├── author/                # FR-M1-002 作者
│   │   │   ├── account/               # FR-M1-003
│   │   │   ├── follower/              # FR-M1-004
│   │   │   ├── content/               # FR-M1-005
│   │   │   ├── internal/              # FR-M1-006
│   │   │   └── productivity/          # FR-M1-007
│   │   ├── service/                   # 业务服务
│   │   ├── dal/                       # 数据访问层
│   │   │   ├── dataobject/            # DO（与表对应）
│   │   │   └── mysql/                 # Mapper
│   │   ├── api/                       # 对外 API DTO
│   │   │   ├── dto/                   # Req / Resp
│   │   │   └── enums/                 # 枚举
│   │   ├── enums/                     # 业务枚举
│   │   ├── statemachine/              # 状态机
│   │   └── util/                      # 工具类
│   └── src/main/resources/
│       └── mapper/                    # MyBatis XML
└── yudao-module-system/                # 系统模块（用户/角色/租户/字典）
```

### 2.2 前端（Vue 3）

```
yudao-ui-admin-vue3/
├── src/
│   ├── views/
│   │   └── oa/                        # OA 模块
│   │       ├── ip/                    # P-M1-001
│   │       ├── author/                # P-M1-002
│   │       ├── accountAnalysis/       # P-M1-003
│   │       ├── followerAnalysis/      # P-M1-004
│   │       ├── contentAnalysis/       # P-M1-005
│   │       ├── internalContent/       # P-M1-006
│   │       └── productivity/          # P-M1-007
│   ├── components/                    # 公共组件
│   ├── api/oa/                        # API 封装（与后端路径对应）
│   ├── stores/                        # Pinia
│   └── router/                        # 路由
```

### 2.3 文档

```
docs/
├── product/                           # L1+L2
├── engineering/                       # L3
├── delivery/                          # L4+L5
├── adr/                               # 决策记录
└── README.md                          # 索引
```

---

## 3. 代码规范

### 3.1 命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 类 | 大驼峰 | `IpGroupService` |
| 方法 | 小驼峰，动词开头 | `createIpGroup` |
| 常量 | 大写下划线 | `MAX_NAME_LENGTH` |
| 变量 | 小驼峰 | `ipGroupId` |
| 表名 | 小写下划线，前缀 `oa_` | `oa_ip_group` |
| 字段名 | 小写下划线 | `group_name` |
| API 路径 | kebab-case | `/ip-group/create` |
| 资源集合 | 复数名词 | `/ip-groups` |

### 3.2 注释

- 业务逻辑必须有中文注释（解释"为什么"，非"做了什么"）
- 公共方法需 JavaDoc
- 不允许删除历史注释（用 `@Deprecated` 标注）

### 3.3 错误处理

- 业务异常用 `BusinessException` + 错误码
- 错误码按 1xxx 业务码 + 模块码 + 子码
- 严禁 `catch (Exception e) { log.error(...); }` 后不抛出

### 3.4 日志

| 级别 | 用途 |
|------|------|
| INFO | 业务关键操作（IP 组创建、补录审核等） |
| WARN | 可恢复异常（网络超时重试） |
| ERROR | 不可恢复异常（数据库失败） |

格式：`[时间] [级别] [线程] [类名] - [消息] [上下文]`

---

## 4. 数据规范

### 4.1 必填字段

- 业务表所有字段 `NOT NULL`（除可选字段外）
- 字符串默认 `''` 或 `NULL`（统一规范）
- 时间字段统一 `DATETIME`
- 金额 `DECIMAL(15,2)`

### 4.2 审计字段（必备）

| 字段 | 类型 | 说明 |
|------|------|------|
| `creator` | VARCHAR(64) | 创建人 |
| `create_time` | DATETIME | 创建时间 |
| `updater` | VARCHAR(64) | 更新人 |
| `update_time` | DATETIME | 更新时间 |
| `deleted` | BIT | 逻辑删除（0/1） |
| `tenant_id` | BIGINT | 租户 ID |

> 多租户隔离：所有业务表必须带 `tenant_id`，查询时由 MyBatis 拦截器自动注入 `WHERE tenant_id = ?`

### 4.3 索引规范

- 主键：自增 `BIGINT`
- 外键：加索引
- 状态字段：加索引
- 联合索引：按"等值在前、范围在后"原则
- 命名：`idx_字段1_字段2`、`uk_唯一字段`

---

## 5. 安全规范

### 5.1 鉴权

- 所有 API 走 Spring Security + JWT
- 接口级权限注解 `@PreAuthorize("hasAuthority('oa:ip-group:create')")`
- 权限标识规范：`{模块}:{资源}:{动作}`
  - 例：`oa:ip-group:create`、`oa:author:delete`、`oa:content:import`

### 5.2 数据脱敏

| 字段类型 | 脱敏规则 |
|----------|----------|
| 手机号 | `138****8000` |
| 身份证 | `110101********1234` |
| 姓名 | `张*`（单字）或 `张*三`（双字以上） |
| 银行卡 | `**** **** **** 1234` |
| 邮箱 | `a***@example.com` |

### 5.3 防注入

- 所有 SQL 走 MyBatis 参数化（`#{}`），禁用字符串拼接
- 前端输入做 XSS 过滤（DOMPurify）
- 文件上传：白名单 + 大小限制 + 病毒扫描（如条件允许）

---

## 6. 性能规范

| 指标 | 要求 |
|------|------|
| 列表分页 200 条 | ≤ 1.5s |
| 树形加载 200 节点 | ≤ 500ms |
| 异步导出 1w 行 | ≤ 30s |
| 趋势图渲染 | ≤ 1s |
| API 平均 RT | ≤ 300ms |
| 慢 SQL 阈值 | > 1s 记录告警 |

---

## 7. 国际化（i18n）

- 一期仅支持中文
- 文案放 `zh-CN.json`（前端 i18n）
- 错误信息放 `messages.properties`（后端 i18n）

---

## 8. AI 实现专用条款（🔴 硬性）

> 这一节是**专门约束 AI 实现行为**的条款，违反即视为不合规。

### 8.1 禁止推断

🔴 **未在 PRD / UX / API / STATE / TECH-CONSTRAINTS 中明确写出的功能、字段、接口、按钮、菜单、权限点，AI 一律不得新增。**

🔴 **若规格不清晰，AI 必须停止实现，输出"阻塞问题清单"，不得用行业惯例私自补全。**

🔴 **若用户明确回复"按你的假设 A"，AI 才可执行假设 A，并写入 ADR 文档（`docs/adr/ADR-NNN-描述.md`）。**

### 8.2 文件范围

🔴 **每个 Slice 只能修改该 Slice 涉及的文件。** 跨 Slice 修改需先开 ADR。

🔴 **不得修改以下文件**（除非明确授权）：
- `pom.xml`（除非新增明确规定的依赖）
- `application.yml`（除非新增明确规定的配置）
- 数据库迁移脚本（除非在该 Slice 计划中）
- 公共组件 / 工具类

### 8.3 命名与位置

- 严格遵守第 2 节目录结构
- 严格遵守第 3.1 节命名规范
- 严格遵守第 4 节数据规范

### 8.4 测试

- 每个 Slice 必须有对应单元测试
- 覆盖率 ≥ 80%
- TESTCASES 中 P0 用例必须 100% 跑过

### 8.5 提交

- Commit message 格式：`{type}({scope}): {subject}`
  - `feat(ip): 新增 IP 组树接口`
  - `fix(author): 修复主推号绑定唯一性校验`
  - `docs(m1): 补充 M1 PRD 验收标准`
- Type 限定：`feat / fix / docs / style / refactor / test / chore`
- Scope：`模块英文简称`（如 `ip / author / content / m1`）

---

## 9. 监控与日志

### 9.1 应用监控

- Micrometer + Prometheus
- 关键指标：QPS、RT、错误率、JVM、DB 连接池
- 告警阈值：错误率 > 5%、RT P99 > 2s

### 9.2 业务日志

- 关键操作记录 `oa_audit_log` 表
- 必录操作：
  - 登录/登出
  - 所有 CUD 操作
  - 状态机转移
  - 权限变更
  - 数据导出
  - 数据补录

### 9.3 链路追踪

- Micrometer Tracing + Zipkin
- 采样率：100% 错误请求、10% 正常请求

---

## 10. 部署规范

| 维度 | 选择 |
|------|------|
| 部署方式 | Docker + Docker Compose（一期）/ K8s（二期） |
| JDK 镜像 | `eclipse-temurin:17-jre` |
| 前端构建 | Nginx 静态部署 |
| 数据库 | MySQL 8.0 |
| 反向代理 | Nginx |

### 10.1 环境

| 环境 | 用途 | 数据库 |
|------|------|--------|
| dev | 开发 | dev-db |
| test | 测试 | test-db |
| staging | 预发 | staging-db |
| prod | 生产 | prod-db（主从） |

---

## 11. 文档同步

- 代码变更必须同步更新 `docs/product/` / `docs/engineering/` / `docs/delivery/`
- ADR 新增决策 → 写入 `docs/adr/`
- Open Questions 关闭或延期 → 更新 PRD `## 9. Open Questions`

---

## 12. 闸门清单（开发前必读）

- [ ] 读完本文件
- [ ] 读完当前 Slice 的 PRD/UX/API/STATE 全部文档
- [ ] 确认无"阻塞问题"再开始写代码
- [ ] 严格使用五段式 Prompt 实现（参考 `AI驱动产品开发方法论 § 6.2`）

---

*本文件是项目级"宪法"，与 PRD 冲突时以 PRD 为准（并开 ADR 修订本文件）。*


---

## 全局规范引用

> 本文档遵循 [`GLOBAL-CONVENTIONS.md`](./GLOBAL-CONVENTIONS.md) 中定义的全局规范：
> - 强关联属性 → 强制使用 5 类选择器组件（RealNameSelect / PhoneSelect / SimCardSelect / CompanySelect / AccountSelect），禁用手动输入
> - 枚举属性（方式/状态/类型/平台/阶段）→ 统一从数据字典（`dict_*`）选择，页面只读下拉
> - 跨租户 + 状态校验 → 错误码 1500-1504 统一语义
> - 数据安全 → 敏感字段（身份证/手机/API 密钥）强制脱敏展示，凭证类字段 AES-256 加密存储
> - 详见 [`GLOBAL-CONVENTIONS.md § 2`](./GLOBAL-CONVENTIONS.md) (字典)、[`§ 3`](./GLOBAL-CONVENTIONS.md) (选择器)、[`§ 4`](./GLOBAL-CONVENTIONS.md) (错误码)

