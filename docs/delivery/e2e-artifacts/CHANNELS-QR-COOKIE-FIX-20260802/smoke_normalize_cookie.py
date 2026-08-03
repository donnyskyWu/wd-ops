#!/usr/bin/env python3
"""Smoke: mirror CollectorQrLoginSupport.normalizeCookieFromCredential for WECHAT_CHANNELS shapes.

No human scan — validates field mapping for poll.confirmed credential payloads.
"""
from __future__ import annotations


def normalize_cookie_from_credential(credential: dict | None) -> str | None:
    if not credential:
        return None

    def join_map(m: dict) -> str | None:
        parts = []
        for k, v in m.items():
            if k is None or v is None:
                continue
            name, val = str(k).strip(), str(v).strip()
            if not name or not val or val.lower() == "null":
                continue
            parts.append(f"{name}={val}")
        return "; ".join(parts) if parts else None

    def cookie_value_to_string(value) -> str | None:
        if value is None:
            return None
        if isinstance(value, dict):
            return join_map(value)
        text = str(value).strip()
        if not text or text.lower() == "null" or text == "{}":
            return None
        return text

    for key in ("cookie", "cookie_str"):
        got = cookie_value_to_string(credential.get(key))
        if got:
            return got
    cookies = credential.get("cookies")
    if isinstance(cookies, dict):
        got = join_map(cookies)
        if got:
            return got
    sessionid = credential.get("sessionid")
    wxuin = credential.get("wxuin")
    parts = {}
    if sessionid and str(sessionid).strip() and str(sessionid).lower() != "null":
        parts["sessionid"] = str(sessionid).strip()
    if wxuin and str(wxuin).strip() and str(wxuin).lower() != "null":
        parts["wxuin"] = str(wxuin).strip()
    return join_map(parts) if parts else None


CASES = [
    (
        "string_cookie",
        {"cookie": "sessionid=abc; wxuin=123"},
        "sessionid=abc; wxuin=123",
    ),
    (
        "dict_cookie",
        {"cookie": {"sessionid": "abc", "wxuin": "123"}},
        "sessionid=abc; wxuin=123",
    ),
    (
        "top_level_sessionid_wxuin",
        {"sessionid": "abc", "wxuin": "123"},
        "sessionid=abc; wxuin=123",
    ),
    (
        "cookies_map",
        {"cookies": {"sessionid": "abc", "wxuin": "123"}},
        "sessionid=abc; wxuin=123",
    ),
    (
        "empty_cookie_rejected",
        {"cookie": ""},
        None,
    ),
    (
        "mp_style_token_ignored_for_cookie",
        {"token": "mp_only"},
        None,
    ),
]


def main() -> int:
    failed = 0
    for name, cred, expected in CASES:
        got = normalize_cookie_from_credential(cred)
        ok = got == expected
        print(f"{'PASS' if ok else 'FAIL'} {name}: got={got!r} expected={expected!r}")
        if not ok:
            failed += 1
    # Simulate confirmed poll → OA must require non-blank cookie before bind
    poll = {
        "status": "confirmed",
        "account_id": "acc_wechat_channels_demo",
        "credential": {"cookie": {"sessionid": "s1", "wxuin": "u1"}},
    }
    cookie = normalize_cookie_from_credential(poll["credential"])
    assert cookie and "sessionid=" in cookie, cookie
    print(f"PASS poll_confirmed_channels_shape cookie={cookie}")
    print(f"RESULT failed={failed}")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
