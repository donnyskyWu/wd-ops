# -*- coding: utf-8 -*-
"""Smoke: collector flat routes + unified task run after ProbeClient path fix."""
import json
import re
import time
import urllib.error
import urllib.request
from pathlib import Path

ART = Path(__file__).resolve().parent
ART.mkdir(parents=True, exist_ok=True)
OPS = "http://127.0.0.1:48080"
COLLECTOR = "http://127.0.0.1:8000"
TOKEN_C = "test-key-2026"


def http(method, url, headers=None, data=None, timeout=180):
    h = dict(headers or {})
    body = None
    if data is not None:
        body = data if isinstance(data, (bytes, bytearray)) else json.dumps(data).encode()
        h.setdefault("Content-Type", "application/json")
    req = urllib.request.Request(url, data=body, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except Exception as e:
        return 0, str(e)


def main():
    results = {"path_probes": [], "task": {}, "notes": []}

    acc_mp = "acc_wechat_mp_22556c49301482a3"
    acc_dy = "acc_douyin_7631a3ce2bc50d6b"
    auth = {"Authorization": f"Bearer {TOKEN_C}"}
    probes = [
        ("NESTED_MP_BAD", f"{COLLECTOR}/api/v1/internal/wechat-mp/{acc_mp}/followers/stats"),
        ("FLAT_MP_FOLLOWER", f"{COLLECTOR}/api/v1/internal/wechat-mp/follower-stats?account_id={acc_mp}"),
        ("FLAT_MP_PUBLISH", f"{COLLECTOR}/api/v1/internal/wechat-mp/publish-list?account_id={acc_mp}&begin=0&end=1"),
        ("NESTED_DY_BAD", f"{COLLECTOR}/api/v1/internal/douyin/{acc_dy}/followers/stats"),
        ("FLAT_DY_FOLLOWER", f"{COLLECTOR}/api/v1/internal/douyin/follower-stats?account_id={acc_dy}"),
        ("FLAT_DY_VIDEO", f"{COLLECTOR}/api/v1/internal/douyin/video-list?account_id={acc_dy}&cursor=0&page_size=1"),
        ("NESTED_DY_VIDEO_BAD", f"{COLLECTOR}/api/v1/internal/douyin/{acc_dy}/videos?cursor=0&count=1"),
    ]
    for name, url in probes:
        st, body = http("GET", url, auth, timeout=60)
        path404 = st == 404 and ("Not Found" in body or '"detail"' in body)
        try:
            j = json.loads(body)
            code = j.get("code", j.get("detail"))
            msg = j.get("message") or j.get("detail") or ""
            biz_ok = st == 200 and j.get("code") == 0
        except Exception:
            code, msg, biz_ok = None, body[:120], False
        entry = {
            "name": name,
            "http": st,
            "code": code,
            "msg": str(msg)[:120],
            "path404_fastapi": path404,
            "ok": biz_ok,
        }
        results["path_probes"].append(entry)
        (ART / f"probe-{name}.json").write_text(body, encoding="utf-8")
        print(f"{name}: http={st} path404={path404} ok={biz_ok} msg={str(msg)[:80]}")

    st, login_raw = http(
        "POST",
        f"{OPS}/admin-api/system/auth/login",
        {"tenant-id": "1", "X-Tenant-Id": "1"},
        {"username": "admin", "password": "admin123"},
    )
    (ART / "00-login.json").write_text(login_raw, encoding="utf-8")
    login = json.loads(login_raw)
    token = (login.get("data") or {}).get("accessToken")
    if not token:
        raise SystemExit(f"login failed: {login_raw[:300]}")
    h = {"Authorization": f"Bearer {token}", "tenant-id": "1", "X-Tenant-Id": "1"}
    print("LOGIN OK")

    st, ensure_raw = http("POST", f"{OPS}/admin-api/ops/collect/task/ensure-unified", h)
    (ART / "ensure-unified.json").write_text(ensure_raw, encoding="utf-8")
    ensure = json.loads(ensure_raw)
    task_id = (ensure.get("data") or {}).get("id")
    print("ENSURE", ensure.get("code"), "taskId", task_id)
    if not task_id:
        raise SystemExit(f"ensure failed: {ensure_raw[:400]}")

    st, run_raw = http(
        "POST", f"{OPS}/admin-api/ops/collect/task/{task_id}/run", h, timeout=300
    )
    (ART / "task-run.json").write_text(run_raw, encoding="utf-8")
    print("RUN", st, run_raw[:300])
    results["task"]["run"] = json.loads(run_raw) if run_raw.startswith("{") else {"raw": run_raw}

    time.sleep(3)
    st, logs_raw = http(
        "GET",
        f"{OPS}/admin-api/ops/collect/log/page?pageNo=1&pageSize=3&taskId={task_id}",
        h,
    )
    (ART / "log-page.json").write_text(logs_raw, encoding="utf-8")
    logs = json.loads(logs_raw)
    items = ((logs.get("data") or {}).get("list")) or []
    print("LOGS count", len(items), "total", (logs.get("data") or {}).get("total"))

    path_mismatch = []
    relogin = []
    other404 = []
    nested_pat = re.compile(
        r"/api/v1/internal/(?:wechat-mp|douyin|kuaishou|wechat-channels|xiaohongshu)/"
        r"[^?/]+/(?:followers|articles|videos|notes)"
    )

    for item in items[:1]:
        detail_id = item.get("id")
        st, det_raw = http("GET", f"{OPS}/admin-api/ops/collect/log/get?id={detail_id}", h)
        (ART / "log-detail.json").write_text(det_raw, encoding="utf-8")
        det = json.loads(det_raw)
        data = det.get("data") or {}
        result_json = data.get("resultJson") or data.get("result_json") or ""
        blob = json.dumps(data, ensure_ascii=False)
        (ART / "log-detail-blob.txt").write_text(blob[:30000], encoding="utf-8")
        results["task"]["logStatus"] = data.get("status") or data.get("collectStatus")
        results["task"]["logId"] = detail_id

        rj = None
        if isinstance(result_json, str) and result_json.strip().startswith("{"):
            try:
                rj = json.loads(result_json)
            except Exception:
                rj = None
        elif isinstance(result_json, dict):
            rj = result_json
        if rj is not None:
            (ART / "result-json.json").write_text(
                json.dumps(rj, ensure_ascii=False, indent=2), encoding="utf-8"
            )
            results["task"]["resultJsonPreview"] = json.dumps(rj, ensure_ascii=False)[:2000]

        text = blob
        for m in re.finditer(r"collector HTTP 404[^\"]*", text):
            msg = m.group(0)
            if nested_pat.search(msg) or "Not Found" in msg:
                path_mismatch.append(msg[:240])
            else:
                other404.append(msg[:240])
        for m in re.finditer(r"Collector 账号需重新登录或凭证失效: [^\"]+", text):
            relogin.append(m.group(0))

        def walk(o):
            if isinstance(o, dict):
                for v in o.values():
                    walk(v)
            elif isinstance(o, list):
                for v in o:
                    walk(v)
            elif isinstance(o, str):
                if "HTTP 404" in o and (nested_pat.search(o) or "/followers/stats" in o):
                    # flat follower-stats biz 404 is OK to note separately
                    if nested_pat.search(o):
                        path_mismatch.append(o[:240])
                    elif re.search(r"/[a-z0-9_]+/followers/stats", o):
                        path_mismatch.append(o[:240])
                if "需重新登录" in o or "凭证失效" in o:
                    relogin.append(o[:240])

        if rj is not None:
            walk(rj)

    results["path_mismatch_remaining"] = sorted(set(path_mismatch))
    results["relogin_needed"] = sorted(set(relogin))
    results["other_http404"] = other404[:20]
    results["verdict"] = {
        "nested_path_still_404_in_logs": len(results["path_mismatch_remaining"]) > 0,
        "flat_collector_probes_ok": all(
            p["ok"] for p in results["path_probes"] if p["name"].startswith("FLAT_")
        ),
        "nested_collector_probes_path404": all(
            p["path404_fastapi"]
            for p in results["path_probes"]
            if p["name"].startswith("NESTED_")
        ),
    }
    results["notes"].append(
        "WECHAT_VIDEO relogin_needed is credential/status (alive=false), not path mismatch."
    )
    (ART / "RESULTS.json").write_text(
        json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print("VERDICT", json.dumps(results["verdict"], ensure_ascii=False))
    print("path_mismatch", results["path_mismatch_remaining"][:5])
    print("relogin", results["relogin_needed"][:5])
    print("other404", other404[:5])


if __name__ == "__main__":
    main()
