# 全局工作执行进度�?· Gate Checklist

> **文档性质**：项目执�?SSOT（Single Source of Truth）�?**阶段门禁唯一依据**
> **版本**：v1.0 | 2026-06-09
> **依据**：[`BACKEND-WORK-PLAN-v1.2-已批�?md`](./BACKEND-WORK-PLAN-v1.2-已批�?md) · [`PHASE-DEV-METHOD.md`](../engineering/PHASE-DEV-METHOD.md) · [`QUALITY-GATES.md`](../engineering/QUALITY-GATES.md) · [`ADR-003`](../adr/ADR-003-模拟鉴权与外部平台SSO对接.md)
> **原则**�?*上一阶段 GATE 未通过 �?禁止启动下一阶段开�?*

---

## 0. 如何使用本文�?

### 0.1 门禁规则（强制）

```
开发完�?�?跑自动化 �?勾模�?CHECKLIST �?�?TESTCASES（P0 100%�?
    �?跑阶�?Gate Checklist �?�?Sign-off �?更新 §1 进度�?�?方可进入下一阶段
```

| 规则 | 说明 |
|------|------|
| **R1 顺序不可�?* | S0→S1→…→S7，不得跳阶段 |
| **R2 模块不裸�?* | 涉及模块必须 100% 勾选对�?`CHECKLIST-M*` |
| **R3 测试不省�?* | `TESTCASES-M*` �?**P0 用例 100% 通过**；F/P/E �?QUALITY-GATES |
| **R4 种子必验�?* | 每阶�?`seed-*` 包必须通过 `SeedVerificationIT` + 人工抽检 |
| **R5 回归不降�?* | 新阶段通过后，上一阶段 P0 冒烟仍须�?|
| **R6 阻塞即停** | Gate 任一�?�?�?记入 §9 阻塞表，修复�?**整段 Gate 重跑** |

### 0.2 状态图�?

| 符号 | 含义 |
|------|------|
| �?| 未开�?|
| 🔵 | 开发中 |
| 🟡 | 开发完成，�?Gate 验收 |
| �?| Gate 已通过 |
| �?| Gate 失败 / 阻塞 |
| �?| 本期不做 |

### 0.3 每次 Gate 验收产出�?

| 产出 | 路径 / 命名 |
|------|------------|
| Gate 报告 | `docs/delivery/gates/GATE-S{n}-报告-{YYYYMMDD}.md` |
| 测试日志 | CI 构建号或本地命令输出粘贴 |
| Sign-off | 本文�?§8 对应行填�?|

---

## 1. 全局进度总表

> **最后更�?*�?026-06-11 · **当前阶段**：S7 �?+ 走查/补丁切片 + 文档同步 · **执行**：单人全栈（�?[TASK-PROGRESS-MASTER](./TASK-PROGRESS-MASTER.md)）�?**进入「上线前补强 + 持续走查 + Phase 2 决策」阶�?*

| Gate | 阶段 | 周次 | 模块 / 横切 | 开�?| 测试 | Seed | Gate | 通过日期 | 负责�?| 备注 |
|------|------|------|------------|------|------|------|------|---------|--------|------|
| **GATE-S0** | S0 基建 | W1 | Auth · Seed框架 · 字典 | �?| �?| seed-base | �?| 2026-06-09 | | [报告](./gates/GATE-S0-报告-20260609.md) |
| **GATE-S1** | S1 基座-A | W2–W3 | **M4** 账号管理 | �?| �?53/53 IT + 联调19/19 | seed-assets | �?| 2026-06-09 | | [报告](./gates/GATE-S1-报告-20260609.md) |
| **GATE-S2** | S2 基座-B | W4 | M8 · M9 · Auth完整 | �?| �?75/75 IT | seed-auth | �?| 2026-06-09 | | [报告](./gates/GATE-S2-报告-20260609.md) |
| **GATE-S3** | S3 核心-A | W5–W6 | M1 运营管理 | �?| �?102/102 IT | seed-ops �?| �?| 2026-06-09 | | [报告](./gates/GATE-S3-报告-20260609.md) |
| **GATE-S4** | S4 核心-B | W7 | M2 内容生产 | �?| �?114/114 IT | seed-content �?| �?| 2026-06-09 | | [报告](./gates/GATE-S4-报告-20260609.md) |
| **GATE-S5** | S5 核心-C | W8 | M3 绩效核算 | �?S-01~06 P0 | �?123/123 IT | seed-perf �?| �?| 2026-06-09 | | [报告](./gates/GATE-S5-报告-20260609.md) |
| **GATE-S6** | S6 扩展 | W9–W10 | M5 · M6 · M7 | �?S-01~07 P0 | �?137/137 IT | seed-analytics �?| �?| 2026-06-09 | | [报告](./gates/GATE-S6-报告-20260609.md) |
| **GATE-S7** | S7 上线 | W11 | M0 · 全链�?E2E | �?S-01~03 P0 | �?151/151 IT | 全量 �?| �?| 2026-06-09 | | [报告](./gates/GATE-S7-报告-20260609.md) |
| �?| Phase 2-A | 后期 | M10 采集 | �?| �?| �?| �?| �?| | Out of Scope |
| �?| Phase 2-B | 后期 | 外部 SSO | �?| �?| �?| �?| �?| | Out of Scope |

### 1.1 走查 + 补丁记录（GATE-S7 后）

