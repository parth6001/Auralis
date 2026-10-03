# Known limitations and remaining validation

This is a development release, not a claim that every BUILD_PROMPT.md acceptance item has been completed. The full prompt remains authoritative.

## Hardware and quality

* OnePlus Nord 2 and Lenovo Legion Tab physical tests remain required. USB removal callbacks and Media3 noisy handling pause by default, but timing and zero speaker leakage cannot be proved by the emulator.
* Pure Direct means application DSP bypass. Android still owns mixing/routing; exclusive USB, negotiated output rate/bit depth, native source-rate switching and bit-perfect delivery are not verified or advertised. Device capability lists are not active output measurements.
* Automatic profile selection prioritizes connected USB, wired, Bluetooth, then speaker. It cannot prove Android's active route when multiple outputs are present. API 26–27 lack device addresses; identical products may share a profile. Passive IEMs cannot be identified behind a DAC.
* Enhanced DSP supports mono/stereo PCM16, five fixed EQ centers, preamp, conservative headroom, balance, crossfeed and hard peak limiting. User-adjustable parametric frequency/Q, crossfade, acoustic loudness analysis, R128 Opus gain handling and exclusive drivers are not implemented. ReplayGain only uses supported tags exposed by Media3; it is not an acoustic analyzer. No unavailable processing option is advertised as active.
* Source bit depth, album artwork and some metadata may be absent or unavailable from platform extractors. ALAC is device dependent; no optional FFmpeg decoder is bundled. AIFF/APE/WMA/CUE/DSD are not supported intentionally in this build.

## Product gaps

* Lyrics display supports user-selected local UTF-8 text/LRC files as plain text. Automatic embedded-lyrics extraction, automatic sidecar matching and synchronized LRC highlighting are pending.
* Artwork displays embedded images with a bounded decoded size; adaptive palette extraction is pending. Theme colors are curated and consistent rather than generated from artwork.
* Scans are explicit and cancellable; there is no continuous MediaStore index or automatic watcher. A foreground activity's ViewModel owns scans; leaving the process can interrupt them. Full snapshots are committed per root; completed earlier roots remain committed if a later root fails.
* Move/copy detection uses size plus the first 128 KB SHA-256 and only preserves a moved ID for a unique match. This is a probable-identity heuristic, not full-file hashing. Very large/provider-backed libraries require broader performance tests. Provider failures retain old entries until a complete scan succeeds.
* Queue checkpoints can lag by up to five seconds on abrupt termination. Shuffle enablement is restored; the exact internal shuffled traversal order is not persisted. Playback resumes paused, never automatically through a newly connected output.
* History distinguishes completion and early skips from elapsed playing time; repeat affinity is derived from repeated events. Interrupted current-track history can be lost on abrupt process death. Recommendation scoring is simple and explainable, not machine learning. No sensitive mood inference is performed.
* Local backup merges only currently authorized matching songs. It does not restore music, storage grants, cached artwork or loaded lyrics. Back up before import if rollback is needed. Exported history is capped at the most recent 100,000 records; backups over 16 MB are not importable.

## Validation and release

Consult TEST_REPORT.md for exact executed checks, device/API and results. Compiled tests are not passed tests. A private debug-only ContentProvider implements DocumentsContract-shaped queries to exercise SAF scanning; it does not substitute for real OEM/provider grant flows. Hardware calls, DAC indicators, long playback/battery tests, low storage and the complete cross-device accessibility matrix require further testing.

No signing key was supplied. Release output is unsigned; no store publication or production signing has occurred. The single maintained native library from Compose graphics is inspected with the APK; it is not a codec or USB driver.
