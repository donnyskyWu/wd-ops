# 运营数据平台·开发总览（2026-06-10 · 单人全栈版）

> **性质**：本期交付与当前进度 SSOT · 含 3 部分
> 1. **现状进度**：已交付 + 验证 + 遗留
> 2. **总计划执行对照**：v1.0 计划 vs 实际
> 3. **开发方法论 + 自查方法**：可直接复制给新协作者

**最后更新**：2026-06-10 · **执行**：单人全栈 · **任务表**：[TASK-PROGRESS-MASTER](./delivery/TASK-PROGRESS-MASTER.md)

---

## 一、现状进度

### 1.1 阶段 Gate 完成情况（v1.0 计划，8 个 Gate 全绿）

| Gate | 阶段 | 模块 | IT 数量 | 状态 | 报告 |
|------|------|------|---------|------|------|
| GATE-S0 | S0 基建 | Auth · Seed · 字典 | 75 | ✅ | [GATE-S0-20260609.md](./delivery/gates/GATE-S0-报告-20260609.md) |
| GATE-S1 | S1 基座-A | **M4** 账号管理 | 53 + 19 联调 | ✅ | [GATE-S1-20260609.md](./delivery/gates/GATE-S1-报告-20260609.md) |
| GATE-S2 | S2 基座-B | M8 · M9 · Auth | 75 | ✅ | [GATE-S2-20260609.md](./delivery/gates/GATE-S2-报告-20260609.md) |
| GATE-S3 | S3 核心-A | **M1** 运营管理 | 102 | ✅ | [GATE-S3-20260609.md](./delivery/gates/GATE-S3-报告-20260609.md) |
| GATE-S4 | S4 核心-B | **M2** 内容生产 | 114 | ✅ | [GATE-S4-20260609.md](./delivery/gates/GATE-S4-报告-20260609.md) |
| GATE-S5 | S5 核心-C | **M3** 绩效核算 | 123 | ✅ | [GATE-S5-20260609.md](./delivery/gates/GATE-S5-报告-20260609.md) |
| GATE-S6 | S6 扩展 | M5 · M6 · M7 | 137 | ✅ | [GATE-S6-20260609.md](./delivery/gates/GATE-S6-报告-20260609.md) |
| GATE-S7 | S7 上线 | M0 · 全链路 E2E | **151** | ✅ | [GATE-S7-20260609.md](./delivery/gates/GATE-S7-报告-20260609.md) |

**v1.0 计划 8/8 Gate 全部通过，本期 S0–S7 后端交付完成**。

### 1.2 补丁 Gate（v1.0 后，2026-06-09 ~ 10）

| Gate | 触发 | 范围 | 状态 | 报告 |
|------|------|------|------|------|
| **P-GATE-DICT** | 作者添加报错 + 列表/组长非真实数据 | DictController/Service + V27 + DictSelect | ✅ | [P-GATE-DICT-20260609.md](./delivery/gates/P-GATE-DICT-报告-20260609.md) |
| **P-GATE-UNMOCK** | 7 个 selector mock + 4 业务页 mock | 13 文件 | ✅ | [P-GATE-UNMOCK-20260609.md](./delivery/gates/P-GATE-UNMOCK-报告-20260609.md) |

### 1.3 走查切片（S-R2 ~ S-R10，2026-06-09 ~ 10）

走查沉淀方法论 + 暴露大量跨页问题：

