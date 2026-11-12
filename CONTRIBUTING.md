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

## UI conventions

New screens and screen-level toolbar titles must use `HuniScreenTitle` from `ui/component/SectionHeader.kt`. Section headings must use `HuniSectionHeader`; settings groups should use `Material3SettingsGroup`, which supplies the shared section header and separate-card spacing. Use `HuniSwitch` for Compose switches so checked and unchecked states remain consistent across the app. Keep titles to one line where they are screen titles and allow section headers to wrap to two lines.

## Palette launcher icons

The palette icon vectors and adaptive-icon XML are generated with:

```bash
python3 scripts/generate_palette_icons.py
```

To add a palette, add its id, display name, dark background, and dark accent to `PALETTES` in that script, add the matching theme colors in `ui/theme/Theme.kt`, and add a launcher alias in `AndroidManifest.xml`. Register the id-to-alias mapping in `utils/IconUtils.kt`. Keep the Amber compatibility alias enabled by default and all non-default aliases disabled in the manifest. Each generated adaptive icon includes its palette background, safe-zone foreground, and Android 13 monochrome layer; commit both the generator and its generated resources. Do not change the application ID, deep links, or updater for an icon-only change.

## Checks

```bash
./gradlew --console=plain :app:testFossDebugUnitTest :innertube:test
./gradlew --console=plain assembleFossDebug :app:lintFossDebug
```

Contributors do not bump `versionName` or `versionCode`. The maintainer handles versions and releases. Contributors build unsigned or debug APKs locally. Signed releases are made only by the maintainer through GitHub Actions using the configured signing secrets. Never add their values to the repository.

## Branches and commits

Start from `main` and use `fix/`, `feat/`, `ref/`, `docs/`, or `chore/` prefixes. Use short commits in the form `type(scope): description`.

## Reporting crashes and bugs

Huni never uploads crash data automatically. After a crash, the next launch shows a redacted preview and lets you open a prefilled issue, email it, copy it, or dismiss it. Crash reports can be turned off in Settings > Privacy. Report other bugs in [GitHub Issues](https://github.com/rxmdyrems/Huni.app/issues) with steps to reproduce, app version, device, Android version, and relevant logs without personal data.

## Pull requests

Keep changes focused and explain how you tested them. Do not change playback, data, network, or signing behavior unless required. Keep upstream license notices, copyright headers, `NOTICE`, and the About screen's “Built on” credits intact.
