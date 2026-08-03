# 内容管理：保存后列表无数据 — E2E 修复报告

**日期**：2026-07-30  
**页面**：内容管理 `/ops/production/content`（路由 `/content`）  
**环境**：localhost Front:5777 / Gateway:48080 / oa:48094；admin / admin123；tenant 1；本地五库（无 Beta）

## 一、结论（Pass）

admin 保存/创建内容后，`GET /admin-api/oa/content/list` 与 UI 表格均可见数据。  
API：create id=30 → 按标题筛 total=1；列表 total≥29。UI：表格 **20** 行（非「暂无内容数据」）。

## 二、根因

1. **数据范围（主因）**  
   Football `admin`（雪花 id）在 `oa_ip_group.leader_user_id` 上兼任 IP 组长，且角色含 `super_admin` + `ip_group_leader`。  
   `ContentDataScopeSupport.shouldApplyLedIpGroupContentScope` 本地 `isOaTenantAdmin` **未识别 `ROLE_super_admin`**，却优先于 `hasUnrestrictedIpGroupAccess` 走 led 过滤。  
   username 桥接不全时 led 仅含无内容的组（如 9016）→ **total=0**；DB 实际仍有 29 行。

2. **列表 500（修复数据范围后暴露）**  
   `enrichFootballFields` → `MemberArticleWriteService.getById` `@DS("member")` 读 `author_article`，但 `dev-local-multidb` 已去掉 member DS → 落到 `wd` → `Table 'wd.author_article' doesn't exist`。

## 三、修复

| 文件 | 改动 |
|------|------|
| `ContentDataScopeSupport.java` | 系统管理员用 `OpsDataScopeSupport.isOaTenantAdmin`（含 `super_admin`），不再被 led 收窄 |
| `ContentDataScopeSupportSuperAdminTest.java` | 单测：super_admin+组长不过滤；纯组长走 led |
| `ProductionContentServiceImpl.java` | `enrichFootballFields` 捕获异常，避免列表整页 500 |
| `FootballArticleBridgeServiceImpl.java` | 读 shelf status 失败时降级 |
| `application-dev-local-multidb.yml` | 恢复本地 `member` → `shenyu-member`（getById 残留路径） |

## 四、验证

| 项 | 结果 |
|----|------|
| `mvn -Dtest=ContentDataScopeSupportSuperAdminTest,OpsDataScopeSupportAdminRoleTest test` | Pass |
| API list（修复前） | total=0（DB 29） |
| API list（修复后） | code=0 total=29 |
| API create → list | create data=30；title 筛 total=1 |
| Browser：登录 → 内容管理 | rows=20；截图 `content-list.png` |
| Browser：新增内容 | Drawer 打开；`content-create-drawer.png` |

## 五、复现 / 回归步骤

1. 登录 `http://localhost:5777`（admin / admin123）  
2. 打开「运营数据 → 内容生产 → 内容管理」  
3. 期望：表格有行；`GET .../oa/content/list` total>0  
4. 新增内容并保存（或 API create）后刷新/关闭抽屉，列表可见新行  

## 六、Pass/Fail

**Pass**
