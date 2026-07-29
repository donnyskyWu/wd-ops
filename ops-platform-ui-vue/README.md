# ops-platform-ui-vue · 运营数据平台前端

Vue 3 + Vite + Element Plus。**独立开发入口**（ADR-049 D6）：本地 `:3000` 经 Vite 代理访问 oa-server dev `:8080`。生产路径为 Football `:5777` + Gateway `:48080`。

后端启动说明见 [`ops-platform-server/README.md`](../ops-platform-server/README.md)。

## 前置

| 依赖 | 说明 |
|------|------|
| Node.js | 建议 18+（与 Vite 5 兼容） |
| npm | 随 Node 安装 |

## 一键启动（standalone dev harness）

仓库根目录（**不含 Football / Nacos / collector**）：

```powershell
.\scripts\start-ops-standalone.ps1
```

等价手动步骤见下方「安装与开发」。

## 安装与开发

```powershell
cd ops-platform-ui-vue
npm install
npm run dev
```

| 项 | 值 |
|----|-----|
| 前端 | `http://localhost:3000`（`vite.config.ts` → `server.port: 3000`） |
| 后端 | `http://localhost:8080`（oa-server profile **`dev`**，无 Nacos 注册） |
| API 代理 | `/admin-api` → `http://localhost:8080` |

**先启动后端**（另开终端）：

```powershell
cd ops-platform-server/ops-platform-module-oa
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

## Dev Token 与租户

请求封装见 `src/utils/request.ts`：

- `Authorization: Bearer <token>`
- `X-Tenant-Id`（默认 `1`）

`.env.development` 已对齐 seed（勿提交生产密钥）：

```env
VITE_API_TOKEN=dev-token-oa-admin
VITE_TENANT_ID=1
VITE_API_BASE_URL=/admin-api/oa
```

也可在浏览器 `localStorage` 设置 `token`、`tenantId`。Token 须与 oa-server dev profile 固定 Token（ADR-003）一致。

## 侧栏菜单与 ADR-049

Standalone 侧栏与 Football 集成 seed **系统管理（OA）** 子集对齐（见 `scripts/integration-config/seed-oa-system-menu.sql`）：

| 菜单 | seed ID | 路由 |
|------|---------|------|
| 系统参数 | 6141 | `/system-param` |
| 字典配置 | 6137 | `/system-dict` |
| 登录日志 | 6138 | `/system-log/login` |
| 操作日志 | 6139 | `/system-log/operation` |
| 消息管理 | 6140 | `/system-message` |
| 元数据维护 | 6165 | `/config-metadata`（配置管理） |

**M9 身份页已物理删除**（ADR-049 D4/D7 · CLEANUP Phase A）：`UserManage` / `RoleManage` / `TenantManage`（及 mock 死页 `SystemManage` / `ConfigManage`）已移除；`/system-user`、`/system-role`、`/system-tenant` 仅保留书签兼容路由，跳转 Football Admin（`FootballAdminRedirect`），**侧栏已隐藏**。用户/角色/租户请在 Football 原生菜单维护。

## 构建

```powershell
npm run build
```

产物目录：`dist/`。部署到 Nginx 或其他静态托管，并将 `/admin-api` 反向代理到生产后端地址。

本地预览构建结果：

```powershell
npm run preview
```

## E2E 测试

```powershell
npm run test:e2e
```

联调类用例：`npm run test:e2e:integration`（需 oa-server `:8080` 可用）。

## 本地全栈流程

1. 启动 oa-server：`ops-platform-module-oa`，profile **`dev`**，端口 **8080**
2. 本目录 `npm run dev`，端口 **3000**
3. 打开 `http://localhost:3000`
4. 确认 `.env.development` / `.env.local` 或 localStorage 中 Token / 租户与后端一致

Football 集成联调（Gateway/Nacos）见 [`docs/delivery/INTEGRATION-S0-Football-Ops.md`](../docs/delivery/INTEGRATION-S0-Football-Ops.md)；**不要**与 standalone `:8080` 混用同一端口。

## 部署说明（简要）

- 构建：`npm run build` → 上传 `dist/` 至静态服务器
- 生产环境通过 Nginx（或网关）配置 API 反向代理，**不要**在前端仓库硬编码生产密钥
- 生产 API 基址可通过构建时环境变量或网关统一前缀调整（当前 dev 使用相对路径 `/admin-api`）

## 常见问题

| 现象 | 处理 |
|------|------|
| 接口 404 / 网络错误 | 确认 oa-server **8080** 已启动（profile `dev`）；检查 Vite proxy |
| 401 / 无权限 | 检查 `VITE_API_TOKEN`、`X-Tenant-Id` 与 dev 用户权限 |
| 端口 3000 占用 | 修改 `vite.config.ts` 中 `server.port` 或结束占用进程 |
| 需要用户/角色/租户 | 使用 Football `:5777` 系统管理，非 standalone M9 页 |
