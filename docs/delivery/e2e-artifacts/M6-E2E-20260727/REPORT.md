# E2E 测试报告 — M6 数据分析 — 20260727

## 1. 概要
- 范围: M6 全链路（元数据→指标→分析→查询→大屏配置→自建大屏全屏；数据源 oa_douyin_video）
- 环境: Gate（admin/admin123 租户 1）；:5777 / :48080 / :48094
- 结论: ❌ 未通过（Pass 4/6，Fail 2，Blocked 0）
- Playwright: `ops-platform-ui-vue/tests/m6-metadata-screen-e2e.spec.ts`
- seed 探测: oa_douyin_video=78 行；oa_content=0；oa_douyin_video=78；query9861 total=0；98601 widgets=14 nonZero=1

## 2. 环境
| 服务 | 地址 | 状态 |
|------|------|------|
| 前端 | :5777 | UP |
| Gateway | :48080 | UP |
| oa-server | :48094 | UP |

## 3. 运行时数据
| 类型 | 值 |
|------|-----|
| 元数据 | oa_douyin_video (V165) |
| 种子查询 | SEED-近30天内容列表 / id=9861 |
| 种子大屏 | 内部运营大屏 / id=98601 |
| 新建指标 | E2E_M6_1785146891381 / id=99247 |
| 新建查询 | E2E查询1785146891381 / id=9886 |
| 新建大屏 | id=— |

## 4. 用例结果
| TC-ID | TESTCASES | 描述 | 结果 | 截图 | 备注 |
|-------|-----------|------|------|------|------|
| M6-E2E-001 | V165 | oa_douyin_video 可读 | Pass | test-results/M6-E2E-001_metadata_entity_pass_1785146910952.png |  |
| M6-E2E-002 | TC-M6-001-01 | 新建 E2E_M6_1785146891381 | Pass | test-results/M6-E2E-002_metric_create_pass_1785146926699.png |  |
| M6-E2E-003 | TC-M6-001-01 | 指标分析完成 | Pass | test-results/M6-E2E-003_metric_analysis_pass_1785146941595.png |  |
| M6-E2E-004 | TC-M6-005-01/02 | oa_douyin_video 查询 78 行 + 保存 id=9886 | Pass | test-results/M6-E2E-004_custom_query_save_pass_1785146964278.png | seed9861 无行（依赖 oa_content） |
| M6-E2E-005 | TC-M6-007-01 | 大屏配置 | Fail | test-results/M6-E2E-005_screen_config_save_fail_1785147070583.png | TimeoutError: page.waitForResponse: Timeout 90000ms exceeded while waiting for event "response" |
| M6-E2E-006 | TC-M6-006-01 | 数据大屏 | Fail | test-results/M6-E2E-006_data_screen_fail_1785147079204.png | Error: [2mexpect([22m[31mreceived[39m[2m).[22mtoBeGreaterThan[2m([22m[32mexpected[39m[2m)[22m

Expected: > [32m0[39m
Received:   [31m0[39m |

## 5. 缺陷
- M6-E2E-005: docs/delivery/defects/DEF-20260727-*.md
- M6-E2E-006: docs/delivery/defects/DEF-20260727-*.md

## 6. 证据索引
- `test-results/M6-E2E-001_metadata_entity_pass_1785146910952.png` → `docs/delivery/e2e-artifacts/M6-E2E-20260727/M6-E2E-001_metadata_entity_pass_1785146910952.png`
- `test-results/M6-E2E-002_metric_create_pass_1785146926699.png` → `docs/delivery/e2e-artifacts/M6-E2E-20260727/M6-E2E-002_metric_create_pass_1785146926699.png`
- `test-results/M6-E2E-003_metric_analysis_pass_1785146941595.png` → `docs/delivery/e2e-artifacts/M6-E2E-20260727/M6-E2E-003_metric_analysis_pass_1785146941595.png`
- `test-results/M6-E2E-004_custom_query_save_pass_1785146964278.png` → `docs/delivery/e2e-artifacts/M6-E2E-20260727/M6-E2E-004_custom_query_save_pass_1785146964278.png`
- `test-results/M6-E2E-005_screen_config_save_fail_1785147070583.png` → `docs/delivery/e2e-artifacts/M6-E2E-20260727/M6-E2E-005_screen_config_save_fail_1785147070583.png`
- `test-results/M6-E2E-006_data_screen_fail_1785147079204.png` → `docs/delivery/e2e-artifacts/M6-E2E-20260727/M6-E2E-006_data_screen_fail_1785147079204.png`

## 7. 阻塞 / 风险
- TC-M6-006-03/04、TC-M6-007-02 未自动化
- Douyin 采集数据在 oa_douyin_video（oa_content=0）；E2E 数据源 oa_douyin_video
- seed 98601 / query 9861 仍依赖 oa_content，seed 项可能 Blocked

## 8. 签字
- E2E Agent: 20260727
