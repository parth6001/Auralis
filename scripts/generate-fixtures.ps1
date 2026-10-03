param([string]$Destination = "$PSScriptRoot/../app/src/androidTest/assets/media", [string]$Ffmpeg = 'ffmpeg')
$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force $Destination | Out-Null
function Write-Tone([string]$Name, [int]$Rate, [int]$Channels, [bool]$Silent) {
    $stream = [IO.File]::Create((Join-Path $Destination $Name))
    $writer = [IO.BinaryWriter]::new($stream)
    try {
        $frames = $Rate * 2
        $bytes = $frames * $Channels * 2
        $writer.Write([Text.Encoding]::ASCII.GetBytes('RIFF')); $writer.Write([int](36 + $bytes))
        $writer.Write([Text.Encoding]::ASCII.GetBytes('WAVEfmt ')); $writer.Write([int]16)
        $writer.Write([int16]1); $writer.Write([int16]$Channels); $writer.Write([int]$Rate)
        $writer.Write([int]($Rate * $Channels * 2)); $writer.Write([int16]($Channels * 2)); $writer.Write([int16]16)
        $writer.Write([Text.Encoding]::ASCII.GetBytes('data')); $writer.Write([int]$bytes)
        for ($frame = 0; $frame -lt $frames; $frame++) {
            $sample = if ($Silent) { 0 } else { [int16](3000 * [Math]::Sin(2 * [Math]::PI * 440 * $frame / $Rate)) }
            for ($channel = 0; $channel -lt $Channels; $channel++) { $writer.Write([int16]$sample) }
        }
    } finally { $writer.Dispose() }
}
Write-Tone 'tone-stereo-44100.wav' 44100 2 $false
Write-Tone 'tone-mono-48000.wav' 48000 1 $false
Write-Tone 'silence.wav' 44100 1 $true
Copy-Item -LiteralPath "$Destination/tone-stereo-44100.wav" -Destination "$Destination/duplicate.wav" -Force
[IO.File]::WriteAllBytes((Join-Path $Destination 'corrupt.mp3'), [byte[]](0, 255, 1, 2, 3, 0, 128))
$tone = [IO.File]::ReadAllBytes((Join-Path $Destination 'tone-stereo-44100.wav'))
[IO.File]::WriteAllBytes((Join-Path $Destination 'truncated.wav'), $tone[0..35])
if (Get-Command $Ffmpeg -ErrorAction SilentlyContinue) {
    foreach ($extension in @('mp3', 'flac', 'm4a', 'ogg', 'opus')) {
        $codec = switch ($extension) { 'mp3' {'libmp3lame'} 'flac' {'flac'} 'm4a' {'aac'} 'ogg' {'libvorbis'} 'opus' {'libopus'} }
        & $Ffmpeg -y -v error -i "$Destination/tone-stereo-44100.wav" -c:a $codec -metadata 'title=Auralis original tone' -metadata 'artist=Auralis fixtures' "$Destination/tone.$extension"
        if ($LASTEXITCODE -ne 0) { throw "Fixture encoding failed: $extension" }
    }
    & $Ffmpeg -y -v error -i "$Destination/tone-stereo-44100.wav" -c:a alac "$Destination/tone-alac.m4a"
    & $Ffmpeg -y -v error -i "$Destination/tone-stereo-44100.wav" -ar 96000 -sample_fmt s32 "$Destination/tone-96000.flac"
    $unicodeTitle = 'Auralis ' + [char]0x97F3 + [char]0x697D
    $longAlbum = 'Auralis long ' + ('metadata ' * 500)
    & $Ffmpeg -y -v error -i "$Destination/tone-stereo-44100.wav" -c:a libmp3lame -metadata "title=$unicodeTitle" -metadata "album=$longAlbum" "$Destination/tagged.mp3"
    if ($LASTEXITCODE -ne 0) { throw 'Tagged fixture encoding failed' }
} else { Write-Output 'FFmpeg unavailable: generated WAV and malformed fixtures only.' }
Get-ChildItem $Destination -File | Get-FileHash -Algorithm SHA256 | Format-Table -AutoSize