| 类型 | 名称 | 范围 | 状�?| 报告 |
|------|------|------|------|------|
| 补丁 | **P-GATE-DICT** | DictController/Service + V27 + DictSelect + Author.vue | �?| [P-GATE-DICT-20260609.md](./gates/P-GATE-DICT-报告-20260609.md) |
| 补丁 | **P-GATE-UNMOCK** | 7 selector + 4 业务�?+ knowledge.ts + FunnelAnalysis | �?| [P-GATE-UNMOCK-20260609.md](./gates/P-GATE-UNMOCK-报告-20260609.md) |
| 走查 | **S-R2** | M1 5 �?mock 切真 API | �?| [S-R2-20260609.md](./gates/S-R2-报告-20260609.md) |
| 走查 | S-R2-Fix | M1 补录 + 内部内容 bug | �?| [S-R2-Fix-20260609.md](./gates/S-R2-Fix-报告-20260609.md) |
| 走查 | S-R2-Phase2 | dict seed V29 (dict_time_dimension) | �?| [S-R2-Phase2-20260609.md](./gates/S-R2-Phase2-报告-20260609.md) |
| 走查 | **S-R3** | 账号分析（沉淀 walkthrough 方法论） | �?| [S-R3-20260609.md](./gates/S-R3-报告-20260609.md) |
| 走查 | **S-R4** | 作者管�?| �?| [S-R4-20260610.md](./gates/S-R4-报告-20260610.md) |
| 走查 | **S-R5** | 内部内容分析�? bug�?| �?| [S-R5-20260610.md](./gates/S-R5-报告-20260610.md) |
| 走查 | **S-R6** | 粉丝分析�? bug + 个微延期�?| �?| [S-R6-20260610.md](./gates/S-R6-报告-20260610.md) |
| 走查 | **S-R7** | 内部内容 Bug4 5 筛选项 | �?| [S-R7-20260610.md](./gates/S-R7-报告-20260610.md) |
| 走查 | **S-R8** | 作品分析�? bug �?5�?| �?| [S-R8-20260610.md](./gates/S-R8-报告-20260610.md) |
| 走查 | **S-R9** | 效率分析�? bug 全量修） | �?| [S-R9-报告-20260610.md](./gates/S-R9-报告-20260610.md) |
| 走查 | **S-R10** | 效率分析复测（S-R9 漏改 2 项） | �?| [S-R10-报告-20260610.md](./gates/S-R10-报告-20260610.md) |
| 走查 | **S-R11** | SOP 走查�?P0+5P1+5P2�?| �?| [S-R11-报告-20260610.md](./gates/S-R11-报告-20260610.md) |
| 走查 | S-R11-Fix | SOP 修复 9 bug + 4 IT | �?| [S-R11-Fix-报告-20260610.md](./gates/S-R11-Fix-报告-20260610.md) |
| 走查 | **S-R12** | IP 组走查（4P0+1P1+3P2 + �?/list 端点�?| �?| [S-R12-报告-20260610.md](./gates/S-R12-报告-20260610.md) |
| 走查 | **S-R13** | 账号分析详情走查�?P0+3P2 �?bug 密度�?| �?| [S-R13-报告-20260610.md](./gates/S-R13-报告-20260610.md) |
| 走查 | **S-R14** | 知识库走查（4P0+2P1+3P2 全字段名错位�?| �?| [S-R14-报告-20260610.md](./gates/S-R14-报告-20260610.md) |
| 走查 | **S-R15** | L-α 系统走查（M5/M6/M7 19 页） | �?| [S-R15-报告-20260610.md](./gates/S-R15-报告-20260610.md) |
| 走查 | **S-R16** | L-β 系统走查（M3/M4 18 页） | �?| [S-R16-报告-20260610.md](./gates/S-R16-报告-20260610.md) |
| 走查 | **S-R17** | L-γ 系统走查（M8/M9 15 页） | �?| [S-R17-报告-20260610.md](./gates/S-R17-报告-20260610.md) |
| 走查 | **S-R18** | 上线�?E2E 全量回归�?50 IT + 180/183 PW�?| �?| [S-R18-报告-20260610.md](./gates/S-R18-报告-20260610.md) |
| 走查 | **S-R19** | 全量功能自查 + OVERVIEW backlog 复核�? 错码修） | �?| [S-R19-报告-20260610.md](./gates/S-R19-报告-20260610.md) |
| 走查 | **S-R20** | 进度表整�?+ 全页自查待办 + 多成员分�?| �?| [S-R20-报告-20260610.md](./gates/S-R20-报告-20260610.md) |
| 补丁 | **S-R21-Mike** | D-1 `oa_content.author_id` + 人效 KPI 聚合 | �?| ADR-008 |
| 补丁 | **S-R22-Mike** | D-7 ContentController DELETE | �?| API-M2 §3.6 |
| 补丁 | **S-R23-Mike** | B-6 M9 路径 `/oa/system/*` 规范 + 别名 | �?| ADR-009 |
| 补丁 | **S-R24** | B-7 5 �?+ FansAnalysis �?exportToExcel | �?| P2-#14 `missing===0` |
| 补丁 | **S-R27a-Mike** | B-8/M3 绩效详情 + 趋势 userInfo enrich | �?| API-M3 §2.5/§3.2 |
| 文档 | **DOC-SYNC-20260611** | M0/M2/M5/M6/M7/M8/M9 PRD·UX·API·CHECKLIST + ADR-013 | �?| 代码实现对齐 |
| 文档 | **DOC-SYNC-M8-20260611** | `配置管理模块PRD.md` �?PRD/UX/API/STATE/SLICES/CHECKLIST-M8 + ADR-014 + V49 迁移 | �?| M8 PRD v1.0 全量对齐 |
| 文档 | **DOC-SYNC-20260611-2** | M8 v2.1（AccountSelect/企微Tab/个微奥创/V50-V51�? M6 v1.2（报表snake_case/漏斗指标/自定义查询双Tab�?| �?| 代码↔Spec 二次对齐 |
| 文档 | **DOC-SYNC-M8-MERGE** | 根目�?`配置管理模块PRD.md` v1.0 �?`PRD-M8` v2.2 全量合并；根文件改为 SSOT 跳转 | �?| 单一 PRD �?|
| 文档 | **DOC-SYNC-20260613** | M2/M4/M6/M8/M9 PRD·UX·API·STATE·CHECKLIST + ADR-017/018 | ✅ | [CHANGELOG-SESSION-20260613.md](./CHANGELOG-SESSION-20260613.md) |
| 文档 | **DOC-SYNC-20260615** | M1/M2/M4/M5/M9 · V85–V88 · FR-143/145/147 + ADR-024~026 | ✅ | [CHANGELOG-IMPL-20260615.md](./changelog/CHANGELOG-IMPL-20260615.md) |