| Slice | 页面 | 状态 | 报告 |
|-------|------|------|------|
| **S-R2** | M1 5 个 mock 切真 API | ✅ | [S-R2-20260609.md](./delivery/gates/S-R2-报告-20260609.md) |
| S-R2-Fix | M1 补录 + 内部内容 bug | ✅ | [S-R2-Fix-20260609.md](./delivery/gates/S-R2-Fix-报告-20260609.md) |
| S-R2-Phase2 | dict seed V29 | ✅ | [S-R2-Phase2-20260609.md](./delivery/gates/S-R2-Phase2-报告-20260609.md) |
| **S-R3** | 账号分析走查（沉淀 walkthrough 方法论） | ✅ | [S-R3-20260609.md](./delivery/gates/S-R3-报告-20260609.md) |
| **S-R4** | 作者管理 | ✅ | [S-R4-20260610.md](./delivery/gates/S-R4-报告-20260610.md) |
| **S-R5** | 内部内容分析（6 bug） | ✅ | [S-R5-20260610.md](./delivery/gates/S-R5-报告-20260610.md) |
| **S-R6** | 粉丝分析 + 个微延期 | ✅ | [S-R6-20260610.md](./delivery/gates/S-R6-报告-20260610.md) |
| **S-R7** | 内部内容 Bug4 5 筛选项 | ✅ | [S-R7-20260610.md](./delivery/gates/S-R7-报告-20260610.md) |
| **S-R8** | 作品分析（6 bug 修 5） | ✅ | [S-R8-20260610.md](./delivery/gates/S-R8-报告-20260610.md) |
| **S-R9** | 效率分析（9 bug 全量修） | ✅ | [S-R9-20260610.md](./delivery/gates/S-R9-报告-20260610.md) |
| **S-R10** | 效率分析复测（S-R9 漏改 2 项） | ✅ | [S-R10-20260610.md](./delivery/gates/S-R10-报告-20260610.md) |

**累计发现并修复**：**50+ 个真 bug**（跨模块 9 个、mock 数据 13 处、enum 不对齐 6+、schema drift 4 处、KPI 聚合 8 处、分页契约 6 处、UI render bug 5 处）

### 1.4 测试 & 质量指标

| 指标 | 数值 | 验证方式 |
|------|------|----------|
| **后端 IT 总量** | **216/216** ✅ 0 失败 | `mvn test` |
| **新增 IT（S-R9+10）** | M1EfficiencyKpiIT 11/11 | `mvn test -Dtest=M1EfficiencyKpiIT` |
| **dict 测试** | dict_position 5 条 | `iwr /admin-api/oa/dict/data?type=dict_position` |
| **前端 console** | 无 ReferenceError / Unhandled error | `agent-browser console` |
| **Flyway 迁移** | V1 → V30 | `mvn flyway:migrate` |
| **数据库** | H2 (IT) + MySQL (dev) | `application-test.yml` |
| **种子数据** | sys_user 5 / ip_group 5 / task 18 / content 26 / order 12 | `SeedVerificationIT` |

### 1.5 当前遗留 / 待办（S-R19 自查后 2026-06-10 19:50 更新）

| ID | 任务 | 状态 | 影响 | 备注 |
|----|------|------|------|------|
| L1 | ~~导出格式 xlsx（spec）vs csv（实现）~~ | ✅ 已对齐 | S-R19 自查：`excel-export.ts` 用 XLSX 库生成 .xlsx，PRD 未明确格式 | 原 P1 backlog 误判，已 close |
| L2 | ~~30+ 文件 enum literal 清理~~ | ✅ 设计内 | S-R19 自查：`enum-alias.ts` 工具已规范高风险面，剩余为类型声明/mock 历史值（合理） | 永久方案非 P1 backlog |
| L3 | ~~ADR-008 oa_content 加 author_id~~ | ✅ 全闭环 | S-R21-Mike + S-R21-Donny 方案 A | author_id 聚合 · 效率页真 KPI |
| L4 | ~~SOP `pageNum` 不匹配~~ | ✅ 已修 | S-R11 走查已修（pageNo→pageNum） | OVERVIEW 老状态，已 close |
| L5 | seed V18-V23 CJK 截断（PowerShell 终端显示） | 0（DB 存的是真中文） | 浏览器显示正常 | 仅显示问题 |
| L6 | **4 个 error code 冲突修复** | ✅ 已修 | S-R19 自查发现：1500/2006/2008/2009 重复 | 修 OaErrorCodes + 11 IT，250/250 全绿 |

