# M4 账号 Excel 导入分析 · xlsx-import-20260918

## 1. Excel 概览

| Sheet | 行数 | 说明 |
|-------|------|------|
| 手机卡 | 460 | SIM 卡台账（主数据源） |
| 抖音 | 94 | 抖音平台账号 |
| 快手 | 24 | 快手平台账号 |
| Sheet1 | 0 | 空表，忽略 |

### 1.1 手机卡 Sheet 列

| Excel 列 | 映射实体 | DB 字段 | 备注 |
|----------|----------|---------|------|
| 手机号 | oa_sim_card | phone_number_encrypted / phone_number_hash | 11 位校验，去重 |
| 存放手机 | oa_phone | device_number | 物理设备编号 |
| 机型 | oa_phone | phone_model | 可选 |
| 卡套餐 | oa_sim_card | package_name | 原样字符串 |
| 主卡 | oa_sim_card | is_primary | 1→YES，其他→NO |
| 营业厅 | oa_sim_card | operator | 联通卡→UNICOM，电信卡→TELECOM，移动卡→MOBILE；广电卡降级 MOBILE |
| 实名者 | oa_realname | real_name | 先导入实名人 |
| 身份证号码 | oa_realname | id_card_encrypted | **Excel 全空**，使用占位加密 |
| 备注 | — | — | 未入库（可后续扩展 remark 字段） |

### 1.2 抖音 Sheet 列

| Excel 列 | 映射实体 | DB 字段 |
|----------|----------|---------|
| 抖音ID | oa_account | external_account_id |
| 抖音名称 | oa_account | account_name |
| 账号认证主体 | oa_company → oa_account.company_id | 自动建公司 |
| 持有人 | oa_realname → oa_account.realname_id | |
| 手机 | oa_phone | device_number 关联 phone_id |
| 登录手机账号 | oa_sim_card | 关联 sim_card_id / phone_number_hash |
| 购买日期 | — | **未映射**（oa_account 无 purchase_date） |
| 运营人/粉丝/状态等 | — | **未映射**（超出 M4 基础 schema） |

### 1.3 快手 Sheet 列

| Excel 列 | 映射实体 | DB 字段 |
|----------|----------|---------|
| 快手ID | oa_account | external_account_id |
| 快手账号昵称 | oa_account | account_name |
| 手机编号 | oa_phone | device_number 关联 phone_id |
| 备注 | — | SQL 注释保留 |

## 2. 导入顺序（依赖链）

```
公司(oa_company) → 实名人(oa_realname) → 手机设备(oa_phone) → 手机卡(oa_sim_card) → 平台账号(oa_account)
```

## 2.1 PRE-IMPORT 清理（test/prod 必读）

SQL 事务开头含两段清理，**顺序不可颠倒**：

| 段落 | 注释标记 | 用途 |
|------|----------|------|
| 定向清理 | `-- PRE-IMPORT CLEANUP (test/prod)` | **test/prod 必用**：删 tenant 下**全部**抖音/快手 + 清 Excel 导入批次支撑数据 |
| ID 段回滚 | `-- ROLLBACK (re-import only)` | dev 仅重导 Excel 时用；**仅**删导入批次抖音/快手（id ≥ 140001）及支撑表，保留低 ID seed |

> 已移除旧版 16 表 tenant 全量 wipe（`DELETE FROM oa_account WHERE tenant_id=1` 等）。

### 删除范围（tenant_id=1）

| 表 | PRE-IMPORT 条件 | ROLLBACK 条件 |
|----|-----------------|---------------|
| oa_account | `platform_type IN ('DOUYIN','KUAISHOU')`（tenant 全量，含历史手工录入） | 同上 **且** `id >= 140001` 或 `creator = 'xlsx-import-20260918'` |
| oa_platform_account_fan_group | 关联 PRE-IMPORT 抖音/快手 `account_id` | 关联 ROLLBACK 导入批次 `account_id` |
| oa_account_cost | 同上 | 同上 |
| oa_account_status_log | 同上 | 同上 |
| oa_collector_account_bind | 同上 | 同上 |
| oa_sim_card | `id >= 130001` 或 `creator = 'xlsx-import-20260918'`，且无剩余账号引用 | 同 PRE-IMPORT |
| oa_phone | `id >= 120001` 或 `creator = 'xlsx-import-20260918'`，且无剩余账号/SIM 引用 | 同 PRE-IMPORT |
| oa_realname | `id >= 110001` 或 `creator = 'xlsx-import-20260918'`，且无剩余账号/设备引用 | 同 PRE-IMPORT |
| oa_company | `id >= 100001` 或 `creator = 'xlsx-import-20260918'`，且无剩余账号引用 | 同 PRE-IMPORT |
| oa_realname_intermediary | 导入批次实名人 | 同 PRE-IMPORT |
| oa_company_expansion | 导入批次公司 | 同 PRE-IMPORT |

### 保留范围（不删除）

