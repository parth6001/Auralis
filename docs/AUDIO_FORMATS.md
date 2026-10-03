# Audio compatibility

Test environment: AOSP Android 15 / API 35, x86_64 emulator, Media3 1.11.1. Original two-second generated tones only. A pass here is not a claim of support on every Android device or of high-resolution USB output.

| Container / codec | Fixture | Decode, seek, end | Decoder path | Gapless | Metadata / artwork |
|---|---|---|---|---|---|
| MPEG audio / MP3 | 44.1 kHz stereo | Automated pass | Media3 extractor, Android MediaCodec | Not tested | Unicode title and 2,048-character bounded album extraction passed / no artwork |
| FLAC / FLAC | 44.1 kHz stereo | Automated pass | Media3, Android MediaCodec | Not tested | Embedded synthetic title/artist; detailed extraction not asserted / no artwork |
| FLAC / FLAC | 96 kHz, 24-bit container | Automated pass | Media3, Android MediaCodec | Not tested | Source is a generated 16-bit tone resampled for decoder coverage; no added source fidelity |
| RIFF WAV / PCM16 | 44.1 kHz stereo | Automated pass | Media3 PCM extraction | Not tested | Missing tags fall back to filename; no artwork |
| MP4 / AAC | 44.1 kHz stereo | Automated pass | Media3, Android MediaCodec | Not tested | Embedded synthetic title/artist; detailed extraction not asserted / no artwork |
| Ogg / Vorbis | 44.1 kHz stereo | Automated pass | Media3, Android MediaCodec | Not tested | Embedded synthetic title/artist; detailed extraction not asserted / no artwork |
| Ogg / Opus | 48 kHz decoded | Automated pass | Media3, Android MediaCodec | Not tested | Embedded synthetic title/artist; detailed extraction not asserted / no artwork |
| MP4 / ALAC | 44.1 kHz stereo | Automated pass on this emulator | Media3 decoder path; platform dependent | Not tested | Detailed metadata / artwork not tested |
| AIFF, APE, WMA, CUE, DSF/DSD | None bundled | Not tested; no advertised support | No optional decoder added | Not tested | Not tested |

Evidence: `PlaybackIntegrationTest` and the instrumentation logs listed in TEST_REPORT.md. The ALAC test allows an explicit unsupported-decoder error on other platforms; this emulator actually decoded, sought and reached end. Logs distinguish that outcome. No external FFmpeg codec is bundled in the app.

Media3 provides automatic adjacent-track playback. Sample-accurate gapless boundaries and device-specific codec delay have not been measured. Tests at zero player volume prove decoding and playback progression, not acoustic output quality. The real media-session test separately exercises the application service and its audio processor.

Pure Direct bypasses application DSP. It does not bypass Android's mixer or guarantee source-rate routing. Enhanced IEM processing accepts PCM16 mono/stereo. Source sample rate/bit depth and connected-device capabilities are displayed separately; negotiated output format remains unavailable unless exposed by the platform. Physical checks are in HARDWARE_TESTS.md.
