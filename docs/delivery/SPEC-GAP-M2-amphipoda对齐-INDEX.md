# M2 amphipoda 对齐 — Spec 缺口包索引

> **日期**：2026-08-31  
> **状态**：Accepted，可开 **S-20** 实现  
> **约束**：仅改 OPS 前后端；AI = jingcai.article；Football 双写保留

---

## 文档清单

| 类型 | 路径 |
|------|------|
| **ADR** | [ADR-076-OPS内容生产amphipoda玩法与matchScheme对齐.md](../adr/ADR-076-OPS内容生产amphipoda玩法与matchScheme对齐.md) |
| **PRD 增量** | [PRD-M2-内容生产-amphipoda对齐增量.md](../product/PRD-M2-内容生产-amphipoda对齐增量.md) |
| **UX 增量** | [UX-M2-内容生产-amphipoda对齐增量.md](../product/UX-M2-内容生产-amphipoda对齐增量.md) |
| **API 增量** | [API-M2-内容生产-amphipoda对齐增量.md](../engineering/API-M2-内容生产-amphipoda对齐增量.md) |
| **STATE 增量** | [STATE-M2-内容生产-amphipoda对齐增量.md](../engineering/STATE-M2-内容生产-amphipoda对齐增量.md) |
| **Slice** | [SLICES-M2-S20-amphipoda对齐.md](./SLICES-M2-S20-amphipoda对齐.md) |
| **Checklist** | [CHECKLIST-M2-内容生产-amphipoda对齐增量.md](./CHECKLIST-M2-内容生产-amphipoda对齐增量.md) |
| **Testcases** | [TESTCASES-M2-内容生产-amphipoda对齐增量.md](./TESTCASES-M2-内容生产-amphipoda对齐增量.md) |

## 已 supersede

- ADR-054 §8.4（`matchScheme` Out of Scope）→ ADR-076 D11

## 开干前最后确认（已闭合）

| 项 | 结论 |
|----|------|
| jingcai 多 Event | ✅ member 已验证；OPS 对齐组包 |
| 润色上下文 | ✅ requirements 注入 conversationHistory |
| 任务无 matchPlays | ✅ 用户编辑页手动选玩法 |
| member generateAiScheme | ❌ OPS 主流程不用 |

## 实现入口

1. 读 ADR-076 + SLICES S-20  
2. 按 S20-a → S20-f 顺序  
3. Gate = CHECKLIST 100% + TESTCASES P0 100%
