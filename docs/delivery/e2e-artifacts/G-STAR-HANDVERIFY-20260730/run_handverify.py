#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""G-* business hand-verify for OPS×Football Feign checklist (2026-07-30)."""
from __future__ import annotations

import json
import os
import socket
import time
import urllib.error
import urllib.request
from datetime import date, timedelta
from pathlib import Path

OUT = Path(__file__).resolve().parent
GW = "http://localhost:48080"
OA = "http://localhost:48094"
RESULTS: list[dict] = []


def req(method: str, url: str, headers=None, body=None, timeout=90):
    data = None
    if body is not None:
        data = body.encode("utf-8") if isinstance(body, str) else body
    r = urllib.request.Request(url, data=data, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(r, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", errors="replace")
    except Exception as e:
        return 0, str(e)


def jload(raw: str):
    if raw and raw.lstrip().startswith(("{", "[")):
        try:
            return json.loads(raw)
        except Exception:
            return {"_parse_error": True, "raw": raw[:2000]}
    return {"raw": (raw or "")[:2000]}


def save(name: str, obj):
    path = OUT / name
    if isinstance(obj, (dict, list)):
        path.write_text(json.dumps(obj, ensure_ascii=False, indent=2), encoding="utf-8")
    else:
        path.write_text(str(obj), encoding="utf-8")
    return path


def record(gid: str, result: str, feign: str, ops: str, notes: str):
    RESULTS.append(
        {
            "id": gid,
            "result": result,
            "feign": feign,
            "ops": ops,
            "notes": notes,
        }
    )
    print(f"[{result}] {gid}: {notes}")


def port_up(port: int) -> bool:
    s = socket.socket()
    s.settimeout(1)
    try:
        return s.connect_ex(("127.0.0.1", port)) == 0
    finally:
        s.close()


def main():
    ports = {p: port_up(p) for p in [5777, 48080, 48081, 48082, 48085, 48086, 48087, 48088, 48094]}
    save("00-ports.json", ports)
    print("PORTS", ports)
    if not ports.get(48080) or not ports.get(48094):
        record("ENV", "Fail", "-", "-", f"gateway/oa down: {ports}")
        save("00-results.json", RESULTS)
        return

    st, raw = req(
        "POST",
        f"{GW}/admin-api/system/auth/login",
        {"Content-Type": "application/json", "tenant-id": "1", "X-Tenant-Id": "1"},
        json.dumps({"username": "admin", "password": "admin123"}),
    )
    login = jload(raw)
    token = (login.get("data") or {}).get("accessToken")
    uid = (login.get("data") or {}).get("userId")
    save(
        "00-login.json",
        {"status": st, "code": login.get("code"), "userId": uid, "hasToken": bool(token)},
    )
    if not token:
        record("ENV", "Fail", "login", "-", f"login failed: {login}")
        save("00-results.json", RESULTS)
        return
    auth = {
        "Authorization": f"Bearer {token}",
        "tenant-id": "1",
        "X-Tenant-Id": "1",
        "Content-Type": "application/json",
    }
    rpc_h = {"tenant-id": "1", "Content-Type": "application/json"}

    # ========== G-SYS-01 ==========
    st, raw = req("GET", "http://127.0.0.1:48081/rpc-api/system/user/simple-list", rpc_h)
    rpc_sl = jload(raw)
    sl = rpc_sl.get("data") or []
    sl_n = len(sl) if isinstance(sl, list) else 0
    save(
        "G-SYS-01-rpc-simple-list.json",
        {"status": st, "code": rpc_sl.get("code"), "count": sl_n, "sample": sl[:3] if isinstance(sl, list) else rpc_sl},
    )

    st, raw = req("GET", f"{GW}/admin-api/oa/ip-group/member-candidates", auth)
    mc = jload(raw)
    mc_list = mc.get("data") or []
    mc_n = len(mc_list) if isinstance(mc_list, list) else 0
    save(
        "G-SYS-01-ops-member-candidates.json",
        {"status": st, "code": mc.get("code"), "count": mc_n, "sample": mc_list[:3] if isinstance(mc_list, list) else mc},
    )

    st, raw = req("GET", f"{GW}/admin-api/oa/ip-group/leader-candidates", auth)
    lc = jload(raw)
    lc_list = lc.get("data") or []
    lc_n = len(lc_list) if isinstance(lc_list, list) else 0
    save(
        "G-SYS-01-ops-leader-candidates.json",
        {"status": st, "code": lc.get("code"), "count": lc_n, "sample": lc_list[:3] if isinstance(lc_list, list) else lc},
    )

    ok = rpc_sl.get("code") == 0 and sl_n > 0 and mc.get("code") == 0 and mc_n > 0
    record(
        "G-SYS-01",
        "Pass" if ok else "Fail",
        f"GET :48081/.../simple-list → HTTP {st if st else '?'} code={rpc_sl.get('code')} n={sl_n}",
        f"member-candidates n={mc_n}; leader-candidates n={lc_n}",
        "UserSelect 候选有数据" if ok else "simple-list 或候选为空/失败",
    )

    # ========== G-SYS-02 ==========
    st, raw = req(
        "GET",
        f"http://127.0.0.1:48081/rpc-api/system/permission/has-any-roles?userId={uid}&roles=super_admin",
        rpc_h,
    )
    har = jload(raw)
    save("G-SYS-02-rpc-has-any-roles.json", {"status": st, "resp": har})

    st, raw = req("GET", f"http://127.0.0.1:48081/rpc-api/system/user/get?id={uid}", rpc_h)
    ug = jload(raw)
    save("G-SYS-02-rpc-user-get.json", {"status": st, "resp": ug})

    st, raw = req("GET", f"{GW}/admin-api/oa/ip-group/list?pageNum=1&pageSize=5", auth)
    igl = jload(raw)
    ig_items = ((igl.get("data") or {}).get("list") or []) if isinstance(igl.get("data"), dict) else []
    save(
        "G-SYS-02-ops-ip-group-list.json",
        {
            "status": st,
            "code": igl.get("code"),
            "total": (igl.get("data") or {}).get("total") if isinstance(igl.get("data"), dict) else None,
            "sample": ig_items[:2],
        },
    )

    ip_save_ok = False
    invalid_reject = False
    ip_note = "no groups"
    if ig_items:
        gid = ig_items[0].get("id")
        st, raw = req("GET", f"{GW}/admin-api/oa/ip-group/{gid}", auth)
        detail = jload(raw)
        d = detail.get("data") or {}
        leader = d.get("leaderId") or d.get("leaderUserId") or uid
        update_body = {
            "id": gid,
            "groupName": d.get("groupName") or "E2E-handverify",
            "leaderId": str(leader) if leader is not None else None,
            "leaderUserId": str(leader) if leader is not None else None,
            "status": d.get("status"),
            "sortOrder": d.get("sortOrder"),
            "level": d.get("level"),
            "remark": (d.get("remark") or "") + "",
            "parentId": d.get("parentId"),
        }
        st, raw = req(
            "PUT",
            f"{GW}/admin-api/oa/ip-group/update",
            auth,
            json.dumps(update_body, ensure_ascii=False),
        )
        upd = jload(raw)
        save("G-SYS-02-ops-ip-group-update.json", {"status": st, "req": update_body, "resp": upd})
        ip_save_ok = upd.get("code") == 0
        ip_note = f"update id={gid} code={upd.get('code')} msg={upd.get('msg')}"

        bad_body = dict(update_body)
        bad_body["leaderId"] = "999999999999999"
        bad_body["leaderUserId"] = "999999999999999"
        st, raw = req(
            "PUT",
            f"{GW}/admin-api/oa/ip-group/update",
            auth,
            json.dumps(bad_body),
        )
        bad = jload(raw)
        save("G-SYS-02-ops-invalid-leader.json", {"status": st, "resp": bad})
        invalid_reject = bad.get("code") not in (0, None)
        ip_note += f"; invalidLeader code={bad.get('code')} msg={bad.get('msg')}"

    ok = ip_save_ok and har.get("code") == 0
    record(
        "G-SYS-02",
        "Pass" if ok else "Fail",
        f"has-any-roles code={har.get('code')} data={har.get('data')}; user/get code={ug.get('code')}",
        ip_note,
        "IP 组保存 + 非法 leader 拒绝" if ok and invalid_reject else ("IP 组保存 OK，非法 leader 未拒绝" if ok else "保存失败"),
    )

    # ========== G-DICT-01 ==========
    st, raw = req(
        "GET",
        "http://127.0.0.1:48081/rpc-api/system/dict-data/list?dictType=dict_ip_group_level",
        rpc_h,
    )
    dl = jload(raw)
    dlist = dl.get("data") or []
    save(
        "G-DICT-01-rpc-dict-list.json",
        {
            "status": st,
            "code": dl.get("code"),
            "count": len(dlist) if isinstance(dlist, list) else None,
            "sample": dlist[:5] if isinstance(dlist, list) else dl,
        },
    )

    # illegal enum via IP group level @InDict → expect 1503
    illegal_code = None
    if ig_items:
        gid = ig_items[0].get("id")
        st, raw = req("GET", f"{GW}/admin-api/oa/ip-group/{gid}", auth)
        d = (jload(raw).get("data") or {})
        leader = d.get("leaderId") or d.get("leaderUserId") or uid
        illegal_body = {
            "id": gid,
            "groupName": d.get("groupName") or "E2E",
            "leaderId": str(leader) if leader is not None else None,
            "level": "NOT_A_VALID_LEVEL_XYZ",
            "status": d.get("status"),
        }
        st, raw = req(
            "PUT",
            f"{GW}/admin-api/oa/ip-group/update",
            auth,
            json.dumps(illegal_body),
        )
        ill = jload(raw)
        illegal_code = ill.get("code")
        save("G-DICT-01-ops-illegal-level.json", {"status": st, "req": illegal_body, "resp": ill})

    # also content create illegal contentType
    body_ill = {
        "title": f"DICT-ILL-{int(time.time())}",
        "body": "x",
        "paidBody": "x",
        "freeBody": "y",
        "contentType": "NOT_A_VALID_CONTENT_TYPE",
        "documentType": "ARTICLE",
        "ipGroupId": 9003,
        "authorId": 107156,
        "competitionId": "e2e-dict",
        "competitionName": "E2E",
        "aiGenerated": 0,
    }
    st, raw = req(
        "POST",
        f"{GW}/admin-api/oa/content/create",
        auth,
        json.dumps(body_ill, ensure_ascii=False),
    )
    cill = jload(raw)
    save("G-DICT-01-ops-illegal-contentType.json", {"status": st, "resp": cill})
    if illegal_code != 1503 and cill.get("code") == 1503:
        illegal_code = 1503

    ok = dl.get("code") == 0 and illegal_code == 1503
    record(
        "G-DICT-01",
        "Pass" if ok else ("Fail" if illegal_code not in (None,) else "blocked"),
        f"dict-data/list dict_ip_group_level code={dl.get('code')} n={len(dlist) if isinstance(dlist, list) else '?'}",
        f"illegal enum → code={illegal_code}; contentType illegal → {cill.get('code')}",
        "非法枚举返回 1503" if ok else f"期望 1503，实际 level={illegal_code} contentType={cill.get('code')}",
    )

    # ========== G-INF-01 ==========
    st, raw = req(
        "GET",
        "http://127.0.0.1:48082/rpc-api/infra/file/presigned-url?path=handverify-test.png",
        rpc_h,
    )
    pre = jload(raw)
    save("G-INF-01-rpc-presigned.json", {"status": st, "resp": pre})

    # multipart upload via OPS
    boundary = "----HandVerifyBoundary7MA4YWxkTrZu0gW"
    png_bytes = (
        b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01"
        b"\x08\x02\x00\x00\x00\x90wS\xde\x00\x00\x00\x0cIDATx\x9cc\xf8\x0f\x00"
        b"\x00\x01\x01\x00\x05\x18\xd8N\x00\x00\x00\x00IEND\xaeB`\x82"
    )
    body_parts = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="handverify.png"\r\n'
        f"Content-Type: image/png\r\n\r\n"
    ).encode("utf-8") + png_bytes + f"\r\n--{boundary}--\r\n".encode("utf-8")
    up_headers = {
        "Authorization": f"Bearer {token}",
        "tenant-id": "1",
        "X-Tenant-Id": "1",
        "Content-Type": f"multipart/form-data; boundary={boundary}",
    }
    st, raw = req("POST", f"{GW}/admin-api/oa/file/upload", up_headers, body_parts, timeout=120)
    up = jload(raw)
    save("G-INF-01-ops-upload.json", {"status": st, "resp": up})
    url = None
    if isinstance(up.get("data"), dict):
        url = up["data"].get("url") or up["data"].get("fileUrl") or up["data"].get("path")
    elif isinstance(up.get("data"), str):
        url = up.get("data")
    infra_ish = bool(url) and (
        "infra" in str(url).lower()
        or "48082" in str(url)
        or "/admin-api/infra/" in str(url)
        or str(url).startswith("http")
    )
    # Feign upload success: code=0 and URL present (infra domain preferred)
    ok = ports.get(48082) and up.get("code") == 0 and bool(url)
    note = f"upload code={up.get('code')} url={url}; rpc_presigned code={pre.get('code')}; infra_ish={infra_ish}"
    if ok and not infra_ish:
        note += " (URL 非明显 infra 域，记 Pass 但备注)"
    record(
        "G-INF-01",
        "Pass" if ok else ("blocked" if not ports.get(48082) else "Fail"),
        f"presigned-url HTTP status→code={pre.get('code')}",
        note,
        "内容配图上传成功" if ok else "上传失败或 infra DOWN",
    )

    # ========== G-MEM-03 ==========
    title = f"G-MEM-03-handverify-{int(time.time())}"
    body = {
        "title": title,
        "body": "<p>paid handverify</p>",
        "paidBody": "<p>paid handverify</p>",
        "freeBody": "<p>free handverify</p>",
        "contentType": "ARTICLE",
        "documentType": "ARTICLE",
        "ipGroupId": 9003,
        "authorId": 107156,
        "competitionId": "e2e-gmem03",
        "competitionName": "E2E G-MEM-03",
        "aiGenerated": 0,
    }
    st, raw = req(
        "POST",
        f"{GW}/admin-api/oa/content/create",
        auth,
        json.dumps(body, ensure_ascii=False),
    )
    created = jload(raw)
    save("G-MEM-03-ops-create.json", {"status": st, "req": body, "resp": created})
    cid = created.get("data")
    scheme = {}
    art_id = None
    sync_err = "n/a"
    if isinstance(cid, int):
        st, raw = req("GET", f"{GW}/admin-api/oa/content/{cid}/football-scheme", auth)
        scheme_resp = jload(raw)
        scheme = scheme_resp.get("data") or {}
        save("G-MEM-03-ops-football-scheme.json", {"status": st, "resp": scheme_resp})
        art_id = scheme.get("authorArticleId")
        sync_err = scheme.get("footballSyncError")
        if art_id is None and scheme.get("id"):
            # maybe nested
            pass
        # also try sync endpoint if not synced
        if not art_id:
            st, raw = req(
                "POST",
                f"{GW}/admin-api/oa/content/{cid}/sync-football-scheme",
                auth,
                "{}",
            )
            sync_resp = jload(raw)
            save("G-MEM-03-ops-sync.json", {"status": st, "resp": sync_resp})
            st, raw = req("GET", f"{GW}/admin-api/oa/content/{cid}/football-scheme", auth)
            scheme_resp = jload(raw)
            scheme = scheme_resp.get("data") or {}
            save("G-MEM-03-ops-football-scheme-after-sync.json", {"status": st, "resp": scheme_resp})
            art_id = scheme.get("authorArticleId")
            sync_err = scheme.get("footballSyncError")

    # RPC smoke (empty may still 200)
    st, raw = req(
        "POST",
        "http://127.0.0.1:48087/rpc-api/member/article/create",
        rpc_h,
        "{}",
    )
    rpc_art = jload(raw)
    save("G-MEM-03-rpc-article-create-empty.json", {"status": st, "resp": rpc_art})

    ok = isinstance(cid, int) and art_id and not sync_err
    record(
        "G-MEM-03",
        "Pass" if ok else "Fail",
        f"member :48087 UP={ports.get(48087)}; empty-create code={rpc_art.get('code')}",
        f"contentId={cid} authorArticleId={art_id} footballSyncError={sync_err}",
        "内容→方案同步成功" if ok else f"同步失败 sync_err={sync_err}",
    )

    # ========== G-MP-01 ==========
    st, raw = req(
        "GET",
        "http://127.0.0.1:48086/rpc-api/mp/accountInfo/page?pageNo=1&pageSize=10",
        rpc_h,
    )
    mp_rpc = jload(raw)
    mp_total = ((mp_rpc.get("data") or {}).get("total") if isinstance(mp_rpc.get("data"), dict) else None)
    save("G-MP-01-rpc-accountInfo-page.json", {"status": st, "code": mp_rpc.get("code"), "total": mp_total, "resp": mp_rpc})

    st, raw = req(
        "GET",
        f"{GW}/admin-api/oa/account/list?pageNum=1&pageSize=10&platformType=WECHAT",
        auth,
    )
    acc = jload(raw)
    # try alternate query params if needed
    if acc.get("code") not in (0, None) or acc.get("data") is None:
        st2, raw2 = req("GET", f"{GW}/admin-api/oa/account/page?pageNo=1&pageSize=10", auth)
        acc2 = jload(raw2)
        if acc2.get("code") == 0:
            acc = acc2
            st = st2
    save("G-MP-01-ops-account-list.json", {"status": st, "resp": acc})

    # followers smoke
    st, raw = req(
        "GET",
        "http://127.0.0.1:48086/rpc-api/mp/mpUser/getUserPageByAccount?accountId=1000002&pageNo=1&pageSize=10",
        rpc_h,
    )
    foll_rpc = jload(raw)
    save("G-MP-01-rpc-followers.json", {"status": st, "resp": foll_rpc})

    st, raw = req(
        "GET",
        f"{GW}/admin-api/oa/account/1000002/mp-followers?pageNo=1&pageSize=10",
        auth,
    )
    foll_ops = jload(raw)
    save("G-MP-01-ops-followers.json", {"status": st, "resp": foll_ops})

    acc_ok = acc.get("code") == 0
    foll_ok = foll_ops.get("code") == 0 or (
        isinstance(foll_ops.get("data"), dict) and (foll_ops.get("data") or {}).get("total") is not None
    )
    # also accept RPC page OK + OPS account list OK
    ok = ports.get(48086) and mp_rpc.get("code") == 0 and acc_ok
    record(
        "G-MP-01",
        "Pass" if ok else "Fail",
        f"accountInfo/page code={mp_rpc.get('code')} total={mp_total}; followers RPC code={foll_rpc.get('code')}",
        f"account list code={acc.get('code')}; mp-followers code={foll_ops.get('code')} total={(foll_ops.get('data') or {}).get('total') if isinstance(foll_ops.get('data'), dict) else None}",
        "微信账号列表 + 粉丝抽检" if ok and foll_ok else ("账号列表 OK，粉丝抽检异常" if ok else "账号列表失败"),
    )

    # ========== G-PAY-01 ==========
    st, raw = req(
        "POST",
        "http://127.0.0.1:48085/rpc-api/pay/order/page",
        rpc_h,
        json.dumps({"pageNo": 1, "pageSize": 10}),
    )
    pay_rpc = jload(raw)
    save("G-PAY-01-rpc-order-page.json", {"status": st, "resp": pay_rpc})

    end = date.today()
    start = end - timedelta(days=30)
    st, raw = req(
        "GET",
        f"{GW}/admin-api/oa/football-order/list?startDate={start.isoformat()}&endDate={end.isoformat()}&pageNum=1&pageSize=10",
        auth,
    )
    pay_ops = jload(raw)
    save("G-PAY-01-ops-football-order-list.json", {"status": st, "resp": pay_ops})

    rpc_ok = pay_rpc.get("code") == 0
    ops_ok = pay_ops.get("code") == 0
    if not ports.get(48085):
        record("G-PAY-01", "blocked", ":48085 DOWN", "-", "pay-server 未启动")
    elif rpc_ok and ops_ok:
        total = (pay_ops.get("data") or {}).get("total") if isinstance(pay_ops.get("data"), dict) else None
        record(
            "G-PAY-01",
            "Pass",
            f"POST :48085/.../order/page code=0",
            f"football-order/list code=0 total={total}",
            "订单 RPC + OPS 列表通",
        )
    elif rpc_ok and pay_ops.get("code") == 403:
        record(
            "G-PAY-01",
            "Fail",
            "POST :48085/.../order/page code=0",
            f"football-order/list code=403 msg={pay_ops.get('msg')}",
            "RPC OK；OPS 缺 oa:order-attribution:list / oa:roi:list — 需 role bridge",
        )
    else:
        record(
            "G-PAY-01",
            "Fail",
            f"order/page code={pay_rpc.get('code')} msg={pay_rpc.get('msg')}",
            f"football-order/list code={pay_ops.get('code')} msg={pay_ops.get('msg')}",
            "pay RPC 或 OPS 列表失败",
        )

    save("00-results.json", RESULTS)
    print("\n=== MATRIX ===")
    for r in RESULTS:
        print(f"{r['id']}: {r['result']} | {r['notes']}")


if __name__ == "__main__":
    main()
