# Enlighten Local Voice

This service runs Kokoro speech synthesis on the student's Windows computer. It is local, free, and independent of any paid cloud API.

The model is intentionally stored outside the OneDrive project at:

```text
%LOCALAPPDATA%\Enlighten\tts\kokoro-en-v0_19
```

## Start

Install the Python runtime and model once from the repository root:

```powershell
.\install-local-voice.ps1
```

Then start Ollama and Kokoro together:

```powershell
.\start-enlighten-services.ps1
```

To run only the voice service:

```powershell
$python = "$env:LOCALAPPDATA\Enlighten\tts\.venv\Scripts\python.exe"
& $python .\local-voice\server.py --host 0.0.0.0 --port 11435
```

Allow access only on a trusted private network if Windows Firewall asks. Do not forward port `11435` from the router.

## Routes

- `GET /health` reports model readiness and voices.
- `GET /voices` lists available preset voices.
- `POST /synthesize` accepts `text`, `voice_id`, and `speed`, then returns PCM WAV audio.

Example request:

```json
{"text":"This is Enlighten's natural local voice.","voice_id":3,"speed":1.0}
```
