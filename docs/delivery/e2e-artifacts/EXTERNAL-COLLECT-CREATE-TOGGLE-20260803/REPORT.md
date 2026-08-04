# 外部账号新增「是否采集」开关

**日期**: 2026-08-03  
**关联**: ADR-068 · ExternalCollectConfig.vue

## 问题

列表页已有「是否采集」列与行内开关，但「新增外部账号 / 编辑外部账号」弹窗未暴露 `collectEnabled`，创建时无法配置采集开关。

## 修复

- `ExternalCollectConfig.vue`：弹窗增加 `el-switch`（是否采集）；表单 state / create / edit / submit 传递 `collectEnabled`。
- 后端 `CollectConfigCreateReq.collectEnabled` 与 `CollectConfigServiceImpl.applyCreate` 已存在，无需改动。

## 默认值（ADR-068 Q4）

新建账号 UI 默认 **关闭**（`collectEnabled: false`），与 DB `DEFAULT 0` 一致。

## 手工验证

1. 打开 **外部采集配置 → 外部账号**。
2. 点击 **新增账号**，确认弹窗有「是否采集」开关，默认关闭。
3. 填写必填项，开启「是否采集」，保存；列表该行开关应为开。
4. 再新增一条，保持「是否采集」关闭，保存；列表该行开关应为关。
5. 编辑已有账号，切换「是否采集」并保存；列表与行内开关一致。
6. 行内开关仍可独立切换（update API `collectEnabled`）。
