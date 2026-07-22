from __future__ import annotations

import argparse
from pathlib import Path

from kokoro_engine import KokoroEngine, VOICE_NAMES, default_model_dir


SAMPLE_VOICES = (2, 3, 6, 10)
SAMPLE_TEXT = (
    "Photosynthesis is the process plants use to convert light energy into chemical energy. "
    "Chlorophyll absorbs sunlight, and the plant uses that energy to combine water and carbon dioxide. "
    "The process produces glucose for stored energy and releases oxygen into the atmosphere."
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Render comparable Enlighten Kokoro voice samples")
    parser.add_argument("--model-dir", type=Path, default=default_model_dir())
    parser.add_argument("--output-dir", type=Path, default=Path(__file__).parent / "samples")
    parser.add_argument("--threads", type=int, default=4)
    parser.add_argument("--voice-id", type=int, action="append", choices=VOICE_NAMES)
    parser.add_argument("--overwrite", action="store_true")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)
    engine = KokoroEngine(args.model_dir, num_threads=args.threads)
    voice_ids = tuple(args.voice_id) if args.voice_id else SAMPLE_VOICES
    for voice_id in voice_ids:
        filename = f"{voice_id:02d}-{VOICE_NAMES[voice_id].lower()}.wav"
        output_path = args.output_dir / filename
        if output_path.exists() and not args.overwrite:
            print(f"Already exists: {output_path.resolve()}", flush=True)
            continue
        output_path.write_bytes(engine.synthesize(SAMPLE_TEXT, voice_id, speed=1.0))
        print(output_path.resolve(), flush=True)


if __name__ == "__main__":
    main()
