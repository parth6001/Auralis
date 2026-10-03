param([string]$Serial = 'emulator-5554')
. "$PSScriptRoot/env.ps1"
$ErrorActionPreference = 'Continue'
$auralisRoot = Split-Path $PSScriptRoot -Parent
foreach ($phase in @('seed', 'verify')) {
    & adb -s $Serial shell am force-stop com.auralis.player
    if ($LASTEXITCODE -ne 0) { throw 'Could not stop test application' }
    $log = "$auralisRoot/artifacts/process-restore-$phase.log"
    & adb -s $Serial shell am instrument -w -r -e class com.auralis.player.ProcessRestoreTest -e restorePhase $phase com.auralis.player.test/androidx.test.runner.AndroidJUnitRunner > $log 2>&1
    $raw = Get-Content $log -Raw
    if ($raw -notmatch 'OK \(1 test\)' -or $raw -match 'FAILURES!!!|Process crashed|INSTRUMENTATION_FAILED') { throw "Process restoration $phase failed. Inspect $log" }
    Write-Output "Automated pass: process restoration $phase."
}
