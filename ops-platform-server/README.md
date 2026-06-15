# ops-platform-server · 运营数据平台后端

本仓库 Spring Boot 可执行模块位于 `ops-platform-module-oa/`。前端见 [`ops-platform-ui-vue/README.md`](../ops-platform-ui-vue/README.md)。

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
| `ops-platform-module-oa/src/main/resources/application-dev.yml` | dev 数据源、Flyway、`oa.auth.mode=dev-fixed` |
| `ops-platform-module-oa/src/main/resources/application-dev-local.yml` | **可选**，本地钉钉密钥（已 gitignore，勿提交） |
| `application-dev-local.yml.example` | 复制为 `application-dev-local.yml` 后填入占位符对应的真实值 |

`application-dev.yml` 已通过 `spring.config.import: optional:classpath:application-dev-local.yml` 自动合并本地覆盖项。未配置钉钉时，组织/用户「同步钉钉」相关能力不可用，其余接口可正常开发。

### 钉钉工作通知（业务推送主通道）

业务通知会写站内信，并优先通过钉钉**工作通知**（`asyncsend_v2`）点对点推送给对应用户。群机器人 Webhook 仅在工作通知不可用或未绑定 `ding_user_id` 时作为**可选降级**，生产环境**无需配置 Webhook**。

已接入的 5 类触发事件：

| 事件 | 接收人 | 触发点 |
|------|--------|--------|
| 计划启动 · 任务待执行 | 任务执行人 | `ContentPlanServiceImpl.start()` |
| 内容提交审核 | 审核人 | 内容提审 |
| 内容审核通过 | 创建人 | 终审通过 |
| 爆款 / 低分作品预警 | IP 组负责人 | 监控扫描 |
| 高粉 / 低粉账号预警 | IP 组负责人 | 监控扫描 |

**必需配置**（`application-dev-local.yml` 或环境变量）：

| 配置项 | 说明 |
|--------|------|
| `oa.dingtalk.enabled` | `true` 启用钉钉集成 |
| `oa.dingtalk.client-id` | 企业内部应用 AppKey |
| `oa.dingtalk.client-secret` | 企业内部应用 AppSecret |
| `oa.dingtalk.agent-id` | 微应用 AgentId（工作通知必需） |
| `oa.notification.platform-base-url` | 前端根 URL，消息内「查看详情」跳转链接 |

**可选降级**（一般不必配置）：

| 配置项 | 说明 |
|--------|------|
| `oa.dingtalk.robot.enabled` | 启用群机器人降级 |
| `oa.dingtalk.robot.webhook-url` | 自定义机器人 Webhook（含 access_token） |
| `oa.dingtalk.robot.secret` | 群机器人加签 SEC 密钥 |

示例 `application-dev-local.yml`：

```yaml
oa:
  dingtalk:
    enabled: true
    client-id: your-app-key
    client-secret: your-app-secret
    corp-id: your-corp-id
    agent-id: 4335523092
  notification:
    platform-base-url: http://localhost:5173
```

环境变量等价：`DINGTALK_ENABLED`、`DINGTALK_CLIENT_ID`、`DINGTALK_CLIENT_SECRET`、`DINGTALK_AGENT_ID`、`OA_PLATFORM_BASE_URL`。

用户须先通过「同步钉钉」写入 `ding_user_id`，否则工作通知会跳过该用户（站内信仍正常）。

**dev 诊断接口**（仅 `dev` profile）：

```powershell
# 查看工作通知是否就绪（primaryChannel=work_notify 表示生产可用）
curl -H "Authorization: Bearer dev-token-oa-admin" -H "X-Tenant-Id: 1" http://localhost:8080/admin-api/oa/dev/dingtalk/status

# 向指定用户发工作通知测试（如张武 userId=2036）
curl -X POST -H "Authorization: Bearer dev-token-oa-admin" -H "X-Tenant-Id: 1" -H "Content-Type: application/json" -d "{\"userId\":2036}" http://localhost:8080/admin-api/oa/dev/dingtalk/test-work-send
```

未配 `client-id` / `agent-id` 时 `status.skipReason` 会说明原因；站内信仍正常，仅钉钉跳过。

## 启动（dev · 8080）

在仓库根目录执行：

**PowerShell（推荐，避免 `-D` 被拆参）：**

```powershell
cd ops-platform-server/ops-platform-module-oa
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

**Bash / Git Bash：**

```bash
cd ops-platform-server/ops-platform-module-oa
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- 监听：`http://localhost:8080`
- 健康检查：`GET http://localhost:8080/actuator/health` → `{"status":"UP"}`
- 鉴权：dev profile 下使用固定 Dev Token（见下），权限从数据库读取（ADR-003）

## 重启服务（dev · 完整命令）

代码或 Flyway 脚本变更后，需**先停旧进程再启动**，否则 8080 仍跑旧字节码。

### 1. 停止占用 8080 的后端（PowerShell）

在任意目录执行：

```powershell
$conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($conn) {
  $procId = $conn.OwningProcess
  Write-Host "Stopping PID $procId on port 8080"
  Stop-Process -Id $procId -Force
  Start-Sleep -Seconds 2
} else {
  Write-Host "Port 8080 is free"
}
```

备用查 PID（CMD / PowerShell 均可）：

```powershell
netstat -ano | findstr :8080
# 记下 LISTENING 行最后一列 PID，再执行：
Stop-Process -Id <PID> -Force
```