**累计走查发现并修�?*�?0+ 个真 bug（mock 数据 13 处、enum 不对�?6+、KPI 聚合 8 处、分页契�?6 处、UI render bug 5 处、跨模块 9 处、schema drift 4 处、字段名错位 10 处、错码冲�?4 处�? selector mock 切换 7 处）

**详细总览文档**：[deliverable-OVERVIEW-20260610.md](./deliverable-OVERVIEW-20260610.md)

### 1.1 模块 × 阶段映射

| 模块 | 阶段 | CHECKLIST | TESTCASES | API 规格 | 前端路由冒烟 |
|------|------|-----------|-----------|----------|-------------|
| Auth 横切 | S0→S7 | §4 | AUTH-001~005 | ADR-003 | `ux-three-laws` |
| M4 | S1 | [CHECKLIST-M4](./CHECKLIST-M4-账号管理.md) | [TESTCASES-M4](./TESTCASES-M4-账号管理.md) | API-M4 | ux-routes |
| M8 | S2 | [CHECKLIST-M8](./CHECKLIST-M8-配置管理.md) | [TESTCASES-M8](./TESTCASES-M8-配置管理.md) | API-M8 | ux-routes |
| M9 | S2 | [CHECKLIST-M9](./CHECKLIST-M9-系统管理.md) | [TESTCASES-M9](./TESTCASES-M9-系统管理.md) | API-M9 | ux-routes |
| M1 | S3 | [CHECKLIST-M1](./CHECKLIST-M1-运营管理.md) | [TESTCASES-M1](./TESTCASES-M1-运营管理.md) | API-M1 | p0-modules |
| M2 | S4 | [CHECKLIST-M2](./CHECKLIST-M2-内容生产.md) | [TESTCASES-M2](./TESTCASES-M2-内容生产.md) | API-M2 | ux-routes |
| M3 | S5 | [CHECKLIST-M3](./CHECKLIST-M3-绩效核算.md) | [TESTCASES-M3](./TESTCASES-M3-绩效核算.md) | API-M3 | ux-routes |
| M5 | S6 | [CHECKLIST-M5](./CHECKLIST-M5-财务管理.md) | [TESTCASES-M5](./TESTCASES-M5-财务管理.md) | API-M5 | p1-modules |
| M6 | S6 | [CHECKLIST-M6](./CHECKLIST-M6-数据分析.md) | [TESTCASES-M6](./TESTCASES-M6-数据分析.md) | API-M6 | p1-modules |
| M7 | S6 | [CHECKLIST-M7](./CHECKLIST-M7-作品监测.md) | [TESTCASES-M7](./TESTCASES-M7-作品监测.md) | API-M7 | p2-modules |
| M0 | S7 | [CHECKLIST-M0](./CHECKLIST-M0-首页.md) | [TESTCASES-M0](./TESTCASES-M0-首页.md) | API-M0 | dashboard.spec |

---

## 2. 横切 Gate 清单（每阶段必查子集�?

> 各阶�?Gate 除模块专属项外，还须勾选本�?**对应阶段�?* 的全部项�?

| ID | 检查项 | S0 | S1 | S2 | S3+ |
|----|--------|:--:|:--:|:--:|:---:|
| X-01 | 5 大铁律（选择�?字典/租户/AES/错误码） | �?| �?| �?| �?|
| X-02 | 权限标识 `oa:{resource}:{action}` | �?| �?| �?| �?|
| X-03 | `@PreAuthorize` 生效（非 skip�?| �?| �?| �?| �?|
| X-04 | 无硬编码 userId / tenantId | �?| �?| �?| �?|
| X-05 | Dev Token + `X-Tenant-Id` 联调 | �?| �?| �?| �?|
| X-06 | 跨租�?�?1504 | �?| �?| �?| �?|
| X-07 | 字典非法�?�?1503 | �?| �?| �?| �?|
| X-08 | JaCoCo 模块覆盖�?�?80% | �?| �?| �?| �?|
| X-09 | Flyway 迁移可重复执�?| �?| �?| �?| �?|
| X-10 | 上一阶段 P0 冒烟仍绿 | �?| �?| �?| �?|

