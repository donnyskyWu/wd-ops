# ADR-018：分析/管理页前端 CSV 导出模式

| 字段 | 值 |
|------|---|
| 编号 | ADR-018 |
| 标题 | `exportToExcel` 客户端 CSV 导出（无后端 `/export`） |
| 状态 | ✅ Accepted（S-R24 及 2026-06-12/13 扩展已落地） |
| 日期 | 2026-06-13 |
| 决策人 | 工程（Gate 后补丁 + 近期 UI 会话） |
| 关联 | `SESSION-PROGRESS.md` S-R24 · `FRONTEND-FIX-PLAN.md` #14 |

---

## 1. 背景

多数分析页与管理页需要「导出 Excel」。后端 **未** 提供统一 `/export` REST 端点（FansAnalysis 等个别页曾有后端 CSV，其余为前端实现）。S-R6/S-R18 走查确认：Phase 1 采用前端 `exportToExcel` 工具导出 **UTF-8 BOM CSV**（文件名 `.csv`，用户可用 Excel 打开）。

---

## 2. 决策

### 2.1 工具契约

- 实现：`ops-platform-ui-vue/src/utils/index.ts` → `exportToExcel(rows, columns, filename)`
- 输出：浏览器下载 CSV（带 BOM），**非** `.xlsx`
- 列定义：`{ key, label }[]` 或字符串数组（label 兼 key）

### 2.2 数据来源

| 模式 | 适用 | 说明 |
|------|------|------|
| 当前页表格 | 列表已全量加载 | 直接 `tableData` |
| 全量拉取 | 分页报表 | `fetchAllPaginated` 后导出 |
| 降级 | 后端 CSV 失败 | 如 `Efficiency.vue` 降级本地列表 |

### 2.3 已接通页面（2026-06-13）

**M1 运营分析**：作品分析、粉丝分析、内部内容分析、人效分析

**M4 内部管理（6 页）**：公司、实名人、手机、手机卡、平台账号、个人账号

**M5/M6 分析/报表**：指标分析、漏斗、自定义查询结果、账号分析（高/低粉/外部）、内容分析（低分/爆款/IP 主题）、M6 八张报表页等

**M2**：内容列表

### 2.4 约束

- 导出内容为 **当前筛选条件下** 已加载数据；分页页默认导出需先 `fetchAllPaginated`（报表页已实现）。
- Phase 2 / M10 可引入 EasyExcel 后端导出；届时本 ADR 标记 Superseded，非本期范围。

---

## 3. 后果

- PRD/UX 中「导出 Excel」按钮 **语义** = 导出 CSV（Excel 可打开）。
- CHECKLIST 验收：页面存在导出按钮且调用 `exportToExcel`，不要求后端 endpoint。
- 测试 P2-#14：`missing exportToExcel` 页面数 ≤ 阈值（见 S-R18 报告）。
