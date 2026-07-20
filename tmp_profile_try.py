# -*- coding: utf-8 -*-
import re, json, base64, html as htmlmod, urllib.parse, time
from curl_cffi import requests

def decode_fakeid(fid):
    try:
        return base64.b64decode(fid + "==").decode("utf-8", errors="ignore")
    except Exception:
        return fid

fakeid = "Mzg5ODkzNDc2NQ=="
print("decoded", decode_fakeid(fakeid.replace("=","")))

# sogou account search type=1
for query in ["leisuapp", "雷速体育APP"]:
    q = urllib.parse.quote(query)
    url = f"https://weixin.sogou.com/weixin?type=1&query={q}&ie=utf8"
    r = requests.get(url, headers={"User-Agent":"Mozilla/5.0"}, impersonate="chrome", timeout=30)
    r.encoding="utf-8"
    print("type1", query, "status", r.status_code)
    for m in re.finditer(r'uigs="account_name_\d+"[^>]*>(.*?)</a>', r.text, re.S):
        print(" ", htmlmod.unescape(re.sub(r"<[^>]+>","",m.group(1)).strip()))
    for m in re.finditer(r'uigs="account_text_\d+"[^>]*>(.*?)</a>', r.text, re.S):
        print(" text", htmlmod.unescape(re.sub(r"<[^>]+>","",m.group(1)).strip())[:60])

# profile ext
profile = f"https://mp.weixin.qq.com/mp/profile_ext?action=home&__biz={urllib.parse.quote(fakeid)}&scene=124#wechat_redirect"
r2 = requests.get(profile, headers={"User-Agent":"Mozilla/5.0"}, impersonate="chrome", timeout=30)
print("profile status", r2.status_code, "len", len(r2.text))
print("profile snippet", r2.text[:400].replace("\n"," "))
for pat in [r"msg_list", r"appmsg", r"nick_name", r"verify"]:
    print(pat, bool(re.search(pat, r2.text)))
