<div align="center">
  <img src="app/src/main/res/drawable-nodpi/huni_avatar.png" alt="Huni logo" width="180" />
  <h1>Huni</h1>
  <p>A focused, personal music listening experience for Android.</p>
  <a href="https://github.com/rxmdyrems/Huni.app/releases/tag/v0.9.7"><strong>Download Huni v0.9.7</strong></a><br />
  <sub>Pre-release build · universal APK</sub><br />
  <a href="https://github.com/rxmdyrems/Huni.app">Source code</a> · <a href="https://github.com/rxmdyrems/Huni.app/releases">Huni releases</a>
</div>

## About Huni

Huni brings together music discovery, playback, and library tools in one Android app. This repository is the home for Huni's Android source, release notes, and builds.

## Features

- Discover music and manage a personal library on Android.
- Choose from ten color themes and optionally match the launcher icon to the selected color.
- Review a redacted crash-report preview and decide whether to share it; reports are never sent automatically.
- Read release history in the offline in-app Changelog.

## Build

Use the included Gradle wrapper with Android SDK API 37 and JDK 21:

```bash
./gradlew assembleFossDebug
```

The FOSS debug APK is written to `app/build/outputs/apk/foss/debug/`. GMS and Izzy variants are also configured. Never commit local credentials or signing material. Signed releases are produced through the repository's configured GitHub Actions workflow.

## Huni changelog

See [`CHANGELOG.md`](CHANGELOG.md) for Huni-authored release notes. In-app Changelog content is maintained for Huni and links to [Huni releases](https://github.com/rxmdyrems/Huni.app/releases).

## License and upstream attribution

Huni is distributed under the GNU General Public License v3.0 and builds on GPL-licensed open-source components. Original copyright notices, source headers, and required attribution are preserved. See [`LICENSE`](LICENSE). Huni's product identity, release notes, and public repository are maintained here.
