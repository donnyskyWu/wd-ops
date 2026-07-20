import re, urllib.parse, json
from curl_cffi import requests
q = urllib.parse.quote("site:mp.weixin.qq.com leisuapp")
for engine, url in [
 ("ddg", f"https://html.duckduckgo.com/html/?q={q}"),
 ("bing", f"https://www.bing.com/search?q={q}"),
]:
    try:
        r = requests.get(url, headers={"User-Agent":"Mozilla/5.0"}, impersonate="chrome", timeout=30)
        r.encoding="utf-8"
        links = re.findall(r"https://mp\.weixin\.qq\.com/[^\"'&\s<]+", r.text)
        print(engine, "status", r.status_code, "links", len(links))
        for l in links[:5]:
            print(" ", l[:120])
    except Exception as e:
        print(engine, "err", e)
