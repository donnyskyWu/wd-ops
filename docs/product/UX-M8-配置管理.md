# UX-M8-配置管理

> **版本**：v1.0 | 2026-06-07
> **关联 PRD**：[`PRD-M8-配置管理.md`](./PRD-M8-配置管理.md)
> **全局规范**：[`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md)

---


> **视觉规范参考**：[`开发规范/UI设计与开发规范.md`](../../开发规范/UI设计与开发规范.md)（仅原型/设计阶段）
> **实现技术栈**：[`TECH-CONSTRAINTS.md § 1.2`](../engineering/TECH-CONSTRAINTS.md)（Vue 3 + Element Plus）
> **决策记录**：[`ADR-002`](../../adr/ADR-002-前端规范源选择.md)

## 1. 页面清单

| 页面 | 路由 | FR |
|------|------|-----|
| 内部采集配置 | `/config/internal-collect` | FR-M8-001 |
| 外部采集配置 | `/config/external-collect` | FR-M8-002 |
| 外部资源（数据源） | `/config/external-source` | FR-M8-003 |
| 通用采集 | `/config/general-collect` | FR-M8-004 |
| 阈值配置 | `/config/threshold` | FR-M8-005 |
| AI 模型配置 | `/config/ai-model` | FR-M8-006 |
| AI 提示词模板 | `/config/ai-prompt` | FR-M8-007 |

---

## 2. 通用规范

- 所有枚举字段 `<DictSelect />`
- 所有关联实体 `<AccountSelect />` / `<IpGroupTreeSelect />`
- 密码字段加密 + 不可见明文

---

*下一步：API / STATE / SLICES / CHECKLIST / TESTCASES。*
