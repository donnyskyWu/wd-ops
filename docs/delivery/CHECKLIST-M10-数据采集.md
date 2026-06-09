# CHECKLIST-M10-数据采集

> **M10 自检清单** | 版本 v1.0 | 2026-06-07

---

## 1. 范围与 FR

- [ ] 5 个 Slice 全部完成
- [ ] 2 个 FR 全部实现
- [ ] 9 个 API 实现

## 2. 全局规范（🔴 必查）

- [ ] `accountId` 用 `<AccountSelect />`（强关联）
- [ ] `platformType` 用 `<DictSelect dict-type="dict_platform_type" />`
- [ ] `method` 用 `<DictSelect dict-type="dict_collect_method" />`
- [ ] `source` 用 `<DictSelect dict-type="dict_collect_source" />`
- [ ] `frequency` 用 `<DictSelect dict-type="dict_collect_frequency" />`
- [ ] `status` 用 `<DictSelect dict-type="dict_collect_status" />`
- [ ] `checkType` 用 `<DictSelect dict-type="dict_quality_check_type" />`
- [ ] `level` 用 `<DictSelect dict-type="dict_quality_level" />`
- [ ] `apiConfig` AES-256 加密
- [ ] 跨租户隔离

## 3. 中间件约束（🔴）

- [ ] **不依赖** XXL-JOB → 用 Spring `@Scheduled`
- [ ] **不依赖** RabbitMQ → 用 Spring `@Async`
- [ ] **不依赖** MinIO → 用本地文件系统

## 4. 状态机

- [ ] 采集任务 5 状态（待执行/执行中/成功/失败/部分成功）
- [ ] 指数退避重试（1/5/15min）

## 5. 测试

- [ ] 单元测试覆盖 80%+
- [ ] AC 全部通过（9 条）
- [ ] cron 表达式校验
- [ ] 加密字段测试

## 6. 文档

- [ ] PRD 同步
- [ ] Swagger 文档
- [ ] 字典项已登记

## 7. Sign-off

| 角色 | 姓名 | 日期 |
|------|------|------|
| 开发 |  |  |
| 测试 |  |  |
| 产品 |  |  |
| 架构 |  |  |


---

## 全局规范引用

> 本文档遵循 [`GLOBAL-CONVENTIONS.md`](./GLOBAL-CONVENTIONS.md) 中定义的全局规范：
> - 强关联属性 → 强制使用 5 类选择器组件（RealNameSelect / PhoneSelect / SimCardSelect / CompanySelect / AccountSelect），禁用手动输入
> - 枚举属性（方式/状态/类型/平台/阶段）→ 统一从数据字典（`dict_*`）选择，页面只读下拉
> - 跨租户 + 状态校验 → 错误码 1500-1504 统一语义
> - 数据安全 → 敏感字段（身份证/手机/API 密钥）强制脱敏展示，凭证类字段 AES-256 加密存储
> - 详见 [`GLOBAL-CONVENTIONS.md § 2`](./GLOBAL-CONVENTIONS.md) (字典)、[`§ 3`](./GLOBAL-CONVENTIONS.md) (选择器)、[`§ 4`](./GLOBAL-CONVENTIONS.md) (错误码)

