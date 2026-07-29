# Football 产品仓库分支约定：`ops`

> 更新日期：2026-07-24

## 约定

Football 与 OPS 联调 / 合入相关开发，**一律使用 Gitee 上的 `ops` 分支**，不要把 OPS 集成工作提交到 `master`。

| 仓库 | Remote | 工作分支 |
|------|--------|----------|
| football-backend-saas | `git@gitee.com:taste-and-play/football-backend-saas.git` | **`ops`**（跟踪 `origin/ops`） |
| football-front | `git@gitee.com:taste-and-play/football-front.git` | **`ops`**（跟踪 `origin/ops`） |

## 本地操作

```powershell
cd football-backend-saas
git fetch origin
git checkout -B ops origin/ops

cd ..\football-front
git fetch origin
# 若工作区有未提交改动，先 stash 再切分支
git stash push -u -m "WIP before checkout ops"
git checkout -B ops origin/ops
```

切回本地 WIP（仅 front 示例）：

```powershell
git stash list
git stash pop stash@{0}   # 确认 stash 说明后再 pop
```

## 说明

- `master` 为 Football 产品主干；OPS 侧合入与联调改动走 `ops`。
- 本仓库（wd / OPS）日常开发不受此分支名限制；仅嵌入的两个 Football 子目录需遵守。
- Beta 测试环境连接：见 `docs/delivery/OPS-TEST-DB.md` 与 `scripts/integration-config/ops-test-remote.env.example`。

---

## Phase A：前端单源方向（过渡）

| 项 | 约定 |
|----|------|
| **源码真相** | 业务仍改 `ops-platform-ui-vue/**`，再 `python scripts/mount-ops-all.py` + `sync-ops-layout-components.py` 挂入 `football-front`（`ops` 分支） |
| **运行入口** | Gate / 日常以 Football `:5777` + Gateway `:48080` 打开 `/ops/*`；standalone `:3000` **非 Gate** |
| **Vite 代理** | `football-front/apps/web-ele/vite.config.mts` 默认 `localhost:48080`；Beta 远程改注释中的 `110.42…` |
| **退役目标** | 单源稳定后删除/归档 `mount-ops-all.py`（CLEANUP P2-2）；勿在冒烟绿前同时删 standalone |
| **工作计划** | [OPS-FOOTBALL-MERGE-WORK-PLAN.md](./OPS-FOOTBALL-MERGE-WORK-PLAN.md) Phase A（A-WP1–A-WP5） |

### 一键启动（Gate / 本地五库）

```powershell
# 仓库根目录 — 默认 localhost DB；勿与 Beta 远程 profile 混用
.\scripts\start-ops-dev.ps1
# 强制重挂 OPS 页面到 football-front：
.\scripts\start-ops-dev.ps1 -MountOps
```

启动脚本会：检查 `football-*` 是否在 **`ops`** 分支（非 ops 仅告警）→ 若 `views/ops` 缺失则自动 `mount-ops-all.py` → 校验 vite proxy → `localhost:48080` → `pnpm dev:ele` 起 `:5777` → 附带 **infra-server :48082**（文件上传 D-INF-01）。  
**不需要**再起 `ops-platform-ui-vue :3000`。登录：http://localhost:5777 · `admin` / `admin123` · 租户 **1**。  
DictSelect：`GET /admin-api/system/dict-data/simple-list`（Admin 无 `/type`）。
