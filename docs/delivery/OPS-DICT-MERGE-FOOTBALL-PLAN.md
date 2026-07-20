# OPS 业务字典合并 Football 方案（OPS-only）

> **范围**：`wd.sys_dict_*` 中 **97 个 `dict_*` 业务类型**迁入 `shenyu-system.system_dict_*`；OPS 侧菜单 6137 / `oa:dict:*` 下线；读写在 oa-server 统一走 `@DS("system")` Adapter。  
> **禁止**修改 `football-backend-saas/**` 原生业务逻辑。  
> **状态**：规划稿 · 待架构确认后分派并行 worker。  
> **日期**：2026-07-18

---

## 0. 背景与现状摘要

| 维度 | 当前 OPS 实现 | Football 标准 |
|------|----------------|---------------|
| **平台字典** | `SystemDictAdapter` → `@DS("system")` 读 `shenyu-system.system_dict_*` | `system-server` 原生 CRUD；菜单 **105**「字典管理」 |
| **业务字典 `dict_*`** | `SystemDictServiceImpl` 双轨路由 → `wd.sys_dict_*` | 无（type 名零重叠，UAT type-list=94 均为 `dict_*`） |
| **运行时读** | `DictService` 缓存读 **wd** `sys_dict_data`（`@InDict` 校验） | — |
| **管理 UI** | OPS 壳 `#/ops/system-dict`（menu **6137**，`oa:dict:admin-list`） | Football `#/dict`（menu **105**，`system:dict:query`） |
| **权限** | `oa:dict:admin-list/create/update/delete`（`sys_permission` id 32–35） | `system:dict:query/create/update/delete/export`（menu 1026–1030） |

**Prep 结论（已实现）**

- `SystemDictAdapter` 已读 Football `@DS("system")`，列映射 `value`↔`dict_value`、`0/1`↔`ENABLED/DISABLED` 已封装。
- `SystemDictServiceImpl` 双轨：`isBusinessDictType(type)`（`type.startsWith("dict_")`）→ wd；否则 → Adapter。
- UAT 2026-07-04：字典配置页 **PASS**；`type-list=94`、`dict/list total=359`（均为 wd 业务字典行）。

**目标态**：**单一 SSOT = `shenyu-system.system_dict_*`**；wd `sys_dict_*` 停写、只读过渡后 deprecate；OPS 字典管理页移除，跳转 Football 原生字典页。

---

## 1. 决策

### 1.1 数据归属 — **业务 `dict_*` 迁入 shenyu-system**

**理由**

1. ADR-050 D4 双轨为集成期过渡；Football 已是平台 infra SSOT，继续分裂导致 DictManage 与 `@InDict` 读路径不一致。
2. `dict_*` 与 Football 平台 type（`system_user_sex`、`infra_*`）**命名空间零重叠**，合并后仍可按前缀区分。
3. Football 字典无 `tenant_id`；当前 wd `sys_dict_*` 亦无 tenant 列 — **1:1 可迁**。

**不迁对象**

| 对象 | 处置 |
|------|------|
| wd 中已 deleted=1 的 type/data | 跳过 |
| Football 已有同 `type` 的平台字典 | type **跳过**；data 按 `(dict_type, value)` **merge** |
| `sys_metadata_field.dict_type` 等引用 | **不变**（仍引用 `dict_*` 字符串，仅数据源换库） |

### 1.2 管理 UI — **移除 OPS 6137，跳转 Football 105**

| 层 | 路径 / 对象 | 动作 |
|----|-------------|------|
| **UI 源码** | `ops-platform-ui-vue/src/views/system/DictManage.vue` | 删除 |
| | `ops-platform-ui-vue/src/router/index.ts`（`/system-dict`） | 删除路由 |
| | `ops-platform-ui-vue/src/views/Layout.vue` 侧栏项 | 删除 |
| | `ops-platform-ui-vue/src/api/system-dict.ts` | 删除 |
| **Football 挂载副本** | `football-front/.../views/ops/system/DictManage.vue` | 删除 |
| | `football-front/.../api/ops/system-dict.ts` | 删除 |
| **后端 API** | `SystemDictController`（`/oa/system/dict/*` CRUD） | 删除或 thin proxy → Football Admin API |
| | `DictController` 读 API（`/oa/dict/data`、`/oa/dict/type/list`） | **保留**；实现改读 `@DS("system")` |
| **菜单/权限** | `system_menu` id **6137** | soft delete（Flyway V148） |
| | `sys_permission` id **32–35** `oa:dict:*` | soft delete |
| | `scripts/integration-config/seed-oa-system-menu.sql` 6137 行 | 删除 |
| **跳转目标** | Football `#/dict` | menu **105**；权限 `system:dict:query` 起 |

