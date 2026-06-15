# SEED 草案：M2 公推预置版式模板

| 字段 | 值 |
|------|---|
| 版本 | v0.1 草案 |
| 日期 | 2026-06-15 |
| 状态 | **Draft · 禁止实现**（待 ADR-020 Accept + OQ-v2-11） |
| 关联 | `PROPOSAL-M2-公推模板版式套用-v2-20260615.md` §14 Q3 · `ADR-020` §2.9 |
| 范围 | 仅 **layout_schema 骨架规格**；不含 Flyway 实现、不含营销文案 |

---

## 1. 目的

为新租户/冷启动提供 **可立即套用** 的公众号版式骨架，对标：

- **135 编辑器**：样式组件序列（导读、引用、分隔、正文循环）
- **淘宝详情模块池**：卖点卡片、清单列表、购物须知式分隔

预置条目 **不得** 含可发布的完整文章或商品专属文案。

---

## 2. 交付约定（实现期）

| 项 | 约定 |
|----|------|
| 表 | `oa_wechat_layout_template` |
| `source_type` | `PRESET`（须先扩展 `dict_layout_template_source`） |
| `status` | `ENABLED` |
| `content_type` | `ARTICLE` |
| `schema_version` | `2` |
| `tenant_id` | **OQ-v2-11**：默认 `1`（与 `seed-base` 一致）；备选平台全局 `0` |
| `creator_user_id` | 系统用户（如 `1`） |
| 删除 | 禁止物理删除；允许 `DISABLED` |
| 编辑 | 建议 **仅允许复制后编辑**；原 PRESET 行只读（待产品确认） |

**Flyway 占位名**：`V80__seed_m2_layout_preset.sql`（依赖 `V79__layout_schema_v2.sql`）

---

## 3. 预置目录总览

| code | 名称 | document_type | 优先级 | 135/淘宝对标 |
|------|------|---------------|--------|--------------|
| `PRESET-01` | 公众号长文导读 | NULL（通用） | P0 | 135「标题+引用导读+正文」 |
| `PRESET-02` | 活动预告 | `PREHEAT_PREVIEW` | P0 | 公众号活动推文 |
| `PRESET-03` | 电商种草清单 | `NEW_ACCOUNT_TRAFFIC` | P1 | 淘宝「种草清单」模块 |
| `PRESET-04` | 新品卖点卡片 | NULL | P1 | 淘宝「卖点提炼/三列图标」 |
| `PRESET-05` | 赛事战报 | `POST_MATCH_REVIEW` | P0 | 体育自媒体战报体 |
| `PRESET-06` | FAQ 问答 | `OFFICIAL_PLAN` | P0 | 方案/客服 FAQ 排版 |
| `PRESET-07` | 赛后数据复盘 | `POST_MATCH_REVIEW` | P1 | 数据图表+解读体 |
| `PRESET-08` | 短视频引流贴片 | `SHORT_VIDEO_SCRIPT` | P0 | 新号引流短文 |

> **OQ-v2-11**：产品确认 P0 五款是否 = #01、#02、#05、#06、#08。

---

## 4. 全局样式基线（共用 `globalStyles`）

所有预置模板可继承以下键（各模板可覆盖）：

```json
{
  "heading2": {
    "fontSize": "18px",
    "fontWeight": "bold",
    "color": "#1a1a1a",
    "lineHeight": "1.4",
    "marginBottom": "12px"
  },
  "heading3": {
    "fontSize": "16px",
    "fontWeight": "bold",
    "color": "#333333",
    "lineHeight": "1.4"
  },
  "paragraph": {
    "fontSize": "16px",
    "color": "#333333",
    "lineHeight": "1.75",
    "marginBottom": "16px"
  },
  "quote": {
    "fontSize": "15px",
    "color": "#666666",
    "backgroundColor": "#f7f7f7",
    "borderLeft": "4px solid #07c160",
    "padding": "12px 16px",
    "lineHeight": "1.6"
  },
  "divider": {
    "borderColor": "#e5e5e5",
    "margin": "24px 0"
  },
  "image": {
    "width": "100%",
    "borderRadius": "4px"
  },
  "list": {
    "fontSize": "16px",
    "lineHeight": "1.75",
    "color": "#333333"
  }
}
```

