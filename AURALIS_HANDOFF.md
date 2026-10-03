# Auralis: achievements, pending work, and complete handoff

Snapshot: 2026-09-13. Project folder: `D:\AndroidProjects\Auralis`.

## Current outcome

We built and tested **Auralis 0.1.0**, a native Android offline music-player development preview. There is an installable debug APK, an optimized unsigned release APK, and an Android Studio source archive. The app has no Internet or network-state permission. This is not yet a production release, and not every item in BUILD_PROMPT.md is complete.

This file consolidates the previous handoff, the current achieved/pending summary, installation and build instructions, checksums, detailed test evidence, audio compatibility, hardware checklist, privacy information, and release/signing instructions. The detailed appendices are included below so the information is available in one file.

## What we achieved

1. **Buildable native Android project:** Kotlin, Jetpack Compose Material 3, Media3 and Room; Gradle wrapper, version catalog, database migration, scripts, documentation and generated test fixtures. Minimum Android 8/API 26; compile/target SDK 37; package `com.auralis.player`; version 0.1.0/code 1.
2. **Local library:** user-selected folder access through Android's Storage Access Framework, persisted read grants, cancellable scans/rescans, metadata reading, library grouping/search/favourites, probable duplicate detection and move/delete reconciliation. Incomplete provider scans retain the last complete snapshot. Source music files are not modified.
3. **Playback:** play/pause/seek/next/previous, shuffle/repeat, editable queues, play-next, background service, media notification/session controls, and paused queue/position restoration. The real service and system media-pause control were exercised on the emulator.
4. **Local listening features:** manual playlists and queue-to-playlist saving; deterministic smart mixes and recommendation explanations; listening history, exclusions, manual music mood tags, history pause/clear/forget controls, and local JSON data/preset import/export.
5. **IEM/audio foundation:** USB/wired/Bluetooth/speaker detection and saved output profiles, default pause-on-removal logic, Pure Direct application-DSP bypass, Enhanced IEM five-band EQ, preamp/headroom, hard peak limiter, balance, crossfeed, tagged track/album ReplayGain, and copyable diagnostics. Physical route/disconnect behavior still needs testing.
6. **Interface:** Home, Library, Mixes, EQ and Search; phone bottom navigation and tablet rail; Light/Dark/AMOLED/System themes; artwork display, local plain-text/LRC lyrics, labelled playback actions and enlarged-text coverage.
7. **Privacy and packaging:** offline manifest/dependency checks, final APK permission/signature/alignment inspection, dependency and license reports, checksums, physical-device checklist and a portable source ZIP. No account, ads, analytics or cloud AI.
8. **Bugs fixed during real tests:** empty-buffer audio crash, corrupt-file extractor error aborting a scan, debug test-provider startup crash, and migration/UI test setup problems. Earlier failure logs remain preserved.

## What actually passed

| Validation | Result |
|---|---|
| Unit tests | 16 passed; 0 failures/errors |
| Android connected tests | 20 passed; 0 failures/errors/skipped, Android 15/API 35 emulator |
| Tablet UI tests | 2 passed at 1600x2560, density 240 and 1.3 font scale |
| Actual process-death check | Passed: separate process restored queue, 750 ms position, shuffle/repeat, remaining paused |
| Lint | 0 errors, 1 warning suggesting a newer Gradle wrapper |
| Debug and optimized release builds | Passed |
| APK inspection | Package/SDK/permissions/native libraries and 16 KB ZIP alignment checked; debug signature verified; release confirmed unsigned |
| Offline permission gate | Neither APK requests INTERNET or ACCESS_NETWORK_STATE |

The two tablet tests repeat existing test methods under another configuration. The process-death check uses separate seed/verify invocations. These are not 24 distinct Android tests. Physical USB, acoustic quality and all Android versions have not been validated.

MP3, FLAC, WAV, AAC/M4A, Vorbis, Opus, 96 kHz FLAC and ALAC decoded, sought and reached end on the API 35 emulator. ALAC remains device-dependent; sample-accurate gapless output and USB format negotiation are not proved by those tests.

## What is pending

### Priority 1: verify on your actual devices

- Install on OnePlus Nord 2/Android 13 and Lenovo Legion Tab TB320FC/Android 15.
- Complete real folder-picker access, scanning and revoked-permission checks with phone/SD/USB storage providers.
- Test DAC/IEM connection, saved profiles, default pause on unplug, speaker leakage, safe reconnection and repeated route changes.
- Test calls, alarms, lock screen, long background playback and headset buttons.
- Verify real music metadata/artwork, audio formats, source-versus-output diagnostics, Direct/Enhanced behavior and gapless boundaries.
- Complete TalkBack, enlarged-text, theme/dialog/system-bar and touch-target checks on both devices.

### Priority 2: finish broader validation and release preparation