### 1.3 代码路由 — **取消双轨，统一 Adapter**

```
                    ┌─────────────────────────────────────┐
  DictController    │  SystemDictServiceImpl (简化)        │
  @InDict/DictService ──► SystemDictAdapter @DS("system") │
                    └──────────────┬──────────────────────┘
                                   ▼
                         shenyu-system.system_dict_*
```

- 删除 `SystemDictServiceImpl` 中 `*Business*` 分支及 `SysDictTypeMapper` / `SysDictDataMapper` 写路径。
- `DictService.listByType` / `isValidValue` 改读 `FootballSystemDictDataMapper`（或经 Adapter 封装）。

---

## 2. 表映射：`sys_dict_*` → `system_dict_*`

### 2.1 `sys_dict_type` → `system_dict_type`

| wd.sys_dict_type | shenyu-system.system_dict_type | 转换规则 |
|------------------|--------------------------------|----------|
| `id` | — | **不保留**；目标表 AUTO_INCREMENT |
| `type` | `type` | 原样（`dict_*`） |
| `name` | `name` | 原样 |
| `status` VARCHAR | `status` TINYINT | `ENABLED`→`0`，`DISABLED`→`1` |
| `creator` | `creator` | 原样或 `'dict-merge'` |
| `create_time` | `create_time` | 原样 |
| `updater` | `updater` | 原样 |
| `update_time` | `update_time` | 原样 |
| `deleted` SMALLINT | `deleted` BIT | `0`→`b'0'`，`1`→`b'1'` |
| — | `remark` | NULL |
| — | `deleted_time` | NULL |

**冲突**：目标库已存在相同 `type` → **跳过 INSERT type**（data 仍 merge）。

### 2.2 `sys_dict_data` → `system_dict_data`

| wd.sys_dict_data | shenyu-system.system_dict_data | 转换规则 |
|------------------|--------------------------------|----------|
| `id` | — | **不保留** |
| `dict_type` | `dict_type` | 原样 |
| `label` | `label` | 原样 |
| `dict_value` | `value` | 列名映射 |
| `sort` | `sort` | 原样 |
| `status` VARCHAR | `status` TINYINT | `ENABLED`→`0`，`DISABLED`→`1` |
| `color_type` | `color_type` | 默认 `'default'` |
| — | `css_class` | `''` |
| `remark` | `remark` | 原样 |
| `creator` / 时间戳 | 同名列 | 原样 |
| `deleted` | `deleted` | 同上 |

**冲突**：`(dict_type, value)` 已存在 → **跳过 INSERT**；可选 Batch-2 脚本 UPDATE label/sort（本期 manual SQL 仅 skip）。

### 2.3 索引与约束差异

| wd | Football |
|----|----------|
| `UNIQUE uk_sys_dict_type (type)` | 无显式 UK（应用层校验） |
| `UNIQUE uk_sys_dict_data (dict_type, dict_value)` | 无显式 UK |

迁移脚本以 `NOT EXISTS` 保证幂等。

---

## 3. Football 端点速查（只读引用）

| 用途 | HTTP | 权限 |
|------|------|------|
| 字典类型分页 | `GET /admin-api/system/dict-type/page` | `system:dict:query` |
| 字典数据分页 | `GET /admin-api/system/dict-data/page` | `system:dict:query` |
| 按 type 查 data | `GET /admin-api/system/dict-data/type?type={type}` | `system:dict:query` |
| 创建 type | `POST /admin-api/system/dict-type/create` | `system:dict:create` |
| 创建 data | `POST /admin-api/system/dict-data/create` | `system:dict:create` |

| 菜单 id | 名称 | permission |
|---------|------|------------|
| 105 | 字典管理（页） | — |
| 1026 | 字典查询 | `system:dict:query` |
| 1027 | 字典新增 | `system:dict:create` |
| 1028 | 字典修改 | `system:dict:update` |
| 1029 | 字典删除 | `system:dict:delete` |
| 1030 | 字典导出 | `system:dict:export` |

