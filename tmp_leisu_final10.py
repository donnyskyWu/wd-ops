# -*- coding: utf-8 -*-
import re, json, html as htmlmod, sys, urllib.parse
from datetime import datetime
sys.path.insert(0, r"d:\self\sy\运营数据平台\202606\wd\unify-collector-api")
from platforms.wechat_mp.scraper import fetch_article_html_via_browser
from platforms.wechat_mp.external_api import resolve_session, search_accounts

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
        cm=re.search(r'url=(https%3A%2F%2Fmmbiz\.qpic\.cn[^"&]+)', block)
        cover=urllib.parse.unquote(cm.group(1)) if cm else ""
        hm=re.search(r'href="(/link\?url=[^"]+)"[^>]*id="sogou_vr_11002601_title_', block)
        sogou_href=htmlmod.unescape(hm.group(1)) if hm else ""
        articles.append({
            "title": title,
            "digest": snippet,
            "create_time": date,
            "cover": cover,
            "sogou_link": ("https://weixin.sogou.com"+sogou_href.replace("&amp;","&")) if sogou_href else "",
            "url": "",
            "content_text_preview": snippet[:500],
            "download_success": False,
            "download_error": "sogou_link_captcha_blocked",
            "stats": None,
        })
    return articles

def parse_date(s):
    for fmt in ("%Y-%m-%d","%Y-%m-%d %H:%M","%Y-%m-%d %H:%M:%S"):
        try: return datetime.strptime(s, fmt)
        except: pass
    m=re.match(r"(\d{4})-(\d{1,2})-(\d{1,2})", s)
    if m:
        return datetime(int(m.group(1)), int(m.group(2)), int(m.group(3)))
    return datetime.min

account_id="acc_wechat_mp_440f02d51e2c8441"
sess=resolve_session(account_id)
target=next(a for a in search_accounts("leisuapp", sess, count=5)["list"] if a.get("alias")=="leisuapp")
all_items=[]
for p in [1,2]:
    q=urllib.parse.quote("雷速体育APP")
    url=f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8&page={p}"
    page=fetch_article_html_via_browser(url,cookie=sess["cookie"],headless=True,stealth=True)
    all_items.extend(parse_page(page))

seen=set(); uniq=[]
for a in all_items:
    k=a["title"]
    if k in seen: continue
    seen.add(k); uniq.append(a)
uniq.sort(key=lambda x: parse_date(x.get("create_time","")), reverse=True)
articles=uniq[:10]

out={
  "account":{
    "nickname": target.get("nickname") or "雷速体育APP",
    "alias": target.get("alias"),
    "fakeid": target.get("fakeid"),
    "service_type": target.get("service_type"),
    "round_head_img": target.get("round_head_img"),
  },
  "collector":{"running":True,"port":8000,"session_account_id":account_id},
  "official_api":{
    "search_account":"success",
    "user_articles_total":0,
    "user_articles":[],
    "session_status":"有效（searchbiz 成功，非 cookie 过期）",
  },
  "errors_and_limits":[
    "公众号后台 /cgi-bin/appmsg list_ex 对 fakeid=Mzg5ODkzNDc2NQ== 返回 app_msg_cnt=0",
    "article-download 未能批量执行：搜狗跳转链接需验证码，无法获得 mp.weixin.qq.com 直链",
    "Brave 搜索 HTTP 429；Bing 检索未解析到可用直链",
    "阅读/点赞等 stats：他人公众号不可用官方图文分析接口",
  ],
  "discovery_source":"搜狗微信搜索 type=2（Playwright 渲染）+ 按发布时间取前10条",
  "articles_count":len(articles),
  "articles":articles,
}
path=r"d:\self\sy\运营数据平台\202606\wd\tmp_leisuapp_final.json"
json.dump(out, open(path,"w",encoding="utf-8"), ensure_ascii=False, indent=2)
print(len(articles), path)
