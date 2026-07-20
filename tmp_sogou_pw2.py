# -*- coding: utf-8 -*-
import re, html as htmlmod, urllib.parse, sys
sys.path.insert(0, r"d:\self\sy\运营数据平台\202606\wd\unify-collector-api")
from curl_cffi import requests
from platforms.wechat_mp.scraper import fetch_article_html_via_browser

q = urllib.parse.quote("雷速体育APP")
search_url = f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8"
r = requests.get(search_url, headers={"User-Agent":"Mozilla/5.0"}, impersonate="chrome", timeout=30)
r.encoding="utf-8"
TITLE_RE = re.compile(r'uigs="article_title_\d+"[^>]*href="([^"]+)"[^>]*>(.*?)</a>', re.S)
m = TITLE_RE.search(r.text)
if not m:
    TITLE_RE = re.compile(r'href="([^"]+)"[^>]*uigs="article_title_\d+"[^>]*>(.*?)</a>', re.S)
    m = TITLE_RE.search(r.text)
href = htmlmod.unescape(m.group(1))
title = htmlmod.unescape(re.sub(r"<[^>]+>","",m.group(2)))
print("title", title)
full = "https://weixin.sogou.com" + href.replace("&amp;","&")
html = fetch_article_html_via_browser(full, cookie="", headless=True, stealth=True)
print("len", len(html))
print("final url hints", re.findall(r"https://mp\.weixin\.qq\.com/[^\"'\s<]+", html)[:5])
if "antispider" in html:
    print("antispider page")
