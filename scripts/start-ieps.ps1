param(
    [string]$DeployRoot
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $DeployRoot) {
    $DeployRoot = Join-Path $projectRoot "deploy"
}

$appDir = Join-Path $DeployRoot "app"
$logDir = Join-Path $DeployRoot "logs"
$runDir = Join-Path $DeployRoot "run"
$deployJarPath = Join-Path $appDir "ieps.jar"
$targetJarPath = Join-Path $projectRoot "target\ieps.jar"
$appLog = Join-Path $logDir "ieps-app.log"
$javaPidFile = Join-Path $runDir "ieps-java.pid"
$redisPidFile = Join-Path $runDir "ieps-redis.pid"
$nginxConfigSource = Join-Path $projectRoot "deploy\nginx\nginx.conf.windows"
$nginxPartialConfigSource = Join-Path $projectRoot "deploy\nginx\ieps.partial-static.conf"
$backendPort = 8080
$redisHost = "127.0.0.1"
$redisPort = 6379

function Get-LatestWriteTimeUtc {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Paths
    )

    $times = @()
    foreach ($path in $Paths) {
        if (-not (Test-Path -LiteralPath $path)) {
            continue
        }

        $item = Get-Item -LiteralPath $path
        if ($item.PSIsContainer) {
            $times += $item.LastWriteTimeUtc
            $times += Get-ChildItem -LiteralPath $path -Recurse -File | Select-Object -ExpandProperty LastWriteTimeUtc
        } else {
            $times += $item.LastWriteTimeUtc
        }
    }

    if (-not $times) {
        return [datetime]::MinValue
    }

    return ($times | Sort-Object -Descending | Select-Object -First 1)
}

function Get-ListeningPid {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $line = netstat -ano | Select-String "LISTENING" | Where-Object { $_.Line -match "^\s*TCP\s+\S+:$Port\s+\S+\s+LISTENING\s+(\d+)\s*$" } | Select-Object -First 1
    if ($line -and $line.Line -match "(\d+)\s*$") {
        return [int]$Matches[1]
    }

    return $null
}

function Test-HttpReady {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [int]$TimeoutSec = 3
    )

    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec $TimeoutSec
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Wait-PortReady {
    param(
        [Parameter(Mandatory = $true)]
        [string]$TargetHost,
        [Parameter(Mandatory = $true)]
        [int]$Port,
        [int]$Attempts = 20,
        [int]$DelayMs = 500
    )

    for ($i = 0; $i -lt $Attempts; $i++) {
        if ((Test-NetConnection -ComputerName $TargetHost -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue)) {
            return $true
        }
        Start-Sleep -Milliseconds $DelayMs
    }

    return $false
}

function Stop-ProcessByPid {
    param(
        [int]$ProcessId
    )

    if ($ProcessId) {
        Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
    }
}

function Ensure-BackendBuild {
    New-Item -ItemType Directory -Force -Path $appDir | Out-Null

    $backendSourceTime = Get-LatestWriteTimeUtc @(
        (Join-Path $projectRoot "src\main\java"),
        (Join-Path $projectRoot "src\main\resources"),
        (Join-Path $projectRoot "pom.xml")
    )

    $needBuild = -not (Test-Path -LiteralPath $targetJarPath)
    if (-not $needBuild) {
        $targetTime = (Get-Item -LiteralPath $targetJarPath).LastWriteTimeUtc
        $needBuild = $backendSourceTime -gt $targetTime
    }

    if ($needBuild) {
        Write-Host "Building backend..."
        & mvn -q -DskipTests package
        if ($LASTEXITCODE -ne 0) {
            throw "Backend build failed."
        }
    } elseif (-not (Test-Path -LiteralPath $targetJarPath)) {
        throw "Backend jar not found: $targetJarPath"
    }

    $copiedJar = $false
    $needCopy = -not (Test-Path -LiteralPath $deployJarPath)
    if (-not $needCopy) {
        $needCopy = (Get-Item -LiteralPath $targetJarPath).LastWriteTimeUtc -gt (Get-Item -LiteralPath $deployJarPath).LastWriteTimeUtc
    }

    if ($needCopy) {
        Copy-Item -LiteralPath $targetJarPath -Destination $deployJarPath -Force
        $copiedJar = $true
        Write-Host "Backend jar deployed to $deployJarPath"
    }

    return [pscustomobject]@{
        NeedBuild = $needBuild
        CopiedJar = $copiedJar
    }
}

function Ensure-FrontendDeploy {
    $frontendSourceTime = Get-LatestWriteTimeUtc @(
        (Join-Path $projectRoot "src\main\resources\static\static"),
        (Join-Path $projectRoot "src\main\resources\static\favicon.ico")
    )

    $targetStaticDir = Join-Path $DeployRoot "static"
    $targetFavicon = Join-Path $DeployRoot "favicon.ico"

    $needDeploy = -not (Test-Path -LiteralPath $targetStaticDir) -or -not (Test-Path -LiteralPath $targetFavicon)
    if (-not $needDeploy) {
        $frontendDeployTime = Get-LatestWriteTimeUtc @($targetStaticDir, $targetFavicon)
        $needDeploy = $frontendSourceTime -gt $frontendDeployTime
    }

    if ($needDeploy) {
        Write-Host "Deploying frontend static assets..."
        & (Join-Path $PSScriptRoot "publish-static.ps1") -TargetRoot $DeployRoot
    }

    return $needDeploy
}

