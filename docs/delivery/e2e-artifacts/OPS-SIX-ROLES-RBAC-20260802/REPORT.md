# OPS Six Roles RBAC — Beta Apply Brief (2026-08-02)

## Summary

Product-confirmed 6-role matrix (ADR-064) seeded to Beta shenyu-system; review params updated on shenyu-ops.

## Artifacts

| Item | Path |
|------|------|
| ADR | docs/adr/ADR-064-OPS六角色RBAC矩阵.md |
| Role/menu seed | scripts/integration-config/seed-ops-six-roles-rbac.sql |
| Generator | scripts/integration-config/_gen_seed_ops_six_roles.py |
| Param Flyway | ootball-module-ops/.../V169__content_review_roles_six_rbac.sql |
| Code | ContentReviewConfigService defaults + IP-group scope for ip_group_leader/OPS_LEADER |

## Beta verification (110.42.49.224)

### shenyu-system.system_role + role_menu (Ops 6100–6999)

| code | id | name | data_scope | ops_menu_cnt | 6118 | 6175 |
|------|-----|------|------------|--------------|------|------|
| super_admin | 1 | 超级管理员 | 1 | 71 | Y | Y |
| ip_group_leader | 160 | IP组长 | 5 | 48 | Y | Y |
| ops_manager | 161 | 运营主管 | 1 | 71 | Y | Y |
| finance | 162 | 财务人员 | 1 | 34 | N | N |
| content_editor | 163 | 内容编辑 | 5 | 29 | N | N |
| ops_operator | 164 | 运营 | 5 | 34 | N | N |
| data_analyst | 165 | 数据分析 | 1 | 54 | N | N |

- Chinese names HEX OK (e.g. IP组长 = 4950E7BB84E995BF)
- super_admin Ops menus **not stripped** (still 71)

### shenyu-ops.sys_param

| key | value |
|-----|-------|
| content.review.level1.role | ip_group_leader |
| content.review.level2.role | ops_manager |

## Apply commands

`powershell
python scripts/integration-config/apply-seed-oa-menu.py 
  --host $env:OPS_TEST_DB_HOST --user shenyu-system --password <env> --database shenyu-system 
  --seed scripts/integration-config/seed-ops-six-roles-rbac.sql

python scripts/integration-config/apply-seed-oa-menu.py 
  --host $env:OPS_TEST_DB_HOST --user shenyu-ops --password <env> --database shenyu-ops 
  --seed football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/V169__content_review_roles_six_rbac.sql
`

## Notes

- Re-running seed-oa-system-menu.sql clears Ops system_role_menu for all roles then only restores super_admin; **re-apply** seed-ops-six-roles-rbac.sql afterward.
- User↔role bindings not changed in this task.