| 对象 | 说明 |
|------|------|
| oa_account（WECHAT_OFFICIAL / WECHAT_VIDEO / XIAOHONGSHU / WEWORK 等） | E2E / seed 其它平台账号（**任意 id**） |
| oa_wework_* / oa_personal_wechat_account | 企微、个微 E2E 数据 |
| oa_account_wechat_video_wework_rel / oa_wechat_official_cert_renewal | 微信三方关联、认证续期 |
| id < 100001 的 company/realname/phone/sim | seed 低 ID 支撑数据 |
| 低 id 抖音/快手（ROLLBACK 段） | 仅 ROLLBACK 时保留；PRE-IMPORT 会删 tenant 下全部抖音/快手 |
| oa_content / oa_order / oa_ip_group 等 | 非 M4 账号资产表（**不在清理范围**） |

## 3. 生成统计

```json
{
  "companies": 87,
  "realnames": 109,
  "phones": 374,
  "sim_cards": 373,
  "accounts_douyin": 91,
  "accounts_kuaishou": 24,
  "sim_skipped_no_phone": 87,
  "accounts_missing_company_link": 1,
  "accounts_missing_realname_link": 0,
  "accounts_missing_sim_link": 7,
  "sim_match_by_login": 54,
  "sim_match_by_device": 30,
  "sim_match_unresolved": 7
}
```

## 4. 数据清洗规则

- **手机号**：仅保留 11 位数字；无效行跳过
- **SIM 去重**：按 phone_number_hash 唯一；重复手机号保留首条
- **tenant_id**：固定 `1`（dev 默认租户）
- **assigned_user_id / keeper_id**：默认 `1001`（seed 用户，生产前请核对 Football system_users.id）
- **身份证**：Excel 无数据 → 占位 `000000000000000000` AES 加密
- **公司 credit_code**：合成 18 位 `91IMPORTxxxx...`（非真实工商码，生产需人工补录）
- **广电卡**：dict_sim_operator 无枚举，降级为 MOBILE

## 5. 跳过 / 缺失项

- 手机卡中 **87** 行无有效手机号
- 抖音账号 **1** 条无法关联 company_id
- 抖音账号 **0** 条无法关联 realname_id
- 抖音账号 **7** 条无法关联 sim_card_id（登录手机号不在 SIM 表且设备号无有效 SIM）
- SIM 关联策略：登录手机号精确匹配 **54** · 设备号回退 **30** · 仍缺失 **7**
- 快手账号默认 **无 company_id / realname_id**（Excel 未提供）
- Cookie / 密码 / ICCID 等敏感字段：**未导入**

## 6. 产出文件

- SQL（通用）: `import-accounts-from-xlsx-20260918.sql`
- SQL（shenyu-ops 专用，含 `USE shenyu-ops;`）: `import-accounts-shenyu-ops-20260918.sql`
- 生成器: `generate_accounts_from_xlsx.py`
- 安全导入器: `apply-import-accounts.py`（**Windows 必用**，避免 PowerShell 管道把中文变成字面量 `?`）

## 7. 字符集与导入（prod / test / local）

### 7.1 根因说明

- 生成器以 **UTF-8** 写 SQL，文件内中文正确。
- 若经 **PowerShell 管道**（`Get-Content | mysql`）导入，Windows 会把无法转码的字节写成 **`?`（HEX 3F）** 落库，UI 显示 `????`，**不是前端问题**。
- 验证：`SELECT company_name, HEX(company_name) FROM oa_company WHERE id >= 100001 LIMIT 3;`
  - 错误：`????` / `3F3F3F3F...`
  - 正确：`湖北枫南邦商贸有限公司` / `E6B996...`（UTF-8 多字节）

### 7.2 推荐导入方式（全部环境）

> ⚠️ SQL 已内置 **PRE-IMPORT CLEANUP**：删除 `tenant_id=1` 全部抖音/快手账号及 Excel 导入批次支撑数据；**保留**公众号/视频号/小红书等 E2E seed。test/prod 执行前请确认连接的是目标环境。

**方式 A — Python 安全导入（Windows / Linux 通用，推荐）**

```bash
python scripts/import/apply-import-accounts.py
# test/prod
python scripts/import/apply-import-accounts.py --host <host> --user <user> --password <pwd>
```

**方式 B — Bash / cmd 重定向（Linux / macOS / Git Bash）**

```bash
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -P 3306 -u root -proot shenyu-ops < import-accounts-shenyu-ops-20260918.sql
```

**方式 C — cmd.exe 原生重定向（Windows，勿用 PowerShell 管道）**

```cmd
chcp 65001
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -P 3306 -u root -proot shenyu-ops < scripts\import\import-accounts-shenyu-ops-20260918.sql
```

### 7.3 禁止方式

```powershell
# ❌ 禁止：PowerShell 管道会导致中文变 '?'
Get-Content -Path scripts/import/import-accounts-shenyu-ops-20260918.sql -Raw -Encoding UTF8 | mysql ...
```

### 7.4 目标库

> **目标库必须是 `shenyu-ops`**（ops-server 实际读取），不是 `wd`。

### 7.5 dev 仅重导（保留 seed）

编辑 SQL，**注释掉** `-- PRE-IMPORT CLEANUP (test/prod)` 整段，仅保留 `-- ROLLBACK (re-import only)` 段后执行。

## 8. 阻塞 / 人工确认项

1. **assigned_user_id=1001** 是否为当前环境有效 Football 用户
2. **公司 credit_code** 为合成值，生产环境需替换真实统一社会信用代码
3. **实名人身份证** 全为占位，合规场景需补录真实证件
4. 部分设备无 SIM 手机号时使用合成号 `199xxxxxxxx`，需人工核对
