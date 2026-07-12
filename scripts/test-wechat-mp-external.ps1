# WeChat MP external API smoke test
param(
    [string]$Base = "http://127.0.0.1:8000",
    [string]$Token = "test-key-2026",
    [string]$Keyword = "rmrb",
    [string]$AccountId = "",
    [string]$FakeId = "",
    [string]$WeChatCookie = "",
    [string]$WeChatToken = ""
)

$headers = @("Authorization: Bearer $Token")
if ($WeChatCookie -and $WeChatToken) {
    $headers += "X-WeChat-Cookie: $WeChatCookie"
    $headers += "X-WeChat-Token: $WeChatToken"
}

function Invoke-Test($Name, $Url) {
    Write-Host "`n=== $Name ===" -ForegroundColor Cyan
    Write-Host "GET $Url"
    $args = @("curl.exe", "-s")
    foreach ($h in $headers) { $args += @("-H", $h) }
    $args += $Url
    $raw = & $args[0] $args[1..($args.Length-1)]
    try {
        $json = $raw | ConvertFrom-Json
        if ($json.code -eq 0) {
            Write-Host "[OK] $($json.message)" -ForegroundColor Green
            $preview = ($json.data | ConvertTo-Json -Depth 5 -Compress)
            if ($preview.Length -gt 600) { $preview = $preview.Substring(0, 600) + "..." }
            Write-Host $preview
        } else {
            Write-Host "[FAIL] code=$($json.code) $($json.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "[FAIL] $raw" -ForegroundColor Red
    }
}

Write-Host "WeChat MP external API smoke test" -ForegroundColor Yellow
Write-Host "Base: $Base  Keyword: $Keyword"
if (-not $AccountId -and -not ($WeChatCookie -and $WeChatToken)) {
    Write-Host "Tip: import session first:" -ForegroundColor DarkYellow
    Write-Host "  POST $Base/api/v1/internal/wechat-mp/login/cookie"
    Write-Host "  Headers: X-WeChat-Token, X-WeChat-Cookie (from mp.weixin.qq.com)"
}

$kw = [uri]::EscapeDataString($Keyword)
$acc = if ($AccountId) { "&account_id=$([uri]::EscapeDataString($AccountId))" } else { "" }

Invoke-Test "search-account" "$Base/api/v1/external/wechat-mp/search-account?keyword=$kw&count=3$acc"
if ($FakeId) {
    $fid = [uri]::EscapeDataString($FakeId)
    Invoke-Test "user-articles" "$Base/api/v1/external/wechat-mp/user-articles?fakeid=$fid&page_size=5$acc"
} else {
    Write-Host "`n=== user-articles ===" -ForegroundColor Cyan
    Write-Host "[SKIP] set -FakeId after search-account returns fakeid"
}
Invoke-Test "search-content" "$Base/api/v1/external/wechat-mp/search-content?keyword=$kw&account_limit=2&articles_per_account=3$acc"
