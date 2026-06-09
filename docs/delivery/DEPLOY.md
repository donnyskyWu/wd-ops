# 部署手册 (DEPLOY.md)

> 版本：S5 / v9.1
> 环境：开发 + 小规模生产
> 平台：Spring Boot 3.x + Vue 3 + MySQL 8

---

## 一、架构总览

```
┌─────────────────┐    ┌──────────────────┐
│  Vue 3 Frontend │───▶│ Spring Boot API  │
│  (port 80)      │    │ (port 8080)      │
└─────────────────┘    └────────┬─────────┘
                                │
                       ┌────────▼────────┐
                       │   MySQL 8       │
                       │   (port 3306)   │
                       └─────────────────┘
```

> 注：本项目未使用 Redis / RabbitMQ / MinIO / XXL-JOB（参见 ADR-001）。
> 单库 + 本地文件 + 内存任务调度即可支撑 MVP 与小规模生产。

---

## 二、依赖环境

| 组件 | 版本 | 备注 |
|------|------|------|
| JDK | 17+ | Spring Boot 3.x 要求 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| MySQL | 8.0+ | utf8mb4 字符集 |
| 内存 | ≥2GB | 单实例 |
| 磁盘 | ≥10GB | 含日志 |

---

## 三、后端

### 3.1 构建
```bash
cd yudao-server
mvn clean package -DskipTests
# 产物：yudao-module-oa/target/yudao-module-oa-1.0.0.jar
```

### 3.2 数据库初始化
```bash
# 1. 创建数据库
mysql -uroot -p -e "CREATE DATABASE cursor DEFAULT CHARSET utf8mb4;"

# 2. 执行 Flyway 迁移（启动应用时自动执行）
# 顺序：V1.0.0 → V1.0.8

# 3. 初始数据（S3 提供）
mysql -uroot -p cursor < db/V1.0.3__init_default_tenant_user.sql
```

### 3.3 启动
```bash
# 配置 application.yml 中 datasource / token-secret
java -jar yudao-module-oa-1.0.0.jar \
  --spring.datasource.url=jdbc:mysql://localhost:3306/cursor \
  --spring.datasource.username=root \
  --spring.datasource.password=root \
  --oa.auth.token-secret=YOUR_RANDOM_SECRET
```

### 3.4 健康检查
```bash
curl http://localhost:8080/actuator/health
# 期望：{"status":"UP"}
```

---

## 四、前端

### 4.1 构建
```bash
cd yudao-ui-admin-vue3
npm install
npm run build:prod
# 产物：dist/
```

### 4.2 部署（Nginx 示例）
```nginx
server {
  listen 80;
  server_name oa.example.com;
  root /var/www/oa/dist;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;
  }

  location /oa-api/ {
    proxy_pass http://127.0.0.1:8080/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  }
}
```

---

## 五、配置项

### 5.1 `application.yml` 关键配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cursor?useUnicode=true&characterEncoding=utf8
    username: root
    password: root
  flyway:
    enabled: true
    locations: classpath:db/migration

oa:
  auth:
    # 32 字节以上随机串
    token-secret: ${OA_TOKEN_SECRET:please-change-in-prod}
  crypto:
    # 16/24/32 字节 AES 密钥
    aes-key: ${OA_AES_KEY:please-change-in-prod-16b}
```

### 5.2 安全建议
- 生产环境必须修改 `token-secret` 与 `aes-key`
- 数据库密码不写入配置（使用环境变量 / Vault）
- 启用 HTTPS（前置 Nginx 终结）
- 防火墙：仅 80/443 对外，3306 仅本机

---

## 六、运维操作

### 6.1 数据备份
```bash
# 每日凌晨 3 点全量备份
mysqldump -uroot -p cursor | gzip > /backup/cursor_$(date +\%Y\%m\%d).sql.gz
```

### 6.2 日志
- 应用日志：`./logs/yudao-module-oa.log`（按日滚动）
- 审计日志：表 `oa_audit_log`（保留 ≥180 天）

### 6.3 监控（建议接入）
- Prometheus + Grafana 拉取 `/actuator/prometheus`
- 关键指标：JVM 内存 / SQL 慢查询 / 接口 P95

### 6.4 性能基线（S5 实测）
- 字典查询：avg 0.48ms / P95 0.73ms
- 采集任务分页：avg 0.74ms / P95 0.80ms
- 代理池查询：avg 0.45ms / P95 0.76ms
- 财务多维统计：avg 0.36ms / P95 0.50ms

---

## 七、回滚策略

```bash
# 1. 停服务
systemctl stop yudao-oa

# 2. 备份当前 jar
cp /opt/yudao/yudao-module-oa.jar /opt/yudao/backup/

# 3. 回退 jar
cp /opt/yudao/backup/yudao-module-oa-1.0.0-prev.jar /opt/yudao/

# 4. 回退 DB（按需）
mysql -uroot -p cursor < /backup/cursor_prev.sql

# 5. 启动
systemctl start yudao-oa
```

---

## 八、故障排查 Checklist

| 现象 | 排查方向 |
|------|----------|
| 401 登录失败 | token-secret 是否一致 / 密码格式（旧 SHA-256 vs 新 PBKDF2） |
| 接口限流 1511 | 检查 `@RateLimit` 配置 / IP 来源 |
| 采集任务不执行 | cron 表达式 / 状态是否 ENABLED |
| 绩效算分异常 | 模板 score_standard 是否连续无重叠 / 权重和=100 |
| 财务审核失败 | 状态机：必须 PENDING 才能 approve/reject |

---

## 九、ADR 摘要

- ADR-001 不引入 Redis / MQ / MinIO / XXL-JOB
- ADR-002 字典作为单一事实源
- ADR-003 多租户通过 TenantContext ThreadLocal
- ADR-004 密码 PBKDF2（S5 升级）

详细见 `docs/engineering/ADR-*.md`。
