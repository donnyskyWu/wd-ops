# OPS 产品交付包清单

**交付日期**：2026-09-20  
**产品版本**：PRD v9.2（2026-09-17）  
**环境假设**：线上 [`https://saas.shenyu.com`](https://saas.shenyu.com)（Hash `#/ops/...`）；本地联调 `http://localhost:5777`  

---

## 1. 本目录（产品交付文档）

| 文件 | 说明 |
|------|------|
| [`README.md`](./README.md) | 本清单 |
| [`OPS产品交付-需求文档.md`](./OPS产品交付-需求文档.md) | **自包含**需求规格（业务版 v9.2 正文 + 功能点/M2 FR·AC + 非功能与验收） |
| [`OPS产品交付-技术文档.md`](./OPS产品交付-技术文档.md) | **自包含**技术规格（架构、模块映射、REST 摘要表、数据/安全/集成/部署摘要） |
| [`OPS产品交付-部署运维手册.md`](./OPS产品交付-部署运维手册.md) | **自包含**部署运维（Greenfield SQL、环境变量、Nacos/Gateway、启动、Collector/AI 联调、FAQ、安全清单） |

---

## 2. 产品使用文档（`docs/product/`）

| 文件 | 说明 |
|------|------|
| [`产品使用操作手册.md`](../../product/产品使用操作手册.md) | **操作正文 SSOT**（Markdown） |
| [`delivery/汇总产品使用手册.md`](../../product/delivery/汇总产品使用手册.md) | 交付索引 + 五本角色摘要 |
| [`delivery/运营主管操作手册.md`](../../product/delivery/运营主管操作手册.md) | 分角色 v2.0 |
| [`delivery/运营操作手册.md`](../../product/delivery/运营操作手册.md) | 分角色 v2.0 |
| [`delivery/编辑操作手册.md`](../../product/delivery/编辑操作手册.md) | 分角色 v2.0 |
| [`delivery/账号管理员操作手册.md`](../../product/delivery/账号管理员操作手册.md) | 分角色 v2.0 |
| [`delivery/财务人员操作手册.md`](../../product/delivery/财务人员操作手册.md) | 分角色 v2.0 |
| [`delivery/README.md`](../../product/delivery/README.md) | 本目录结构说明 |
| [`delivery/ROUTE-线上地址索引.md`](../../product/delivery/ROUTE-线上地址索引.md) | 功能 → 线上 Hash URL（70 条） |
| [`delivery/通用入门与权限说明.md`](../../product/delivery/通用入门与权限说明.md) | 登录、权限、常见错误 |
| [`delivery/端到端业务流程手册.md`](../../product/delivery/端到端业务流程手册.md) | 主业务链路 |
| [`delivery/系统配置与权限管理手册.md`](../../product/delivery/系统配置与权限管理手册.md) | 系统参数 / AI / 元数据 |
| [`OPS业务操作手册-IP组与工作任务.md`](../../product/OPS业务操作手册-IP组与工作任务.md) | IP 组 + 工作任务专项 |
| [`delivery-screenshots/`](../../product/delivery-screenshots/) | 交付用截图（同步自 manual-screenshots） |

### Word / PDF 生成

```powershell
cd "d:\self\sy\运营数据平台\202606\wd"
pip install python-docx Pillow
python docs/product/_build_product_delivery_docx.py
```

**输出位置**（`docs/product/delivery/`）：

- `汇总产品使用手册.docx`（全文 + 截图，源自操作手册 SSOT）
- `运营主管操作手册.docx` … `财务人员操作手册.docx`（五本角色手册）
- `通用入门与权限说明.docx` · `端到端业务流程手册.docx` · `系统配置与权限管理手册.docx`

**仅生成原完整手册**（输出在 `docs/product/`）：

```powershell
python docs/product/_build_manual_docx.py
python docs/product/_build_workflow_manual_docx.py
```

**PDF（可选）**：脚本在已安装 `docx2pdf` 且本机有 Word 时会尝试生成 `汇总产品使用手册.pdf`；否则请 Word 另存为 PDF。

---

### 三件套 Markdown 再生成

客户 Zip 内三份 `.md` 均为**正文自包含**（不依赖仓库其他 Markdown 阅读）。源码变更后可在仓库根目录执行：

```powershell
cd "d:\self\sy\运营数据平台\202606\wd"
python docs/delivery/product-handover/_assemble_standalone_handover.py   # 需求 + 技术 + 部署（链式调用 deploy 脚本）
# 或单独：
python docs/delivery/product-handover/_assemble_standalone_deploy.py    # 仅部署运维手册
```

---

## 3. 需求与设计 SSOT

**客户 Zip 内副本**：`process-docs/`（与三件套并列，打包脚本 [`_pack_process_docs.py`](./_pack_process_docs.py) 维护收集规则）。

| 文档 | 仓库路径 | Zip 内路径 |
|------|----------|------------|
| 过程文档总目录 | [`OPS过程文档-目录.md`](./OPS过程文档-目录.md) | `product-handover/` + `process-docs/` |
| 开发版 PRD | [`完整PRD-v9.2-开发版.md`](../../../完整PRD-v9.2-开发版.md) | `process-docs/prd/` |
| 业务版 / 模块 PRD | `docs/product/PRD-*.md` | `process-docs/prd/` |
| API | `docs/engineering/API-M*.md` | `process-docs/api/` |
| 库表 / Greenfield | `docs/deploy/ops-greenfield-production/` | `process-docs/database/` |
| ADR / UX / 方法 | `docs/adr/` · `docs/product/UX-M*.md` · `PHASE-DEV-METHOD.md` 等 | `process-docs/design/` |
| 切片 / 清单 / 用例 | `docs/delivery/SLICES*` · `CHECKLIST*` · `TESTCASES*` · `gates/` | `process-docs/delivery/` |
| 开发启动 | [`OPS-DEV-DEPLOY-GUIDE.md`](../OPS-DEV-DEPLOY-GUIDE.md) | `process-docs/delivery/` |

---

## 4. 客户交付包（Zip）

| 项 | 路径 |
|----|------|
| **交付包** | `OPS产品交付包-YYYYMMDD.zip`（与本目录 [`_pack_delivery_zip.py`](./_pack_delivery_zip.py) 同目录；文件名按打包日生成） |
| 打包脚本 | [`_pack_delivery_zip.py`](./_pack_delivery_zip.py) |

### 包内目录结构

| Zip 前缀 | 内容 |
|----------|------|
| `product-handover/` | **客户三件套**（必选）：`OPS产品交付-需求文档`、`OPS产品交付-技术文档`、`OPS产品交付-部署运维手册` 各 `.md` + `.docx`；`README.md`；[`OPS过程文档-目录.md`](./OPS过程文档-目录.md) |
| `product-manuals/` | `docs/product/delivery/` 下角色/流程手册 `.docx`、`.md`（不含 `_` 前缀脚本说明） |
| `delivery-screenshots/` | 交付截图 PNG + `README.md` |
| **`process-docs/`** | **研发过程文档**：`prd/` · `api/` · `database/` · `design/`（adr/ux/method/state）· `delivery/`（SLICES/CHECKLIST/TESTCASES/gates）；`SQL-MANIFEST.txt`；详见 [`OPS过程文档-目录.md`](./OPS过程文档-目录.md) |
| 收集 / 校验脚本 | [`_pack_process_docs.py`](./_pack_process_docs.py)（仅统计，不写 Zip） |

打包前若三件套 `.docx` 缺失或早于对应 `.md`，脚本会自动调用 [`_build_handover_docx.py`](../../product/_build_handover_docx.py) 重建。

重新打包：

```powershell
cd "d:\self\sy\运营数据平台\202606\wd"
python docs/delivery/product-handover/_pack_delivery_zip.py
```

### 三件套 Word

```powershell
python docs/product/_build_handover_docx.py
```

输出：`OPS产品交付-需求文档.docx`、`OPS产品交付-技术文档.docx`、`OPS产品交付-部署运维手册.docx`。

### 截图增量（2026-09-20）

**主流程重采**（登录联调通过后，`admin` / `admin123`，租户「神鱼体育」）：

```powershell
python docs/product/_capture_delivery_screenshots.py
```

| 目录 | PNG 数量 | 说明 |
|------|----------|------|
| [`manual-screenshots/`](../../product/manual-screenshots/) | 18 | 主流程 01–15（含 05b 历史） |
| [`manual-screenshots/workflow/`](../../product/manual-screenshots/workflow/) | 18 | A00–A13、B01–B04（2026-08-25，本次未重跑 Playwright workflow） |
| [`delivery-screenshots/`](../../product/delivery-screenshots/) | 36 | 主流程 + A/B 扁平同步（打包用） |

主流程本次覆盖：`01-login` … `15-data-report`，含系统参数三 Tab（`12` / `12b` / `12c`）。`01-login` 已确认无错误 Toast。

清单见 [`delivery-screenshots/README.md`](../../product/delivery-screenshots/README.md)。

## 5. 已知缺口（2026-09-20）

| 项 | 说明 |
|----|------|
| 配置管理子页 | 除系统参数外多数无独立截图 |
| M10 采集运维 | Phase 2，交付文档仅边界说明 |
| Vite 代理 | 已改回 `localhost:48080`；改 `vite.config.mts` 后需重启 `:5777` |

---

## 6. 验收、测试用例与 Gate

| 类型 | 仓库路径 | Zip 内路径 |
|------|----------|------------|
| 总进度 | [`MASTER-EXECUTION-TRACKER.md`](../MASTER-EXECUTION-TRACKER.md) | `process-docs/test/reports/` |
| 模块清单 | `docs/delivery/CHECKLIST-M*.md`（13） | `process-docs/test/testcases/` |
| 模块用例 | `docs/delivery/TESTCASES-M*.md`（13） | `process-docs/test/testcases/` |
| Gate / 签收 | `docs/delivery/gates/*.md` | `process-docs/test/reports/` |
| Playwright 索引 | 打包时扫描 `football-front/.../tests`、`ops-platform-ui-vue/tests` | `process-docs/test/testcases/PLAYWRIGHT-OPS-SPECS-INDEX.md` |
| Playwright 源码 | `*ops*.spec.ts` · `*uat*.spec.ts` 等（子模块已检出时） | `process-docs/test/automation/playwright/`（相对 `tests/` 路径） |

打包后脚本会打印 `process-docs/test/*` 文件数、Playwright `.ts` 数量与 Zip 总体积。

---

## 修订记录

| 日期 | 说明 |
|------|------|
| 2026-09-20 | 首版交付 manifest |
| 2026-09-20 | 登录联调修复说明、主流程截图重采、三件套 docx、交付 Zip |
| 2026-09-20 | 登录可用后主流程 Playwright 重采；六本角色手册 + 操作手册 docx 重建；Zip 重打 |
| 2026-09-20 | 打包脚本显式纳入三件套 md/docx、过期 docx 自动重建；更新 Zip manifest |
| 2026-09-20 | 部署运维手册改为自包含全文；`_assemble_standalone_deploy.py` 与 handover 链式再生成 |
| 2026-09-22 | 交付 Zip 增加 `process-docs/` 过程文档包；`OPS过程文档-目录.md` · `_pack_process_docs.py` |
| 2026-09-22 | 交付 Zip 纳入 `process-docs/`；测试用例与 Gate 报告归入 `test/testcases`、`test/reports` |
| 2026-09-22 | 交付 Zip 纳入 Playwright ops/uat spec 源码至 `test/automation/playwright/` |
