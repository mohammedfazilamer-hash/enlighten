$ErrorActionPreference = "Stop"

$ollamaExe = Join-Path $env:LOCALAPPDATA "Programs\Ollama\ollama.exe"
if (-not (Test-Path -LiteralPath $ollamaExe)) {
    throw "Ollama is not installed at $ollamaExe"
}

$listener = Get-NetTCPConnection -LocalPort 11434 -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    Write-Host "Ollama is already listening on port 11434."
    exit 0
}

$ollamaHost = [Environment]::GetEnvironmentVariable("OLLAMA_HOST", "User")
if ([string]::IsNullOrWhiteSpace($ollamaHost)) {
    $ollamaHost = "0.0.0.0:11434"
    [Environment]::SetEnvironmentVariable("OLLAMA_HOST", $ollamaHost, "User")
}

$env:OLLAMA_HOST = $ollamaHost
Start-Process -FilePath $ollamaExe -ArgumentList "serve" -WindowStyle Hidden

$deadline = (Get-Date).AddSeconds(20)
do {
    Start-Sleep -Milliseconds 500
    $listener = Get-NetTCPConnection -LocalPort 11434 -State Listen -ErrorAction SilentlyContinue
} until ($listener -or (Get-Date) -gt $deadline)

if (-not $listener) {
    throw "Ollama did not start on port 11434."
}

Write-Host "Ollama is ready at http://$ollamaHost"
