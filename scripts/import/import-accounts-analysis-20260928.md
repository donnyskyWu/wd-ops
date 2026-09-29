# M4 账号 Excel 导入分析 · xlsx-import-20260928（收窄范围）

> **0928 用户确认**：不导入 Set A/B 列；**不映射 ip_group_id**（无分配小组阻塞）；**不用登录手机账号/登录手机号** 做 SIM 关联。

## 0. 用户 SSOT 映射（Excel / 表单 / 导入 · 客户 2026-09-28）

| 来源 | 目标 | 说明 |
|------|------|------|
| Excel **「持有人」**（快手 L/R **「持有」**） | 系统 **持有人** `holder_user_id` / `holderUserId` | 与表单「持有人」同一字段；`user_map.json` |
| Excel **「运营人」** | 系统 **运营人** `operator_user_id` / `operatorUserId` | **V206**；`user_map.json` |
| 同上持有人姓名（合规） | **实名人** `realname_id` | 并行建 `oa_realname`；DOUYIN 仍配合 `company_id` 校验 |

## 1. 仍导入 / 不导入（按 Sheet）

### 1.1 抖音（`20W抖音`，94 行 → 91 账号）

| 仍导入 | 映射 |
|--------|------|
| 抖音ID、抖音名称 | `external_account_id` / `account_name` |
| 账号认证主体 | `oa_company` → `company_id` |
| 持有人 | `holder_user_id` + `oa_realname` → `realname_id` |
| 手机 | `oa_phone.device_number` → `phone_id` |
| 手机卡（列内 **11 位手机号**） | `oa_sim_card` → `sim_card_id`；`phone_number_hash` |
| 短视频状态、直播状态 | `short_video_status` / `live_status` |
| 运营人 | `operator_user_id`（需 `user_map.json`） |
| 抖音密码 | `password_encrypted` |

| 不导入 | 说明 |
|--------|------|
| 接蓝改号手机号、卖家微信名称、截图 | Set A |
| 运营人费用、登录手机账号、分配小组、备注、购买日期、购买价格 | Set B |
| 粉丝 | 系统无专用字段，仍可选人工补录 |
| Unnamed 列 | 忽略 |

### 1.2 快手（header=1，22 行 → 27 账号）

| 仍导入 | 映射 |
|--------|------|
| 快手ID / 右侧 ID | `external_account_id`（双列拆分 L/R） |
| 快手账号昵称 / 持有 | `account_name` |
| 持有人 / 持有 | `holder_user_id` + `realname_id`（L/R 块） |
| 手机编号 / 右侧「手机号」列 | `phone_id`（**设备编号**，非登录号） |
| 仅发短视频、短+直播 | 快手状态字段 |
| 密码 / 密码.1 | `password_encrypted` |

| 不导入 | 说明 |
|--------|------|
| 分配小组、分配 | IP 组 |
| 登录手机号 | Set B 同类 |
| 价格、价格.1、收号日期 | 成本/采购 |
| Unnamed 列 | 忽略 |

### 1.3 手机卡 Sheet

| 状态 | 说明 |
|------|------|
| 本文件 **无**「手机卡」Sheet（0 行） | SIM 主数据改由抖音行内「手机卡」列 11 位号补建（19 条有号） |

## 2. Excel 概览

| Sheet | 行数 | 说明 |
|-------|------|------|
| 手机卡 | 0 | 有 Sheet 时仍为 SIM 主数据源 |
| 抖音 | 94 | 见 §1.1 |
| 快手 | 22 | 见 §1.2 |

## 3. 导入顺序（依赖链）

```
公司(oa_company) → 实名人(oa_realname) → 手机设备(oa_phone) → 手机卡(oa_sim_card) → 平台账号(oa_account)
```

## 3.1 PRE-IMPORT 清理（test/prod 必读）

SQL 事务开头含两段清理，**顺序不可颠倒**：

| 段落 | 注释标记 | 用途 |
|------|----------|------|
| 定向清理 | `-- PRE-IMPORT CLEANUP (test/prod)` | **test/prod 必用**：删 tenant 下**全部**抖音/快手 + 清 Excel 导入批次支撑数据 |
| ID 段回滚 | `-- ROLLBACK (re-import only)` | dev 仅重导 Excel 时用；**仅**删导入批次抖音/快手（id ≥ 140001）及支撑表，保留低 ID seed |

> 已移除旧版 16 表 tenant 全量 wipe（`DELETE FROM oa_account WHERE tenant_id=1` 等）。

### 删除范围（tenant_id=1）

