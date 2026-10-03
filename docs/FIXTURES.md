# Test media

`scripts/generate-fixtures.ps1` creates original two-second 440 Hz sine tones at low amplitude, silence, mono/stereo PCM, a byte-for-byte duplicate, deliberately invalid MP3 bytes and a truncated WAV header. These generated fixtures contain no third-party recordings and may be used without restriction (CC0).

Optional FFmpeg conversion creates MP3, FLAC, AAC/M4A, Vorbis/Ogg, Opus, ALAC and 96 kHz FLAC fixtures. The 96 kHz tone is synthetic test data, not a quality enhancement. FFmpeg is a development tool and is not bundled with the app. Run without FFmpeg to generate WAV-only fixtures. No commercial music is used.

The tagged MP3 includes a Unicode title and an album tag longer than 4,000 characters, for bounded metadata extraction. Decoder, seek, gapless, artwork, Unicode/long-tag and hardware test outcomes are recorded separately in AUDIO_FORMATS.md and TEST_REPORT.md. A generated file alone is not a passed playback test.
