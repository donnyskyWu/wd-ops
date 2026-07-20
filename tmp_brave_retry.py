import time, json, sys
sys.path.insert(0, r"d:\self\sy\运营数据平台\202606\wd\unify-collector-api")
from platforms.wechat_mp.scraper import search_wechat_articles_brave, normalize_text, match_account_name

try:
    arts = search_wechat_articles_brave("leisuapp", fetch_count=30, variants=["leisuapp","雷速体育APP","雷速体育APP 公众号"])
    print("total", len(arts))
    filtered=[]
    for a in arts:
        src = a.get("source","")
        m = match_account_name("雷速体育APP", src)
        if m in ("exact","contains") or "leisu" in normalize_text(src):
            filtered.append(a)
    print("filtered", len(filtered))
    for a in filtered[:10]:
        print(json.dumps(a, ensure_ascii=False))
except Exception as e:
    print("ERR", e)
