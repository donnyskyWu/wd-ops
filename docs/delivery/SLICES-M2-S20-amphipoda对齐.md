# SLICES-M2-S20 — amphipoda 玩法对齐

> **Slice**：S-20  
> **版本**：v1.0 | 2026-08-31  
> **预估工时**：8~10 人日  
> **优先级**：P0  
> **Gate**：独立 Slice；**一片一会话**  
> **关联 FR**：FR-M2-011（PRD 增量）

---

## 1. 目标

OPS 内容编辑对齐 amphipoda：**N 场 + matchScheme + jingcai AI + Football 全量 sync**；仅改 OPS 前后端。

---

## 2. 依赖

| 依赖 | 状态 |
|------|------|
| S-06 内容 CRUD | ✅ |
| S-15 AI 对话抽屉 | ✅ |
| ADR-054 Football 桥接 | ✅ |
| ADR-075 合并组 competitions | ✅ |
| ADR-076 | ✅ Accepted |

---

## 3. 交付物

### 3.1 后端（football-module-ops）

| # | 交付 | 文件/模块 |
|---|------|-----------|
| B1 | Flyway `match_scheme_json` / `match_type` / `competition_ids_json` | `docs/deploy/.../sql/` |
| B2 | `ProductionContentDO` / Req / VO 增量 | `dal` / `api/dto` |
| B3 | create/update 持久化 + 首场摘要回填 | `ProductionContentServiceImpl` |
| B4 | Football sync 全量 matchScheme | `FootballArticleBridgeServiceImpl` |
| B5 | jingcai N events + plays 映射 | `JingcaiArticleRequestBuilder` |
| B6 | jingcai 润色上下文注入 | `JingcaiArticleRequestBuilder` + `AiContentServiceImpl` |
| B7 | author persona 只读填充 | `JingcaiArticleRequestBuilder` |
| B8 | IT：`MatchSchemeSyncIT` / `JingcaiEventsBuilderTest` | `src/test` |

### 3.2 前端（football-front OPS 视图）

| # | 交付 | 文件/模块 |
|---|------|-----------|
| F1 | 抽取 shared 玩法组件 | `#/components/ops/match-scheme/*` |
| F2 | `ContentEditPanel` 集成 Tab + 玩法区 | `views/ops/production/content/` |
| F3 | 任务/合并组预填 + Banner | 任务执行 → 内容编辑入口 |
| F4 | `AiContentDrawer` N 场摘要 | `AiContentDrawer.vue` |
| F5 | API 类型增量 | `#/types/ops/content` `#/api/ops/content` |
| F6 | E2E：`content-match-scheme.spec.ts` | `e2e/` |

---

## 4. 分期（建议单 Slice 内顺序）

| 阶段 | 内容 | DoD |
|------|------|-----|
| **S20-a** | B1~B4 DB + sync | IT 全量 sync 绿 |
| **S20-b** | B5~B7 jingcai 组包 | 单测 events 映射 |
| **S20-c** | F1~F2 玩法 UI | 竞足 2 场选手动玩法 |
| **S20-d** | F3 任务预填 | 合并 2 场预填 |
| **S20-e** | F4 + B6 润色上下文 | 2 轮对话 IT/E2E |
| **S20-f** | E2E P0 | TESTCASES 100% |

---

## 5. 验收（DoD）

- [ ] CHECKLIST-M2-amphipoda对齐增量 **100%**
- [ ] TESTCASES-M2-amphipoda对齐增量 **P0 100%**
- [ ] `mvn verify`（ops-server 范围）无失败
- [ ] `playwright test content-match-scheme` 无失败
- [ ] ADR-076 决策项代码可追踪
- [ ] **未修改** member-server / jingcai 服务代码

---

## 6. 禁止

- ❌ 与本 Slice 无关的 M8/M6/工作任务改动
- ❌ 推断 Spec 未写字段（1501 铁律仍生效）
- ❌ 调用 member `generateAiScheme` 作为主路径

---

## 7. 回滚

- Flyway 列可 NULL；关闭功能：`sys_param` 暂无开关 — 回滚靠代码 revert
- Football sync：无 matchScheme 时回退 stub 逻辑（feature flag **不**做，Spec 未要求）