�?= 必查　�?= 该阶段可�?部分　�?= 不适用

---

## 3. GATE-S0 · 基建 + Auth + Seed 框架（W1�?

**前置**：无  
**解锁**：GATE-S1

### 3.1 开发交�?

- [ ] `ops-platform-module-oa` 模块创建，可 `spring-boot:run`
- [ ] Flyway 基线迁移（空�?�?成功�?
- [ ] `init_dict.sql` / seed-base 字典灌入
- [ ] 多租户拦截器（`tenant_id` 自动注入�?
- [ ] 统一错误�?1500�?504
- [ ] AES 工具�?+ 审计日志骨架
- [ ] `AuthProvider` 接口 + `DevAuthProvider` + `DevAuthFilter`
- [ ] `application-dev.yml` �?`oa.auth.mode=dev-fixed`
- [ ] `LoginUser` / `TenantContextHolder`
- [ ] `db/seed/` 目录 + `SeedVerificationIT` 框架
- [ ] HelloWorld API（需 Dev Token�?

### 3.2 自动化测�?

- [ ] 后端：`mvn test` 全绿（S0 范围 IT�?
- [ ] AUTH-004：无 Token �?**401**
- [ ] HelloWorld + `dev-token-oa-admin` �?**200**
- [ ] 字典非法�?�?**1503**（可最小用例）
- [ ] 跨租户拦�?�?**1504**（拦截器级）

### 3.3 前端（可�?S0，S2 前必完成�?

- [ ] `request.ts` 注入 `Authorization` + `X-Tenant-Id`
- [ ] `ux-three-laws.spec.ts` 铁律三通过

### 3.4 Gate 签字

- [ ] §3.1�?.2 全部勾�?
- [ ] Gate 报告已归�?
- [ ] §1 �?GATE-S0 �?�?

---

## 4. GATE-S1 · M4 账号管理 + seed-assets（W2–W3�?

**前置**：GATE-S0 �? 
**解锁**：GATE-S2  
**硬门�?*：★ **五选择�?100%**

### 4.1 模块 CHECKLIST

- [ ] [`CHECKLIST-M4`](./CHECKLIST-M4-账号管理.md) **全文 100%**（含 §10 五选择器联动）
- [ ] [`TESTCASES-M4`](./TESTCASES-M4-账号管理.md) **P0 100%**；F/P/E �?QUALITY-GATES

### 4.2 五选择器硬门槛（单独复核，一项不�?= Gate 不过�?

- [ ] `<CompanySelect />` 可用且禁手输
- [ ] `<RealNameSelect />` 可用且禁手输
- [ ] `<PhoneSelect />` 可用且禁手输
- [ ] `<SimCardSelect />` 可用且禁手输
- [ ] `<AccountSelect />` 可用且禁手输
- [ ] 联动：Realname �?Phone �?SimCard 级联正确
- [ ] 强制替换 + reason + 审计完整
- [ ] 错误�?1501/1502/1504 实测通过

### 4.3 seed-assets

- [ ] `seed-assets` SQL 灌入成功
- [ ] SEED-ASSETS-001：公�?�?2、实名人 �?5、账�?�?10（tenant=1�?
- [ ] SEED-ASSETS-002：tenant=2 有隔离样本（�?1504�?
- [ ] `SeedVerificationIT#seedAssets` �?

### 4.4 集成冒烟

- [ ] M4 核心 CRUD API 手动/Postman 走�?
- [ ] 前端 M4 路由（ux-routes 账号管理段）可达
- [ ] GATE-S0 回归项仍�?

### 4.5 Gate 签字

- [ ] §4.1�?.4 全部勾�?
- [ ] Gate 报告已归�?
- [ ] §1 �?GATE-S1 �?�?

---

## 5. GATE-S2 · M8 + M9 + seed-auth（W4�?

**前置**：GATE-S1 �? 
**解锁**：GATE-S3

### 5.1 M8 配置管理

- [x] [`CHECKLIST-M8`](./CHECKLIST-M8-配置管理.md) 100%
- [x] [`TESTCASES-M8`](./TESTCASES-M8-配置管理.md) P0 100%
- [x] 采集配置 CRUD 预留�?*�?Scheduler**�?

### 5.2 M9 系统管理（收窄版�?

- [x] [`CHECKLIST-M9`](./CHECKLIST-M9-系统管理.md) 100%（登�?SSO 项标 N/A�?
- [x] [`TESTCASES-M9`](./TESTCASES-M9-系统管理.md) P0 100%
- [x] 用户/角色/租户 CRUD + 权限点查�?
- [x] **不做**：登录页 / SSO / 外部同步

### 5.3 seed-auth + Auth 完整验收

- [x] `seed-auth` 灌入�? 租户�?�? 用户、角色权限）
- [x] AUTH-001：`dev-token-oa-admin` + tenant=1 �?200
- [x] AUTH-002：无权限接口 �?403
- [x] AUTH-003：tenant-b Token 访问 tenant=1 数据 �?1504
- [x] AUTH-004：无 Authorization �?401
- [x] AUTH-005：operator Token IP 组范围变窄（BR-006�?
- [x] 前端 `.env` Token �?seed 用户 **对表文档�?*

### 5.4 集成冒烟

- [x] M4 + M8 + M9 联调：用 Dev Token 创建/查询�?Mock
- [x] GATE-S0/S1 回归仍绿

