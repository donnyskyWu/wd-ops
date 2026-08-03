import json
import urllib.error
import urllib.request

GW = "http://127.0.0.1:48080"


def req(method, url, headers=None, data=None):
    h = dict(headers or {})
    body = None
    if data is not None:
        body = data.encode() if isinstance(data, str) else data
        h.setdefault("Content-Type", "application/json")
    r = urllib.request.Request(url, data=body, headers=h, method=method)
    try:
        with urllib.request.urlopen(r, timeout=30) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


def main():
    st, raw = req(
        "POST",
        f"{GW}/admin-api/system/auth/login",
        {"tenant-id": "1", "X-Tenant-Id": "1"},
        json.dumps({"username": "admin", "password": "admin123"}),
    )
    login = json.loads(raw)
    token = (login.get("data") or {}).get("accessToken")
    print("login", st, login.get("code"), "token", bool(token))
    h = {
        "Authorization": f"Bearer {token}",
        "tenant-id": "1",
        "X-Tenant-Id": "1",
    }
    for label, path in [
        ("OPS tree", "/admin-api/ops/ip-group/tree"),
        ("OPS content", "/admin-api/ops/content/list?pageNo=1&pageSize=1"),
        ("OA tree", "/admin-api/oa/ip-group/tree"),
        ("OA content", "/admin-api/oa/content/list?pageNo=1&pageSize=1"),
    ]:
        st, raw = req("GET", GW + path, h)
        try:
            j = json.loads(raw)
            msg = str(j.get("msg"))[:100]
            print(f"{label} HTTP={st} code={j.get('code')} msg={msg}")
        except Exception:
            print(f"{label} HTTP={st} body={raw[:160]}")

    st, raw = req("GET", "http://127.0.0.1:48094/actuator/health")
    print("ops-server", st, raw)


if __name__ == "__main__":
    main()
