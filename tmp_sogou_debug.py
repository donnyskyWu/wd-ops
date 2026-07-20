# -*- coding: utf-8 -*-
import re, json, html as htmlmod, urllib.parse
from curl_cffi import requests
q = urllib.parse.quote("雷速体育APP")
url = f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8"
r = requests.get(url, headers={"User-Agent":"Mozilla/5.0"}, impersonate="chrome", timeout=30)
r.encoding="utf-8"
html = r.text
# dump account names found
for m in re.finditer(r'uigs="account_name_\d+"[^>]*>(.*?)</a>', html, re.S):
    acct = re.sub(r"<[^>]+>", "", m.group(1))
    print("ACCT:", htmlmod.unescape(acct.strip()))
# count li blocks
print("li count", len(re.findall(r'<li id="sogou_vr_', html)))
# alternative account pattern
for m in re.finditer(r'class="account"[^>]*>(.*?)</div>', html, re.S):
    print("DIV", re.sub(r"\s+"," ", htmlmod.unescape(re.sub(r"<[^>]+>","",m.group(1)).strip()))[:80])
