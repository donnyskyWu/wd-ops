# AI 实现方法论规范

> **版本**：v1.0 | 2026-06-07
> **关联**：[`AI驱动产品开发方法论-产品经理指南.md`](../../AI驱动产品开发方法论-产品经理指南.md) § 6 · [`PHASE-DEV-METHOD.md`](./PHASE-DEV-METHOD.md)
> **目的**：确保 AI 实现质量，避免自由发挥
> **核心约束**：🔴 严格按文档实现，禁止推断

---

## 1. 五段式 Prompt（标准模板）

每个 Slice 实施时，**必须**按以下 5 段结构给 AI。

### 第 1 段：上下文（Context）

```markdown
# 上下文

你是资深 Java 工程师，负责 `yudao-module-oa` 子模块的开发。

## 技术栈
- 语言：Java 17
- 框架：Spring Boot 3.x
- ORM：MyBatis Plus 3.5.x
- 数据库：MySQL 8.0+
- 状态机：Spring State Machine
- 测试：JUnit 5 + Mockito + Testcontainers

## 必读文档（5 份）
1. [`docs/engineering/TECH-CONSTRAINTS.md`](./TECH-CONSTRAINTS.md) - 前后端技术栈
2. [`docs/engineering/GLOBAL-CONVENTIONS.md`](./GLOBAL-CONVENTIONS.md) - 5 大铁律
3. [`docs/product/PRD-M{N}.md`](../product/PRD-M{N}-{name}.md) - 需求
4. [`docs/engineering/API-M{N}.md`](./API-M{N}-{name}.md) - 接口
5. [`docs/engineering/STATE-M{N}.md`](./STATE-M{N}-{name}.md) - 状态机
6. [`docs/delivery/SLICES-M{N}.md`](../delivery/SLICES-M{N}-{name}.md) - 切片
7. [`docs/delivery/TESTCASES-M{N}.md`](../delivery/TESTCASES-M{N}-{name}.md) - 测试
```

### 第 2 段：任务定义（Task）

```markdown
# 任务

## Slice 信息
- Slice ID: S-M{N}-{NN}
- 标题: [Slice 标题]
- 估时: [N] 人天

## 关联 AC
- AC ID: AC-M{N}-{NN}
- 标题: [AC 标题]
- 文档位置: PRD-M{N}.md § [章节号]

## 实现范围
- 后端: [DO/DTO/Service/Controller/Mapper/StateMachine]
- 前端: [Page/Form/Table/Components]
- DDL: [新增/修改表]
- 测试: [F+P+E 用例]
```

### 第 3 段：约束清单（Constraints）

```markdown
# 🔴 硬性约束（违反 → 阻断合并）

## 5 大铁律
1. **强关联选择器**：5 类字段（realnameId/phoneId/simCardId/companyId/accountId）
   禁止手动输入，前端必须用 <XxxSelect /> 组件，后端必须校验（错误码 1501/1504）
2. **数据字典**：所有枚举字段（方式/状态/类型/平台/阶段）
   必须用 @InDict("dict_xxx") 注解，错误码 1503
3. **多租户隔离**：所有业务表带 tenant_id，查询自动注入 WHERE tenant_id = ?
4. **AES-256 加密**：敏感字段（idCard/phone/cookie/apiKey/password）落库前加密
5. **错误码统一**：业务异常用 1500-1504，禁止 RuntimeException

## 5 个禁止
- ❌ 禁止修改 pom.xml（除非 Slice 明确说明新增依赖）
- ❌ 禁止修改 application.yml（除非 Slice 明确说明）
- ❌ 禁止修改公共组件 / 工具类
- ❌ 禁止修改数据库迁移脚本（除非 Slice 计划中）
- ❌ 禁止超出本 Slice 范围修改其他文件
```

### 第 4 段：实现要求（Deliverables）

```markdown
# 输出要求

## 必交产物
1. **Java 代码**（完整可编译）
   - DO 类（继承 BaseDO，含 tenant_id）
   - DTO 类（@InDict 注解、@NotNull 等校验）
   - Service 接口 + 实现
   - Controller（@PreAuthorize 权限）
   - Mapper 接口 + XML（如需）
   - 状态机配置（如涉及）

2. **数据库 DDL**
   - CREATE TABLE（含索引、审计字段、tenant_id）
   - 唯一索引
   - 状态字段索引

3. **单元测试**（覆盖率 ≥ 80%）
   - F 用例：正常路径
   - P 用例：权限
   - E 用例：异常

4. **审计日志点**
   - 所有 CUD 操作

## 代码风格
- 类：大驼峰
- 方法：小驼峰，动词开头
- 表名：oa_ 前缀
- API 路径：kebab-case
- 中文注释：解释"为什么"，不解释"做了什么"
```

### 第 5 段：自检清单（Self-Check）

