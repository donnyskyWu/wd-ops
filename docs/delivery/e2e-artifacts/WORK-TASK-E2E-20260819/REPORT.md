# WORK-TASK-E2E 报告：FR-M2-010 工作任务管理

**日期** 2026-08-19 · **模块** M2 内容生产 · **路径** `/ops/production/work-task` · **环境** Gate `:5777` / Gateway `:48080` / Ops `:48094` · admin/admin123 tenant=1 · Beta DB 110.42.49.224

## 一、走查结论（一句话）

**P0 修复后 API 冒烟全绿**（2026-08-19 18:25）：V183 菜单 6194–6196 + shenyu-system 字典 SSOT 已灌入 beta；`get-permission-info` 含 `ops:work-task:*`；save 2 行 / confirm / matrix 均 code=0。**UI 待复测**（需重新登录刷新菜单缓存）。

## 十二、P0 修复记录（2026-08-19）

| Bug | 修复 | 状态 |
|-----|------|------|
| WT-E2E-01/02 | V183 菜单 **6194**（list）/ **6195**（register）/ **6196**（manage）；beta 6176–6190 已被 APP 开屏广告等占用 | ✅ test DB applied |
| WT-E2E-03 | V183 同步 4 类 dict → `shenyu-system.system_dict_*`（Feign SSOT） | ✅ LIVE_PUBLIC/DOUYIN 校验通过 |
| WT-E2E-04 | `saveSheet`：`putIfAbsent` 替代 `toMap` + 批内重复键校验 | ✅ 2-row save code=0 |
| WT-E2E-05 | 测试数据：IP 组 9016 需 bind 作者（非代码缺陷） | ⚠️ 文档备注 |

### 新菜单 ID

| menu_id | permission | 说明 |
|--------:|------------|------|
| 6194 | `ops:work-task:list` | 工作任务管理页面 |
| 6195 | `ops:work-task:register` | 登记按钮 |
| 6196 | `ops:work-task:manage` | 矩阵按钮 |

### SQL / 脚本

| 目标 | 脚本 | 结果 |
|------|------|------|
| Flyway | `V183__m2_work_task_menu_dict_fix.sql` | 新增 |
| Beta test (110.42.49.224) | `python scripts/integration-config/apply_v183_work_task_fix.py --target test` | ✅ menus=3, dict_types=4, dict_data=11, role_menu=6 |
| Local | 同上 `--target local` | ⚠️ 本地 `system_dict_type` 无 `remark` 列，需先 seed Football 菜单或手工适配 |

### API 冒烟（修复后）

```text
GET  get-permission-info          → ops:work-task:list/register/manage ✅
GET  sheet/get-or-create          → code=0 sheetId=1
PUT  sheet/save (1 row + dict)    → code=0
PUT  sheet/save (2 rows)          → code=0
POST sheet/confirm                → code=0
GET  matrix                       → rows=2
GET  matrix/summary               → totalTasks=2
```

脚本：`python scripts/integration-config/smoke_work_task_api.py`（需 Gate :48080 + Ops :48094，`X-Tenant-Id: 1`）

### mvn

`mvn -pl football-module-ops/football-module-ops-server -am compile test` → **BUILD SUCCESS**（24 tests）

---

## 一（原始）、走查结论（一句话）

**P0 通过率 39%（7/18）**：栈健康且重建 Ops 后 API 骨架可用（get-or-create / 单行 save / matrix）；**UI 全阻断**（菜单 6176 ID 冲突 + 路由落欢迎页）；**confirm 全链路阻断**（Feign 字典未灌入 + IP 组初始无作者）。

## 二、Pre-flight

| # | 步骤 | 期望 | 实际 | 结果 |
|---|------|------|------|------|
| P0 | Gateway :48080 | 200 | 200 | ✅ |
| P0 | Frontend :5777 | 200 | 200 | ✅ |
| P0 | Ops :48094 health | UP | UP | ✅ |
| — | 初始 Ops JAR 无 work-task | — | `/ops/work-task/*` 404 | ⚠️ 已 `-Rebuild` 重启 |