---

## 二、总计划执行对照（v1.0 计划 vs 实际）

### 2.1 计划范围（[MASTER-EXECUTION-TRACKER.md](./delivery/MASTER-EXECUTION-TRACKER.md) v1.0）

```
S0 基建 → S1 M4 → S2 M8/M9 → S3 M1 → S4 M2 → S5 M3 → S6 M5/M6/M7 → S7 M0
8/8 Gate · 全过 ✅
```

### 2.2 实际执行（含补丁 + 走查）

```
【v1.0 计划】
S0 → S1 → S2 → S3 → S4 → S5 → S6 → S7        8/8 Gate 全绿

【v1.0 后补丁】（2026-06-09）
P-GATE-DICT  →  P-GATE-UNMOCK                  2/2 补丁 Gate

【v1.0 后走查】（2026-06-09~10）
S-R2 → S-R2-Fix → S-R2-Phase2                 字典 + mock 切真
→ S-R3（账号分析）→ S-R4（作者）→ S-R5（内部内容）
→ S-R6（粉丝）→ S-R6-TODO4（个微）→ S-R7（内部内容 5 筛）
→ S-R8（作品）→ S-R9（效率）→ S-R10（效率复测）   11 个走查切片

【待走查】
SOP / IP 组 / 账号分析详情 / 知识库
```

### 2.3 模块完成度

| 模块 | v1.0 计划 | 走查后实际 | 备注 |
|------|-----------|------------|------|
| M0 首页 | ✅ 9/9 E2E | ✅ | 无后续走查 |
| M1 运营管理 | ✅ 102 IT | ✅ +50+ bug 修复 | S-R3~R10 走查覆盖 7 页 |
| M2 内容生产 | ✅ 114 IT | ✅ | SOP 待走查 |
| M3 绩效核算 | ✅ 123 IT | ✅ | 无后续走查 |
| M4 账号管理 | ✅ 53+19 | ✅ | S-R3 走查联动 |
| M5/M6/M7 财务/分析/监测 | ✅ 137 | ✅ | S-R6 走查 1 页 |
| M8 配置 | ✅ 75 | ✅ | |
| M9 系统 | ✅ 75 | ✅ | |

### 2.4 一句话总结

> **v1.0 计划 8/8 Gate 全过 + 11 个走查切片摸出 50+ 真 bug + 2 个补丁 Gate。** 本期 S0–S7 后端交付完成，进入"上线前补强 + 持续走查"阶段。

---

## 三、开发方法论 + 自查方法（可直接复制给新协作者）

### 3.1 已沉淀的方法论文档（SSOT）

| 文档 | 性质 | 何时用 |
|------|------|--------|
| [PHASE-DEV-METHOD.md](./engineering/PHASE-DEV-METHOD.md) | AI 驱动 + 阶段 Gate 总体方法 | **写代码前必读** |
| [AI-IMPL-GUIDE.md](./engineering/AI-IMPL-GUIDE.md) | 五段式 Prompt 模板 | **每片实现前必读** |
| [walkthrough-methodology.mdc](../.cursor/rules/walkthrough-methodology.mdc) | 浏览器 E2E 走查 7 步法 | **走查 1 页前必读** |
| [spec-driven-impl.mdc](../.cursor/rules/spec-driven-impl.mdc) | Spec 驱动实现 | **任何实现前必读** |
| [phase-gate-protocol.mdc](../.cursor/rules/phase-gate-protocol.mdc) | 阶段 Gate 协议 | **跑 Gate 前必读** |
| [doc-sync-with-code.mdc](../.cursor/rules/doc-sync-with-code.mdc) | 文档与代码同步 | **改完代码后必读** |

### 3.2 6 大工作流（**新协作者第 1 天学这些**）

#### 工作流 1：新增 / 修 API 端点

