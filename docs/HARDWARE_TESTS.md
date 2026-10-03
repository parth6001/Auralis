# Physical USB-C/IEM validation

Run separately on OnePlus Nord 2 / Android 13 and Lenovo Legion Tab TB320FC / Android 15. Record build SHA-256, device/OS build, DAC firmware/name, IEM, source fixture, system volume, date and tester. Initial result for every row below is **Not tested**. Use exactly Automated pass, Automated fail, Manual pass, Manual fail, Blocked, or Not tested.

Keep system volume low. Use generated fixtures; do not use protected streaming material. Obtain a second observer or loopback measurement where safe speaker-leakage timing is important. An absent audible sound alone does not prove zero leaked samples.

| Action | Required observation |
|---|---|
| Connect USB while stopped | Device list updates; saved profile identified or ambiguity disclosed; no autoplay |
| Connect USB while playing | Route follows Android; profile update does not crash; log audible discontinuities |
| Rename profile to “KA11 + KZ IEM”, disconnect/reconnect | Correct preset restored without autoplay |
| Play MP3 44.1 kHz and FLAC 44.1/48/96 kHz; include a native 24-bit fixture | Source fields match fixture; device capability list is not presented as negotiated format |
| Select Pure Direct with nonzero saved EQ/balance/crossfeed | App DSP bypassed; system mixing disclosed; do not infer bit-perfect from a DAC LED |
| Select Enhanced with a mild negative EQ curve | Audible/measurable filter response; attenuation and balance function; PCM support reported |
| Enable limiter and feed safe synthetic peaks | Peaks limited at processor output; compare measured response; this is hard peak limiting |
| Remove USB while playing | Immediate default pause, no unexpected speaker continuation; record actual leakage/timing |
| Reconnect after removal | Remains paused until explicit play |
| Repeat connect/remove 20 times | No crash, leaked service, unintended resume or wrong profile |
| Switch between USB, wired analog, Bluetooth and speaker | Separate settings restored; simultaneous-output ambiguity checked |
| Receive/end call; alarm; transient notification | Appropriate focus loss/duck/pause/recovery; call privacy checked |
| Lock screen, background 30 min, media-button play/pause/next | Playback/session/notification controls stay synchronized |
| Force-stop and reopen; revoke folder permission | Paused queue recovery, useful unavailable-file state; no hidden scanning |
| Change Light/Dark/AMOLED/System; enlarge font; TalkBack | Player, dialogs, bars, menus and controls readable on both devices |

PowerShell evidence commands (after `. .\scripts\env.ps1`):

```powershell
adb devices -l
adb shell getprop ro.build.fingerprint
adb shell dumpsys audio > artifacts\device-audio.txt
adb shell dumpsys media_session > artifacts\device-media-session.txt
adb shell dumpsys media.audio_flinger > artifacts\device-audio-flinger.txt
adb logcat -d -v threadtime > artifacts\device-logcat.txt
adb shell screencap -p /sdcard/auralis-screen.png
adb pull /sdcard/auralis-screen.png artifacts\device-screen.png
```

Device dumps may include private data from other applications. Keep these captures local and review before sharing. Copy Auralis's diagnostic report from Equalizer and append fixture parameters, DAC indicators, exact observed result, and any mismatch. System PCM/direct/exclusive assertions require technical route evidence; the app does not advertise them as verified.
