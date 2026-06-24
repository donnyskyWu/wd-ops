# CHECKLIST-M1-运营管理

> **开发自检清单**：M1 运营管理每个 Slice 实现完成后必走
> **版本**：v1.0 | 2026-06-07
> **关联文档**：PRD / UX / API / STATE / SLICES（同目录）
> **使用方式**：每条 ☐ 必填，最终签字

---

## 1. 范围

- [ ] 仅实现已批准 Slice 中的 FR
- [ ] 未新增 Spec 外的 API/页面/字段
- [ ] Out of Scope 项确认未实现
- [ ] 未引入 Spec 外的中间件/依赖

## 2. Slice 完成度

- [ ] Slice-{N} 全部 FR 已实现
- [ ] 每个 FR 的所有 AC 有对应实现
- [ ] 阻塞问题清单为空（或已写明理由）

## 3. 功能（按 FR 勾选）

### FR-M1-001 IP 组管理

- [ ] AC-M1-001-1 新建大组（含正确 Toast、列表刷新）
- [ ] AC-M1-001-2 名称重复（前端 + 后端双校验）
- [ ] AC-M1-001-3 删除受保护（错误码 1005）
- [ ] AC-M1-001-4 权限校验（按钮置灰、tooltip）

### FR-M1-002 作者管理

- [ ] AC-M1-002-1 新建作者
- [ ] AC-M1-002-2 IP 组类型校验（1101）
- [ ] AC-M1-002-3 运营→主播关联

### FR-M1-003 账号分析

- [ ] AC-M1-003-1 Tab 切换（9 个平台）
- [ ] AC-M1-003-2 粉丝详情跳转（带 accountId）
- [ ] AC-M1-003-3 导出（异步任务、文件名规范）

### FR-M1-004 粉丝分析

- [ ] AC-M1-004-1 趋势图（5 个指标卡 + 折线图）
- [ ] AC-M1-004-2 导出

### FR-M1-005 作品分析

- [ ] AC-M1-005-1 爆款识别（BR-003 命中 ✅ 红色标签）

### FR-M1-006 内部内容分析

- [ ] AC-M1-006-1 补录提交
- [ ] AC-M1-006-2 审核生效（数据合并 + data_source=IMPORT）
- [ ] AC-M1-006-3 审核驳回
- [ ] AC-M1-006-4 时间窗口限制（90 天）

### FR-M1-007 人效盘点

- [ ] AC-M1-007-1 展开详情（4 个 Card）
- [ ] AC-M1-007-2 时间维度切换（week/month）

## 4. 状态机（M1 涉及）

- [ ] IP 组状态机：启用/停用/删除 三状态转移正确
- [ ] 作者状态机：删除前检查进行中任务
- [ ] **数据补录状态机**：5 个转移路径全覆盖
  - [ ] 新 → 待审核(0)
  - [ ] 待审核(0) → 审核通过(1)（不可逆）
  - [ ] 待审核(0) → 已驳回(2)
  - [ ] 已驳回(2) → 待审核(0)（修改后重提）
  - [ ] 待审核(0) → 已撤回(3)
- [ ] 数据合并逻辑：API 优先 + IMPORT 覆盖
- [ ] 运营→主播关联：日期段重叠校验

## 5. 交互

- [ ] 所有按钮文案与 UX-M1 表一致
- [ ] 空/加载/错误/无权限 4 种状态已实现
- [ ] 补录标签颜色与 UX-M1 § 10.1 一致
- [ ] 导出文件名格式 `{功能}_{yyyyMMddHHmmss}.xlsx`
- [ ] 移动端：本期不支持（已确认无响应式布局）

## 6. 数据与安全

- [ ] 权限校验与 PRD § 2.1 角色矩阵一致
- [ ] 越权按钮置灰 + tooltip "无权限"
- [ ] 敏感字段（手机/身份证/姓名）脱敏
- [ ] 所有 CUD 操作 + 状态转移写入 `oa_audit_log`
- [ ] 多租户隔离：所有查询带 `tenant_id`
- [ ] 导出需对应导出权限
- [ ] 补录记录与原作品的关联在审核通过后写入 `oa_content_daily.import_record_id`

## 7. 性能

- [ ] 列表分页 200 条 ≤ 1.5s
- [ ] IP 组树加载 ≤ 500ms
- [ ] 异步导出 ≤ 30s
- [ ] 趋势图渲染 ≤ 1s

## 8. 测试

- [ ] 单元测试覆盖率 ≥ 80%
- [ ] TESTCASES-M1 中 P0 用例已执行并通过
- [ ] 状态机 5 个转移路径有单元测试
- [ ] 数据合并逻辑有专门测试
- [ ] 权限越权有专门测试（无权限用户访问所有 4 级数据范围）

## 9. 数据

