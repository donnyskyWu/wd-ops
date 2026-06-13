$headers = @{
  "Authorization" = "Bearer dev-token-oa-admin"
  "X-Tenant-Id"   = "1"
  "Content-Type"  = "application/json"
}

function Test-Api($name, $method, $url, $body) {
  Write-Host "=== $name ==="
  try {
    if ($method -eq "GET") {
      $r = Invoke-RestMethod -Uri $url -Headers $headers -Method GET
    } elseif ($method -eq "PUT") {
      $r = Invoke-RestMethod -Uri $url -Headers $headers -Method PUT -Body $body
    } else {
      $r = Invoke-RestMethod -Uri $url -Headers $headers -Method $method -Body $body
    }
    Write-Host "OK code=$($r.code)"
    return $r
  } catch {
    if ($_.Exception.Response) {
      $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
      Write-Host "FAIL $($_.Exception.Response.StatusCode) $($sr.ReadToEnd())"
    } else {
      Write-Host "FAIL $($_.Exception.Message)"
    }
    return $null
  }
}

Test-Api "hello" GET "http://localhost:8080/admin-api/oa/hello" $null | Out-Null
Test-Api "match/list" GET "http://localhost:8080/admin-api/oa/match/list?date=2026-06-12&pageNo=1&pageSize=5" $null | Out-Null
Test-Api "plan/list" GET "http://localhost:8080/admin-api/oa/plan/list?pageNo=1&pageSize=5" $null | Out-Null
Test-Api "content/list" GET "http://localhost:8080/admin-api/oa/content/list?pageNo=1&pageSize=5" $null | Out-Null
Test-Api "task/my-tasks" GET "http://localhost:8080/admin-api/oa/task/my-tasks?pageNum=1&pageSize=5" $null | Out-Null

$planBody = @'
{"planName":"SMOKE-计划","templateId":9401,"ipGroupId":9001,"startDate":"2026-06-01","endDate":"2026-06-30","competitions":[{"competitionId":"cmp-smoke","competitionName":"冒烟赛事"}],"steps":[{"nodeId":9402,"competitionId":"cmp-smoke","assigneeIds":[1001]}]}
'@
$createPlan = Test-Api "plan/create" POST "http://localhost:8080/admin-api/oa/plan/create" $planBody
if ($createPlan) {
  $planId = $createPlan.data
  $updateBody = "{`"id`":$planId,`"planName`":`"SMOKE-计划-已改`",`"templateId`":9401,`"ipGroupId`":9001,`"startDate`":`"2026-06-01`",`"endDate`":`"2026-06-30`",`"competitions`":[{`"competitionId`":`"cmp-smoke`",`"competitionName`":`"冒烟赛事`"}],`"steps`":[{`"nodeId`":9402,`"competitionId`":`"cmp-smoke`",`"assigneeIds`":[1001]}]}"
  Test-Api "plan/update" PUT "http://localhost:8080/admin-api/oa/plan/update" $updateBody | Out-Null
  Test-Api "plan/start" POST "http://localhost:8080/admin-api/oa/plan/$planId/start" "{}" | Out-Null
}

$tasks = Test-Api "task/list" GET "http://localhost:8080/admin-api/oa/task/list?pageNum=1&pageSize=20" $null
if ($tasks -and $tasks.data.list.Count -gt 0) {
  $task = $tasks.data.list | Where-Object { $_.nodeName -match "9402|文案|生成" -or $_.status -eq "PENDING" } | Select-Object -First 1
  if (-not $task) { $task = $tasks.data.list[0] }
  $taskId = $task.id
  $exec = Test-Api "task/execute" GET "http://localhost:8080/admin-api/oa/task/$taskId/execute" $null
  if ($exec -and $exec.data.executionInstruction) {
    Write-Host "  instruction=$($exec.data.executionInstruction.Substring(0, [Math]::Min(50, $exec.data.executionInstruction.Length)))"
    Write-Host "  attachments=$($exec.data.attachments.Count)"
  }
  $contentBody = "{`"title`":`"SMOKE-内容`",`"body`":`"正文`",`"taskId`":$taskId,`"competitionId`":`"cmp-smoke`",`"documentType`":`"SHORT_VIDEO`",`"platformType`":`"DOUYIN`",`"accountId`":9101,`"contentType`":`"ARTICLE`"}"
  $content = Test-Api "content/create" POST "http://localhost:8080/admin-api/oa/content/create" $contentBody
  if ($content) {
    $contentId = $content.data
    Test-Api "content/confirm" POST "http://localhost:8080/admin-api/oa/content/$contentId/confirm" "{}" | Out-Null
    Test-Api "task/execute/complete" POST "http://localhost:8080/admin-api/oa/task/$taskId/execute/complete" "{}" | Out-Null
  }
}

Write-Host "=== DONE ==="
