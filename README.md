# Noticias Público
Independent Android news reader for Público.

Feed: https://news.google.com/rss/search?q=site%3Apublico.es&hl=es&gl=ES&ceid=ES%3Aes
Feed mode: google-news-site-filter

The build tries Público direct RSS and HTML RSS autodiscovery first. If Público does not expose a usable feed to a clean client, it falls back to Google News RSS restricted to site:publico.es.

Includes search, refresh, sharing, article view, offline cache, infinite scroll through older feed windows, passive NetworkObserver, DNS-first reachability, and optional ICMP diagnostics. The current requested gist is vendored under third_party/connectivity.

## Network request flow

Remote operations first use the cheap passive `isConnected()`/`NetworkCapabilities` guard. When a usable network exists, RSS and article requests run directly; the real response remains authoritative for redirects, HTTP status codes, timeouts and parsing. The active `ConnectivityAndInternetAccess` diagnostic runs only after an ambiguous connectivity failure without an HTTP response. A valid HTTP response, including a feed/server error status, is reported as a service-specific failure without launching a redundant general probe. Offline guards use the cached news and the corresponding offline UI.

This is not an official Público application.
