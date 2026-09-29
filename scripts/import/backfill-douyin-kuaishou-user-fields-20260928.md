# 抖音/快手 holder/operator 补数说明（0928 导入）

## 何时需要

- `import-accounts-shenyu-ops-20260928.sql` 已执行，但 `holder_user_id` / `operator_user_id` 仍为 NULL。
- 根因多为 `scripts/import/data/user_map.json` 未覆盖 Excel 姓名（见 `unmapped-20260928.txt` 中 `[holder_names]` / `[operator_names]`）。

## 推荐步骤

1. 更新 `user_map.json`（Beta：`build_import_maps_from_beta.py` 或手工补映射）。
2. 重新生成 SQL：`python scripts/import/generate_accounts_from_xlsx.py ...`
3. **勿全量删号**：仅对需补行执行 UPDATE（示例，需按生成器输出替换 id 与 user id）：

```sql
USE shenyu-ops;
START TRANSACTION;
-- 示例：账号 140002 补持有人/运营人
UPDATE oa_account SET holder_user_id = 2077584627645607936, operator_user_id = 2081995136448966656, updater = 'backfill-20260928'
WHERE tenant_id = 1 AND id = 140002 AND platform_type = 'DOUYIN';
COMMIT;
```

## 校验

```sql
SELECT COUNT(*) total,
  SUM(holder_user_id IS NOT NULL) holder_cnt,
  SUM(operator_user_id IS NOT NULL) operator_cnt
FROM oa_account WHERE tenant_id = 1 AND platform_type IN ('DOUYIN','KUAISHOU');
```

列表展示依赖 ops-server 列表 API 返回 `holderUserName` / `operatorUserName` / `shortVideoStatus` / `liveStatus`（需部署含 V206 字段与最新 `PlatformAccountServiceImpl` 的版本）。

## Beta 实测（2026-09-28 · tenant=1 抖音 91 行）

导入 SQL 执行后库内已有数据（**非**「全 NULL」）：

| 指标 | 非空行数 / 91 |
|------|----------------|
| `holder_user_id` | 50 |
| `operator_user_id` | 25 |
| `short_video_status` | 90 |
| `live_status` | 90 |

若 UI **持有人/运营人/双状态全空** 但 **实名人** 有值 → **根因在 API/部署**：Beta `ops-server` 未发布含 `AccountDO` V206 字段 + `PlatformAccountServiceImpl.toResp` 的版本；**不是**再跑全量 PRE-IMPORT。

**一键补字段（与 INSERT 对齐，幂等）**：

```bash
python scripts/import/generate_accounts_from_xlsx.py --patch-only
mysql ... shenyu-ops < scripts/import/patch-accounts-user-fields-20260928.sql
```

**修复列表展示**：重新构建并重启 Beta `football-module-ops-server.jar`（及已改列表列的前端静态资源）。
