<p align="center"><img src="assets/readme/banner.png" alt="Huni" width="100%"></p>

<p align="center">Free and open-source (GPL v3) music player for Android, playing music from YouTube and your local files.</p>

<p align="center">
  <a href="https://github.com/rxmdyrems/Huni.app/releases/latest"><img src="assets/readme/btn-download.svg" alt="Download the latest Huni APK" width="200"></a>
  <a href="https://github.com/rxmdyrems/Huni.app"><img src="assets/readme/btn-source.svg" alt="View Huni source code" width="200"></a>
  <a href="CHANGELOG.md"><img src="assets/readme/btn-changelog.svg" alt="Read the Huni changelog" width="200"></a>
  <a href="https://github.com/rxmdyrems/Huni.app/issues/new/choose"><img src="assets/readme/btn-bug.svg" alt="Report a Huni bug" width="200"></a>
</p>

<p align="center">
  <img src="https://img.shields.io/github/v/release/rxmdyrems/Huni.app?style=flat-square&labelColor=2C2437&color=FFB84D&label=latest%20release" alt="Latest release">
  <img src="https://img.shields.io/badge/license-GPL%20v3-FFB84D?style=flat-square&labelColor=2C2437" alt="GPL v3 license">
  <img src="https://img.shields.io/badge/Android-8.0%2B-FFB84D?style=flat-square&labelColor=2C2437" alt="Android 8.0 or newer">
  <img src="https://img.shields.io/badge/Kotlin-FFB84D?style=flat-square&labelColor=2C2437" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-FFB84D?style=flat-square&labelColor=2C2437" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Material%203-FFB84D?style=flat-square&labelColor=2C2437" alt="Material 3">
  <img src="https://img.shields.io/badge/FOSS-FFB84D?style=flat-square&labelColor=2C2437" alt="FOSS">
</p>

## Why Huni

Huni is a personal, open-source music player with a calm dark-first interface, flexible colors, and local playback that does not require an account.

- **Personal:** shape the library, Home layout, playback, lyrics, and appearance to fit your listening habits.
- **Open source:** inspect, build, and improve the GPL v3 app.
- **Themeable:** choose from ten color palettes plus light, dark, or system appearance modes; optionally match the launcher icon.
- **No account required to start:** optional integrations are off until you enable them.

## Features

- **Playback and library:** YouTube playback, local files, playlists, downloads, offline caching, history, queue controls, sleep timer, and Media3 playback.
- **Discovery:** search, recommendations, Home content, and optional Spotify integration for discovery and matching.
- **Personalization:** ten color palettes, light/dark/system modes, Home customization, lyrics preferences, and an optional matching launcher icon.
- **Android Auto:** browse and control supported library and playback flows from Android Auto.
- **Lyrics:** multiple lyrics providers and configurable lyrics display options.
- **Stats and backup:** listening statistics, database backup/restore, and storage tools.
- **Updater:** FOSS and GMS builds can check GitHub Releases; the updater asks before installing and verifies the APK SHA-256.
- **Privacy tools:** an in-app Privacy screen, local redacted crash reports, and user-controlled GitHub issue or email-draft sharing.

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Kotlin 2.4.10 |
| UI | Jetpack Compose 1.12.0, Material 3 1.5.0-alpha27 |
| Playback | AndroidX Media3 1.10.1 |
| Networking | Ktor 3.5.2; OkHttp-backed clients |
| Images | Coil 3.6.0 |
| Database | Room 2.8.4 |
| Dependency injection | Hilt 2.60.1 |
| Build | AGP 9.3.1, Gradle wrapper, JDK 21, compile SDK 37, target SDK 36, min SDK 26 |
| CI and releases | GitHub Actions; signed release APKs with SHA-256 checksums |
| Modules | `app`, `innertube` |
| Flavors | `foss`, `gms`, `izzy` |

## Install and update

1. Download an APK from [GitHub Releases](https://github.com/rxmdyrems/Huni.app/releases/latest).
2. If Android asks, allow installs from the browser or file manager you used.
3. Open the APK and follow Android's install prompt.

To check a downloaded checksum locally:

```bash
sha256sum Huni-*.apk
```

The in-app updater checks GitHub Releases at most every 24 hours, asks before installing, and verifies the downloaded APK's SHA-256. Release APKs are signed with one permanent key. If you are on 0.9.2 or older, uninstall once and reinstall: the signing key changed in 0.9.3. Newer versions update in place.

## Privacy and data

Huni needs no account to play YouTube or local music. Optional integrations such as a YouTube account, Last.fm scrobbling, Spotify, and Discord remain off until you enable them. Features that use third-party services contact those services when used. Crash reports are prepared locally, redacted, and never sent automatically; after a crash, Huni asks whether to open a GitHub issue or an email draft. Review the app's [Privacy settings implementation](app/src/main/kotlin/com/metrolist/music/ui/screens/settings/PrivacySettings.kt) for the current controls.

## Build from source

Prerequisites: JDK 21 and the Android SDK platform/build tools for compile SDK 37.

```bash
git clone https://github.com/rxmdyrems/Huni.app
cd Huni.app
./gradlew assembleFossDebug --console=plain
```

The debug APK is written under `app/build/outputs/apk/foss/debug/`. Available flavors are:

| Flavor | Notes |
| --- | --- |
| `foss` | Updater enabled; Google Cast disabled. |
| `gms` | Updater and Google Cast enabled. |
| `izzy` | No updater or Google Cast; the F-Droid-compliant build. |

See [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

## Contributing and support

Use the [issue chooser](https://github.com/rxmdyrems/Huni.app/issues/new/choose) for bugs and feature requests, or read [CONTRIBUTING.md](CONTRIBUTING.md) for project guidance. The crash-report flow asks first, shows a redacted preview, and lets you open a GitHub issue or an email draft.

## Built on

The About screen credits these projects and preserves their licenses:

- **Metrolist / InnerTune** — GPL v3
- **AndroidX Media3** — Apache 2.0
- **Ktor, Coil, and Material 3** — Apache 2.0

## License

Huni is licensed under the [GNU General Public License v3.0](LICENSE). Huni is an independent personal project, not affiliated with or endorsed by YouTube, Google, or any other named service. Use of those services is subject to their terms. For personal use.
