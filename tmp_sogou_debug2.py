# -*- coding: utf-8 -*-
import re, urllib.parse
from curl_cffi import requests
q = urllib.parse.quote("雷速体育APP")
url = f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8"
r = requests.get(url, headers={"User-Agent":"Mozilla/5.0"}, impersonate="chrome", timeout=30)
r.encoding="utf-8"
html = r.text
idx = html.find("sogou_vr")
open(r"d:\self\sy\运营数据平台\202606\wd\tmp_sogou_sample.html","w",encoding="utf-8").write(html[idx:idx+8000] if idx>=0 else html[:8000])
print("written", idx, "len", len(html))
# find all uigs values
uigs = set(re.findall(r'uigs="([^"]+)"', html))
print("uigs", sorted(uigs)[:30], "total", len(uigs))
