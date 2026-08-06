# CONTENT-CREATE-FIX-20260804

## 现象

内容管理 → **新增内容** → 页面 toast「系统错误」/ Vite 编译失败，抽屉无法打开。

## 根因

`football-front/apps/web-ele/src/api/ops/author.ts` 中 `getAuthorExt` 函数签名被误替换为注释，模块出现 dangling `return`，**TS/Vite 无法编译该模块**。

`ContentEditPanel.vue`（新增内容抽屉）静态 import：

```ts
import { getAuthorPage } from '#/api/ops/author'
```

点击「新增内容」→ 挂载 `ContentEditDialog` → 加载 `ContentEditPanel` → 解析 `#/api/ops/author` 失败 → 前端报错。

**非** 后端 404/500；抽屉初始化 API（profile / ip-groups / accessible-tree / dict）在本机探测均为 `code:0`。

## 修复

| 仓库 | Commit | 文件 |
|------|--------|------|
| football-front | `857cf561` | `apps/web-ele/src/api/ops/author.ts` — 恢复 `export function getAuthorExt(...)` |

## 验证（2026-08-04）

| 项 | 结果 |
|----|------|
| Gateway API smoke（profile / ip-groups / accessible-tree / content/list） | ✅ code 0 |
| Playwright `CONTENT-GATE-001` | ✅ pass |
| Playwright `CONTENT-GATE-002`（新增内容 drawer） | ✅ pass |

```powershell
cd football-front/apps/web-ele
npx playwright test tests/football-content-smoke.spec.ts -g "CONTENT-GATE-00"
```

## 人工验收

1. 确认 `football-front` 在 `857cf561` 或更新（`git -C football-front log -1`）
2. 刷新 `:5777`（必要时硬刷新 Ctrl+F5）
3. admin/admin123 → 内容管理 → **新增内容**
4. 预期：右侧 drawer 标题「新增内容」，表单含 IP 组 / 标题 / 内容类型等字段