预览渲染：槽位文本统一用 **灰块** 或固定占位「标题样式」「正文段落样式」（OQ-v2-07 选项 C）。

---

## 5. 各模板骨架规格

### PRESET-01 公众号长文导读

**用途**：日常长文、周报、行业观察。  
**document_type**：NULL  

**blocks 序列**：

| 序 | 类型 | 配置 |
|----|------|------|
| 1 | `heading` | `level: 2`, `styleRef: heading2`, `slotKind: heading` |
| 2 | `slot` | `slotKind: quote`, `styleRef: quote`, `optional: false` |
| 3 | `slot` | `slotKind: paragraph`, `styleRef: paragraph`, `repeat: true` |
| 4 | `divider` | `styleRef: divider` |
| 5 | `slot` | `slotKind: paragraph`, `styleRef: paragraph`, `repeat: true` |

**套用映射（默认）**：首段 → H2；次段 → 引用槽；其余 → 段落 repeat。

---

### PRESET-02 活动预告

**用途**：赛事/直播/线下活动预告。  
**document_type**：`PREHEAT_PREVIEW`  

**blocks 序列**：

| 序 | 类型 | 配置 |
|----|------|------|
| 1 | `heading` | `level: 2`, `styleRef: heading2`, `align: center` |
| 2 | `slot` | `slotKind: quote`, `styleRef: quote`（高亮时间地点示意槽） |
| 3 | `slot` | `slotKind: paragraph`, `styleRef: paragraph`, `repeat: true` |
| 4 | `frame` | `slotKind: image`, `styleRef: image`, `optional: true` |
| 5 | `divider` | `styleRef: divider` |
| 6 | `slot` | `slotKind: paragraph`, `styleRef: paragraph`, `repeat: true`, `maxRepeat: 2` |

---

### PRESET-03 电商种草清单

**用途**：多品推荐、好物清单、引流清单文。  
**document_type**：`NEW_ACCOUNT_TRAFFIC`  

**blocks 序列**：

| 序 | 类型 | 配置 |
|----|------|------|
| 1 | `heading` | `level: 2`, `styleRef: heading2` |
| 2 | `slot` | `slotKind: list`, `ordered: true`, `styleRef: list`, `repeat: true` |
| 3 | `divider` | `styleRef: divider` |
| 4 | `slot` | `slotKind: paragraph`, `styleRef: paragraph`, `repeat: true` |
| 5 | `frame` | `slotKind: image`, `styleRef: image`, `optional: true`, `layout: row-2` |

> `layout: row-2` 表示双图横排装饰框（实现期细化）；套用时不覆盖用户 list 条目文字。

---

### PRESET-04 新品卖点卡片

**用途**：单品上新、爆款卖点提炼。  
**document_type**：NULL  

**blocks 序列**：

| 序 | 类型 | 配置 |
|----|------|------|
| 1 | `heading` | `level: 2`, `styleRef: heading2` |
| 2 | `fixed` | `type: selling-point-row`, `columns: 3`, `styleRef: paragraph`（固定装饰，无用户文本） |
| 3 | `slot` | `slotKind: paragraph`, `styleRef: paragraph`, `repeat: true` |
| 4 | `slot` | `slotKind: quote`, `styleRef: quote`, `optional: true` |
| 5 | `divider` | `styleRef: divider` |

**说明**：`selling-point-row` 为 v2 扩展固定块（三列图标+短标签占位），每次套用均出现；列内 **不** 填商品名。

---

### PRESET-05 赛事战报

**用途**：赛后速报、比分通报。  
**document_type**：`POST_MATCH_REVIEW`  

**blocks 序列**：

| 序 | 类型 | 配置 |
|----|------|------|
| 1 | `heading` | `level: 2`, `styleRef: heading2` |
| 2 | `fixed` | `type: score-highlight`, `styleRef: quote`（比分/数据高亮框，固定装饰） |
| 3 | `slot` | `slotKind: paragraph`, `styleRef: paragraph`, `repeat: true` |
| 4 | `slot` | `slotKind: quote`, `styleRef: quote`, `optional: true` |
| 5 | `frame` | `slotKind: image`, `styleRef: image`, `optional: true` |

---

### PRESET-06 FAQ 问答

**用途**：官方方案说明、常见问答。  
**document_type**：`OFFICIAL_PLAN`  

**blocks 序列**：

