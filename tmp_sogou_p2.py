# -*- coding: utf-8 -*-
import re, json, html as htmlmod, sys, urllib.parse
sys.path.insert(0, r"d:\self\sy\运营数据平台\202606\wd\unify-collector-api")
from platforms.wechat_mp.scraper import fetch_article_html_via_browser
from platforms.wechat_mp.external_api import resolve_session

def parse_page(page):
    blocks = re.split(r'(?=<a[^>]*id="sogou_vr_11002601_title_)', page)
    articles=[]
    for block in blocks[1:]:
        tm=re.search(r'id="sogou_vr_11002601_title_\d+"[^>]*>(.*?)</a>', block, re.S)
        if not tm: continue
        title=htmlmod.unescape(re.sub(r"<[^>]+>","",tm.group(1))); title=re.sub(r"\s+"," ",title).strip()
        sm=re.search(r'class="txt-info[^"]*"[^>]*>(.*?)</p>', block, re.S)
        snippet=htmlmod.unescape(re.sub(r"\s+"," ",re.sub(r"<[^>]+>","",sm.group(1)).strip())) if sm else ""
        dm=re.search(r'class="s2"[^>]*>(.*?)</span>', block, re.S)
        date_raw=re.sub(r"<script>.*?</script>","",dm.group(1),flags=re.S) if dm else ""
        date=htmlmod.unescape(re.sub(r"<[^>]+>","",date_raw)).strip() if dm else ""
        articles.append({"title":title,"digest":snippet,"create_time":date})
    return articles

sess=resolve_session("acc_wechat_mp_440f02d51e2c8441")
for p in [1,2]:
    q=urllib.parse.quote("雷速体育APP")
    url=f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8&page={p}"
    page=fetch_article_html_via_browser(url,cookie=sess["cookie"],headless=True,stealth=True)
    arts=parse_page(page)
    print("page",p,"count",len(arts),"antispider", "antispider" in page)
