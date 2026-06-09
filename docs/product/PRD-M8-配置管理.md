# PRD-M8-配置管理

> **业务域**：M8 配置管理
> **功能模块**：内部采集 + 外部采集 + 阈值 + AI 模型
> **详细设计章节**：5.34
> **版本**：v1.0 | 2026-06-07
> **全局规范**：[`docs/engineering/GLOBAL-CONVENTIONS.md`](./../engineering/GLOBAL-CONVENTIONS.md)

---

## 0. 元信息

| 字段 | 值 |
|------|---|
| 模块 | M8 配置管理 |
| 业务域 | 配置（CONFIG） |
| 详细设计 | `## 5.34` |

---

## 1. 概述

管理**采集配置 + 阈值配置 + AI 模型配置**等系统级参数。包含 7 个子模块。

---

## 2. 范围

| FR 编号 | 名称 | 优先级 |
|---------|------|--------|
| FR-M8-001 | 内部采集配置（公众号/视频号/抖音/快手/小红书） | P0 |
| FR-M8-002 | 外部采集配置 | P0 |
| FR-M8-003 | 外部资源（数据源）配置 | P0 |
| FR-M8-004 | 通用采集配置 | P0 |
| FR-M8-005 | 阈值配置（爆款/低分/预警/粉丝） | P0 |
| FR-M8-006 | AI 模型配置（API Key + 模型参数） | P1 |
| FR-M8-007 | AI 提示词模板 | P1 |

---

## 3. 关键数据项

### FR-M8-001 内部采集配置

| 字段 | 控件 | 字典 |
|------|------|------|
| `platformType` | `<DictSelect dict-type="dict_platform_type" />` | 字典 |
| `accountId` | `<AccountSelect />` | `oa_account` |
| `collectFrequency` | `<DictSelect dict-type="dict_collect_frequency" />` | 字典 |
| `collectMethod` | `<DictSelect dict-type="dict_collect_method" />` | 字典 |

### FR-M8-005 阈值配置

| 字段 | 控件 | 字典 |
|------|------|------|
| `platformType` | `<DictSelect dict-type="dict_platform_type" />` | 字典 |
| `ipGroupId` | `<IpGroupTreeSelect />` | `oa_ip_group` |
| `metric` | `<Input />`（如"爆款阈值"） | - |
| `threshold` | `<InputNumber />` | - |

### FR-M8-006 AI 模型配置

| 字段 | 控件 |
|------|------|
| `modelName` | `<Select />`（GPT-4/Claude/Gemini） |
| `apiKey` | `<Input type="password" />`（加密） |
| `baseUrl` | `<Input />` |
| `maxTokens` | `<InputNumber />` |
| `temperature` | `<InputNumber :precision="2" />` |

---

## 4. 关联属性

| 字段 | 选择器 |
|------|--------|
| `platformType` | `<DictSelect dict-type="dict_platform_type" />` |
| `accountId` | `<AccountSelect />` |
| `ipGroupId` | `<IpGroupTreeSelect />` |
| `collectFrequency` | `<DictSelect dict-type="dict_collect_frequency" />` |
| `collectMethod` | `<DictSelect dict-type="dict_collect_method" />` |
| `collectStatus` | `<DictSelect dict-type="dict_collect_status" />` |

---

*下一步：UX / API / STATE / SLICES / CHECKLIST / TESTCASES。*
