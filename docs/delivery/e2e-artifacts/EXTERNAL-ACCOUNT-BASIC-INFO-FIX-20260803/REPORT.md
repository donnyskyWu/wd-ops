# EXTERNAL-ACCOUNT-BASIC-INFO-FIX-20260803

> 外部账号详情 → 账号基本信息：乱码拼接 + 粉丝数缺失 + 互动率异常

## 用户现象

详情抽屉「账号基本信息」显示类似 **`小乌探影212902,804,86100.00%`**，数字与百分号连在一起，无法区分含义；用户期望看到 **粉丝数** 及带标签的独立字段。

## 字段含义（修复后）

| 字段 | 含义 | 数据来源 |
|------|------|----------|
| **账号名称** | 外部账号展示名 | `oa_external_account.display_name` |
| **账号ID** | 外部账号主键 | `oa_external_account.id` |
| **粉丝数** | 账号粉丝总量 | `oa_external_account.follower_count`（抖音 profile 采集写入） |
| **作品数 / 文章数 / 视频数** | 已采集作品条数 | 前端按 `accountId` 聚合作品数 |
| **总播放(估)** | 各作品播放量之和 | Σ `play_count`（抖音 tab） |
| **获赞数** | 各作品点赞之和 | Σ `like_count` |
| **平均播放 / 平均阅读** | 总播放 ÷ 作品数 | 聚合计算 |
| **互动率** | 总获赞 ÷ 总播放 × 100% |  capped 100%；无播放数据时显示 `-` |
| **阅读数 / 播放量** | 公众号/视频号总阅读或播放 | Σ `play_count` |

## Root cause

| # | 问题 | 根因 |
|---|------|------|
| 1 | 数值连写、无标签 | `el-descriptions` 内使用 `<template v-if>` 包裹 `el-descriptions-item`，Element Plus 无法正确收集子项 slot，表格结构错乱，内容挤在同一行 |
| 2 | 「总播放」误当粉丝数 | `aggregateWorksByAccount` 将 `followerCount` 赋值为 `Math.max(playCount)`，列表/详情均绑定该字段，语义与「粉丝数」不符 |
| 3 | 互动率 100% / 超高 | 采集作品 `play_count=0` 时仍用 `获赞/播放` 计算；分母极小或缺失时产生无意义百分比；现改为无播放时 `-`，有播放时 cap 100% |
| 4 | 后端无粉丝字段 | `ExternalWorkVO` 未带出 `oa_external_account.follower_count`；采集 executor 拉 profile 后也未 persist `follower_count` |

## Fix

| 文件 | 变更 |
|------|------|
| `ExternalAccountAnalysis.vue` | 移除 template 包裹，每项独立 `v-if`；新增粉丝数；总播放用 `totalPlayEstimate`；统一 `formatEngagement` / `formatFollowerCount`；列表抖音 tab 增加粉丝数列 |
| `monitor-map.ts` | 区分 `followerCount`（profile）与 `totalPlayEstimate`（Σ播放）；`engagement` 改为 `number \| null`；新增 `calcEngagementRate` |
| `ExternalWorkVO.java` | 新增 `accountFollowerCount` |
| `MonitorServiceImpl.java` | 批量加载 `ExternalAccountDO`，VO 填充粉丝数 |
| `ExternalAccountCollectExecutor.java` | 抖音 user profile 采集时写入 `follower_count` |

## Before / After

| 场景 | Before | After |
|------|--------|-------|
| 基本信息布局 | `小乌探影212,902804100.00%` 一行乱码 | 双列带标签：账号名称 / 粉丝数 / 作品数 / 总播放(估) / … |
| 粉丝数 | 无；误显示 max(play) | `125,000` 或 `-`（未采集） |
| 总播放(估) | 绑定 max(play) 称 followerCount | Σ 各作品 `play_count` |
| 互动率 | `100.00%` 或 `86100.00%` | `12.50%` 或 `-`（无播放） |

## Verification

- `read_lints` on Vue/TS → 无新增 lint
- `mvn compile -DskipTests` on `football-module-ops-server` → **通过**
- 手动：外部账号分析 → 抖音 tab → 详情 → 确认每字段独立一行且粉丝数可见

## Notes

- 历史账号需 **重新跑一次抖音 user 采集** 才会回填 `oa_external_account.follower_count`
- 抖音作品 `play_count` 仍为 0 时，总播放/互动率会显示 0 或 `-`，属采集数据缺口，非展示 bug
