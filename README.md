<div align="center">
  <img src="app/src/main/res/drawable-nodpi/huni_avatar.png" alt="Huni logo" width="180" />
  <h1>Huni</h1>
  <p>A focused, personal music listening experience for Android.</p>
  <a href="https://github.com/rxmdyrems/Huni.app/releases/download/v0.9.7/Huni-0.9.7.apk"><strong>Download Huni 0.9.7 APK</strong></a><br />
  <sub>Signed release build · see the release notes for changes</sub><br />
  <a href="https://github.com/rxmdyrems/Huni.app">Source code</a> · <a href="https://github.com/rxmdyrems/Huni.app/releases">Huni releases</a>
</div>

## About Huni

Huni is an open-source Android music player for discovering music, organizing a personal library, and shaping a listening experience around your preferences.

## Features

- Browse music and playlists, with Home shortcuts for frequently used content.
- Manage a personal library, playlists, listening history, downloads, and offline caching.
- Customize themes, playback controls, lyrics, and the Home layout.
- Use Android Auto and optional service integrations.
- Review a redacted crash-report preview and choose whether to share it; crash data is never sent automatically.

## Build

Use the included Gradle wrapper with Android SDK API 37 and JDK 21:

```bash
./gradlew assembleFossDebug
```

The local FOSS debug APK is written to `app/build/outputs/apk/foss/debug/`. GMS and Izzy variants are also configured. Optional local integration values belong in ignored `local.properties`; never commit credentials. Official release APKs are built and signed by GitHub Actions and published on the [Huni releases page](https://github.com/rxmdyrems/Huni.app/releases).

## Huni changelog

See [`CHANGELOG.md`](CHANGELOG.md) for Huni-authored release notes. The in-app Changelog is bundled with the same Huni release history.

## License and upstream attribution

Huni is distributed under the GNU General Public License v3.0 and builds on GPL-licensed open-source components. Original copyright notices, source headers, and required attribution are preserved. See [`LICENSE`](LICENSE). Huni's product identity, release notes, and public repository are maintained here.
