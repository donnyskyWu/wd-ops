# CHECKLIST-M10-数据采集

> **M10 自检清单** | 版本 v1.0 | 2026-06-07

---

## 1. 范围与 FR

- [ ] 5 个 Slice 全部完成
- [ ] 2 个 FR 全部实现
- [ ] 9 个 API 实现

## 2. 全局规范（🔴 必查）

- [ ] `accountId` 用 `<AccountSelect />`（强关联）
- [ ] `platformType` 用 `<DictSelect dict-type="dict_platform_type" />`
- [ ] `method` 用 `<DictSelect dict-type="dict_collect_method" />`
- [ ] `source` 用 `<DictSelect dict-type="dict_collect_source" />`
- [ ] `frequency` 用 `<DictSelect dict-type="dict_collect_frequency" />`
- [ ] `status` 用 `<DictSelect dict-type="dict_collect_status" />`
- [ ] `checkType` 用 `<DictSelect dict-type="dict_quality_check_type" />`
- [ ] `level` 用 `<DictSelect dict-type="dict_quality_level" />`
- [ ] `apiConfig` AES-256 加密
- [ ] 跨租户隔离
- [x] Collector bind / batch-import API 实现（§8 · ADR-047 IT 覆盖）
- [x] Channel-A 六平台执行路由 stub IT 通过（`M10ApiCollectorExec*IT`）
- [x] 公众号图文 article-list MVP（`oa_wechat_mp_article` · V116 · `M10ApiCollectorExecMpArticleIT`）
- [x] Playwright 采集 Tab 冒烟（`integration-platform-collect-tab.spec.ts`）

## 3. 中间件约束（🔴）

- [ ] **不依赖** XXL-JOB → 用 Spring `@Scheduled`
- [x] M10-COL-S-02 cron 调度扫描 + `nextRunAt`（`M10ColCollectScheduleS02IT`）
- [ ] **不依赖** RabbitMQ → 用 Spring `@Async`
- [ ] **不依赖** MinIO → 用本地文件系统

## 4. 状态机

- [x] 采集任务/日志 5 状态（待执行/执行中/成功/失败/部分成功 · `PARTIAL` · ADR-049）
- [ ] 指数退避重试（1/5/15min）

## 5. 测试

- [ ] 单元测试覆盖 80%+
- [ ] AC 全部通过（9 条）
- [ ] cron 表达式校验
- [ ] 加密字段测试

## 6. 文档

- [x] PRD 同步（2026-06-24 · [CHANGELOG-IMPL-20260624](./changelog/CHANGELOG-IMPL-20260624.md)）
- [x] API-M10 Collector 绑定 / Channel-A / 全量 dataType / 日志详情 delta（ADR-047/049 · 2026-06-24）
- [ ] Swagger 文档
- [x] 字典项已登记（V53：`dict_collect_status` / `dict_quality_check_type`）

## 7. Sign-off

| 角色 | 姓名 | 日期 |
|------|------|------|
| 开发 |  |  |
| 测试 |  |  |
| 产品 |  |  |
| 架构 |  |  |

## 8. Phase 2 M10-API（ADR-047 · 可验证项）

> 仅勾选代码/IT 已验证项；未实现项保持 `[ ]`。

- [x] V110 `oa_collector_account_bind` 表 + 凭证扩展列（`M10ApiCollectorBindS01IT`）
- [x] `UnifiedCollectorAdapter` bind / sync / test-connection（`M10ApiCollectorBindS02IT`）
- [x] M4 平台账号「采集 Tab」（`PlatformAccountCollectTab.vue` + `collector-bind.ts`）
- [x] `CollectExecutionService` 路由：`WECHAT_MP_API` / `DOUYIN_OPEN_API` / `KUAISHOU_OPEN_API`（S-05/S-06 IT）
- [x] `CollectExecutionService` 路由：`WECHAT_CHANNELS_API` / `XIAOHONGSHU_OPEN_API` / `BILIBILI_OPEN_API`（`M10ApiCollectorExecChannelAStubIT`）
- [x] M8 INTERNAL 平台 Tab 硬切 + V113 凭证合并（`M8ColS06IT`）
- [x] V113 后 batch-import：`POST /admin-api/oa/collector-bind/batch-import`（`M10ApiCollectorBatchBindIT`）
- [x] `CollectExecutionService` 路由：`WECHAT_MP_API` + `dataType=MP_ARTICLE_LIST` → article-list（P2 · `M10ApiCollectorExecMpArticleIT`）
- [x] V116 `oa_wechat_mp_article` + `oa_collect_task.data_type` + `dict_collect_data_type`
- [x] OA ↔ collector HTTP 契约 IT（`M10ApiCollectorChannelAHttpIT` · bilibili/xhs 路径修正 · kuaishou 嵌套 envelope）
- [x] 生产 collector 联调步骤已文档化（`ops-platform-server/README.md` · `M10ApiCollectorLiveSmokeIT` · 需本地 `stub=false` + collector 运行）
- [ ] M10-API Gate Sign-off（P2-M10-A 整包验收）
- [ ] 全 Channel-A 平台生产 collector 联调签收（非 stub · 需真实凭证人工验证）

