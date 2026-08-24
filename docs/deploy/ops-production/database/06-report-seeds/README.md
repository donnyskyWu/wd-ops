# Report Seeds — Ops Production Deploy Pack

**Version:** 2026-08-24

## V184 私域报表 MVP

No seed data required for production migration.

| Feature | Seed needed? | Reason |
|---------|--------------|--------|
| IP业务月达成 | No | Aggregates live data from member-server (orders, users) + `oa_ip_group_anchor_rel` |
| 周度私域转化 | No | Same — runtime aggregation; only U 列 feedback is persisted on first save |
| weekly-feedback | No | Rows created on first PUT by users |

## Prerequisites (runtime, not SQL)

- `oa_ip_group_anchor_rel` populated with anchor authors for tenant
- member-server registered in Nacos (Feign: author/user/order APIs)
- Existing menu **6126 数据报表** (`oa:report:list`) grants access to Report Center cards
