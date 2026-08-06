# V172 Table Drop — OPS API Regression

- Date: 2026-08-03
- Beta: 110.42.49.224 / shenyu-ops
- Verdict: **PASS**

## V172 apply

- Flyway V172 row: `172	1`
- Metadata oa_author/oa_demo_item rows: 0
- Drop targets still present: none

## API matrix

| Module | Check | HTTP | Code | Pass |
|--------|-------|------|------|------|
| infra | ops-health | 200 | ? | ✅ |
| account | account-list | 200 | 0 | ✅ |
| ip-group | ip-group-tree | 200 | 0 | ✅ |
| ip-group | ip-group-accessible-tree | 200 | 0 | ✅ |
| author | author-list | 200 | 0 | ✅ |
| plan | plan-list | 200 | 0 | ✅ |
| task | task-list | 200 | 0 | ✅ |
| content | content-list | 200 | 0 | ✅ |
| content | internal-content-list | 200 | 0 | ✅ |
| review | productivity-review-list | 200 | 0 | ✅ |
| system-param | system-param-list | 200 | 0 | ✅ |
| dict | dict-content-type | 200 | 0 | ✅ |
| dict | dict-platform-type | 200 | 0 | ✅ |
| metadata | metadata-list | 200 | 0 | ✅ |
| analytics | content-analysis-stats | 200 | 0 | ✅ |
| analytics | football-order-list | 200 | 0 | ✅ |
| system | system-user-profile | 200 | 0 | ✅ |
| collect | collect-task-page | 200 | 0 | ✅ |
| collect | collect-ensure-unified | 200 | 0 | ✅ |
| collect | collect-log-page | 200 | 0 | ✅ |

Summary: {'v172_applied': True, 'api_pass': '20/20', 'modules': ['account', 'analytics', 'author', 'collect', 'content', 'dict', 'infra', 'ip-group', 'metadata', 'plan', 'review', 'system', 'system-param', 'task']}
