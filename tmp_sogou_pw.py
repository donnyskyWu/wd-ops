# -*- coding: utf-8 -*-
import re, html as htmlmod, urllib.parse, time, sys
sys.path.insert(0, r"d:\self\sy\运营数据平台\202606\wd\unify-collector-api")
from curl_cffi import requests
from platforms.wechat_mp.scraper import fetch_article_html_via_browser

q = urllib.parse.quote("雷速体育APP")
search_url = f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8"
r = requests.get(search_url, headers={"User-Agent":"Mozilla/5.0"}, impersonate="chrome", timeout=30)
r.encoding="utf-8"
TITLE_RE = re.compile(r'id="sogou_vr_11002601_title_(\d+)"[^>]*href="([^"]+)"[^>]*>(.*?)</a>', re.S)
item = None
for m in TITLE_RE.finditer(r.text):
    item = {"title": htmlmod.unescape(re.sub(r"<[^>]+>","",m.group(3))), "href": htmlmod.unescape(m.group(2))}
    break
print("item", item)
if not item:
    sys.exit(1)
full = "https://weixin.sogou.com" + item["href"].replace("&amp;","&")
print("opening", full[:100])
html = fetch_article_html_via_browser(full, cookie="", headless=True, stealth=True)
print("html len", len(html))
for pat in [r"https://mp\.weixin\.qq\.com/[^\"'\s]+", r"location\.href\s*=\s*'([^']+)'", r'url = "([^"]+)"']:
    ms = re.findall(pat, html)
    print(pat, ms[:3])
