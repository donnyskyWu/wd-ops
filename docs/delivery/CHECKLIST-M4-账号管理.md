# CHECKLIST-M4-账号管理

> **M4 自检清单** | 版本 v1.0 | 2026-06-07
> **关联**：[`SLICES-M4-账号管理.md`](./SLICES-M4-账号管理.md)、[`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md)
> **🔴 关键模块**：强关联 + 数据字典 + 加密

---

## 1. 范围与 FR

- [ ] 9 个 Slice 全部完成
- [ ] 7 个 FR（FR-M4-001~007）全部实现
- [ ] 35+ 个 API 实现完整
- [ ] 5 个强关联选择器组件

## 2. 功能

- [ ] 公司管理 + 公众号容量自动统计（AC-M4-001-1, 2, 3, 4）
- [ ] 实名人 CRUD + AES-256（AC-M4-002-1, 3, 4）
- [ ] 中介人 1 对多 + 佣金脱敏（AC-M4-002-2, 5）
- [ ] 手机 + 手机卡 + 跨平台聚合（AC-M4-003, 004 全部）
- [ ] **平台账号强关联**（AC-M4-005 全部 12 条）
- [ ] 个人账号（AC-M4-006 全部 3 条）
- [ ] 三方关联（AC-M4-007 全部 3 条）

## 3. 全局规范（🔴🔴 必查）

### 3.1 强关联属性选择器（必查）

- [ ] **FR-M4-005** 中 `companyId` 使用 `<CompanySelect />`
- [ ] **FR-M4-005** 中 `realnameId` 使用 `<RealNameSelect />`
- [ ] **FR-M4-005** 中 `phoneId` 使用 `<PhoneSelect />`
- [ ] **FR-M4-005** 中 `simCardId` 使用 `<SimCardSelect />`
- [ ] **FR-M4-005** 中 `intermediaryId` 使用 `<RealNameSelect />`（复用实名人表）
- [ ] **FR-M4-005** 中 `ipGroupId` 使用 `<IpGroupTreeSelect />`
- [ ] **所有** 选择器禁用手动输入
- [ ] **所有** 选择器跨租户过滤

### 3.2 字典字段

- [ ] `platformType` 用 `<DictSelect dict-type="dict_platform_type" />`
- [ ] `accountType` 用 `<DictSelect dict-type="dict_account_type" />`
- [ ] `accountStatus` 用 `<DictSelect dict-type="dict_account_status" />`
- [ ] `realnameStatus` 用 `<DictSelect dict-type="dict_realname_status" />`
- [ ] `idType` 用 `<DictSelect dict-type="dict_id_type" />`
- [ ] `gender` 用 `<DictSelect dict-type="dict_gender" />`
- [ ] `phoneStatus` 用 `<DictSelect dict-type="dict_phone_status" />`
- [ ] `simStatus` 用 `<DictSelect dict-type="dict_sim_status" />`
- [ ] `companyStatus` 用 `<DictSelect dict-type="dict_company_status" />`
- [ ] `isPrimary` 用 `<DictSelect dict-type="dict_yes_no" />`
- [ ] `relationType` 用 `<DictSelect dict-type="dict_intermediary_relation" />`
- [ ] `operator` 用 `<DictSelect dict-type="dict_sim_operator" />`

### 3.3 强校验

- [ ] 已停用实名人/手机/手机卡 不可被新账号引用（错误码 1501）
- [ ] 跨租户实名人/手机 不可选（错误码 1504）
- [ ] 已绑定实名人被新账号选择时弹"强制替换"确认框
- [ ] 强制替换需 `reason` 字段（错误码 2014）
- [ ] 强制替换记录到 `sys_audit_log`

## 4. 状态机

- [ ] 平台账号状态机（4 状态）完整
- [ ] 实名人 / 手机 / 手机卡 / 公司 状态机正确
- [ ] 状态变更自动同步数据（BANNED → NORMAL 触发重新拉取）

## 5. UX 一致性

- [ ] 5 个强关联选择器样式统一
- [ ] 跨平台账号聚合侧滑（UX-M4-007）
- [ ] 强制替换弹窗（UX-M4-009 § 7.4）
- [ ] 中介人佣金脱敏显示
- [ ] 奥创接口只读脱敏
- [ ] Skeleton / Error / Empty 三态

## 6. 数据 & 安全

- [ ] AES-256 加密：身份证 / Cookie / ICCID / 凭证
- [ ] 字段脱敏：身份证 / 手机 / ICCID / 中介人佣金
- [ ] 跨租户隔离
- [ ] SQL 注入防护
- [ ] 审计日志：所有 CRUD + 强制替换

## 7. 性能

- [ ] 跨平台账号聚合 ≤ 1s（10 个账号）
- [ ] 列表分页（1000 条）≤ 500ms
- [ ] 强关联选择器远端搜索 ≤ 300ms

## 8. 测试

- [ ] 单元测试覆盖 80%+
- [ ] AC 全部通过（24 条）
- [ ] **🔴 强关联测试**（必查）：
  - [ ] 传 `name` 而非 `id` → 错误码 1500
  - [ ] 跨租户 → 错误码 1504
  - [ ] 已停用 → 错误码 1501
  - [ ] 已绑定 → 默认失败
  - [ ] 强制替换 → 成功 + 审计
- [ ] 跨平台账号聚合准确性
- [ ] 加密字段数据库存储为密文

## 9. 文档

- [ ] PRD 同步
- [ ] Swagger 文档
- [ ] 字典项已登记
- [ ] 选择器组件文档

## 10. 五选择器联动测试（🔴 M4 重点）

> 本节专门验证 M4 模块的 5 个强关联选择器联动逻辑。每项**必须**勾选。

### 10.1 单选择器功能

- [ ] `<RealNameSelect />` 只显示本租户 + 启用状态
- [ ] `<RealNameSelect />` 默认过滤"已绑定"实体
- [ ] `<PhoneSelect />` 同上
- [ ] `<SimCardSelect />` 同上
- [ ] `<CompanySelect />` 同上
- [ ] `<AccountSelect />` 同平台过滤

### 10.2 联动功能

- [ ] 选 Realname 后，Phone 仅显示该 Realname 绑定的
- [ ] 选 Phone 后，SimCard 仅显示该 Phone 关联的
- [ ] Realname 切换触发 Phone/SimCard 联动重置

### 10.3 强制替换流程

- [ ] 选择"已绑定"实名人 → 弹出"强制替换"确认框
- [ ] 确认 → 提示填 `reason`（最小 5 字）
- [ ] 提交 → 旧账号解绑 + 新账号绑定 + 审计日志
- [ ] 未填 reason → 错误码 1500
- [ ] `forceReplace=false` 选已绑定 → 错误码 1502

### 10.4 状态机联动

- [ ] 账号创建成功 → Realname 状态 = `BOUND`
- [ ] 账号删除 → Realname 状态 = `UNBOUND`
- [ ] 强制替换 → 旧 UNBOUND + 新 BOUND 同步触发

### 10.5 错误码验证

- [ ] Realname 跨租户 → 1504
- [ ] Realname 已停用 → 1501
- [ ] platformType 字典非法 → 1503
- [ ] forceReplace=true 但无 reason → 1500

## 10. Sign-off

| 角色 | 姓名 | 日期 | 签名 |
|------|------|------|------|
| 开发 |  |  |  |
| 测试 |  |  |  |
| 产品 |  |  |  |
| 架构 |  |  |  |
| 安全 |  |  |  | ← 重点（加密/脱敏）
