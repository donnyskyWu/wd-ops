# oa-server 五库分库配置矩阵（GATE-MDB-S4）

> **用途**：localhost 已 cutover；远程 **101.37.161.136** 按本矩阵执行需 **另批审批**（禁止自动 touch 远程）。

## localhost（已生效）

| 组件 | Profile | 数据源 |
|------|---------|--------|
| **oa-server** | `dev,dev-nacos,dev-nacos-local,dev-local-multidb` | `master`→wd · `member`→shenyu-member · `mp`→shenyu-mp · `pay`→shenyu-pay · `system`→shenyu-system |
| **system-server** | `local,nacos-local` + overlay | **localhost:3306/wd**（2026-07-05 切换；远程见 `football-integration-overlay-remote.yml`） |
| **Gateway** | `local` | 路由不变 |

SSOT 文件：`ops-platform-module-oa/src/main/resources/application-dev-local-multidb.yml`

启动：

```powershell
.\scripts\start-integration-all.ps1 -SkipBuild
# oa-server 由 start-integration-oa.ps1 带 dev-local-multidb
```

## 远程 101.37.161.136 cutover 清单（用户执行）

> 用户已有四库 export（`docs/sql/shenyu-*.sql`）。**勿在本程序自动执行**。

**辅助脚本（post-S4 2026-07-05）**：

| 脚本 | 用途 |
|------|------|
| `scripts/test-remote-mysql-connection.ps1` | TCP + MySQL ping 五库（设 `OA_DB_PASSWORD`） |
| `scripts/integration-config/oa-server-remote-multidb.yaml` | Nacos 五库 DS overlay 模板 |
| `scripts/push-remote-multidb-config.ps1 -WhatIf` | 预览 Nacos push（需审批后执行） |
| `scripts/integration-config/mdb-remote-flyway-checklist.md` | V131/V132 逐步清单 |

### 前置

- [ ] 备份 `wd` + 四库 mysqldump
- [ ] 在远程创建库：`shenyu-member` / `shenyu-mp` / `shenyu-pay` / `shenyu-system`
- [ ] 导入用户 export（schema + seed）
- [ ] 确认 Flyway V131/V132 已在 **localhost 验证通过**

### oa-server Nacos（`oa-server-dev.yaml` 或等价）

```yaml
spring:
  datasource:
    dynamic:
      primary: master
      strict: false
      datasource:
        master:
          url: jdbc:mysql://101.37.161.136:3306/wd?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
          username: ${OA_DB_USER}
          password: ${OA_DB_PASSWORD}
        member:
          url: jdbc:mysql://101.37.161.136:3306/shenyu-member?...
        mp:
          url: jdbc:mysql://101.37.161.136:3306/shenyu-mp?...
        pay:
          url: jdbc:mysql://101.37.161.136:3306/shenyu-pay?...
        system:
          url: jdbc:mysql://101.37.161.136:3306/shenyu-system?...
```

### wd Flyway（远程）

1. 跑 V131（若未应用）
2. 跑 **V132** — DROP `oa_author` + wd 内 `author_user`/`pay_*` 副本
3. **保留** remote wd 内 `system_*` 直至 system-server 改连 `shenyu-system`

### system-server（第二阶段，可选）

- 将 `spring.datasource.dynamic.datasource.master.url` 从 `wd` 改为 `shenyu-system`
- 验证登录 / 菜单 / tenant API
- 确认后 DROP remote wd 内 `system_*` 副本表

### 验收（远程）

- [ ] `:5777` 登录 admin/admin123 tenant 1
- [ ] `#/ops/author` total ≥ Football member 作者数
- [ ] `#/ops/internal-account` 微信 ≥ 187
- [ ] `#/ops/system-log/login` ≥ 3000
- [ ] `run-uat-football-e2e.ps1` → **58/58**

### 回滚

- 从 cutover 前 mysqldump restore `wd`
- oa-server profile 回退单库 `application-dev.yml`
- revert V132 需 restore（Flyway 无 down）
