# -*- coding: utf-8 -*-
"""Smoke: external-collect list/create — must not be 410 stub."""
import json
import urllib.error
import urllib.request
from pathlib import Path

GW = "http://127.0.0.1:48080"
DIRECT = "http://127.0.0.1:48094"
OUT = Path(__file__).resolve().parent


def req(method, url, token=None, body=None):
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode()
    h = {
        "Content-Type": "application/json",
        "tenant-id": "1",
        "X-Tenant-Id": "1",
    }
    if token:
        h["Authorization"] = f"Bearer {token}"
    r = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(r, timeout=60) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        raw = e.read().decode(errors="replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}


def dump(name, obj):
    (OUT / name).write_text(json.dumps(obj, ensure_ascii=False, indent=2), encoding="utf-8")


def main():
    st, login = req(
        "POST",
        f"{GW}/admin-api/system/auth/login",
        body={"username": "admin", "password": "admin123"},
    )
    dump("00-login.json", {"http": st, "body": login})
    token = (login.get("data") or {}).get("accessToken")
    assert token, f"login failed: {login}"

    # list before
    st, before = req(
        "GET",
        f"{GW}/admin-api/ops/config/external-collect/list?pageNo=1&pageSize=10&subType=account",
        token=token,
    )
    dump("01-list-before.json", {"http": st, "body": before})
    print("LIST_BEFORE", st, before.get("code"), before.get("msg"), "total", (before.get("data") or {}).get("total"))

    # create
    body = {
        "subType": "account",
        "platformType": "DOUYIN",
        "configName": "smoke-ext-account-20260802",
        "accountIdentifier": "smoke_dy_uid_20260802",
        "status": "ENABLED",
    }
    st, create = req(
        "POST",
        f"{GW}/admin-api/ops/config/external-collect/create",
        token=token,
        body=body,
    )
    dump("02-create.json", {"http": st, "req": body, "body": create})
    print("CREATE", st, create.get("code"), create.get("msg"), "id", create.get("data"))

    created_id = create.get("data")

    # list after
    st, after = req(
        "GET",
        f"{GW}/admin-api/ops/config/external-collect/list?pageNo=1&pageSize=10&subType=account&configName=smoke-ext-account",
        token=token,
    )
    dump("03-list-after.json", {"http": st, "body": after})
    print("LIST_AFTER", st, after.get("code"), "total", (after.get("data") or {}).get("total"))

    # keyword list (smoke read)
    st, kw = req(
        "GET",
        f"{GW}/admin-api/ops/config/external-collect/keyword/list?pageNo=1&pageSize=5",
        token=token,
    )
    dump("04-keyword-list.json", {"http": st, "body": kw})
    print("KEYWORD_LIST", st, kw.get("code"), "total", (kw.get("data") or {}).get("total"))

    # control: still-stubbed external-source write should 410
    st, stub = req(
        "POST",
        f"{GW}/admin-api/ops/config/external-source/create",
        token=token,
        body={"configName": "should-stub", "status": "ENABLED"},
    )
    dump("05-external-source-still-stub.json", {"http": st, "body": stub})
    print("STUB_CONTROL", st, stub.get("code"), stub.get("msg"))

    # cleanup created (best-effort)
    if created_id:
        st, deleted = req(
            "DELETE",
            f"{GW}/admin-api/ops/config/external-collect/delete?id={created_id}",
            token=token,
        )
        dump("06-delete.json", {"http": st, "body": deleted})
        print("DELETE", st, deleted.get("code"), deleted.get("msg"))

    create_ok = create.get("code") == 0 and created_id is not None
    list_ok = before.get("code") == 0 and after.get("code") == 0
    not_stub = "deferred" not in str(create.get("msg", "")).lower() and create.get("code") != 410
    stub_still = stub.get("code") == 410 or "deferred" in str(stub.get("msg", "")).lower()

    results = {
        "pass": bool(create_ok and list_ok and not_stub),
        "createOk": create_ok,
        "listOk": list_ok,
        "notStub410": not_stub,
        "externalSourceStillStub": stub_still,
        "createdId": created_id,
        "listBeforeTotal": (before.get("data") or {}).get("total"),
        "listAfterTotal": (after.get("data") or {}).get("total"),
        "keywordTotal": (kw.get("data") or {}).get("total"),
    }
    dump("RESULTS.json", results)
    report = f"""# EXTERNAL-COLLECT-UNSTUB-20260802

**Status**: {"PASS" if results["pass"] else "FAIL"} · ADR-060 §5.3  
**Date**: 2026-08-02

## Smoke（Gateway `:48080`）

| 检查 | 结果 |
|------|------|
| GET list subType=account | code={before.get("code")} total={results["listBeforeTotal"]} |
| POST create DOUYIN account | code={create.get("code")} id={created_id} msg={create.get("msg")} |
| GET list after | code={after.get("code")} total={results["listAfterTotal"]} |
| GET keyword/list | code={kw.get("code")} total={results["keywordTotal"]} |
| external-source create（对照 stub） | code={stub.get("code")}（期望 410） |

## Notes

- 权限：`ops:config:external-collect:list`
- 跑批 / ExternalCollectorAdapter / unify-collector 竞品通道：仍 follow-up
"""
    (OUT / "REPORT.md").write_text(report, encoding="utf-8")
    print(json.dumps(results, ensure_ascii=False, indent=2))
    raise SystemExit(0 if results["pass"] else 1)


if __name__ == "__main__":
    main()
