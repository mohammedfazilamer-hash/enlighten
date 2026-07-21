@echo off
setlocal

set "OLLAMA_EXE=%LOCALAPPDATA%\Programs\Ollama\ollama.exe"
if not exist "%OLLAMA_EXE%" (
  echo Ollama is not installed at "%OLLAMA_EXE%".
  exit /b 1
)

netstat -ano | findstr /R /C:":11434 .*LISTENING" >nul
if %ERRORLEVEL% EQU 0 (
  echo Ollama is already listening on port 11434.
  exit /b 0
)

set "OLLAMA_HOST=0.0.0.0:11434"
start "Ollama for Enlighten" /min "%OLLAMA_EXE%" serve
echo Ollama is starting at http://0.0.0.0:11434
