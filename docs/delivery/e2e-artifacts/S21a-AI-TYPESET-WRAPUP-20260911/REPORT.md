# S-21a AI 排版 — 收尾验证报告 (2026-09-11 session 2)

## 范围

| 项 | 值 |
|----|-----|
| Slice | S-21a（FR-M2-012 AI 排版 MVP） |
| 目标 | 关闭 P0 自动化缺口、复验真实 LLM 联调、更新 Checklist/TESTCASES |
| Gate 结论 | **未通过** — Checklist ~41%、P0 37/39、无正式 Sign-off |

## 栈状态

| 服务 | 端口 | 健康 |
|------|------|------|
| ops-server | 48094 | HTTP 200 |
| Gateway | 48080 | HTTP 200 |
| football-front | 5777 | HTTP 200 |

## 自动化测试

### 后端（聚焦 S-21a）

```bash
mvn -pl football-module-ops/football-module-ops-server \
  -Dtest=AiSemanticTypesetIT,FootballLayoutSemanticRendererTest,ParagraphEmphasisHelperTest,AiSemanticTypesetServiceTest test
# → 53/53 绿

mvn -pl football-module-ops/football-module-ops-server \
  -Dtest=FootballTemplateDeciderTest,TypesetFidelityGateTest,SegmentSlotMapperTest,AiTypesetBodySupportTest,ProductionContentAiSemanticTypesetTest,OpsPermissionCheckerTest,OpsGlobalExceptionHandlerAccessDeniedTest test
# → 30/30 绿
```

**合计：83/83 绿，0 失败。**

### 前端

```bash
pnpm exec vitest run src/api/ops/aiSemanticTypesetting.test.ts
# → 7/7 绿
```

### 未跑 / 缺失

| 项 | 状态 |
|----|------|
| `content-ai-semantic-typeset.spec.ts` | **不存在** → P0-32 阻塞 |
| `SeedVerificationIT` | **不存在** |
| ops-server 全量 `mvn verify` | 未重跑（历史 8 失败在 WorkTask/WechatArticleHtmlFetcher，非 S-21a） |

## API 冒烟（真实 LLM）

脚本：`scripts/_tmp_ai_typeset_preview_probe.py`

| 内容 ID | 模式 | 结果 | 备注 |
|---------|------|------|------|
| 9448 | AUTO | code=0，7.5s | `decision-scan`，5 segments，fidelity=true，layoutHtml 2299B |
| 9449 | AUTO | code=0，~37s | `analysis-report`（marketing），layoutHtml 9421B |

登录：`admin/admin123` via Gateway；tenant-id=1。

**AI 模型**：真实 LLM 调用成功 → tenant 1 模型已 CONNECTED 且 `AI_TYPESET_SEMANTIC` 可用（无需用户手动「测试连接」）。

## P0 统计

| 类别 | 通过 | 说明 |
|------|------|------|
| 自动 | 33/35 | 后端 IT/单测 + Vitest 部分 |
| 人工 | 4 | P0-28/31/33/39（2026-09-08 浏览器证据） |
| N/A | 1 | P0-30（工作台 AUTO-only） |
| 待补 | 1 | **P0-32** Playwright 全链路 |
| **合计** | **37/39** | 失败 0 |

## Checklist

- §1~§10：**45/111 ≈ 41%**
- 本轮新勾选：Fidelity Gate、decider/单测覆盖、真实 LLM 冒烟

## 剩余阻塞（Gate 不可通过）

1. **P0-32**：需新增 Playwright spec 并跑通降级/错误/overwrite 全链路
2. **SeedVerificationIT**：decision-scan / analysis-report / AI_TYPESET_SEMANTIC seed 自动化验证
3. **Checklist 100%**：§1 实现前 Gate、§6 前端交互细项、§9 并发/Playwright/verify、§10 部署归档
4. **ops-server 全量 verify** 无失败（或正式豁免非 S-21a 项）
5. **Gate 报告 Sign-off** + `MASTER-EXECUTION-TRACKER.md` 更新（仅 Gate 全项满足后）

## 用户下一步

1. 新增 `football-front/apps/web-ele/tests/content-ai-semantic-typeset.spec.ts` 覆盖 P0-32
2. 在 `SeedVerificationIT`（或等价 IT）验证 V198/V201/V202 seed
3. 人工补勾 Checklist §6 前端细项（或 Playwright 通过后自动闭合）
4. 修复或豁免 WorkTask IT 后重跑 `mvn verify`
5. Gate 全项满足后归档 `GATE-S{n}-报告-20260911.md`

## 关联文档

- [CHECKLIST-M2-AI排版增量.md](../../CHECKLIST-M2-AI排版增量.md)
- [TESTCASES-M2-AI排版增量.md](../../TESTCASES-M2-AI排版增量.md)
- [SLICES-M2-S21a-AI排版.md](../../SLICES-M2-S21a-AI排版.md)
