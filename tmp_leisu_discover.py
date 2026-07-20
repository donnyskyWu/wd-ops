import sys, json, time, urllib.parse
sys.path.insert(0, r"d:\self\sy\运营数据平台\202606\wd\unify-collector-api")
from platforms.wechat_mp.external_api import resolve_session, search_accounts
from platforms.wechat_mp.scraper import discover_articles_by_account_search, discover_articles_by_account

account_id = "acc_wechat_mp_440f02d51e2c8441"
session = resolve_session(account_id)
acct = search_accounts("leisuapp", session, count=5)
print("=== search_accounts ===")
print(json.dumps(acct, ensure_ascii=False, indent=2))

target = None
for a in acct.get("list", []):
    if a.get("alias") == "leisuapp":
        target = a
        break
print("=== target ===", json.dumps(target, ensure_ascii=False))

auth = discover_articles_by_account("雷速体育APP", session, limit=10)
print("=== discover authenticated ===")
print(json.dumps(auth, ensure_ascii=False, indent=2)[:3000])

search = discover_articles_by_account_search(
    "雷速体育APP",
    limit=10,
    preferred_names=["雷速体育APP", "leisuapp"],
    allow_fuzzy=False,
)
print("=== discover search ===")
print(json.dumps(search, ensure_ascii=False, indent=2)[:5000])