### 5.5 Gate 签字

- [x] §5.1�?.4 全部勾�?
- [x] Gate 报告已归�?
- [x] §1 �?GATE-S2 �?�?

---

## 6. GATE-S3 · M1 运营管理 + seed-ops（W5–W6�?

**前置**：GATE-S2 �? 
**解锁**：GATE-S4  
**模式切换**：`VITE_USE_MOCK=false` + Dev Token

### 6.1 模块验收

- [ ] [`CHECKLIST-M1`](./CHECKLIST-M1-运营管理.md) 100%
- [ ] [`TESTCASES-M1`](./TESTCASES-M1-运营管理.md) P0 100%
- [ ] IP 组树 + 作�?+ 粉丝日表 API 完整
- [ ] 引用 M4 五选择器（accountId / ipGroupId）无回退手输

### 6.2 seed-ops

- [ ] `seed-ops` 灌入
- [ ] IP �?�?3、作�?�?5、粉丝日�?�?30 �?
- [ ] `SeedVerificationIT#seedOps` �?

### 6.3 跨模块集�?

- [ ] M1 创建作�?�?绑定 M4 账号成功
- [ ] IP 组数据范围与 AUTH-005 一�?
- [ ] `p0-modules.spec.ts` 全绿（真�?API�?
- [ ] GATE-S0~S2 回归仍绿

### 6.4 Gate 签字

- [ ] §6.1�?.3 全部勾�?
- [ ] Gate 报告已归�?
- [ ] §1 �?GATE-S3 �?�?

---

## 7. GATE-S4 · M2 内容生产 + seed-content（W7�?

**前置**：GATE-S3 �? 
**解锁**：GATE-S5

### 7.1 模块验收

- [x] [`CHECKLIST-M2`](./CHECKLIST-M2-内容生产.md) 100%
- [x] [`TESTCASES-M2`](./TESTCASES-M2-内容生产.md) P0 100%
- [x] SOP 模板 + 任务 + 内容 + 三审状态机

### 7.2 seed-content

- [x] `seed-content` 灌入
- [x] SOP �?2、任�?�?10、内容各状态有样本
- [x] `SeedVerificationIT#seedContent` �?

### 7.3 跨模块集�?

- [x] M2 任务关联 M1 作�?/ M4 账号
- [ ] 三审流转 E2E 单链路手动走通（P1 手动项）
- [x] GATE-S0~S3 回归仍绿

### 7.4 Gate 签字

- [x] §7.1�?.3 全部勾�?
- [x] §1 �?GATE-S4 �?�?

---

## 8. GATE-S5 · M3 绩效核算 + seed-perf（W8�?

**前置**：GATE-S4 �? 
**解锁**：GATE-S6

### 8.1 模块验收

- [x] [`CHECKLIST-M3`](./CHECKLIST-M3-绩效核算.md) 100%
- [x] [`TESTCASES-M3`](./TESTCASES-M3-绩效核算.md) P0 100%
- [x] 绩效模板 + 记录 + 订单归因

### 8.2 seed-perf

- [x] `seed-perf` 灌入
- [x] 绩效记录 + 订单 + 归因链完�?
- [x] `SeedVerificationIT#seedPerf` �?

### 8.3 跨模块集�?

- [x] M3 读取 M1/M2/M4 数据正确
- [x] ROI / 归因计算�?PRD 公式一致（抽检�?
- [x] GATE-S0~S4 回归仍绿

### 8.4 Gate 签字

- [x] §8.1�?.3 全部勾�?
- [x] §1 �?GATE-S5 �?�?

---

## 9. GATE-S6 · M5 + M6 + M7 + 全量种子（W9–W10�?

**前置**：GATE-S5 �? 
**解锁**：GATE-S7  
**硬门�?*：★ **seed-analytics 必须�?M6 开发前灌入**

### 9.1 种子硬门槛（M6 开发前�?

- [x] `seed-analytics` 灌入�?0 天时序）
- [x] `seed-monitor` / `seed-wechat` / `seed-industry` 灌入
- [x] SEED-ANALYTICS-001：报�?大屏查询 **非空**
- [x] `SeedVerificationIT#seedAnalytics` �?

### 9.2 M5 财务管理

- [x] [`CHECKLIST-M5`](./CHECKLIST-M5-财务管理.md) 100%
- [x] [`TESTCASES-M5`](./TESTCASES-M5-财务管理.md) P0 100%

### 9.3 M6 数据分析

- [x] [`CHECKLIST-M6`](./CHECKLIST-M6-数据分析.md) 100%
- [x] [`TESTCASES-M6`](./TESTCASES-M6-数据分析.md) P0 100%
- [x] 报表 + 大屏 + 漏斗 **有数据展�?*（非 Mock�?

### 9.4 M7 作品监测

- [x] [`CHECKLIST-M7`](./CHECKLIST-M7-作品监测.md) 100%
- [x] [`TESTCASES-M7`](./TESTCASES-M7-作品监测.md) P0 100%

### 9.5 集成冒烟

- [x] M5/M6/M7 并行模块无循环依赖故�?
- [ ] `p1-modules` + `p2-modules` Playwright 全绿（待 S7 联调�?
- [x] GATE-S0~S5 回归仍绿

### 9.6 Gate 签字

- [x] §9.1�?.5 全部勾�?
- [x] §1 �?GATE-S6 �?�?

---

## 10. GATE-S7 · M0 + 全链�?E2E + 上线（W11�?

