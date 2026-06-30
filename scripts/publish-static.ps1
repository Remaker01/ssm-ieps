param(
    [string]$TargetRoot = "D:\deploy\ieps-static"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$staticSource = Join-Path $projectRoot "src\main\resources\static\static"
$faviconSource = Join-Path $projectRoot "src\main\resources\static\favicon.ico"
$targetStatic = Join-Path $TargetRoot "static"
$targetFavicon = Join-Path $TargetRoot "favicon.ico"

if (-not (Test-Path -LiteralPath $staticSource)) {
    throw "Static source directory not found: $staticSource"
}

if (-not (Test-Path -LiteralPath $faviconSource)) {
    throw "Favicon source file not found: $faviconSource"
}

New-Item -ItemType Directory -Force -Path $TargetRoot | Out-Null

if (Test-Path -LiteralPath $targetStatic) {
    Remove-Item -LiteralPath $targetStatic -Recurse -Force
}

Copy-Item -LiteralPath $staticSource -Destination $TargetRoot -Recurse -Force
Copy-Item -LiteralPath $faviconSource -Destination $targetFavicon -Force

Write-Host "Static assets published to $TargetRoot"
Write-Host "Nginx should serve:"
Write-Host "  /static/    -> $targetStatic"
Write-Host "  /favicon.ico -> $targetFavicon"
