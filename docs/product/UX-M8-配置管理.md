# UX-M8-配置管理



> **版本**：v2.1 | 2026-06-11

> **关联 PRD**：[`PRD-M8-配置管理.md`](./PRD-M8-配置管理.md)

> **ADR**：[`ADR-014`](../adr/ADR-014-M8-配置管理数据模型.md)



---



## 1. 页面清单



| 页面 | 路由 | FR |

|------|------|-----|

| 内部采集配置 | `/config/internal-collect` | FR-M8-001 |

| 外部采集配置 | `/config/external-collect` | FR-M8-002 |

| 外部数据配置 | `/config/external-source` | FR-M8-003 |

| 订单采集配置 | `/config/order-collect` | FR-M8-004 |

| 阈值规则配置 | `/config/threshold` | FR-M8-005 |

| AI 模型配置 | `/config/ai-model` | FR-M8-006 |

| AI 提示词配置 | `/config/ai-prompt` | FR-M8-007 |

| 元数据维护 | `/config-metadata` | FR-M8-008 |

| 租户采集凭账号 | `/config-external-collect` 子 Tab 或 `/config-tenant-credential`（P1+） | FR-M8-002 / CFG-013a |



---



## 2. 内部采集配置 UX



### 2.1 布局（按 Tab 分支）



**平台类 Tab**（公众号/抖音/快手/视频号/服务号）：



```

[Alert 说明]

[平台 Tab: 公众号|抖音|快手|视频号|服务号|企微|个微]

[搜索: 账号名称 + 状态]

[新增配置]

[表格: ID | 平台账号 | 账号标识 | APPID | 直播号 | 状态 | 更新时间 | 操作]

[编辑弹窗: AccountSelect + 凭证字段]

```



**企业微信 Tab**：



```

[WeworkAppConfigPanel]  — 与账号管理·个人账号·企微应用配置同源

```



**个人微信 Tab**：



```

[奥创接口配置表单]  — 仅 apiUrl/appId/appSecret/token，无账号列表

```



### 2.2 组件复用



| 组件 | 用途 |

|------|------|

| `<AccountSelect />` | 平台 Tab 选 M4 平台账号 |

| `<WeworkAppConfigPanel />` | 企微 Tab 应用配置（共享 `PersonalAccountManage`） |



### 2.3 表单校验



平台 Tab：`accountId` 必选；APPSECRET 脱敏展示。



---



## 3. 外部采集配置 UX



### 3.1 Tab



外部账号 | 关键词配置



### 3.2 外部账号 Tab



列：ID | 平台(`dict_third_platform`) | 账号名称 | 账号标识 | 状态 | 更新时间 | 操作



工具栏：[新增] [批量导入 CSV] [批量删除]



### 3.3 关键词 Tab



列：ID | 平台(`dict_platform_type`) | 关键词 | 匹配类型 | 状态 | 操作



### 3.4 租户采集凭账号 Tab（P1+ · ADR-052）

列：平台 | profile | 展示名 | 连接状态 | 过期时间 | 状态 | 操作

表单：Cookie/Token（脱敏录入）、`expire_at`、探活按钮；**禁止**在外部账号行内嵌密钥。



---



## 4. 元数据维护 UX（FR-M8-008）

路由：`/config-metadata` · 组件 `MetadataManage.vue`

- 实体列表 + 新增/编辑/删除（删除仅超级管理员）
- 新增弹窗：未映射表下拉 `{tableName} ({tableComment})`；选中后默认填充实体名称
- 字段配置抽屉：`query_condition_type`、`dict_type`、`selector_config`

---



## 5. 订单采集配置 UX



列：ID | 名称 | 主机 | 端口 | 数据库名 | 表名 | 采集模式 | 连接状态 | 状态 | 操作(编辑/连接测试/启用/删除)



---



## 5. 阈值规则配置 UX



Tab：预警阈值 | 粉丝阈值 | 作品阈值 | 账号覆盖



各 Tab **独立表格列 + 独立弹窗字段**（非通用表单）：



| Tab | 弹窗关键字段 |

|-----|-------------|

| 预警 | 指标、平台、阈值类型、比较符、阈值、通知渠道 |

| 粉丝 | 平台、低粉/高粉、日增低粉/日增高粉 |

| 作品 | 平台、内容类型、指标、爆款/低分、判定模式 |

| 覆盖 | 平台 + `AccountSelect`、指标、覆盖值 |



---



## 6. AI 模型 / 提示词 UX



（同 v2.0：统计卡片、连接测试、设默认、版本号、查看详情）



---



## 7. 通用规范



- 状态筛选：`dict_config_status`

- 外部账号平台：`dict_third_platform`；关键词平台：`dict_platform_type`

- 密码/API Key：脱敏 + 留空不修改

- 删除二次确认


