# 缺陷库（DEF）

> **SSOT 关联**：[`E2E-AGENT-METHOD.md`](../../engineering/E2E-AGENT-METHOD.md) §6 · [`MASTER-EXECUTION-TRACKER.md`](../MASTER-EXECUTION-TRACKER.md) §13

业务 E2E / 走查发现的缺陷在此登记，与 Gate 阻塞表、Gate 报告 §缺陷 互补。

---

## 何时登记

| 场景 | 登记位置 |
|------|----------|
| E2E / Playwright **Fail**（业务不符合预期） | 本目录 `DEF-*.md` |
| 环境未就绪（oa-server DOWN、Nacos 不可达等） | **不**入本库 → `MASTER-EXECUTION-TRACKER.md` §13 记 Blocked |
| Gate 级阻塞（整阶段无法推进） | §13 阻塞表 + 可选本目录 DEF |
| 走查（S-R*）单页问题 | 本目录 DEF 或走查报告内嵌，修复后回归 |

---

## 文件命名

```
docs/delivery/defects/DEF-{YYYYMMDD}-{序号}.md
```

- **按条单文件**（推荐）：`DEF-20260727-001.md`
- **按日聚合**（可选）：`DEF-20260727.md` 内多条 `## DEF-20260727-001`

序号从 `001` 起，同日递增。

---

## 新建缺陷

1. 复制 [`DEF-TEMPLATE.md`](./DEF-TEMPLATE.md) 为新文件
2. 填写 **关联用例**（TC-ID / CHECKLIST 条目）、**现象 / 预期 / 复现步骤**
3. 附上 **证据**：截图（`test-results/` 或 [`e2e-artifacts/`](../e2e-artifacts/)）、API、日志摘录
4. 写明 **限定修复范围**（文件/模块；Football 改动注明 `ops` 分支）
5. 状态流转：`Open` → `Fixed` → `Verified`

---

## 与开发 Agent 交接

缺陷单 + TC-ID + 证据路径 + 限定文件范围 → 开发 Agent 修复（见 E2E-AGENT-METHOD §6.2）。

修复后由 **E2E Agent** 重跑失败 TC + 同模块 P0 冒烟，Pass 后将 DEF 标为 `Verified`。

---

## 示例

[`DEF-20260727-001-example.md`](./DEF-20260727-001-example.md) 为格式示例，**可删除**，不参与真实缺陷跟踪。
