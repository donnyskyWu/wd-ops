from __future__ import annotations

import json
import urllib.error
import urllib.request
from pathlib import Path

GW = "http://127.0.0.1:48080"
OUT = Path(__file__).resolve().parent
OUT.mkdir(parents=True, exist_ok=True)


def req(method: str, url: str, token: str | None = None, body=None, tenant: str = "1"):
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode()
    headers = {
        "Content-Type": "application/json",
        "tenant-id": tenant,
        "X-Tenant-Id": tenant,
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=60) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        raw = e.read().decode(errors="replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}


def walk(nodes):
    for n in nodes or []:
        yield n
        yield from walk(n.get("children") or [])


def main() -> int:
    results: dict = {"steps": []}

    st, login = req(
        "POST",
        f"{GW}/admin-api/system/auth/login",
        body={"username": "opsleader", "password": "admin123"},
    )
    (OUT / "00-login.json").write_text(
        json.dumps({"http": st, "body": login}, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    token = (login.get("data") or {}).get("accessToken")
    uid = (login.get("data") or {}).get("userId")
    results["steps"].append({"login": {"http": st, "code": login.get("code"), "userId": uid}})
    print("LOGIN", st, login.get("code"), "userId", uid, "token", bool(token))
    if not token:
        return 1

    for path in (
        "/admin-api/ops/ip-group/my-led",
        "/admin-api/ops/ip-group/led-list",
        "/admin-api/ops/ip-group/tree",
    ):
        st, body = req("GET", f"{GW}{path}", token=token)
        name = path.rsplit("/", 1)[-1]
        (OUT / f"01-{name}.json").write_text(
            json.dumps({"http": st, "body": body}, ensure_ascii=False, indent=2), encoding="utf-8"
        )
        print(name, st, body.get("code") if isinstance(body, dict) else None)

    st, tree = req("GET", f"{GW}/admin-api/ops/ip-group/tree", token=token)
    led_id = None
    led_name = None
    candidates = []
    if isinstance(tree, dict) and tree.get("code") == 0:
        for n in walk(tree.get("data") or []):
            if n.get("groupType") == 2 and n.get("status") == 1:
                lid = n.get("leaderUserId") or n.get("leaderId")
                candidates.append((n.get("id"), n.get("groupName"), lid))
                if str(lid) in {str(uid), "9160"}:
                    led_id, led_name = n.get("id"), n.get("groupName")
    print("candidates", candidates[:8], "count", len(candidates))
    print("matched led", led_id, led_name)

    if led_id is None:
        st_a, login_a = req(
            "POST",
            f"{GW}/admin-api/system/auth/login",
            body={"username": "admin", "password": "admin123"},
        )
        token_a = (login_a.get("data") or {}).get("accessToken")
        st, tree_a = req("GET", f"{GW}/admin-api/ops/ip-group/tree", token=token_a)
        (OUT / "01-tree-admin.json").write_text(
            json.dumps({"http": st, "body": tree_a}, ensure_ascii=False, indent=2), encoding="utf-8"
        )
        parent_id = None
        for n in walk((tree_a.get("data") if isinstance(tree_a, dict) else None) or []):
            if n.get("groupType") == 2 and n.get("status") == 1:
                lid = n.get("leaderUserId") or n.get("leaderId")
                if str(lid) in {str(uid), "9160"}:
                    led_id, led_name = n.get("id"), n.get("groupName")
                    break
            if parent_id is None and n.get("groupType") == 1:
                parent_id = n.get("id")
        print("admin matched led", led_id, led_name)

        if led_id is None:
            body = {
                "groupName": "smoke-leader-member-0802",
                "groupType": 2,
                "parentId": parent_id,
                "leaderUserId": str(uid),
                "leaderId": str(uid),
                "status": 1,
                "sortOrder": 0,
                "level": "S",
                "remark": "ADR-066 smoke",
            }
            st, create_g = req(
                "POST", f"{GW}/admin-api/ops/ip-group/create", token=token_a, body=body
            )
            (OUT / "01-create-led-group.json").write_text(
                json.dumps({"http": st, "req": body, "body": create_g}, ensure_ascii=False, indent=2),
                encoding="utf-8",
            )
            print("CREATE_GROUP", st, create_g)
            if create_g.get("code") == 0:
                led_id = create_g.get("data")
                led_name = body["groupName"]

    results["ledIpGroupId"] = led_id
    results["ledIpGroupName"] = led_name
    if led_id is None:
        (OUT / "RESULTS.json").write_text(
            json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8"
        )
        print("FAIL no led group")
        return 2

    create_body = {
        "title": "ADR-066 opsleader content smoke",
        "contentType": "ARTICLE",
        "creatorUserId": int(uid) if str(uid).isdigit() else uid,
        "body": "smoke body for leader membership",
        "documentType": "PREHEAT_PREVIEW",
        "ipGroupId": int(led_id) if str(led_id).isdigit() else led_id,
        "competitionId": "e2e-adr066",
        "competitionName": "ADR066 smoke match",
    }
    st, create = req("POST", f"{GW}/admin-api/ops/content/create", token=token, body=create_body)
    (OUT / "02-create-content.json").write_text(
        json.dumps({"http": st, "req": create_body, "body": create}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print("CREATE_CONTENT", st, create)
    results["steps"].append(
        {
            "create": {
                "http": st,
                "code": create.get("code"),
                "msg": create.get("msg"),
                "id": create.get("data"),
            }
        }
    )
    results["pass"] = create.get("code") == 0
    (OUT / "RESULTS.json").write_text(
        json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print("PASS" if results["pass"] else "FAIL")
    return 0 if results["pass"] else 3


if __name__ == "__main__":
    raise SystemExit(main())
