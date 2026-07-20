# -*- coding: utf-8 -*-
import re, json, sys, time, urllib.parse, urllib.request
sys.path.insert(0, r"d:\self\sy\运营数据平台\202606\wd\unify-collector-api")
from platforms.wechat_mp.scraper import fetch_article_html_via_browser, fetch_article_html_via_curl_cffi
from platforms.wechat_mp.external_api import resolve_session

account_id = "acc_wechat_mp_440f02d51e2c8441"
api_token = "test-key-2026"
sess = resolve_session(account_id)

def bing_links():
    q = urllib.parse.quote("site:mp.weixin.qq.com leisuapp")
    url = f"https://www.bing.com/search?q={q}&count=20"
    html = fetch_article_html_via_browser(url, cookie="", headless=True, stealth=True)
    links = sorted(set(re.findall(r"https://mp\.weixin\.qq\.com/s[^\"'&\s<>]+", html)))
    return links, len(html)

links, hl = bing_links()
print("bing html", hl, "links", len(links), file=sys.stderr)
for l in links[:15]:
    print(l, file=sys.stderr)

def extract_meta(html):
    import html as htmlmod
    nick=alias=""
    for pat in [r'var\s+nickname\s*=\s*"([^"]*)"', r"var\s+nickname\s*=\s*'([^']*)'"]:
        m=re.search(pat, html)
        if m: nick=htmlmod.unescape(m.group(1)); break
    for pat in [r'var\s+user_name\s*=\s*"([^"]*)"', r"var\s+user_name\s*=\s*'([^']*)'"]:
        m=re.search(pat, html)
        if m: alias=htmlmod.unescape(m.group(1)); break
    return nick, alias

def download(wx_url):
    qurl=urllib.parse.quote(wx_url, safe="")
    api=f"http://127.0.0.1:8000/api/v1/internal/wechat-mp/article-download?url={qurl}&account_id={account_id}"
    req=urllib.request.Request(api, headers={"Authorization": f"Bearer {api_token}"})
    with urllib.request.urlopen(req, timeout=180) as resp:
        return json.loads(resp.read().decode()).get("data") or {}

results=[]; errors=[]
for i,u in enumerate(links):
    try:
        data=download(u)
        html=fetch_article_html_via_curl_cffi(u, cookie=sess.get("cookie",""))
        nick, alias = extract_meta(html)
        if alias != "leisuapp" and nick != "雷速体育APP":
            continue
        if not data.get("success"):
            errors.append({"url":u,"error":data.get("error")}); continue
        results.append({
            "title": data.get("title"),
            "url": u,
            "create_time": data.get("publish_time"),
            "digest": data.get("summary"),
            "account_nickname": nick,
            "account_alias": alias,
            "author": data.get("author"),
            "content_text_preview": (data.get("content_text") or "")[:500],
            "content_length": len(data.get("content_text") or ""),
        })
    except Exception as e:
        errors.append({"url":u,"error":str(e)})
    if len(results)>=10: break
    time.sleep(5)

out_path=r"d:\self\sy\运营数据平台\202606\wd\tmp_leisuapp_final.json"
out={
  "account":{"nickname":"雷速体育APP","alias":"leisuapp","fakeid":"Mzg5ODkzNDc2NQ=="},
  "discovery_source":"bing_playwright_search",
  "articles_count":len(results),
  "articles":results,
  "errors":errors,
}
json.dump(out, open(out_path,"w",encoding="utf-8"), ensure_ascii=False, indent=2)
print(json.dumps({"articles":len(results),"errors":len(errors),"path":out_path}, ensure_ascii=False))
