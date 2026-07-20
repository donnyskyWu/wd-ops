from curl_cffi import requests
import urllib.parse, re
q=urllib.parse.quote("雷速体育APP")
url=f"https://weixin.sogou.com/weixin?type=2&query={q}&ie=utf8"
r=requests.get(url,headers={"User-Agent":"Mozilla/5.0"},impersonate="chrome",timeout=30)
r.encoding="utf-8"
print(r.status_code,len(r.text))
print("article_title",r.text.count("article_title"))
print("antispider", "antispider" in r.text)
open(r"d:\self\sy\运营数据平台\202606\wd\tmp_sogou_full.html","w",encoding="utf-8").write(r.text)
