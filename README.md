# Huni for Android

Huni is an Android music client combining Spotify-powered discovery with YouTube Music playback. This repository contains the Android application source.

## Project status

The Android project retains the upstream app's existing playback, library, account, integration, and settings functionality. Huni changes in this repository focus on branding and a dark-green visual system; they do not replace the original service implementations.

## Build

Use the included Gradle wrapper with Android SDK API 37 and JDK 21:

```bash
./gradlew assembleFossDebug
```

The debug APK is written under `app/build/outputs/apk/foss/debug/`. The project also defines GMS and Izzy variants.

## Attribution and license

Huni is derived from Meld and the Metrolist open-source project. Their source notices and GPL-3.0 license are retained. See [`LICENSE`](LICENSE), source-file headers, and the existing lower-case [`changelog.md`](changelog.md) for upstream release history. This repository's Huni-specific release notes are in [`CHANGELOG.md`](CHANGELOG.md).

Upstream project links are retained where required for attribution, dependency provenance, or existing release/update behavior. They are not Huni support or release channels.
