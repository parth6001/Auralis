. "$PSScriptRoot/env.ps1"
$auralisRoot = Split-Path $PSScriptRoot -Parent
$rows = [Collections.Generic.List[string]]::new()
$rows.Add('# Resolved release dependency licenses')
$rows.Add('')
$rows.Add('License declarations read from the exact resolved Maven POMs in the local build cache. This inventory precedes R8 shrinking; build/test tooling is documented separately in docs/DEPENDENCIES.md. Preserve applicable upstream notices when redistributing.')
$rows.Add('')
$rows.Add('| Component | Declared license | License reference |')
$rows.Add('|---|---|---|')
foreach ($coordinate in Get-Content "$auralisRoot/artifacts/dependencies.txt") {
    $parts = $coordinate.Split(':')
    $names = @()
    $urls = @()
    for ($depth = 0; $depth -lt 8 -and $names.Count -eq 0; $depth++) {
        $directory = "$env:GRADLE_USER_HOME/caches/modules-2/files-2.1/$($parts[0])/$($parts[1])/$($parts[2])"
        $pom = Get-ChildItem -LiteralPath $directory -Filter '*.pom' -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if (-not $pom) { break }
        [xml]$xml = Get-Content -LiteralPath $pom.FullName
        foreach ($license in $xml.SelectNodes("/*[local-name()='project']/*[local-name()='licenses']/*[local-name()='license']")) {
            $names += [string]$license.name
            $urls += [string]$license.url
        }
        $parent = $xml.SelectSingleNode("/*[local-name()='project']/*[local-name()='parent']")
        if (-not $parent) { break }
        $parts = @([string]$parent.groupId, [string]$parent.artifactId, [string]$parent.version)
    }
    $label = if ($names.Count) { $names -join '; ' } else { 'Not declared in cached POM; review upstream parent/notice' }
    $rows.Add("| $coordinate | $label | $($urls -join '; ') |")
}
[IO.File]::WriteAllLines("$auralisRoot/artifacts/dependencies-licenses.md", $rows)
Write-Output "License report: $($rows.Count - 6) resolved components."
