import json
d=json.load(open(r"d:\self\sy\运营数据平台\202606\wd\tmp_leisuapp_final.json",encoding="utf-8"))
lines=[]
lines.append("账号: "+d["account"]["nickname"]+" / "+d["account"]["alias"]+" / "+d["account"]["fakeid"])
for i,a in enumerate(d["articles"],1):
    lines.append(f"{i}. {a['create_time']} | {a['title']}")
    lines.append("   摘要: "+a["digest"][:120])
open(r"d:\self\sy\运营数据平台\202606\wd\tmp_leisuapp_summary.txt","w",encoding="utf-8").write("\n".join(lines))
