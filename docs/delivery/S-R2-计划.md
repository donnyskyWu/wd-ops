# S-R2 计划：8 个分析页 + 字典修复（2026-06-09）

> **触发**：用户在保存知识修复后又跑 8 个分析/字典类目，反馈"还存在以下问题"。
> **前置**：S-R1 P0 修复 + P-GATE-UNMOCK-R（13 项 🟡 待办）已识别未修
> **范围**：本次 S-R2 把其中最严重、影响 UAT 走查的 8 项一次性修掉

---

## 1. 根因清单

| # | 用户反馈 | 实际根因 | 切片 |
|---|---------|---------|------|
| Q1 | 搜索条件窄、应默认"全部" | `TableSearch` 缺 `width=220` 默认值；select 没 `default-first-option` | S-R2-A |
| Q2 | 账号分析 粉丝/作品详情不能点 | 后端 `AccountAnalysisController` **缺** `/{id}/followers` 和 `/{id}/contents` 接口（spec API-M1 §4.1 有定义）；前端 `handleViewDetail` 只是 `ElMessage.info` 没跳转 | S-R2-B |
| Q3 | 粉丝分析是 mock | 前端 `api/follower.ts` 路径是 `/oa/follower/...`，后端实为 `/oa/follower-analysis/...`（spec §4.2）；前端兜底 mock | S-R2-C |
| Q4 | 作品分析无数据 | 前端 `types/works.ts` VO 字段名（`viewCount/shareCount/isViral`）与后端 VO（`readCount/forwardCount/isHit`）不一致；后端 `ContentAnalysisController` 只 `list/stats`，缺 `/trend`（spec §4.3 有） | S-R2-D |
| Q5 | 内部内容 补录类型字典为空 + 平台切换数据不变 | ① 前端用 `dict_import_source`，后端真实 dict_type 是 `dict_content_import_type`（实测）；② 页面完全是硬编码 mock，`loadData` 是空函数 | S-R2-E |
| Q6 | 人效盘点都是 mock | Efficiency.vue 4 张表全硬编码（wechatList/videoList/fansList/vipList）；后端 `/productivity-review/list` 有 5 条真实数据，但前端根本没接 | S-R2-F |
| Q7 | SOP 不能点击 | SOP 后端**完整**（35 个 java 文件），前端路由 /api/OK；问题在前端 `sop/index.vue` 的 `handleEdit` 跳转后报 404 或后端 `SopTemplateController` 收 `pageNum/pageSize` 而前端用 `pageNo/pageSize` 导致列表为空 → 看上去"不能点击" | S-R2-G |
| Q8 | 成本类型字典没有 | 后端 `dict_cost_type` 真实存在 3 条（实测 PURCHASE/PROCESS_HUMAN/AD_SPEND），前端 `DictSelect dict-type="dict_cost_type"` 正确。需用户在浏览器手动看；可能 `DictSelect` 列表 label/value 渲染有问题（GBK 编码显示乱码） | S-R2-H |

---

## 2. 切片划分

| 切片 | 范围 | 文件数 | 风险 | 状态 |
|------|------|--------|------|------|
| S-R2-A | Q1 `TableSearch` 宽度 + 默认全部 | 1 组件 | 0 | 待办 |
| S-R2-B | Q2 详情接口补齐 | 1 Controller | 中 | 待办 |
| S-R2-C | Q3 粉丝分析接真 | 1 API + 1 页 | 0 | 待办 |
| S-R2-D | Q4 作品分析字段对齐 + /trend 补齐 | 1 VO + 1 API + 1 页 + 1 Controller | 中 | 待办 |
| S-R2-E | Q5 内部内容接真 | 1 dict verify + 1 页改 | 0 | 待办 |
| S-R2-F | Q6 人效接真 | 1 API + 1 页改 | 0 | 待办 |
| S-R2-G | Q7 SOP 分页参数对齐 | 1 前端 API | 0 | 待办 |
| S-R2-H | Q8 字典 GBK 乱码排查 | 1 DictSelect | 0 | 待办 |

切片顺序：S-R2-A → S-R2-H（先排查小问题）→ S-R2-C/E/F（前端接真）→ S-R2-D（VO 对齐 + trend 补）→ S-R2-B（详情接口）→ S-R2-G（SOP）

---

## 3. Spec 参照

- `docs/engineering/API-M1-运营管理.md` §4.1–4.5 — 5 个分析页 API + DTO
- `docs/engineering/API-M1-运营管理.md` §2.4 — IP组 parentId（S-R1 已修）
- `docs/product/PRD-M1-运营管理.md` — 字段/校验
- `docs/engineering/API-M11-字典管理.md` §2.1 — 字典 type 查询端点

---

## 4. 风险与豁免

- **后端补 `/trend` 等接口**：需要新增 service 方法 + IT 覆盖。**每个补的端点必须有 IT**（M0 质量门）。
- **前端 mock 清除**：按 P-GATE-UNMOCK 策略 = real-or-empty（real 失败就空列表 + ElMessage.error），不再兜底。
- **Q7 SOP**：先实测 `/sop/template/list?pageNo=1&pageSize=10` 真实行为再决定改前端还是后端。

---

## 5. 完成定义

- [ ] 8 个用户反馈点全部消除（截图 + curl 验证）
- [ ] 后端新增端点有 IT 覆盖
- [ ] 前端 `pnpm tsc --noEmit` 无新增错误
- [ ] 后端 `mvn verify` 全绿
- [ ] 写 S-R2 报告

---

## 6. 关联

- 前置：P-GATE-UNMOCK-R（13 项 🟡 中本批次覆盖 8 项）
- 后续：剩余 5 项（报表导出、IpGroup KPI、AccountCostManage 死代码、AuthorDashboardVO 类型、`import.meta.env` 类型等）纳入 P-GATE-UNMOCK-R2
