# -*- coding: utf-8 -*-
import re, json, html as htmlmod, urllib.parse, time, urllib.request
from curl_cffi import requests

OUT = r"d:\self\sy\运营数据平台\202606\wd\tmp_leisuapp_final.json"
QUERY = "雷速体育APP"
account_id = "acc_wechat_mp_440f02d51e2c8441"
api_token = "test-key-2026"
TARGET_ALIAS = "leisuapp"
TARGET_NAMES = ("雷速体育APP",)

TITLE_RE = re.compile(
    r'<h3>\s*<a[^>]*href="([^"]+)"[^>]*uigs="article_title_\d+"[^>]*>(.*?)</a>',
    re.S,
)

def sogou_collect(max_pages=6, max_candidates=40):
    items = []
    for page in range(1, max_pages + 1):
        q = urllib.parse.quote(QUERY)
        url = f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8&page={page}"
        r = requests.get(url, headers={"User-Agent": "Mozilla/5.0"}, impersonate="chrome", timeout=30)
        r.encoding = "utf-8"
        for m in TITLE_RE.finditer(r.text):
            title = htmlmod.unescape(re.sub(r"<[^>]+>", "", m.group(2)))
            title = re.sub(r"\s+", " ", title).strip()
            href = htmlmod.unescape(m.group(1))
            items.append({"title": title, "sogou_href": href})
        if len(items) >= max_candidates:
            break
        time.sleep(2)
    seen = set(); out = []
    for it in items:
        if it["title"] in seen:
            continue
        seen.add(it["title"])
        out.append(it)
    return out

def resolve_wechat_url(sogou_href):
    full = sogou_href if sogou_href.startswith("http") else "https://weixin.sogou.com" + sogou_href
    r = requests.get(full, headers={"User-Agent": "Mozilla/5.0"}, impersonate="chrome", timeout=30, allow_redirects=True)
    if "mp.weixin.qq.com" in r.url:
        return r.url, r.text[:5000]
    m = re.search(r'url\s*=\s*"(https://mp\.weixin\.qq\.com/[^"]+)"', r.text)
    return (m.group(1) if m else r.url), r.text[:5000]

def extract_mp_meta(html: str):
    nick = ""
    alias = ""
    for pat in [r'var\s+nickname\s*=\s*"([^"]*)"', r"var\s+nickname\s*=\s*'([^']*)'"]:
        m = re.search(pat, html)
        if m:
            nick = htmlmod.unescape(m.group(1)); break
    for pat in [r'var\s+user_name\s*=\s*"([^"]*)"', r"var\s+user_name\s*=\s*'([^']*)'"]:
        m = re.search(pat, html)
        if m:
            alias = htmlmod.unescape(m.group(1)); break
    return nick, alias

def download_article(wx_url):
    qurl = urllib.parse.quote(wx_url, safe="")
    api = f"http://127.0.0.1:8000/api/v1/internal/wechat-mp/article-download?url={qurl}&account_id={account_id}"
    req = urllib.request.Request(api, headers={"Authorization": f"Bearer {api_token}"})
    with urllib.request.urlopen(req, timeout=180) as resp:
        payload = json.loads(resp.read().decode("utf-8"))
    return payload.get("data") or {}

def match_target(nick, alias):
    if alias == TARGET_ALIAS:
        return True
    if nick in TARGET_NAMES:
        return True
    return False

candidates = sogou_collect()
print("candidates", len(candidates))
results = []
errors = []
skipped = []
for i, it in enumerate(candidates):
    try:
        wx_url, _ = resolve_wechat_url(it["sogou_href"])
        data = download_article(wx_url)
        if not data.get("success"):
            err = data.get("error") or "download_failed"
            if err and ("verify" in str(err).lower() or "环境" in str(err)):
                errors.append({"title": it["title"], "url": wx_url, "error": "verify_page"})
            else:
                errors.append({"title": it["title"], "url": wx_url, "error": err})
            time.sleep(5)
            continue
        # fetch nickname via lightweight curl
        from platforms.wechat_mp.scraper import fetch_article_html_via_curl_cffi
        import sys
        sys.path.insert(0, r"d:\self\sy\运营数据平台\202606\wd\unify-collector-api")
        from platforms.wechat_mp.external_api import resolve_session
        sess = resolve_session(account_id)
        html = fetch_article_html_via_curl_cffi(wx_url, cookie=sess.get("cookie", ""))
        nick, alias = extract_mp_meta(html)
        if not match_target(nick, alias):
            skipped.append({"title": data.get("title") or it["title"], "url": wx_url, "nickname": nick, "user_name": alias})
            time.sleep(5)
            continue
        results.append({
            "title": data.get("title") or it["title"],
            "url": wx_url,
            "create_time": data.get("publish_time") or "",
            "digest": data.get("summary") or "",
            "cover": "",
            "account_nickname": nick,
            "account_alias": alias,
            "author": data.get("author") or "",
            "content_text_preview": (data.get("content_text") or "")[:500],
            "content_length": len(data.get("content_text") or ""),
            "download_success": True,
        })
    except Exception as e:
        errors.append({"title": it.get("title"), "error": str(e)})
    if len(results) >= 10:
        break
    time.sleep(5)

out = {
    "account": {
        "nickname": "雷速体育APP",
        "alias": "leisuapp",
        "fakeid": "Mzg5ODkzNDc2NQ==",
        "service_type": 1,
        "round_head_img": "http://mmbiz.qpic.cn/mmbiz_png/6UylibE9r5Mt7rWiatdhcItQOk0Pic0JfTVTQ2qTzUiaS6wQHPXNpzxGgMVFziaiaPXIpBZVPZcFMnK5BsGMPEok3w2A/0?wx_fmt=png",
    },
    "api_user_articles": {"total": 0, "articles": [], "note": "运营会话可 searchbiz，但 list_ex 历史为空"},
    "discovery_source": "sogou_weixin_type2 + article-download 校验 user_name/nickname",
    "articles_count": len(results),
    "articles": results,
    "skipped_non_target_account": skipped[:15],
    "errors": errors,
}
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, indent=2)
print("saved", OUT)
print("matched", len(results), "skipped", len(skipped), "errors", len(errors))
