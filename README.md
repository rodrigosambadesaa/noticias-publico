# Noticias Público
Independent Android news reader for Público.

Feed: https://news.google.com/rss/search?q=site%3Apublico.es&hl=es&gl=ES&ceid=ES%3Aes
Feed mode: google-news-site-filter

The build tries Público direct RSS and HTML RSS autodiscovery first. If Público does not expose a usable feed to a clean client, it falls back to Google News RSS restricted to site:publico.es.

Includes search, refresh, sharing, article view, offline cache, infinite scroll through older feed windows, passive NetworkObserver, DNS-first reachability, and optional ICMP diagnostics. The current requested gist is vendored under third_party/connectivity at revision `3b0497e976765653a7467e3bd7d6bff28b96bd7c`.

## Network request flow

Remote operations first use the cheap passive `isConnected()`/`NetworkCapabilities` guard. When a usable network exists, RSS and article requests run directly; the real response remains authoritative for redirects, HTTP status codes, timeouts and parsing. The active `ConnectivityAndInternetAccess` diagnostic runs only after an ambiguous connectivity failure without an HTTP response. A valid HTTP response, including a feed/server error status, is reported as a service-specific failure without launching a redundant general probe. Offline guards use the cached news and the corresponding offline UI.

## Orientation state

La actividad principal conserva la lista y la posición actual del `RecyclerView` al cambiar la orientación. Además, guarda y restaura el estado para recreaciones posteriores sin relanzar la descarga RSS.

Before starting a download, the app also confirms that Android still reports a connected active interface, preventing stale capabilities from showing the progress indicator without a real network.

## VPN/AdGuard manual checks

The request guard is `isConnected(context) && hasPhysicalNetwork(context)`. A VPN
transport by itself is not treated as a usable physical network, while a VPN over
Wi-Fi, mobile data, or Ethernet remains usable. To verify this on a device, repeat
initial load, pull-to-refresh, retry, and infinite-scroll pagination in these states:

1. Airplane mode or no interface: no RSS request, no progress indicator, cached/offline UI.
2. VPN/AdGuard with its underlying Wi-Fi/mobile interface disconnected: same offline behavior.
3. VPN/AdGuard over an active Wi-Fi/mobile interface: the real RSS request runs directly.
4. Feed failure with a physical network: the app runs the general diagnostic only for an ambiguous connectivity exception; a valid HTTP status is reported as a feed/service result.

This is not an official Público application.
