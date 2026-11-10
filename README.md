<div align="center">
  <img src="assets/huni-glass-logo.png" alt="Huni glass logo" width="220" />
  <h1>Huni</h1>
  <p><strong>Your music. Your colors. Your listening space.</strong></p>
  <p>A focused, personal music experience for Android—built for discovery, playback, and an uncluttered library.</p>

  <p>
    <a href="https://github.com/rxmdyrems/Huni.app/releases/download/v0.9.8/Huni-0.9.8.apk"><strong>⬇ Download the latest Huni APK</strong></a>
    &nbsp;·&nbsp;
    <a href="https://github.com/rxmdyrems/Huni.app/releases">Release notes</a>
    &nbsp;·&nbsp;
    <a href="https://github.com/rxmdyrems/Huni.app/issues">Report a bug</a>
  </p>

  <p>
    <a href="https://github.com/rxmdyrems/Huni.app/releases"><img src="https://img.shields.io/github/v/release/rxmdyrems/Huni.app?display_name=tag&sort=semver&style=flat-square&color=157630" alt="Latest release" /></a>
    <a href="https://github.com/rxmdyrems/Huni.app/actions/workflows/build-apk.yml"><img src="https://img.shields.io/github/actions/workflow/status/rxmdyrems/Huni.app/build-apk.yml?style=flat-square&label=build" alt="Build status" /></a>
    <a href="LICENSE"><img src="https://img.shields.io/github/license/rxmdyrems/Huni.app?style=flat-square" alt="GPL-3.0 license" /></a>
  </p>
</div>

## Why Huni?

Huni keeps the focus on the music. Browse discoveries, search quickly, play without friction, and organize your library in a dark-green interface with rounded, calm surfaces and personal color themes.

- **Discover and play** — Move from search and recommendations to playback with a focused, responsive UI.
- **Your library, your way** — Manage playlists, listening history, downloads, and offline caching.
- **Make it yours** — Customize themes, playback controls, lyrics, and the Home layout.
- **Match your palette** — Optionally match the launcher icon to the selected color palette.
- **A better glance** — Enjoy a polished header, mini-player, artwork surfaces, stats, and Android Auto support.
- **Private by default** — Crash reports stay local until you explicitly choose to copy, email, or share them.

## Download

Get the current signed FOSS Universal APK:

<a href="https://github.com/rxmdyrems/Huni.app/releases/download/v0.9.8/Huni-0.9.8.apk"><img src="https://img.shields.io/badge/Download-current%20Huni%20APK-157630?style=for-the-badge&logo=android&logoColor=white" alt="Download current Huni APK" /></a>

Visit the [Huni releases page](https://github.com/rxmdyrems/Huni.app/releases) for version history, release notes, and SHA-256 checksums. On Android, download the APK, open it, and allow installation from your browser or file manager if Android asks.

> Huni is distributed as a signed FOSS build. Streaming behavior can vary by service, device, Android version, and required playback tokens. Review the release notes before installing an update.

## Build from source

Use JDK 21, Android SDK platform 37, and the included Gradle wrapper:

```bash
git clone https://github.com/rxmdyrems/Huni.app
cd Huni.app
./gradlew assembleFossDebug --console=plain
```

The local FOSS debug APK is written to `app/build/outputs/apk/foss/debug/`. The repository also contains `gms` and `izzy` variants. Optional local integration values belong in ignored `local.properties`; never commit credentials. Official release APKs are built and signed by GitHub Actions.

## What’s new

See [`CHANGELOG.md`](CHANGELOG.md) for Huni-authored release notes. The app’s offline Changelog screen is backed by the same bundled content and links to [all Huni releases](https://github.com/rxmdyrems/Huni.app/releases).

## Contributing

Bug reports, focused improvements, and thoughtful pull requests are welcome. Please read [`CONTRIBUTING.md`](CONTRIBUTING.md) first. When reporting a bug, include the Huni version, Android version, device, steps to reproduce, and logs with personal data removed.

## License and attribution

Huni is distributed under the [GNU General Public License v3.0](LICENSE) and builds on GPL-licensed open-source components. Original copyright notices, source headers, and required upstream attribution are preserved in the repository. Huni’s product identity, release notes, and public builds are maintained here.
