# Noticias Público
Independent Android news reader for Público.

Feed: https://news.google.com/rss/search?q=site%3Apublico.es&hl=es&gl=ES&ceid=ES%3Aes
Feed mode: google-news-site-filter

The build tries Público direct RSS and HTML RSS autodiscovery first. If Público does not expose a usable feed to a clean client, it falls back to Google News RSS restricted to site:publico.es.

Includes search, refresh, sharing, article view, offline cache, passive NetworkObserver and full connectivity diagnostics. The complete requested gist is vendored under third_party/connectivity.

This is not an official Público application.
