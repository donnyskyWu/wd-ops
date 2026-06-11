# ops-platform-ui-vue · 运营数据平台前端

Vue 3 + Vite + Element Plus。本地开发通过 Vite 代理访问后端 `http://localhost:8080`。

后端启动说明见 [`yudao-server/README.md`](../yudao-server/README.md)。

## 前置

| 依赖 | 说明 |
|------|------|
| Node.js | 建议 18+（与 Vite 5 兼容） |
| npm | 随 Node 安装 |

## 安装与开发

```powershell
cd ops-platform-ui-vue
npm install
npm run dev
```

- 开发地址：`http://localhost:3000`（`vite.config.ts` 中 `server.port: 3000`）
- API 代理：`/admin-api` → `http://localhost:8080`（需先启动后端）

## Dev Token 与租户

请求封装见 `src/utils/request.ts`：

- `Authorization: Bearer <token>`
- `X-Tenant-Id`（默认 `1`）

本地开发可在项目根目录创建 `.env.local`（勿提交密钥仓库外泄）：

```env
VITE_API_TOKEN=dev-token-oa-admin
VITE_TENANT_ID=1
```

也可在浏览器 `localStorage` 设置 `token`、`tenantId`。与后端 dev profile 固定 Token 一致即可联调。

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

联调类用例：`npm run test:e2e:integration`（需后端可用）。

## 本地全栈流程

1. 启动后端：`yudao-server/yudao-module-oa`，profile `dev`，端口 **8080**
2. 本目录 `npm run dev`，端口 **3000**
3. 打开 `http://localhost:3000`
4. 确认 `.env.local` 或 localStorage 中 Token / 租户与后端一致

## 部署说明（简要）

- 构建：`npm run build` → 上传 `dist/` 至静态服务器
- 生产环境通过 Nginx（或网关）配置 API 反向代理，**不要**在前端仓库硬编码生产密钥
- 生产 API 基址可通过构建时环境变量或网关统一前缀调整（当前 dev 使用相对路径 `/admin-api`）

## 常见问题

| 现象 | 处理 |
|------|------|
| 接口 404 / 网络错误 | 确认后端 8080 已启动；检查 Vite proxy 是否指向正确地址 |
| 401 / 无权限 | 检查 `VITE_API_TOKEN`、`X-Tenant-Id` 与 dev 用户权限 |
| 端口 3000 占用 | 修改 `vite.config.ts` 中 `server.port` 或结束占用进程 |