| 表 | PRE-IMPORT 条件 | ROLLBACK 条件 |
|----|-----------------|---------------|
| oa_account | `platform_type IN ('DOUYIN','KUAISHOU')`（tenant 全量，含历史手工录入） | 同上 **且** `id >= 140001` 或 `creator = 'xlsx-import-20260928'` |
| oa_platform_account_fan_group | 关联 PRE-IMPORT 抖音/快手 `account_id` | 关联 ROLLBACK 导入批次 `account_id` |
| oa_account_cost | 同上 | 同上 |
| oa_account_status_log | 同上 | 同上 |
| oa_collector_account_bind | 同上 | 同上 |
| oa_sim_card | `id >= 130001` 或 `creator = 'xlsx-import-20260928'`，且无剩余账号引用 | 同 PRE-IMPORT |
| oa_phone | `id >= 120001` 或 `creator = 'xlsx-import-20260928'`，且无剩余账号/SIM 引用 | 同 PRE-IMPORT |
| oa_realname | `id >= 110001` 或 `creator = 'xlsx-import-20260928'`，且无剩余账号/设备引用 | 同 PRE-IMPORT |
| oa_company | `id >= 100001` 或 `creator = 'xlsx-import-20260928'`，且无剩余账号引用 | 同 PRE-IMPORT |
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

## 4. 生成统计

```json
{
  "scope": "slim-20260928",
  "dry_run": false,
  "douyin_sheet": "20W抖音",
  "douyin_accounts": 91,
  "kuaishou_accounts": 27,
  "sim_sheet_rows": 0,
  "douyin_with_device_no": 91,
  "douyin_with_sim_card_phone": 19,
  "companies": 87,
  "realnames": 35,
  "phones": 100,
  "sim_cards": 19,
  "accounts_douyin": 91,
  "accounts_kuaishou": 27,
  "sim_from_account_column": 19,
  "sim_skipped_no_phone_in_sheet": 0,
  "accounts_missing_company_link": 1,
  "accounts_missing_realname_link": 0,
  "accounts_missing_sim_link": 72,
  "sim_match_by_sim_card_column": 19,
  "sim_match_by_device": 0,
  "sim_match_unresolved_douyin": 72,
  "holder_unresolved_names": [
    "刘其荣",
    "易英杰",
    "李姣资",
    "栋哥",
    "梁钰平",
    "江永明",
    "汤笑天",
    "舒彬",
    "账号",
    "邓希萌",
    "魏彬",
    "黄诗"
  ],
  "holder_names_distinct": 35,
  "holder_names_mapped_distinct": 23,
  "operator_unresolved_names": [
    "**强",
    "**思",
    "**炜",
    "**烨",
    "**莎",
    "**韦（栋哥朋友）",
    "*彬",
    "*芳",
    "*鹏",
    "侯军",
    "刘聪",
    "刘镔鋈",
    "吴双喜",
    "周正",
    "崔斌",
    "张铭远",
    "张锦翔",
    "彭炎",
    "无",
    "朱洪达",
    "李冰冰",
    "李安兵（皮总）",
    "李湘慧",
    "李雪雀",
    "杨宇轩",
    "杨荃智",
    "楚北平（皮总）",
    "汪青？？",
    "潘峰",
    "王淼",
    "田水晶",
    "老板妈妈",
    "老板娘",
    "自助餐饭店老板",
    "蒋超",
    "许灿",
    "邓希萌",
    "阙勇（吴嘉勤）",
    "陶怡亮",
    "高心意"
  ],
  "import_no_columns": [
    "接蓝改号手机号",
    "卖家微信名称",
    "截图",
    "运营人员费用",
    "运营人费用",
    "登录手机账号",
    "登录手机号",
    "分配小组",
    "分配",
    "备注",
    "购买日期",
    "购买价格",
    "价格",
    "价格.1",
    "收号日期"
  ],
  "with_password": 59,
  "with_short_video_status": 95,
  "with_live_status": 104,
  "sample_mapping": {
    "douyin_example": {
      "platform": "DOUYIN",
      "external_account_id": "beitang999",
      "account_name": "武汉市洪山区与格羽百货经营部",
      "holder_name": "汤笑天",
      "operator_name": "周培林（产研）",
      "device_no": "4",
      "sim_card_phone": null,
      "company_name": "武汉市洪山区与格羽百货经营部",
      "password_plain": "shenyu123456",
      "short_video_status": "正常",
      "live_status": "2026/6/26永封",
      "source_sheet": "20W抖音",
      "source_row": 2,
      "block": "single"
    },
    "kuaishou_example": {
      "platform": "KUAISHOU",
      "external_account_id": "jjy2549476575",
      "account_name": "解说员浩南",
      "holder_name": "郭威",
      "operator_name": null,
      "device_no": "62",
      "sim_card_phone": null,
      "company_name": null,
      "password_plain": null,
      "short_video_status": null,
      "live_status": "短+直播",
      "source_sheet": "快手",
      "source_row": 3,
      "block": "L"
    }
  }
}
```

## 5. 数据清洗规则

- **手机号**：仅保留 11 位数字；无效行跳过
- **SIM 去重**：按 phone_number_hash 唯一；重复手机号保留首条
- **tenant_id**：固定 `1`（dev 默认租户）
- **assigned_user_id / keeper_id**：默认 `1001`（seed 用户，生产前请核对 Football system_users.id）
- **身份证**：Excel 无数据 → 占位 `000000000000000000` AES 加密
- **公司 credit_code**：合成 18 位 `91IMPORTxxxx...`（非真实工商码，生产需人工补录）
- **广电卡**：dict_sim_operator 无枚举，降级为 MOBILE

