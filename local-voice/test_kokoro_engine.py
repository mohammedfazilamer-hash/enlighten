from __future__ import annotations

import unittest

from kokoro_engine import VOICE_NAMES, samples_to_wav


class KokoroEngineTest(unittest.TestCase):
    def test_voice_catalog_contains_expected_comparison_voices(self) -> None:
        self.assertEqual("Nicole", VOICE_NAMES[2])
        self.assertEqual("Sarah", VOICE_NAMES[3])
        self.assertEqual("Michael", VOICE_NAMES[6])
        self.assertEqual("Lewis", VOICE_NAMES[10])

    def test_samples_to_wav_creates_pcm_wave(self) -> None:
        wav_data = samples_to_wav([0.0, 0.5, -0.5], sample_rate=24_000)
        self.assertTrue(wav_data.startswith(b"RIFF"))
        self.assertIn(b"WAVE", wav_data[:16])


if __name__ == "__main__":
    unittest.main()