- Measure real file scans at approximately 100/1,000/10,000 files, scrolling performance, long-session stability, memory/battery behavior and low-storage handling.
- Test API 26 compatibility at runtime; API 26 is currently a declared minimum, not an executed-device result.
- Supply an owner-controlled signing key for a production-signed APK. No key has been generated or publication performed. The unsigned release cannot be installed as delivered.
- Run release-variant runtime regression after deliberate signing, then decide whether the remaining product gaps are acceptable for release.

### Priority 3: remaining product features and limits

- Advanced parametric EQ with adjustable frequency/Q, crossfade, acoustic loudness normalization and Opus R128 gain handling are not implemented.
- Automatic embedded lyrics, sidecar matching and synchronized LRC highlighting are pending; selected local lyric files currently display as plain text.
- Artwork-derived adaptive colors are pending; current theme colors are curated.
- Continuous MediaStore indexing/automatic file watching is not implemented; scans are explicit and cancellable.
- Exclusive USB drivers, source-rate switching and verified bit-perfect output are not implemented/verified. Pure Direct only bypasses application DSP; Android still owns mixing and routing.
- DSP currently handles PCM16 mono/stereo. Connected-device capability lists do not prove the active negotiated output. Simultaneous-output and identical-device profile identity can be ambiguous.
- AIFF/APE/WMA/CUE/DSD are not advertised as supported; no optional native audio decoder is bundled.
- Queue checkpoints can lag five seconds on abrupt termination; exact shuffled traversal order is not persisted. The interrupted track's history can be lost on abrupt death.
- Duplicate/move matching uses size plus the first 128 KB hash, not a full-file identity guarantee.
- Backups merge already-authorized library matches. They do not restore music, grants, artwork or lyrics; exported history is capped at 100,000 records and import size at 16 MB.

## Files and checksums

All locations below are relative to `D:\AndroidProjects\Auralis` unless stated otherwise.

| Deliverable | Location | Size |
|---|---|---:|
| Installable debug APK | `artifacts\auralis-0.1.0-debug.apk` | 15,937,066 bytes |
| Optimized unsigned release | `artifacts\auralis-0.1.0-release-unsigned.apk` | 2,393,264 bytes |
| Android Studio source ZIP | `artifacts\auralis-0.1.0-source.zip` | 756,987 bytes |
| Live source project | `D:\AndroidProjects\Auralis` | Open this directory in Android Studio |
| This consolidated handoff | `AURALIS_HANDOFF.md` | Current-folder document |
| Full test evidence | `docs\TEST_REPORT.md`, `artifacts\connected-final.xml`, `artifacts\device-gate.log` | Details included below |
| APK inspection/checksums | `artifacts\APK_REPORT.md`, `SHA256SUMS.txt`, `SOURCE_SHA256SUMS.txt` | Binary identities |
| Dependency inventory/licenses | `artifacts\dependencies.txt`, `dependencies.cdx.json`, `dependencies-licenses.md` | 81 resolved release components |
| UI screenshots | `artifacts\phone.png`, `artifacts\tablet.png` | Captured emulator screens |

SHA-256:

```text
4bc12725bee4b5c1d80021a0aa357bc56bdd17908350942d3683abcc59038028  auralis-0.1.0-debug.apk
ed5e439844bb369757685e3e115108911c97df4aaf9a1b883a3cc7a7bded24a0  auralis-0.1.0-release-unsigned.apk
6eb32bdcb4bc9361c7967822ebe98101c0d87eab0aafd896dc47e2765591f7ab  auralis-0.1.0-source.zip
```

The source ZIP is the previously packaged build snapshot. This newly created handoff file lives beside the project and is not inside that existing ZIP. The APKs and ZIP have not been rebuilt for this documentation request.

## Install and start using the app

Enable Developer options and USB debugging on the device, connect it to Windows, and accept the device authorization prompt. In PowerShell:

```powershell
cd D:\AndroidProjects\Auralis
. .\scripts\env.ps1
adb devices -l
adb install -r .\artifacts\auralis-0.1.0-debug.apk
adb shell am start -n com.auralis.player/.MainActivity
```

If several devices are connected, add `-s DEVICE_SERIAL` immediately after `adb`. Alternatively, copy the debug APK to the device, open it and allow that file manager to install unknown apps.

In Auralis, tap **Add folder**, select a music subfolder in Android's picker, grant access and let scanning finish. Open a track to play. The bottom player opens Now Playing, queue and source details. Use Privacy & settings for history, recommendations, themes and local backups. Start with low system volume when trying a DAC/IEM.

## Single best next step

**Install the debug APK on the OnePlus Nord 2, scan one real music folder, and perform the low-volume USB unplug/reconnect checklist below.** This checks the most important behavior the emulator cannot prove. After that, repeat on the Lenovo tablet and address any observed failures before production signing.

## Detailed appendices

The following sections reproduce the current detailed project reports for a self-contained handoff. Their results are the recorded build/test results above; no new build or test run was performed just to save this document.


---

Source: docs/TEST_REPORT.md

# Auralis 0.1.0 validation report

