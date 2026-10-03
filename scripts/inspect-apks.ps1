. "$PSScriptRoot/env.ps1"
$auralisRoot = Split-Path $PSScriptRoot -Parent
$toolsDirectory = "$env:ANDROID_HOME/build-tools/36.0.0"
$ErrorActionPreference = 'Continue'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$report = [Collections.Generic.List[string]]::new()
$report.Add('# APK inspection')
$report.Add('')
$checksums = [Collections.Generic.List[string]]::new()
foreach ($variant in @('debug', 'release')) {
    $name = if ($variant -eq 'debug') { 'app-debug.apk' } else { 'app-release-unsigned.apk' }
    $apkPath = "$auralisRoot/app/build/outputs/apk/$variant/$name"
    if (-not (Test-Path -LiteralPath $apkPath)) { throw "Missing $variant APK" }
    $outputName = if ($variant -eq 'debug') { 'auralis-0.1.0-debug.apk' } else { 'auralis-0.1.0-release-unsigned.apk' }
    Copy-Item -LiteralPath $apkPath -Destination "$auralisRoot/artifacts/$outputName" -Force
    $badging = & "$toolsDirectory/aapt2.exe" dump badging $apkPath
    if ($LASTEXITCODE -ne 0) { throw 'APK badging failed' }
    $badging | Set-Content "$auralisRoot/artifacts/$variant-badging.txt"
    if (($badging -join "`n") -match 'android.permission.INTERNET|android.permission.ACCESS_NETWORK_STATE') { throw 'Offline APK permission regression' }
    & "$toolsDirectory/aapt2.exe" dump xmltree $apkPath --file AndroidManifest.xml > "$auralisRoot/artifacts/$variant-manifest-tree.txt"
    if ($LASTEXITCODE -ne 0) { throw 'APK manifest inspection failed' }
    & "$toolsDirectory/zipalign.exe" -c -P 16 -v 4 $apkPath > "$auralisRoot/artifacts/$variant-alignment.txt" 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'APK alignment verification failed' }
    & "$toolsDirectory/apksigner.bat" verify --verbose --print-certs $apkPath > "$auralisRoot/artifacts/$variant-signature.txt" 2>&1
    $signatureExit = $LASTEXITCODE
    if ($variant -eq 'debug' -and $signatureExit -ne 0) { throw 'Debug signature verification failed' }
    if ($variant -eq 'release' -and $signatureExit -eq 0) { throw 'Unexpected release signing; review signing configuration' }
    $hash = (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash.ToLower()
    $size = (Get-Item -LiteralPath $apkPath).Length
    $checksums.Add("$hash  $outputName")
    $report.Add("## $variant")
    $report.Add('')
    $report.Add("File: $outputName; bytes: $size; SHA-256: $hash")
    $signatureText = if ($variant -eq 'debug') { 'Signature: Android Debug; apksigner verification passed.' } else { 'Signature: UNSIGNED; apksigner rejection is expected. No release key was supplied.' }
    $report.Add($signatureText)
    $report.Add('')
    $badging | Where-Object { $_ -match '^(package:|minSdkVersion|targetSdkVersion|uses-permission:|native-code:)' } | ForEach-Object { $report.Add($_) }
    $zip = [IO.Compression.ZipFile]::OpenRead($apkPath)
    try { $report.Add('Native entries:'); $zip.Entries | Where-Object FullName -like 'lib/*' | ForEach-Object { $report.Add($_.FullName) } } finally { $zip.Dispose() }
    $report.Add('')
}
[IO.File]::WriteAllLines("$auralisRoot/artifacts/APK_REPORT.md", $report)
[IO.File]::WriteAllLines("$auralisRoot/artifacts/SHA256SUMS.txt", $checksums)
