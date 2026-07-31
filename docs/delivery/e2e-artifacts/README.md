# E2E 证据归档（e2e-artifacts）

> **SSOT 关联**：[`E2E-AGENT-METHOD.md`](../../engineering/E2E-AGENT-METHOD.md) §5.3 · §8 · §11.4 M2

Playwright 运行时截图默认落在 `football-front/apps/web-ele/test-results/`（**gitignore，不入库**）。Gate 通过或 E2E 报告归档时，将 **关键 Pass/Fail 截图** 复制到本目录，供报告引用与人工复核。

---

## 何时复制

| 时机 | 动作 |
|------|------|
| Gate / 模块 E2E **P0 全绿** | 复制关键步骤 Pass 截图 |
| DEF **Fail** 登记 | 复制 Fail 截图 + 在 DEF 单写相对路径 |
| 回归 **Verified** | 可选追加 Verified 截图 |

**不要**复制整份 `test-results/` 或 `playwright-report/`（体积大、含 trace）。

---

## 目录命名

```
docs/delivery/e2e-artifacts/{批次}-{YYYYMMDD}/
```

示例：

| 目录 | 含义 |
|------|------|
| `CONTENT-GATE-20260727/` | 内容管理 Football Gate 试点（CONTENT-GATE-001~004） |
| `GATE-S4-20260728/` | S4 阶段 Gate 归档 |
| `M2-P0-20260730/` | M2 模块 P0 冒烟批次 |

---

## 文件命名

与运行时规范一致（见 E2E-AGENT-METHOD §5.3）：

```
{TC-ID}_{步骤}_{pass|fail}_{timestamp}.png
```

示例：

```
CONTENT-GATE-004_edit_pass_1785133147754.png
CONTENT-GATE-004_view_pass_1785133147754.png
M2-CONTENT-012_view_fail_1730000000.png
```

---

## 从 test-results 复制

```powershell
# 示例：内容 Gate 归档
$dest = "docs/delivery/e2e-artifacts/CONTENT-GATE-20260727"
New-Item -ItemType Directory -Force -Path $dest
Copy-Item football-front/apps/web-ele/test-results/content-edit-*.png `
  "$dest/CONTENT-GATE-004_edit_pass_<timestamp>.png"
Copy-Item football-front/apps/web-ele/test-results/content-view-*.png `
  "$dest/CONTENT-GATE-004_view_pass_<timestamp>.png"
```

在 E2E 报告 §5 证据索引中写 **相对路径**（如 `docs/delivery/e2e-artifacts/CONTENT-GATE-20260727/...`）。

---

## 与缺陷库的关系

- Fail 截图：先留 `test-results/`，登记 [`defects/`](../defects/) DEF 时引用；Verified 后可复制归档副本到本目录。
- Pass 截图：Gate 签收前复制关键步骤，避免 test-results 清理后报告外链失效。
