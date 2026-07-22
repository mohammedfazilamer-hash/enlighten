$ErrorActionPreference = "Stop"

& (Join-Path $PSScriptRoot "start-ollama.ps1")

$python = Join-Path $env:LOCALAPPDATA "Enlighten\tts\.venv\Scripts\python.exe"
$server = Join-Path $PSScriptRoot "local-voice\server.py"
$logDirectory = Join-Path $env:LOCALAPPDATA "Enlighten\tts\logs"
$stdoutLog = Join-Path $logDirectory "voice-service.log"
$stderrLog = Join-Path $logDirectory "voice-service-error.log"

if (-not (Test-Path -LiteralPath $python)) {
    throw "The Enlighten voice runtime is not installed at $python"
}

$listener = Get-NetTCPConnection -LocalPort 11435 -State Listen -ErrorAction SilentlyContinue
if (-not $listener) {
    New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null
    $quotedServer = '"' + $server + '"'
    Start-Process `
        -FilePath $python `
        -ArgumentList @($quotedServer, "--host", "0.0.0.0", "--port", "11435") `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdoutLog `
        -RedirectStandardError $stderrLog
}

$deadline = (Get-Date).AddMinutes(3)
do {
    Start-Sleep -Milliseconds 500
    $listener = Get-NetTCPConnection -LocalPort 11435 -State Listen -ErrorAction SilentlyContinue
} until ($listener -or (Get-Date) -gt $deadline)

if (-not $listener) {
    throw "The natural voice service did not start. Check $stderrLog"
}

Write-Host "Enlighten services are ready on ports 11434 and 11435."
