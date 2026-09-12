# CHECKLIST-M2-内容生产 — amphipoda 对齐增量

> **Slice**：S-20 | **版本** v1.0 | 2026-08-31  
> **关联**：[SLICES-M2-S20-amphipoda对齐.md](./SLICES-M2-S20-amphipoda对齐.md) · [ADR-076](../adr/ADR-076-OPS内容生产amphipoda玩法与matchScheme对齐.md)

---

## 1. Spec

- [ ] ADR-076 Accepted 且与实现一致
- [ ] PRD/UX/API/STATE 增量文档已阅读
- [ ] ADR-054 §8.4 作废已在代码注释/文档引用 ADR-076

## 2. 数据面（S20-a）

- [ ] Flyway：`match_scheme_json` / `match_type` / `competition_ids_json`
- [ ] `ProductionContentDO` + Mapper 字段映射
- [ ] create/update 持久化 `matchScheme` + 首场 `competition_id/name`
- [ ] GET 内容详情/列表返回 `matchType` / `matchScheme` / `matchSummary`
- [ ] 存量内容无 matchScheme 仍可 GET（兼容）

## 3. Football sync（S20-a）

- [ ] create/update sync **全量** `match_scheme`（非 stub）
- [ ] sync `match_type` 取自内容表（非硬编码 1）
- [ ] sync 失败不阻断 OPS save（ADR-054 §9.2）
- [ ] `sync-football-scheme` 重试幂等
- [ ] IT：`MatchSchemeFootballSyncIT` 通过

## 4. jingcai AI（S20-b / S20-e）

- [ ] `JingcaiArticleRequestBuilder` N events 映射（参照 member 语义）
- [ ] matchPlays → playType/picks 映射单测
- [ ] `authorProfile` 从作者 persona 只读填充
- [ ] round≥2：`conversationHistory` + 上一轮正文注入 requirements
- [ ] requirements 截断策略（3 轮 + 4000 字）已实现
- [ ] `chat-via-jingcai=false` 降级 M8 仍绿（回归）
- [ ] **未**调用 member `/generate-ai-scheme`

## 5. 前端玩法 UI（S20-c）

- [ ] shared 组件自 amphipoda 抽取（非 1600 行复制）
- [ ] Tab：竞足/传足/北单/足球 + matchType 同步
- [ ] 多选场次 + 玩法 + 主玩法 + 删除
- [ ] 「确定玩法」态；未确认 AI disabled
- [ ] 传足场次精确校验
- [ ] `dict_scheme_type` 非玩法 SSOT（隐藏或可选）
- [ ] 保存/回显 matchScheme 正确

## 6. 任务预填（S20-d）

- [ ] 单场 task → 预填 1 场（无 matchPlays）
- [ ] 合并组 task → 预填 **全部** competitions
- [ ] 预填 Banner 文案
- [ ] 预填后须手动选玩法才可 AI

## 7. AI 抽屉（S20-e）

- [ ] 信息参数展示 N 场摘要
- [ ] context 传 `matchScheme` + `matchType`
- [ ] 采纳写回 paid/free（ADR-054 §6.2）

## 8. 全局铁律

- [ ] 强关联字段选择器 + 后端校验（1501）
- [ ] 枚举 `@InDict`（matchType 若入字典则 1503；否则 Integer 范围校验）
- [ ] 全表 `tenant_id` 隔离
- [ ] `@PreAuthorize` 内容生产权限不变
- [ ] JSON 传 snowflake id 用字符串（如涉及）

## 9. 测试

- [ ] TESTCASES-M2-amphipoda对齐增量 P0 **100%**
- [ ] `mvn verify` ops-server 范围无失败
- [ ] Playwright `content-match-scheme` 无失败
- [ ] 上一阶段 M2 P0 冒烟仍绿

## 10. 范围外确认

- [ ] **未修改** member-server 代码
- [ ] **未修改** jingcai 服务
- [ ] amphipoda 老入口仍可用（ smoke 可选）
