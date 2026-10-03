# Auralis

A native, private Android music player for local files. Kotlin, Compose Material 3, Media3 and Room. Application ID: `com.auralis.player`; minimum Android 8.0 (API 26). The application has no Internet permission, account, analytics, advertisements or cloud service.

This is a development release. See [test evidence](docs/TEST_REPORT.md) and [limitations](docs/KNOWN_LIMITATIONS.md) before treating it as production ready. USB behavior must be checked on physical hardware.

## Windows setup

Open this folder in Android Studio. Install SDK Platform 37, SDK Build Tools 36.0.0 and Platform Tools. Use Java 17. The project includes a checksum-pinned Gradle wrapper and dependency version catalog. For the toolchain provisioned in this workspace:

```powershell
cd D:\AndroidProjects\Auralis
. .\scripts\env.ps1
.\gradlew.bat clean
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest verifyReleaseOffline
```

On another machine, set `JAVA_HOME` and `ANDROID_HOME` to your installed JDK/SDK before sourcing env.ps1. Android Studio may create an ignored local.properties file. Build downloads require Internet on the computer; playback does not. The official Google Maven URL without the extra `/dl/` path is used because the alternative path timed out in this environment. The Kotlin compiler runs in process to avoid separate daemon state and reduce memory overhead.

## Install and use

Enable Developer options and USB debugging on Android, connect by USB, and authorize the computer:

```powershell
. .\scripts\env.ps1
adb devices -l
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.auralis.player/.MainActivity
```

Alternatively copy the APK to the device and open it, granting that file manager permission to install unknown apps. Select **Add folder**, choose a folder in Android's picker, and grant access. The app reads files without altering them. Scan progress, cancellation and rescan are on Home and Library. If a provider revokes access, reselect its folder. Android may restrict selecting storage roots; select a music subfolder instead.

Library groups songs by album, artist, genre and folder and includes favourites and probable copies. Search matches title/artist/album. Open a track to play; the bottom player opens artwork, source quality, queue, seek and playback controls. More actions include play-next, exclusions, forgetting history and a manual music mood tag. The queue can be reordered, trimmed and saved as a playlist. Mixes includes deterministic automatic lists and manual playlist editing. Privacy & settings controls history, recommendations, folder access, local backup and all four themes.

Equalizer offers a profile for the detected connected output, Pure Direct or Enhanced IEM, five bands, preamp/headroom, balance, crossfeed, peak limiter and tagged ReplayGain. Output capabilities are explicitly separated from negotiated format, which this system playback path cannot verify. See the [audio architecture](docs/ARCHITECTURE.md). Start with low system volume when changing IEMs.

## Test

```powershell
. .\scripts\env.ps1
.\scripts\validate.ps1
# Only when adb devices lists an authorized device or a running emulator:
.\gradlew.bat connectedDebugAndroidTest
# Full gate including runtime tests:
.\scripts\validate.ps1 -Connected
# Additional checks on the test emulator (install app and test APKs first):
.\scripts\run-process-restore.ps1 -Serial emulator-5554
.\scripts\test-tablet.ps1 -Serial emulator-5554
```

The workspace includes an API 35 AOSP emulator image under ignored `.tools`, plus `scripts/start-emulator.ps1`. That script starts it headlessly with Windows Hypervisor Platform. On other machines create an API 35 emulator through Android Studio first. If Windows denies Android host-state writes, use a normal permitted desktop PowerShell session. Do not silently disable virtualization or change BIOS settings. Device tests modify test library/settings/history; use a test installation. `run-device-tests.ps1` rejects zero-test reports and runs explicit instrumentation if required.

Unit test reports: `app/build/reports/tests/testDebugUnitTest/`. Lint: `app/build/reports/lint-results-debug.html`. Connected tests: `app/build/reports/androidTests/connected/debug/`. Raw phase logs and packaged reports: `artifacts/`.

Generate original fixtures with `scripts/generate-fixtures.ps1`. Pass `-Ffmpeg C:/path/to/ffmpeg.exe` for compressed formats; FFmpeg is a development-only tool. Compressed fixtures are included in instrumentation assets and never packaged in the normal app. The private debug-only document provider exists solely for scanner integration tests and is absent from release.

## Architecture and delivery

One application module, separate UI/domain/data/playback/audio packages, application-container injection, service-owned ExoPlayer, lifecycle-aware controller, Room transactions, cancellable SAF scanning and local ranking. No background worker or continuous scanner is needed.

* [Product requirements](docs/PRODUCT_REQUIREMENTS.md), [implementation plan](docs/IMPLEMENTATION_PLAN.md), [architecture](docs/ARCHITECTURE.md)
* [Audio compatibility](docs/AUDIO_FORMATS.md), [physical USB checklist](docs/HARDWARE_TESTS.md)
* [Privacy/permissions](docs/PRIVACY.md), [dependency/licenses](docs/DEPENDENCIES.md)
* [Release/signing](docs/RELEASE.md), [release notes](docs/RELEASE_NOTES.md), [test report](docs/TEST_REPORT.md)

No release signing key is supplied. The release build is unsigned until you sign it with your own key using the documented commands. Keystores, passwords, local.properties and local tools are ignored and must not be committed.
