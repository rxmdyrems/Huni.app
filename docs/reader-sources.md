# Huni reader sources

The reader currently supports importing local CBZ/ZIP archives and individual images through Android's document picker. Imported page data is stored in Huni's private app storage and is removed when a book is deleted or app data is reset.

## Online sources and extensions

Online extension repositories and Mihon extensions are **not supported** in this version. Huni does not download or decode the protobuf `index.pb` catalog, install extension APKs, load extension code, or provide remote source discovery/search/chapter reading. The reader screen labels this route as coming soon; do not treat a repository URL as a working source integration.

The requested default repository address for future evaluation is:

`https://github.com/keiyoushi/extensions/raw/repo/index.pb`

No extension catalog is fetched from this URL. Any future integration must independently verify the published schema, package signatures, extension dependencies, SDK/API compatibility, and security implications before enabling code installation or execution. Extension sources can carry copyright and licensing concerns; users remain responsible for complying with source-site terms and applicable law. Huni does not endorse or bundle third-party content.
