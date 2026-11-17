# Huni reader and Mihon extensions

The Reader supports local CBZ/ZIP archives and individual images through Android's document picker. It can also browse, install, and run Mihon-compatible extension APKs through the `mihon-kit` runtime.

## Extension catalog and repositories

The Extensions screen starts with the Keiyoushi repository:

`https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.json`

Catalog data is parsed from `index.json` using kotlinx.serialization-compatible JSON models. Huni handles both flat extension records and the nested `extensionList.extensions` layout used by Keiyoushi. Absolute APK URLs in the current catalog are used directly; relative APK filenames are resolved using the repository's `/apk/{filename}` path. Repositories can be added, edited, removed, and refreshed from the Browse tab.

Installed APKs are stored in Huni's app-private `filesDir/extensions` directory and loaded at runtime by MihonKit; they are not installed as Android packages. Each APK is third-party executable code. Install only extensions and repositories you trust, and follow source-site terms and applicable laws. Huni does not bundle third-party extensions or endorse their content.

## Browse and read

Open an installed extension to choose one of its sources. Source browsing supports popular manga, keyword search, pagination, manga details, chapter lists, and page loading. Remote source images are requested through Coil with the source's `Referer` header. Chapters opened from a source are added to the existing Reader library and can be revisited there; local CBZ/ZIP and image imports remain available.

Catalog fetch, APK download, incompatible extension, and source request failures are surfaced in the UI with retry or update guidance instead of leaving a blank screen.
