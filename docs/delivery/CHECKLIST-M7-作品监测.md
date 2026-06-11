# CHECKLIST-M7-作品监测

> **M7 自检清单** | 版本 v1.1 | 2026-06-11

---

## 1. 范围与 FR

- [ ] 4 个 Slice 全部完成
- [ ] 2 个 FR 全部实现
- [ ] 7 个 API 实现

## 2. 全局规范（🔴）

- [ ] `platformType` 用 `<DictSelect dict-type="dict_platform_type" />`
- [ ] `ipGroupId` 用 `<IpGroupTreeSelect />`
- [ ] `accountId` 用 `<AccountSelect />`
- [ ] 跨租户隔离

## 3. 业务规则

- [ ] 爆款阈值 BR-003 = 100w
- [ ] 低分阈值 BR-004 = 完播率 < 20%
- [ ] 高粉/低粉定义
- [ ] 5 个分析页「详情」按钮 + 抽屉可打开并展示行数据

## 4. 状态机

- [ ] 监测状态 3 状态

## 5. 测试

- [ ] 单元测试覆盖 80%+
- [ ] AC 全部通过（9 条）

## 6. Sign-off

| 角色 | 姓名 | 日期 |
|------|------|------|
| 开发 |  |  |
| 测试 |  |  |
| 产品 |  |  |


---

## 全局规范引用

> 本文档遵循 [`GLOBAL-CONVENTIONS.md`](./GLOBAL-CONVENTIONS.md) 中定义的全局规范：
> - 强关联属性 → 强制使用 5 类选择器组件（RealNameSelect / PhoneSelect / SimCardSelect / CompanySelect / AccountSelect），禁用手动输入
> - 枚举属性（方式/状态/类型/平台/阶段）→ 统一从数据字典（`dict_*`）选择，页面只读下拉
> - 跨租户 + 状态校验 → 错误码 1500-1504 统一语义
> - 数据安全 → 敏感字段（身份证/手机/API 密钥）强制脱敏展示，凭证类字段 AES-256 加密存储
> - 详见 [`GLOBAL-CONVENTIONS.md § 2`](./GLOBAL-CONVENTIONS.md) (字典)、[`§ 3`](./GLOBAL-CONVENTIONS.md) (选择器)、[`§ 4`](./GLOBAL-CONVENTIONS.md) (错误码)

