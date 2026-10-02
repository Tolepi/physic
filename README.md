# Physic

```
First TW: I dont know what i just did.
```

A local-first Android music player with a TUI soul.

---

## ⚠️ DISCLAIMER ⚠️

**THIS THING IS VIBECODED. I'M SO SORRY EVERYONE, I'LL DO BETTER. 3: (btw the readme is also vibecoded lmfao)**

---

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-2024-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/Media3-1.4-3DDC84?style=for-the-badge&logo=android&logoColor=black)](https://developer.android.com/guide/topics/media/media3)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)
[![Vibecoded](https://img.shields.io/badge/Development-100%25_Vibecoded-ff69b4?style=for-the-badge)](https://en.wikipedia.org/wiki/Vibe_coding)

> screenshot or something
<img width="286" height="640" alt="Screenshot_2026-10-02-15-39-36-11_ced7f87d148198a9a5d73debfbcaaa47" src="https://github.com/user-attachments/assets/66d62519-04a4-4dbc-98ae-5488660fc0d0" />


---

## What is this

Physic is a small but fully-featured music player that tries to look like a
terminal in the style of [system24](https://refact0r.github.io/system24/) (even though it doesnt look like it) while actually being a ~~competent~~ STUPIDLY HORRIBLE local music player.

Think Namida + Retro Music + Spotify-home, wearing a trench coat made of ASCII.

## Features

### Playback
- Full Media3/ExoPlayer pipeline with lockscreen + notification controls
- Shuffle & repeat (off / all / one)
- Seekbar, prev / play-pause / next
- Home-screen widget with play/pause + next
- `.lrc` sidecar lyrics with current-line highlighting

### Library
- MediaStore indexer with folder selection (`~/settings` → index folders)
- Browse by songs, albums, or artists
- Albums & artists show total track time and release year
- Album → tap → per-song list · Artist → tap → albums → songs
- Album art on every screen, custom artist images

### Home (`~/home`)
- Editable profile (name + picture)
- Editable welcome message
- Most played / recently added / albums row, Spotify-style
- Shuffle all

### Stats (`~/stats`)
- Total plays, most played songs, top albums, top artists
- Tap a card to see the full ranked list
- JSON stats import (Namida-style) so you never lose your history

### Audio
- Real system equalizer with per-band sliders (`~/eq`)
- Bundled fonts: **DM Mono** (default), **JetBrains Mono**, **Fira Code**,
  **Cascadia Code**, **Space Mono**
- Built-in `.ttf` font downloader + selector

### Theming
- **System24** (default, TUI-style)
- **AMOLED** (true black, separate option)
- **Catppuccin Mocha**, **Everforest**, **Tokyo Night**, **Gruvbox**, **Nord**,
  **Rose Pine**, **Dracula**, **Solarized Dark**, **One Dark**, **Sakura**
- Custom hex-color theme maker
- Square corners by default, rounded-corner option

## Install

From the [Releases](../../releases) tab, grab the latest `app-release.apk`
and sideload it. Or build from source:

```bash
git clone https://github.com/Tolepi/physic.git
cd physic
./gradlew assembleRelease
```

Requires Android 8.0+ (minSdk 26).

## Building from source

- JDK 17 (Temurin)
- Android SDK 34, build-tools 34.0.0
- Gradle (wrapper included)

```bash
doas emerge dev-java/openjdk-bin:17   # Gentoo
sdkmanager "platforms;android-34" "build-tools;34.0.0"
./gradlew assembleDebug
```

## License

MIT — see [LICENSE](LICENSE).

---

*made with ❤️, terminal aesthetics, and questionable life choices*
