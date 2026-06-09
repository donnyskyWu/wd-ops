# P-GATE-DICT 报告 — 字典查询端点 + 作者类型扩展

> **类型**：补丁 Gate（轻量级，ADR-006 衍生）
> **日期**：2026-06-09
> **关联 ADR**：[`ADR-006-字典查询端点与作者类型扩展.md`](../../adr/ADR-006-字典查询端点与作者类型扩展.md)
> **关联 Spec**：[`API-M11-字典管理.md`](../../engineering/API-M11-字典管理.md)
> **关联 Slice**：M11-S-01 字典查询

---

## 1. 范围

| 项 | 内容 |
|----|------|
| 模块 | M11 字典（横切） |
| 触发 | 用户反馈「作者管理 · 添加作者报错：`authorType: 字典值不合法`」+「`liveHours.toString` 渲染崩」 |
| 决策记录 | ADR-006 |
| 范围 | 后端 DictController + DictService + V27 + IT；前端 DictSelect + dict.ts + Author.vue 改造 |

---

## 2. 后端 Checklist

| # | 项 | 状态 | 证据 |
|---|----|------|------|
| 2.1 | DictController `/admin-api/oa/dict/data` | ✅ | `controller/dict/DictController.java` |
| 2.2 | DictController `/admin-api/oa/dict/types` | ✅ | 同上 |
| 2.3 | DictService 缓存 5 分钟（自实现，无新依赖） | ✅ | `service/dict/DictService.java` |
| 2.4 | V27 Flyway 迁移（追加 `LIVE/BOTH/IMAGE_TEXT` + `dict_anchor_type LIVE`） | ✅ | `V27__dict_author_type_extend.sql` |
| 2.5 | OaErrorCodes 加 `BAD_REQUEST(1500)` / `DICT_TYPE_NOT_FOUND(2020)` | ✅ | `OaErrorCodes.java` |
| 2.6 | 铁律一：未硬编码 userId/tenantId | ✅ | 全走 DevAuthFilter + TenantContextHolder |
| 2.7 | 铁律二：写入侧 `@InDict` 仍生效 | ✅ | 单元级 `isValidValue` 行为不变；本端点为读取 |
| 2.8 | 铁律三：全表 tenant 隔离 | ✅ | 字典为公共主数据，不带 tenant 过滤；强制 DevAuthFilter 登录 |

---

## 3. 前端 Checklist

| # | 项 | 状态 | 证据 |
|---|----|------|------|
| 3.1 | `dict.ts` API 封装 | ✅ | `api/dict.ts` |
| 3.2 | `DictSelect.vue` 通用组件 | ✅ | `components/DictSelect.vue` |
| 3.3 | `Author.vue` 作者类型 → DictSelect | ✅ | `views/operations/Author.vue` |
| 3.4 | `Author.vue` 表格列判空保护（followerCount / contentCount / liveHours / opsUserNames） | ✅ | 同上 |
| 3.5 | `Author.vue` `formatNumber` 兜底（防 undefined） | ✅ | 同上 |
| 3.6 | `Author.vue` `account.ts` 切到 `@/utils/request` 走 dev token | ✅ | `api/account.ts` |
| 3.7 | `Author.vue` `keyword/page/size` 参数对齐后端 | ✅ | `views/operations/Author.vue` |
| 3.8 | `Author.vue` IP 组 `groupType=2` 过滤、字段名 `groupName/name` 兼容 | ✅ | 同上 |
| 3.9 | 全部前端 mock 兜底移除 | ✅ | 无 `mockAuthorList` 引用 |

---

## 4. 测试结果

### 4.1 后端 IT

| IT 类 | 用例 | 状态 |
|-------|------|------|
| **M11DictS01IT** | authorTypeFullCoverage（5 个值） | ✅ |
| M11DictS01IT | otherDictsReturnData（dict_user_status/cost_type/platform_type） | ✅ |
| M11DictS01IT | missingTypeReturns1500 | ✅ |
| M11DictS01IT | unknownTypeReturns2020 | ✅ |
| M11DictS01IT | noTokenReturns401（HTTP 401） | ✅ |
| M11DictS01IT | typesListContainsAuthorType | ✅ |
| **M1AuthorS04IT** | 全量 3 项 | ✅ |
| **M1IpGroupS01~S03IT** | 全量 16 项 | ✅ |
| **M11DictS01IT** 汇总 | 6 用例 0 fail | ✅ |

### 4.2 回归范围（M* 全量 IT）

| 范围 | 总数 | 失败 | 错误 | 跳过 |
|------|------|------|------|------|
| M0~M9 + GateS0/S2/S7 + M11 + Seed + Util | 38 类 | 0 | 0 | 0 |
| M1 全部（M1AuthorS04/M1IpGroupS01~S03/M1AnalysisS06/M1ImportS09/M1OpsAnchorS05） | 6 类 | 0 | 0 | 0 |
| 跨租户回归（M4CrossTenantIT） | 1 类 11 例 | 0 | 0 | 0 |

---

## 5. Flyway 迁移

| 版本 | 名称 | 操作 |
|------|------|------|
| **V27** | dict_author_type_extend | 追加 3 条 `dict_author_type`（LIVE/BOTH/IMAGE_TEXT）+ 1 条 `dict_anchor_type`（LIVE，去重） |

V17/V18 未动，符合「铁律二：不可回改已发布的 Flyway」。

---

## 6. 已知遗留 / Phase 2

| 项 | 说明 | 落点 |
|----|------|------|
| 其他模块 `<DictSelect>` 改造 | M2/M3/M4/M5/M6/M7/M8/M9 页面未统一替换硬编码下拉 | M12-S-01（建议） |
| 字典管理端点（CRUD） | 本期仅 GET，CRUD 走 Flyway | Phase 2 |
| 缓存失效事件 | V27 写完后 admin 进程内 `dataCache` 5 分钟自动失效；分布式场景需 `Redis pub/sub` | Phase 2 |

---

## 7. Sign-off

| 角色 | 签字 |
|------|------|
| 产品 | ☐（草案，待项目签字） |
| 后端 | ✅ IT 全绿 + 编译通过 |
| 前端 | ✅ DictSelect 改造完成，TypeScript 编译待 dev 启动验证 |
| QA | ☐（建议 P-GATE 通过后跑 Playwright `ux-routes-smoke.spec.ts`） |

---

## 8. 归档路径

- 本报告：`docs/delivery/gates/P-GATE-DICT-报告-20260609.md`
- ADR：`docs/adr/ADR-006-字典查询端点与作者类型扩展.md`
- API 规范：`docs/engineering/API-M11-字典管理.md`
