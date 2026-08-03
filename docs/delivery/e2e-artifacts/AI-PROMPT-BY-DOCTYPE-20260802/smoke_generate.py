# -*- coding: utf-8 -*-
"""Smoke: two documentTypes resolve to two different AI_CONTENT_CHAT prompts."""
from __future__ import annotations

import json
import uuid
from pathlib import Path
from urllib import error, request

OUT = Path(__file__).resolve().parent
GATEWAY = "http://127.0.0.1:48080/admin-api"
TENANT = "1"


def http_json(method: str, url: str, body=None, token: str | None = None):
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode("utf-8")
    headers = {
        "Content-Type": "application/json",
        "tenant-id": TENANT,
        "X-Tenant-Id": TENANT,
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = request.Request(url, data=data, headers=headers, method=method)
    try:
        with request.urlopen(req, timeout=180) as resp:
            raw = resp.read().decode("utf-8")
            return resp.status, json.loads(raw) if raw else {}
    except error.HTTPError as ex:
        raw = ex.read().decode("utf-8", errors="replace")
        try:
            parsed = json.loads(raw) if raw else {}
        except Exception:
            parsed = {"raw": raw}
        return ex.code, parsed


def main():
    code, login = http_json(
        "POST",
        f"{GATEWAY}/system/auth/login",
        {"username": "admin", "password": "admin123", "captchaVerification": ""},
    )
    (OUT / "00-login.json").write_text(json.dumps({"http": code, "body": login}, ensure_ascii=False, indent=2), encoding="utf-8")
    token = (login.get("data") or {}).get("accessToken")
    if not token:
        raise SystemExit(f"login failed: {code} {login}")

    code, models = http_json("GET", f"{GATEWAY}/ops/ai-content/models", token=token)
    (OUT / "models.json").write_text(json.dumps({"http": code, "body": models}, ensure_ascii=False, indent=2), encoding="utf-8")
    model_list = (models.get("data") or {}).get("models") or []
    if not model_list:
        raise SystemExit(f"no models: {models}")
    model_id = model_list[0]["id"]

    results = []
    for doc_type in ("POST_MATCH_REVIEW", "PREHEAT_PREVIEW"):
        payload = {
            "sessionId": str(uuid.uuid4()),
            "modelId": model_id,
            "message": "请生成一版简短内容用于冒烟",
            "documentType": doc_type,
            "contentType": "ARTICLE",
            "roundCount": 1,
            "preferenceSummary": "",
            "conversationHistory": [],
            "context": {
                "matchName": "冒烟赛事 曼联 vs 切尔西",
                "authorName": "冒烟作者",
                "schemeTypes": ["COMPREHENSIVE"],
                "historyRecord": "近10场 6胜2平2负",
                "anchorStyle": "comprehensive",
                "productDescription": "冒烟产品说明",
            },
        }
        http, body = http_json("POST", f"{GATEWAY}/ops/ai-content/generate", payload, token=token)
        data = body.get("data") or {}
        item = {
            "documentType_req": doc_type,
            "http": http,
            "code": body.get("code"),
            "msg": body.get("msg"),
            "promptId": data.get("promptId"),
            "promptTemplateName": data.get("promptTemplateName"),
            "documentType_resp": data.get("documentType"),
            "mock": data.get("mock"),
            "contentPrefix": (data.get("content") or "")[:120],
        }
        results.append(item)
        (OUT / f"generate-{doc_type}.json").write_text(
            json.dumps({"http": http, "body": body, "summary": item}, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    distinct = len({r.get("promptId") for r in results}) == 2 and all(r.get("promptId") for r in results)
    prefixes_distinct = len({r.get("promptTemplateName") for r in results}) == 2
    report = {
        "ok": distinct and prefixes_distinct and all(r.get("code") == 0 for r in results),
        "distinctPromptIds": distinct,
        "distinctTemplateNames": prefixes_distinct,
        "results": results,
    }
    (OUT / "RESULTS.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()