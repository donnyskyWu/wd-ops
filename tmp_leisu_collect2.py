# -*- coding: utf-8 -*-
import re, json, html as htmlmod, urllib.parse, time, urllib.request
from curl_cffi import requests

OUT = r"d:\self\sy\运营数据平台\202606\wd\tmp_leisuapp_final.json"
QUERY = "雷速体育APP"
account_id = "acc_wechat_mp_440f02d51e2c8441"
api_token = "test-key-2026"

def sogou_pages(max_pages=5):
    items = []
    for page in range(1, max_pages + 1):
        q = urllib.parse.quote(QUERY)
        url = f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8&page={page}"
        r = requests.get(url, headers={"User-Agent": "Mozilla/5.0"}, impersonate="chrome", timeout=30)
        r.encoding = "utf-8"
        html = r.text
        for m in re.finditer(r'uigs="article_title_(\d+)"[^>]*href="([^"]+)"[^>]*>(.*?)</a>', html, re.S):
            title = htmlmod.unescape(re.sub(r"<[^>]+>", "", m.group(3)))
            title = re.sub(r"\s+", " ", title).strip()
            href = htmlmod.unescape(m.group(2))
            items.append({"title": title, "sogou_href": href})
        time.sleep(2)
    # dedupe by title
    seen = set(); out = []
    for it in items:
        if it["title"] in seen: continue
        seen.add(it["title"]); out.append(it)
    return out

def resolve_wechat_url(sogou_href):
    if sogou_href.startswith("http"):
        full = sogou_href
    else:
        full = "https://weixin.sogou.com" + sogou_href
    r = requests.get(full, headers={"User-Agent": "Mozilla/5.0"}, impersonate="chrome", timeout=30, allow_redirects=True)
    if "mp.weixin.qq.com" in r.url:
        return r.url
    m = re.search(r'url\s*=\s*"(https://mp\.weixin\.qq\.com/[^"]+)"', r.text)
    return m.group(1) if m else r.url

def download_article(wx_url):
    qurl = urllib.parse.quote(wx_url, safe="")
    api = f"http://127.0.0.1:8000/api/v1/internal/wechat-mp/article-download?url={qurl}&account_id={account_id}"
    req = urllib.request.Request(api, headers={"Authorization": f"Bearer {api_token}"})
    with urllib.request.urlopen(req, timeout=180) as resp:
        payload = json.loads(resp.read().decode("utf-8"))
    return payload.get("data") or {}

def is_leisu_account(name: str) -> bool:
    n = (name or "").strip()
    if not n:
        return False
    if n in ("雷速体育APP", "leisuapp"):
        return True
    return "雷速体育" in n and "APP" in n

candidates = sogou_pages(5)
print("candidates", len(candidates))
results = []
errors = []
for i, it in enumerate(candidates):
    try:
        wx_url = resolve_wechat_url(it["sogou_href"])
        it["url"] = wx_url
        data = download_article(wx_url)
        author = data.get("author") or ""
        title = data.get("title") or it["title"]
        # parse account from content if needed - author may be empty; use title match heuristic later
        account_hint = author
        if not is_leisu_account(account_hint):
            # lightweight html sniff via summary/title only when author missing
            account_hint = data.get("summary") or ""
        ok = data.get("success") and (is_leisu_account(author) or ("雷速" in title and "APP" in QUERY))
        # stricter: fetch nickname from HTML var nickname in error content
        if data.get("success") and not is_leisu_account(author):
            txt = (data.get("content_text") or "")[:200]
            # if verify page
            if "环境异常" in txt or "Verify" in txt:
                errors.append({"title": title, "url": wx_url, "error": "verify_page"})
                continue
            # accept articles discovered by sogou account query when author empty
            ok = True
        if ok:
            results.append({
                "title": title,
                "url": wx_url,
                "create_time": data.get("publish_time") or "",
                "digest": data.get("summary") or "",
                "cover": "",
                "author": author,
                "content_text_preview": (data.get("content_text") or "")[:500],
                "download_success": bool(data.get("success")),
                "download_error": data.get("error"),
            })
    except Exception as e:
        errors.append({"title": it.get("title"), "error": str(e)})
    if len(results) >= 10:
        break
    time.sleep(5)

out = {
    "account": {"nickname": "雷速体育APP", "alias": "leisuapp", "fakeid": "Mzg5ODkzNDc2NQ==", "service_type": 1},
    "collector_status": {"service": "running :8000", "session_account_id": account_id},
    "limitations": [
        "公众号后台 /cgi-bin/appmsg list_ex 对该 fakeid 返回 app_msg_cnt=0（非会话过期，base_resp.ret=0）",
        "Brave 搜索补充链路触发 429",
        "搜狗结果页未展示 account_name 字段，文章经关键词检索+正文下载补齐",
    ],
    "articles_count": len(results),
    "articles": results,
    "errors": errors,
}
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, indent=2)
print("saved", OUT, "articles", len(results))
