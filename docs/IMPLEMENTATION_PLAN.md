# Auralis implementation plan

The complete product specification is BUILD_PROMPT.md. Both supplied prompts were read in full before editing. This is a new workspace containing no existing source, build, tests, Git repository, or installed Android toolchain discovered on PATH or in standard installation directories.

## Phases and acceptance gates

| Phase | Work | Depends on | Acceptance |
|---|---|---|---|
| 0 | Inventory, provision isolated tools, verify official versions | Network access | Reproducible wrapper, tool/version evidence |
| 1 | Compose theme, Room, SAF scanner, Media3 service and controller, queue restoration | 0 | Debug APK, domain tests, lint; device smoke when available |
| 2 | Device callback, removal pause, modes, EQ/profile and diagnostic state | 1 | Safety calculation and mode tests; hardware assertions explicitly pending |
| 3 | Local listening events, explainable ranking, deterministic mixes, privacy controls | Core playback/persistence gate | Scoring/membership/deletion tests |
| 4 | Adaptive UI, themes, queue/playlists, imports, accessibility and hardening | 1–3 | Instrumentation suite compiled and run where device available |
| 5 | Regression, APK inspection, privacy gate, reports and signing instructions | All | Artifact checksum, exact validation outcomes and limitation register |

Manual application-container injection avoids Hilt/compiler overhead for a single application module. Packages separate domain, data, playback, audio and UI. Room is the persistence authority; the service owns the only player. SAF read grants avoid broad media/storage permissions. No WorkManager is needed for explicit cancellable scans. Source files are never modified.

## Risk register / reversible defaults

* No installed SDK/JDK/Gradle or connected device discovered initially: install tools under ignored `.tools`; do not change global environment. Verify downloadable stable artifacts against official sources.
* Android's system mixer cannot establish exclusive USB or bit-perfect delivery. Pure Direct disables application DSP; actual path remains labelled System mixed unless measured otherwise. Do not add an unreviewed USB driver.
* Device identity may be ambiguous: use route type and available address/product identity, disclose ambiguity. Passive IEM identity is not inferable.
* Device removal callbacks are best effort; physical speaker-leakage timing needs the two target devices.
* Platform codecs differ by API/OEM. Compatibility matrix distinguishes implementation from actual decoding tests. Optional unsupported codecs/DSP stay disclosed, without decorative controls.
* Provider failure/cancelled scans must not delete library entries. Reconcile only a complete root snapshot in one transaction; preserve identity across moves only when a fingerprint match is unique.
* Import/export is local and size/schema bounded; URI grants are never manufactured from imported data. No signing keys supplied, so debug signing and unsigned release only.
* Production readiness requires runtime and hardware evidence, not compilation. Every remaining criterion will be recorded honestly.
