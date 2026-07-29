# Link Ops runtime deps from ops-platform-ui-vue into football-front/node_modules.
# ADR-047: additive integration; avoids broken pnpm workspace install.
# Prefer declaring Ops deps in apps/web-ele/package.json via pnpm catalog (see @element-plus/icons-vue, markdown-it, echarts, tiptap, xlsx).
$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
$OpsNm = Join-Path $Root 'ops-platform-ui-vue\node_modules'
$FfNm = Join-Path $Root 'football-front\node_modules'

if (-not (Test-Path $OpsNm)) {
    Write-Error "Missing $OpsNm — run npm/pnpm install in ops-platform-ui-vue first."
}

# Junction fallbacks for OPS pages that import libs directly (not only via @vben/plugins/echarts).
$packages = @(
    'echarts',
    'vue-echarts',
    'zrender',
    'markdown-it',
    '@element-plus\icons-vue',
    '@logicflow\core',
    '@tiptap\core',
    '@tiptap\vue-3',
    '@tiptap\starter-kit',
    '@tiptap\extension-placeholder',
    '@tiptap\extension-text-align',
    '@tiptap\extension-underline',
    '@tiptap\extension-color',
    '@tiptap\extension-table',
    '@tiptap\extension-table-row',
    '@tiptap\extension-table-cell',
    '@tiptap\extension-table-header',
    '@tiptap\extension-paragraph',
    '@tiptap\extension-highlight',
    '@tiptap\extension-image',
    '@tiptap\extension-text-style'
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

Write-Host "Done. Restart Vite dev server if running."
