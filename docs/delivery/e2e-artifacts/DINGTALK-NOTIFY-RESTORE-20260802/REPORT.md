# ADR-026 钉钉通知恢复 — 冒烟报告 2026-08-02

## 结果：**PASS**

| 检查项 | 结果 |
|--------|------|
| V170 sys_param seed（9 键） | ✅ category DINGTALK/NOTIFICATION 已入库 |
| `NoopNotificationService` | ✅ 已删除，由 `NotificationServiceImpl` 替代 |
| `GET /admin-api/ops/dev/dingtalk/status` | ✅ code=0；凭证空时 `primaryChannel=none`，graceful skip |
| 计划启动 | ⏭ 无 DRAFT 计划可测；`ContentPlanServiceImpl.start` 已有 try/catch 包裹 notify |

## 凭证为空时的 dev status

```json
{
  "primaryChannel": "none",
  "sendEnabled": false,
  "workNotifySkipReason": "dingtalk.enabled=false（请在系统参数中启用）"
}
```

## 变更文件摘要

见父 agent 返回的 files changed 列表。

## 下一步（用户）

1. M9 **系统参数** 填写 `dingtalk.*` 凭证
2. Football 用户管理确保执行人有 `dingtalk_user_id`
3. 启动草稿计划验证 TASK_PENDING 站内信 + 钉钉
4. 可选：`POST /admin-api/ops/dev/dingtalk/test-send` 单用户测试
