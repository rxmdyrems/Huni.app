# Working with Huni as an AI agent

Huni is an open-source Android music player written in Kotlin with Jetpack Compose and Material 3.

## Project rules

1. Keep changes focused and preserve existing custom features.
2. Use clear commit messages in the form `type(scope): short description`.
3. Keep user-facing strings in the existing English resource conventions and do not edit translated resources unless the task requires it.
4. Avoid database schema changes unless they are necessary and reviewed.
5. Do not bump `versionName` or `versionCode`; releases and release signing are maintainer responsibilities.
6. Do not change playback behavior when working on UI, onboarding, settings, or navigation.
7. Never print or commit credentials, cookies, signing material, or personal profile data.
8. Profile name and avatar are local-only data and must remain excluded from Android backup.
9. Navigation changes must preserve deep links and internal routes, with at least two visible tabs.
10. Before pushing, verify the branch, inspect the last three commit authors and committers, and run static checks appropriate to the change.

## Testing

The authoritative Android compile and unit-test check runs in GitHub Actions. Do not run a local Android build when the task explicitly delegates compilation to Actions. For ordinary contributor work, the standard checks are:

```bash
./gradlew --console=plain :app:testFossDebugUnitTest :innertube:test
./gradlew --console=plain assembleFossDebug :app:lintFossDebug
```

## Branches and releases

Use `fix/`, `feat/`, `ref/`, `docs/`, or `chore/` branches. Feature work belongs on its feature branch and must not create releases, tags, or merges into `main` unless explicitly authorized.

## Privacy and support

Huni does not upload crash data automatically. Keep crash reports redacted and user-controlled. Report bugs with reproduction steps, app version, device, Android version, and logs that contain no personal data.