## 6. 跳过 / 缺失项（收窄后）

- **ip_group_id**：全部 NULL（不导分配小组 → **无 IP 组映射阻塞**）
- 抖音 **1** 条无 company_id · **0** 条无 realname_id
- 抖音 SIM：`手机卡`列匹配 **19** · 设备回退 **0** · 仍无 sim **72**（列内非 11 位号如 `226`/运营商简称无法绑卡）
- 抖音有设备号 **91** · 有「手机卡」11 位号 **19**
- 持有人未解析 **12** 个姓名 · 运营人未解析 **40** 个（维护 `user_map.json` 或 UI 补选）
- 持有人去重 **35** 个姓名 · 已映射 **23** 个
- 粉丝、Cookie、ICCID、oa_account_cost：**未导入**

## 6.1 平台账号能力缺口（收窄范围下）

| 能力 | 够用？ | 说明 |
|------|--------|------|
| 外部 ID / 昵称 / 密码 / 双状态 | ✅ | 抖音+快手均覆盖 |
| 持有人 → holderUserId + 实名人 | ✅ | 快手 R「持有」作持有人姓名 |
| 运营人 | ⚠️ | 仅抖音列；依赖 user_map |
| 设备 phone_id | ✅ | 「手机」「手机编号」等 |
| SIM sim_card_id | ⚠️ | 仅抖音「手机卡」11 位 + 可选 SIM Sheet；**不用**登录手机账号 |
| IP 组 | ➖ 刻意不导 | 导入后 UI/任务再绑 |
| 粉丝数 | ❌ | Excel 有列未入库 |
| 采购/成本/备注 | ❌ 刻意不导 | |

## 7. 产出文件

- SQL（通用）: `import-accounts-from-xlsx-20260928.sql`
- SQL（shenyu-ops 专用，含 `USE shenyu-ops;`）: `import-accounts-shenyu-ops-20260928.sql`
- 生成器: `generate_accounts_from_xlsx.py`
- 安全导入器: `apply-import-accounts.py`（**Windows 必用**，避免 PowerShell 管道把中文变成字面量 `?`）

## 8. 字符集与导入（prod / test / local）

### 8.1 根因说明

- 生成器以 **UTF-8** 写 SQL，文件内中文正确。
- 若经 **PowerShell 管道**（`Get-Content | mysql`）导入，Windows 会把无法转码的字节写成 **`?`（HEX 3F）** 落库，UI 显示 `????`，**不是前端问题**。
- 验证：`SELECT company_name, HEX(company_name) FROM oa_company WHERE id >= 100001 LIMIT 3;`
  - 错误：`????` / `3F3F3F3F...`
  - 正确：`湖北枫南邦商贸有限公司` / `E6B996...`（UTF-8 多字节）

### 8.2 推荐导入方式（全部环境）

> ⚠️ SQL 已内置 **PRE-IMPORT CLEANUP**：删除 `tenant_id=1` 全部抖音/快手账号及 Excel 导入批次支撑数据；**保留**公众号/视频号/小红书等 E2E seed。test/prod 执行前请确认连接的是目标环境。

**方式 A — Python 安全导入（Windows / Linux 通用，推荐）**

```bash
python scripts/import/apply-import-accounts.py
# test/prod
python scripts/import/apply-import-accounts.py --host <host> --user <user> --password <pwd>
```

**方式 B — Bash / cmd 重定向（Linux / macOS / Git Bash）**

```bash
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -P 3306 -u root -proot shenyu-ops < import-accounts-shenyu-ops-20260928.sql
```

**方式 C — cmd.exe 原生重定向（Windows，勿用 PowerShell 管道）**

```cmd
chcp 65001
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -P 3306 -u root -proot shenyu-ops < scripts\import\import-accounts-shenyu-ops-20260928.sql
```

### 8.3 禁止方式

```powershell
# ❌ 禁止：PowerShell 管道会导致中文变 '?'
Get-Content -Path scripts/import/import-accounts-shenyu-ops-20260928.sql -Raw -Encoding UTF8 | mysql ...
```

### 8.4 目标库

> **目标库必须是 `shenyu-ops`**（ops-server 实际读取），不是 `wd`。

### 8.5 dev 仅重导（保留 seed）

编辑 SQL，**注释掉** `-- PRE-IMPORT CLEANUP (test/prod)` 整段，仅保留 `-- ROLLBACK (re-import only)` 段后执行。

## 9. 阻塞 / 人工确认项

1. **assigned_user_id=1001** 是否为当前环境有效 Football 用户
2. **公司 credit_code** 为合成值，生产环境需替换真实统一社会信用代码
3. **实名人身份证** 全为占位，合规场景需补录真实证件
4. 设备无「手机卡」11 位号时使用合成设备号 `199xxxxxxxx`，需人工核对
5. ~~IP 组映射~~（已移除阻塞）
6. **运营人 user_map** 未覆盖姓名需补映射或导入后编辑
