param(
    [string]$DeployRoot = "D:\codes\ieps-deploy",
    [string]$NginxRoot = "D:\Program Files\nginx-1.30.3"
)

$ErrorActionPreference = "Stop"

$nginxExe = Join-Path $NginxRoot "nginx.exe"
$runDir = Join-Path $DeployRoot "run"
$javaPidFile = Join-Path $runDir "ieps-java.pid"
if (Test-Path -LiteralPath $nginxExe) {
    Push-Location $NginxRoot
    try {
        & $nginxExe -s stop | Out-Null
    } finally {
        Pop-Location
    }
}

if (Test-Path -LiteralPath $javaPidFile) {
    $javaPid = Get-Content -LiteralPath $javaPidFile | Select-Object -First 1
    if ($javaPid) {
        Stop-Process -Id ([int]$javaPid) -Force -ErrorAction SilentlyContinue
    }
    Remove-Item -LiteralPath $javaPidFile -Force -ErrorAction SilentlyContinue
} else {
    $listenLine = netstat -ano | Select-String "127.0.0.1:8080" | Select-String "LISTENING" | Select-Object -First 1
    if ($listenLine) {
        $listenPid = ($listenLine -split "\s+")[-1]
        if ($listenPid) {
            Stop-Process -Id ([int]$listenPid) -Force -ErrorAction SilentlyContinue
        }
    }
}

Write-Host "IEPS backend and nginx stop commands have been issued."