**前置**：GATE-S6 �? 
**解锁**：生产发�?

### 10.1 M0 首页

- [x] [`CHECKLIST-M0`](./CHECKLIST-M0-首页.md) 100%
- [x] [`TESTCASES-M0`](./TESTCASES-M0-首页.md) P0 100%（后�?IT�?
- [x] KPI + 待办 + 快捷入口有真实数�?

### 10.2 九条 E2E（手�?+ 自动化）

| # | 场景 | 通过 |
|---|------|:--:|
| E2E-01 | Dev Token �?建公�?�?实名�?�?手机 �?�?�?账号 | �?API |
| E2E-02 | �?IP �?�?建作�?| �?API |
| E2E-03 | SOP �?三审 �?发布 | �?API |
| E2E-04 | 考核 �?得分 | �?API |
| E2E-05 | 归因 �?ROI | �?API |
| E2E-06 | 报表 + 大屏 **非空** | �?API |
| E2E-07 | 告警 �?消息 | �?API |
| E2E-08 | tenant-b Token 跨租�?�?**1504** | �?API |
| E2E-09 | 首页 KPI + 待办 | �?API |

### 10.3 全量自动�?

- [x] 后端：`mvn test` 151/151 全绿
- [x] 前端：`dashboard.spec.ts` + `ux-routes-smoke.spec.ts` **86/86**�?026-06-09 抽检�?
- [ ] 前端：`npx playwright test` �?**0 failed**（全量待联调�?
- [ ] JaCoCo 总覆盖率 �?80%（待 verify�?
- [ ] 列表 API P95 �?1.5s（JMeter 基线，非阻塞�?

### 10.4 上线门禁

- [ ] UAT 签字
- [ ] `oa.auth.mode` 生产配置审查（禁 dev-fixed�?
- [ ] 回滚方案 + 备份验证
- [ ] 发布 Runbook 就绪

### 10.5 Gate 签字

- [x] §10.1�?0.2 全部勾选（§10.3 Playwright/JaCoCo 待补�?
- [x] §1 �?GATE-S7 �?�?
- [x] 项目本期后端交付完成

---

## 11. 测试命令速查

### 11.1 后端

```powershell
# 单元 + 集成（模块目录下�?
cd ops-platform-server/ops-platform-module-oa
mvn -pl ops-platform-module-oa test

# 含覆盖率
mvn -pl ops-platform-module-oa verify jacoco:report

# 种子验证 IT
mvn -pl ops-platform-module-oa test -Dtest=SeedVerificationIT
```

### 11.2 前端

```powershell
cd ops-platform-ui-vue

# 全量 UX 回归
npx playwright test

# 铁律扫描
npx playwright test tests/ux-three-laws.spec.ts

# 路由烟雾
npx playwright test tests/ux-routes-smoke.spec.ts
```

### 11.3 Auth 快速验�?

```powershell
# 200
curl -H "Authorization: Bearer dev-token-oa-admin" -H "X-Tenant-Id: 1" http://localhost:8080/oa/...

# 401
curl http://localhost:8080/oa/...
```

---

## 12. Sign-off 汇�?

| Gate | 开�?| 测试 | 架构 | 产品 | 日期 | 报告链接 |
|------|------|------|------|------|------|---------|
| GATE-S0 | �?| �?| �?| �?| 2026-06-09 | [GATE-S0-报告-20260609.md](./gates/GATE-S0-报告-20260609.md) |
| GATE-S1 | �?| �?| �?| �?| 2026-06-09 | [GATE-S1-报告-20260609.md](./gates/GATE-S1-报告-20260609.md) |
| GATE-S2 | �?| �?| �?| �?| 2026-06-09 | [GATE-S2-报告-20260609.md](./gates/GATE-S2-报告-20260609.md) |
| GATE-S3 | �?| �?| �?| �?| 2026-06-09 | [GATE-S3-报告-20260609.md](./gates/GATE-S3-报告-20260609.md) |
| GATE-S4 | �?| �?| �?| �?| 2026-06-09 | [GATE-S4-报告-20260609.md](./gates/GATE-S4-报告-20260609.md) |
| GATE-S5 | �?| �?| �?| �?| 2026-06-09 | [GATE-S5-报告-20260609.md](./gates/GATE-S5-报告-20260609.md) |
| GATE-S6 | �?| �?| �?| �?| 2026-06-09 | [GATE-S6-报告-20260609.md](./gates/GATE-S6-报告-20260609.md) |
| GATE-S7 | �?| �?| �?| �?| 2026-06-09 | [GATE-S7-报告-20260609.md](./gates/GATE-S7-报告-20260609.md) |

---

## 13. 阻塞与风险登�?

| 日期 | Gate | 问题 | 影响模块 | 负责�?| 状�?| 解除日期 |
|------|------|------|---------|--------|------|---------|
| | | | | | | |

---

## 14. 文档索引

| 文档 | 用�?|
|------|------|
| [BACKEND-WORK-PLAN-v1.2-已批准](./BACKEND-WORK-PLAN-v1.2-已批�?md) | 排期与范�?SSOT |
| **本文�?* | 执行进度 + Gate Checklist SSOT |
| [QUALITY-GATES](../engineering/QUALITY-GATES.md) | Slice/迭代 DoD |
| [ADR-003](../adr/ADR-003-模拟鉴权与外部平台SSO对接.md) | 鉴权 SSOT |
| [UX-AUTOMATED-TEST-REPORT](./UX-AUTOMATED-TEST-REPORT.md) | 前端基线报告 |
| `docs/delivery/CHECKLIST-M*.md` | 模块自检 |
| `docs/delivery/TESTCASES-M*.md` | 模块测试用例 |