## 三、Tab1 任务登记（P0）

| # | AC / 步骤 | 期望 | 实际 | 结果 | 证据 |
|---|-----------|------|------|------|------|
| T1-1 | 导航 内容生产→工作任务管理 | 页面加载 | `#/ops/production/work-task` 重定向 `/welcome`；Ctrl+K 搜「工作任务」无菜单项 | ❌ | `04-work-task-after-tenant.png` |
| T1-2 | 两个 Tab | 任务登记 + 任务管理 | UI 未进入 | ❌ BLOCK | — |
| T1-3 | 选 IP 组 + 日期 | 自动/手动选择 | API：`get-or-create` ipGroupId=9016 workDate=2026-08-19 → 10 行 DRAFT | ✅ API | `03-get-or-create.json` |
| T1-4 | 填 1–2 行 | 赛事/作者/执行人/营销/直播/平台 | UI 阻断；API 逐行 save 成功（行1+行2）；**批量 save 2 行 → HTTP 500** | ⚠️ PARTIAL | `04-get-or-create-after-save.json` |
| T1-5 | 保存草稿 | success toast | API 单行 save code=0 | ✅ API | — |
| T1-6 | 确认登记 | 生成任务 | code=1400「第 1 行营销计划不能为空」；带 `LIVE_PUBLIC`/`DOUYIN` save → **1503 字典无效** | ❌ | `05-confirm-attempt.json` |
| T1-7 | 我的任务 | CONTENT_GENERATION 待办 | 无新增（confirm 未成功）；my-tasks 现有 6 条旧任务 | ❌ SKIP | `07-my-tasks-before-confirm.json` |
| T1-8 | 撤回登记 | 回草稿 + 任务取消 | 未执行（无 CONFIRMED sheet） | ❌ SKIP | — |

## 四、Tab2 任务管理（P0）

| # | AC / 步骤 | 期望 | 实际 | 结果 | 证据 |
|---|-----------|------|------|------|------|
| T2-1 | 确认后切 Tab2 | 矩阵有数据 | UI 阻断；API 在 DRAFT 下 matrix 空 | ❌ UI / ✅ API empty | `06-matrix-draft.json` |
| T2-2 | 同日期查询 | rows + author 列 | rows=0 cols=0（无 CONFIRMED 数据） | ⚠️ | `06-matrix-draft.json` |
| T2-3 | Summary 统计条 | totalTasks 等 | totalTasks=0 totalMatchRows=0 livePublic=0 paidSales=0 | ✅ API | `06-matrix-summary-draft.json` |
| T2-4 | Badge LIVE_PUBLIC/PAID_SALES | 绿/橙标签 | 无 confirmed 单元格，未验证 | ❌ SKIP | — |

## 五、红黑 P1

| # | 步骤 | 期望 | 实际 | 结果 |
|---|------|------|------|------|
| R1 | POST refresh-win-prediction | 已完赛可刷新 | code=2023「仅已确认登记可刷新红黑」 | ⚠️ BLOCK（sheet=DRAFT，符合规则） |

## 六、发现清单（Bug / 环境）

| ID | 级别 | 问题 | 位置 / 范围 |
|----|------|------|-------------|
| WT-E2E-01 | **P0** | `system_menu.id=6176` 在 beta **已被「APP开屏广告管理」占用**，V181 工作任务菜单 `INSERT … WHERE NOT EXISTS` 静默跳过；权限树无 `ops:work-task:*` / `work-task` 路由 | `V181__m2_work_task_foundation.sql` §5 · shenyu-system DB |
| WT-E2E-02 | **P0** | 直达 `#/ops/production/work-task` 落欢迎页（动态路由未注册） | 前端 `access.ts` + 菜单缺失 |
| WT-E2E-03 | **P0** | Feign 字典 `dict_marketing_plan_type` / `dict_sales_platform` **不存在**，save 带营销/平台 → 1503 | V181 §2 未应用到 system-server 字典库；`SystemDictAdapter` |
| WT-E2E-04 | **P1** | `PUT /sheet/save` **多行同一请求** 500；逐行 save 正常 | `WorkTaskServiceImpl.saveSheet` |
| WT-E2E-05 | **P2** | 组长 IP 组 9016 初始 **anchors=0**，登记页作者下拉为空（走查中临时 bind 107156） | 测试数据 / seed |
| WT-E2E-06 | **P2** | 初始运行 Ops JAR **未含 S-17 controller**（404），需 `-Rebuild` | 部署流程 |

