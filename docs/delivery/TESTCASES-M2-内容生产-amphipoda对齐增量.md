# TESTCASES-M2-内容生产 — amphipoda 对齐增量

> **Slice**：S-20 | **版本** v1.0 | 2026-08-31  
> **优先级**：P0 必须 100% 通过方可 Gate

---

## P0

### TC-M2-011-P0-01 竞足双场玩法保存

| 项 | 内容 |
|----|------|
| **前置** | 登录内容生产权限；选作者+IP组 |
| **步骤** | 竞足 Tab 选 2 场 → 各选胜平负 → 确定玩法 → 保存 |
| **期望** | DB `match_scheme_json` 长度=2；`match_type=1`；`competition_id`=首场 scheduleId |

### TC-M2-011-P0-02 Football 全量 sync

| 项 | 内容 |
|----|------|
| **前置** | TC-01 已保存 |
| **步骤** | 查 ext.`author_article_id` → member DB `author_article.match_scheme` |
| **期望** | JSON 含 2 场且各有 plays；非 stub；`match_type=1` |

### TC-M2-011-P0-03 jingcai 首生成 N Event

| 项 | 内容 |
|----|------|
| **前置** | 2 场已确定玩法；`chat-via-jingcai=true` |
| **步骤** | 打开 AI 抽屉 → 首条消息生成 |
| **期望** | ops 日志 jingcai submit body 中 `events.length=2`；返回 title/正文；可采纳 |

### TC-M2-011-P0-04 jingcai 润色带上下文

| 项 | 内容 |
|----|------|
| **前置** | TC-03 已生成 1 轮 |
| **步骤** | 第 2 轮输入「加强付费段分析」 |
| **期望** | jingcai submit 的 `requirements` 含历史对话/正文摘要 + 本次要求；返回新正文 |

### TC-M2-011-P0-05 无玩法不可 AI

| 项 | 内容 |
|----|------|
| **步骤** | 预填或选 1 场但不选玩法 → 点 AI |
| **期望** | 按钮 disabled 或 1501/前端拦截；不发起 jingcai |

### TC-M2-011-P0-06 合并任务预填两场

| 项 | 内容 |
|----|------|
| **前置** | ADR-075 合并组 confirm 后 task 含 2 competitions |
| **步骤** | 任务执行 → 创建内容 |
| **期望** | 玩法区预填 2 场队名；Banner 提示；matchPlays 空；AI 仍 disabled |

### TC-M2-011-P0-07 编辑回显

| 项 | 内容 |
|----|------|
| **前置** | TC-01 内容 id |
| **步骤** | 重新打开编辑 |
| **期望** | Tab、2 场、玩法、主玩法与保存一致 |

### TC-M2-011-P0-08 更新玩法 resync

| 项 | 内容 |
|----|------|
| **步骤** | 改其中一场玩法 → 保存 |
| **期望** | Football `match_scheme` 同步更新 |

### TC-M2-011-P0-09 存量兼容只读

| 项 | 内容 |
|----|------|
| **前置** | 仅 `competition_id` 无 matchScheme 的旧内容 |
| **步骤** | GET + 打开编辑 |
| **期望** | 200；显示旧 competitionName；不 500 |

### TC-M2-011-P0-10 M8 降级回归

| 项 | 内容 |
|----|------|
| **前置** | `ai.content.chat-via-jingcai=false` |
| **步骤** | AI 多轮对话 2 轮 |
| **期望** | 走直连 LLM；conversationHistory 生效；不调用 jingcai |

---

## P1（Gate 不阻塞，Slice 内建议）

### TC-M2-011-P1-01 传足场次精确约束

选传足 Tab + 错误场次数 → 前端校验失败。

### TC-M2-011-P1-02 sync 失败重试

模拟 member 写失败 → ext 有 error → 重试 sync 成功。

### TC-M2-011-P1-03 北单 Tab 单场玩法

北单 Tab 选 1 场 + 玩法 → save + sync 绿。

---

## E2E 脚本建议

`e2e/content-match-scheme.spec.ts`：

1. login → 内容列表 → 新建
2. 竞足选 2 场 + 玩法 + AI 首生成（mock jingcai 或 beta 环境）
3. 保存 → 列表赛事列含「2场」
4. （可选）合并任务入口预填断言

---

## 自动化 IT 建议

| 类 | 覆盖 |
|----|------|
| `JingcaiArticleRequestBuilderTest` | N events / plays 映射 / 润色 requirements |
| `FootballArticleBridgeMatchSchemeTest` | 全量 JSON sync |
| `ProductionContentMatchSchemeIT` | create/update/get CRUD |
