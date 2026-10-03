. "$PSScriptRoot/env.ps1"
$auralisRoot = Split-Path $PSScriptRoot -Parent
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression
$destination = Join-Path $auralisRoot 'artifacts/auralis-0.1.0-source.zip'
$files = [Collections.Generic.List[string]]::new()
foreach ($name in @('README.md', 'BUILD_PROMPT.md', 'Astra_Offline_Music_Player_Build_Prompt.md', '.gitignore', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat', 'keystore.properties.example', 'app/build.gradle.kts', 'app/proguard-rules.pro')) {
    $path = Join-Path $auralisRoot $name
    if (Test-Path -LiteralPath $path) { $files.Add($path) }
}
foreach ($directory in @('app/src', 'app/schemas', 'gradle', 'scripts', 'docs')) {
    Get-ChildItem -LiteralPath (Join-Path $auralisRoot $directory) -File -Recurse | ForEach-Object { $files.Add($_.FullName) }
}
Get-ChildItem -LiteralPath (Join-Path $auralisRoot 'artifacts') -File | Where-Object { $_.Name -match '^(APK_REPORT\.md|SHA256SUMS\.txt|dependencies.*|.*test-summary\.json|.*final.*\.(log|xml)|regression-tests\.log|device-gate\.log|tablet-tests\.log|process-restore-.*\.log|decoder-evidence\.log|corrupt-media-gate\.log|runtime-fixes-tests\.log|instrumentation-verified\.log|performance\.txt|fixture-checksums\.txt|phone\.png|tablet\.png|debug-.*\.txt|release-.*\.txt)$' } | ForEach-Object { $files.Add($_.FullName) }
$stream = [IO.File]::Open($destination, [IO.FileMode]::Create)
$archive = [IO.Compression.ZipArchive]::new($stream, [IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($file in ($files | Sort-Object -Unique)) {
        $relative = $file.Substring($auralisRoot.Length + 1).Replace('\', '/')
        if ($relative -match '(^|/)(\.tools|\.git|build)(/|$)|local\.properties$|\.(jks|keystore)$') { throw "Excluded file reached archive: $relative" }
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $file, "Auralis/$relative", [IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally { $archive.Dispose(); $stream.Dispose() }
$hash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLower()
"$hash  auralis-0.1.0-source.zip" | Set-Content (Join-Path $auralisRoot 'artifacts/SOURCE_SHA256SUMS.txt')
Get-Item -LiteralPath $destination | Select-Object FullName,Length
