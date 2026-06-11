# yudao-server · 运营数据平台后端

本仓库 Spring Boot 可执行模块位于 `yudao-module-oa/`。前端见 [`ops-platform-ui-vue/README.md`](../ops-platform-ui-vue/README.md)。

## 前置

| 依赖 | 说明 |
|------|------|
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.x；dev 默认连远程库（见 `application-dev.yml`），也可改为本地实例 |

初始化库（若使用自建 MySQL）：

```sql
CREATE DATABASE IF NOT EXISTS wd DEFAULT CHARSET utf8mb4;
```

## 配置

| 文件 | 说明 |
|------|------|
| `yudao-module-oa/src/main/resources/application-dev.yml` | dev 数据源、Flyway、`oa.auth.mode=dev-fixed` |
| `yudao-module-oa/src/main/resources/application-dev-local.yml` | **可选**，本地钉钉密钥（已 gitignore，勿提交） |
| `application-dev-local.yml.example` | 复制为 `application-dev-local.yml` 后填入占位符对应的真实值 |

`application-dev.yml` 已通过 `spring.config.import: optional:classpath:application-dev-local.yml` 自动合并本地覆盖项。未配置钉钉时，组织/用户「同步钉钉」相关能力不可用，其余接口可正常开发。

## 启动（dev · 8080）

在仓库根目录执行：

**PowerShell（推荐，避免 `-D` 被拆参）：**

```powershell
cd yudao-server/yudao-module-oa
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

**Bash / Git Bash：**

```bash
cd yudao-server/yudao-module-oa
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- 监听：`http://localhost:8080`
- 健康检查：`GET http://localhost:8080/actuator/health` → `{"status":"UP"}`
- 鉴权：dev profile 下使用固定 Dev Token（见下），权限从数据库读取（ADR-003）

HelloWorld 冒烟：

```powershell
curl -H "Authorization: Bearer dev-token-oa-admin" -H "X-Tenant-Id: 1" http://localhost:8080/admin-api/oa/hello
```

### Flyway

启动时自动执行 `classpath:db/migration` 下脚本。日志中应出现迁移成功且无 `Validate failed` / checksum 错误。

**常见问题**

| 现象 | 处理 |
|------|------|
| Flyway checksum mismatch | 本地改过已执行过的 migration 文件 → 在测试库 `flyway repair` 或与团队对齐脚本版本，勿随意改历史 migration |
| 端口 8080 占用 | `netstat -ano \| findstr :8080` 查 PID，结束旧进程或改 `server.port` |
| PowerShell 报 `Unknown lifecycle phase ".run.profiles=dev"` | 给 `-D` 参数加引号，见上文启动命令 |
| 连不上 MySQL | 检查 `application-dev.yml` 中 url/账号，或 VPN/防火墙 |

## 测试

```powershell
cd yudao-server/yudao-module-oa
mvn test
```

## 打包与部署（简要）

```powershell
cd yudao-server/yudao-module-oa
mvn -DskipTests package
```

产物：`target/yudao-module-oa-*.jar`。生产环境使用 `prod`（或运维约定 profile），通过环境变量/外部配置注入数据源与密钥，**勿将钉钉、DB 密码写入仓库**。

## 本地全栈联调

1. 先启动本后端（8080）
2. 再启动 [`ops-platform-ui-vue`](../ops-platform-ui-vue/README.md)（3000，代理 `/admin-api` → 8080）
3. 浏览器打开 `http://localhost:3000`
4. 前端配置 `VITE_API_TOKEN=dev-token-oa-admin`、`VITE_TENANT_ID=1`（或 localStorage 写入 token/tenantId）

## 目录

| 路径 | 说明 |
|------|------|
| `yudao-module-oa/` | Spring Boot 可执行模块 |
| `db/init_dict.sql` | 字典基线（补充灌库） |
| `db/seed/` | Seed Data Program |