function Ensure-Redis {
    New-Item -ItemType Directory -Force -Path $runDir | Out-Null

    if (Wait-PortReady -TargetHost $redisHost -Port $redisPort -Attempts 2 -DelayMs 200) {
        $existingPid = Get-ListeningPid -Port $redisPort
        if ($existingPid) {
            Set-Content -LiteralPath $redisPidFile -Value $existingPid -Encoding ASCII
        }
        Write-Host "Redis is already running."
        return
    }

    $redisCommand = Get-Command redis-server -ErrorAction SilentlyContinue
    if (-not $redisCommand) {
        throw "redis-server command not found."
    }

    $redisProcess = Start-Process -FilePath $redisCommand.Source `
        -ArgumentList "--bind", $redisHost, "--port", $redisPort `
        -WorkingDirectory $DeployRoot `
        -WindowStyle Hidden `
        -PassThru

    Set-Content -LiteralPath $redisPidFile -Value $redisProcess.Id -Encoding ASCII

    if (-not (Wait-PortReady -TargetHost $redisHost -Port $redisPort)) {
        throw "Redis server did not become ready on $redisHost`:$redisPort"
    }

    Write-Host "Redis started."
}

function Ensure-Backend {
    param(
        [bool]$NeedRestart
    )

    New-Item -ItemType Directory -Force -Path $logDir, $runDir | Out-Null

    $backendReady = Test-HttpReady -Url "http://127.0.0.1:$backendPort/"
    $existingPid = Get-ListeningPid -Port $backendPort

    if ($NeedRestart -and $existingPid) {
        Stop-ProcessByPid -ProcessId $existingPid
        Start-Sleep -Seconds 2
        $backendReady = $false
        $existingPid = $null
    }

    if ($backendReady -and $existingPid) {
        Set-Content -LiteralPath $javaPidFile -Value $existingPid -Encoding ASCII
        Write-Host "IEPS backend is already running."
        return
    }

    $javaw = Join-Path (Split-Path (Get-Command java).Source) "javaw.exe"
    if (-not (Test-Path -LiteralPath $javaw)) {
        throw "javaw.exe not found next to java command."
    }

    $process = Start-Process -FilePath $javaw `
        -ArgumentList "-Dlogging.file.name=$appLog", "-jar", $deployJarPath, "--server.address=127.0.0.1", "--server.port=$backendPort" `
        -WorkingDirectory $appDir `
        -WindowStyle Hidden `
        -PassThru

    Set-Content -LiteralPath $javaPidFile -Value $process.Id -Encoding ASCII

    for ($i = 0; $i -lt 20; $i++) {
        if (Test-HttpReady -Url "http://127.0.0.1:$backendPort/") {
            Write-Host "IEPS backend started."
            return
        }
        Start-Sleep -Milliseconds 500
    }

    throw "IEPS backend did not become ready on http://127.0.0.1:$backendPort/"
}

function Ensure-Nginx {
    param(
        [bool]$NeedReload
    )

    $warningMessage = "启动失败仅能使用8080端口"
    $nginxCommand = Get-Command nginx -ErrorAction SilentlyContinue
    if (-not $nginxCommand) {
        Write-Warning $warningMessage
        return
    }

    $nginxPrefix = Split-Path -Parent $nginxCommand.Source
    $nginxPrefixArg = ($nginxPrefix -replace "\\", "/") + "/"
    $nginxConfDir = Join-Path $nginxPrefix "conf"

    try {
        if ((Test-Path -LiteralPath $nginxConfigSource) -and (Test-Path -LiteralPath $nginxPartialConfigSource)) {
            Copy-Item -LiteralPath $nginxConfigSource -Destination (Join-Path $nginxConfDir "nginx.conf") -Force
            Copy-Item -LiteralPath $nginxPartialConfigSource -Destination (Join-Path $nginxConfDir "ieps.partial-static.conf") -Force
        }

        & $nginxCommand.Source -p $nginxPrefixArg -c "conf/nginx.conf" -t | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Warning $warningMessage
            return
        }

        $nginxRunning = Get-Process nginx -ErrorAction SilentlyContinue
        if ($nginxRunning) {
            if ($NeedReload) {
                & $nginxCommand.Source -p $nginxPrefixArg -c "conf/nginx.conf" -s reload | Out-Null
                if ($LASTEXITCODE -ne 0) {
                    Write-Warning $warningMessage
                    return
                }
                Write-Host "Nginx reloaded."
            } else {
                Write-Host "Nginx is already running."
            }
        } else {
            $nginxStartCommand = "& '$($nginxCommand.Source)' -p '$nginxPrefixArg' -c 'conf/nginx.conf'"
            Start-Process -FilePath "powershell.exe" `
                -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $nginxStartCommand `
                -WorkingDirectory $nginxPrefix `
                -WindowStyle Hidden | Out-Null

            Start-Sleep -Seconds 2
            if (-not (Wait-PortReady -TargetHost "127.0.0.1" -Port 80 -Attempts 10 -DelayMs 500)) {
                Write-Warning $warningMessage
                return
            }
            Write-Host "Nginx started."
        }
    } catch {
        Write-Warning $warningMessage
    }
}

$backendBuild = Ensure-BackendBuild
$frontendDeployed = Ensure-FrontendDeploy
Ensure-Redis
Ensure-Backend -NeedRestart ($backendBuild.NeedBuild -or $backendBuild.CopiedJar)
Ensure-Nginx -NeedReload $frontendDeployed

if (Test-HttpReady -Url "http://127.0.0.1/") {
    Write-Host "IEPS is available at http://127.0.0.1/"
} else {
    Write-Host "IEPS backend is available at http://127.0.0.1:8080/"
}
