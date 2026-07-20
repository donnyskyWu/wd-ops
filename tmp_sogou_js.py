# -*- coding: utf-8 -*-
import re, urllib.parse
from curl_cffi import requests
q = urllib.parse.quote("雷速体育APP")
url = f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8"
r = requests.get(url, headers={"User-Agent":"Mozilla/5.0"}, impersonate="chrome", timeout=30)
r.encoding="utf-8"
for pat in ["js_url","url.replace","bili","eval","charset"]:
    print(pat, len(re.findall(pat, r.text)))
# save js snippets
scripts = re.findall(r"<script[^>]*>(.*?)</script>", r.text, re.S)
print("scripts", len(scripts))
for i,s in enumerate(scripts):
    if "url" in s and len(s)>50 and len(s)<5000:
        print("--- script", i, "len", len(s))
        print(s[:800])
