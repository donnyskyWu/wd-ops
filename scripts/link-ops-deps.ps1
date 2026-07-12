# Link Ops runtime deps from ops-platform-ui-vue into football-front/node_modules.
# ADR-047: additive integration; avoids broken pnpm workspace install.
$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
$OpsNm = Join-Path $Root 'ops-platform-ui-vue\node_modules'
$FfNm = Join-Path $Root 'football-front\node_modules'

if (-not (Test-Path $OpsNm)) {
    Write-Error "Missing $OpsNm — run npm/pnpm install in ops-platform-ui-vue first."
}

$packages = @(
    'echarts',
    'vue-echarts',
    'xlsx',
    '@logicflow',
    '@tiptap'
)

function Link-Package($name) {
    $src = Join-Path $OpsNm $name
    $dst = Join-Path $FfNm $name
    if (-not (Test-Path $src)) {
        Write-Warning "SKIP missing source: $name"
        return
    }
    if (Test-Path $dst) {
        Write-Host "EXISTS $name"
        return
    }
    $parent = Split-Path $dst -Parent
    if (-not (Test-Path $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
    New-Item -ItemType Junction -Path $dst -Target $src | Out-Null
    Write-Host "LINKED $name"
}

foreach ($p in $packages) { Link-Package $p }

# logicflow extension deps (peer packages under @logicflow)
$logicflowExtras = @('@logicflow/core', '@logicflow/extension', '@logicflow/vue-node-registry')
foreach ($p in $logicflowExtras) {
    if ($p -like '@logicflow/*') { continue } # covered by @logicflow junction
}

Write-Host "Done. Restart Vite dev server if running."
