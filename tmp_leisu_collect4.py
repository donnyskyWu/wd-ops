# -*- coding: utf-8 -*-
import re, json, html as htmlmod, urllib.parse, time, urllib.request, sys
sys.path.insert(0, r"d:\self\sy\运营数据平台\202606\wd\unify-collector-api")

from curl_cffi import requests
from platforms.wechat_mp.scraper import fetch_article_html_via_curl_cffi
from platforms.wechat_mp.external_api import resolve_session

OUT = r"d:\self\sy\运营数据平台\202606\wd\tmp_leisuapp_final.json"
QUERY = "雷速体育APP"
account_id = "acc_wechat_mp_440f02d51e2c8441"
api_token = "test-key-2026"
TARGET_ALIAS = "leisuapp"
TARGET_NAMES = ("雷速体育APP",)
TITLE_RE = re.compile(r'<h3>\s*<a[^>]*href="([^"]+)"[^>]*uigs="article_title_\d+"[^>]*>(.*?)</a>', re.S)

def sogou_page1():
    q = urllib.parse.quote(QUERY)
    url = f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8"
    r = requests.get(url, headers={"User-Agent": "Mozilla/5.0"}, impersonate="chrome", timeout=30)
    r.encoding = "utf-8"
    items = []
    for m in TITLE_RE.finditer(r.text):
        title = htmlmod.unescape(re.sub(r"<[^>]+>", "", m.group(2)))
        title = re.sub(r"\s+", " ", title).strip()
        href = htmlmod.unescape(m.group(1))
        items.append({"title": title, "sogou_href": href})
    return items

def resolve_wechat_url(sogou_href):
    full = sogou_href if sogou_href.startswith("http") else "https://weixin.sogou.com" + sogou_href
    r = requests.get(full, headers={"User-Agent": "Mozilla/5.0"}, impersonate="chrome", timeout=30, allow_redirects=True)
    if "mp.weixin.qq.com" in r.url:
        return r.url
    m = re.search(r'url\s*=\s*"(https://mp\.weixin\.qq\.com/[^"]+)"', r.text)
    return m.group(1) if m else r.url

def extract_mp_meta(html: str):
    nick, alias = "", ""
    for pat in [r'var\s+nickname\s*=\s*"([^"]*)"', r"var\s+nickname\s*=\s*'([^']*)'"]:
        m = re.search(pat, html)
        if m: nick = htmlmod.unescape(m.group(1)); break
    for pat in [r'var\s+user_name\s*=\s*"([^"]*)"', r"var\s+user_name\s*=\s*'([^']*)'"]:
        m = re.search(pat, html)
        if m: alias = htmlmod.unescape(m.group(1)); break
    return nick, alias

def download_article(wx_url):
    qurl = urllib.parse.quote(wx_url, safe="")
    api = f"http://127.0.0.1:8000/api/v1/internal/wechat-mp/article-download?url={qurl}&account_id={account_id}"
    req = urllib.request.Request(api, headers={"Authorization": f"Bearer {api_token}"})
    with urllib.request.urlopen(req, timeout=180) as resp:
        payload = json.loads(resp.read().decode("utf-8"))
    return payload.get("data") or {}

def match_target(nick, alias):
    return alias == TARGET_ALIAS or nick in TARGET_NAMES

sess = resolve_session(account_id)
candidates = sogou_page1()
print("candidates", len(candidates), file=sys.stderr)
results, errors, skipped = [], [], []
for i, it in enumerate(candidates):
    try:
        wx_url = resolve_wechat_url(it["sogou_href"])
        data = download_article(wx_url)
        if not data.get("success"):
            errors.append({"title": it["title"], "url": wx_url, "error": data.get("error") or "download_failed"})
            time.sleep(5); continue
        html = fetch_article_html_via_curl_cffi(wx_url, cookie=sess.get("cookie", ""))
        nick, alias = extract_mp_meta(html)
        if not match_target(nick, alias):
            skipped.append({"title": data.get("title") or it["title"], "url": wx_url, "nickname": nick, "user_name": alias})
            time.sleep(5); continue
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
            "stats": None,
        })
    except Exception as e:
        errors.append({"title": it.get("title"), "error": str(e)})
    if len(results) >= 10:
        break
    time.sleep(5)

out = {
    "account": {"nickname": "雷速体育APP", "alias": "leisuapp", "fakeid": "Mzg5ODkzNDc2NQ==", "service_type": 1},
    "api_user_articles": {"total": 0, "articles": []},
    "discovery_source": "sogou_weixin page1 + article-download",
    "articles_count": len(results),
    "articles": results,
    "skipped_non_target_account": skipped,
    "errors": errors,
}
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, indent=2)
print(json.dumps({"matched": len(results), "skipped": len(skipped), "errors": len(errors), "path": OUT}, ensure_ascii=False))