| 序 | 类型 | 配置 |
|----|------|------|
| 1 | `heading` | `level: 2`, `styleRef: heading2` |
| 2 | `section` | `repeat: true`, `maxRepeat: 10`, `children`: |
|   | → | `heading` `level: 3`, `slotKind: heading`, `styleRef: heading3` |
|   | → | `slot` `slotKind: paragraph`, `styleRef: paragraph` |
| 3 | `divider` | `styleRef: divider` |

**套用映射（待 OQ-v2-05）**：默认奇数段 → H3，偶数段 → 段落；或按 Markdown `###` 识别。

---

### PRESET-07 赛后数据复盘

**用途**：数据向复盘、图表解读。  
**document_type**：`POST_MATCH_REVIEW`  

**blocks 序列**：

| 序 | 类型 | 配置 |
|----|------|------|
| 1 | `heading` | `level: 2`, `styleRef: heading2` |
| 2 | `fixed` | `type: table-placeholder`, `styleRef: paragraph`（表格装饰框，无真实数据） |
| 3 | `slot` | `slotKind: paragraph`, `styleRef: paragraph`, `repeat: true` |
| 4 | `slot` | `slotKind: quote`, `styleRef: quote`, `optional: true` |

---

### PRESET-08 短视频引流贴片

**用途**：短文案引流、关注引导。  
**document_type**：`SHORT_VIDEO_SCRIPT`  

**blocks 序列**：

| 序 | 类型 | 配置 |
|----|------|------|
| 1 | `heading` | `level: 2`, `styleRef: heading2`, `align: center` |
| 2 | `slot` | `slotKind: quote`, `styleRef: quote`, `align: center` |
| 3 | `slot` | `slotKind: paragraph`, `styleRef: paragraph`, `maxRepeat: 2` |
| 4 | `divider` | `styleRef: divider` |
| 5 | `fixed` | `type: brand-footer`, `styleRef: paragraph`（关注引导装饰条，固定文案由运营参数化 Phase 2） |

---

## 6. Seed 行元数据示例（PRESET-01）

```json
{
  "template_name": "【预置】公众号长文导读",
  "description": "适用于日常长文与导读引用，套用后保留您的正文",
  "content_type": "ARTICLE",
  "document_type": null,
  "source_type": "PRESET",
  "status": "ENABLED",
  "schema_version": 2,
  "layout_schema": { "version": 2, "globalStyles": {}, "blocks": [] }
}
```

> 实现时将 §5 对应 blocks 填入 `layout_schema`；`preview_html` 由服务端 `renderSchemaPreview()` 生成。

---

## 7. 验收要点（TESTCASES 扩展草案）

| ID | 场景 | 期望 |
|----|------|------|
| TC-M2-005-P01 | 新租户 seed 后列表 | ≥5 条 PRESET 且 `ENABLED` |
| TC-M2-005-P02 | 套用 PRESET-01 到含 body 的内容 | `body` 不变；`layout_json.version=2` |
| TC-M2-005-P03 | 删除 PRESET 模板 | 拒绝或 UI 不可见删除按钮 |
| TC-M2-005-P04 | `document_type=POST_MATCH_REVIEW` 内容 | 仅见 PRESET-05/07 + 通用模板 |

---

## 8. 工作量与维护

| 项 | 估算 |
|----|------|
| 8 款 schema 编写 + seed SQL | 0.5 会话（S-14e） |
| 字典 `PRESET` + 权限（只读原条目） | 含在上项 |
| `SeedVerificationIT` 扩展 | 含在上项 |
| 维护 | 新版 Flyway 可 **新增** PRESET 行，**不 UPDATE** 已发行 schema（避免破坏租户复制副本） |

---

## 9. 开放问题

| 编号 | 问题 | 默认 |
|------|------|------|
| OQ-v2-11a | 首批 8 款 vs P0 五款 | P0 五款 |
| OQ-v2-11b | 租户策略：per-tenant vs 全局 | per-tenant |
| OQ-v2-11c | 预置原条目是否可编辑 | 否，仅复制后编辑 |
| OQ-v2-11d | `selling-point-row` / `table-placeholder` 是否纳入 v2.0 固定块枚举 | P1，可先简化为 quote/frame |

---

*草案 · 2026-06-15 · 待产品确认 OQ-v2-11 后进入实现*
