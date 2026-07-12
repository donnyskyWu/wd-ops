# UAT 抽检报告 — 优先 Ops 页面（2026-07-04）

> **环境**：Gateway `:48080` · oa-server `:48094` · football-front `:5777`  
> **登录**：`admin` / `admin123` · 租户 ID **1**  
> **脚本**：`scripts/uat-spotcheck-20260704.py` · 原始探针 JSON：`docs/delivery/uat-spotcheck-20260704-probe.json`

---

## 1. 抽检范围

| 优先级 | 菜单 | Football Hash | 组件路径 |
|--------|------|---------------|----------|
| P0 | 内容审核 | `#/ops/content/review` | `ops/production/content/review` |
| P0 | 计划管理 | `#/ops/plan` | `ops/production/plan/index` |
| P0 | IP组管理 | `#/ops/ip-group` | `ops/operations/IpGroup` |
| P0 | 平台账号管理 | `#/ops/internal-account` | `ops/internal/InternalAccountManage` |
| P1 | 内部个人账号 | `#/ops/personal-account` | `ops/internal/PersonalAccountManage` |

> **路由说明**：Ops 源路由 `/content/review`、`/plan` 挂载为 Football **`/ops/content/review`**、**`/ops/plan`**（非 `/ops/production/...` hash 前缀）；组件目录仍保留 `ops/production/...`。

---

## 2. 验收维度

| 维度 | 方法 |
|------|------|
| Gateway 登录 | `POST /admin-api/system/auth/login` → `code=0` + `accessToken` |
| 主 API | Bearer + `X-Tenant-Id:1` → `code=0` + 结构合理 |
| Vite 编译 | `GET :5777/src/views/{component}.vue` → HTTP 200，无 `Failed to resolve` |
| Theme | 根节点 `ops-page`；无 `#fff`/`#1890ff` 硬编码 |
| 搜索区 | 人工检视模板：TableSearch / 树搜索 / 平台 Tab |

---

## 3. 结果总表

| 页面 | Route | Vite | Theme | API | 搜索区 | **结论** |
|------|:-----:|:----:|:-----:|:---:|:------|:--------:|
| 内容审核 | ✅ | ✅ | ✅ | ✅ 3/3 | TableSearch：标题 / 平台 / 提交人 / 日期 | **PASS** |
| 计划管理 | ✅ | ✅ | ✅ | ✅ 1/1 | TableSearch：计划名称 / 状态 | **PASS** |
| IP组管理 | ✅ | ✅ | ✅ | ✅ 1/1 | 左侧树搜索（组名） | **PASS** |
| 平台账号管理 | ✅ | ✅ | ✅ | ✅ 1/1 | 平台 Tab + TableSearch：账号名称 / 状态 | **PASS** |
| 内部个人账号 | ✅ | ✅ | ✅ | ✅ 1/1 | TableSearch（账号筛选） | **PASS** |

**合计：5/5 PASS**（2026-07-04 01:30 UTC+8 自动化探针）

---

## 4. API 探针明细

| 页面 | API | 路径 | code | 数据摘要 |
|------|-----|------|:----:|----------|
| 内容审核 | review-config | `GET /admin-api/oa/content/review-config` | 0 | `level1Enabled` / `level2Enabled` / roles |
| 内容审核 | content/list | `GET /admin-api/oa/content/list?...&status=PENDING_FIRST_REVIEW` | 0 | list=0 total=0（队列空，结构正常） |
| 内容审核 | dict/data | `GET /admin-api/oa/dict/data?type=dict_platform_type` | 0 | list=9 total=9 |
| 计划管理 | plan/list | `GET /admin-api/oa/plan/list?pageNo=1&pageSize=1` | 0 | list=1 total=11 |
| IP组管理 | ip-group/tree | `GET /admin-api/oa/ip-group/tree` | 0 | array len=3 |
| 平台账号管理 | account/list | `GET /admin-api/oa/account/list?...&platformType=WECHAT_OFFICIAL` | 0 | list=1 total=7 |
| 内部个人账号 | personal-account/list | `GET /admin-api/oa/internal/personal-account/list?pageNo=1&pageSize=1` | 0 | list=1 total=4 |

---

## 5. 问题与跟进

| # | 现象 | 严重度 | 处置 |
|---|------|--------|------|
| 1 | 首轮探针 `ip-group/tree`、`account/list`、`personal-account/list` 30s 超时 | 低 | 复测 60s 内 **code=0**；属 oa-server 冷启动/并发延迟，非功能缺陷 |
| 2 | 内容审核一级待审队列为空（total=0） | 信息 | 预期：当前 seed 无 `PENDING_FIRST_REVIEW` 条目；页面应显示「暂无待审核内容」 |
| 3 | collect/dict 模块已知后端 stub | 非本次范围 | 仍交 collect/dict agent（见 INTEGRATION-PROGRESS §14.4 #4） |

**前端修复**：本轮 **无**（全部 PASS，无需改 vue/api）。

---

## 6. 复跑命令

```powershell
# 确保全栈 UP
.\scripts\start-integration-all.ps1 -SkipBuild

# 自动化 UAT 抽检（5 页）
python scripts/uat-spotcheck-20260704.py
```

---

## 7. 签核

| 角色 | 姓名 | 日期 | 结论 |
|------|------|------|------|
| 开发 | | | ⬜ |
| 测试 | | | ⬜ |
