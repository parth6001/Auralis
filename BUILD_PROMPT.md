# Astra Master Prompt — Build and Test a Private Offline Android Music Player

## Role

Act as a Principal Android Engineer, Audio Application Architect, Kotlin/Jetpack Compose Expert, Android Media3 Expert, USB Audio Engineer, Privacy Engineer, UI/UX Designer, QA Automation Architect, and Release Engineer.

Your task is to design, implement, test, document, and package a production-quality native Android application for private offline music playback. Work autonomously, but never hide errors, claim an unperformed test, or silently remove a requirement.

## Working approach

1. Inspect the supplied repository and environment before editing anything.
2. Read all existing documentation, source files, build files, tests, and repository instructions.
3. Preserve working code and user changes. Do not regenerate or replace the project blindly.
4. Create a concise implementation plan with phases, dependencies, risks, and acceptance criteria.
5. Build the smallest working vertical slice first, then add features incrementally.
6. After every phase, compile, run relevant tests, fix failures, and record evidence.
7. If information is missing, choose a safe, reversible default and document it. Ask me only when a decision is irreversible, security-sensitive, or materially changes the product.
8. Do not stop after producing sample code or a plan. Continue until a buildable project and APK are produced, unless an external blocker makes that impossible.

## Product objective

Build a lightweight, modern, private Android music player for songs already stored on the user's phone, tablet, SD card, or user-selected USB storage. It must provide excellent local playback, USB-C IEM/DAC support, device-specific audio profiles, intelligent offline recommendations, automatic playlists, and a polished interface.

The application must not download, extract, decrypt, record, or convert protected music from Spotify, YouTube Music, Apple Music, or other streaming services. It may import normal audio files that the user owns or is authorized to use. Do not add DRM bypassing, stream ripping, scraping, or hidden network services.

## Initial product defaults

- Working app name: `Auralis` (make the display name easy to change from one resource).
- Default package/application ID: `com.auralis.player` unless an existing valid ID is present.
- Platform: native Android.
- Language: Kotlin.
- UI: Jetpack Compose with Material 3.
- Minimum Android version: Android 8.0/API 26.
- Target and compile SDK: newest stable SDK installed and supported by the toolchain.
- Primary physical devices: OnePlus Nord 2 on Android 13 and Lenovo Legion Tab TB320FC on Android 15.
- Architecture: clean, modular, testable, and appropriately simple; avoid unnecessary enterprise abstraction.
- Build system: Gradle Kotlin DSL with version catalog when practical.
- Playback foundation: AndroidX Media3/ExoPlayer and `MediaSessionService`.
- Local data: Room.
- Background work: WorkManager only where justified.
- Dependency injection: Hilt or a lightweight alternative; select one and explain briefly.
- No account, advertisements, telemetry, analytics, crash-upload SDK, or cloud AI.
- Exclude the `INTERNET` permission from the normal offline build.

Use current stable dependency versions that are mutually compatible. Verify versions from official documentation when internet access is available. Do not invent version numbers. If offline, use compatible installed/cached versions and record this constraint.

## Functional requirements

### 1. Secure local-library access

- Use Android's Storage Access Framework and MediaStore correctly for modern scoped storage.
- Let the user choose folders rather than requesting broad file access unnecessarily.
- Persist only granted URI permissions.
- Scan selected locations for supported audio files.
- Detect additions, changes, moves, and deletions without duplicating database entries.
- Provide manual rescan, scan progress, cancellation, and useful error states.
- Organize the library by songs, albums, artists, genres, folders, favourites, recently added, recently played, and most played.
- Read embedded title, album, artist, album artist, track/disc number, year, genre, duration, artwork, codec, container, bitrate, sample rate, channel count, and bit depth when available.
- Handle missing or malformed metadata gracefully.
- Never alter the user's source file without explicit confirmation.
- Detect probable duplicates using safe metadata plus file identity/hash strategies without blocking normal scanning.

### 2. Playback

- Play, pause, seek, next, previous, shuffle, repeat one/all/off, queue editing, and play-next.
- Gapless playback when supported.
- Correct audio focus, interruption, headphone-disconnection, call, Bluetooth, and lifecycle behaviour.
- Lock-screen controls, notification controls, headset media buttons, and system media-session integration.
- Resume the last queue and playback position according to user settings.
- Never unexpectedly play through the phone speaker after wired/USB audio is removed; pause by default and offer a setting to change this.
- Support background playback without keeping unnecessary components alive.

### 3. Audio formats

Prioritize reliable support for MP3, FLAC, WAV, AAC/M4A, Ogg Vorbis, Opus, and ALAC. Add AIFF, APE, WMA, CUE, DSF/DSD only when a maintained, legally distributable decoder is available and the size/security impact is acceptable.

