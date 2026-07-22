from __future__ import annotations

import json
import threading
import unittest
from http.server import ThreadingHTTPServer
from urllib.request import Request, urlopen

from server import VoiceRequestHandler


class FakeEngine:
    def synthesize(self, text: str, voice_id: int, speed: float) -> bytes:
        if text != "Test sentence" or voice_id != 3 or speed != 1.0:
            raise ValueError("Unexpected request")
        return b"RIFF\x00\x00\x00\x00WAVE"


class VoiceServerTest(unittest.TestCase):
    def setUp(self) -> None:
        VoiceRequestHandler.engine = FakeEngine()
        self.server = ThreadingHTTPServer(("127.0.0.1", 0), VoiceRequestHandler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.base_url = f"http://127.0.0.1:{self.server.server_port}"

    def tearDown(self) -> None:
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=2)

    def test_health_lists_the_model_and_voices(self) -> None:
        with urlopen(self.base_url + "/health", timeout=2) as response:
            payload = json.load(response)
        self.assertEqual("ready", payload["status"])
        self.assertEqual("kokoro-en-v0_19", payload["model"])
        self.assertEqual(11, len(payload["voices"]))

    def test_synthesize_returns_wave_audio(self) -> None:
        body = json.dumps({"text": "Test sentence", "voice_id": 3, "speed": 1.0}).encode("utf-8")
        request = Request(
            self.base_url + "/synthesize",
            data=body,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urlopen(request, timeout=2) as response:
            audio = response.read()
            content_type = response.headers["Content-Type"]
        self.assertEqual("audio/wav", content_type)
        self.assertEqual(b"RIFF\x00\x00\x00\x00WAVE", audio)


if __name__ == "__main__":
    unittest.main()
