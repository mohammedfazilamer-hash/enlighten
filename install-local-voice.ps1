$ErrorActionPreference = "Stop"

$pythonCommand = Get-Command python -ErrorAction SilentlyContinue
if (-not $pythonCommand) {
    throw "Python 3.10 or newer is required to install the local voice service."
}

& $pythonCommand.Source -c "import sys; raise SystemExit(0 if sys.version_info >= (3, 10) else 1)"
if ($LASTEXITCODE -ne 0) {
    throw "Python 3.10 or newer is required to install the local voice service."
}

$root = Join-Path $env:LOCALAPPDATA "Enlighten\tts"
$venv = Join-Path $root ".venv"
$venvPython = Join-Path $venv "Scripts\python.exe"
$modelDirectory = Join-Path $root "kokoro-en-v0_19"
$modelFile = Join-Path $modelDirectory "model.onnx"
$archive = Join-Path $root "kokoro-en-v0_19.tar.bz2"

New-Item -ItemType Directory -Force -Path $root | Out-Null
if (-not (Test-Path -LiteralPath $venvPython)) {
    & $pythonCommand.Source -m venv $venv
}
& $venvPython -m pip install --disable-pip-version-check "sherpa-onnx==1.13.4"

if (-not (Test-Path -LiteralPath $modelFile)) {
    curl.exe `
        -L `
        --fail `
        --retry 3 `
        --output $archive `
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-en-v0_19.tar.bz2"
    tar -xf $archive -C $root
}

if (-not (Test-Path -LiteralPath $modelFile)) {
    throw "Kokoro did not install correctly at $modelDirectory"
}

Remove-Item -LiteralPath $archive -Force -ErrorAction SilentlyContinue
Write-Host "Enlighten natural voice is installed at $modelDirectory"