Do not claim universal format support. Maintain a tested compatibility matrix containing:

- container and codec;
- Android/API level;
- software or platform decoder;
- seek support;
- gapless result;
- metadata/artwork result;
- test status.

### 4. Honest quality information

On the Now Playing details panel show, when detectable:

- source format and codec;
- source bitrate;
- sample rate and bit depth;
- channel count;
- active output device;
- requested and actual output path/format when Android exposes it;
- active audio processing.

Never describe resampling, transcoding, upsampling, or a DAC's maximum capability as an improvement to the source. Never show “bit-perfect” unless the output path has been technically verified. Use `Direct`, `System mixed`, `DSP active`, or `Bit-perfect verified` as distinct states.

### 5. USB-C IEM and DAC support

- Detect USB audio devices, USB-C digital IEMs, and DAC dongles through supported Android APIs.
- Detect connection and disconnection and update the UI immediately.
- Show the device/product name when Android supplies it; otherwise provide a user-editable profile name.
- Automatically restore the saved profile for that USB device identifier when safely possible.
- Provide two clearly different modes:
  - **Pure Direct Mode:** prioritize original sample rate and minimal processing; disable EQ, normalization, crossfade, crossfeed, and other DSP that prevents bit-perfect output.
  - **Enhanced IEM Mode:** allow EQ, preamp, limiter, ReplayGain, balance, crossfeed, and related processing.
- Attempt exclusive/direct USB output only through a maintained and legally usable implementation. Provide a reliable Media3/system-audio fallback.
- Never advertise exclusive or bit-perfect playback on devices where it cannot be verified.
- Pause on unexpected USB removal and prevent speaker leakage.
- Provide a diagnostic screen with connected device, supported capabilities when available, source parameters, route, processing state, and a copyable diagnostic report.

Android may expose the DAC rather than the passive IEM model. Design profiles accordingly and allow names such as “KA11 + KZ IEM”.

### 6. IEM-focused audio controls

Implement features only through stable, maintainable audio processing APIs:

- simple equalizer mode;
- advanced parametric EQ when technically supported;
- preamp with clipping protection;
- limiter;
- left/right balance;
- ReplayGain track and album modes;
- optional loudness normalization;
- optional crossfeed;
- configurable gapless/crossfade behaviour;
- separate profiles for USB, wired analog, Bluetooth, and speaker outputs;
- import/export of user presets in a documented format;
- safe-volume warning without overriding the user's system controls.

Clearly show when DSP is active. Prevent excessive positive EQ gain by applying appropriate preamp/headroom or warning the user.

### 7. Offline recommendations and privacy

All recommendation data and computation must remain on the device. Store only information necessary for features, such as:

- completed plays;
- partial plays;
- early skips;
- repeats;
- favourites;
- manual dislikes or exclusions;
- last-played time;
- time-of-day/day-of-week patterns;
- artist, album, genre, and optional user mood tags.

Start with an explainable scoring/ranking engine rather than a large ML model. Include recency, affinity, completion rate, skip penalties, diversity, and repetition fatigue. Avoid a feedback loop that recommends only a small number of songs.

Each recommendation must support an explanation such as “Because you often finish this artist in the evening.” Add controls for:

- pause listening history;
- clear all history;
- forget a song/artist;
- never recommend a song/artist;
- reduce repetition;
- disable recommendations completely;
- export/import app data locally.

Do not infer sensitive emotional or health information. Mood labels describe music, not the user's mental state.

### 8. Automatic playlists

Generate and update local smart playlists including:

- Most Played;
- Recently Added;
- Recently Played;
- No-Skip Favourites;
- Forgotten Favourites;
- Morning Mix;
- Evening Mix;
- Weekend Mix;
- Rediscover Old Songs;
- Current Rotation;
- Favourite Artists;
- user-selected mood mixes.

Support manual playlists and queue-to-playlist saving. Ensure automatic playlists are deterministic and testable. Users must be able to pin, hide, refresh, or disable each smart playlist.

For mood classification, begin with embedded metadata and manual tags. If optional local acoustic analysis is later implemented, make it opt-in, explain that it is approximate, run only under user-selected battery/charging conditions, permit corrections, and never upload audio.

### 9. Interface and accessibility

Create an original premium design inspired by the clarity and fluidity of modern mobile systems, but do not copy Apple assets, screens, trademarks, or proprietary design exactly.

Navigation should include Home, Library, Smart Mixes, Equalizer, and Search. The Now Playing experience should include large artwork, adaptive colours, queue access, lyrics display for embedded/local lyric files, favourite, shuffle/repeat, output route, and audio-quality details.

Provide:

