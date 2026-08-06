# WECHAT External Collect E2E — 2026-08-04 (run 2: 翕蔚热说-公众号 cookie)

> **Slice**: ADR-068 §2.2 addendum · `collect.external.wechat_official.cookie` → external unified collect  
> **Env**: Beta DB `110.42.49.224` / gateway `:48080` / ops-server `:48094` / collector `:8000`

## Summary

| Item | Result |
|------|--------|
| Target internal account | **翕蔚热说-公众号** · oa_account id **1000112** (bind only; row **missing** on beta `oa_account`) |
| Cookie source | **collector `accounts.db`** · `acc_wechat_mp_22556c49301482a3` (Fernet decrypt; bound to oa_id=1000112) |
| Beta `oa_account.cookie_encrypted` | **Not available** — id 1000112 absent; 0 WECHAT_OFFICIAL rows with cookie on beta |
| sys_param update | **Yes** — id=**23** · len **1174 → 1299** (masked `_qimei_u...record`) |
| Config 45 (`Agent_AI_Z`) | WECHAT_OFFICIAL · collect_enabled=true |
| Task 9 | ensure-external-unified OK · run OK |
| Config 45 collect | Credential path OK (no skip/1512) · **Cookie 失效** · recordCount=**0** |
| `oa_external_work` config 45 | **0** rows (WECHAT_OFFICIAL total **0**) |
| E2E smoke | **FAIL** (`wechat-collect-success` — expired session, not missing param) |

## 1. Account discovery

1. Beta `oa_account` has **no row** id=1000112 (account deleted or never synced to beta).
2. Orphan bind remains: `oa_collector_account_bind` id=**6** → oa_id=**1000112** · collector **`acc_wechat_mp_22556c49301482a3`** · `BOUND`.
3. Collector `accounts.db` holds encrypted credential for that session (nickname 翕蔚热说-公众号, `identity.oa_account_id=1000112`).
4. Alternate MP session `acc_wechat_mp_67304199be06eb3f` also present (oa_id=1000006) — not used (different account).

## 2. Cookie bridge

Script (local only, gitignored cookie output): `scripts/_tmp_xiwei_collector_cookie_to_param.py`

1. Decrypt Fernet credential from `unify-collector-api/data/accounts.db` using `.env` `CREDENTIAL_FERNET_KEY`.
2. Extract `credential.cookie` (len=1299; differs from stale `.env` `WECHAT_MP_COOKIE` len=1174).
3. `UPDATE sys_param` on beta for `collect.external.wechat_official.cookie` (tenant 1, id=23).
4. Meta written to `scripts/_tmp_wechat_cookie_meta.txt` (not committed).

## 3. Collect run (task 9)

- **ensure-external-unified**: OK (task id=9)
- **task run**: OK
- **log**: `PARTIAL` (DOUYIN member may succeed; WECHAT fails)
- **config:45** typeResult: `success=false`, `recordCount=0`, error *已保存登录态已失效，请重新扫码登录*
- **Interpretation**: sys_param bridge **works**; upstream WeChat session in collector DB is **stale**

## 4. Stack status

Integration stack **UP** during run (gateway `:48080`, ops `:48094`, collector `:8000`). No rebuild required.

## 5. Artifacts

| File | Purpose |
|------|---------|
| `smoke_wechat_external_collect_e2e.py` | Runnable smoke |
| `RESULTS.json` | Check matrix + masked cookie meta |
| `00-login.json` … `log-detail.json` | API snapshots (secrets redacted) |
| `REPORT.md` | This report |

## 6. Unblock for green E2E

1. **QR re-login** 翕蔚热说-公众号 via ops UI or `unify-collector-api/tools/local_qr_login.py wechat_mp` for bind `acc_wechat_mp_22556c49301482a3`.
2. Optionally restore beta `oa_account` id=1000112 with fresh `cookie_encrypted` + `mp_token_encrypted`.
3. Re-run `scripts/_tmp_xiwei_collector_cookie_to_param.py` to push fresh cookie into sys_param.
4. Re-run smoke — expect config **45** `success=true` and `oa_external_work` rows with `collect_config_id=45`.

## 7. Manual re-run

```powershell
python scripts/_tmp_xiwei_collector_cookie_to_param.py
python docs/delivery/e2e-artifacts/WECHAT-EXTERNAL-COLLECT-E2E-20260804/smoke_wechat_external_collect_e2e.py
```
