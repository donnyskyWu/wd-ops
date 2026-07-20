import json
d=json.load(open(r"d:\self\sy\运营数据平台\202606\wd\tmp_leisuapp_final.json",encoding="utf-8"))
lines=[]
for i,a in enumerate(d["articles"],1):
    lines.append(f"{i}. {a['create_time']} {ascii(a['title'])}")
    lines.append(f"   digest: {ascii(a['digest'][:100])}")
open(r"d:\self\sy\运营数据平台\202606\wd\tmp_titles_ascii.txt","w",encoding="utf-8").write("\n".join(lines))
