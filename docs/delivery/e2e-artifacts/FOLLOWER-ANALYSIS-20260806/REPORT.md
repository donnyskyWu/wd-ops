# FOLLOWER-ANALYSIS E2E — 20260806

## Root cause
`FollowerAnalysisServiceImpl` only queried `oa_follower_daily`. Collected follower data lives in
`account_status_log` / platform follower tables and was already used by `AccountAnalysisServiceImpl`
via `CollectedDataQueryService`, but not by the standalone 粉丝分析 page.

WeChat official accounts (mp_account SSOT, e.g. id 1000006) were also missing from bulk account
resolution because `resolveAccountIds` did not use `WechatOfficialAccountResolver`.

## Fix
- Merge collected follower stats when `oa_follower_daily` is empty for an account (same as account analysis).
- Resolve single/bulk account IDs via `WechatOfficialAccountResolver` + `PlatformAccountService` for WECHAT_OFFICIAL.

## API smoke (5/5 pass)
- [PASS] `follower-analysis-list` total=6 rows=6
- [PASS] `follower-analysis-stats` total=None rows=None
- [PASS] `follower-analysis-trend` total=None rows=6
- [PASS] `follower-analysis-account-1000006` total=1 rows=1
- [PASS] `account-analysis-followers-1000006` total=None rows=1

## Manual UI verify
1. Login http://localhost:5777 — admin / admin123, tenant 1
2. 运营管理 → 粉丝分析
3. Default 近30日 range → KPI cards and 粉丝趋势/粉丝列表 should show rows (not all zeros)
4. Optional: filter IP组 功能测试A → should include account 1000006 with followerCount=3