```markdown
# 自检清单（提交前必过）

## 一致性
- [ ] 字段名与 API 文档 100% 一致
- [ ] 字典使用与 GLOBAL-CONVENTIONS 一致
- [ ] 错误码使用与 § 5.4 一致

## 5 大铁律
- [ ] 所有强关联字段用选择器（前端）+ 校验（后端）
- [ ] 所有字典字段有 @InDict
- [ ] 所有表带 tenant_id，查询带 WHERE
- [ ] 敏感字段 AES-256 加密
- [ ] 业务异常用 1500-1504

## 9 项检查
- [ ] 中文注释解释"为什么"
- [ ] 单元测试覆盖率 ≥ 80%
- [ ] 状态机转移完整
- [ ] 审计日志记录 CUD
- [ ] 权限标识符合 `oa:{resource}:{action}`
- [ ] 未修改 pom.xml（除非明确）
- [ ] 未修改 application.yml（除非明确）
- [ ] 未修改公共组件（除非 ADR 授权）
- [ ] Commit message 格式 `feat(scope): subject`

## 测试
- [ ] F+P+E 用例至少各 3 条
- [ ] 覆盖率 ≥ 80%
- [ ] P0 用例 100% 通过
```

---

## 2. 阻塞问题清单（Blocker List）

### 2.1 何时输出

遇到以下情况**必须**输出阻塞问题清单：

1. 文档未定义字段的默认值
2. 文档未定义业务规则的边界
3. 文档未定义状态机的入口事件
4. 文档未定义选择器的联动逻辑
5. 文档未定义加密字段的具体范围

### 2.2 格式

```markdown
## 阻塞问题清单 - [Slice ID]

> 实施前必须澄清以下问题，否则按"不推断"原则暂停

| # | 文档 | 问题 | 影响范围 | 我的建议 |
|---|------|------|---------|---------|
| 1 | API-M4 § 5.2 | `forceReplace` 字段未明确默认值 | 创建/更新 | 建议 false |
| 2 | STATE-M4 § 5 | BOUND/UNBOUND 状态机事件未命名 | 状态机配置 | 建议 BOUND/UNBOUND |
| 3 | CHECKLIST-M4 § 10 | 5 选择器联动测试清单缺失 | 验证步骤 | 建议按 § 10.1-10.5 补充 |
| 4 | TESTCASES-M4 | 缺"强制替换-无 reason"用例 | 测试覆盖 | 建议新增 TC-M4-E-008 |

**等待**：
- [ ] 产品确认
- [ ] 更新对应文档
- [ ] 继续实现
```

### 2.3 处理流程

```
AI 实施 → 发现歧义 → 输出阻塞清单
                          ↓
                产品/技术 review
                          ↓
              澄清 + 更新文档
                          ↓
                    继续实施
```

---

## 3. AI 提示词模板库

### 3.1 创建 DO 类

```markdown
请为 oa_realname 表创建 RealnameDO 类：

# 表结构
[DDL]

# 字段说明
[字段含义]

# 要求
1. 继承 BaseDO（含 creator/create_time/updater/update_time/deleted/tenant_id）
2. @TableName("oa_realname")
3. Lombok @Data + @Builder
4. 敏感字段用 xxxEncrypted 后缀
5. 添加字段注释
```

### 3.2 创建 DTO 类

```markdown
请为 [实体] 创建 [动作]Req DTO：

# API 文档
[API 章节]

# 要求
1. @Data + 链式 setter
2. 必填字段 @NotBlank / @NotNull
3. 字典字段 @InDict("dict_xxx")
4. 强关联字段命名为 xxxId
5. 金额字段 @DecimalMin / @DecimalMax
6. 枚举字段注释合法值
```

### 3.3 创建 Service 实现

```markdown
请实现 [Service] 类的 [方法]：

# 业务规则
- BR-X-001: [规则]
- BR-X-002: [规则]

# 校验
1. 字典校验（@InDict）
2. 强关联校验（错误码 1501/1504）
3. 业务校验（如唯一性）

# 状态机（如适用）
- 触发事件：[EVENT]
- 失败回滚

# 审计日志
- 操作：CREATE / UPDATE / DELETE
- 记录：before / after

# 异常处理
- BusinessException([CODE], [MSG])
```

### 3.4 创建 Controller

```markdown
请为 [实体] 创建 Controller：

# API 路径
[路径]

# 权限标识
oa:[resource]:[action]

# 限流（如适用）
[规则]

# 接口
- POST /create
- PUT /update
- DELETE /{id}
- GET /{id}
- GET /list

# 响应
R<T> 格式
```

### 3.5 创建单元测试

```markdown
请为 [Service] 类创建单元测试：

# 测试方法
## TC-F-M{N}-{NN} 正常路径
- given: [前置]
- when: [动作]
- then: [预期]

## TC-P-M{N}-{NN} 权限
- given: [越权场景]
- when: [动作]
- then: [错误码]

## TC-E-M{N}-{NN} 异常
- given: [异常场景]
- when: [动作]
- then: [错误码]

# 要求
- JUnit 5 + Mockito
- 覆盖率 ≥ 80%
- 至少 1 个 TC-F、1 个 TC-P、1 个 TC-E
```

