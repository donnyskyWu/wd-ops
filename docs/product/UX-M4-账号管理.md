# UX-M4-账号管理

> **版本**：v1.0 | 2026-06-07
> **关联 PRD**：[`PRD-M4-账号管理.md`](./PRD-M4-账号管理.md)
> **全局规范**：[`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md)

---


> **视觉规范参考**：[`开发规范/UI设计与开发规范.md`](../../开发规范/UI设计与开发规范.md)（仅原型/设计阶段）
> **实现技术栈**：[`TECH-CONSTRAINTS.md § 1.2`](../engineering/TECH-CONSTRAINTS.md)（Vue 3 + Element Plus）
> **决策记录**：[`ADR-002`](../../adr/ADR-002-前端规范源选择.md)

## 1. 页面清单

| 页面 ID | 名称 | 路由 | 关联 FR |
|---------|------|------|---------|
| P-M4-001 | 公司管理列表 | `/account/company` | FR-M4-001 |
| P-M4-002 | 公司详情（含公众号容量） | `/account/company/:id` | FR-M4-001 |
| P-M4-003 | 实名人管理列表 | `/account/realname` | FR-M4-002 |
| P-M4-004 | 实名人详情（含中介人） | `/account/realname/:id` | FR-M4-002 |
| P-M4-005 | 手机管理 | `/account/phone` | FR-M4-003 |
| P-M4-006 | 手机卡管理 | `/account/sim-card` | FR-M4-004 |
| P-M4-007 | 跨平台账号查询（手机卡详情侧滑） | `/account/sim-card/:id/linked` | FR-M4-004 |
| P-M4-008 | 平台账号列表 | `/account/platform` | FR-M4-005 |
| P-M4-009 | 平台账号详情/编辑 | `/account/platform/:id` | FR-M4-005 |
| P-M4-010 | 个人账号管理 | `/account/personal` | FR-M4-006 |
| P-M4-011 | 三方关联图谱 | `/account/triple-rel` | FR-M4-007 |

---

## 2. 通用约定

🔴 **所有关联属性必须用选择器**，禁用手动输入：

**导出（ADR-018，2026-06-13）**：内部管理 6 页（公司、实名人、手机、手机卡、平台账号、个人账号）均已接通 `exportToExcel` CSV 导出。

| 字段 | 控件 | 数据源 |
|------|------|--------|
| `realnameId` | `<RealNameSelect />` | 实名人表 |
| `phoneId` | `<PhoneSelect />` | 手机表 |
| `simCardId` | `<SimCardSelect />` | 手机卡表 |
| `companyId` | `<CompanySelect />` | 公司表 |
| `ipGroupId` | `<IpGroupTreeSelect />` | IP 组 |
| `intermediaryId` | `<RealNameSelect />` | 实名人表（复用） |

> ⚠️ 详情见 GLOBAL-CONVENTIONS § 3.2

---

## 3. P-M4-001 公司管理列表

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| F-NAME | `<Input />` | - |
| F-CREDIT-CODE | `<Input />` | - |
| TBL-COMPANY | 表格 | `oa_company` |
| COL-CAPACITY | 表格列 | 容量 / 已注册 / 剩余 |
| BTN-EXPAND | 链接 | "扩容" |
| BTN-MP-STATS | 链接 | "公众号统计" |

### 3.1 公司详情页

| 区域 | 内容 |
|------|------|
| 基本信息 | 公司名、信用代码、行业、地址、法人 |
| 公众号容量 | 容量/已注册/剩余/阈值预警 |
| 扩容记录 | 时间线（`expansion_history`） |
| 关联账号 | 表格（通过 `company_id` 关联的所有平台账号） |

---

## 4. P-M4-003 实名人管理列表

| 控件 | 类型 | 字典/实体 | 脱敏 |
|------|------|----------|------|
| F-NAME | `<Input />` | - | - |
| F-ID-TYPE | `<DictSelect dict-type="dict_id_type" />` | 字典 | - |
| F-STATUS | `<DictSelect dict-type="dict_realname_status" />` | 字典 | - |
| COL-REAL-NAME | 表格列 | - | ✅ |
| COL-ID-CARD | 表格列 | - | ✅ 中间 4 位 `****` |
| COL-PHONE | 表格列 | - | ✅ 中间 4 位 `****` |
| TBL-REALNAME | 表格 | `oa_account_realname` | - |
| BTN-ADD | 按钮 | "新增实名人" | - |

### 4.1 实名人详情（弹窗/抽屉）

| 区域 | 内容 |
|------|------|
| 基本信息 | 姓名、证件类型、证件号（脱敏）、手机（脱敏）、微信 |
| 关联账号 | 表格（通过 `realname_id` 关联的所有平台账号） |
| 中介人列表 | 表格（姓名、电话、关系类型、佣金比例） |
| 操作 | 按钮"新增中介人"、"编辑"、"删除" |

### 4.2 中介人弹窗

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| F-INTERMEDIARY-NAME | `<Input />` | - |
| F-INTERMEDIARY-PHONE | `<Input />` | - |
| F-INTERMEDIARY-WECHAT | `<Input />` | - |
| F-RELATION-TYPE | `<Select />` | 固定值（DIRECT/INTERMEDIARY/AGENCY） |
| F-COMMISSION-RATE | `<InputNumber :precision="2" />` | -（数据分析师/财务脱敏） |
| F-REMARK | `<TextArea />` | - |

---

## 5. P-M4-005 手机管理

> **ADR-011**：无实名人字段/列；保管人 `<UserSelect />`。

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| F-PHONE-NUMBER | `<Input />`（新增）；编辑只读脱敏 | - |
| F-CODE | `<Input />` | 手机编码 |
| F-MODEL | `<Input />` | - |
| F-KEEPER | `<UserSelect />` | `sys_user` |
| F-WECHAT | `<Input />` | 绑定微信 |
| F-STATUS | `<DictSelect dict-type="dict_phone_status" />` | 字典 |
| F-DEVICE-NO | `<Input />` | 设备编号（V85） |
| F-PHONE-TYPE | `<DictSelect dict-type="dict_phone_type" />` | Android / iPhone |
| F-AOCHUANG | `<DictSelect dict-type="dict_yes_no" />` | 奥创手机 |
| F-HANDLER | `<Input />` | 经手人 |
| F-PURCHASE | 批次/日期/时间 | 采购信息区 |
| F-IMAGES | `<ImageUpload />` ×3 | 设置截图、正/背面照 |
| SRCH-PHONE-TYPE | 筛选 | 列表按手机类型 |
| TBL-PHONE | 表格 | `oa_phone` |
| COL-PHONE-NUMBER | 表格列 | 脱敏显示 |
| COL-KEEPER | 表格列 | 保管人姓名 |
| COL-IMAGES | 表格列 | 缩略图预览 |

> **变更 2026-06-15**（ADR-024）：表单分「基础 / 采购 / 影像」；保管人仍为 `<UserSelect />`。

---

## 6. P-M4-006 手机卡管理（⭐ 跨平台聚合）

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| F-REALNAME-FILTER | `<RealNameSelect />` | 仅筛选 `PhoneSelect` 范围（不入库） |
| F-PHONE | `<PhoneSelect />` | `oa_phone`（**强关联**） |
| F-ICCID | `<Input />` | ICCID |
| F-OPERATOR | `<DictSelect dict-type="dict_sim_operator" />` | 字典 |
| F-IS-PRIMARY | `<DictSelect dict-type="dict_yes_no" />` | 字典 |
| F-PACKAGE | `<Input />` | 套餐名称 |
| F-ASSIGNED | `<UserSelect />` | 归属人 `sys_user` |
| F-STATUS | `<DictSelect dict-type="dict_sim_status" />` | 含 DAMAGED/LOST（V85） |
| TBL-SIM | 表格 | `oa_sim_card` |
| COL-LINKED-COUNT | 表格列 | 关联账号数（可点击） |
| BTN-VIEW-LINKED | 链接 | "跨平台查询" |

### 6.1 跨平台账号详情（侧滑）

点击"关联账号数" → 侧滑面板展示该手机号关联的所有平台账号：

```
+---------------------------------------------+
| 13800001111 关联账号（8 个）                |
+---------------------------------------------+
| 平台筛选：[全部▾]  运营商：[全部▾]         |
+---------------------------------------------+
| 平台        | 账号名   | 状态   | 关联时间 |
| 公众号      | （具体值详见相应章节）
| 视频号      | （具体值详见相应章节）
| 抖音        | （具体值详见相应章节）
| ...                                         |
+---------------------------------------------+
```

---

## 7. P-M4-008/009 平台账号列表/详情（⭐⭐ 核心）

### 7.1 列表

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| F-PLATFORM | `<DictSelect dict-type="dict_platform_type" />` | 字典 |
| F-ACCOUNT-TYPE | `<DictSelect dict-type="dict_account_type" />` | 字典 |
| F-IP | `<IpGroupTreeSelect />` | `oa_ip_group` |
| F-REALNAME | `<RealNameSelect />` | `oa_account_realname` |
| F-COMPANY | `<CompanySelect />` | `oa_company` |
| F-STATUS | `<DictSelect dict-type="dict_account_status" />` | 字典 |
| TBL-ACCOUNT | 表格 | `oa_internal_account` |

### 7.2 详情（弹窗/抽屉）

| 区域 | 内容 |
|------|------|
| 基本信息 | 账号名、平台、IP 组、状态 |
| 🔴 强关联 | 实名人、手机、手机卡、公司、中介人（**全部选择器**） |
| 公众号扩展（WECHAT_OFFICIAL） | 商标、邮箱、密码、资质类型、使用状态、认证到期、视频号关联、管理员（UserSelect）、身份证（脱敏） |
| 条件表单 | 企业 → 公司必选；个人 → 隐藏企业、展示实名人区 |
| 续费认证 | 表格：续费时间/续费人/金额；「新增续费」弹窗（ADR-025） |
| 凭证 | Cookie / authorization_token（脱敏） |
| 关联 | 关联的内容/粉丝/作品数据 |

### 7.3 编辑弹窗

🔴 **关键交互**：

| 控件 | 类型 | 强校验 |
|------|------|--------|
| F-PLATFORM | `<DictSelect />` | ✅ 联动账号类型 |
| F-ACCOUNT-TYPE | `<DictSelect />` | ✅ 联动实名人/手机选择器 |
| F-ACCOUNT-NAME | `<Input />` | - |
| F-ACCOUNT-ID | `<Input />` | ✅ 同平台唯一 |
| F-IP | `<IpGroupTreeSelect />` | ✅ |
| **F-REALNAME** | **`<RealNameSelect />`** | 🔴 禁用手动输入 |
| **F-PHONE** | **`<PhoneSelect />`** | 🔴 禁用手动输入 |
| **F-SIM-CARD** | **`<SimCardSelect />`** | 🔴 禁用手动输入 |
| **F-COMPANY** | **`<CompanySelect />`** | 🔴 禁用手动输入 |
| **F-INTERMEDIARY** | **`<RealNameSelect />`** | 🔴 禁用手动输入 |
| F-COOKIE | `<Input />`（密码框） | -（加密） |
| F-STATUS | `<DictSelect dict-type="dict_account_status" />` | - |

### 7.4 强制替换弹窗

当选择的实名人已被其他账号绑定时弹出：

```
+---------------------------------------------+
| ⚠️ 该实名人已被以下账号绑定：              |
| - 账号 A（2025-01-15 创建）                |
| 强制替换后：                                |
| - 账号 A 将自动解除绑定                    |
| - 替换操作将记录到审计日志                  |
+---------------------------------------------+
| [取消]    [确认替换]                        |
+---------------------------------------------+
```

---

## 8. P-M4-010 个人账号管理

Tab：**个微** | **企微**

### 8.1 个微列表 + 弹窗

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| F-NAME | `<Input />` | 账号名称 |
| F-WECHAT-ID | `<Input />` | 微信号 |
| F-CONTACT-PHONE | `<Input />` | 联系电话（明文，ADR-010） |
| F-STATUS | `<DictSelect />` | 启用/停用 |
| TBL-WECHAT | 表格 | `oa_personal_wechat_account` |

### 8.2 个微详情（奥创接口只读区域）

| 区域 | 内容 |
|------|------|
| 基本信息 | 微信名、微信号、联系电话 |
| 奥创接口 | api_url / app_id / app_secret / token（全部脱敏 `****`） |

### 8.3 企微 Tab

**上部：应用配置**（`oa_wework_account`）

| 控件 | 类型 |
|------|------|
| F-NAME | `<Input />` |
| F-CORP-ID | `<Input />` |
| F-AGENT-ID | `<Input />` |
| F-SECRET | `<Input />`（脱敏） |

**下部：员工账号**（`oa_wework_employee`，S-08b）

| 控件 | 类型 |
|------|------|
| TBL-EMPLOYEE | 表格（昵称、企微 ID、手机、部门、岗位、状态） |
| BTN-ADD-EMP | 新增员工弹窗 CRUD |

---

## 9. P-M4-011 三方关联图谱

| 控件 | 类型 |
|------|------|
| F-USER | `<UserSelect />` |
| TBL-REL | 表格（个微 / 视频号 / 企微 三方映射） |

### 9.1 图谱视图

```
+--------------------------------+
| 张三（个微 zhangsan_wx）      |
+--------------------------------+
    ↓
+--------------------------------+
| 视频号 video_001 + 视频号 video_002 |
+--------------------------------+
    ↓
+--------------------------------+
| 企微 corp_001                  |
+--------------------------------+
```

---

## 10. 跨页通用约定

- **强关联选择器**：禁用手动输入（前端 + 后端双重校验）
- **敏感数据脱敏**：身份证/手机/ICCID/Cookie 中间 4 位 → `****`
- **审计日志**：所有 CRUD 记录到 `sys_audit_log`
- **空/错/加载**：三态完整

---

*下一步：API Spec / STATE / SLICES / CHECKLIST / TESTCASES。*