Hash：`#/dict`（system 模块下）。

---

## 4. 并行子任务表

| id | 范围 | 主要文件/模块 | 依赖 | 可并行 |
|----|------|---------------|------|--------|
| **DM-01** | **手工数据迁移 SQL** | `docs/sql/V148__merge_ops_dict_to_football_manual.sql` | DB 备份 | **Y** |
| **DM-02** | **Flyway 菜单/权限下线** | `V148__remove_ops_dict_menu.sql` | — | **Y** |
| **DM-03** | 角色补 Football 字典权限 | `system_role_menu` 映射 6137→105,1026–1029 | DM-02 | **Y**（与 DM-02 同文件） |
| **DM-04** | 移除 OPS 字典 UI | `DictManage.vue`、router、Layout、`system-dict.ts`、football-front 副本 | DM-02 方案确认 | **Y** |
| **DM-05** | **后端：取消双轨** | `SystemDictServiceImpl` 删 business 分支；`SystemDictController` 删或 proxy | **DM-01 数据迁完** | **N** |
| **DM-06** | **DictService 改读 system** | `DictService.java`、`DictController`；`@InDict` IT | DM-05 | **N** |
| **DM-07** | 清理 wd 字典栈 | `SysDictTypeDO/DataDO`、Mapper；可选 deprecate 表 | DM-06 + 稳定期 | **N** |
| **DM-08** | 集成脚本/文档 | `seed-oa-system-menu.sql`、`oa-menu-permission-map.csv`、`OPS-MENU-LIST.md` | DM-02 | **Y** |
| **DM-09** | 测试与验收 | `MdbS2DictAdapterIT`、`M11DictS01IT`、UAT spotcheck §19 | DM-05–DM-06 | **N** |

---

## 5. 推荐执行顺序（并行批次）

```text
Batch-0（可全开 3 路并行）
  DM-01   ← 备份后于 shenyu-system 执行 manual SQL（wd → system）
  DM-02 + DM-03 + DM-08   ← Flyway V148 菜单/权限 + seed 文档

Batch-1（串行 gate — 依赖 DM-01 数据就绪）
  DM-05   ← SystemDictServiceImpl 单轨
  DM-06   ← DictService / DictController 读 system

Batch-2（可并行 UI）
  DM-04   ← 删 OPS DictManage；文档指向 #/dict

Batch-3（收敛）
  DM-07   ← deprecate sys_dict_*（可选 TRUNCATE，单独 ADR）
  DM-09   ← IT + UAT + smoke
```

---

## 6. 业务字典 type 清单（97，`dict_*` 前缀）

来源：`ops-platform-module-oa/src/main/resources/db/migration` Flyway seed 汇总（2026-07-18）。

<details>
<summary>按模块分组（点击展开）</summary>

**M1/M4 基础**

`dict_platform_type` · `dict_account_type` · `dict_account_status` · `dict_author_type` · `dict_anchor_type` · `dict_data_source` · `dict_yes_no` · `dict_gender` · `dict_id_type` · `dict_realname_status` · `dict_company_status` · `dict_intermediary_relation` · `dict_phone_status` · `dict_phone_type` · `dict_sim_status` · `dict_sim_operator` · `dict_triple_rel_type` · `dict_qualification_type` · `dict_wechat_usage_status` · `dict_industry` · `dict_ip_group_level` · `dict_time_dimension`

**M2 内容**

`dict_content_type` · `dict_content_status` · `dict_content_import_type` · `dict_content_review_result` · `dict_content_body_format` · `dict_content_length_type` · `dict_document_type` · `dict_knowledge_category` · `dict_review_stage` · `dict_review_status` · `dict_sop_node_type` · `dict_sop_node_status` · `dict_plan_status` · `dict_layout_template_status` · `dict_layout_template_source` · `dict_layout_import_job_status` · `dict_layout_style_category` · `dict_layout_style_status` · `dict_scheme_type` · `dict_anchor_style`

**M3 绩效**

`dict_perf_period` · `dict_perf_status` · `dict_perf_grade` · `dict_perf_metric_type`

**M5–M7 分析**

