import json, urllib.parse, sys
sys.path.insert(0, r"d:\self\sy\运营数据平台\202606\wd\unify-collector-api")
from curl_cffi import requests
from platforms.wechat_mp.external_api import resolve_session
biz="Mzg5ODkzNDc2NQ=="
sess=resolve_session("acc_wechat_mp_440f02d51e2c8441")
url=f"https://mp.weixin.qq.com/mp/profile_ext?action=getmsg&__biz={urllib.parse.quote(biz)}&f=json&offset=0&count=10&is_ok=1&scene=124&uin=777&key=777&pass_ticket=&wxtoken=&appmsg_token=&x5=0"
headers={
 "User-Agent":"Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.42(0x18002a2f) NetType/WIFI Language/zh_CN",
 "Cookie": sess.get("cookie",""),
}
r=requests.get(url,headers=headers,impersonate="chrome",timeout=30)
print("status",r.status_code)
text=r.text
print(text[:1500])
try:
    data=r.json()
    print("keys", data.keys())
    gl=data.get("general_msg_list","")
    if isinstance(gl,str):
        glj=json.loads(gl)
        print("msgs", len(glj.get("list",[])))
        for item in glj.get("list",[])[:3]:
            print(item.keys())
except Exception as e:
    print("json err", e)