---

*维护说明：每完成一�?Gate，更�?§1 进度�?+ §12 Sign-off + 归档 Gate 报告。禁止在 Gate 未通过时修改下一阶段状态为「开发中」�?

---

## 15. 全页自查待办（S-R20 沉淀�?026-06-10�?

> **背景**：S-R15~R17 系统走查 89 �?�?100% 覆盖。但**未来 P0 决策补后端（5 �?L-γ spec gap）、前�?mock 兜底�?API、B22 �?delete 端点、跨租户 + 权限 audit 重测**等仍待办�?

### 15.1 P0 待产品决策（不阻塞上线）

| ID | 任务 | 模块 | 决策�?| 现状 |
|----|------|------|--------|------|
| D-1 | **oa_content �?author_id**（schema drift�?| M1/M2 | ADR-008 方案 A | �?S-R21-Mike + S-R21-Donny |
| D-2 | **B13 ParamManage 后端 sys_param**（基础/采集/AI/通知�?| M9 | 是否 Phase 2 落地 | 前端 placeholder |
| D-3 | **B14 DictManage 后端 sys_dict CRUD** | M9 | 是否本期�?| 前端 placeholder |
| D-4 | **B15 LogManage 后端 sys_operation_log** | M9 | 是否本期�?| 前端 placeholder |
| D-5 | **B16 LoginLog 后端 sys_login_log** | M9 | 是否本期�?| 前端 placeholder |
| D-6 | **B17 MessageManage 后端 sys_message** | M9/M10 | 是否本期补（�?M10�?| 前端 placeholder |
| D-7 | **B22 ContentController �?delete 端点** | M2 | �?| �?S-R22-Mike |

### 15.2 P1 业务待办（上线后 1 个月内）

| ID | 任务 | 模块 | 现状 |
|----|------|------|------|
| B-1 | L1 导出 xlsx 确认 + �?Sheet 模板 | M1-M7 | �?已对�?.xlsx |
| B-2 | L2 enum literal 全量清理 | 前端 | �?enum-alias 工具已规�?|
| B-3 | L3 ADR-008 �?D-1 决策 | M1/M2 | �?�?D-1（S-R21 全闭环） |
| B-4 | L4 SOP pageNum | M2 | �?S-R11 已修 |
| B-5 | L5 seed V18-V23 CJK 终端显示 | 终端 | �?仅显示问�?|
| B-6 | B19 M8/M9 路径 prefix 一致�?| M8/M9 | �?S-R23：`/oa/system/*` 规范 + 旧别名兼�?|
| B-7 | 5 �?L-α/β/γ 页面�?exportToExcel | 前端 | �?S-R24：Efficiency / content / AccountCost / Financial / Metric + Fans 降级 |
| B-8 | L-β 5 个详情页 follow-up：assignee/userOwner 字段补全 | M3/M4 | 🟡 S-R27·27a �?· 27b~27d 全栈待办 |

### 15.3 P2 体验/工程�?

