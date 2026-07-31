# P-G 删除 `legacy-archive` — 报告

| 字段 | 值 |
|------|---|
| 日期 | 2026-07-31 |
| Slice | P-G / 终态缺口计划 §P-G |
| 状态 | **✅ 完成** |
| 选项 | ① `git rm -r`（仅 git 历史保留） |

## 范围落地

| 项 | 结果 |
|----|------|
| 路径 | `football-backend-saas/football-module-ops/football-module-ops-server/legacy-archive/` |
| 删除 | **`git rm -r`** 422 tracked；另强制删 158 untracked disk leftovers → **合计 580 files** |
| 目录现状 | **不存在**；`git ls-files` = 0 |
| Maven | `pom.xml` 无自定义 `sourceDirectory`；archive **从未**在 compile/test classpath（默认仅 `src/main|test/java`） |
| 未做 | **P-E**（MasterTokenMapper）；无 commit（按授权仅删+文档） |

## 回滚

Archive 引入 commit（football-backend-saas）：

```text
7e5f1b709 feat(ops): ADR-058 CLEANUP — Flyway SSOT in football-module-ops
```

恢复命令：

```bash
git -C football-backend-saas checkout 7e5f1b709 -- \
  football-module-ops/football-module-ops-server/legacy-archive
```

说明：仅恢复当时已入库的 **422** 文件；158 个当时未跟踪的磁盘文件不可从该 commit 取回。

## 冒烟（RESULTS.json）

| Check | 结果 |
|-------|------|
| `:48094/actuator/health` | UP / http=200 |
| 直连 `:48094/admin-api/ops/account/list` | code=0 |
| GW `:48080/admin-api/ops/account/list` | code=0 |
| **合计** | **3/3** |

Admin 登录：`admin` / `admin123`，tenant=1（`login-user` + Bearer，同 P-D 模式）。

## 文档指针

- 执行计划 §P-G / §8 ✅
- [GAP-INVENTORY](../P5-MIGRATE-8-cutover/GAP-INVENTORY.md) CLEANUP 节已注删除
- WORK-PLAN §8.10 Phase E：剩余仅 P-E
- ADR-058 变更表 + Foundation 延后项指针
