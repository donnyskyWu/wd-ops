import re, json, sys, time, urllib.parse, urllib.request
sys.path.insert(0, r"d:\self\sy\运营数据平台\202606\wd\unify-collector-api")
from platforms.wechat_mp.scraper import fetch_article_html_via_browser
from platforms.wechat_mp.external_api import resolve_session

account_id="acc_wechat_mp_440f02d51e2c8441"
sess=resolve_session(account_id)
cookie=sess["cookie"]
q=urllib.parse.quote("雷速体育APP")
search_url=f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8"
html=fetch_article_html_via_browser(search_url,cookie=cookie,headless=True,stealth=True)
print("len",len(html),"antispider", "antispider" in html, "article_title", html.count("article_title"), file=sys.stderr)
# extract titles and sogou links
items=[]
for m in re.finditer(r'href="(/link\?url=[^"]+)"[^>]*id="sogou_vr_11002601_title_\d+"', html):
    items.append(m.group(1))
if not items:
    for m in re.finditer(r'href="(/link\?url=[^"]+)"[^>]*uigs="article_title_\d+"', html):
        items.append(m.group(1))
print("items",len(items), file=sys.stderr)
# resolve first 3 links via browser
resolved=[]
for href in items[:12]:
    full="https://weixin.sogou.com"+href.replace("&amp;","&")
    h=fetch_article_html_via_browser(full,cookie=cookie,headless=True,stealth=True)
    urls=re.findall(r"https://mp\.weixin\.qq\.com/s[^\"'\s<]+", h)
    if urls:
        resolved.append(urls[0])
    elif "mp.weixin.qq.com" in h:
        m=re.search(r"https://mp\.weixin\.qq\.com/[^\"'\s<]+", h)
        if m: resolved.append(m.group(0))
    time.sleep(2)
print("resolved",len(resolved), file=sys.stderr)
for u in resolved: print(u)
