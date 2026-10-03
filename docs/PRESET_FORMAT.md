# Local formats

An audio preset is UTF-8 JSON, maximum 16 KB:

```json
{"schema":1,"name":"KA11 + KZ IEM","mode":"ENHANCED_IEM","bands":[-2,0,0,-1,0],"preamp":-3,"balance":0,"crossfeed":0.1,"limiter":true,"replayGain":"TRACK"}
```

`mode`: PURE_DIRECT or ENHANCED_IEM. `bands`: exactly five finite dB values in [-12,12], centered at 60/230/910/3600/14000 Hz (last frequency limited below Nyquist). `preamp`: [-24,0] dB. `balance`: [-1,1]. `crossfeed`: [0,0.3]. `limiter`: boolean. `replayGain`: OFF/TRACK/ALBUM; absent in older schema-1 presets defaults to OFF. `name`: 1–80 characters. Pure Direct ignores all DSP settings without discarding them. Actual applied headroom may attenuate further than the chosen preamp to accommodate positive overlapping bands. Positive ReplayGain is capped by headroom; absent album tags fall back to track, then unity.

App data JSON schema 1 contains `songs` (IDs, fingerprints, favourite/exclusion/mood annotations), `settings`, `playlists` and `history`. It excludes source audio, artwork, lyrics and permission grants. Song annotations match a current ID or an unambiguous current fingerprint; unknown files are ignored. Import merges, validates all input before writing, and is transactional. The in-app confirmation explains that imported settings can replace existing choices. Keep a prior export for manual rollback.
