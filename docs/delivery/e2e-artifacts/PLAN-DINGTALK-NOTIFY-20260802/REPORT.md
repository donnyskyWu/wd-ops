# 计划启动钉钉通知 — 排查报告 2026-08-02

## 结果：**PASS**（修复后）

| 检查项 | 结果 |
|--------|------|
| Beta DB：用户 `2077584621618393088` 有 `dingtalk_user_id` | ✅ `17657642164761319` |
| Beta DB：`dingtalk.enabled=true` + 凭证齐全 | ✅ |
| 计划「交付计划12」(id=23) 站内信已写入 | ✅ `sys_message` id 16/17 |
| 修复前 ops 日志 | ❌ `work notify skipped … no dingtalk_user_id` |
| `GET /ops/dev/dingtalk/status` | ✅ `primaryChannel=work_notify` |
| `POST /ops/dev/dingtalk/test-work-send` | ✅ `errcode=0`, task_id 返回 |
| 修复后新建计划启动 (plan id=24) | ✅ code=0；日志无 skip |

## 根因（代码 Bug，非配置）

**`NotificationServiceImpl.resolveDingtalkUserId` 使用了 `AdminUserApi.getUser()`。**

计划启动时 Feign 调用携带**启动人**的数据权限上下文。非 admin 启动人查询 assignee 时，`getUser` 无法返回对方 `dingtalk_user_id`（DB 有值但 RPC 解析为空），导致：

1. 工作通知跳过：`DingTalk work notify skipped for user 2077584621618393088: no dingtalk_user_id`
2. 机器人降级失败：`robot webhook = https://example.com/webhook`（E2E 占位 URL）→ HTML 解析错误

站内信仍正常写入（`channel=IN_APP,DINGTALK` 为意图标记，不代表钉钉已成功）。

**与 ADR-056 / `FootballSystemUserValidator` 一致：通知场景应使用 `getByIds`（忽略数据权限）。**

## 修复

- `NotificationServiceImpl.resolveDingtalkUserId` → `adminUserApi.getByIds`
- `DingTalkDevController.resolveTargetUser` → 同步改为 `getByIds`

## 用户需配置（M9 系统参数）

| 参数 | 必填 | 说明 |
|------|------|------|
| `dingtalk.enabled` | 是 | `true` 才发工作通知 |
| `dingtalk.client-id` | 是 | 钉钉 AppKey |
| `dingtalk.client-secret` | 是 | 钉钉 AppSecret |
| `dingtalk.agent-id` | 是 | 企业内部应用 AgentId |
| `dingtalk.corp-id` | 建议 | 企业 CorpId |
| `dingtalk.robot.enabled` | 否 | 工作通知失败时的降级 |
| `dingtalk.robot.webhook-url` | 降级时 | **须为真实钉钉机器人 URL**，勿留 `example.com` |
| `dingtalk.robot.secret` | 降级时 | 机器人 SEC 签名 |
| `notification.platform-base-url` | 建议 | 消息内「查看详情」链接基址 |

**用户侧**：Football 用户管理确保执行人有 `dingtalk_user_id`（与钉钉 userid 一致）。

## 代码路径（计划启动 → 钉钉）

```
ContentPlanServiceImpl.start()
  → NotificationServiceImpl.notifyPlanStarted()
    → send() → saveInAppMessage + pushDingTalk()
      → resolveDingtalkUserId() [getByIds]
      → DingTalkWorkNotifyClient.sendMarkdown()
      → (fallback) DingTalkRobotClient
```

## 复现 / 回归

```powershell
python docs/delivery/e2e-artifacts/PLAN-DINGTALK-NOTIFY-20260802/smoke_plan_dingtalk_notify.py
```

Artifacts：`docs/delivery/e2e-artifacts/PLAN-DINGTALK-NOTIFY-20260802/`
