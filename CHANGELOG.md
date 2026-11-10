# Huni Changelog
## 0.9.4 — Color themes and appearance modes
- Color themes, light/dark/system mode, About name fix.
## 0.9.3 — Amber night redesign
- Replaced the green theme with a fixed dark plum palette and amber accent.
- Completed the Home, Search, mini-player, navigation, and Account/Stats UI refresh.
- Used the repository-bundled BBH Bartle font for titles and the Huni wordmark.
- Refined Huni release identity and About links for version 0.9.3.
- Kept existing playback and service behavior unchanged.


## 0.9.2 — Huni branding and visual polish

- Applied the Huni dark-green palette and shared rounded shape tokens to the Compose theme.
- Refined filter chips, Speed Dial artwork tiles, settings groups, and the bottom navigation surface.
- Redesigned the About screen with Huni identity, app version information, and Remy Camiguel as the displayed developer.
- Used the supplied Huni graphic in the About profile/avatar slot and in the README.
- Replaced About-page upstream/Discord links with the Huni.app repository and license file.
- Made the in-app changelog Huni-authored and linked its release button to Huni.app.
- Repointed the update feed to Huni.app and redesigned the home toolbar around the Huni logo.
- Preserved existing playback, library, navigation, account, settings, and service behavior.

This release retains upstream source notices and GPL-3.0 licensing. The in-app changelog is Huni-authored, and update checks now target the public Huni.app repository. No Huni release has been published yet, so update downloads will become available only after a compatible Huni release and APK assets are published. The pre-existing private `Huni` repository is a separate project. Credentials are kept out of the public source.


### Distribution note

The initial downloadable v0.9.2 build is a debug-signed pre-release built without the Huni-local service credential. Third-party dependencies may include their own client identifiers. Automated unit tests pass, but real streaming has not been tested on an Android device; PO-token-dependent playback may fail.