---

## 4. 反模式（Anti-Patterns）

### 🔴 绝对禁止

| 反模式 | 正确做法 |
|--------|---------|
| 用行业惯例推断默认值 | 输出阻塞清单，等澄清 |
| 直接 `throw new RuntimeException()` | 用 `BusinessException(CODE, MSG)` |
| 把身份证明文落库 | AES-256 加密后落库 |
| 字典字段用 `String` 接收 | `@InDict("dict_xxx")` |
| 强关联字段用 input 框 | 用 `<XxxSelect />` 组件 |
| 跨租户查询不带 `tenant_id` | 拦截器自动注入 |
| 修改 `pom.xml` 加新依赖 | 必须先开 ADR |
| 修改公共组件 | 必须先开 ADR |
| 跳过测试 | 必须有 F+P+E |
| Commit 信息 `update` | `feat(ip-group): 新增 IP 组树接口` |

### 🟡 应避免

| 反模式 | 正确做法 |
|--------|---------|
| 复制粘贴代码 | 抽取公共方法 |
| 硬编码字符串 | 用常量/枚举 |
| 业务代码写在 Controller | 写在 Service |
| 一个 Service 超过 1000 行 | 拆分子 Service |
| Mapper 返回 Map | 定义 VO 类 |
| SQL 字符串拼接 | `#{}` 参数化 |
| 注释 `// 设置字段` | 注释解释"为什么" |

---

## 5. 提示词最佳实践

### 5.1 提供充足上下文

```markdown
# ❌ 不好的提示词
"实现实名人管理"

# ✅ 好的提示词
"实现 S-M4-01 实名人管理 Slice：
- DO：RealnameDO（含 idCardEncrypted 等加密字段）
- DTO：RealnameCreateReq（含 @InDict 校验）
- Service：createRealname / disableRealname / deleteRealname
- 文档：API-M4 § 2.1-2.5
- 状态机：ENABLED → DISABLED
- 错误码：1501/1502
- 测试：F+P+E 各 3 条"
```

### 5.2 明确边界

```markdown
# ❌ 模糊边界
"实现账号管理"

# ✅ 明确边界
"只实现账号创建的 Service 层和 Controller 层：
- 不实现 Mapper（用现有的 BaseMapper）
- 不实现 DDL（用现有的 oa_account 表）
- 不实现前端
- 不修改任何公共组件"
```

### 5.3 引用文档原文

```markdown
# ❌ 转述
"实名人管理要支持启用和停用"

# ✅ 引用原文
"按 STATE-M4 § 2 实名人状态机：
[ENABLED] --(disable)--> [DISABLED]
[DISABLED] --(enable)--> [ENABLED]
...（复制 STATE 文档原文）"
```

---

## 6. 审查 AI 输出

### 6.1 必查清单

每次 AI 输出代码后，**人工审查**：

| # | 审查项 | 方法 |
|---|--------|------|
| 1 | 字段名是否与 API 一致 | diff |
| 2 | 是否用了 @InDict | grep |
| 3 | 是否用了选择器 | grep |
| 4 | 是否带 tenant_id | grep |
| 5 | 敏感字段是否加密 | grep |
| 6 | 错误码是否 1500-1504 | grep |
| 7 | 是否有审计日志 | grep |
| 8 | 是否有单元测试 | 文件 |
| 9 | 覆盖率 ≥ 80% | JaCoCo |
| 10 | 状态机是否完整 | diff |

### 6.2 常见 AI 错误

| 错误 | 示例 | 修正 |
|------|------|------|
| 字段名拼写错误 | `realName` vs `realname` | 严格按 API 文档 |
| 字典写错 | `dict_gender` 写成 `dict_sex` | 用 GLOBAL-CONVENTIONS |
| 错误码 1503 写成 503 | 误以为是 HTTP 码 | 严格 1500-1504 |
| 加密字段命名 | `idCard` 落库 | 改为 `idCardEncrypted` |
| 缺失 tenant_id | 直接 `selectById` | 加 `WHERE tenant_id = ?` |
| 选择器误用 | `<el-input v-model="realnameId">` | 用 `<RealNameSelect />` |

---

## 7. 总结

| 原则 | 说明 |
|------|------|
| **文档先行** | 不读文档不写代码 |
| **五段式 Prompt** | 上下文 / 任务 / 约束 / 输出 / 自检 |
| **阻塞即停** | 不推断，输出阻塞清单 |
| **5 铁律** | 强关联 / 字典 / 多租户 / 加密 / 错误码 |
| **5 禁止** | 不改公共文件（除 ADR）|
| **F+P+E** | 正常 / 权限 / 异常 三类用例 |
| **覆盖率 ≥ 80%** | JaCoCo 验证 |

**核心心法**：把 AI 当作**严格执行的初级工程师**，给他清晰、完整、无歧义的指令。不要让他"创造"，只让他"翻译"。