### 2. （可选）编译后再启动

```powershell
cd ops-platform-server/ops-platform-module-oa
mvn compile -DskipTests
```

### 3. 启动后端

```powershell
cd ops-platform-server/ops-platform-module-oa
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

日志出现 `Tomcat started on port 8080` 即启动成功。进程在前台占用终端；要后台跑可用 IDE Run 或另开终端窗口。

### 4. 启动后冒烟验证

```powershell
curl -H "Authorization: Bearer dev-token-oa-admin" -H "X-Tenant-Id: 1" http://localhost:8080/admin-api/oa/hello
```

大屏数据接口示例：

```powershell
curl -H "Authorization: Bearer dev-token-oa-admin" -H "X-Tenant-Id: 1" "http://localhost:8080/admin-api/oa/dashboard/98601/data?startDate=2026-06-05&endDate=2026-06-11&ipGroupId=1&platformType=DOUYIN"
```

可选查询参数：`startDate`、`endDate`（默认近 7 天）、`ipGroupId`、`platformType`。STAT 组件不受日期参数影响。详见 `docs/engineering/API-M6-数据分析.md` §5.2。

### 5. 重启前端（联调时）

后端重启后，前端 Vite 一般**无需**重启；若代理异常，在 `ops-platform-ui-vue` 目录：

```powershell
cd ops-platform-ui-vue
npm run dev
```

浏览器访问 `http://localhost:3000`。

### 6. 重启失败常见原因

| 现象 | 处理 |
|------|------|
| `Validate failed` / `Migration checksum mismatch` | 勿修改**已执行过**的 `V*.sql`；新变更用更高版本号（如 V60）；或测试库执行 Flyway repair |
| `Port 8080 was already in use` | 重复执行上文「停止占用 8080」步骤 |
| `Unknown lifecycle phase ".run.profiles=dev"` | `-Dspring-boot.run.profiles=dev` 必须加引号（见启动命令） |

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
cd ops-platform-server/ops-platform-module-oa
mvn test
```

## 打包与部署（简要）

```powershell
cd ops-platform-server/ops-platform-module-oa
mvn -DskipTests package
```

产物：`target/ops-platform-module-oa-*.jar`。生产环境使用 `prod`（或运维约定 profile），通过环境变量/外部配置注入数据源与密钥，**勿将钉钉、DB 密码写入仓库**。

## 本地全栈联调

1. 先启动本后端（8080）
2. 再启动 [`ops-platform-ui-vue`](../ops-platform-ui-vue/README.md)（3000，代理 `/admin-api` → 8080）
3. 浏览器打开 `http://localhost:3000`
4. 前端配置 `VITE_API_TOKEN=dev-token-oa-admin`、`VITE_TENANT_ID=1`（或 localStorage 写入 token/tenantId）

### M2 需求 2–6 冒烟（S-10~S-13）

后端已启动且 Flyway ≥ V65 后，可用 Dev Token 快速验证主链路：

```powershell
$auth = "Bearer dev-token-oa-admin"
$tenant = "1"

# SOP 节点（S-10 nodeType）
curl.exe -H "Authorization: $auth" -H "X-Tenant-Id: $tenant" `
  "http://localhost:8080/admin-api/oa/sop/node/list?templateId=9401"

# 计划 + 赛事代理（S-09/S-11）
curl.exe -H "Authorization: $auth" -H "X-Tenant-Id: $tenant" `
  "http://localhost:8080/admin-api/oa/plan/list?pageNo=1&pageSize=5"
curl.exe -H "Authorization: $auth" -H "X-Tenant-Id: $tenant" `
  "http://localhost:8080/admin-api/oa/match/list?date=2026-06-12&pageNo=1&pageSize=5"

# 我的任务 + 执行页（S-12；将 TASK_ID 换为真实值）
curl.exe -H "Authorization: $auth" -H "X-Tenant-Id: $tenant" `
  "http://localhost:8080/admin-api/oa/task/my-tasks?pageNum=1&pageSize=5"
curl.exe -H "Authorization: $auth" -H "X-Tenant-Id: $tenant" `
  "http://localhost:8080/admin-api/oa/task/TASK_ID/execute"

# 任务内容 + 用户 IP 组（S-13）
curl.exe -H "Authorization: $auth" -H "X-Tenant-Id: $tenant" `
  "http://localhost:8080/admin-api/oa/content/by-task?taskId=TASK_ID"
curl.exe -H "Authorization: $auth" -H "X-Tenant-Id: $tenant" `
  "http://localhost:8080/admin-api/oa/user/ip-groups"
```

集成测试（H2，含 V62–V65）：

```powershell
cd ops-platform-server/ops-platform-module-oa
mvn test "-Dtest=M2SopS10IT,M2PlanS11IT,M2TaskS12IT,M2ContentS13IT"
```

**浏览器走查（需求 2–6）**：SOP 编辑页设置节点类型 → 计划管理新建草稿（步骤绑赛事）→ 启动计划 → 任务管理「我的任务」点「执行」→ 内容生成节点进入内容创作 → 保存/确认 COMPLETED → 返回执行页点「完成」。

## 目录

| 路径 | 说明 |
|------|------|
| `ops-platform-module-oa/` | Spring Boot 可执行模块 |
| `db/init_dict.sql` | 字典基线（补充灌库） |
| `db/seed/` | Seed Data Program |
