param([string]$Serial)
. "$PSScriptRoot/env.ps1"
$ErrorActionPreference = 'Continue'
$auralisRoot = Split-Path $PSScriptRoot -Parent
Push-Location $auralisRoot
try {
    $devices = @(& adb devices | Select-String '^([^\s]+)\s+device$' | ForEach-Object { $_.Matches[0].Groups[1].Value })
    if (-not $Serial) {
        if ($devices.Count -ne 1) { throw 'Connect one test device, or pass -Serial. Instrumentation clears listening history; use a test installation.' }
        $Serial = $devices[0]
    }
    if ($Serial -notin $devices) { throw 'Requested device is not authorized/connected.' }
    $env:ANDROID_SERIAL = $Serial
    & ./gradlew.bat assembleDebug assembleDebugAndroidTest connectedDebugAndroidTest --console=plain 2>&1 | Tee-Object artifacts/connected-tests.log
    $gradleExit = $LASTEXITCODE
    $count = 0
    $failures = 0
    Get-ChildItem app/build/outputs/androidTest-results/connected/debug -Filter '*.xml' -ErrorAction SilentlyContinue | ForEach-Object {
        [xml]$report = Get-Content $_.FullName
        $root = $report.DocumentElement
        $count += [int]$root.GetAttribute('tests')
        $failures += [int]$root.GetAttribute('failures') + [int]$root.GetAttribute('errors')
    }
    if ($count -gt 0) {
        if ($gradleExit -ne 0 -or $failures -gt 0) { throw "Connected regression failed: $failures failures across $count tests." }
        Write-Output "Automated pass: $count connected tests."
        return
    }
    Write-Output 'Gradle produced zero tests; checking explicit instrumentation instead. Zero tests is not a pass.'
    & adb -s $Serial install -r app/build/outputs/apk/debug/app-debug.apk
    if ($LASTEXITCODE -ne 0) { throw 'App installation failed.' }
    & adb -s $Serial install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    if ($LASTEXITCODE -ne 0) { throw 'Test APK installation failed.' }
    & adb -s $Serial shell am instrument -w -r com.auralis.player.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | Tee-Object artifacts/direct-instrumentation-final.log
    $raw = Get-Content artifacts/direct-instrumentation-final.log -Raw
    $pass = [regex]::Matches($raw, '(?m)^INSTRUMENTATION_STATUS_CODE: 0\s*$').Count
    $fail = [regex]::Matches($raw, '(?m)^INSTRUMENTATION_STATUS_CODE: -(1|2)\s*$').Count
    $skip = [regex]::Matches($raw, '(?m)^INSTRUMENTATION_STATUS_CODE: -(3|4)\s*$').Count
    @{ device=$Serial; passed=$pass; failed=$fail; skipped=$skip; total=($pass+$fail+$skip) } | ConvertTo-Json | Set-Content artifacts/direct-test-summary.json
    if ($pass -eq 0 -or $fail -gt 0 -or $raw -notmatch 'OK \(\d+ tests?\)' -or $raw -match 'Process crashed|INSTRUMENTATION_FAILED|FAILURES!!!') {
        throw 'Direct Android regression did not pass. Inspect artifacts/direct-instrumentation-final.log.'
    }
    Write-Output "Automated pass: $pass direct instrumentation tests; $skip skipped."
} finally { Pop-Location }
