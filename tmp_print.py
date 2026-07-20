import json
d=json.load(open(r"d:\self\sy\运营数据平台\202606\wd\tmp_leisuapp_final.json",encoding="utf-8"))
for i,a in enumerate(d["articles"],1):
    print(str(i)+". "+a["create_time"]+" | "+a["title"])
