# Huni Changelog
## Unreleased
### Changed
- Rewrote the README with banner, buttons, tech stack and install guide.
- Removed leftover upstream naming from documentation.

## v0.9.9 (2026-11-11)
### Changed
- Fixed the shared switch's doubled off-state outline, leaving one 2dp track on card and surface backgrounds.
- Framed Login with the shared top bar, themed WebView background, loading progress, dark-mode support, and an offline Retry state; added the optional sign-in note to token login.
- Updated palette-matched launcher icons to use contrast-safe palette-colored backgrounds behind the original untinted Huni bird, retaining its monochrome layer.

## v0.9.8 (2026-11-11)
### Added
- Added an optional launcher icon that matches the selected color palette, with adaptive and monochrome variants.

## v0.9.7 (2026-11-10)
### Added
- Added the local crash-report flow with a redacted preview and user-controlled GitHub, email, copy, or dismiss actions.
- Added an offline in-app Changelog screen backed by this file.
- Added a Report a bug link that opens the Huni issue chooser.

### Changed
- Unified section headers and responsive screen titles; settings-row titles now wrap within two lines.
- Improved Home card alignment, subtitles, speed-dial labels, and mini-player progress/fade treatment.
- Refined the About layout, Privacy crash-report controls and switch contrast, Android Auto rows, and Appearance settings.
- Documented the shared screen header and title conventions for new screens.

## v0.9.6 (2026-11-10)
- Fixed a crash caused by switching launcher aliases from the Dynamic icon colors setting.
- Configured themed monochrome icon resources for Android 13 and newer.
- Removed the misleading in-app toggle; themed icons are controlled by the Android launcher.

## v0.9.5 (2026-11-10)
- Replaced the launcher artwork with a transparent glass Huni icon.
- Added adaptive monochrome icon support so Android can apply dynamic theme colors.

## v0.9.4 (2026-11-10)
- Added color themes and appearance modes.
- Refined the About screen and Huni release identity.

## v0.9.3 (2026-11-10)
- Completed the Huni redesign across Home, Search, mini-player, navigation, Account, and Stats.
- Added the Huni header, wordmark, and bundled title font.
- Refined About links, updater behavior, avatar treatment, and color themes.
- Kept playback and service behavior unchanged.

## v0.9.2 (2026-11-10)
- Applied Huni branding, the dark-green palette, and shared rounded Compose tokens.
- Refined the header, filter chips, artwork tiles, settings groups, and bottom navigation.
- Redesigned About with Huni identity, version information, and the displayed developer profile.
- Added the Huni avatar, changelog link, release feed, and update target.
