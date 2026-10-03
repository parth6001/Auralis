# Product requirements and acceptance tracking

BUILD_PROMPT.md is the authoritative complete requirements document and has not been edited or reduced. `IMPLEMENTATION_PLAN.md` records implementation phases, dependencies and risks. `TEST_REPORT.md` will record observed validation; `KNOWN_LIMITATIONS.md` distinguishes remaining implementation from hardware blockers.

Core acceptance: authorized local folder scan, persistent library, background playback and queue controls, safe default disconnect handling, honest source/output diagnostics, isolated device profiles, deterministic erasable local recommendations and mixes, accessible adaptive navigation and consistent themes, no network permission, inspected debug APK and reproducible Windows instructions.

Defaults: application ID `com.auralis.player`, display name in one resource, minimum API 26, stable SDK supported by pinned AGP, no login, no source-file modifications, paused queue restoration, listening history enabled with explicit pause/delete controls, safe disconnect pause enabled, direct-mode request as default, system theme, no acoustic mood inference.

Optional features that cannot be implemented safely with the maintained selected stack must be explicitly listed as unavailable, with no nonfunctional switches or quality claims. Release readiness is never inferred from compilation alone.