- Light, Dark, AMOLED black, and system themes;
- consistent theme application to every screen, dialog, sheet, card, navigation surface, status/navigation bar, and player component;
- smooth but lightweight animations;
- reduced-motion support;
- phone and tablet/adaptive layouts;
- large touch targets;
- TalkBack descriptions;
- scalable text;
- adequate contrast;
- empty, loading, permission-denied, and error states.

Avoid excessive blur, battery-heavy visualizers, or animations that delay controls.

## Privacy and security acceptance criteria

- Normal release manifest contains no `android.permission.INTERNET`.
- No WebView, advertising, analytics, telemetry, remote configuration, or undisclosed network library.
- No exported component unless required and secured.
- Use least-privilege storage and notification permissions.
- Validate imported files/presets and handle corrupted media without crashing.
- Do not log filenames, listening history, full storage URIs, or personal metadata in release builds.
- Provide an in-app Privacy page explaining local data, permissions, deletion, and export.
- Add an automated check that fails the build if the offline release unexpectedly gains Internet permission or prohibited trackers/dependencies.

## Architecture and code-quality requirements

- Separate UI, domain, data, playback/service, audio-routing, and recommendation concerns.
- Keep playback state single-sourced and lifecycle-safe.
- Use immutable UI state and predictable event handling.
- Avoid global mutable state and blocking main-thread I/O.
- Use coroutines and Flow correctly.
- Include database migrations and migration tests.
- Prefer interfaces at meaningful external boundaries, not for every class.
- Document non-obvious audio-routing and scoring logic.
- Enable Kotlin compiler warnings, Android Lint, and a consistent formatter/static analyser.
- Do not suppress warnings broadly merely to make CI green.

## Delivery phases

### Phase 0 — Discovery and baseline

- Inspect repository and toolchain.
- Record Java, Gradle, AGP, Kotlin, SDK, emulator/device availability, and cached dependencies.
- Build and test the untouched baseline if a project already exists.
- Produce the implementation plan and risk register.

### Phase 1 — Minimum viable local player

- Project foundation and theme.
- Folder selection and local scan.
- Library database and primary browsing screens.
- Media3 background playback, queue, notification, and media session.
- MP3/FLAC/WAV/AAC/Opus smoke coverage.
- First installable debug APK.

### Phase 2 — USB-C/IEM and audio quality

- USB audio detection and route state.
- Safe pause-on-disconnect.
- Direct versus Enhanced IEM modes.
- Quality and diagnostic display.
- EQ/profile foundation and clipping protection.

### Phase 3 — Smart local experience

- Listening-event collection.
- Explainable recommendation engine.
- Automatic playlists.
- Privacy controls, reset and exclusions.

### Phase 4 — Product polish

- Adaptive phone/tablet UI.
- Complete themes and transitions.
- Accessibility.
- Performance, power, corrupted-file, large-library, process-death, and migration hardening.

### Phase 5 — Release readiness

- Full regression suite.
- Signed release instructions/build.
- APK/AAB as requested.
- SBOM/dependency list, permission report, privacy verification, test report, known limitations, and reproducible build instructions.

Do not begin advanced recommendations before core playback and persistence are reliable.

## Testing strategy

Build a balanced automated test pyramid.

### Unit tests

Cover at minimum:

- metadata normalization;
- duplicate detection;
- queue operations;
- repeat and shuffle rules with deterministic seeds;
- recommendation scoring, diversity, skip penalties, recency, and repetition fatigue;
- smart-playlist membership;
- IEM profile selection;
- output-mode state transitions;
- ReplayGain/preamp safety calculations;
- privacy settings and history deletion.

### Database and integration tests

- Room DAO operations and migrations;
- rescan idempotency;
- deleted/moved file reconciliation;
- persistence after process recreation;
- playlist and history consistency;
- corrupted or unsupported metadata handling.

### Compose UI tests

- first-run folder selection;
- library navigation and search;
- play/pause/seek/next/previous;
- queue editing;
- theme changes across all surfaces;
- create/edit/delete playlist;
- EQ/profile changes;
- recommendation explanation and exclusion;
- privacy reset confirmation;
- phone and tablet layouts;
- accessibility labels and minimum touch targets.

Use stable semantic test tags only where accessibility semantics are insufficient. Do not rely on fragile coordinates or arbitrary delays.

### End-to-end scenarios

Automate on emulator where possible:

1. Fresh install, grant/select test-media folder, scan fixtures, verify albums/artists/songs.
2. Play a track, background the app, use notification controls, reopen, and verify state.
3. Build and modify a queue, rotate/recreate the activity, and verify persistence.
4. Add/remove/move test media, rescan, and verify reconciliation without duplicates.
5. Play/skip/favourite a controlled sequence, trigger recommendation refresh, and validate expected explanations and smart playlists.
6. Switch Light/Dark/AMOLED themes and verify all primary screens and system bars.
7. Import a malformed file and confirm safe error handling.
8. Clear listening history and confirm related records and recommendations are removed.
9. Verify an offline release has no Internet permission and makes no network request.
10. Validate upgrade from the previous database/app version without data loss.

