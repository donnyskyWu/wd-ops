# P5-MIGRATE-8 Rollback（ADR-058 CLEANUP 后）

> **2026-07-31**：`ops-platform-server` 已删除。`-UseLegacyOa` **fail-fast**。  
> Flyway SSOT = `football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/`。  
> 未迁域源码归档：`football-module-ops-server/legacy-archive/`（不在编译路径）。

## 回滚到删除前的 legacy 模块（仅紧急）

```powershell
# 1) 从 CLEANUP 删除提交之前恢复目录（示例：用删除提交的父 commit）
git checkout <CLEANUP_PARENT_SHA> -- ops-platform-server

# 2) 停 monorepo ops-server :48094
Get-NetTCPConnection -LocalPort 48094 -State Listen -ErrorAction SilentlyContinue |
  Select-Object -ExpandProperty OwningProcess -Unique |
  ForEach-Object { Stop-Process -Id $_ -Force }

# 3) 在恢复出的树内手工启动（脚本不再支持 -UseLegacyOa）
cd ops-platform-server/ops-platform-module-oa
mvn spring-boot:run "-Dspring-boot.run.profiles=dev,dev-nacos,dev-nacos-local,dev-local-multidb"
```

## 正常路径：monorepo

```powershell
.\scripts\start-integration-oa.ps1            # 使用已有 JAR
.\scripts\start-integration-oa.ps1 -Rebuild   # 重新 package 后启
```

## 校验

```powershell
curl http://127.0.0.1:48094/actuator/health
curl "http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=ops-server&namespaceId=local"
python docs/delivery/e2e-artifacts/P5-MIGRATE-8-cutover/smoke.py
```
