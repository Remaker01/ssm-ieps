param(
    [string]$DeployRoot
)

$ErrorActionPreference = "Stop"
$ErrorView = "DetailedView"

$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $DeployRoot) {
    $DeployRoot = Join-Path $projectRoot "deploy"
}

$runDir = Join-Path $DeployRoot "run"
$javaPidFile = Join-Path $runDir "ieps-java.pid"
$redisPidFile = Join-Path $runDir "ieps-redis.pid"
$backendPort = 8080
$redisPort = 6379

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

function Stop-ProcessByPid {
    param(
        [int]$ProcessId
    )

    if ($ProcessId) {
        Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
    }
}

function Stop-TrackedProcess {
    param(
        [string]$PidFile,
        [int]$FallbackPort
    )

    if (Test-Path -LiteralPath $PidFile) {
        $trackedPid = Get-Content -LiteralPath $PidFile | Select-Object -First 1
        if ($trackedPid) {
            Stop-ProcessByPid -ProcessId ([int]$trackedPid)
        }
        Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
        return
    }

    $listenPid = Get-ListeningPid -Port $FallbackPort
    if ($listenPid) {
        Stop-ProcessByPid -ProcessId $listenPid
    }
}

function Stop-Nginx {
    $nginxCommand = Get-Command nginx -ErrorAction SilentlyContinue
    if (-not $nginxCommand) {
        return
    }

    $nginxPrefix = Split-Path -Parent $nginxCommand.Source
    $nginxPrefixArg = ($nginxPrefix -replace "\\", "/") + "/"
    try {
        & $nginxCommand.Source -p $nginxPrefixArg -c "conf/nginx.conf" -s stop | Out-Null
    } catch {
        Write-Error $_.Exception.Message
    }
}

function Stop-Redis {
    $redisCli = Get-Command redis-cli -ErrorAction SilentlyContinue
    if ($redisCli) {
        try {
            & $redisCli.Source -h 127.0.0.1 -p $redisPort shutdown nosave | Out-Null
            if ($LASTEXITCODE -eq 0) {
                Remove-Item -LiteralPath $redisPidFile -Force -ErrorAction SilentlyContinue
                return
            }
        } catch {
            Write-Error $_.Exception.Message
        }
    }

    Stop-TrackedProcess -PidFile $redisPidFile -FallbackPort $redisPort
}

Stop-Nginx
Stop-TrackedProcess -PidFile $javaPidFile -FallbackPort $backendPort
Stop-Redis

Write-Host "IEPS backend, nginx, and redis stop commands have been issued."
