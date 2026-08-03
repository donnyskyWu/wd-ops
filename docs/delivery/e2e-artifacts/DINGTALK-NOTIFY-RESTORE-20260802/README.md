# ADR-026 钉钉通知恢复 — E2E 冒烟

## 系统参数键（M9 系统参数 UI 填写）

| param_key | 类型 | 说明 | 示例 |
|-----------|------|------|------|
| `dingtalk.enabled` | BOOLEAN | 启用工作通知 | `true` |
| `dingtalk.client-id` | STRING | 钉钉 AppKey | （企业内部应用） |
| `dingtalk.client-secret` | STRING | 钉钉 AppSecret | **勿提交 git** |
| `dingtalk.corp-id` | STRING | 企业 CorpId | 可选归档 |
| `dingtalk.agent-id` | STRING | 工作通知 AgentId | `4335523092` |
| `dingtalk.robot.enabled` | BOOLEAN | 启用机器人降级 | `false` |
| `dingtalk.robot.webhook-url` | STRING | 机器人 Webhook | 含 access_token |
| `dingtalk.robot.secret` | STRING | Webhook 加签 SEC | 可选 |
| `notification.platform-base-url` | STRING | 消息跳转前缀 | `https://ops.example.com/#/ops` |

分类：`DINGTALK` / `NOTIFICATION`。Flyway **V170** 为 tenant=1 seed 空占位。

## 如何在系统参数 UI 配置

1. 登录 Gate UI → **系统管理(OA)** → **系统参数**
2. 切换到 **钉钉配置** Tab（category `DINGTALK`）；`notification.platform-base-url` 在 **通知配置** Tab
3. 也可在任意 Tab 用参数键搜索 `dingtalk.`
4. 编辑各参数值并保存（client-secret 等敏感项仅存 DB）

## dev 诊断端点（profile `dev`）

- `GET /admin-api/ops/dev/dingtalk/status` — 工作通知/机器人是否可用及 skip 原因
- `POST /admin-api/ops/dev/dingtalk/test-send` — 测试推送（需凭证 + userId）

## 运行冒烟

```powershell
python docs/delivery/e2e-artifacts/DINGTALK-NOTIFY-RESTORE-20260802/smoke_dingtalk_notify.py
```

## 期望

- 凭证为空：`primaryChannel=none`，`sendEnabled=false`，计划启动不报错
- 凭证已填 + 用户有 dingtalk_user_id：工作通知发送；失败降级机器人
