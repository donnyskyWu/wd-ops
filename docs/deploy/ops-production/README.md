# ⚠️ 已废弃 — 请使用 ops-greenfield-production

**本目录内容已合并至唯一 SSOT 部署包:**

👉 **[`docs/deploy/ops-greenfield-production/`](../ops-greenfield-production/OPERATIONS-GUIDE.md)**

---

## 迁移说明

| 旧路径 (ops-production) | 新路径 (ops-greenfield-production) |
|-------------------------|-------------------------------------|
| `database/00-greenfield/verify_schema.sql` | `sql/verify-schema.sql` |
| `database/01-ops-schema/` | `sql/01-shenyu-ops-schema.sql` |
| `database/02-system-menus/` | `sql/02-shenyu-system-menus.sql` |
| `database/03-system-dicts/` | `sql/02-shenyu-system-menus.sql`（含字典段） |
| `database/04-ops-seeds/` | `sql/03-shenyu-ops-seeds.sql` |
| `database/05-report-schema/` | `sql/01-shenyu-ops-schema.sql`（V184） |
| `database/07-system-rbac/` | `sql/02-shenyu-system-menus.sql`（含 RBAC 段） |
| `config/` | `config/` |
| `manifests/` | `OPERATIONS-GUIDE.md` Part B |
| `database/ROLLBACK-NOTES.md` | `rollback.md` |
| `CHECKLIST.md` | `OPERATIONS-GUIDE.md`（验收章节） |

## 场景指引

| 场景 | 文档 |
|------|------|
| **Greenfield**（零 Ops） | [OPERATIONS-GUIDE.md](../ops-greenfield-production/OPERATIONS-GUIDE.md) |
| **增量升级**（已有 Ops，升 V181–V189） | [OPERATIONS-GUIDE.md 附录](../ops-greenfield-production/OPERATIONS-GUIDE.md#附录已有-ops-增量升级v181v189) |

**版本:** 2026-08-25
