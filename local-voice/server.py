from __future__ import annotations

import argparse
import json
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

from kokoro_engine import KokoroEngine, VOICE_NAMES, default_model_dir


MAX_REQUEST_BYTES = 16_384


class VoiceRequestHandler(BaseHTTPRequestHandler):
    engine: KokoroEngine

    def do_GET(self) -> None:
        path = urlparse(self.path).path
        if path == "/health":
            self._send_json(
                HTTPStatus.OK,
                {
                    "status": "ready",
                    "model": "kokoro-en-v0_19",
                    "voices": [
                        {"id": voice_id, "name": name}
                        for voice_id, name in VOICE_NAMES.items()
                    ],
                },
            )
            return
        if path == "/voices":
            self._send_json(
                HTTPStatus.OK,
                {"voices": [{"id": voice_id, "name": name} for voice_id, name in VOICE_NAMES.items()]},
            )
            return
        self._send_json(HTTPStatus.NOT_FOUND, {"error": "Route not found."})

    def do_POST(self) -> None:
        if urlparse(self.path).path != "/synthesize":
            self._send_json(HTTPStatus.NOT_FOUND, {"error": "Route not found."})
            return
        try:
            content_length = int(self.headers.get("Content-Length", "0"))
            if content_length <= 0 or content_length > MAX_REQUEST_BYTES:
                raise ValueError("Request is empty or too large.")
            payload = json.loads(self.rfile.read(content_length).decode("utf-8"))
            text = str(payload.get("text", ""))
            voice_id = int(payload.get("voice_id", 3))
            speed = float(payload.get("speed", 1.0))
            wav_data = self.engine.synthesize(text, voice_id, speed)
        except (ValueError, TypeError, json.JSONDecodeError) as error:
            self._send_json(HTTPStatus.BAD_REQUEST, {"error": str(error)})
            return
        except Exception as error:
            self.log_error("Speech generation failed: %s", error)
            self._send_json(HTTPStatus.INTERNAL_SERVER_ERROR, {"error": "Speech generation failed."})
            return

        self.send_response(HTTPStatus.OK)
        self.send_header("Content-Type", "audio/wav")
        self.send_header("Content-Length", str(len(wav_data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(wav_data)

    def _send_json(self, status: HTTPStatus, payload: dict) -> None:
        body = json.dumps(payload, separators=(",", ":")).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, message_format: str, *args) -> None:
        print(f"{self.client_address[0]} - {message_format % args}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Enlighten local Kokoro voice service")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=11435)
    parser.add_argument("--model-dir", type=Path, default=default_model_dir())
    parser.add_argument("--threads", type=int, default=4)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    VoiceRequestHandler.engine = KokoroEngine(args.model_dir, num_threads=args.threads)
    server = ThreadingHTTPServer((args.host, args.port), VoiceRequestHandler)
    print(f"Enlighten voice service ready at http://{args.host}:{args.port}")
    print(f"Model: {args.model_dir}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