## 9. Phase 2 M10-WECOM（ADR-048 · Channel-C）

> 仅勾选代码/IT 已验证项；S-02~S-04 保持 `[ ]`。

- [x] ADR-048 已采纳（Q1~Q7 · 2026-06-22）
- [x] V117 `oa_wework_daily_stats` + `dict_collect_data_type.WECOM_DAILY_STATS`
- [x] `WeComApiClient` + `WeComAdapter` 直连 OpenAPI（不经 collector bind）
- [x] `CollectExecutionService` 路由：`WECOM_API` + `WEWORK` + `INTERNAL` + `WECOM_DAILY_STATS`
- [x] 任务 `account_id` → `oa_wework_account.id`（应用级，展开员工采集）
- [x] MockWebServer IT（`M10ApiCollectorExecWecomIT`）
- [x] M10-WECOM-S-02 探活 / conn_status（V118 · `M10WecomS02IT` · `WeworkAppConfigPanel`）
- [x] M10-WECOM-S-03 任务 CRUD + WeworkAccountSelect（`M10WecomS03IT` · `task-edit.vue`）
- [x] M10-WECOM-S-04 M1 微信分析企微 Tab 消费（`WechatAnalysisWeworkController` · `WechatDataAnalysis.vue` · `M10WecomS04IT`）
- [x] M10-AO-S-06 延伸：M1 个微 Tab 消费 + 详情 7/30 日趋势图（`WechatAnalysisPersonalController` · `M10PersonalWechatAnalysisIT`）

## 10. Phase 2 M10 全量采集与展示桥接（ADR-049 · 2026-06-24）

> 仅勾选代码/IT 已验证项。

- [x] `CollectPlatformDefaults` 平台默认 source/method + 全量 dataType 顺序
- [x] 任务 CRUD 自动补全 method/source；`data_type` 空 = 全量（`M10ApiCollectorExecAllTypesIT`）
- [x] 多平台执行 + 日志 `PARTIAL` + `typeResults`（V120 · `M10ColCollectLogDetailIT`）
- [x] V121 抖音粉丝/作品表 + V122 视频号/快手/小红书表 + 公众号文章扩展列
- [x] 多平台 stub/HTTP IT（`M10ApiCollectorExecMultiPlatformIT` · `M10ApiCollectorExecDouyinListIT` 等）
- [x] `task-edit.vue` 隐藏 method/source/dataType；只读「采集范围」
- [x] `log.vue` 多类型 `typeResults` 折叠展示
- [x] `CollectedDataQueryService` → M1/M4 分析 API（`M10CollectedDataDisplayIT`）
- [ ] 全 Channel-A 平台 **生产** collector 全 dataType 人工签收（非 stub）


---

## 全局规范引用

> 本文档遵循 [`GLOBAL-CONVENTIONS.md`](./GLOBAL-CONVENTIONS.md) 中定义的全局规范：
> - 强关联属性 → 强制使用 5 类选择器组件（RealNameSelect / PhoneSelect / SimCardSelect / CompanySelect / AccountSelect），禁用手动输入
> - 枚举属性（方式/状态/类型/平台/阶段）→ 统一从数据字典（`dict_*`）选择，页面只读下拉
> - 跨租户 + 状态校验 → 错误码 1500-1504 统一语义
> - 数据安全 → 敏感字段（身份证/手机/API 密钥）强制脱敏展示，凭证类字段 AES-256 加密存储
> - 详见 [`GLOBAL-CONVENTIONS.md § 2`](./GLOBAL-CONVENTIONS.md) (字典)、[`§ 3`](./GLOBAL-CONVENTIONS.md) (选择器)、[`§ 4`](./GLOBAL-CONVENTIONS.md) (错误码)

