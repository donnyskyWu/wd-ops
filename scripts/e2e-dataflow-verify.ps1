# E2E Dataflow API Verification | TRACE=E2E-DF-20260611
$Base = "http://localhost:8080/admin-api/oa"
$Headers = @{
  "Authorization" = "Bearer dev-token-oa-admin"
  "X-Tenant-Id"    = "1"
}
$Trace = "E2E-DF"
$AccountId = 91001
$IpGroupId = 92001
$AuthorId = 93001
$script:Results = @()

function Test-Step($No, $Name, $Url, $Predicate) {
  try {
    $r = Invoke-RestMethod -Uri $Url -Headers $Headers -Method GET -TimeoutSec 30
    $ok = ($r.code -eq 0) -and (& $Predicate $r.data)
    $script:Results += [pscustomobject]@{ Step=$No; Name=$Name; Pass=$ok; Detail=$(if($ok){"OK"}else{"predicate fail code=$($r.code)"}) }
    Write-Host ("[{0}] {1} {2}" -f $(if($ok){"PASS"}else{"FAIL"}), $No, $Name)
  } catch {
    $script:Results += [pscustomobject]@{ Step=$No; Name=$Name; Pass=$false; Detail=$_.Exception.Message }
    Write-Host ("[FAIL] {0} {1} {2}" -f $No, $Name, $_.Exception.Message)
  }
}

Test-Step "01" "internal-collect" "$Base/config/internal-collect/list?platformType=WECHAT_VIDEO&pageNo=1&pageSize=50" {
  param($d); ($d.list | Where-Object { $_.accountId -eq 91001 -or $_.configName -like "*E2E-DF*" }).Count -ge 1
}

Test-Step "05" "account-analysis" "$Base/account-analysis/$AccountId/overview" {
  param($d); $null -ne $d
}

Test-Step "03" "ip-group" "$Base/ip-group/$IpGroupId" {
  param($d); $d.groupName -like "*E2E-DF*"
}

Test-Step "04" "author" "$Base/author/$AuthorId" {
  param($d); $d.authorName -like "*E2E-DF*"
}

Test-Step "06" "works-analysis" "$Base/content-analysis/list?accountId=$AccountId&pageNo=1&pageSize=20" {
  param($d); ($d.list | Where-Object { $_.title -like "*E2E-DF*" }).Count -ge 1
}

Test-Step "07" "internal-content" "$Base/internal-content/list?accountId=$AccountId&pageNo=1&pageSize=20" {
  param($d); ($d.list | Where-Object { $_.title -like "*E2E-DF*" }).Count -ge 1
}

Test-Step "09" "finance-cost" "$Base/finance/cost/list?accountId=$AccountId&pageNo=1&pageSize=20" {
  param($d); ($d.list | Where-Object { $_.remark -like "*E2E-DF*" }).Count -ge 1
}

Test-Step "10" "roi-report" "$Base/report/roi/list?ipGroupId=$IpGroupId&startDate=2026-06-01&endDate=2026-06-30&pageNo=1&pageSize=20" {
  param($d); $null -ne $d.list
}

Test-Step "11" "metrics" "$Base/metric/list?pageNo=1&pageSize=100" {
  param($d); ($d.list | Where-Object { $_.metricCode -like "E2E_DF_*" }).Count -ge 4
}

try {
  $body = '{"metricFormula":"SELECT COALESCE(MAX(f.follower_count),0) AS cnt FROM oa_follower_daily f WHERE f.tenant_id = 1 AND f.deleted = 0 AND f.account_id = 91001"}'
  $r = Invoke-RestMethod -Uri "$Base/metric/preview" -Headers $Headers -Method POST -Body $body -ContentType "application/json"
  $ok = ($r.code -eq 0) -and ($r.data.rows[0].cnt -ge 2000000)
  $script:Results += [pscustomobject]@{ Step="12"; Name="metric-preview"; Pass=$ok; Detail=$(if($ok){"cnt>=2M"}else{"fail"}) }
  Write-Host ("[{0}] 12 metric-preview" -f $(if($ok){"PASS"}else{"FAIL"}))
} catch {
  $script:Results += [pscustomobject]@{ Step="12"; Name="metric-preview"; Pass=$false; Detail=$_.Exception.Message }
}

$reportUrls = @{
  "13a" = "$Base/report/unified-account/list?ipGroupId=$IpGroupId&pageNo=1&pageSize=20"
  "13b" = "$Base/report/account-status/summary?ipGroupId=$IpGroupId"
  "13c" = "$Base/report/video-output/list?ipGroupId=$IpGroupId&pageNo=1&pageSize=20"
  "13d" = "$Base/report/live-duration/list?ipGroupId=$IpGroupId&pageNo=1&pageSize=20"
  "13e" = "$Base/report/cost-allocation/list?ipGroupId=$IpGroupId&pageNo=1&pageSize=20"
  "13f" = "$Base/report/roi/list?ipGroupId=$IpGroupId&startDate=2026-06-01&endDate=2026-06-30&pageNo=1&pageSize=20"
  "13g" = "$Base/report/team-config/list?ipGroupId=$IpGroupId&pageNo=1&pageSize=20"
  "13h" = "$Base/report/account-alert/list?ipGroupId=$IpGroupId&pageNo=1&pageSize=20"
}
foreach ($k in $reportUrls.Keys) {
  Test-Step $k "report-$k" $reportUrls[$k] { param($d); $null -ne $d }
}

Test-Step "14" "funnel" "$Base/funnel/99301/data" {
  param($d); $d.steps.Count -ge 1
}

Test-Step "15" "custom-query" "$Base/query/list?pageNo=1&pageSize=20" {
  param($d); $null -ne $d.list
}

Test-Step "16a" "hot-works" "$Base/monitor/hit/list?platformType=WECHAT_VIDEO&pageNum=1&pageSize=20" {
  param($d); ($d.list | Where-Object { $_.title -like "*E2E-DF*" }).Count -ge 1
}

Test-Step "16b" "high-follower" "$Base/monitor/high-follower/list?platformType=WECHAT_VIDEO&ipGroupId=$IpGroupId&pageNum=1&pageSize=20" {
  param($d); ($d.list | Where-Object { $_.accountId -eq 91001 -and $_.followerCount -ge 2000000 }).Count -ge 1
}

Test-Step "17" "dashboard" "$Base/home/dashboard/overview?ipGroupId=$IpGroupId" {
  param($d); $null -ne $d
}

Test-Step "18" "operation-log" "$Base/system/log/operation?pageNo=1&pageSize=50" {
  param($d); $d.list.Count -ge 1
}

Write-Host ""
$script:Results | Format-Table -AutoSize
$pass = ($script:Results | Where-Object { $_.Pass }).Count
Write-Host "Passed: $pass / $($script:Results.Count)"
