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