- [ ] 表 `oa_ip_group` / `oa_ip_group_member` / `oa_ip_group_account_rel` 已建
- [ ] 表 `oa_author` / `oa_author_account_rel` / `oa_ops_anchor_rel` 已建
- [ ] 表 `oa_content_data_import`（v9.1 新增）已建 + 索引 `idx_content_date` / `idx_import_type` / `idx_review_status`
- [ ] 表 `oa_content_daily` 已加字段 `import_record_id`、`data_source` 枚举
- [ ] 字典数据 `import_type`（API_EXCEPTION / ACCOUNT_BANNED / OFFLINE_PRACTICE / OTHER）已初始化

## 10. 文档

- [ ] 变更说明已写（CHANGELOG）
- [ ] Open Questions 已关闭或延期
- [ ] ADR-M1-001~003 已写入 docs/adr/
- [ ] API 接口已注册到 API 网关

## 11. 闸门签字

- [ ] 后端开发：___________
- [ ] 前端开发：___________
- [ ] 测试：___________
- [ ] 产品经理：___________

---

## 12. 常见问题 FAQ

**Q1：补录数据合并时，API 数据后到怎么办？**
A：API 数据优先（ADR-M1-001）。补录仅作为补丁。SQL 触发器或服务层需保证"先 API 后补录"时，API 仍生效。

**Q2：补录审核通过后能修改吗？**
A：不能。审核通过的记录是终态（`STATE-M1-运营管理.md § 3.4`）。如需修正，运营管理者需走"作废"流程（本期不实现）。

**Q3：运营人员能看到其他 IP 组的数据吗？**
A：默认不能。角色权限按 PRD § 2.1。运营人员仅可见自己所属 IP 组。

**Q4：数据补录是否需要计入实时统计？**
A：需要，但合并到 `oa_content_daily` 后走 T+1 汇总（与 API 数据一致）。

---

*完成所有勾选后，方可合并代码并进入下一片。*


---

## 🔴 M1 全局规范补丁（2026-06-07）

> 本模块必须严格遵循 [`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md) 的三大铁律。

### 三大铁律（必查）

#### 铁律一：关联属性必须用"选择器"，禁止手动填写

M1 中所有 `*_id` 字段必须通过选择器：

| 字段 | 选择器 |
|------|--------|
| `ip_group_id` | `<IpGroupTreeSelect />` |
| `parent_id` | `<IpGroupTreeSelect />` |
| `account_id` | `<AccountSelect />`（**强关联** ⭐，需从 M4 选择） |
| `author_id` | `<UserSelect />` |
| `assignee_id` | `<UserSelect />` |
| `anchor_user_id` | `<UserSelect />` |
| `ops_user_id` | `<UserSelect />` |
| `metric_id` | `<MetricSelect />` |

#### 铁律二：枚举属性（方式/状态/类型/平台）必须从数据字典选择

| 字段 | 字典 type |
|------|----------|
| `platform_type` | `dict_platform_type` |
| `account_type` | `dict_account_type` |
| `account_status` | `dict_account_status` |
| `ip_group_type` | `dict_ip_group_type` |
| `ip_group_status` | `dict_ip_group_status` |
| `author_type` | `dict_author_type` |
| `author_status` | `dict_author_status` |
| `content_type` | `dict_content_type` |
| `import_type` | `dict_content_import_type` |
| `data_source` | `dict_data_source` |
| `position` | `dict_position` |
| `is_primary` | `dict_yes_no` |
| `need_review` | `dict_yes_no` |
| `is_public` | `dict_yes_no` |

#### 铁律三：实体关系必须在 ER 图中明确

所有跨实体的关联已在 `PRD-M1-运营管理.md § 5 集成与数据` 中明确。

### 与 M4 账号管理的关键关联

M1 中所有平台账号必须通过 M4 的"实名人/手机/手机卡/公司"选择：

```
M1 IP 组 → M1 账号 → M4 实名人（强关联）
                     → M4 手机（强关联）
                     → M4 手机卡（强关联）
                     → M4 公司（强关联）
```

**校验**：
- 已停用实名人/手机/手机卡 不可被新账号引用（错误码 1501）
- 已绑定到其他账号的实名人/手机/手机卡 需"强制替换"（错误码 1502）
- 跨租户过滤（错误码 1504）

### 错误码

| 错误码 | 含义 |
|--------|------|
| 1500 | 关联的实体不存在 |
| 1501 | 关联的实体已停用/注销 |
| 1502 | 关联的实体已被其他记录引用 |
| 1503 | 字典值不合法 |
| 1504 | 跨租户访问禁止 |

详见 [`GLOBAL-CONVENTIONS.md § 5.3`](../engineering/GLOBAL-CONVENTIONS.md)

---

## Phase 2 · M10 采集展示桥接（ADR-049 · 2026-06-24）

- [x] PRD-M1 §4.3.5 / API-M1 桥接说明已同步
- [x] `M10CollectedDataDisplayIT` — 抖音采集表 → 账号分析 / 内部内容 API
- [ ] 五平台生产采集数据人工抽检（非 IT seed）

---

*补丁完成：M1 全套文档（M0/M1/M2/M3/M4/M5/M6/M7/M8/M9/M10）已统一遵循全局规范。*
