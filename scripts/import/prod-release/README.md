# 生产发布 SQL（索引）

**主包路径：** [`docs/deploy/prod-sync-20260928/README.md`](../../docs/deploy/prod-sync-20260928/README.md)

| 类型 | 路径 |
|------|------|
| 架构 V205/V206 | `docs/deploy/prod-sync-20260928/01-shenyu-ops-schema-V205-V206.sql` |
| Flyway history（可选） | `docs/deploy/prod-sync-20260928/02-shenyu-ops-flyway-history-insert.sql` |
| 测试全量导入（含 PRE-IMPORT） | `scripts/import/import-accounts-shenyu-ops-20260928.sql` |
| 生产可选导入（仅 INSERT） | `docs/deploy/prod-sync-20260928/04-shenyu-ops-data-import-0928-OPTIONAL.sql` |

生成器：`python scripts/import/prod-release/_generate_prod_sync_data_sql.py`
