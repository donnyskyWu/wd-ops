# MP create RPC 500 fix — 2026-08-01

## Root cause

`POST /rpc-api/mp/accountInfo/create` → `MpAccountServiceImpl.createAccount`:

```java
if (createReqVO.getIsPrimary() == 1) { ... }  // NPE when isPrimary == null
if (createReqVO.getType() == 0) { ... }       // same for type
```

OPS Feign payload leaves `isPrimary`/`type` null (optional fields). Update path already used `ObjUtil.isNotEmpty` for `isPrimary`; create did not.

## Fix

Null-safe checks in `MpAccountServiceImpl` create (and `type` on update) — no new Spec fields.

## Smoke

| Step | Result |
|------|--------|
| Direct RPC create (no isPrimary/type) | ✅ id 1000009 |
| OPS create + realnameId=1 | ✅ id 1000008 |
| get/list realName | ✅ `zzw` |
