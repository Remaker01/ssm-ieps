param(
    [string]$DeployRoot = "D:\codes\ieps-deploy",
    [string]$NginxRoot = "D:\Program Files\nginx-1.30.3"
)

$ErrorActionPreference = "Stop"

$appDir = Join-Path $DeployRoot "app"
$jarPath = Join-Path $appDir "ieps.jar"
$nginxExe = Join-Path $NginxRoot "nginx.exe"
$logDir = Join-Path $DeployRoot "logs"
$runDir = Join-Path $DeployRoot "run"
$appLog = Join-Path $logDir "ieps-app.log"
$javaPidFile = Join-Path $runDir "ieps-java.pid"

if (-not (Test-Path -LiteralPath $jarPath)) {
    throw "Deploy jar not found: $jarPath"
}

if (-not (Test-Path -LiteralPath $nginxExe)) {
    throw "nginx.exe not found: $nginxExe"
}

New-Item -ItemType Directory -Force -Path $logDir | Out-Null
New-Item -ItemType Directory -Force -Path $runDir | Out-Null

$backendReady = $false
try {
    $response = Invoke-WebRequest -Uri "http://127.0.0.1:8080/" -UseBasicParsing -TimeoutSec 3
    $backendReady = $response.StatusCode -eq 200
} catch {
    $backendReady = $false
}

if ($backendReady) {
    $listenLine = netstat -ano | Select-String "127.0.0.1:8080" | Select-String "LISTENING" | Select-Object -First 1
    if ($listenLine) {
        $listenPid = ($listenLine -split "\s+")[-1]
        if ($listenPid) {
            Set-Content -LiteralPath $javaPidFile -Value $listenPid -Encoding ASCII
        }
    }
    Write-Host "IEPS backend is already running."
} else {
    $javaw = Join-Path (Split-Path (Get-Command java).Source) "javaw.exe"
    if (-not (Test-Path -LiteralPath $javaw)) {
        throw "javaw.exe not found next to java command."
    }

    $process = Start-Process -FilePath $javaw `
        -ArgumentList "-Dlogging.file.name=$appLog", "-jar", $jarPath, "--server.address=127.0.0.1", "--server.port=8080" `
        -WorkingDirectory $appDir `
        -WindowStyle Hidden `
        -PassThru

    Set-Content -LiteralPath $javaPidFile -Value $process.Id -Encoding ASCII

    Start-Sleep -Seconds 5
}

$backendReady = $false
for ($i = 0; $i -lt 10; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:8080/" -UseBasicParsing -TimeoutSec 3
        if ($response.StatusCode -eq 200) {
            $backendReady = $true
            break
        }
    } catch {
        Start-Sleep -Milliseconds 500
    }
}

if (-not $backendReady) {
    throw "IEPS backend did not become ready on http://127.0.0.1:8080/"
}

Push-Location $NginxRoot
try {
    & $nginxExe -t | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "nginx config test failed."
    }

    $nginxRunning = Get-Process nginx -ErrorAction SilentlyContinue
    if ($nginxRunning) {
        & $nginxExe -s reload | Out-Null
        Write-Host "Nginx reloaded."
    } else {
        Start-Process -FilePath $nginxExe -WorkingDirectory $NginxRoot -WindowStyle Hidden | Out-Null
        Write-Host "Nginx started."
    }
} finally {
    Pop-Location
}

Write-Host "IEPS is available at http://127.0.0.1/"
