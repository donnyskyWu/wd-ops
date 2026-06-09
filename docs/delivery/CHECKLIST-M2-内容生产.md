# CHECKLIST-M2-内容生产

> **M2 自检清单** | 版本 v1.0 | 2026-06-07
> **关联**：[`SLICES-M2-内容生产.md`](./SLICES-M2-内容生产.md)、[`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md)

---

## 1. 范围与 FR

- [ ] 8 个 Slice 全部完成
- [ ] 4 个 FR（FR-M2-001~004）全部实现
- [ ] 14 个 API 实现完整

## 2. 功能

- [ ] SOP 模板 CRUD（AC-M2-001-3, AC-M2-001-4）
- [ ] DAG 验证（AC-M2-001-1）
- [ ] 预置模板加载（AC-M2-001-3）
- [ ] 任务状态机 + DAG 顺序激活（AC-M2-002-1, AC-M2-002-2）
- [ ] SOP 审核 + 钉钉通知（AC-M2-001-5, AC-M2-002-4）
- [ ] 内容 CRUD + 三级审核（AC-M2-003-2, AC-M2-003-3, AC-M2-003-4）
- [ ] AI 辅助 + 自动发布（AC-M2-003-4, AC-M2-003-5）
- [ ] 知识库（AC-M2-004-1~3）

## 3. 全局规范（🔴 必查）

- [ ] SOP 节点 `executor_role` / `reviewer_role` 使用 `<DictSelect dict-type="dict_position" />`
- [ ] 内容 `contentType` / `platformType` 使用 `<DictSelect />`
- [ ] 内容 `accountId` 使用 `<AccountSelect :platformType="..." />`（联动）
- [ ] 后端校验 `account.platformType == content.platformType`（错误码 2006）
- [ ] 知识库 `isPublic` 使用 `<DictSelect dict-type="dict_yes_no" />`
- [ ] 所有 `*_id` 字段强制传 ID
- [ ] 跨租户隔离已实现
- [ ] 错误码符合 § 5

## 4. 状态机

- [ ] SOP 任务状态机（8 个状态）完整实现
- [ ] 内容三级审核状态机（8 个状态）完整实现
- [ ] SOP 模板状态机（3 个状态）完整实现
- [ ] 驳回回退路径正确

## 5. UX 一致性

- [ ] DAG 画布节点/边样式与 UX-M2-002 一致
- [ ] 任务详情时间轴与 UX-M2-004 一致
- [ ] 内容编辑器、AI 弹窗、审核弹窗与 UX-M2-007/008 一致
- [ ] Skeleton / Error / Empty 三态覆盖

## 6. 数据 & 安全

- [ ] SQL 注入防护
- [ ] 跨租户隔离
- [ ] 审核人岗位匹配校验
- [ ] 敏感数据脱敏
- [ ] AI 调用流式响应（不阻塞前端）

## 7. 性能

- [ ] DAG 验证（50 节点）≤ 500ms
- [ ] 审核列表加载 ≤ 1s
- [ ] 内容列表分页 ≤ 500ms

## 8. 测试

- [ ] 单元测试覆盖 80%+
- [ ] AC 全部通过（16 条）
- [ ] 状态机分支测试（驳回、跳过、超时）
- [ ] 关联属性强校验测试

## 9. 文档

- [ ] PRD 同步更新
- [ ] API 文档自动生成
- [ ] 字典项已登记在 GLOBAL-CONVENTIONS

## 10. Sign-off

| 角色 | 姓名 | 日期 | 签名 |
|------|------|------|------|
| 开发 |  |  |  |
| 测试 |  |  |  |
| 产品 |  |  |  |
| 架构 |  |  |  |
