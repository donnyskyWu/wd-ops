# ADR-084：投产比（ROI）运行时统一到 ops-server

| 项 | 值 |
|---|---|
| 状态 | 草稿（方案 B，2026-11-05 用户确认） |
| 关联 | ADR-058 · ADR-047 · ADR-049 · PRD-M-Live · `docs/handoff/roi-20261030/` |

## 背景

- 抖音投产管理 API 合同：`/admin-api/ops/roi/**`、部分 OA 桥接 `/admin-api/ops/oa/**`、`/admin-api/ops/finance/approval-costs/**`（handoff 02 · PRD-M-Live）。
- 实现曾落在 `football-server` 的 `@Profile("football-selfhost")` 包 `football.server.selfhost.roi`，本地 Gateway 通过 `gateway-roi-selfhost.yaml` 将上述路径 **优先** 转发到 `:48180` 独立进程。
- `football-module-ops-server`（`:48094`，Nacos `ops-server`）已承载 OPS 主栈与 Gateway 默认 `/admin-api/ops/**` 路由，但 **未** 注册 ROI 域 Bean，导致联调依赖第二条运行时。

## 决策（方案 B）

1. **运行时 SSOT**：ROI / OA 桥接 / approval-costs 与 OPS 其余能力同属 **`ops-server`（:48094）**。
2. **Gateway**：`/admin-api/ops/roi/**`（及同文件中的 OA、approval-costs 前缀）经 **`grayLb://ops-server`**，与 JAR 内 `ops-admin-api` 一致；**不再** 为 ROI 单独挂载 `order: -1` 的 `http://127.0.0.1:48180` 路由。
3. **代码归属**：ROI 域从 `football-server/.../selfhost/roi` **迁入** `football-module-ops-server/.../football/module/ops/roi`，包名 `football.module.ops.roi`；移除 `@Profile("football-selfhost")`；**禁止** 与 selfhost 长期双份逻辑。
4. **48180 selfhost**：标记 **deprecated**（本地可选、非 Gate 路径必需）；`start-integration-all.ps1` / `start-ops-dev.ps1` 不再依赖 `restart-selfhost-48180` 或 `gateway-roi-selfhost.yaml`。
5. **不变**：Football 用户 SSOT（ADR-056）、`tenant_id` 隔离、handoff 核算公式与 FR-M-Live 口径。

## 实现要点

| 区域 | 动作 |
|------|------|
| Gateway 联调 | `start-integration-all.ps1` 仅加载 `gateway-integration-local.yaml`；`gateway-roi-selfhost.yaml` 保留文件但标注 DEPRECATED |
| ops-server | 扫描 `football.module.ops` 含 `roi` 子包；Feign 增加 `PermissionApi`（`getNativeReadScope`） |
| selfhost | 删除 `football-server` 内 ROI 源码；`-Pfootball-selfhost` 打包 **不** 再作为 ROI 主路径 |
| 鉴权 | 沿用 Gateway `login-user` + `LoginUserContext`；Redis 与 system 一致（integration db0） |
| OCR / analysis / responsibility | 随 ROI 包一并注册在 ops-server |

## 验证（Gate 冒烟）

经 **48080 → 48094**（需有效登录 Token）：

- `GET /admin-api/ops/roi/bootstrap`
- `POST /admin-api/ops/roi/ocr/recognize`（或等价 OCR 端点）
- `GET /admin-api/ops/roi/report`

期望：`code` 为业务成功或非 500；**不要求** `:48180` 监听。

## 剩余项（后续 Slice）

- [ ] 生产 Nacos Gateway 路由确认无 ROI 直链 48180
- [ ] `football-selfhost` 文档与 `start-selfhost-local.ps1` 标注 ROI 已迁出
- [ ] ROI 域 IT 从 `football-server` 迁册并在 ops-server `mvn verify` 门禁
- [ ] 测试环境单 JAR 部署 ops-server 含 ROI 的发布检查清单

## 后果

- **正面**：一条 ops-server 进程 + Gateway 即可联调投产比；与 ADR-058 单仓 ops 命名一致。
- **负面**：ops-server fat jar 体积与启动 Bean 增加；selfhost 独立站若仍启 `-Pfootball-selfhost` 将不再自带 ROI（需改走 Gateway 或显式旧分支，不推荐）。
