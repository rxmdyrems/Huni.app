# Huni Anime JavaScript Provider Contract

Huni's anime provider repository/runtime is separate from MihonKit reader extensions. Providers are downloaded as UTF-8 JavaScript files from HTTPS repositories and execute in an isolated Rhino scope. The runtime exposes no Java classes, reflection, filesystem, Android APIs, arbitrary native code, or module loader. Provider calls are serialized.

## Manifest

A repository manifest is a JSON object with `name`, optional `description`, and `sources`:

```json
{
  "name": "Example repository",
  "description": "Anime sources",
  "sources": [
    {
      "id": "example-anime",
      "name": "Example Anime",
      "version": "1.0.0",
      "type": "anime",
      "lang": "en",
      "file": "providers/example.js",
      "logo": "images/example.png",
      "nsfw": false,
      "description": "Optional source description"
    }
  ]
}
```

Source IDs are unique within a manifest and, while installed, across all repositories. Repository and script URLs must be HTTPS. Relative paths resolve against the manifest URL. The manifest is capped at 2 MiB/1,000 sources; each script is capped at 1 MiB. Installed state and scripts are stored separately from all Mihon APK extensions.

## Synchronous JavaScript API

A provider may define `getInfo()`, `search(query, page)`, `getEpisodes(seriesUrl)`, and `getVideoSources(episodeUrl)`. `getInfo()` may return `{name, version}` matching the manifest entry. The required methods return arrays synchronously:

- `search`: items with `id`, `title`, `url`, and optional `cover`, `description`, `sourceId`.
- `getEpisodes`: items with `id`, `title`, `number`, `url`.
- `getVideoSources`: items with `url`, `quality`, `container` (`hls` or `mp4`), optional string-valued `headers`.

`Huni.fetch(url)` performs a synchronous HTTP(S) GET and returns response text. Redirects are not followed. `console.log`, `console.warn`, and `console.error` are no-op compatibility helpers. There are no promises, async/await transport, POST/body request API, extractor-specific helpers, or Ketsuren/QuickJS bootstrap objects. Providers written for those additional facilities need adaptation; this interface does not claim full Ketsuren compatibility.

Provider calls are subject to an instruction deadline, per-call request/response limits, bounded result/field counts, URL validation, and an 8-second network connection/read timeout. Providers should perform only the small number of source requests needed for one operation and return compact arrays.
