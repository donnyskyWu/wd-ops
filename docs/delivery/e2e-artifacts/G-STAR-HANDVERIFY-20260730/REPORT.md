# G-* 业务手验批量签字报告

| 字段 | 值 |
|------|---|
| 日期 | 2026-07-30 |
| 环境 | localhost Integration（无 -Beta）· Front :5777 · Gateway :48080 · oa :48094 |
| 登录 | admin / admin123 · tenant 1 |
| SSOT | [OPS-FOOTBALL-INTEGRATION-FEIGN-CHECKLIST](../../OPS-FOOTBALL-INTEGRATION-FEIGN-CHECKLIST.md) §2–3 |
| 执行人 | AI hand-verify |
| 跳过 | G-DING（延期） |

---

## 一、结论矩阵

| G-* | 结果 | 证据摘要 |
|-----|------|----------|
| G-SYS-01 | **Pass** | RPC simple-list n=65；OPS member-candidates n=62 |
| G-SYS-02 | **Pass** | IP 组 update code=0；非法 leader→1004；无角色→1500 |
| G-DICT-01 | **Pass** | 非法 `level` / `contentType` → **1503** |
| G-INF-01 | **Pass** | 上传 URL `https://upload.shenyu.com/...` |
| G-MEM-03 | **Pass** | content **36** → article **1000319**，`footballSyncError=null` |
| G-MP-01 | **Pass** | account/list total=182；followers 1000002 total=**13** |
| G-PAY-01 | **Pass** | ADR-057 假设 B：`page-for-ops` RPC code=0 total≈183485；Gateway list code=0 · 见 [G-PAY-01-FIX.md](./G-PAY-01-FIX.md) |
| G-DING | 跳过 | 延期 |

**Pass 7 / Fail 0 / Skip 1**

---

## 二、端口 / 登录

| 端口 | 服务 | 状态 |
|------|------|------|
| 5777 | Front | UP |
| 48080 | Gateway | UP |
| 48081 | system | UP |
| 48082 | infra | UP |
| 48085 | pay | UP |
| 48086 | mp | UP |
| 48087 | member | UP |
| 48094 | oa | UP |

登录：`POST /admin-api/system/auth/login` → code=0，userId=`1749825673829120001`（见 `00-login.json`）。

---

## 三、分项手验

### G-SYS-01 用户 simple-list / UserSelect

| 步骤 | 结果 | 证据 |
|------|------|------|
| RPC `GET :48081/.../user/simple-list` | code=0，n=65 | `G-SYS-01-rpc-simple-list.json` |
| OPS `GET .../oa/ip-group/member-candidates` | code=0，n=62 | `G-SYS-01-ops-member-candidates.json` |
| OPS leader-candidates | code=0，n=8 | `G-SYS-01-ops-leader-candidates.json` |

### G-SYS-02 用户校验 / 角色

| 步骤 | 结果 | 证据 |
|------|------|------|
| RPC `has-any-roles` | code=0 | `G-SYS-02-rpc-has-any-roles.json` |
| OPS IP 组保存（remark，不改 leader） | code=0 | `G-SYS-02-ops-ip-group-update-retry.json` |
| 非法 leaderId | code=**1004** 组长用户不存在 | `G-SYS-02-ops-invalid-leader-retry.json` |
| leader 无 IP组长 角色 | code=**1500** | `G-SYS-02-ops-leader-no-role.json` |

> 注：带 leader 保存时 Feign 角色校验生效；当前 leader-candidates 列表中用户在 system RPC `has-any-roles?roles=ip_group_leader` 多为 false（数据/角色桥接另议题），不影响「校验路径」签字。

### G-DICT-01 字典 / @InDict

| 步骤 | 结果 | 证据 |
|------|------|------|
| RPC dict-data/list `dict_ip_group_level` | code=0，n=4 | `G-DICT-01-rpc-dict-list.json` |
| OPS update `level=NOT_A_VALID_LEVEL_XYZ` | code=**1503** | `G-DICT-01-ops-illegal-level.json` |
| OPS content create 非法 contentType | code=**1503** | `G-DICT-01-ops-illegal-contentType.json` |

### G-INF-01 文件上传

| 步骤 | 结果 | 证据 |
|------|------|------|
| OPS `POST .../oa/file/upload` | code=0，infra 域 URL | `G-INF-01-ops-upload.json` |

### G-MEM-03 文章写 / 方案同步

| 步骤 | 结果 | 证据 |
|------|------|------|
| OPS content create（SHORT_VIDEO + creatorUserId） | id=**36** | create 响应（会话日志） |
| `GET .../content/36/football-scheme` | authorArticleId=**1000319**，syncError=null | `G-MEM-03-ops-football-scheme-retry.json` |
| content list 按标题可见 | code=0 | 会话 LIST 抽检 |

### G-MP-01 公众号 / 粉丝

| 步骤 | 结果 | 证据 |
|------|------|------|
| RPC accountInfo/page | code=0，total=166 | `G-MP-01-rpc-accountInfo-page.json` |
| OPS account/list | code=0，total=182 | `G-MP-01-ops-account-list-retry.json` |
| RPC followers 1000002 | total=13 | `G-MP-01-rpc-followers.json` |
| OPS followers 1000002 | total=**13**（修复后） | `G-MP-01-ops-followers-1000002-after-fix.json` |

**会话内修复**：mp-server 返回时间戳为 epoch millis，OPS `MpUserDTO` 原 `LocalDateTime` 导致 Feign 解码失败→1400。改为 `Long` + `MpUserDataService.toLocalDateTime`。并补 `mpUserApi` / `payOrderApi` Feign URL。

### G-PAY-01 订单列表 — **Pass**（假设 B · [G-PAY-01-FIX.md](./G-PAY-01-FIX.md) · ADR-057）

| 步骤 | 结果 | 证据 |
|------|------|------|
| RPC `page-for-ops` + tenant-id（无 Authorization） | code=**0** total≈183485 | `G-PAY-01-rpc-page-for-ops-tenant-only.json` |
| RPC `page-for-ops` + Bearer | code=**0** | `G-PAY-01-rpc-page-for-ops-with-auth.json` |
| Gateway `football-order/list`（宽日期） | code=**0** total≈183485 | `G-PAY-01-ops-football-order-list-page-for-ops.json` |

**决策**：废止复用 Admin `getOrderPage`；MUST-HAVE §7.8 `page-for-ops`（无 permitted-ids / 渠道富化）。

---

## 四、会话内代码改动（最小）

| 文件 | 改动 |
|------|------|
| `application-dev-nacos-local.yml` | 增加 `mpUserApi` / `payOrderApi` 直连 URL |
| Football `PayOrderApi.pageForOps` + `getOrderPageForOps` | ADR-057 轻量分页 |
| OPS `PayOrderApi` / `FootballOrderReadServiceImpl` | 切 `pageForOps`；响应时间 Long epoch |
| `MpUserDTO.java` | 时间字段改为 `Long`（epoch millis） |
| `MpUserDataService.java` | millis → LocalDateTime |

---

## 五、复现命令（抽样）

```powershell
# G-PAY-01 Pass
# POST http://127.0.0.1:48085/rpc-api/pay/order/page-for-ops
#   Header tenant-id:1
#   Body {"pageNo":1,"pageSize":10,"startTime":"2020-01-01T00:00:00","endTime":"2030-01-01T00:00:00"}
# GET /admin-api/oa/football-order/list?startDate=2020-01-01&endDate=2026-07-30  + Bearer
```

脚本：`run_handverify.py`（初跑）+ 本目录 JSON 证据。
