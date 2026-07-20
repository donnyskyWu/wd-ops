# -*- coding: utf-8 -*-
import re, urllib.parse
from curl_cffi import requests
q = urllib.parse.quote("雷速体育APP")
url = f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8"
r = requests.get(url, headers={"User-Agent":"Mozilla/5.0"}, impersonate="chrome", timeout=30)
r.encoding="utf-8"
print("status", r.status_code, "len", len(r.text))
print("captcha", "验证码" in r.text or "antispider" in r.text)
print("titles", len(re.findall(r"article_title_", r.text)))
m = re.search(r'uigs="article_title_0"[^>]*href="([^"]+)"', r.text)
print("first href", m.group(1)[:80] if m else None)
# order of attrs
m2 = re.search(r'<h3>\s*<a([^>]*article_title_0[^>]*)>', r.text)
print("attrs sample", m2.group(1)[:200] if m2 else "no h3")
