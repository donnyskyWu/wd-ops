# OPS × Football 停写规范（B-WP1）

| 字段 | 值 |
|------|---|
| 文档性质 | **研发规范**（Phase B 立即生效；Feign 切轨过渡期内强制执行） |
| 版本 | v1.0 |
| 日期 | 2026-07-28 |
| 工作包 | [OPS-FOOTBALL-MERGE-WORK-PLAN.md §B-WP1](./OPS-FOOTBALL-MERGE-WORK-PLAN.md#b-wp1　立即停写--规范可与-phase-a-并行) |
| 关联 ADR | [ADR-056](../adr/ADR-056-Football用户身份SSOT.md) · [ADR-050](../adr/ADR-050-Ops与Football多库复用总纲.md) · [ADR-050-REV1](../adr/ADR-050-REV1-Football-G-RPC-Supersede.md) |
| 关联决策 | D-AUTHOR-01 · D-DEDUP-01 · [OPS-FOOTBALL-MERGE-DECISIONS D-PHASE-C](./OPS-FOOTBALL-MERGE-DECISIONS.md#d-phase-c执行节奏整包-vs-切片是否先-b-wp1-停写) |
| 清理明细 | [OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md §3.1](./OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md#31-停止新写--逐步废弃football-ssot) |
| Flyway 政策 | [WORK-PLAN §B-WP3](./OPS-FOOTBALL-MERGE-WORK-PLAN.md#b-wp3　flyway--seed-所有权移交)（跨库写 shenyu-* 禁止） |

---

## 1. 目的

在 OPS 后端由 `@DS` 多库直连 **过渡** 至 Feign/RPC 切轨期间，防止继续向 **非 SSOT 表** 写入数据，避免：

- 用户身份双轨（wd overlay vs shenyu-system）污染 ADR-056
- 作者主数据与 Football Admin 分叉（D-AUTHOR-01）
- 字典/系统主数据在 wd 与 shenyu-system 双维护
- OPS Flyway 继续跨库灌入 Football 库

**读路径**：过渡期内仍允许 `@DS("system"|"member"|"mp"|"pay")` **只读**（直至对应 G-* cutover 验收后删除）。  
**写路径**：本节所列对象 **禁止新写**；业务数据应写入 `wd` 自建表或经 Feign 写 Football SSOT。

---

## 2. 禁止新写（Forbidden）

### 2.1 用户身份 — `wd.system_users` / legacy `sys_user`

| 项 | 说明 |
|----|------|
| **禁止** | 向 `wd.system_users` overlay **INSERT/UPDATE** 作为新用户身份 SSOT；向 legacy `sys_user` / `sys_user_token` **新写**生产路径数据 |
| **SSOT** | **shenyu-system `system_users.id`**（[ADR-056 D1/D6](../adr/ADR-056-Football用户身份SSOT.md)） |
| **写入** | `UserSelect` / API 提交的用户 id 须 `FootballSystemUserValidator.resolveStorableUserId` 后持久化为 **shenyu id** |
| **禁止** | 将 UserSelect id normalize 为 wd master id 再存储（ADR-056 D6） |
| **legacy 用途** | `sys_user` 等 **仅** H2 IT / standalone harness fallback；生产停用新写 |
| **依据** | ADR-056 · CLEANUP §3.1 · WORK-PLAN B-WP1 |

### 2.2 作者主数据 — `wd.oa_author`（非 ext）

| 项 | 说明 |
|----|------|
| **禁止** | 向 `wd.oa_author` **INSERT/UPDATE** 新作者主数据行 |
| **允许** | 只读历史；运营扩展 **仅写** `wd.oa_author_ext` |
| **SSOT** | Football member `author_user`；管理 CUD 归 Football Admin（**D-AUTHOR-01**） |
| **依据** | D-AUTHOR-01 · ADR-049/051 · CLEANUP §1.5 · §3.1 |

### 2.3 字典 — `wd.sys_dict_*` / 平行字典表

| 项 | 说明 |
|----|------|
| **禁止** | 新业务字典 **仅写** `wd.sys_dict_type` / `wd.sys_dict_data` 作为 SSOT |
| **正确路径** | 字典变更经 **Football Admin** 或 **shenyu-system** 运维脚本维护 |
| **过渡** | 已 merge 至 shenyu-system 的字典可读 wd 存量；**新变更勿只写 wd** |
| **后端** | `@InDict` 读路径切 G-DICT-01 Feign 前仍可读 @DS；**管理写** 已 410 / 菜单已摘 |
| **依据** | CLEANUP §1.4 · §3.1 · V152/V158 历史迁移（勿再仿写） |

### 2.4 OPS Flyway 跨库写 shenyu-*

| 项 | 说明 |
|----|------|
| **禁止** | OPS Flyway 新 migration 中 `INSERT`/`UPDATE`/`DELETE` 指向 `` `shenyu-system`.* ``、`` `shenyu-member`.* ``、`` `shenyu-mp`.* ``、`` `shenyu-pay`.* `` |
| **历史** | V152 · V158 · V137 等 **保留**（已执行）；**禁止**新增同类脚本 |
| **菜单 seed** | OPS **6100–6999** 菜单 seed **可保留**；勿再同步 Football 原生菜单树 |
| **详细政策** | 见 [WORK-PLAN §B-WP3](./OPS-FOOTBALL-MERGE-WORK-PLAN.md#b-wp3　flyway--seed-所有权移交) · Football 库 schema/seed 由 football-backend-saas Flyway 负责 |
| **依据** | CLEANUP §3.3 · ADR-050 数据归属原则 |

### 2.5 跨 Football 库直连写（member / mp / pay）

| 项 | 说明 |
|----|------|
| **禁止** | OPS 经 `@DS("member"|"mp"|"pay")` **新写** `author_user`、`author_article`、`mp_account`、`pay_all_order` 等 Football SSOT 表（切轨完成前若仍存写 Mapper，**不得扩大**写范围） |
| **目标** | 写路径改 G-MEM-03 / G-MP-01 Feign；读路径 G-PAY-01 等 |
| **依据** | ADR-050-REV1 白名单 · MUST-HAVE §1 · CLEANUP §3.1 |

### 2.6 操作日志本地双写（计划停写）

| 项 | 说明 |
|----|------|
| **禁止（新）** | 向 `wd.sys_operation_log` **新增**双写（`OperationLogRecorder` 本地写） |
| **SSOT** | `OperateLogCommonApi` Feign 写 shenyu-system（已接入 AL-05） |
| **时机** | 确认无读依赖后与 C-WP0 物理删除联动 |
| **依据** | CLEANUP §1.8 · WORK-PLAN B-WP1 任务清单 |

---

## 3. 允许（Allowed）

| 类别 | 说明 |
|------|------|
| **@DS 只读** | 过渡期内 `@DS("system"|"member"|"mp"|"pay")` **SELECT**；Feign 双跑失败时回退读（设计如此，见 D-FEIGN-IT） |
| **wd 业务表写** | 所有 OPS 自建域：`oa_*`、`oa_*_ext`、IP 组、SOP/任务、绩效、计划、内容编排、`sys_param` 等（MUST-HAVE §6） |
| **Feign 写 Football SSOT** | 经 MUST-HAVE §7 白名单 G-* RPC（如 `ArticleApi`、`MpAccountInfoApi`）— **推荐**方向，替代 @DS 写 |
| **H2 IT fallback** | 集成测试 profile 下 legacy `sys_user` 等 **仅** 测试数据写入 |
| **历史数据读** | username 桥接、`resolvePresentableUserId` 兼容存量 wd/legacy id（ADR-056 §5） |

---

## 4. PR Review Checklist（复制到 PR 描述）

```markdown
### B-WP1 停写自检（Feign 过渡期内必填）

- [ ] **未**向 `wd.system_users` / legacy `sys_user` 新增生产身份写入（ADR-056）
- [ ] 新增/修改 `*_user_id` 写入已调用 `resolveStorableUserId`；回显已用 `resolvePresentableUserId`
- [ ] **未**向 `wd.oa_author` 新增主数据行；作者扩展仅写 `oa_author_ext`（D-AUTHOR-01）
- [ ] **未**仅写 `wd.sys_dict_*` 作为字典 SSOT；字典变更走 Football/shenyu-system
- [ ] Flyway migration **未**含跨库写 `` `shenyu-*`.* ``（B-WP3）
- [ ] **未**扩大 `@DS("member"|"mp"|"pay")` 写 Football SSOT 表的范围
- [ ] **未**新增 `sys_operation_log` 本地双写
- [ ] 新增写路径若跨 Football 域，已优先 Feign（G-* 白名单）而非新 @DS 写
```

---

## 5. 执行与抽检

| 手段 | 说明 |
|------|------|
| **Code Review** | 所有涉及用户/作者/字典/Flyway/多库 Mapper 的 PR 必须勾选 §4 |
| **抽检 SQL** | 定期（或 Gate 前）对 `wd.system_users`、`wd.oa_author` 查 `MAX(create_time)` / 行数增量 |
| **可选 CI grep** | 后续可在 CI 增加禁止模式扫描（见 §6） |
| **违规处理** | 阻塞合并；若已入库需评估回滚或 Football 侧同步 |

---

## 6. 可选 CI / 本地 grep 模式（后续）

> 当前 **不强制** 接入 CI；供 Reviewer 或 pre-commit 手工运行。

```bash
# 示例：Flyway 跨库写（新 migration）
rg -n "INSERT INTO \`shenyu-(system|member|mp|pay)\`" ops-platform-server/**/db/migration/

# 示例：oa_author 写 Mapper（应仅 ext 或只读）
rg -n "oa_author[^_]" ops-platform-server/**/java/ --glob '!*Ext*'

# 示例：SysUserMapper 作唯一写入校验（应走 FootballSystemUserValidator）
rg -n "SysUserMapper.*selectById" ops-platform-server/**/service/
```

---

## 7. 与 Phase C cutover 的关系

| 阶段 | 停写规范 | 删 @DS |
|------|----------|--------|
| **现在（双跑）** | **强制** | **禁止**删除 multidb / Mapper |
| **G-* Integration 手验绿** | 仍强制 | 可单片删除对应 `@DS` |
| **C-WP7 终态** | 仍禁止 SSOT 污染 | 仅 `wd` + Feign |

手验清单：[OPS-FOOTBALL-INTEGRATION-FEIGN-CHECKLIST.md](./OPS-FOOTBALL-INTEGRATION-FEIGN-CHECKLIST.md)

---

## 8. 维护

1. B-WP1 验收：本文发布 + WORK-PLAN §B-WP1 勾选 + 至少一次 PR 抽检记录。
2. 与 B-WP3 Flyway 政策同步更新（跨库写条款一致）。
3. cutover 完成后停写规范 **仍有效**（防止回退污染 SSOT）。

---

**版本** v1.0 · **日期** 2026-07-28 · **状态** 已发布（B-WP1 规范 SSOT）
