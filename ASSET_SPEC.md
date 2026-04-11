# StellarStep Asset Specification

This document describes every asset used by the default skin and where to place replacement images.

All skin assets live under: `assets/skins/<skinName>/`  
Default skin: `assets/skins/default/`

---

## Skin Assets

| Key | Filename | Usage | Required Dimensions | Format | Notes |
|-----|----------|-------|---------------------|--------|-------|
| `bg` | `bg.png` | Full-screen gameplay background | **1280 × 720 px** | PNG (RGB or RGBA) | Scaled to fill the screen; can be any aspect ratio but 16:9 recommended |
| `lane` | `lane.png` | Lane column background (repeated per lane) | **80 × 600 px** | PNG (RGBA) | Stretched vertically to fill lane height; transparency supported |
| `note` | `note.png` | Tap note (falling block) | **76 × 20 px** | PNG (RGBA) | Rendered per note at its Y position; width = lane width − 4 px gap |
| `hold_body` | `hold_body.png` | Hold note body (connecting bar) | **76 × 1 px** | PNG (RGBA) | Stretched vertically to match hold duration; should tile cleanly |
| `hold_end` | `hold_end.png` | Hold note head/tail marker | **76 × 20 px** | PNG (RGBA) | Same width as `note.png`; rendered at the top of the hold body |
| `key_on` | `key_on.png` | Key pressed indicator (bottom of lane) | **80 × 120 px** | PNG (RGBA) | Shown while the lane key is held down |
| `key_off` | `key_off.png` | Key released indicator (bottom of lane) | **80 × 120 px** | PNG (RGBA) | Shown when the lane key is not pressed |
| `hit_effect` | `hit_effect.png` | Flash effect at the judgment line on hit | **80 × 80 px** | PNG (RGBA) | Centered on the lane, fades out over ~150 ms |
| `judgment_line` | `judgment_line.png` | Horizontal line marking the hit zone | **320 × 4 px** | PNG (RGBA) | Spans all 4 lanes (4 × 80 px); rendered at Y = 120 px from bottom |

---

## Icon

| Filename | Dimensions | Format | Notes |
|----------|-----------|--------|-------|
| `assets/imgs/icon.png` | **16 × 16 px** (or 32 × 32) | PNG | Window/taskbar icon |

---

## Audio

| Path | Format | Notes |
|------|--------|-------|
| `assets/sound/hihat.mp3` | MP3, 44100 Hz | Used as the metronome tick on the calibration screen |
| `assets/songs/<songDir>/audio.ogg` or `audio.wav` | OGG Vorbis or WAV 16-bit PCM, 44100 Hz stereo | Song audio; referenced by `audioFile` field in `chart.json` |

---

## Chart Format (`chart.json`)

Each song directory under `assets/songs/` should contain a `chart.json`:

```json
{
  "title": "Song Title",
  "artist": "Artist Name",
  "bpm": 120,
  "audioFile": "audio.ogg",
  "offsetMs": 0,
  "notes": [
    { "time": 1000, "lane": 0, "type": "tap" },
    { "time": 1500, "lane": 2, "type": "hold", "duration": 500 }
  ]
}
```

### Note fields
| Field | Type | Description |
|-------|------|-------------|
| `time` | integer (ms) | Hit time from audio start in milliseconds |
| `lane` | integer 0–3 | Lane index (0 = leftmost) |
| `type` | string | `"tap"` or `"hold"` |
| `duration` | integer (ms) | Hold duration (only for `"hold"` notes); omit for tap |

---

## Scaling Notes

- The game window is fixed at **1280 × 720** (no resizing).
- Lane area: **320 × 600 px** centered horizontally (x: 480–800).
- Assets are stretched/tiled by libGDX `SpriteBatch.draw()` with no filtering by default; for crisp pixel art, nearest-neighbor filtering is recommended.
- Hold body (`hold_body.png`) should be a 1 px tall strip that tiles cleanly vertically.
