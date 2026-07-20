# -*- coding: utf-8 -*-
import re, json, time, html as htmlmod, urllib.parse
from curl_cffi import requests

OUT = r"d:\self\sy\运营数据平台\202606\wd\tmp_leisuapp_final.json"
ACCOUNT_NAMES = {"雷速体育APP", "leisuapp"}
QUERY = "雷速体育APP"

def fetch_sogou_page(page=1):
    q = urllib.parse.quote(QUERY)
    url = f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8&page={page}"
    r = requests.get(url, headers={"User-Agent": "Mozilla/5.0"}, impersonate="chrome", timeout=30)
    r.encoding = "utf-8"
    return r.text

def parse_items(page_html):
    items = []
    blocks = re.split(r'<li id="sogou_vr_\d+_box_0"', page_html)
    for block in blocks[1:]:
        title_m = re.search(r'uigs="article_title_\d+"[^>]*href="([^"]+)"[^>]*>(.*?)</a>', block, re.S)
        if not title_m:
            continue
        href = htmlmod.unescape(title_m.group(1))
        title = re.sub(r"<[^>]+>", "", title_m.group(2))
        title = htmlmod.unescape(re.sub(r"\s+", " ", title)).strip()
        acct_m = re.search(r'uigs="account_name_\d+"[^>]*>(.*?)</a>', block, re.S)
        account = re.sub(r"<[^>]+>", "", acct_m.group(1)).strip() if acct_m else ""
        account = htmlmod.unescape(account)
        time_m = re.search(r'class="s2"[^>]*>\s*<script>[^<]*</script>\s*([^<]+)<', block)
        if not time_m:
            time_m = re.search(r'class="s2"[^>]*>([^<]+)<', block)
        pub_time = htmlmod.unescape(time_m.group(1).strip()) if time_m else ""
        snippet_m = re.search(r'class="txt-info[^"]*"[^>]*>(.*?)</p>', block, re.S)
        snippet = re.sub(r"<[^>]+>", "", snippet_m.group(1)).strip() if snippet_m else ""
        snippet = htmlmod.unescape(re.sub(r"\s+", " ", snippet))
        items.append({
            "title": title,
            "account": account,
            "pub_time": pub_time,
            "snippet": snippet,
            "sogou_href": href,
        })
    return items

def resolve_wechat_url(sogou_href):
    if sogou_href.startswith("http"):
        full = sogou_href
    else:
        full = "https://weixin.sogou.com" + sogou_href.replace("&amp;", "&")
    r = requests.get(full, headers={"User-Agent": "Mozilla/5.0"}, impersonate="chrome", timeout=30, allow_redirects=True)
    final = r.url
    if "mp.weixin.qq.com" in final:
        return final
    # sometimes redirect in meta
    m = re.search(r'url\s*=\s*"(https://mp\.weixin\.qq\.com/[^"]+)"', r.text)
    return m.group(1) if m else final

all_items = []
for p in range(1, 4):
    html = fetch_sogou_page(p)
    page_items = parse_items(html)
    all_items.extend(page_items)
    time.sleep(2)

seen = set()
filtered = []
for it in all_items:
    acct = it.get("account", "")
    if acct not in ACCOUNT_NAMES and "雷速体育" not in acct:
        continue
    key = (it.get("title"), it.get("pub_time"))
    if key in seen:
        continue
    seen.add(key)
    filtered.append(it)

filtered = filtered[:10]

# download content via internal API
import urllib.request
account_id = "acc_wechat_mp_440f02d51e2c8441"
token = "test-key-2026"
results = []
for i, it in enumerate(filtered):
    try:
        wx_url = resolve_wechat_url(it["sogou_href"])
        it["url"] = wx_url
        qurl = urllib.parse.quote(wx_url, safe="")
        api = f"http://127.0.0.1:8000/api/v1/internal/wechat-mp/article-download?url={qurl}&account_id={account_id}"
        req = urllib.request.Request(api, headers={"Authorization": f"Bearer {token}"})
        with urllib.request.urlopen(req, timeout=120) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
        data = payload.get("data") or {}
        it["download"] = {
            "success": data.get("success"),
            "title": data.get("title"),
            "author": data.get("author"),
            "publish_time": data.get("publish_time"),
            "summary": data.get("summary"),
            "content_text": (data.get("content_text") or "")[:500],
            "error": data.get("error"),
        }
    except Exception as e:
        it["download"] = {"success": False, "error": str(e)}
    results.append(it)
    if i < len(filtered) - 1:
        time.sleep(5)

out = {
    "account": {
        "nickname": "雷速体育APP",
        "alias": "leisuapp",
        "fakeid": "Mzg5ODkzNDc2NQ==",
        "service_type": 1,
        "note": "公众号后台 appmsg 历史列表返回 0 篇；文章来自搜狗微信搜索并按账号名过滤",
    },
    "api_limitation": {
        "user_articles_total": 0,
        "warning": "当前运营会话可搜到该号，但无法通过 /cgi-bin/appmsg list_ex 拉取他人号历史",
    },
    "articles": results,
}
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, indent=2)
print("saved", OUT)
print("count", len(results))
for a in results:
    print("-", a.get("pub_time"), a.get("title"), a.get("url", "")[:80])