Date: 2026-09-13. This is a tested development preview with the remaining product gaps in KNOWN_LIMITATIONS.md. It is not a certification of physical USB audio, all Android versions, or every BUILD_PROMPT.md acceptance item.

## Final executed results

| Check | Status | Exact result / evidence |
|---|---|---|
| Debug compilation | Automated pass | `assembleDebug`; final application changes built in `artifacts/corrupt-media-gate.log` |
| JVM tests | Automated pass | **16 tests, 0 failures, 0 errors**: audio 5, library rules 4, recommendations 7; `artifacts/unit-test-summary.json` |
| Android Lint | Automated pass | **0 errors, 1 warning**; wrapper update suggestion; `artifacts/lint-final.xml` |
| Connected Android regression | Automated pass | **20 tests, 0 failures, 0 errors, 0 skipped** on API 35; `artifacts/connected-final.xml`, `artifacts/device-gate.log` |
| Process death | Automated pass | Two separate instrumentation invocations across `am force-stop`; different process ID asserted; queue, 750 ms position, shuffle and repeat restored paused; `artifacts/process-restore-seed.log`, `process-restore-verify.log` |
| Tablet and enlarged text | Automated pass | **2 UI tests, 0 failures**, rerun at 1600x2560, density 240, font scale 1.3; `artifacts/tablet-tests.log` |
| Screenshot inspection | Manual pass | Agent inspected captured phone and tablet first-run screens: visible actions, bottom navigation / rail, readable text without overlap; `artifacts/phone.png`, `tablet.png` |
| Offline merged-manifest / dependency gate | Automated pass | Debug and release gates reject INTERNET, ACCESS_NETWORK_STATE and listed tracker/network SDKs; build logs |
| Optimized release / final APK inspection | Automated pass | Release build succeeded in 4m 4s; both APKs passed 16 KB ZIP alignment, package/SDK/permission/native-library inspection; debug v2 signature verified, release confirmed unsigned; `artifacts/APK_REPORT.md` |
| Physical OnePlus / Lenovo USB tests | Not tested | No physical DAC/IEM or either target device attached; follow HARDWARE_TESTS.md |
| Production signing | Blocked | No owner release key supplied; unsigned release and signing instructions provided |

The tablet tests repeat two of the 20 test methods. The process check uses the same restoration test with separate seed/verify arguments. These are additional configurations and lifecycle evidence, not 24 distinct Android test methods.

## Coverage

The 20 connected tests comprise two PCM/DSP tests, two Room DAO tests, two import-safety tests, one real media-session/service test, one schema migration test, one navigation test, eight decoder tests, one queue-restoration test, one product UI test and one scanner integration test.

* Audio: byte-preserving application DSP bypass, empty-buffer drain regression, enhanced balance/peak bounds, profile identity/mode and ReplayGain/headroom calculations.
* Persistence/import: upsert and database reopen, listening-history erasure, validated presets, malformed/oversized import rejection, idempotent authorized-library backup merge, schema 1 to 2 migration preserving a song, favourite and theme while adding the empty mood field. The migration loads the stored version-1 schema fixture and Room validates the complete version-2 schema on open. The version-1 fixture represents the development schema before mood was added; there is no previously published production app upgrade claim.
* Service: real playback, foreground media notification, background continuity, system media-pause key dispatch, queue move/remove, activity recreation and safe paused restoration.
* Scanning: private read-only provider queries, scan/rescan identity, move identity retaining favourites, duplicate copies, deletion, provider failure retaining the last complete snapshot, corrupt MP3/truncated WAV fallback, Unicode title and long album bounded to 2,048 characters.
* UI: first-run folder action, search, play/pause, seek accessibility label, queue reorder/save-to-playlist, navigation, Light/Dark/AMOLED selection and confirmed history reset. Tablet repetition uses 1.3 text scale. Not every control or surface has an assertion.
* Recommendations: deterministic ranking, explanation, skip/fatigue penalties, diversity, exclusions, smart membership, old favourites and removal of history affinity. Ranking measurements use 100, 1,000 and 10,000 synthetic metadata entries.
* Formats: MP3, FLAC, WAV, AAC/M4A, Vorbis, Opus, 96 kHz FLAC and ALAC decoded, sought and reached end on this emulator. See AUDIO_FORMATS.md and `artifacts/decoder-evidence.log` for limits.

## Environment and commands

Windows host: Intel Core i5-9300H, approximately 8 GB RAM, Windows Hypervisor Platform. Project-local Temurin Java 17.0.20.1+1, Gradle 9.6.0, AGP 9.4.0, Kotlin 2.4.20, KSP 2.3.12, compile/target SDK 37, Build Tools 36.0.0. AOSP Android 15/API 35 x86_64 emulator, emulator 37.1.11, serial emulator-5554. Dependencies and tool versions were verified against official sources during provisioning; see DEPENDENCIES.md. Minimum API 26 is declared, but API 26 execution is not tested.

PowerShell equivalents actually executed, from the workspace root:

```powershell
. .\scripts\env.ps1
.\gradlew.bat clean assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest verifyReleaseOffline --console=plain
# Following the documented migration-test compile fix:
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest verifyReleaseOffline --offline --console=plain
# Following the corrupt-media regression fix:
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --offline --console=plain
.\scripts\run-device-tests.ps1 -Serial emulator-5554
.\scripts\run-process-restore.ps1 -Serial emulator-5554
.\scripts\test-tablet.ps1 -Serial emulator-5554
.\gradlew.bat assembleRelease verifyReleaseOffline dependencyInventory --offline --console=plain
.\scripts\inspect-apks.ps1
.\scripts\dependency-licenses.ps1
```

`run-device-tests.ps1` invokes `connectedDebugAndroidTest`, reads real XML counts and refuses a zero-test pass. It provides explicit `adb am instrument` fallback only when Gradle reports zero tests. The final Gradle connected run itself passed all 20; fallback was not needed. Device tests seed/erase local test data and listening history: use a test installation. After connected tests, reinstall the debug and test APKs if Gradle removed them before running the standalone scripts.

## Measured performance

Single debug/emulator samples, not production thresholds: cold launch `am start -W` TotalTime **2,440 ms**, foreground return WaitTime **155 ms** (Android reported UNKNOWN launch state, so this is not labeled a warm process start). First-run idle memory **82,631 KB PSS**, **207,012 KB RSS**; `artifacts/performance.txt`. Tablet launch after tests measured 2,324 ms separately.

The final JVM rank benchmark measured **16 / 25 / 115 ms** for **100 / 1,000 / 10,000** entries. This measures the ranking function, not storage scanning. A provisional same-host regression target is under 5 seconds for a debug cold launch and under 1 second to rank 10,000 entries; these need repeated measurements before becoming CI gates. Provider scan throughput for 100/1,000/10,000 files, scrolling frame timing, battery use, long playback and route churn remain **Not tested**.

## Phase evidence and artifacts

| Phase | Changes and evidence | Outcome / next acceptance |
|---|---|---|
| 0 | Empty source workspace inspected; both prompts read; isolated toolchain provisioned; IMPLEMENTATION_PLAN.md and DEPENDENCIES.md | No pre-existing build baseline; tool/download failures retained in artifacts |
| 1 | Gradle/app foundation, Room/SAF library, Media3 service/controller, Compose shell; `foundation-gate.log` | Debug build, initial unit/lint and instrumentation compilation passed; runtime validation followed later |
| 2 | AudioRouting, AudioProfile, IemProcessor, preset validation and diagnostics; phase build logs and final AudioRules/AudioProcessor suites | Final 5 JVM audio tests and 2 device PCM tests pass; physical routing remains Not tested |
| 3 | ListeningTracker, Recommendations, local mixes/history/privacy and backup; `phase3-gate.log` | 15 unit tests plus compile/lint passed at that point; final recommendation suite has 7 tests |
| 4 | Adaptive AppShell/NowPlaying/playlist/privacy UI, malformed-file and migration/process hardening | Final 20 connected tests, 2 tablet repetitions and process-death check pass; full hardware/accessibility matrix remains Not tested |
| 5 | Optimized build, offline gates, APK inspection, CycloneDX inventory, license declarations, signing/docs/source packaging | Debug installable; release unsigned; no production signing or publication |

Earlier development APKs were overwritten by subsequent builds; no historical hash is invented. The final binaries below supersede those phase artifacts. Both are version **0.1.0 / code 1**, `com.auralis.player`, minimum SDK **26**, target/compile SDK **37**, ABIs arm64-v8a/armeabi-v7a/x86/x86_64. The sole native library is AndroidX graphics path support.

| Final file under artifacts/ | Bytes | SHA-256 |
|---|---:|---|
| auralis-0.1.0-debug.apk | 15,937,066 | `4bc12725bee4b5c1d80021a0aa357bc56bdd17908350942d3683abcc59038028` |
| auralis-0.1.0-release-unsigned.apk | 2,393,264 | `ed5e439844bb369757685e3e115108911c97df4aaf9a1b883a3cc7a7bded24a0` |

Source workspace: `D:\AndroidProjects\Auralis`; portable archive: `artifacts/auralis-0.1.0-source.zip`. Source checksum is separate in SOURCE_SHA256SUMS.txt to avoid a self-referential archive checksum. Source includes wrapper, schemas, test fixtures, scripts, docs and selected evidence; excludes tool downloads, build caches, local.properties and signing keys.

## Failures found and corrected

Raw earlier failure logs are preserved; none are counted as final passes. Initial tooling failures included SDK/dependency download timeouts, SDK 36 versus dependency minimum 37, compiler daemon access and a temporary AAPT2 fetch problem. The compatible toolchain, official alternate Google Maven URL and in-process compiler resolved build access issues.