```
读 API-M* spec 端点契约
   ↓
按 5 段式 prompt 写：DTO → Mapper(@Select/@Update) → Service → Controller → IT
   ↓
mvn test -Dtest=*<FeatureName>IT*
   ↓
浏览器 dev 复测（iwr / agent-browser）
   ↓
写 ADR（如有架构决策）/ 更新 PRD（字段变更）
```

**关键**：
- 端点 = spec + service + controller + IT **4 步缺一不可**（S-R2-Fix 教训）
- 后端 `Map<String,Object>` 返回值要在 service 层 `toInt/toLong/toDouble` 转换（NaN 防护）
- Spring 静默忽略未声明的 `@RequestParam`（S-R8 B6 / S-R9 B1 教训）—— 改了 service 一定要 grep controller 端点

#### 工作流 2：修前端 mock / 假数据

```
找到 <view>.vue → 找 computed 里 × 系数 / 硬编码 0 / Math.random
   ↓
查后端：是否已有 /list 端点？
   ↓
   有 → 改前端调真 API + 改 types/*.ts 对齐后端 VO
   没有 → 走工作流 1 补后端
   ↓
mvn test + 浏览器真值校验（不是只检查 200 OK）
   ↓
跑 enum-alias.ts 看是否需要规范化
```

**关键**：
- **不能只看 API 200 OK** —— 还要看数据真值（S-R9 报告漏改 2 项的教训）
- 前端 `pageNo` vs 后端 `pageNum`：**以 API spec 为准**，不是看后端 controller 写啥
- 前端 enum 字面值 ≠ DB dict 真实值 → 用 `enum-alias.ts` 规范化

#### 工作流 3：走查 1 页（S-R3 沉淀 7 步法）

```
Step 1: 锁定 scope (AskQuestion 选 1 页)
Step 2: 读 spec (CHECKLIST + PRD + API)
Step 3: 查 DB 字典真值
Step 4: agent-browser 走查 4 命令循环（open/snapshot/eval/click）
Step 5: 列发现清单 + AskQuestion 让用户批准修复范围
Step 6: 实施 (mvn compile → mvn test → reload)
Step 7: 写报告 + 同步 SESSION-PROGRESS
```

#### 工作流 4：报 bug / 修 bug

```
FR/AC 编号 + 现象 + 期望 + 复现步骤 + 限定文件范围 + 回归用例
   ↓
查已有 IT 是否覆盖 → 没覆盖先补 IT
   ↓
按工作流 1 / 2 实施
   ↓
mvn test 全套 + 浏览器复测
   ↓
写报告 + 更新 P-GATE backlog
```

#### 工作流 5：阶段 Gate 验收

```
8 条 R 规则 [MASTER-EXECUTION-TRACKER.md §0.1]
   ↓
模块 CHECKLIST-M* 100% 勾选
   ↓
TESTCASES-M* P0 100% 通过
   ↓
seed-* 通过 SeedVerificationIT + 人工抽检
   ↓
mvn verify 无失败
   ↓
上一阶段 P0 冒烟仍绿
   ↓
归档 GATE-S{n}-报告-{YYYYMMDD}.md
   ↓
更新 MASTER-EXECUTION-TRACKER.md §1 为 ✅
```

#### 工作流 6：多模块 schema drift

```
发现 oa_content 无 user 关联 / oa_order 无 ops_user_id
   ↓
不要硬填 0
   ↓
写 ADR-NNN：列候选方案（A 加字段 / B 多对多 / C 改 spec）
   ↓
vo 加全字段（占位 0）
   ↓
前端展开卡加 ADR 提示
   ↓
等 M2 SOP / M4 走查时一起定方案
   ↓
定下来后：DB migration + 后端聚合 SQL + 前端展示
```

### 3.3 自查方法（**S-R10 沉淀，新增**）

> **背景**：S-R9 报告说"已修"实际是空数据；S-R10 复测才发现 2 项漏改。

