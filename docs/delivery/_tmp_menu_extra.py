
import subprocess
from collections import defaultdict

# reuse globals from prior script run - re-exec full script inline by reading file and extending
exec(open(r"d:\self\sy\运营数据平台\202606\wd\docs\delivery\_tmp_menu_diff.py", encoding="utf-8").read().split("print(f\"baseline")[0])

b = {k: v for k, v in baseline.items() if k < 6100}
l = {k: v for k, v in latest.items() if k < 6100}
added_ids = sorted(set(l) - set(b))

new_roots = sorted([i for i in added_ids if l[i]["parent_id"]==0 and l[i]["type"] in (1,2)], key=lambda x: l[x]["sort"])
print("NEW_TOP_LEVEL")
for i in new_roots:
    print(f"{i}|{l[i]['name']}|{l[i]['path']}")

for kw in ["渠道", "个微", "企微", "销售", "合伙人", "营销", "财务", "直播", "站点", "作者"]:
    hits = [(i,l[i]["name"],l[i]["path"]) for i in added_ids if kw in l[i]["name"]]
    if hits:
        print(f"KW_{kw}={len(hits)}")
        for h in hits[:10]:
            print(" ", h)

ids=",".join(str(i) for i in added_ids)
r=subprocess.run(["mysql","-uroot","-proot","wd","-N","-e",f"SELECT COUNT(*) FROM system_menu WHERE id IN ({ids}) AND deleted=0;"],capture_output=True,text=True,encoding="utf-8")
print("DB_ADDED_PRESENT", r.stdout.strip())
r2=subprocess.run(["mysql","-uroot","-proot","wd","-N","-e","SELECT COUNT(*) FROM system_menu WHERE id<6100 AND deleted=0;"],capture_output=True,text=True,encoding="utf-8")
print("DB_FOOTBALL_TOTAL", r2.stdout.strip())
