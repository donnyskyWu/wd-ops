import re, sys, urllib.parse, base64
sys.path.insert(0, r"d:\self\sy\运营数据平台\202606\wd\unify-collector-api")
from platforms.wechat_mp.scraper import fetch_article_html_via_browser
from platforms.wechat_mp.external_api import resolve_session
sess=resolve_session("acc_wechat_mp_440f02d51e2c8441")
q=urllib.parse.quote("site:mp.weixin.qq.com leisuapp")
url=f"https://www.bing.com/search?q={q}&count=20"
html=fetch_article_html_via_browser(url,cookie=sess["cookie"],headless=True,stealth=True)
# bing redirect links
hrefs=re.findall(r'href="([^"]+)"', html)
mp=[]
for h in hrefs:
    if "mp.weixin.qq.com" in h:
        mp.append(h)
    if "bing.com/ck/a" in h and "u=" in h:
        m=re.search(r'u=([^&]+)', h)
        if m:
            try:
                dec=urllib.parse.unquote(m.group(1))
                if dec.startswith("a1"): # base64?
                    pass
                if "mp.weixin.qq.com" in dec:
                    mp.append(dec)
            except: pass
print("mp count", len(set(mp)))
for u in list(dict.fromkeys(mp))[:20]:
    print(u[:150])
# also search encoded
for m in re.finditer(r'mp\.weixin\.qq\.com[^\"\s<]{0,120}', html):
    print("frag", m.group(0)[:120])