An initial private DocumentsProvider declaration crashed application startup because that base class requires exported/provider permissions. The debug test seam is now a private ContentProvider implementing the required DocumentsContract query shape; it is absent from release. A real Media3 service crash when draining an empty PCM buffer was fixed and has a regression test. The new corrupt-media test found an uncaught extractor IOException that aborted the folder snapshot; it now retains fallback metadata and continues.

Test failures included offscreen lazy-list lookup, a backgrounded ActivityScenario recreation, a JUnit cleanup return type, a BOM in the exported old schema, optional absent index arrays, and Room test-helper serialization ABI incompatibility. Migration coverage now uses the schema SQL directly with real Room migration/schema validation; no migration assertion was removed.

Earlier Android tooling runs returned zero tests or a report-listener exception; neither was accepted as success. One later connected run correctly reported the corrupt-scanner failure. The final connected report contains all 20 passes. Logs include `instrumentation-verified.log` (13/17 passed), `runtime-fixes-tests.log` (16/19), `regression-tests.log` (19/20), `device-gate-before-final.log` (scanner failure), and the passing focused/final logs.

## Warnings and remaining limits

Lint suggests Gradle 9.7.1; the verified compatible wrapper remains pinned to 9.6.0. The SDK attempted to initialize host metrics outside the workspace and was denied; builds completed. Native graphics symbols could not be stripped without an NDK; there is no optional native audio decoder. A unit-test manifest merge warns that network-state permission removal had no matching declaration in that test variant; final app variants are inspected independently.

**Not tested:** actual Android folder-picker grant completion and revocation on OEM/USB providers, physical USB unplug timing/zero speaker leakage, call/alarm behavior, DAC indicators, exclusive/bit-perfect delivery, gapless sample boundaries, extended sessions, full accessibility/TalkBack and system-bar/dialog contrast matrix, low storage, broad real-media metadata/artwork variety and installation on API 26/physical API 33/35. A failed provider snapshot is covered; that does not prove every revoked-grant scenario. No packet capture was performed; absence of Internet permission is verified instead. The optimized unsigned release cannot be installed until deliberately signed; runtime tests use the debug variant.

Best next validation: install the debug APK on one target device, choose a real music folder, then run the low-volume USB disconnect/reconnect checklist before relying on the app for everyday IEM playback.


---

Source: docs/KNOWN_LIMITATIONS.md

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


---

Source: docs/AUDIO_FORMATS.md

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


---

Source: docs/HARDWARE_TESTS.md

# Physical USB-C/IEM validation

Run separately on OnePlus Nord 2 / Android 13 and Lenovo Legion Tab TB320FC / Android 15. Record build SHA-256, device/OS build, DAC firmware/name, IEM, source fixture, system volume, date and tester. Initial result for every row below is **Not tested**. Use exactly Automated pass, Automated fail, Manual pass, Manual fail, Blocked, or Not tested.

Keep system volume low. Use generated fixtures; do not use protected streaming material. Obtain a second observer or loopback measurement where safe speaker-leakage timing is important. An absent audible sound alone does not prove zero leaked samples.

| Action | Required observation |
|---|---|
| Connect USB while stopped | Device list updates; saved profile identified or ambiguity disclosed; no autoplay |
| Connect USB while playing | Route follows Android; profile update does not crash; log audible discontinuities |
| Rename profile to “KA11 + KZ IEM”, disconnect/reconnect | Correct preset restored without autoplay |
| Play MP3 44.1 kHz and FLAC 44.1/48/96 kHz; include a native 24-bit fixture | Source fields match fixture; device capability list is not presented as negotiated format |
| Select Pure Direct with nonzero saved EQ/balance/crossfeed | App DSP bypassed; system mixing disclosed; do not infer bit-perfect from a DAC LED |
| Select Enhanced with a mild negative EQ curve | Audible/measurable filter response; attenuation and balance function; PCM support reported |
| Enable limiter and feed safe synthetic peaks | Peaks limited at processor output; compare measured response; this is hard peak limiting |
| Remove USB while playing | Immediate default pause, no unexpected speaker continuation; record actual leakage/timing |
| Reconnect after removal | Remains paused until explicit play |
| Repeat connect/remove 20 times | No crash, leaked service, unintended resume or wrong profile |
| Switch between USB, wired analog, Bluetooth and speaker | Separate settings restored; simultaneous-output ambiguity checked |
| Receive/end call; alarm; transient notification | Appropriate focus loss/duck/pause/recovery; call privacy checked |
| Lock screen, background 30 min, media-button play/pause/next | Playback/session/notification controls stay synchronized |
| Force-stop and reopen; revoke folder permission | Paused queue recovery, useful unavailable-file state; no hidden scanning |
| Change Light/Dark/AMOLED/System; enlarge font; TalkBack | Player, dialogs, bars, menus and controls readable on both devices |

PowerShell evidence commands (after `. .\scripts\env.ps1`):

