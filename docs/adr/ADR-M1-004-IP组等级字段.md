# ADR-M1-004：IP 组等级字段

| 字段 | 值 |
|------|---|
| 编号 | ADR-M1-004 |
| 标题 | IP 组等级（level）字段语义 |
| 状态 | ✅ Accepted |
| 日期 | 2026-07-02 |
| 关联 PRD | `docs/product/PRD-M1-运营管理.md § 4.1.6` |
| 关联 API | `docs/engineering/API-M1-运营管理.md § 2.3–2.4` |
| 迁移 | `V128__ip_group_level.sql` |

---

## 1. 问题

FR-M1-001（IP 组管理）原 PRD / UX / API 均未定义「等级」字段。业务侧需要在 IP 组上标注 S/A/B/C 分级，用于运营识别与后续筛选扩展。

---

## 2. 决策

| 项 | 决策 |
|----|------|
| 字段名 | `level`（`oa_ip_group.level`，VARCHAR(8) NULL） |
| 字典 | `dict_ip_group_level`：S级/S、A级/A、B级/B、C级/C |
| 必填性 | **可选**（nullable）；未选时 UI 显示「-」 |
| 适用范围 | **大组与小组均可**设置，互不继承 |
| 校验 | 写入时 `@InDict("dict_ip_group_level")`；传值则必须在字典内 |
| 展示 | 树节点 Tag + 基本信息 Tab `DictLabel` + 新建/编辑弹窗 `DictSelect` |

---

## 3. 理由

- 等级为运营标注属性，不应阻塞 IP 组创建流程 → 可选
- 大组与小组均可能被单独评级 → 两者均暴露字段
- 沿用全局铁律二：枚举走字典 + `@InDict`，禁止自由文本

---

## 4. 后果

- 正面：树形列表可快速识别高优先级 IP 组
- 中性：等级**不**参与删除保护、成员/账号绑定等业务规则（本期）
- 待产品确认：是否需按等级筛选列表/树、是否小组继承大组等级、是否参与 ROI/绩效权重

---

## 5. 实现引用

- 后端：`IpGroupDO.level`，create/update/tree/detail/list VOs
- 前端：`IpGroup.vue` — 弹窗 `DictSelect`、树 Tag、详情 `DictLabel`
- 字典种子：`V128__ip_group_level.sql`
