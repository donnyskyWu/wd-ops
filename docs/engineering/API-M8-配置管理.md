# API-M8-配置管理

> **版本**：v2.1 | 2026-06-11（accountId 关联 + 企微/个微 Tab）
> **PRD 映射**：`配置管理模块PRD.md` → 本契约（路径前缀 `/admin-api/oa/config`）
> **ADR**：[`ADR-014`](../adr/ADR-014-M8-配置管理数据模型.md)

---

## 1. 内部采集 `/internal-collect`

| 方法 | 路径 | 说明 | PRD 映射 |
|------|------|------|----------|
| GET | `/list` | 列表；`platformType`/`configName`/`status`/`pageNo`/`pageSize`；响应含 `accountId`、`accountName`（join `oa_account`） | CFG-002 |
| POST | `/create` | 新增账号 | CFG-003 |
| PUT | `/update` | 编辑 | CFG-004 |
| PUT | `/toggle-status` | `{id, status}` 启用/禁用 | CFG-005 |
| DELETE | `/delete?id=` | 删除 | CFG-006 |

### Create/Update Body（INTERNAL）

```json
{
  "accountId": 9001,
  "configName": "账号名称",
  "accountIdentifier": "openid_xxx",
  "platformType": "DOUYIN",
  "appId": "wx123",
  "appSecret": "secret",
  "cookie": "仅快手",
  "authToken": "仅快手",
  "fieldMapping": "{\"fans\":\"fan_count\"}",
  "isLive": false,
  "status": "ENABLED",
  "remark": ""
}
```

校验：`platformType` `@InDict(dict_platform_type)`；`accountIdentifier` 必填；`status` `@InDict(dict_config_status)`。

### 奥创接口 `/internal-collect/aocreate`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/aocreate` | 获取（管理员） |
| POST | `/aocreate` | 创建/更新 |

Body: `apiUrl`, `appId`, `appSecret`, `token`（AES 存储，响应脱敏）。

### 企业微信应用配置（企微 Tab，非本 Controller）

企微 Tab 复用账号管理 API：

| 方法 | 路径 |
|------|------|
| GET | `/admin-api/oa/internal/wework/list` |
| POST | `/admin-api/oa/internal/wework/create` |
| PUT | `/admin-api/oa/internal/wework/update` |

数据表：`oa_wework_account`。前端组件：`WeworkAppConfigPanel`。

**种子**：`V50` 关联 `oa_account` 9001–9010；`V51` 清理无 `account_id` 的 V43 遗留内部配置。

---

## 2. 外部采集 `/external-collect`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | `subType=account` 外部账号 |
| POST | `/create` | 新增外部账号 |
| PUT | `/update` | 编辑 |
| DELETE | `/delete` | 删除 |
| POST | `/import` | CSV 批量导入（multipart） |

Body（account）: `configName`, `accountIdentifier`, `platformType` `@InDict(dict_third_platform)`, `status`, `subType=account`。

**种子**：V50 灌 4 条外部账号 + 5 条关键词。

### 关键词 `/external-collect/keyword`

| 方法 | 路径 |
|------|------|
| GET | `/keyword/list` |
| POST | `/keyword/create` |
| PUT | `/keyword/update` |
| DELETE | `/keyword/delete?id=` |

Body: `platform`, `keyword`, `matchType` `@InDict(dict_match_type)`, `status`。

---

## 3. 外部数据 `/external-source`

标准 CRUD：`list/create/update/delete`。字段 `configName`, `platformType`, `apiUrl`, `apiKey`, `status`。

---

## 4. 订单采集 `/order-collect`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | 数据库配置列表 |
| POST | `/create` | 新增 |
| PUT | `/update` | 编辑 |
| DELETE | `/delete` | 删除 |
| POST | `/test-connection` | 连接测试（30s 超时） |

Body:

```json
{
  "configName": "订单库A",
  "dbHost": "127.0.0.1",
  "dbPort": 3306,
  "dbName": "order_db",
  "dbUsername": "root",
  "dbPassword": "pwd",
  "tableName": "pay_all_order",
  "syncMode": "INCREMENTAL",
  "status": "ENABLED"
}
```

`syncMode` `@InDict(dict_sync_mode)`。

---

## 5. 阈值 `/threshold`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | `thresholdCategory` 必填过滤 |
| POST | `/create` | 按 category 校验字段 |
| PUT | `/update` | 编辑 |
| DELETE | `/delete` | 删除 |

### ALERT 类

`metricName`, `platformType`, `thresholdType`, `compareOperator`, `thresholdValue`, `notifyMethods`, `status`。

### FANS 类

`platformType`, `lowFans`, `highFans`, `dailyLow`, `dailyHigh`, `status`。

### WORK 类

`platformType`, `contentType`, `metricName`, `hotValue`, `lowValue`, `judgeMode`, `status`。

### OVERRIDE 类

`overrideAccountId`（AccountSelect FK 校验 1501）, `metricName`, `overrideValue`, `status`。

---

## 6. AI 模型 `/ai-model`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | 含统计 `stats` |
| POST | `/create` | 新增 |
| PUT | `/update` | 编辑；apiKey 留空不修改 |
| DELETE | `/delete` | 默认模型拒绝删除 |
| POST | `/test-connection` | `{id}` 异步测试 |
| PUT | `/set-default` | `{id}` 设默认 |

Body: `modelName`, `modelId`, `apiEndpoint`, `apiKey`, `temperature`, `maxTokens`, `timeout`, `isDefault`, `status`。

---

## 7. AI 提示词 `/ai-prompt`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | |
| GET | `/get?id=` | 详情（查看弹窗） |
| POST | `/create` | 版本 v1 |
| PUT | `/update` | 版本 +1 |
| DELETE | `/delete` | |

Body: `templateName`, `scene`/`type` `@InDict(dict_prompt_type)`, `promptContent`, `status`。

---

## 字典映射

| 字段 | dict-type |
|------|-----------|
| platformType | dict_platform_type |
| status | dict_config_status |
| matchType | dict_match_type |
| syncMode | dict_sync_mode |
| thresholdCategory | dict_threshold_category |
| thresholdType | dict_threshold_type |
| contentType | dict_content_type |
| judgeMode | dict_judge_mode |
| connStatus | dict_conn_status |
| scene/type | dict_prompt_type |
| notifyMethods | dict_notify_channel |

## 数据安全

- 凭证 AES-256；响应 `*Masked` 或 `****`
- 写操作 `@AuditLog`
- 跨租户 1504