### Physical-device and hardware tests

Some USB-C/IEM behaviour cannot be truthfully validated on an emulator. Produce a guided test script for the OnePlus Nord 2 and Lenovo Legion Tab covering:

- connect USB-C IEM/DAC while stopped and while playing;
- identify route/device and apply saved profile;
- play MP3 44.1 kHz, FLAC 44.1/48/96 kHz, and any available 24-bit fixture;
- compare displayed source versus reported output;
- verify Direct Mode disables DSP;
- verify Enhanced IEM Mode applies EQ and limiter;
- disconnect during playback and confirm immediate pause with no speaker leakage;
- reconnect and verify safe resume behaviour;
- test calls, alarms, notifications, screen lock, backgrounding, and media buttons;
- capture `adb logcat`, dumpsys audio/media session information, screenshots, and observed DAC indicators where applicable.

Label every result as one of: `Automated pass`, `Automated fail`, `Manual pass`, `Manual fail`, `Blocked`, or `Not tested`. Never convert “not tested” into “pass.”

### Performance and reliability tests

- Cold start and warm start.
- Scan libraries of approximately 100, 1,000, and 10,000 synthetic metadata entries/files where feasible.
- Scrolling responsiveness and memory usage.
- Playback for an extended session.
- Repeated route connection/disconnection.
- Process death and restoration.
- Low-storage and permission-revocation behaviour.
- Battery-conscious background operation.

Set realistic thresholds after measuring the test environment; document measurements rather than inventing numbers.

## Required validation commands and evidence

Use the repository's platform-appropriate commands. At minimum, run equivalents of:

```text
gradlew clean
gradlew assembleDebug
gradlew testDebugUnitTest
gradlew lintDebug
gradlew connectedDebugAndroidTest
```

Run connected tests only when an emulator/device is available. If unavailable, clearly mark them blocked and provide exact Windows PowerShell commands for me to run. Do not claim an APK works merely because compilation succeeded.

For every phase, report:

- files/features changed;
- commands executed;
- pass/fail counts;
- important warnings;
- APK path, variant, size, version name/code, minimum/target SDK, and SHA-256;
- untested or hardware-dependent behaviour;
- next action.

## Test media

Use only original, public-domain, generated tone/noise, or appropriately licensed fixtures. Include small fixtures for supported formats, silence, short tone, mono/stereo, missing artwork, Unicode metadata, long metadata, malformed metadata, truncated/corrupt file, and duplicate cases. Record each fixture's source/license or generation method. Do not place copyrighted commercial songs in the repository.

## Release and signing safety

- Never commit keystores, passwords, signing secrets, `local.properties`, or machine-specific SDK paths.
- Provide a safe `keystore.properties.example` and Windows PowerShell signing instructions.
- Keep debug and release signing separate.
- Generate a debug APK during development.
- Generate a release APK only when signing inputs are available; otherwise provide the exact reproducible command and clearly label the unsigned output.
- Inspect the final APK manifest, permissions, package ID, version, supported SDKs, native libraries, and signature.

## Definition of done

The work is complete only when:

- the project builds reproducibly;
- unit tests and lint pass, or every remaining failure is documented with evidence;
- available emulator E2E tests pass;
- a debug APK is generated and inspected;
- the app can scan authorized local files and play them in the background;
- media session/notification controls work in available tests;
- USB route handling and safe disconnect logic are implemented;
- physical USB-C assertions are not claimed without device testing;
- automatic playlists and offline recommendations are deterministic, explainable, and erasable;
- no Internet permission or prohibited tracker exists in the offline release;
- phone/tablet UI, dark themes, accessibility, process restoration, and corrupted-file handling are covered;
- documentation and known limitations are complete.

## Required deliverables

1. Complete Android Studio source project.
2. Debug APK and, when signing is available, signed release APK.
3. `README.md` with Windows setup, build, install, test, troubleshooting, and architecture overview.
4. Product requirements and architecture documents.
5. Automated unit, integration, Compose UI, and E2E tests.
6. Physical USB-C/IEM test checklist.
7. Test report containing evidence and honest status.
8. Audio-format compatibility matrix.
9. Permission/privacy report.
10. Dependency and license report.
11. Known-limitations and future-work document.
12. Release notes and APK checksums.

## Final response format

When handing off, lead with the actual outcome. State:

1. what was built;
2. what was tested and exact results;
3. APK and source locations;
4. how I install the APK on Windows/Android;
5. what I must test using my physical USB-C IEM/DAC;
6. remaining limitations or blockers;
7. the single best next step.

Do not report planned functionality as implemented. Do not use vague statements such as “everything works.” Provide evidence for every material claim.
