import re, json, time
from urllib.parse import quote, unquote
from curl_cffi import requests

queries = [
    ("yahoo", f"https://search.yahoo.com/search?p={quote('site:mp.weixin.qq.com leisuapp')}"),
    ("qwant", f"https://www.qwant.com/?q={quote('site:mp.weixin.qq.com leisuapp')}&t=web"),
    ("ecosia", f"https://www.ecosia.org/search?q={quote('site:mp.weixin.qq.com 雷速体育APP')}"),
    ("google", f"https://www.google.com/search?q={quote('site:mp.weixin.qq.com leisuapp')}&num=15"),
]
for name, url in queries:
    try:
        r = requests.get(url, headers={"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}, impersonate="chrome", timeout=30)
        text = r.text
        links = sorted(set(re.findall(r"https://mp\\.weixin\\.qq\\.com/s[^\"'&\s<>]+", text)))
        links = [unquote(l).replace("&amp;","&") for l in links]
        print(name, r.status_code, "links", len(links))
        for l in links[:8]:
            print(" ", l[:130])
    except Exception as e:
        print(name, "err", e)
    time.sleep(2)