| ID | 任务 | 现状 |
|----|------|------|
| P-1 | 25+ mock 文件清理（types/*.ts enum literal�?| 不强�?|
| P-2 | 前端 console 0 error 重测（Playwright 全量�?| �?183/183（DASH-006 修后全绿�?|
| P-3 | `as any` 临时类型映射清理 | �?S-R25：unwrapApiData + 11 文件首批 |
| P-4 | Playwright 3 �?skip 测试补全 | �?S-R25：SYSTEM-001~003 3/3 �?|

### 15.4 横切（合�?/ 安全 / 鉴权�?

| ID | 任务 | 状�?| 来源 |
|----|------|------|------|
| X-1 | 5 大铁律独立审�?| �?S-R19 | S-R19 §4 |
| X-2 | 跨租户隔离全量回�?| �?GateS7E2EIT | GATE-S7 |
| X-3 | 错误码冲突修复（4 个） | �?S-R19 | S-R19 §3 |
| X-4 | 敏感字段 AES-256 加密 | �?M4 走查 | S-R3 + S-R17 |
| X-5 | 字典�?@InDict 校验 | �?6 字段对照�?| S-R12 升级 |

### 15.5 状态统�?

| 类型 | 总数 | P0 | P1 | P2 | �?�?|
|------|------|----|----|----|----|
| 待办 | **24 �?* | 5（D-2~D-6 待决策） | 6 | 4 | 9（含 D-1 后端/D-7/B-6/B-7 + 横切�?|

---

## 16. 执行模式�?026-06-10 起：单人全栈�?

> **任务 SSOT**：[TASK-PROGRESS-MASTER.md](./TASK-PROGRESS-MASTER.md)（唯一维护�? 
> 原双人个人表已归档：[Mike](./TASK-PROGRESS-MIKE.md) · [Donny](./TASK-PROGRESS-DONNY.md)（只读）

### 16.1 模式变更

| �?| 原（废止�?| 现（生效�?|
|----|-----------|-----------|
| 人员 | Mike 后端 + Donny 前端，文件域互锁 | **一�?*前后端同 slice 交付 |
| 并行 | 2 slice 并行，防冲突 | **顺序**执行，无 pull 合并负担 |
| 命名 | `S-R{N}-Mike` / `S-R{N}-Donny` | `S-R{N}` + 子项编号（如 S-R27·27b�?|
| 同步 | 三张�?+ stand-up | **一张表** + SESSION-PROGRESS |

### 16.2 单人 slice 流程

1. **一片一会话** �?不跨 slice
2. **全栈闭环** �?后端 VO/IT �?前端 types/vue/PW �?文档
3. **自测** �?`mvn test`（相�?IT�? Playwright 子集
4. **提交** �?`git pull` �?commit `S-R{N}: 简述` �?push

### 16.3 当前 Slice 队列

| Slice | 范围 | 状�?|
|-------|------|------|
| S-R21~R25 | D-1 / D-7 / B-6 / B-7 / P-3 / P-4 | �?|
| **S-R27** | B-8 详情页（27a 后端 �?· 27b~27d 全栈�?| 🔵 |
| P-2 | Playwright 全量 0 error | �?|
| S-R26 | 集成回归 + Gate | �?|
| D-2~D-6 | M9 Phase 2 决策 | �?|

### 16.4 Wave 顺序（单人）

| Wave | 内容 | 状�?|
|------|------|------|
| Wave-1 | S-R21~R24 补强 | �?|
| Wave-2 | S-R25 工程�?| �?|
| **Wave-3** | S-R27 + P-2 | 🔵 |
| Wave-4 | S-R26 + 上线决策 | �?|

### 16.5 不变量（仍适用�?

- **5 大铁�?*（选择�?/ @InDict / tenant_id / AES / 错码 1500�?504�?
- **5 段式 Prompt**（AI-IMPL-GUIDE.md�?
- **6 字段对照�?*（S-R15�?
- **PowerShell 编码�?*（S-R19）�?批量改中文走 Python UTF-8
- **MASTER §1.1 + SESSION-PROGRESS** �?slice 必同�?

---

## 17. Phase 2 Backlog（待 Gate 立项）

> **状态**：🔵 P2-M10-A Channel-A 多平台 MVP 代码+IT 完成（S-01~S-06 + V121/V122 全量 dataType + ADR-049 展示桥接 · [ADR-047](../adr/ADR-047-M4-平台账号凭证SSOT与Collector映射.md) · [ADR-049](../adr/ADR-049-M10-全量采集与展示桥接.md)）· **待正式 Gate 签收** · **不得**将 §1 Phase 2 行标为 ✅  
> **SSOT**：[ADR-045](../adr/ADR-045-M10-奥创多账号与设备同步.md) · [ADR-047](../adr/ADR-047-M4-平台账号凭证SSOT与Collector映射.md) · [ADR-048](../adr/ADR-048-M10-企微采集通道草案.md) · [ADR-049](../adr/ADR-049-M10-全量采集与展示桥接.md) · [M10-三通道采集计划](./M10-三通道采集计划.md)

### 17.1 M10 三通道采集

| ID | 通道 | 切片 | 说明 | 状态 |
|----|------|------|------|------|
| P2-M10-00 | 基建 | M10-COL-S-01~S-03 | 采集任务壳 / 日志 / 重试 | ⬜ |
| P2-M10-A | api.json | M10-API-S-01~S-06 + V121/V122 全量 dataType | `UnifiedCollectorAdapter` + bind + 多平台落库 + `CollectedDataQueryService` 桥接（[ADR-047](../adr/ADR-047-M4-平台账号凭证SSOT与Collector映射.md) · [ADR-049](../adr/ADR-049-M10-全量采集与展示桥接.md)） | 🔵 MVP 完成：stub/HTTP IT + 展示桥接 IT；**待 Gate 签收**与生产 collector 全类型人工联调 |
| P2-M10-B | 奥创 | M10-AO-S-00~S-07 | 多账号子表 · 设备同步 · 签名客户端（§4.1 ADR-045）· 好友/消息 · 桥接骨架 | ⬜ 下一切片 **S-00**（`AochuangApiClient` 签名已实现） |
| P2-M10-C | 企微 | M10-WECOM-S-01~S-04 | `WeComAdapter` + `oa_wework_account` · [ADR-048](../adr/ADR-048-M10-企微采集通道草案.md) 已采纳 | 🔵 S-01~S-04 IT 通过；待 Gate 与 M1 微信分析人工联调 |
| P2-M10-04 | 闭环 | M10-COL-S-04 · AO-S-07 · API-S-05~08 · WECOM-S-05 | 私域桥接 / 漏斗分析 | ⬜ P2 |

### 17.2 M2 微信公众号发布（与 M10 采集分离）

| ID | 切片 | 说明 | 依赖 |
|----|------|------|------|
| P2-M2-PUB-00 | — | 发布凭证 SSOT（appId/appSecret vs 采集配置复用；ADR 待写） | 产品决策 |
| P2-M2-PUB-01 | M2-PUB-WX-S-01 | `WechatOfficialPublishAdapter` + access_token 缓存 | P2-M2-PUB-00 |
| P2-M2-PUB-02 | M2-PUB-WX-S-02 | `layout_html`→图文 · 图片上传 · **`draft/add`** | P2-M2-PUB-01 |
| P2-M2-PUB-03 | M2-PUB-WX-S-03 | **`freepublish/submit`** + 异步状态轮询 | P2-M2-PUB-02 |

> **API 边界**：推草稿箱用 `draft/add`；`freepublish/submit` 仅**发布**已有草稿（非存草稿）。
