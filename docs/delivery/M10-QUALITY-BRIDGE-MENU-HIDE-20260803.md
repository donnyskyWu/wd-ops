# M10 数据质量 / 私域桥接菜单隐藏 — 20260803

**Decision:** 本期不交付 FR-M10-002 数据质量、M10-AO-S-07 私域桥接 UI；菜单与 RBAC 权限从侧栏移除。

**Refs:** ADR-060 Phase 2 OOS · PRD-M10 §Out of Scope

## Removed (shenyu-system)

| menu_id | 名称 | permission |
|---------|------|------------|
| 6134 | 私域桥接 | `ops:collect:bridge:list` |
| 6135 | 数据质量 | `ops:collect:quality:list` |

**Role bindings removed:** super_admin 70024/70025 · ops_manager 71234/71235 · data_analyst 72031/72032 · any role_menu on above.

**Not removed:** `oa_private_domain_conversion_bridge` 表 · 漏斗分析 `PRIVATE_DOMAIN` 预置 · `dict_quality_*` / `dict_private_domain_*` 字典 · stub API（DeferredCutoverStubController）

## Artifacts

- `scripts/integration-config/cleanup-m10-quality-bridge-menu.sql` — beta/存量幂等 DELETE
- `scripts/integration-config/seed-oa-system-menu.sql` — 新装不再插入 6134/6135
- `V174__hide_m10_quality_bridge_menus.sql` — Flyway 文档锚点（shenyu-ops 无 cross-DB 写权限）

## Apply (beta)

```bash
python scripts/integration-config/apply-seed-oa-menu.py \
  --host 110.42.49.224 --user shenyu-system --password '<OPS_TEST_SYSTEM_PASSWORD>' \
  --database shenyu-system \
  --seed scripts/integration-config/cleanup-m10-quality-bridge-menu.sql
```

Full re-seed: `scripts/integration-config/seed-ops-test-remote.ps1`

## Verify

1. Admin 登录 → 数据采集 下仅 **采集日志**、**采集任务**（无数据质量、私域桥接）
2. SQL:
   ```sql
   SELECT id,name FROM system_menu WHERE id IN (6134,6135);
   -- expect 0 rows
   SELECT id,name,permission FROM system_menu WHERE parent_id=6104 AND deleted=0 ORDER BY sort;
   ```
3. 直链 `/ops/collect/quality`、`/ops/collect/private-domain-bridge` 无菜单时不出现在侧栏；路由组件仍保留（stub/OOS）
