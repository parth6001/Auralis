param([switch]$Connected)
. "$PSScriptRoot/env.ps1"
$ErrorActionPreference = 'Continue'
$auralisRoot = Split-Path $PSScriptRoot -Parent
Push-Location $auralisRoot
try {
    New-Item -ItemType Directory -Force artifacts | Out-Null
    & ./gradlew.bat clean assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest verifyReleaseOffline --console=plain 2>&1 | Tee-Object artifacts/validation.log
    if ($LASTEXITCODE -ne 0) { throw 'Build/test/lint gate failed. See artifacts/validation.log.' }
    if ($Connected) {
        & ./scripts/run-device-tests.ps1
    } else { Write-Output 'Connected tests NOT RUN. Use -Connected with an authorized adb device.' }
} finally { Pop-Location }
