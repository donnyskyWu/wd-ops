# EXTERNAL-COLLECT-UNSTUB-20260802

**Status**: PASS · ADR-060 §5.3  
**Date**: 2026-08-02

## Smoke（Gateway `:48080`）

| 检查 | 结果 |
|------|------|
| GET list subType=account | code=0 total=4 |
| POST create DOUYIN account | code=0 id=41 msg=ok |
| GET list after | code=0 total=1 |
| GET keyword/list | code=0 total=5 |
| external-source create（对照 stub） | code=410（期望 410） |

## Notes

- 权限：`ops:config:external-collect:list`
- 跑批 / ExternalCollectorAdapter / unify-collector 竞品通道：仍 follow-up
