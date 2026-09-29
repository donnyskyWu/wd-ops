# M4 抖音/快手 0928 导入 — 后端/前端最小增量（ops-server + web-ele）

> SSOT 决策：[ADR-078](../adr/ADR-078-M4-抖音快手双状态与登录密码.md) · Flyway [V206](../deploy/ops-greenfield-production/sql/V206__oa_account_short_video_live_operator.sql)

## 后端（ops-server · platform account）

| 项 | 动作 |
|----|------|
| DO | `AccountDO` 增 `shortVideoStatus`、`liveStatus`、`operatorUserId` |
| DO | `AccountCreateReq` / `AccountUpdateReq` / `AccountRespVO`：`platformType ∈ {DOUYIN,KUAISHOU}` 时暴露 **`operatorUserId`**（V206）与 **`holderUserId`**（V205；与 Excel/表单「持有人」同语义） |
| DO | `password` 入参 plain → `password_encrypted`（复用现有 AES 工具，与 WECHAT_OFFICIAL 同路径） |
| DO | 列表/详情：`passwordEncrypted` 脱敏 `******`；非空时表单显示「已设置，留空不改」 |
| SKIP | 卖家/截图字段；粉丝/成本 |

## 前端（InternalAccountManage · 抖音/快手 Tab）

| 字段 | 控件 |
|------|------|
| `realnameId` | `<RealNameSelect />` 实名人（合规；导入用持有人姓名建链） |
| `operatorUserId` | `<UserSelect />` 运营人（Excel「运营人」，V206） |
| `holderUserId` | `<UserSelect />` 持有人（Excel「持有人」，V205；导入写入） |
| `ipGroupId` | 已有 `<IpGroupTreeSelect />` |
| `password` | `<Input type="password" show-password />` |
| `shortVideoStatus` / `liveStatus` | `<Input />` 或 `<Input type="textarea" />` |
| 列表列 | 持有人 + 运营人 + 双状态（实名人在详情/合规字段） |

## 导入

```bash
python scripts/import/xlsx_account_loader.py   # 自检
python scripts/import/generate_accounts_from_xlsx.py --dry-run
python scripts/import/generate_accounts_from_xlsx.py
# test 库：先 V206，再 apply-import-accounts.py（勿对 prod 未经确认执行）
```
