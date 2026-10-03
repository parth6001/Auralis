# Architecture

Auralis is a native Kotlin/Compose application with one Gradle application module and separate packages for UI, data, domain, playback, and audio. `AuralisApp` provides a small application-owned dependency container. There is no static mutable singleton and no account/network backend.

`LibraryViewModel` exposes Room Flows with lifecycle-aware Compose collection. `PlaybackConnection` owns a MediaController connection, never an ExoPlayer. `PlaybackService` owns the sole ExoPlayer and MediaSession, audio focus, noisy handling, foreground notification, and queue checkpoints. Queue restoration is paused and uses retained song IDs. Checkpoints occur at state changes and every five seconds; abrupt termination can lose up to five seconds of position.

`LibraryScanner` performs read-only SAF traversal on Dispatchers.IO. Each root is staged and reconciled in a Room transaction only after successful enumeration. Failed/cancelled roots retain their previous records. Stable document URIs preserve IDs. A unique missing file and unique new file with the same size plus bounded SHA-256 prefix can be reconciled as a probable move. This is a heuristic, not full-content equality; duplicate copies are not deleted or merged. Metadata errors fall back to filenames and unknown fields. No source writes or broad storage permission are used. MediaStore is deliberately unnecessary for folder-scoped access; no all-device MediaStore index is maintained.

`AudioRouting` observes supported AudioManager device callbacks. Connection capabilities are not the actual routed AudioTrack or negotiated PCM format. Automatic profiles choose USB, wired, Bluetooth, then speaker; users are warned that simultaneous outputs can be ambiguous. Product/address identity may change on OEMs. A passive IEM cannot be identified behind a DAC.

`IemProcessor` is a Media3 streaming PCM16 processor. Pure Direct copies PCM without application DSP. Enhanced IEM applies five fixed-frequency peaking filters, conservative attenuation, balance, crossfeed, and a hard peak limiter. It does not claim transparent limiting, exclusive USB, native sample-rate negotiation, or verified bit-perfect delivery. No external decoder/USB binary is bundled. Default disconnect handling pauses the service player and uses Media3 noisy handling. Callback timing and speaker-leakage assertions require hardware evidence.

No WorkManager or continuous background scanner is used. Scans are explicit, cancellable work tied to the ViewModel lifetime. Playback uses the service lifecycle and wake lock only as managed by ExoPlayer. Release logging strips informational logs and application errors do not include filenames, URIs, or listening metadata.

Room schema 2 adds the optional manual music mood label through a non-destructive 1→2 migration. Destructive fallback is never enabled. Instrumentation validates the current schema and migration.
