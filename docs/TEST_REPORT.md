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
