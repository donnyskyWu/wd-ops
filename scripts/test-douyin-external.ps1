# Test Douyin external collector APIs
param(
    [string]$Base = "http://127.0.0.1:8000",
    [string]$Token = "test-key-2026",
    [string]$SecUid = "MS4wLjABAAAAf-F4Se2zQt0kXM4WGHU_CR0wuBlxm_NLWLvEJqlppzw",
    [string]$Keyword = "test"
)

$headers = @{ Authorization = "Bearer $Token" }

function Test-Endpoint($Name, $Url) {
    Write-Host "`n=== $Name ===" -ForegroundColor Cyan
    Write-Host "GET $Url"
    try {
        $resp = Invoke-WebRequest -Uri $Url -Headers $headers -UseBasicParsing -TimeoutSec 60
        $json = $resp.Content | ConvertFrom-Json
        if ($json.code -eq 0) {
            Write-Host "[OK] $($json.message)" -ForegroundColor Green
            $preview = ($json.data | ConvertTo-Json -Depth 4 -Compress)
            if ($preview.Length -gt 400) { $preview = $preview.Substring(0, 400) + "..." }
            Write-Host $preview
        } else {
            Write-Host "[FAIL] code=$($json.code) $($json.message)" -ForegroundColor Red
        }
    } catch {
        $body = $_.ErrorDetails.Message
        if ($body) { Write-Host "[FAIL] $body" -ForegroundColor Red }
        else { Write-Host "[FAIL] $($_.Exception.Message)" -ForegroundColor Red }
    }
}

Write-Host "Douyin external API smoke test" -ForegroundColor Yellow
Write-Host "Base: $Base  SecUid: $SecUid"

Test-Endpoint "user-profile" "$Base/api/v1/external/douyin/user-profile?sec_uid=$SecUid"
Test-Endpoint "user-videos" "$Base/api/v1/external/douyin/user-videos?sec_uid=$SecUid&page_size=3"
Test-Endpoint "search-video" "$Base/api/v1/external/douyin/search-video?keyword=$Keyword&page_size=3"
