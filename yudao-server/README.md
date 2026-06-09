# yudao-server · GATE-S0 后端

## 前置

- JDK 17+
- Maven 3.8+
- MySQL 8（本地 dev  profile）

## 初始化数据库

```sql
CREATE DATABASE IF NOT EXISTS wd DEFAULT CHARSET utf8mb4;
```

## 启动

```powershell
cd yudao-server/yudao-module-oa
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

健康检查：`GET http://localhost:8080/actuator/health`

HelloWorld（需 Dev Token）：

```powershell
curl -H "Authorization: Bearer dev-token-oa-admin" -H "X-Tenant-Id: 1" http://localhost:8080/admin-api/oa/hello
```

## 测试

```powershell
mvn test
```

## 目录

| 路径 | 说明 |
|------|------|
| `yudao-module-oa/` | Spring Boot 可执行模块 |
| `db/init_dict.sql` | 字典基线（补充灌库） |
| `db/seed/` | Seed Data Program |
