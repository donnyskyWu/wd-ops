# run-football-front-dev.ps1 — start football-front Vite on :5777 (Gate UI)
#
# Fallback when node_modules/.bin/vite is missing (Windows EPERM during pnpm install)
# but vite package exists under node_modules/.pnpm (common after partial install).

param(
    [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"
$frontDir = Join-Path $Root "football-front"
$webEle = Join-Path $frontDir "apps\web-ele"

if (-not (Test-Path $webEle)) {
    Write-Error "football-front apps/web-ele not found: $webEle"
    exit 1
}

$viteBin = Join-Path $frontDir "node_modules\.bin\vite.cmd"
$usePnpm = $false
if ((Test-Path $viteBin) -and (Get-Command pnpm -ErrorAction SilentlyContinue)) {
    Push-Location $frontDir
    try {
        & pnpm exec vite --version 2>$null | Out-Null
        $usePnpm = ($LASTEXITCODE -eq 0)
    } catch {
        $usePnpm = $false
    } finally {
        Pop-Location
    }
}
if ($usePnpm) {
    Set-Location $frontDir
    & pnpm dev:ele
    exit $LASTEXITCODE
}

$pnpmdir = Join-Path $frontDir "node_modules\.pnpm"
$viteJs = Get-ChildItem -Path $pnpmdir -Recurse -Filter "vite.js" -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -match '\\vite\\bin\\vite\.js$' -and $_.FullName -match 'vite@7\.1\.11' -and $_.FullName -match '@types\+node@22' } |
    Select-Object -First 1
if (-not $viteJs) {
    $viteJs = Get-ChildItem -Path $pnpmdir -Recurse -Filter "vite.js" -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '\\vite\\bin\\vite\.js$' -and $_.FullName -match 'vite@7' } |
        Sort-Object FullName -Descending |
        Select-Object -First 1
}

if (-not $viteJs) {
    Write-Error @"
vite not found under football-front/node_modules.
Fix: cd football-front && pnpm install
     (if EPERM on Windows, close apps locking node_modules and retry)
"@
    exit 1
}

Write-Host "[fallback] starting vite via $($viteJs.FullName)"
Set-Location $webEle
& node $viteJs.FullName --mode development --port 5777 --strictPort
exit $LASTEXITCODE
