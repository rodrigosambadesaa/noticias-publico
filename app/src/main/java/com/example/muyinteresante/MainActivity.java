package com.example.muyinteresante;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.muyinteresante.util.ConnectivityAndInternetAccess;
import com.example.muyinteresante.util.NewsCacheManager;
import com.example.muyinteresanteNoTocar.DescargaNoticiasRSS;
import com.example.muyinteresanteNoTocar.NoticiaRSS;
import com.example.muyinteresanteNoTocar.iNoticiaRSS;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements iNoticiaRSS {

    private static final String TAG = "MainActivity";
    private static final String RSS_URL = "https://news.google.com/rss/search?q=site%3Apublico.es&hl=es&gl=ES&ceid=ES%3Aes";
    private static final String RSS_PAGE_URL = "https://news.google.com/rss/search?q=site%3Apublico.es&hl=es&gl=ES&ceid=ES%3Aes";
    private static final int LOAD_MORE_THRESHOLD = 4;
    private static final int MAX_CONSECUTIVE_DUPLICATE_PAGES = 2;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvNoticias;
    private NoticiasAdapter adapter;

    private LinearLayout bannerNetworkNotice;
    private TextView tvBannerText;
    private Button btnDiagnosticarRed;

    private LinearLayout layoutNetworkStatusPill;
    private View viewNetworkDot;
    private TextView tvNetworkStatusText;

    private LinearLayout layoutEmptyState;
    private Button btnReintentar;

    private ConnectivityAndInternetAccess.NetworkObserver networkObserver;
    private ConnectivityAndInternetAccess.NetworkState currentNetworkState;

    private boolean isLoadingMore = false;
    private boolean hasMoreNews = false;
    private int nextArchivePage = 2;
    private int consecutiveDuplicatePages = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvNoticias = findViewById(R.id.rvNoticias);
        bannerNetworkNotice = findViewById(R.id.bannerNetworkNotice);
        tvBannerText = findViewById(R.id.tvBannerText);
        btnDiagnosticarRed = findViewById(R.id.btnDiagnosticarRed);

        layoutNetworkStatusPill = findViewById(R.id.layoutNetworkStatusPill);
        viewNetworkDot = findViewById(R.id.viewNetworkDot);
        tvNetworkStatusText = findViewById(R.id.tvNetworkStatusText);

        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        btnReintentar = findViewById(R.id.btnReintentar);

        // Soporte para márgenes de ventana/cámara en smartphones tipo S25 Ultra
        final View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, new OnApplyWindowInsetsListener() {
                @Override
                public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                    int top = insets.getSystemWindowInsetTop();
                    int bottom = insets.getSystemWindowInsetBottom();
                    int left = insets.getSystemWindowInsetLeft();
                    int right = insets.getSystemWindowInsetRight();

                    if (toolbar != null && top > 0) {
                        toolbar.setPadding(left, top, right, 0);
                    }
                    if (rvNoticias != null && bottom > 0) {
                        rvNoticias.setPadding(left, rvNoticias.getPaddingTop(), right, bottom + 12);
                    }
                    return insets;
                }
            });
        }

        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvNoticias.setLayoutManager(layoutManager);
        adapter = new NoticiasAdapter(this, new ArrayList<NoticiaRSS>(), new NoticiasAdapter.OnNoticiaClickListener() {
            @Override
            public void onNoticiaClick(NoticiaRSS noticia) {
                if (noticia != null && noticia.getEnlace() != null) {
                    Intent intent = new Intent(MainActivity.this, DetalleActivity.class);
                    intent.putExtra(DetalleActivity.EXTRA_URL, noticia.getEnlace());
                    intent.putExtra(DetalleActivity.EXTRA_TITULO, noticia.getTitulo());
                    startActivity(intent);
                }
            }
        });
        rvNoticias.setAdapter(adapter);

        // Infinite scroll: cuando el usuario se aproxima al final se solicita la
        // siguiente página del feed oficial, que contiene noticias más antiguas.
        rvNoticias.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0 || isLoadingMore || !hasMoreNews || adapter == null) {
                    return;
                }

                int totalItems = adapter.getItemCount();
                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                if (totalItems > 0 && lastVisibleItem >= totalItems - 1 - LOAD_MORE_THRESHOLD) {
                    cargarMasNoticias();
                }
            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ejecutarDescargarNoticias();
            }
        });

        btnReintentar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ejecutarDescargarNoticias();
            }
        });

        View.OnClickListener listenerDiagnostico = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ejecutarDiagnosticoRedCompleto();
            }
        };
        layoutNetworkStatusPill.setOnClickListener(listenerDiagnostico);
        btnDiagnosticarRed.setOnClickListener(listenerDiagnostico);

        // Cargar noticias iniciales (intenta descargar o usa caché offline)
        cargarNoticiasIniciales();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Iniciar el observador pasivo de conectividad basado en el Gist
        networkObserver = ConnectivityAndInternetAccess.observeNetwork(this, new ConnectivityAndInternetAccess.NetworkStateCallback() {
            @Override
            public void onStateChanged(ConnectivityAndInternetAccess.NetworkState state) {
                currentNetworkState = state;
                actualizarInterfazEstadoRed(state);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (networkObserver != null) {
            networkObserver.close();
            networkObserver = null;
        }
    }

    private void actualizarInterfazEstadoRed(ConnectivityAndInternetAccess.NetworkState state) {
        // Comprobaciones avanzadas de red usando los métodos relevantes de ConnectivityAndInternetAccess
        boolean isConnectedOrConnecting = ConnectivityAndInternetAccess.isConnectedOrConnecting(this);
        boolean isConnected = ConnectivityAndInternetAccess.isConnected(this);
        boolean isWifi = ConnectivityAndInternetAccess.isConnectedWifi(this);
        boolean isMobile = ConnectivityAndInternetAccess.isConnectedMobile(this);
        boolean isVpn = ConnectivityAndInternetAccess.vpnActive(this);
        boolean isAirplane = ConnectivityAndInternetAccess.isAirplaneModeOn(this);
        boolean isFast = ConnectivityAndInternetAccess.isConnectedFast(this);
        boolean isCaptive = ConnectivityAndInternetAccess.isCaptivePortalDetected(this);
        boolean isValidated = ConnectivityAndInternetAccess.isInternetValidated(this);

        Log.d(TAG, "Chequeo de red: ConnectedOrConnecting=" + isConnectedOrConnecting +
                ", Connected=" + isConnected + ", Wifi=" + isWifi + ", Mobile=" + isMobile +
                ", VPN=" + isVpn + ", Airplane=" + isAirplane + ", Fast=" + isFast);

        if (!isConnectedOrConnecting && !isConnected) {
            // Disconnected / Offline
            viewNetworkDot.setBackgroundResource(R.color.status_offline);
            tvNetworkStatusText.setText(isAirplane ? "Modo Avión" : "Sin red");
            tvNetworkStatusText.setTextColor(getResources().getColor(R.color.status_offline));

            bannerNetworkNotice.setVisibility(View.VISIBLE);
            bannerNetworkNotice.setBackgroundResource(R.color.status_offline_bg);
            tvBannerText.setText(isAirplane ?
                    "Modo Avión activado. Mostrando noticias guardadas en caché." :
                    "Dispositivo sin conexión a internet. Mostrando noticias guardadas en caché.");
        } else if (isCaptive) {
            // Captive Portal
            viewNetworkDot.setBackgroundResource(R.color.status_warning);
            tvNetworkStatusText.setText("Portal Cautivo");
            tvNetworkStatusText.setTextColor(getResources().getColor(R.color.status_warning));

            bannerNetworkNotice.setVisibility(View.VISIBLE);
            bannerNetworkNotice.setBackgroundResource(R.color.status_warning_bg);
            tvBannerText.setText("Se requiere inicio de sesión en red (Portal Cautivo detectado).");
        } else if (!isValidated && !isConnected) {
            // Connected without validated internet
            viewNetworkDot.setBackgroundResource(R.color.status_warning);
            tvNetworkStatusText.setText("Conectando...");
            tvNetworkStatusText.setTextColor(getResources().getColor(R.color.status_warning));

            bannerNetworkNotice.setVisibility(View.VISIBLE);
            bannerNetworkNotice.setBackgroundResource(R.color.status_warning_bg);
            tvBannerText.setText("Conectado a la interfaz de red pero sin acceso verificado a internet.");
        } else {
            // Fully connected & validated
            viewNetworkDot.setBackgroundResource(R.color.status_online);

            String statusType = "Online";
            if (isVpn) {
                statusType = "Online (VPN)";
            } else if (isWifi) {
                statusType = "Online (Wi-Fi)";
            } else if (isMobile) {
                statusType = isFast ? "Online (4G/5G)" : "Online (Móvil Lento)";
            }
            tvNetworkStatusText.setText(statusType);
            tvNetworkStatusText.setTextColor(getResources().getColor(R.color.status_online));

            bannerNetworkNotice.setVisibility(View.GONE);
        }
    }

    private void cargarNoticiasIniciales() {
        // Cargar desde caché offline primero para renderizado instantáneo
        ArrayList<NoticiaRSS> cached = NewsCacheManager.loadNewsFromCache(this);
        if (cached != null && !cached.isEmpty()) {
            adapter.updateData(cached);
            layoutEmptyState.setVisibility(View.GONE);
            rvNoticias.setVisibility(View.VISIBLE);
        }

        // Luego lanzar la descarga del RSS
        ejecutarDescargarNoticias();
    }

    private void ejecutarDescargarNoticias() {
        // Comprobación rápida inicial de estado de red antes del sondeador activo
        if (!ConnectivityAndInternetAccess.isConnectedOrConnecting(this)) {
            Toast.makeText(this, "Sin conexión disponible para iniciar la descarga.", Toast.LENGTH_SHORT).show();
            usarNoticiasOffline();
            return;
        }

        swipeRefreshLayout.setRefreshing(true);

        // Sondeo activo DNS-first / HTTP probe del Gist
        ConnectivityAndInternetAccess.checkInternetAsyncDefault(this, new ConnectivityAndInternetAccess.InternetCallback() {
            @Override
            public void onResult(ConnectivityAndInternetAccess.InternetResult result) {
                if (result != null && result.isReachable()) {
                    Log.d(TAG, "Conexión a internet verificada mediante sondeador DNS/HTTP (" + result.getElapsedMilliseconds() + "ms). Iniciando descarga RSS...");
                    new DescargaNoticiasRSS(MainActivity.this, MainActivity.this).execute(RSS_URL, NoticiaRSS.RSS_MUY_INTERESANTE);
                } else {
                    swipeRefreshLayout.setRefreshing(false);
                    Log.w(TAG, "Chequeo activo de internet falló");
                    Toast.makeText(MainActivity.this, "Sin acceso a internet para descargar noticias.", Toast.LENGTH_SHORT).show();
                    usarNoticiasOffline();
                }
            }
        });
    }

    /**
     * Solicita una página adicional del feed oficial sin bloquear la interfaz con
     * un diálogo modal. Las páginas se acumulan en el adapter y se deduplican.
     */
    private void cargarMasNoticias() {
        if (isLoadingMore || !hasMoreNews || adapter == null) {
            return;
        }

        if (!ConnectivityAndInternetAccess.isConnectedOrConnecting(this)) {
            Log.d(TAG, "No se cargan más noticias: sin conexión disponible.");
            return;
        }

        isLoadingMore = true;
        final int pageToLoad = nextArchivePage;
        Log.d(TAG, "Solicitando noticias antiguas. Página RSS: " + pageToLoad);

        ConnectivityAndInternetAccess.checkInternetAsyncDefault(this, new ConnectivityAndInternetAccess.InternetCallback() {
            @Override
            public void onResult(ConnectivityAndInternetAccess.InternetResult result) {
                if (result == null || !result.isReachable()) {
                    isLoadingMore = false;
                    Log.w(TAG, "No se pudo verificar internet para cargar la página " + pageToLoad);
                    return;
                }

                new DescargaNoticiasRSS(MainActivity.this, new iNoticiaRSS() {
                    @Override
                    public void onRecibeNoticiasRSS(ArrayList<NoticiaRSS> listaNoticias) {
                        isLoadingMore = false;

                        if (listaNoticias == null) {
                            // Error transitorio: no avanzamos de página para poder reintentarlo.
                            Log.w(TAG, "Error descargando la página RSS " + pageToLoad + ". Se reintentará al volver al final.");
                            return;
                        }

                        if (listaNoticias.isEmpty()) {
                            hasMoreNews = false;
                            Log.d(TAG, "Fin del archivo RSS alcanzado en la página " + pageToLoad);
                            Toast.makeText(MainActivity.this, "No hay más noticias antiguas disponibles", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int added = adapter.appendData(listaNoticias);
                        nextArchivePage = pageToLoad + 1;

                        if (added > 0) {
                            consecutiveDuplicatePages = 0;
                            NewsCacheManager.saveNewsToCache(MainActivity.this, adapter.getAllData());
                            Log.d(TAG, "Página " + pageToLoad + " cargada: " + added + " noticias nuevas (" + listaNoticias.size() + " recibidas).");
                        } else {
                            consecutiveDuplicatePages++;
                            Log.d(TAG, "Página " + pageToLoad + " sin noticias nuevas tras deduplicar.");

                            // Algunos feeds pueden repetir una página al cambiar su contenido.
                            // Saltamos como máximo un pequeño número de páginas para evitar un bucle infinito.
                            if (consecutiveDuplicatePages >= MAX_CONSECUTIVE_DUPLICATE_PAGES) {
                                hasMoreNews = false;
                                Log.w(TAG, "Se detiene la paginación tras varias páginas consecutivas duplicadas.");
                            } else {
                                rvNoticias.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        cargarMasNoticias();
                                    }
                                });
                            }
                        }
                    }
                }, false).execute(RSS_PAGE_URL + pageToLoad, NoticiaRSS.RSS_MUY_INTERESANTE);
            }
        });
    }

    private void usarNoticiasOffline() {
        ArrayList<NoticiaRSS> cached = NewsCacheManager.loadNewsFromCache(this);
        if (cached != null && !cached.isEmpty()) {
            adapter.updateData(cached);
            layoutEmptyState.setVisibility(View.GONE);
            rvNoticias.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Mostrando noticias guardadas en modo offline", Toast.LENGTH_SHORT).show();
        } else {
            rvNoticias.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRecibeNoticiasRSS(ArrayList<NoticiaRSS> listaNoticias) {
        swipeRefreshLayout.setRefreshing(false);

        if (listaNoticias != null && !listaNoticias.isEmpty()) {
            adapter.updateData(listaNoticias);
            NewsCacheManager.saveNewsToCache(this, listaNoticias);
            layoutEmptyState.setVisibility(View.GONE);
            rvNoticias.setVisibility(View.VISIBLE);

            // Una actualización completa reinicia el recorrido del archivo.
            nextArchivePage = 2;
            hasMoreNews = false;
            isLoadingMore = false;
            consecutiveDuplicatePages = 0;

            Log.d(TAG, "Noticias recibidas con éxito: " + listaNoticias.size());
        } else {
            Toast.makeText(this, "No se pudieron obtener nuevas noticias del canal RSS", Toast.LENGTH_SHORT).show();
            usarNoticiasOffline();
        }
    }

    private void ejecutarDiagnosticoRedCompleto() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Diagnóstico de Conectividad")
                .setMessage("Ejecutando comprobación avanzada de red y sondeo activo DNS/HTTP...")
                .setPositiveButton("Cerrar", null)
                .show();

        // Chequeos estáticos rápidos de ConnectivityAndInternetAccess
        boolean isConnectedOrConnecting = ConnectivityAndInternetAccess.isConnectedOrConnecting(this);
        boolean isConnected = ConnectivityAndInternetAccess.isConnected(this);
        boolean isWifi = ConnectivityAndInternetAccess.isConnectedWifi(this);
        boolean isMobile = ConnectivityAndInternetAccess.isConnectedMobile(this);
        boolean isFast = ConnectivityAndInternetAccess.isConnectedFast(this);
        boolean isVpn = ConnectivityAndInternetAccess.vpnActive(this);
        boolean isAirplane = ConnectivityAndInternetAccess.isAirplaneModeOn(this);

        // Sondeo activo DNS/HTTP
        ConnectivityAndInternetAccess.checkInternetAsyncDefault(this, new ConnectivityAndInternetAccess.InternetCallback() {
            @Override
            public void onResult(ConnectivityAndInternetAccess.InternetResult result) {
                if (dialog != null && dialog.isShowing()) {
                    boolean reachable = result != null && result.isReachable();
                    String reachedHost = result != null ? result.getReachedHost() : "Ninguno";
                    long time = result != null ? result.getElapsedMilliseconds() : 0;

                    StringBuilder sb = new StringBuilder();
                    sb.append("📡 ESTADO DE INTERFAZ DE RED:\n");
                    sb.append("• Estado general: ").append(isConnected ? "Conectado" : (isConnectedOrConnecting ? "Conectando..." : "Desconectado")).append("\n");
                    sb.append("• Tipo de red: ").append(isWifi ? "Wi-Fi" : (isMobile ? "Móvil / Celular" : "Otra / Ninguna")).append("\n");
                    sb.append("• Velocidad estimada: ").append(isFast ? "Rápida (High Speed)" : "Lenta / Desconocida").append("\n");
                    sb.append("• Red VPN Activa: ").append(isVpn ? "SÍ" : "No").append("\n");
                    sb.append("• Modo Avión: ").append(isAirplane ? "ACTIVADO" : "Desactivado").append("\n\n");

                    sb.append("🔍 PRUEBA ACTIVA DNS/HTTP (GIST):\n");
                    sb.append("• Internet Real: ").append(reachable ? "SÍ (Internet Verificado)" : "NO (Sin Internet)").append("\n");
                    sb.append("• Servidor alcanzado: ").append(reachedHost).append("\n");
                    sb.append("• Latencia de respuesta: ").append(time).append(" ms\n");

                    if (currentNetworkState != null) {
                        sb.append("\n📋 REGISTRO DE RED (NetworkState):\n");
                        sb.append(currentNetworkState.toString());
                    }

                    dialog.setMessage(sb.toString());
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_buscar);
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint("Buscar noticia...");
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        if (adapter != null) {
                            adapter.filter(query);
                        }
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        if (adapter != null) {
                            adapter.filter(newText);
                        }
                        return true;
                    }
                });
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_actualizar) {
            ejecutarDescargarNoticias();
            return true;
        } else if (id == R.id.action_test_conectividad) {
            ejecutarDiagnosticoRedCompleto();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}