**自查方法 7 条**（每次写"已修"前必走）：

| # | 自查项 | 操作 | 通过标准 |
|---|--------|------|----------|
| 1 | **代码 ≠ 报告** | 报告写"已修 X" → grep 源码确认真有改动 | 报告每行对应真实代码 |
| 2 | **API 200 ≠ 数据真** | 改完调 iwr 拿真值，不只看 HTTP 200 | 数值 > 0（除非 spec 允许 0） |
| 3 | **canvas/div ≠ 有数据** | chart 容器存在 ≠ 数据渲染 | echarts.init 后 `canvas.length > 0` |
| 4 | **HMR 状态 ≠ 用户视角** | 改完要 hard reload，不只 HMR | 用户视角无 console warning |
| 5 | **断言 ≥ 0 ≠ 等于 N** | H2 库 seed 数 ≠ dev 库 | 用 `greaterThanOrEqualTo(0)` 而非 `equalTo(26)` |
| 6 | **空 chart ≠ 占位** | S-R10 教训：空 chart 是无意义 | 至少显示"暂无数据"+ 折叠清理 |
| 7 | **手敲 ≠ 引用** | 用 dict_type 名前**先查 DB** | `dict_position` ≠ `dict_position_type` |

### 3.4 走查报告 6 节模板（SSOT）

```markdown
# S-R{N} 报告：{页面名} 走查

**日期** · **模块** · **路径** · **前置**

## 一、走查结论（一句话）
## 二、发现清单（表格：ID / 级别 / 问题 / 位置）
## 三、修复方案（每个 bug 一节）
## 四、验证（4 类）
   4.1 mvn compile
   4.2 mvn test 回归（含新 IT 数量）
   4.3 API 实测（curl 拿真数据）
   4.4 浏览器复测（每个 AC 一行）
## 五、新增 / 修改文件清单
## 六、反思（5 大类）
   - 发现的 spec/数据结构问题
   - 留给下个 slice 的 TODO
   - 方法论沉淀（写给后人）
   - 走查方法升级
   - ADR 触发
```

### 3.5 ADR 触发条件

满足任一即写 ADR-NNN：

- [ ] 新引入外部依赖 / 中间件
- [ ] 跨模块数据关联新增字段
- [ ] API 契约与 spec 不一致（spec 改 or code 改？）
- [ ] 性能 / 安全 / 鉴权 选型
- [ ] schema drift（实际表结构 ≠ spec 描述）

---

## 四、执行指引（单人全栈）

> 2026-06-10 起废止 Mike/Donny 双人并行；前后端由同一人按 slice 顺序交付。

### 4.1 角色（一人兼）

| 职责 | 范围 | SSOT |
|------|------|------|
| 全栈实现 | Mapper → Controller → IT → api/types/vue → PW | API-M*.md · UX-M*.md |
| 产品/架构 | PRD · OQ · ADR · Gate 签字 | PRD-M*.md · PHASE-DEV-METHOD.md |
| 验收 | mvn test + Playwright + 走查 | TESTCASES-M*.md · walkthrough-methodology.mdc |

### 4.2 切片执行

- slice 命名：`S-R{N}`（子项如 S-R27·27b）
- **唯一任务表**：`docs/delivery/TASK-PROGRESS-MASTER.md`
- 每个 slice：**后端 → 前端 → 测试 → 文档** 一气呵成
- 完成：更新 SESSION-PROGRESS + `git pull` → commit → push

### 4.3 变更同步（单人仍须遵守）

| 情况 | 处理 |
|------|------|
| 后端 API 字段新增 | 同 slice 内同步：VO + DTO + Mapper + IT + types + vue + ADR |
| 前端 enum 改名 | 同步 types + enum-alias.ts + 使用处 + ADR |
| 前端 enum 改名 | 必须同步：types + 5+ 处使用 + enum-alias.ts + ADR |
| schema drift | **写 ADR** + 走"工作流 6" |

