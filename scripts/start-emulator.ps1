. "$PSScriptRoot/env.ps1"
$auralisRoot = Split-Path $PSScriptRoot -Parent
$env:ANDROID_AVD_HOME = "$auralisRoot/.tools/avd"
$avdDirectory = "$env:ANDROID_AVD_HOME/Auralis_API35.avd"
New-Item -ItemType Directory -Force $avdDirectory | Out-Null
$config = @'
avd.ini.encoding=UTF-8
abi.type=x86_64
disk.dataPartition.size=2G
hw.cpu.arch=x86_64
hw.cpu.ncore=2
hw.gpu.enabled=yes
hw.gpu.mode=swiftshader
hw.lcd.height=1920
hw.lcd.width=1080
hw.lcd.density=420
hw.ramSize=1536
hw.keyboard=yes
hw.audioInput=no
hw.audioOutput=yes
image.sysdir.1=system-images/android-35/default/x86_64/
tag.id=default
tag.display=Default
vm.heapSize=256
PlayStore.enabled=false
'@
[IO.File]::WriteAllText("$avdDirectory/config.ini", $config)
[IO.File]::WriteAllLines("$env:ANDROID_AVD_HOME/Auralis_API35.ini", @('avd.ini.encoding=UTF-8', "path=$avdDirectory", 'target=android-35'))
# Normalize environment key casing for Windows PowerShell's Start-Process dictionary.
$auralisPath = $env:Path
[Environment]::SetEnvironmentVariable('PATH', $null, 'Process')
[Environment]::SetEnvironmentVariable('Path', $auralisPath, 'Process')
Start-Process -FilePath "$env:ANDROID_HOME/emulator/emulator.exe" -ArgumentList @('-avd','Auralis_API35','-no-window','-no-boot-anim','-no-snapshot','-no-audio','-gpu','swiftshader','-no-metrics') -WindowStyle Hidden -RedirectStandardOutput "$auralisRoot/artifacts/emulator.log" -RedirectStandardError "$auralisRoot/artifacts/emulator-error.log"
