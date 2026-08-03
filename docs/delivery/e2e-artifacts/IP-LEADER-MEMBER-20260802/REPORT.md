# ADR-066 IP leader as member — smoke 2026-08-02

| Step | Result |
|------|--------|
| Login opsleader | userId=9160 |
| Led IP group | 9022 |
| Create content on led group | PASS code=0 id=33 |
| Other group reject | {'code': 403, 'msg': '当前用户不属于所选 IP 组', 'data': None} |

Root fix: IpGroupAccessSupport.isMemberOfIpGroup includes 
esolveLedIpGroupIds.