### 4.4 每日 stand-up 模板

```markdown
## YYYY-MM-DD · {name} 进展

### 昨日
- 完成 S-R{N}：{页面} 走查，{N} bug
- 修了 {N} IT，新增 {M} IT

### 今日
- S-R{N+1}：{页面} 走查

### 阻塞
- L3 ADR-008 待产品决策

### 风险
- {描述}
```

### 4.5 文档同步规则（**写代码就改文档**）

| 改动 | 必同步 |
|------|--------|
| 后端 API 字段 | PRD + API-M*.md + types/*.ts + ADR |
| 前端路由 | router/index.ts + MASTER-EXECUTION-TRACKER.md §1.1 |
| 数据库表结构 | ADR + flyway V-NN__xxx.sql + TECH-M*.md |
| 业务规则 | PRD §4.* + 必加 AC 编号 |
| 走查完成 | SESSION-PROGRESS.md + gates/S-R{N}-报告 |

---

## 五、Phase 2（Out of Scope）

按 [MASTER-EXECUTION-TRACKER.md](./delivery/MASTER-EXECUTION-TRACKER.md) §1：
- **M10 采集**（外部平台数据抓取）—— 后期
- **外部 SSO**（钉钉/企微 OAuth）—— 后期

本期**禁止**做这两类工作。所有 Phase 2 范围若发现"看起来该做"的需求，统一**写 Open Question 推到后期**。

---

## 六、SSOT 文档索引（速查）

```
docs/
├── product/                # 产品 spec (PRD/UX)
│   ├── PRD-M0-首页.md
│   ├── PRD-M1-运营管理.md        ← S-R3~R10 持续同步
│   ├── PRD-M2-内容生产.md
│   ├── PRD-M3-绩效核算.md
│   ├── PRD-M4-账号管理.md
│   └── ...
├── engineering/            # 工程 spec (API/STATE/METHOD)
│   ├── PHASE-DEV-METHOD.md      ← 总体方法论 SSOT
│   ├── AI-IMPL-GUIDE.md         ← 五段式 Prompt
│   ├── API-M*.md                ← API 契约
│   └── STATE-M*.md              ← 状态机
├── delivery/               # 交付管理
│   ├── MASTER-EXECUTION-TRACKER.md  ← 阶段 Gate 总表
│   ├── SESSION-PROGRESS.md          ← 会话进度
│   ├── CHECKLIST-M*.md              ← 模块自检
│   ├── TESTCASES-M*.md              ← 模块测试
│   ├── SLICES-M*.md                 ← 切片
│   └── gates/                       ← 11 份走查报告 + 8 份 Gate 报告
├── adr/                    # 架构决策
│   ├── ADR-001~008               ← 8 份 ADR
│   └── ADR-M1-001~002
└── .cursor/rules/          # Cursor 工作流规则
    ├── spec-driven-impl.mdc
    ├── phase-gate-protocol.mdc
    ├── doc-sync-with-code.mdc
    └── walkthrough-methodology.mdc  ← 走查方法论 (S-R3 沉淀)
```

---

## 七、致新协作者

1. **先读** [PHASE-DEV-METHOD.md](./engineering/PHASE-DEV-METHOD.md) + [walkthrough-methodology.mdc](../.cursor/rules/walkthrough-methodology.mdc)（2 份文档共 1 小时读完）
2. **再读** [SESSION-PROGRESS.md](./delivery/SESSION-PROGRESS.md)（15 分钟）
3. **从 [TASK-PROGRESS-MASTER](./delivery/TASK-PROGRESS-MASTER.md) 取下一个 S-R{N}**
4. **每步遵循 6 大工作流**（3.2 节）
5. **每改一处自查 7 条**（3.3 节）
6. **完成写报告 + 同步文档**（3.4 / 3.5 节）

按 [TASK-PROGRESS-MASTER](./delivery/TASK-PROGRESS-MASTER.md) 顺序执行即可。
