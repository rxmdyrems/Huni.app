# Contributing to Huni

Huni is an open-source Android music app using Kotlin, Jetpack Compose, Material 3, Gradle, and GPL-3.0. The default branch is `main`.

## Set up

Use JDK 21 and Android SDK platform 37. The app targets SDK 36. The only Gradle modules are `app` and `innertube`.

```bash
git clone https://github.com/rxmdyrems/Huni.app
cd Huni.app
./gradlew assembleFossDebug --console=plain
```

The normal local build is the `foss` flavor. `gms` adds Google Cast support and `izzy` disables the updater. The default application ID is `com.remycamiguel.huni`; debug builds add the `.debug` suffix unless `METROLIST_APPLICATION_ID` is set. The debug APK is at `app/build/outputs/apk/foss/debug/app-foss-debug.apk`.

For optional local values, copy `local.properties.sample` to `local.properties`. Never commit it or any credentials. Source strings are in `app/src/main/res/values/metrolist_strings.xml`; do not rename the file.

New screens must use the shared `HuniSectionHeader` and `HuniScreenTitle` composables for section headings and top-app-bar titles so wrapping, sizing, and ellipsis stay consistent. Settings screens should use the shared `Material3SettingsGroup` and `HuniSwitch` components.

Palette launcher assets are generated with `python3 scripts/generate_launcher_icons.py` (requires Pillow) from `scripts/assets/huni_bird_transparent.png`. The outputs use a solid palette-colored background and the original untinted, transparent bird foreground inside the 66% safe zone, plus adaptive icons, Android 13+ monochrome layers, and density-aware legacy fallbacks. The generator checks WCAG contrast against the bird's dominant color and darkens a palette background in 10% steps until it reaches 3:1. When adding a palette, update `HuniPalettes` in `Theme.kt`, the launcher alias and mapping in `AndroidManifest.xml` and `IconUtils.kt`, and the palette background entry in the generator, then regenerate the assets. Keep the stable Amber default alias unchanged, add a separate palette Amber alias if needed, and verify that only one launcher alias is enabled.

## Checks

```bash
./gradlew --console=plain :app:testFossDebugUnitTest :innertube:test
./gradlew --console=plain assembleFossDebug :app:lintFossDebug
```

Contributors do not bump `versionName` or `versionCode`. The maintainer handles versions and releases. Contributors build unsigned or debug APKs locally. Signed releases are made only by the maintainer through GitHub Actions using `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`. Never add their values to the repository.

## Branches and commits

Start from `main` and use `fix/`, `feat/`, `ref/`, `docs/`, or `chore/` prefixes. Use short commits in the form `type(scope): description`.

## Reporting crashes and bugs

Huni never uploads crash data automatically. After a crash, the next launch shows a redacted preview and lets you open a prefilled issue, email it, copy it, or dismiss it. Crash reports can be turned off in Settings > Privacy. Report other bugs in [GitHub Issues](https://github.com/rxmdyrems/Huni.app/issues) with steps to reproduce, app version, device, Android version, and relevant logs without personal data.

## Pull requests

Keep changes focused and explain how you tested them. Do not change playback, data, network, or signing behavior unless required. Keep upstream license notices, copyright headers, `NOTICE`, and the About screen's “Built on” credits intact.
