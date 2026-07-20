# -*- coding: utf-8 -*-
import re, json, html as htmlmod, sys, urllib.parse
sys.path.insert(0, r"d:\self\sy\运营数据平台\202606\wd\unify-collector-api")
from platforms.wechat_mp.scraper import fetch_article_html_via_browser
from platforms.wechat_mp.external_api import resolve_session, search_accounts

account_id = "acc_wechat_mp_440f02d51e2c8441"
sess = resolve_session(account_id)
acct = search_accounts("leisuapp", sess, count=5)
target = next(a for a in acct["list"] if a.get("alias")=="leisuapp")

q = urllib.parse.quote("雷速体育APP")
search_url = f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8"
page = fetch_article_html_via_browser(search_url, cookie=sess["cookie"], headless=True, stealth=True)

# split by list items
chunks = re.split(r'(<li[^>]*id="sogou_vr_\d+_box_0"[^>]*>)', page)
articles = []
for i in range(1, len(chunks), 2):
    block = chunks[i] + (chunks[i+1] if i+1 < len(chunks) else "")
    tm = re.search(r'id="sogou_vr_11002601_title_\d+"[^>]*>(.*?)</a>', block, re.S)
    if not tm:
        tm = re.search(r'uigs="article_title_\d+"[^>]*>(.*?)</a>', block, re.S)
    if not tm:
        continue
    title = htmlmod.unescape(re.sub(r"<[^>]+>", "", tm.group(1)))
    title = re.sub(r"\s+", " ", title).strip()
    sm = re.search(r'class="txt-info[^"]*"[^>]*>(.*?)</p>', block, re.S)
    snippet = htmlmod.unescape(re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", sm.group(1)).strip())) if sm else ""
    dm = re.search(r'class="s2"[^>]*>(.*?)</span>', block, re.S)
    date = htmlmod.unescape(re.sub(r"<[^>]+>", "", dm.group(1)).strip()) if dm else ""
    cm = re.search(r'url=(https%3A%2F%2Fmmbiz\.qpic\.cn[^"&]+)', block)
    cover = urllib.parse.unquote(cm.group(1)) if cm else ""
    hm = re.search(r'href="(/link\?url=[^"]+)"', block)
    sogou_href = htmlmod.unescape(hm.group(1)) if hm else ""
    articles.append({
        "title": title,
        "digest": snippet,
        "create_time": date,
        "cover": cover,
        "sogou_link": "https://weixin.sogou.com" + sogou_href.replace("&amp;","&") if sogou_href else "",
        "url": "",
        "content_text_preview": snippet[:500],
        "download_success": False,
        "download_error": "sogou_link_captcha_blocked",
    })

articles = articles[:10]
out = {
    "account": {
        "nickname": target.get("nickname") or "雷速体育APP",
        "alias": target.get("alias"),
        "fakeid": target.get("fakeid"),
        "service_type": target.get("service_type"),
        "round_head_img": target.get("round_head_img"),
    },
    "collector": {"running": True, "session_account_id": account_id, "api_token_used": "test-key-2026"},
    "official_api": {
        "search_account": "ok",
        "user_articles_total": 0,
        "user_articles": [],
        "session_status": "有效（searchbiz 成功；非登录过期）",
    },
    "errors_and_limits": [
        "公众号后台 list_ex 对该号返回 app_msg_cnt=0，无法通过运营会话直接拉历史列表",
        "搜狗 /link 跳转页触发验证码，未能解析出 mp.weixin.qq.com 正文链接",
        "Brave 搜索补充链路 HTTP 429",
        "阅读/点赞等统计数据需自有号图文明细权限，他人号不可得",
    ],
    "articles_count": len(articles),
    "articles": articles,
}
path = r"d:\self\sy\运营数据平台\202606\wd\tmp_leisuapp_final.json"
json.dump(out, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
print(json.dumps({"count": len(articles), "path": path}, ensure_ascii=False))
for a in articles:
    print(a["create_time"], a["title"][:60])
