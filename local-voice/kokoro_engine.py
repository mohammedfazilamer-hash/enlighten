from __future__ import annotations

import io
import os
import sys
import threading
import wave
from array import array
from pathlib import Path

import sherpa_onnx


VOICE_NAMES = {
    0: "American female",
    1: "Bella",
    2: "Nicole",
    3: "Sarah",
    4: "Sky",
    5: "Adam",
    6: "Michael",
    7: "Emma",
    8: "Isabella",
    9: "George",
    10: "Lewis",
}


def default_model_dir() -> Path:
    local_app_data = os.environ.get("LOCALAPPDATA")
    if local_app_data:
        return Path(local_app_data) / "Enlighten" / "tts" / "kokoro-en-v0_19"
    return Path.home() / ".enlighten" / "tts" / "kokoro-en-v0_19"


class KokoroEngine:
    def __init__(self, model_dir: Path | str, num_threads: int = 4) -> None:
        self.model_dir = Path(model_dir).expanduser().resolve()
        self._validate_model_files()
        config = sherpa_onnx.OfflineTtsConfig(
            model=sherpa_onnx.OfflineTtsModelConfig(
                kokoro=sherpa_onnx.OfflineTtsKokoroModelConfig(
                    model=str(self.model_dir / "model.onnx"),
                    voices=str(self.model_dir / "voices.bin"),
                    tokens=str(self.model_dir / "tokens.txt"),
                    data_dir=str(self.model_dir / "espeak-ng-data"),
                ),
                provider="cpu",
                debug=False,
                num_threads=max(1, num_threads),
            ),
            max_num_sentences=1,
        )
        if not config.validate():
            raise ValueError(f"Kokoro configuration is invalid: {self.model_dir}")
        self._tts = sherpa_onnx.OfflineTts(config)
        self._lock = threading.Lock()

    def _validate_model_files(self) -> None:
        required = ("model.onnx", "voices.bin", "tokens.txt", "espeak-ng-data")
        missing = [name for name in required if not (self.model_dir / name).exists()]
        if missing:
            names = ", ".join(missing)
            raise FileNotFoundError(f"Kokoro model is incomplete. Missing: {names}")

    def synthesize(self, text: str, voice_id: int, speed: float) -> bytes:
        clean_text = " ".join(text.split())
        if not clean_text:
            raise ValueError("Text cannot be empty.")
        if len(clean_text) > 3_000:
            raise ValueError("Text is limited to 3,000 characters per request.")
        if voice_id not in VOICE_NAMES:
            raise ValueError("Unknown voice.")
        if not 0.6 <= speed <= 1.5:
            raise ValueError("Speed must be between 0.6 and 1.5.")

        generation = sherpa_onnx.GenerationConfig()
        generation.sid = voice_id
        generation.speed = speed
        generation.silence_scale = 0.2
        with self._lock:
            audio = self._tts.generate(clean_text, generation)
        if not audio.samples:
            raise RuntimeError("Kokoro did not generate audio.")
        return samples_to_wav(audio.samples, audio.sample_rate)


def samples_to_wav(samples, sample_rate: int) -> bytes:
    pcm = array(
        "h",
        (
            max(-32_768, min(32_767, round(float(sample) * 32_767)))
            for sample in samples
        ),
    )
    if sys.byteorder == "big":
        pcm.byteswap()
    output = io.BytesIO()
    with wave.open(output, "wb") as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        wav_file.writeframes(pcm.tobytes())
    return output.getvalue()
