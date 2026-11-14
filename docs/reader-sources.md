# Huni reader sources

The reader supports local CBZ/ZIP archives and individual images through Android's document picker. It also supports **user-supplied JSON extension definitions** that make ordinary HTTP(S) GET requests and parse static HTML with CSS selectors. Huni does not execute JavaScript, install APKs, load JVM extension code, or claim Mihon compatibility.

## JSON extension configuration

Paste a single config below in Reader → Extensions → Add extension, or enter a URL that serves one config, a JSON array, or an object with an `extensions` array. Index endpoints are JSON only.

```json
{
  "id": "sample",
  "name": "Sample Source",
  "baseUrl": "https://example.org",
  "lang": "en",
  "popular": { "url": "/manga", "selectors": { "title": ".manga-title", "cover": ".manga-cover img", "link": ".manga-title" } },
  "search": { "url": "/search?q={{query}}", "selectors": { "title": ".manga-title", "cover": ".manga-cover img", "link": ".manga-title" } },
  "details": { "url": "{{mangaUrl}}", "selectors": { "title": "h1", "author": ".author", "cover": ".cover img" } },
  "chapters": { "url": "{{mangaUrl}}", "selectors": { "title": ".chapter a", "link": ".chapter a" } },
  "pages": { "url": "{{chapterUrl}}", "selectors": { "image": ".reader-page img" } }
}
```

The CSS selectors for list titles, covers, and links should return corresponding elements in the same order and count. Manga and chapter links are resolved relative to the page; image URLs may use `src`, `data-src`, `data-original`, or `href`. Supported template variables are `{{query}}`, `{{mangaUrl}}`, and `{{chapterUrl}}`. A selector that matches nothing, mismatched result counts, failed HTTP status, or malformed config is surfaced as an error in the UI. Chapters store page image URLs and use the existing reader screen; images are fetched by the app's image loader when displayed.

## Installing sources and safety

Extension definitions are stored as JSON files in Huni's private app storage. Only HTTP(S) URLs are accepted, and an extension config cannot run code. A source can still track requests or serve material whose rights and terms vary; add sources you trust and use them lawfully. Huni does not endorse or bundle third-party content.

The previously requested `https://github.com/keiyoushi/extensions/raw/repo/index.pb` is a protobuf catalog, not a JSON index, and is not compatible with this JSON-only extension format. Huni does not fetch or decode it.
