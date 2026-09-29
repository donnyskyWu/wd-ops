# shenyu-system — 本批次无变更

V205 / V206 与 M4 Excel 导入均为 **shenyu-ops** 表结构变更：

- `holder_user_id` / `operator_user_id` 存 **Football `system_users.id`**，不在 shenyu-system 增列。
- 双状态 `short_video_status` / `live_status` 为 **VARCHAR 原文**（ADR-078），**无**新增字典 seed。

holder / operator 写入前请在业务上确认对应用户已在 shenyu-system 存在且 tenant 一致（ADR-056）。

**无需对 shenyu-system 执行 SQL。**
