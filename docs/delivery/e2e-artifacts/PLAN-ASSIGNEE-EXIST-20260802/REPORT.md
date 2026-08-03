# Plan assignee「执行人不存在」fix — smoke 2026-08-02

| Step | Result |
|------|--------|
| Login opsleader | userId=9160 |
| Preview assignees | [('9163', '内容编辑测试', False), ('9163', '内容编辑测试', False), ('9160', 'IP组长测试', True)] |
| Save draft (preview 9163/9160 + schedule strings) | {'code': 0, 'msg': 'ok', 'data': 17} |
| Save draft (张武 snowflake string) | {'code': 0, 'msg': 'ok', 'data': 18} |

**Verdict**: PASS
