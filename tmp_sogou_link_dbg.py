import re, sys
sys.path.insert(0, r"d:\self\sy\运营数据平台\202606\wd\unify-collector-api")
from platforms.wechat_mp.scraper import fetch_article_html_via_browser
from platforms.wechat_mp.external_api import resolve_session
import urllib.parse
sess=resolve_session("acc_wechat_mp_440f02d51e2c8441")
q=urllib.parse.quote("雷速体育APP")
search_url=f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8"
html=fetch_article_html_via_browser(search_url,cookie=sess["cookie"],headless=True,stealth=True)
m=re.search(r'href="(/link\?url=[^"]+)"[^>]*id="sogou_vr_11002601_title_0"', html)
href="https://weixin.sogou.com"+m.group(1).replace("&amp;","&")
page=fetch_article_html_via_browser(href,cookie=sess["cookie"],headless=True,stealth=True)
open(r"d:\self\sy\运营数据平台\202606\wd\tmp_sogou_link.html","w",encoding="utf-8").write(page)
print("len",len(page))
print("title", re.search(r"<title>(.*?)</title>", page, re.S).group(1) if re.search(r"<title>", page) else "")
for kw in ["mp.weixin","antispider","验证码","环境异常","js_content","location.href","url +"]:
    print(kw, page.count(kw))
