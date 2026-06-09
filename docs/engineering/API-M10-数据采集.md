# API-M10-数据采集

> **版本**：v1.0 | 2026-06-07
> **关联全局规范**：[`GLOBAL-CONVENTIONS.md`](./GLOBAL-CONVENTIONS.md)

---

## 1. 采集任务 API

### 1.1 GET `/admin-api/oa/collect/task/list`

| 参数 | 字典 |
|------|------|
| platformType | `dict_platform_type` |
| method | `dict_collect_method` |
| frequency | `dict_collect_frequency` |
| status | `dict_collect_status` |

### 1.2 POST `/admin-api/oa/collect/task/create`

```json
{
  "taskName": "公众号每日数据采集",
  "platformType": "WECHAT_OFFICIAL",
  "accountId": 123,            // 强关联
  "method": "INTERNAL",
  "source": "WECHAT_MP_API",
  "frequency": "DAILY",
  "cron": "0 0 2 * * ?",
  "apiConfig": "{...}",        // 加密
  "status": "ENABLED"
}
```

**校验**：
- `platformType` `@InDict(type="dict_platform_type")`
- `accountId` `@NotNull`
- `method` `@InDict(type="dict_collect_method")`
- `source` `@InDict(type="dict_collect_source")`
- `frequency` `@InDict(type="dict_collect_frequency")`
- `status` `@InDict(type="dict_collect_status")`
- `apiConfig` AES-256 加密

### 1.3 PUT `/admin-api/oa/collect/task/update`

### 1.4 DELETE `/admin-api/oa/collect/task/{id}`

### 1.5 POST `/admin-api/oa/collect/task/{id}/run`

**业务**：
- 立即执行一次
- 同步调用（v1.0 简化）
- 记录到 `oa_collect_log`

### 1.6 GET `/admin-api/oa/collect/log`

| 参数 | 字典 |
|------|------|
| taskId | - |
| status | `dict_collect_status` |
| startDate / endDate | - |

---

## 2. 数据质量 API

### 2.1 GET `/admin-api/oa/collect/quality/list`

### 2.2 POST `/admin-api/oa/collect/quality/create`

```json
{
  "checkName": "粉丝数非空检查",
  "checkType": "COMPLETENESS",
  "targetTable": "oa_follower_daily",
  "targetField": "follower_count",
  "rule": "follower_count IS NOT NULL",
  "level": "EXCELLENT"
}
```

**校验**：
- `checkType` `@InDict(type="dict_quality_check_type")`
- `level` `@InDict(type="dict_quality_level")`

### 2.3 POST `/admin-api/oa/collect/quality/{id}/run`

**业务**：
- 执行质量检查
- 返回合格率

### 2.4 GET `/admin-api/oa/collect/quality/log`

---

## 3. 错误码

| 错误码 | 含义 |
|--------|------|
| 1500~1504 | 全局 |
| 1503 | 字典值不合法 |
| 2021 | cron 表达式非法 |
| 2022 | 采集失败 |
| 2023 | 质量检查规则非法 |

---

*下一步：STATE / SLICES / CHECKLIST / TESTCASES。*

---

## 字典与数据安全（🔴 API 必含）

### 字典使用（dict-type）

| 字段 | dict-type | 取值约束 |
|------|-----------|----------|
| collect_freq | `dict_collect_freq` | 字典合法值，错误码 1503 |
| collect_type | `dict_collect_type` | 字典合法值，错误码 1503 |
| proxy_type | `dict_proxy_type` | 字典合法值，错误码 1503 |

### 数据安全

- **脱敏**：`Cookie（**AES-256 加密**）、账号密码（**AES-256 加密**）、API 密钥` 在 API 响应中按权限脱敏（前端展示如 `138****1234`）
- **加密**：所有凭证字段在数据库中以 **AES-256** 加密存储（key 由配置中心注入）
- **日志**：所有写操作记录 `sys_audit_log`（含 operatorId / operation / before / after）
- **传输**：HTTPS + JWT；敏感字段二次加密（前端公钥 + 后端私钥）

详见 [`GLOBAL-CONVENTIONS.md § 5`](./GLOBAL-CONVENTIONS.md) (数据安全)

---

## 字段字典映射表（🔴 API 必含）

下表列出 数据采集 模块所有需要走数据字典的字段。前端使用 `<DictSelect dict-type="..." />` 组件，后端 `@InDict` 注解校验。

| 字段 | dict-type | 取值范围 | 错误码（不合法时） |
|------|-----------|----------|--------------------|
| `collectType` | `dict_collect_type` | 视频/评论/账号/作品 | 1503 |
| `collectFreq` | `dict_collect_freq` | 5min/30min/1h/24h | 1503 |
| `proxyType` | `dict_proxy_type` | 无/HTTP/HTTPS/SOCKS | 1503 |
| `authType` | `dict_auth_type` | 无/账密/Cookie/扫码/Token | 1503 |
| `status` | `dict_collect_status` | 待运行/运行中/暂停/失败/已停止 | 1503 |

### 使用规范

1. **前端**：`<DictSelect v-model="form.collectType" dict-type="dict_collect_type" />`
2. **后端**：`@InDict("dict_collect_type") private String collectType;`
3. **字典维护**：在 M8 配置管理模块维护
4. **错误码**：字典值不在范围内 → 1503

详见 [`GLOBAL-CONVENTIONS.md § 2`](./GLOBAL-CONVENTIONS.md) (字典)

## 强关联字段 → 选择器映射（🔴 API 必含）

本模块涉及以下强关联字段，**前端必须使用对应选择器组件**：

| 字段 | 选择器组件 | 关联实体 | 错误码 |
|------|----------|---------|--------|
| `accountId` | `<AccountSelect />` | 平台账号 | 1501 / 1504 |
| `proxyType` | `<DictSelect dict-type="dict_proxy_type" />` | 代理类型 | 1503 |
| `authType` | `<DictSelect dict-type="dict_auth_type" />` | 认证类型 | 1503 |
| `collectFreq` | `<DictSelect dict-type="dict_collect_freq" />` | 采集频率 | 1503 |
