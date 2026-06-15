# CHECKLIST-M2-内容生产

> **M2 自检清单** | 版本 v1.3 | 2026-06-13
> **关联**：[`SLICES-M2-内容生产.md`](./SLICES-M2-内容生产.md)、[`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md)

---

## 1. 范围与 FR

- [ ] 13 个 Slice 全部完成（含 S-10~S-13 需求 2–6）
- [ ] 5 个 FR（FR-M2-001~004 + FR-M2-009）全部实现
- [ ] 22 个 API 实现完整（含计划 9 个）

## 2. 功能

- [ ] SOP 模板 CRUD（AC-M2-001-3, AC-M2-001-4）
- [ ] DAG 验证（AC-M2-001-1）
- [ ] SOP 编辑页栅格 4+14+6 布局
- [ ] 保存 `persistNodes` create/update + ID remap 连线正确
- [ ] 节点删除无后端 API（已知缺口，文档已标注）
- [ ] 预置模板加载（AC-M2-001-3）
- [ ] 任务状态机 + DAG 顺序激活（AC-M2-002-1, AC-M2-002-2）
- [ ] SOP 审核 + 钉钉通知（AC-M2-001-5, AC-M2-002-4）
- [ ] 内容 CRUD + **可配置二级审核**（ADR-017；AC-M2-003-2~4 适配）
- [x] AI 辅助 + 真实 LLM `ai-generate`（M8 模型+提示词）
- [x] 内容列表弹窗创作；无封面；可选多平台/账号；IP 组必填
- [x] 内容审核 Tab 一级/二级；IP 组长范围；审核流程 steps 展示
- [x] 计划步骤多赛事 + 单执行人；详情含 tasks 表
- [x] 任务列表「我的任务」默认 Tab；计划起止时间列
- [x] 任务执行 ContentEditDialog；提交审核（非确认）；待审只读
- [x] 移除重复 breadcrumb；内容创作菜单隐藏
- [x] 内容列表 exportToExcel（ADR-018）
- [x] M9 内容审核系统参数 Tab + 角色下拉
- [ ] 知识库（AC-M2-004-1~3）
- [ ] **S-09 计划管理**（AC-M2-009-1~5）：草稿任务隐藏 / 启动可见 / 组长终止审批 / 草稿编辑
- [x] 计划草稿 `PUT /plan/update` + 前端编辑链路
- [x] 内容列表页接入真实 `/oa/content/list` API
- [x] `CONTENT_PUBLISH` 执行页发布指引（完成=普通节点，BLK-M2-009 占位）
- [ ] `M2PlanS09IT` 通过
- [x] **S-10 节点类型**（AC-M2-001-6）：`dict_sop_node_type` 三值 + 属性面板
- [x] **S-11 步骤赛事**（AC-M2-009-4）：步骤/任务 `competition_id` 继承
- [x] `M2PlanS11IT` 通过
- [x] **S-12 任务执行页**（AC-M2-002-5~7）：我的任务「执行」+ 内容完成门禁 2008
- [x] `M2TaskS12IT` 通过
- [x] **S-13 任务内容编辑**（AC-M2-003-6~10）：文档类型 / 文案引用 / 保存确认 COMPLETED
- [x] `M2ContentS13IT` 通过
- [x] **S-14 公推模板库**（AC-M2-005-1~7）：模板 CRUD / 导入 / 应用模板 / 富版式正文
- [x] **S-14b** `layout_schema` + `extractLayoutSchema` + V79 迁移
- [x] **S-14c** `LayoutMergeService` + apply/preview merge API + 错误码 2036~2038
- [x] **S-14d** 前端骨架预览、套用预览、导入免责声明
- [x] **FR-143** ARTICLE 正文 `RichTextEditor` 统一（2026-06-15）
- [x] **FR-147** 模板编辑页同源 `RichTextEditor`（2026-06-15）
- [x] **FR-145** V87 图文混排预置模板 seed（2026-06-15）
- [x] 编辑器全屏/版式侧栏默认收起/图片宽度/微信 HTML（ADR-021 §5）
- [x] **S-14e** V80 五款 PRESET seed + IT 修订
- [x] `M2LayoutTemplateS14IT` 通过（merge 保真 P0）
- [x] ADR-019 **Accepted**（§导入/§套用由 ADR-020 supersede）
- [x] ADR-020 **Accepted**
- [x] **BLK-M2-008** 执行说明：`instruction_text` + SOP 编辑 + 执行页展示
- [x] **BLK-M2-007** 附件只读：`attachment_urls` JSON + Seed + 执行页链接（无上传 API）
- [ ] ADR-016 其余阻塞项（BLK-M2-004~006、009~011）已产品确认或标注延期

## 3. 全局规范（🔴 必查）

- [ ] SOP 节点 `executor_role` / `reviewer_role` 使用 `<DictSelect dict-type="dict_position" />`
- [x] SOP 节点 `node_type` 使用 `<DictSelect dict-type="dict_sop_node_type" />`
- [x] 计划步骤 `competition_id` 使用选择器（非手输外部 ID）
- [x] 任务驱动内容 `document_type` 使用 `<DictSelect dict-type="dict_document_type" />`
- [ ] 内容 `contentType` / `platformType` 使用 `<DictSelect />`
- [ ] 内容 `accountId` 使用 `<AccountSelect :platformType="..." />`（联动）
- [ ] 后端校验 `account.platformType == content.platformType`（错误码 2006）
- [ ] 知识库 `isPublic` 使用 `<DictSelect dict-type="dict_yes_no" />`
- [ ] 计划 `templateId` / `ipGroupId` / `assigneeIds` 使用选择器（非手输 ID）
- [ ] 计划 `status` 使用 `<DictSelect dict-type="dict_plan_status" />`
- [ ] 所有 `*_id` 字段强制传 ID
- [ ] 跨租户隔离已实现
- [ ] 错误码符合 § 5

## 4. 状态机

- [ ] SOP 任务状态机（含 `PLAN_DRAFT` / `TERMINATED` 扩展）完整实现
- [ ] 计划状态机（DRAFT / IN_PROGRESS / TERMINATE_PENDING / TERMINATED）完整实现
- [ ] 内容三级审核状态机（8 个状态）完整实现
- [x] **可配置二级审核**状态机（ADR-017）+ 遗留 FINAL 兼容
- [ ] SOP 模板状态机（3 个状态）完整实现
- [ ] 驳回回退路径正确

## 5. UX 一致性

- [ ] DAG 画布节点/边样式与 UX-M2-002 一致
- [ ] 任务详情时间轴与 UX-M2-004 一致
- [ ] 内容编辑器、AI 弹窗、审核弹窗与 UX-M2-007/008 一致
- [ ] 计划新增弹窗与 UX-M2-011 一致
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
- [ ] AC 全部通过（30 条，含 AC-M2-001-6、AC-M2-009-4、AC-M2-002-5~7、AC-M2-003-6~10）
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
