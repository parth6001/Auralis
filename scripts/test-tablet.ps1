param([string]$Serial = 'emulator-5554')
. "$PSScriptRoot/env.ps1"
$ErrorActionPreference = 'Continue'
$auralisRoot = Split-Path $PSScriptRoot -Parent
if ($Serial -notmatch '^emulator-') { throw 'This display-override check is for a test emulator only.' }
$fontScale = (& adb -s $Serial shell settings get system font_scale).Trim()
try {
    & adb -s $Serial shell wm size 1600x2560
    & adb -s $Serial shell wm density 240
    & adb -s $Serial shell settings put system font_scale 1.3
    & adb -s $Serial shell am instrument -w -r -e class 'com.auralis.player.NavigationTest,com.auralis.player.ProductUiTest' com.auralis.player.test/androidx.test.runner.AndroidJUnitRunner > "$auralisRoot/artifacts/tablet-tests.log" 2>&1
    $raw = Get-Content "$auralisRoot/artifacts/tablet-tests.log" -Raw
    if ($raw -notmatch 'OK \(2 tests\)' -or $raw -match 'FAILURES!!!|Process crashed|INSTRUMENTATION_FAILED') { throw 'Tablet UI tests failed; inspect artifacts/tablet-tests.log.' }
    & adb -s $Serial shell am start -W -n com.auralis.player/.MainActivity
    & adb -s $Serial shell uiautomator dump /sdcard/auralis-layout.xml
    & adb -s $Serial pull /sdcard/auralis-layout.xml "$auralisRoot/artifacts/tablet-layout.xml"
    & adb -s $Serial shell screencap -p /sdcard/auralis-tablet.png
    & adb -s $Serial pull /sdcard/auralis-tablet.png "$auralisRoot/artifacts/tablet.png"
    Write-Output 'Automated pass: two tablet UI tests at 1600x2560, density 240, font scale 1.3.'
} finally {
    & adb -s $Serial shell wm size reset
    & adb -s $Serial shell wm density reset
    & adb -s $Serial shell settings put system font_scale $fontScale
}
