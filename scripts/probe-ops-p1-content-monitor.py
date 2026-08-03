#!/usr/bin/env python3
"""P1 acceptance probe: 内容生产 + 作品监测 (14 pages)."""
from __future__ import annotations

import csv
import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CSV_PATH = ROOT / "docs/delivery/oa-menu-permission-map.csv"
VIEWS = ROOT / "football-front/apps/web-ele/src/views"
OPS_VIEWS = ROOT / "football-front/apps/web-ele/src/views/ops"  # A-WP1 SSOT; ui-vue removed
GATEWAY = "http://localhost:48080"
VITE_BASE = "http://localhost:5777"
MODULES = {"内容生产", "作品监测"}

PAGE_API: dict[str, str] = {
    "/ops/content": "/admin-api/oa/content/list?pageNum=1&pageSize=1",
    "/ops/content/review": "/admin-api/oa/content/list?pageNum=1&pageSize=1",
    "/ops/knowledge": "/admin-api/oa/knowledge/list?pageNo=1&pageSize=1",
    "/ops/layout-template": "/admin-api/oa/layout-template/list?pageNo=1&pageSize=1",
    "/ops/plan": "/admin-api/oa/plan/list?pageNo=1&pageSize=1",
    "/ops/sop": "/admin-api/oa/sop/template/list?pageNo=1&pageSize=1",
    "/ops/sop/review": "/admin-api/oa/sop/review/pending",
    "/ops/task": "/admin-api/oa/task/list?pageNum=1&pageSize=1",
    "/ops/external-account": "/admin-api/oa/monitor/external/list?pageNum=1&pageSize=1",
    "/ops/high-fans-account": "/admin-api/oa/monitor/high-follower/list?pageNum=1&pageSize=1",
    "/ops/hot-works": "/admin-api/oa/monitor/hit/list?pageNum=1&pageSize=1",
    "/ops/ip-theme": "/admin-api/oa/monitor/ip-theme/1",
    "/ops/low-fans-account": "/admin-api/oa/monitor/low-follower/list?pageNum=1&pageSize=1",
    "/ops/low-score": "/admin-api/oa/monitor/low-score/list?pageNum=1&pageSize=1",
}

FFF_RE = re.compile(
    r"background(?:-color)?\s*:\s*(#fff(f{2})?|white|#f5f7fa|#fafafa)\b", re.I
)


def load_pages() -> list[dict[str, str]]:
    rows = list(csv.DictReader(CSV_PATH.open(encoding="utf-8")))
    return [
        r
        for r in rows
        if r.get("parent_group") in MODULES and r.get("hide_in_menu", "").upper() != "Y"
    ]


def http_json(method: str, url: str, headers: dict[str, str], body: dict | None = None) -> dict:
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    if body is not None:
        req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def login() -> str | None:
    url = f"{GATEWAY}/admin-api/system/auth/login"
    headers = {"tenant-id": "1", "Content-Type": "application/json"}
    body = {"username": "admin", "password": "admin123", "captchaVerification": ""}
    try:
        r = http_json("POST", url, headers, body)
        if r.get("code") == 0:
            return r["data"]["accessToken"]
        print(f"login fail code={r.get('code')} msg={r.get('msg')}")
    except Exception as ex:
        print(f"login error: {ex}")
    return None


def describe_data(data: object) -> str:
    if data is None:
        return "null"
    if isinstance(data, list):
        return f"array len={len(data)}"
    if isinstance(data, dict):
        if "list" in data and "total" in data:
            lst = data.get("list") or []
            return f"page list={len(lst)} total={data.get('total')}"
        return f"object keys={list(data.keys())[:8]}"
    return type(data).__name__


def probe_api(path: str, token: str) -> tuple[bool, str, int | None, str]:
    url = f"{GATEWAY}{path}"
    headers = {
        "Authorization": f"Bearer {token}",
        "X-Tenant-Id": "1",
        "tenant-id": "1",
    }
    try:
        r = http_json("GET", url, headers)
        code = r.get("code")
        data_shape = describe_data(r.get("data"))
        if code == 0:
            return True, "OK", code, data_shape
        return False, str(r.get("msg", ""))[:120], code, data_shape
    except urllib.error.HTTPError as ex:
        body = ex.read(500).decode("utf-8", errors="replace")
        return False, f"HTTP {ex.code}: {body[:120]}", ex.code, ""
    except Exception as ex:
        return False, str(ex), None, ""


def resolve_vue(comp: str) -> Path | None:
    ff = VIEWS / f"{comp}.vue"
    if ff.exists():
        return ff
    # football_component like ops/production/content/index -> ops-platform path
    alt = OPS_VIEWS / f"{comp.replace('ops/', '')}.vue"
    if alt.exists():
        return alt
    return None


def check_theme(comp: str) -> tuple[bool, str]:
    vue = resolve_vue(comp)
    if not vue:
        return False, "vue file missing"
    text = vue.read_text(encoding="utf-8")
    if "ops-page" not in text:
        return False, "missing ops-page root class"
    style_blocks = re.findall(r"<style[^>]*>([\s\S]*?)</style>", text)
    style_text = "\n".join(style_blocks)
    if FFF_RE.search(style_text):
        return False, "hardcoded light background in scoped styles"
    return True, "OK"


def main() -> int:
    pages = load_pages()
    token = login()
    results = []
    print("=== P1 probe: 内容生产 + 作品监测 ===")
    for row in pages:
        title = row["menu_title"]
        fp = row["football_path"].strip()
        comp = row["football_component"].strip()
        api_path = PAGE_API.get(fp, "")
        theme_ok, theme_msg = check_theme(comp)
        if not token:
            api_ok, api_msg, code, data_shape = False, "login failed", None, ""
        elif not api_path:
            api_ok, api_msg, code, data_shape = False, "no API mapping", None, ""
        else:
            api_ok, api_msg, code, data_shape = probe_api(api_path, token)
        status = "PASS" if api_ok and theme_ok else "FAIL"
        print(
            f"  [{status}] {title} | api={'Y' if api_ok else 'N'} code={code} data={data_shape} "
            f"| theme={'Y' if theme_ok else 'N'} | {api_msg if not api_ok else theme_msg if not theme_ok else 'OK'}"
        )
        results.append(
            {
                "title": title,
                "football_path": fp,
                "api": api_path,
                "api_ok": api_ok,
                "code": code,
                "data": data_shape,
                "theme_ok": theme_ok,
                "fixed": False,
                "notes": api_msg if not api_ok else (theme_msg if not theme_ok else ""),
            }
        )
    passed = sum(1 for r in results if r["api_ok"] and r["theme_ok"])
    print(f"\n=== Summary: {passed}/{len(results)} pass ===")
    out = ROOT / "docs/delivery/ops-acceptance-p1-probe.json"
    out.write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"JSON: {out}")
    return 0 if passed == len(results) else 1


if __name__ == "__main__":
    sys.exit(main())
