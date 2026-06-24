# CHECKLIST-M8-配置管理



> **版本**：v2.1 | 2026-06-11（企微/个微 Tab + 账号统一）



---



## 1. 范围



- [x] 5 Slice 完成（S01–S05 代码已落地）

- [x] 7 FR 实现

- [x] CFG-001~036 覆盖（订单连接测试为 stub）



## 2. 内部采集 FR-M8-001



- [x] 7 平台 Tab 切换（企微 / 个微奥创；平台类 Tab 已硬切至 M4 采集 Tab · ADR-047 V113）

- [x] ~~平台类 Tab：AccountSelect 关联 `oa_account` + CRUD~~ → **已退役**（凭证 SSOT：`oa_account` + M4 采集 Tab）

- [x] ~~快手特殊字段~~ → 迁至 `oa_account`（bind 携带）

- [x] 企业微信 Tab：`WeworkAppConfigPanel`（`/oa/internal/wework/*`）

- [x] 个人微信 Tab：仅奥创接口配置（`/aocreate`）

- [x] 敏感字段脱敏

- [ ] 奥创接口管理员权限校验（前端可见性已实现，后端权限待补）



## 3. 外部采集 FR-M8-002



- [x] 外部账号 Tab（`subType=account`，`dict_third_platform`）

- [x] 关键词 Tab + CRUD

- [x] CSV 批量导入

- [x] V50 种子数据（4 账号 + 5 关键词）



## 4. 外部数据 FR-M8-003



- [x] 数据源 CRUD



## 5. 订单采集 FR-M8-004



- [x] 数据库配置 CRUD

- [ ] 连接测试（stub，待真实 JDBC 探测）

- [x] 增量/全量模式（`dict_sync_mode`）



## 6. 阈值 FR-M8-005



- [x] 4 Tab（预警/粉丝/作品/覆盖）

- [x] 各类型独立表单 CRUD

- [x] AccountSelect 账号覆盖



## 7. AI FR-M8-006/007



- [x] 模型 CRUD + 统计卡片

- [x] 连接测试 + 设默认

- [x] 默认模型不可删

- [x] 提示词版本管理

- [x] 查看详情弹窗



## 8. 全局规范



- [x] tenant_id 隔离

- [x] @InDict 校验

- [x] 错误码 1500-1504

- [x] AES-256 凭证

- [x] dict_config_status 状态筛选



## 9. 文档



- [x] PRD/UX/API/STATE/SLICES 同步 v2.1

- [x] ADR-014 补充（账号统一 + 企微/个微 Tab）



## 10. Sign-off



| 角色 | 姓名 | 日期 |

|------|------|------|

| 开发 | | |

| 测试 | | |

| 产品 | | |