## 七、API 实证摘要

```text
POST /admin-api/system/auth/login                         → code=0
GET  /admin-api/ops/ip-group/led                          → [9016 SSOT-Snow-小组]
GET  /admin-api/ops/work-task/sheet/get-or-create         → sheetId=1, 10 rows, DRAFT
PUT  /admin-api/ops/work-task/sheet/save (1 row)          → code=0
PUT  /admin-api/ops/work-task/sheet/save (2 rows)         → code=500
PUT  ... save + marketingPlan=LIVE_PUBLIC                 → code=1503
POST /admin-api/ops/work-task/sheet/confirm               → code=1400 (缺营销计划)
GET  /admin-api/ops/work-task/matrix?workDate=2026-08-19    → rows=0 (draft)
GET  /admin-api/ops/work-task/matrix/summary              → totalTasks=0
POST .../assignment/1/refresh-win-prediction              → code=2023
GET  /admin-api/ops/task/my-tasks                         → 6 条（无本次新增）
```

## 八、通过率

| 范围 | 通过 | 总计 | 率 |
|------|------|------|-----|
| Pre-flight | 3 | 3 | 100% |
| Tab1 P0 | 2 | 8 | 25% |
| Tab2 P0 | 1 | 4 | 25% |
| P1 红黑 | 0 | 1 | 0%（预期阻断） |
| **合计 P0** | **7** | **18** | **39%** |

## 九、Artifacts

| 文件 | 说明 |
|------|------|
| `00-login.json` | 登录 token |
| `01-led-groups.json` | 组长 IP 组 |
| `02-permission-info.json` | 权限树（无 work-task） |
| `03-get-or-create.json` | 登记表初始化 |
| `03-matches.json` | 当日赛事样本 |
| `04-get-or-create-after-save.json` | 保存后 sheet |
| `05-confirm-attempt.json` | confirm 失败 |
| `06-matrix-*.json` | 矩阵/汇总（draft） |
| `07-my-tasks-before-confirm.json` | 我的任务 |
| `08-refresh-win-prediction.json` | 红黑 refresh |
| `01-homepage.png` … `04-work-task-after-tenant.png` | 浏览器截图 |

## 十、留给后续 Slice

1. **修复菜单 ID 冲突**：V181 改用未占用 menu_id 或 upsert by `permission=ops:work-task:list`（非 id=6176）。
2. **字典灌入 SSOT**：确认 V181 dict 目标库（system-server Feign）并 idempotent apply。
3. **调查 batch save 500** + IT 覆盖多行 save。
4. **E2E 复测**：菜单可见 → Tab1 全流程 → Tab2 矩阵 badge → confirm 后 my-tasks → withdraw。

## 十一、反思

- **Spec/数据**：V181 `WHERE NOT EXISTS (id=6176)` 在存量 Football 库 silently fail，CHECKLIST 标 S-16 ✅ 但 Gate 环境菜单/字典未真就绪。
- **方法论**：走查前先 `GET /ops/work-task/sheet/get-or-create` + `get-permission-info` 过滤 `work-task`，可秒级发现 UI/API 双阻断。
- **Rebuild**：S-16~S-19 代码合入后必须 `start-integration-oa.ps1 -Rebuild`，否则 404 假阴性。
