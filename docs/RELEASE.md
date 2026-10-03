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
