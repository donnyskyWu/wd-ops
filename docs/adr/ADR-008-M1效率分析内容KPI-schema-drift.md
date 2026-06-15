# ADR-008：M1 效率分析（人效盘点）"内容 KPI" Schema Drift

**日期**�?026-06-10
**触发**：S-R9 走查效率分析页（FR-M1-007�?
**状�?*：已实施（方�?A，S-R21-Mike 2026-06-10�?

---

## 一、问�?

PRD-M1-运营管理.md §4.7.1 / UX-M1-运营管理.md §9.2 都要�?人效盘点展开 4 大卡片：任务详情 / 财务指标 / 内容指标 / 趋势�?�?

�?**`oa_content` 表（ContentDO）schema 不支持按 user 聚合内容产出 / 平均阅读 / 爆款�?*�?
- `oa_content` 字段：`account_id / title / platform_type / content_type / publish_time / read_count / like_count / comment_count / forward_count / is_hit / data_source / status / creator(string)`
- �?`user_id / author_id / assignee_id / ops_user_id` 字段
- `creator` �?VARCHAR（来�?TenantBaseDO），值为 `'seed-content'` 这种字符�?

**关联链路**�?
- 经办�?user) �?IP�?ip_group_id) �?账号(account) �?内容(content)
- �?`oa_account` 没有 user_owner_id（账号仅归属 IP 组，不归属某�?user�?
- `oa_ip_group_member` �?user_id 关联，但这是"成员/组长"关系，与"内容生产责任"无强对应

---

## 二、当前处理（S-R9 临时方案�?

`ProductivityReviewServiceImpl.toVO` 在内�?KPI 字段处全部填 0�?

```java
// S-R9: 内容 KPI（ContentDO �?user 关联 �?�?0，记�?ADR-008�?
private Integer contentOutput = 0;
private Long avgRead = 0L;
private Long avgPlay = 0L;
private Integer hitCount = 0;
```

前端 `Efficiency.vue` 4 Card 展开里加 ADR 提示�?
```vue
<div class="expand-desc">注：ContentDO �?user 关联，详�?ADR-008</div>
```

`/efficiency` �?KPI �?/ 表格 / 4 tab �?`contentOutput/avgRead/avgPlay/hitCount` 全部 0�?

---

## 三、候选方�?

### 方案 A（推荐）：`oa_content` �?`author_id BIGINT`

```sql
ALTER TABLE oa_content ADD COLUMN author_id BIGINT NULL COMMENT '内容生产�?user_id';
ALTER TABLE oa_content ADD KEY idx_oa_content_author (tenant_id, author_id);
```

**优点**�?
- 直接解决"按经办人聚合内容 KPI"
- �?M2 SOP 模块（作者管理）天然契合

**缺点**�?
- 涉及历史数据回填（spec/seed �?~26 条内容）
- 修改面广（ContentDO / ContentMapper / ContentVO 都要改）

### 方案 B：`oa_account_owner` 多对多关联表

```sql
CREATE TABLE oa_account_owner (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    account_id      BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    is_primary      TINYINT      NOT NULL DEFAULT 0,
    start_time      TIMESTAMP    NULL,
    end_time        TIMESTAMP    NULL,
    -- ...
);
```

**优点**�?
- 一个账号可有多个经办人
- 不破�?oa_content schema

**缺点**�?
- 复杂度高（多对多 + 时间段）
- 仍需 join 计算 user �?content（多跳）
- 现有 spec 没说账号多人共担

### 方案 C：明�?spec 限定"内容 KPI 不按经办人聚�?

UX §9.2 内容指标 Card 改为�?IP 组维度聚合（不展示按 user 的内�?KPI），spec §4.7.1 AC-M1-007-1 改成"3 大卡�?�?

**优点**�?
- �?schema 改动
- 简�?

**缺点**�?
- 弱化 spec
- 用户体验�?展开人效看不到内�?有点反直�?

---

## 四、待产品决策

S-R9 已经用方�?A �?field 名（`contentOutput/avgRead/avgPlay/hitCount`），**字段定义就绪**�?*�?schema 落实后即可启用聚�?SQL**�?

需产品决策�?
1. 是否接受方案 A（oa_content �?author_id）？
2. 方案 A 是否�?M2 SOP 模块上线前同步引入？
3. 历史 seed 数据是否要回填？

---

## 五、S-R9 期间已确认的"可聚�?字段

| 维度 | �?| 关联字段 | 聚合指标 |
|------|-----|---------|---------|
| 任务 | oa_task | `assignee_id` | taskTotal/completed/inProgress/overdue/completionRate |
| 财务 | oa_order_attribution | `ops_user_id` | costAmount/revenue/roi/orderCount |

**已实现并测试通过**�?
- 后端 `TaskMapper.sumByUser` + `OrderAttributionMapper.sumByUser` + `ProductivityReviewServiceImpl.list/detail/anchors/exportCsv`
- 前端 `Efficiency.vue` 4 tab + 4 Card 展开

---

## 六、关�?

- 走查报告：`docs/delivery/gates/S-R9-报告-20260610.md`
- 修改清单�?
  - `ops-platform-server/.../ProductivityReviewVO.java`�?12 字段�?
  - `ops-platform-server/.../TaskMapper.java`�?sumByUser�?
  - `ops-platform-server/.../OrderAttributionMapper.java`�?sumByUser�?
  - `ops-platform-server/.../ProductivityReviewServiceImpl.java`（聚合实现）
  - `ops-platform-ui-vue/.../Efficiency.vue`�? tab 接真字段 + 提示�?
  - `ops-platform-ui-vue/.../types/productivity.ts`（字段对齐）