`dict_cost_type` · `dict_cost_pay_method` · `dict_cost_period` · `dict_funnel_type` · `dict_query_status` · `dict_dashboard_type` · `dict_monitor_freq` · `dict_alert_level` · `dict_roi_dimension`

**M8 配置 / 元数据**

`dict_ai_model_type` · `dict_ai_scene` · `dict_collect_frequency` · `dict_collect_method` · `dict_compare_operator` · `dict_config_status` · `dict_ecom_platform` · `dict_sync_frequency` · `dict_third_platform` · `dict_threshold_metric` · `dict_judge_mode` · `dict_match_type` · `dict_notify_channel` · `dict_prompt_type` · `dict_sync_mode` · `dict_threshold_category` · `dict_threshold_type` · `dict_param_category` · `dict_param_type` · `dict_metadata_query_condition_type` · `dict_metadata_entity_status`

**M9 系统（随字典页下线）**

`dict_user_status` · `dict_position` · `dict_tenant_status` · `dict_log_level` · `dict_log_module` · `dict_log_type` · `dict_message_category` · `dict_message_status`

**M10 采集 / 私域**

`dict_collect_source` · `dict_collect_data_type` · `dict_collect_status` · `dict_quality_check_type` · `dict_conn_status` · `dict_collector_bind_status` · `dict_aochuang_bind_status` · `dict_aochuang_sync_type` · `dict_aochuang_message_direction` · `dict_aochuang_message_type` · `dict_private_domain_identity_type` · `dict_private_domain_match_method` · `dict_private_domain_review_status`

</details>

完整字母序列表见 `docs/sql/V148__merge_ops_dict_to_football_manual.sql` 头部注释。

---

## 7. 验收标准（DoD 草案）

1. `shenyu-system.system_dict_type` 含全部 **97** 个 `dict_*` type（`SELECT COUNT(*) WHERE type LIKE 'dict_%' AND deleted=0` ≥ 97）。
2. 每个 type 的 data 行数 ≥ wd 源表（merge 后允许 Football 侧多于 wd 若曾有人工补录）。
3. OPS 菜单/路由**无**「字典配置」6137；`oa:dict:*` 权限停用。
4. 原持 6137 的角色可在 Football `#/dict` 访问（含 `system:dict:query`）。
5. `GET /admin-api/oa/dict/data?type=dict_platform_type` **code=0**，数据来自 system 库。
6. `@InDict` 校验 IT 绿；`mvn -pl ops-platform-module-oa test` 绿。
7. wd `sys_dict_*` **无新写入**（停写 gate 后 SQL 或 IT 断言）。
8. **零** `football-backend-saas/**` Java diff。

---

## 8. 阻塞 / 待确认项

| # | 问题 | 建议默认 |
|---|------|----------|
| Q1 | 冲突 data：skip only vs UPDATE label/sort？ | **skip only**（manual SQL）；差异人工复核 |
| Q2 | wd `sys_dict_*` 何时 TRUNCATE？ | 稳定 **2 周**后单独 Flyway + ADR |
| Q3 | OPS 是否保留 `/oa/system/dict/*` thin proxy？ | **否**；UI 直用 Football API |
| Q4 | `DictController.typeList` 返回范围：仅 `dict_*` 还是含平台 type？ | **全量** `system_dict_type`（与现 Adapter 一致） |
| Q5 | DM-01 执行环境：localhost 四库 vs 生产 | **先 localhost 演练**；生产维护窗 + 全库备份 |

---

## 9. 附录：关键代码索引

| 路径 | 说明 |
|------|------|
| `.../service/system/SystemDictAdapter.java` | `@DS("system")` 读 Football；`isBusinessDictType()` |
| `.../service/system/SystemDictServiceImpl.java` | 双轨路由（待 DM-05 删除 business 分支） |
| `.../service/dict/DictService.java` | 运行时缓存读 wd（待 DM-06 改 system） |
| `.../controller/dict/DictController.java` | 业务读 API |
| `.../controller/system/SystemDictController.java` | 管理 CRUD（待删） |
| `docs/sql/V148__merge_ops_dict_to_football_manual.sql` | 手工迁移脚本 |
| `.../db/migration/V148__remove_ops_dict_menu.sql` | 菜单/权限下线 |

---

*本文档为规划产出；实施前需确认 §8 待确认项。Manual SQL 须在 **shenyu-system 全库备份** 后执行。*
