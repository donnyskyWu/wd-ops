# shenyu-ops 测试环境清理清单（V163 执行范围）

> 日期：2026-07-25 · 主机：`110.42.49.224` · 库：`shenyu-ops`  
> SSOT 决策：`docs/delivery/OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md` §3

## 已删除（Flyway V163 自动执行）

| 对象 | 原因 |
|------|------|
| `system_menu_backup_20260716` | 2026-07-16 手工导入备份，非运行态 |
| `system_role_menu_backup_20260716` | 同上 |
| `football_demo01_contact` … `football_demo03_student` | Football 示例表，非 OPS 业务 |
| `system_dict_type` / `system_dict_data` | 字典 SSOT 已迁至 `shenyu-system`（V152+）；OPS 读 `@DS(system)` |
| `system_mail_*` / `system_sms_*` / `system_social_*` / `system_notify_*` | Football 基础设施副本；OPS master 无引用 |
| `system_operate_log` / `system_login_log` | 审计 SSOT = Football Admin；OPS 不平行读 |
| `system_dept` / `system_post` / `system_tenant*` | Football 组织/租户 SSOT = shenyu-system |
| `system_user_author` / `system_user_data` / `system_user_post` | Football 用户扩展副本 |
| `system_oauth2_refresh_token` / `code` / `approve` / `client` | Token 校验仅用 `system_oauth2_access_token`（overlay 过渡） |
| `system_notice` | Football 公告 SSOT |
| `sys_audit_log` / `sys_dept` / `sys_login_log` | 空 legacy standalone 表 |

## 保留（勿删）

| 对象 | 原因 |
|------|------|
| `system_users` / `system_role` / `system_menu` / `system_user_role` | `FootballOAuth2MasterTokenMapper` @DS master 过渡 overlay |
| `system_oauth2_access_token` | 同上（token 解析） |
| `sys_dict_*` | OPS 侧 staging；V152/V158/V161 源；待 Football 字典所有权完全移交后再归档 |
| `sys_operation_log` | 待确认 `OperationLogRecorder` 本地双写下线（CLEANUP P0-6） |
| `sys_user*` / `sys_role*` / `sys_permission*` | Dev token / 集成测试 harness（测试环境仍可能使用） |
| 全部 `oa_*` 业务表 | OPS 自建域 |

## 待人工复核（本次未 DROP）

| 对象 | 风险 | 建议 |
|------|------|------|
| `shenyu-ops.system_menu` 中 6100-6999 副本 | 与 shenyu-system 重复；master overlay 仍可读 | shenyu-system seed 完成后，确认 `@DS(system)` 鉴权绿再删 master 副本 |
| `shenyu-ops.system_users` overlay 行 | ADR-056 禁止新写 | 仅只读过渡；全量切轨后删 overlay |
| `sys_dict_*` | 字典 merge 源表 | 停新写后只读；勿在 merge 未完成时物理删 |

## 验证

```sql
-- shenyu-ops：备份/demo/重复 infra 应不存在
SHOW TABLES LIKE '%backup%';
SHOW TABLES LIKE 'football_demo%';
SHOW TABLES LIKE 'system_dict%';

-- shenyu-system：OPS 菜单应存在
SELECT COUNT(*) FROM system_menu WHERE id BETWEEN 6100 AND 6999 AND deleted=0;
```
