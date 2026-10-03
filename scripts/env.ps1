$ErrorActionPreference = 'Stop'
$auralisRoot = Split-Path $PSScriptRoot -Parent
if (-not $env:JAVA_HOME) {
    $auralisJdk = Get-ChildItem "$auralisRoot/.tools/jdk" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($auralisJdk) { $env:JAVA_HOME = $auralisJdk.FullName }
}
if (-not $env:ANDROID_HOME -and (Test-Path "$auralisRoot/.tools/android-sdk")) { $env:ANDROID_HOME = "$auralisRoot/.tools/android-sdk" }
$env:GRADLE_USER_HOME = "$auralisRoot/.tools/gradle-user"
$env:ANDROID_USER_HOME = "$auralisRoot/.tools/android-user"
if ($env:JAVA_HOME) { $env:Path = "$env:JAVA_HOME/bin;$env:Path" }
if ($env:ANDROID_HOME) { $env:Path = "$env:ANDROID_HOME/platform-tools;$env:Path" }
