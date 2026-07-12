# Test connectivity to remote MySQL 101.37.161.136 (read-only ping).
# Does NOT modify data. Set credentials via env or params.
param(
  [string]$DbHost = '101.37.161.136',
  [int]$Port = 3306,
  [string]$User = $env:OA_DB_USER,
  [string]$Password = $env:OA_DB_PASSWORD,
  [string[]]$Databases = @('wd', 'shenyu-member', 'shenyu-mp', 'shenyu-pay', 'shenyu-system')
)
$ErrorActionPreference = 'Stop'

if (-not $User) { $User = 'shenyu' }
if (-not $Password) {
  Write-Host "WARN: OA_DB_PASSWORD not set; connection may fail." -ForegroundColor Yellow
}

Write-Host "=== Remote MySQL connectivity: ${DbHost}:${Port} ==="

# TCP reachability
$tcp = Test-NetConnection -ComputerName $DbHost -Port $Port -WarningAction SilentlyContinue
if (-not $tcp.TcpTestSucceeded) {
  Write-Host "FAIL: TCP ${DbHost}:${Port} unreachable" -ForegroundColor Red
  exit 1
}
Write-Host "OK: TCP ${DbHost}:${Port} reachable"

# MySQL client ping (if mysql in PATH)
$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysql) {
  Write-Host "SKIP: mysql client not in PATH; TCP-only check passed" -ForegroundColor Yellow
  exit 0
}

$env:MYSQL_PWD = $Password
foreach ($db in $Databases) {
  try {
    $out = & mysql -h $DbHost -P $Port -u $User -N -D $db -e "SELECT 1 AS ok;" 2>&1
    if ($LASTEXITCODE -eq 0) {
      Write-Host "OK: database '$db' — $out" -ForegroundColor Green
    } else {
      Write-Host "FAIL: database '$db' — $out" -ForegroundColor Red
    }
  } catch {
    Write-Host "FAIL: database '$db' — $_" -ForegroundColor Red
  }
}
Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
Write-Host "Done."