```powershell
adb devices -l
adb shell getprop ro.build.fingerprint
adb shell dumpsys audio > artifacts\device-audio.txt
adb shell dumpsys media_session > artifacts\device-media-session.txt
adb shell dumpsys media.audio_flinger > artifacts\device-audio-flinger.txt
adb logcat -d -v threadtime > artifacts\device-logcat.txt
adb shell screencap -p /sdcard/auralis-screen.png
adb pull /sdcard/auralis-screen.png artifacts\device-screen.png
```

Device dumps may include private data from other applications. Keep these captures local and review before sharing. Copy Auralis's diagnostic report from Equalizer and append fixture parameters, DAC indicators, exact observed result, and any mismatch. System PCM/direct/exclusive assertions require technical route evidence; the app does not advertise them as verified.


---

Source: docs/RELEASE.md

# Release and reproducible build

This development build uses application ID `com.auralis.player`, version 0.1.0 / code 1, minimum API 26. Compile/target SDK are pinned in app/build.gradle.kts. Rename the display label only in app/src/main/res/values/strings.xml.

Use Java 17 and the wrapper. In Windows PowerShell from the source root:

```powershell
. .\scripts\env.ps1
.\gradlew.bat clean assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest verifyReleaseOffline
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

Dependency downloads need network access on the build computer; the installed application has no Internet permission. `.tools`, local.properties, keystores and signing secrets are excluded by .gitignore. Initial build tools are project-local; ordinary Android Studio users may set JAVA_HOME and ANDROID_HOME to their existing tools before running env.ps1. The wrapper distribution is SHA-256 pinned. Exact byte-for-byte APK reproduction additionally requires the same toolchain and debug signing key; debug key generation is machine-dependent.

No release key was supplied. `assembleRelease` therefore produces **unsigned** output, never a debug-signed release. To sign deliberately in PowerShell:

```powershell
. .\scripts\env.ps1
.\gradlew.bat assembleRelease verifyReleaseOffline
# Once only, choose a private directory outside the project; keytool prompts for passwords.
keytool -genkeypair -v -keystore C:\secure-location\auralis-release.jks -alias auralis -keyalg RSA -keysize 4096 -validity 10000
& "$env:ANDROID_HOME\build-tools\36.0.0\zipalign.exe" -P 16 -f 4 app\build\outputs\apk\release\app-release-unsigned.apk artifacts\auralis-release-aligned.apk
& "$env:ANDROID_HOME\build-tools\36.0.0\apksigner.bat" sign --ks C:\secure-location\auralis-release.jks --ks-key-alias auralis --out artifacts\auralis-release.apk artifacts\auralis-release-aligned.apk
& "$env:ANDROID_HOME\build-tools\36.0.0\apksigner.bat" verify --verbose --print-certs artifacts\auralis-release.apk
Get-FileHash artifacts\auralis-release.apk -Algorithm SHA256
```

Back up the release key privately. Debug and release signatures differ; installing one over the other requires a matching signature or uninstalling first, which deletes app data. Export local app data before intentionally uninstalling. No publication, Play upload or key generation is performed automatically.


---

Source: docs/PRIVACY.md

# Privacy and permissions

The normal release requests only foreground-service, media-playback foreground-service and wake-lock permissions. SAF read grants are obtained from Android's folder picker; no broad media/storage permission, microphone, location, contacts or Bluetooth identity permission is requested. Playback media-session notifications are exempt from Android 13's ordinary notification permission requirement.

There is no INTERNET permission, WebView, account, telemetry, advertising, remote configuration, cloud AI or crash-upload SDK. Android backup and device-transfer rules exclude app data. The manifest's only app-defined exported component is the launcher activity; it accepts no data import or file URI extras. PlaybackService is private. Any AndroidX exported component must be secured by its declared platform permission and is included in the final manifest report.

The build's verifyDebugOffline/verifyReleaseOffline tasks inspect the merged manifest and resolved runtime dependencies, fail on Internet permission, and reject named tracker/network SDK families. These tasks are dependencies of assemble/bundle, so transitive manifest changes are included. The build computer downloads development dependencies. Media3's common data-source library contains upstream networking implementations; this is disclosed and unreachable for network playback through the app's local library flow and permission boundary.

The app stores file URIs and normalized metadata, folder grants, favourites, playlists, queue checkpoints, output profiles and minimal listening events. Clear history removes listening records in a transaction and sets an epoch that prevents delayed service writes from resurrecting erased events. Forget-song/artist uses equivalent per-song epochs. Exclusions and history pause remain explicit settings. No sensitive mood inference is performed.

Local JSON import/export is user initiated. App imports are schema/range/size validated before transactional mutation, only match songs already in the authorized library, never manufacture URI grants, and deduplicate history records. Export includes personal listening history, preferences and fingerprints. Lyrics loaded by the user are stored locally and are not included in backup exports. Presets have a 16 KB limit; app backup has a 16 MB input limit. Backups do not embed music or artwork.

Diagnostics are copied only when the user taps Copy audio diagnostic. They contain device/profile and audio capability information but no full file URI or listening history. Application error strings omit private source paths. Raw development adb reports can contain device data and should remain local.

Permission enforcement is verified from the final APK. A denied Internet permission is a platform isolation guarantee; it is not a claim of a performed packet capture. Final test outcomes and untested security conditions are in TEST_REPORT.md.

Final release inspection confirms three Android permission requests (foreground service, media-playback foreground service, wake lock) plus AndroidX's app-scoped signature permission for non-exported dynamic receivers. INTERNET and ACCESS_NETWORK_STATE are absent. The launcher is exported as required. AndroidX Media3's BluetoothValidationActivity is protected by BLUETOOTH_PRIVILEGED, and ProfileInstallReceiver by DUMP; neither grants those permissions to the app. The playback/Room services and startup provider are private. The test fixture provider is absent from release. The release is not debuggable and Android backup is disabled. Full decoded manifests are in artifacts/debug-manifest-tree.txt and release-manifest-tree.txt.


---

Source: docs/DEPENDENCIES.md

# Dependencies and licenses

Versions are pinned in `gradle/libs.versions.toml` and Gradle's wrapper checksum. Verification date: 2026-09-12. Official Maven metadata was checked in addition to release pages because documentation snippets can lag the published version.

| Component | Selected | License / role |
|---|---|---|
| Android Gradle plugin | 9.4.0 | Apache-2.0, build only |
| Gradle | 9.6.0 | Apache-2.0, build only |
| Kotlin / Compose compiler | 2.4.20 | Apache-2.0 |
| KSP | 2.3.12 | Apache-2.0, Room generation only |
| Compose BOM | 2026.09.00 | Apache-2.0, UI/Material 3 |
| AndroidX Activity | 1.13.0 | Apache-2.0 |
| Lifecycle | 2.11.0 | Apache-2.0 |
| Media3 | 1.11.1 | Apache-2.0, local extractor/player/session |
| Room | 2.8.5 | Apache-2.0, SQLite persistence |
| JUnit | 4.13.2 | EPL-1.0, test only |
| AndroidX Test | runner 1.7.0 / ext 1.3.0 | Apache-2.0, test only |
| Eclipse Temurin JDK | 17.0.20.1+1 | GPLv2 with Classpath Exception, build only |

No HLS/DASH, OkHttp, Retrofit, WebView, cloud AI, tracking or crash-upload dependency is requested. Media3's core data-source artifact contains generic upstream implementations, but the app only creates locally scanned media items and has no Internet permission. This shared upstream code is disclosed; it is not a hidden network service.

Official references: [AGP compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes), [built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin), [Kotlin releases](https://kotlinlang.org/docs/releases.html), [Media3](https://developer.android.com/jetpack/androidx/releases/media3), [Room](https://developer.android.com/jetpack/androidx/releases/room), [Compose BOM](https://developer.android.com/develop/ui/compose/bom/bom-mapping), [KSP](https://github.com/google/ksp/releases).

The resolved transitive inventory is in `artifacts/dependencies.txt`, with a CycloneDX 1.5 component inventory in `dependencies.cdx.json`. `scripts/dependency-licenses.ps1` reads the corresponding cached Maven POM license declarations into `artifacts/dependencies-licenses.md`. Undeclared licenses are labeled for upstream review; no paid or proprietary decoder is bundled.


---

Source: artifacts/dependencies-licenses.md

# Resolved release dependency licenses

License declarations read from the exact resolved Maven POMs in the local build cache. This inventory precedes R8 shrinking; build/test tooling is documented separately in docs/DEPENDENCIES.md. Preserve applicable upstream notices when redistributing.

| Component | Declared license | License reference |
|---|---|---|
| androidx.activity:activity-compose:1.13.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.activity:activity-ktx:1.13.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.activity:activity:1.13.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.annotation:annotation-experimental:1.5.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.annotation:annotation-jvm:1.9.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.arch.core:core-common:2.2.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.arch.core:core-runtime:2.2.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.autofill:autofill:1.0.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.collection:collection-jvm:1.5.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.collection:collection-ktx:1.5.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.animation:animation-android:1.12.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.animation:animation-core-android:1.12.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.foundation:foundation-android:1.12.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.foundation:foundation-layout-android:1.12.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.material3:material3-android:1.4.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.material:material-ripple-android:1.12.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.runtime:runtime-android:1.12.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.runtime:runtime-annotation-android:1.12.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.runtime:runtime-retain-android:1.12.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.runtime:runtime-saveable-android:1.12.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.ui:ui-android:1.12.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.ui:ui-geometry-android:1.12.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.ui:ui-graphics-android:1.12.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.ui:ui-text-android:1.12.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.ui:ui-tooling-preview-android:1.12.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.ui:ui-unit-android:1.12.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.compose.ui:ui-util-android:1.12.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.concurrent:concurrent-futures:1.2.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.core:core-ktx:1.18.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.core:core-viewtree:1.0.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.core:core:1.18.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.customview:customview-poolingcontainer:1.0.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.emoji2:emoji2:1.4.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.exifinterface:exifinterface:1.3.6 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.graphics:graphics-path:1.0.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.interpolator:interpolator:1.0.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.lifecycle:lifecycle-common-java8:2.11.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.lifecycle:lifecycle-common-jvm:2.11.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.lifecycle:lifecycle-livedata-core:2.11.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.lifecycle:lifecycle-process:2.11.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.lifecycle:lifecycle-runtime-android:2.11.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.lifecycle:lifecycle-runtime-compose-android:2.11.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.lifecycle:lifecycle-runtime-ktx-android:2.11.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.lifecycle:lifecycle-service:2.11.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.lifecycle:lifecycle-viewmodel-android:2.11.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.lifecycle:lifecycle-viewmodel-compose-android:2.11.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.lifecycle:lifecycle-viewmodel-savedstate-android:2.11.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.media3:media3-common:1.11.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.media3:media3-container:1.11.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.media3:media3-database:1.11.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.media3:media3-datasource:1.11.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.media3:media3-decoder:1.11.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.media3:media3-exoplayer:1.11.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.media3:media3-extractor:1.11.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.media3:media3-session:1.11.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.media:media:1.7.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.navigationevent:navigationevent-android:1.0.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.navigationevent:navigationevent-compose-android:1.0.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.profileinstaller:profileinstaller:1.4.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.room:room-common-jvm:2.8.5 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.room:room-runtime-android:2.8.5 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.savedstate:savedstate-android:1.4.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.savedstate:savedstate-compose-android:1.4.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.savedstate:savedstate-ktx:1.4.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.sqlite:sqlite-android:2.6.2 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.sqlite:sqlite-framework-android:2.6.2 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.startup:startup-runtime:1.1.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.tracing:tracing:1.2.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.versionedparcelable:versionedparcelable:1.1.1 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.window:window-core-android:1.5.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| androidx.window:window:1.5.0 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| com.google.guava:failureaccess:1.0.2 | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| com.google.guava:guava:33.3.1-android | Apache License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava | The Apache Software License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| org.jetbrains.kotlin:kotlin-stdlib:2.4.20 | Apache-2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |
| org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0 | The Apache Software License, Version 2.0 | https://www.apache.org/licenses/LICENSE-2.0.txt |
| org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0 | The Apache Software License, Version 2.0 | https://www.apache.org/licenses/LICENSE-2.0.txt |
| org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.7.3 | The Apache Software License, Version 2.0 | https://www.apache.org/licenses/LICENSE-2.0.txt |
| org.jetbrains:annotations:23.0.0 | The Apache Software License, Version 2.0 | https://www.apache.org/licenses/LICENSE-2.0.txt |
| org.jspecify:jspecify:1.0.0 | The Apache License, Version 2.0 | http://www.apache.org/licenses/LICENSE-2.0.txt |


---

Source: artifacts/APK_REPORT.md

# APK inspection

## debug

File: auralis-0.1.0-debug.apk; bytes: 15937066; SHA-256: 4bc12725bee4b5c1d80021a0aa357bc56bdd17908350942d3683abcc59038028
Signature: Android Debug; apksigner verification passed.

package: name='com.auralis.player' versionCode='1' versionName='0.1.0' platformBuildVersionName='17' platformBuildVersionCode='37' compileSdkVersion='37' compileSdkVersionCodename='17'
minSdkVersion:'26'
targetSdkVersion:'37'
uses-permission: name='android.permission.FOREGROUND_SERVICE'
uses-permission: name='android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK'
uses-permission: name='android.permission.WAKE_LOCK'
uses-permission: name='com.auralis.player.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
native-code: 'arm64-v8a' 'armeabi-v7a' 'x86' 'x86_64'
Native entries:
lib/arm64-v8a/libandroidx.graphics.path.so
lib/armeabi-v7a/libandroidx.graphics.path.so
lib/x86/libandroidx.graphics.path.so
lib/x86_64/libandroidx.graphics.path.so

## release

File: auralis-0.1.0-release-unsigned.apk; bytes: 2393264; SHA-256: ed5e439844bb369757685e3e115108911c97df4aaf9a1b883a3cc7a7bded24a0
Signature: UNSIGNED; apksigner rejection is expected. No release key was supplied.

package: name='com.auralis.player' versionCode='1' versionName='0.1.0' platformBuildVersionName='17' platformBuildVersionCode='37' compileSdkVersion='37' compileSdkVersionCodename='17'
minSdkVersion:'26'
targetSdkVersion:'37'
uses-permission: name='android.permission.FOREGROUND_SERVICE'
uses-permission: name='android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK'
uses-permission: name='android.permission.WAKE_LOCK'
uses-permission: name='com.auralis.player.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
native-code: 'arm64-v8a' 'armeabi-v7a' 'x86' 'x86_64'
Native entries:
lib/arm64-v8a/libandroidx.graphics.path.so
lib/armeabi-v7a/libandroidx.graphics.path.so
lib/x86/libandroidx.graphics.path.so
lib/x86_64/libandroidx.graphics.path.so

