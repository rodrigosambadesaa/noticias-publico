package com.example.muyinteresante;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import android.os.Parcelable;
import android.os.Build;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.muyinteresante.util.ConnectivityAndInternetAccess;
import com.example.muyinteresante.util.NewsCacheManager;
import com.example.muyinteresante.util.RemoteOperationPolicy;
import com.example.muyinteresanteNoTocar.DescargaNoticiasRSS;
import com.example.muyinteresanteNoTocar.NoticiaRSS;
import com.example.muyinteresanteNoTocar.iNoticiaRSS;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements iNoticiaRSS {

    private static final String TAG = "MainActivity";
    private static final String RSS_URL = "https://news.google.com/rss/search?q=site%3Apublico.es&hl=es&gl=ES&ceid=ES%3Aes";
    private static final String RSS_PAGE_URL = "https://news.google.com/rss/search?q=site%3Apublico.es";
    private static final int LOAD_MORE_THRESHOLD = 4;
    private static final int MAX_CONSECUTIVE_DUPLICATE_PAGES = 2;
    private static final String STATE_HAS_NEWS = "main_has_news";
    private static final String STATE_LAYOUT = "main_layout_state";
    private static final String STATE_HAS_MORE = "main_has_more";
    private static final String STATE_NEXT_PAGE = "main_next_page";
    private static final String STATE_ARCHIVE_BEFORE = "main_archive_before";
    private static final String STATE_DUPLICATES = "main_duplicate_pages";

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

    private NewsViewModel newsState;
    private Toast activeToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        newsState = new ViewModelProvider(this).get(NewsViewModel.class);

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
            final int toolbarHeight = getResources().getDimensionPixelSize(R.dimen.toolbar_height);
            final int toolbarPaddingLeft = toolbar.getPaddingLeft();
            final int toolbarPaddingRight = toolbar.getPaddingRight();
            final int recyclerPaddingBottom = rvNoticias.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(rootView, new OnApplyWindowInsetsListener() {
                @Override
                public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                    int top = insets.getSystemWindowInsetTop();
                    int bottom = insets.getSystemWindowInsetBottom();
                    int left = insets.getSystemWindowInsetLeft();
                    int right = insets.getSystemWindowInsetRight();

                    if (toolbar != null && top > 0) {
                        toolbar.setPadding(toolbarPaddingLeft + left, top, toolbarPaddingRight + right, 0);
                        ViewGroup.LayoutParams toolbarParams = toolbar.getLayoutParams();
                        toolbarParams.height = toolbarHeight + top;
                        toolbar.setLayoutParams(toolbarParams);
                    }
                    if (rvNoticias != null && bottom > 0) {
                        rvNoticias.setPadding(left, rvNoticias.getPaddingTop(), right, recyclerPaddingBottom + bottom);
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
        if (!newsState.visibleNews.isEmpty()) {
            adapter.updateData(newsState.visibleNews);
            layoutEmptyState.setVisibility(View.GONE);
            rvNoticias.setVisibility(View.VISIBLE);
        }

        // Infinite scroll: cuando el usuario se aproxima al final se solicita la
        // siguiente página del feed oficial, que contiene noticias más antiguas.
        rvNoticias.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0 || newsState.isLoadingMore || !newsState.hasMoreNews || adapter == null) {
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

        // Tras una recreación por orientación, restaura la lista y el scroll desde
        // el estado/caché y no vuelve a iniciar la descarga del feed.
        if (!restaurarEstadoTrasRecreacion(savedInstanceState, layoutManager)) {
            cargarNoticiasIniciales();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        boolean hasNews = adapter != null && adapter.getItemCount() > 0;
        outState.putBoolean(STATE_HAS_NEWS, hasNews);
        if (rvNoticias != null && rvNoticias.getLayoutManager() != null) {
            outState.putParcelable(STATE_LAYOUT, rvNoticias.getLayoutManager().onSaveInstanceState());
        }
        outState.putBoolean(STATE_HAS_MORE, newsState.hasMoreNews);
        outState.putInt(STATE_NEXT_PAGE, newsState.nextArchivePage);
        outState.putInt(STATE_DUPLICATES, newsState.consecutiveDuplicatePages);
        if (newsState.archiveBeforeDate != null) {
            outState.putLong(STATE_ARCHIVE_BEFORE, newsState.archiveBeforeDate.getTime());
        }
    }

    private boolean restaurarEstadoTrasRecreacion(Bundle savedInstanceState,
                                                   final LinearLayoutManager layoutManager) {
        if (savedInstanceState == null || !savedInstanceState.getBoolean(STATE_HAS_NEWS, false)) {
            return false;
        }
        Runnable restoreScroll = () -> {
            Parcelable state = savedInstanceState.getParcelable(STATE_LAYOUT);
            if (state != null) {
                rvNoticias.post(() -> layoutManager.onRestoreInstanceState(state));
            }
        };
        if (!newsState.visibleNews.isEmpty()) {
            adapter.updateData(newsState.visibleNews);
            layoutEmptyState.setVisibility(View.GONE);
            rvNoticias.setVisibility(View.VISIBLE);
            restoreScroll.run();
            return true;
        }
        NewsCacheManager.loadNewsFromCache(this, cached -> {
            if (isFinishing() || isDestroyed() || newsState.hasFreshFeed) return;
            if (cached != null && !cached.isEmpty()) {
                adapter.updateData(cached);
                layoutEmptyState.setVisibility(View.GONE);
                rvNoticias.setVisibility(View.VISIBLE);
            }
            restoreScroll.run();
        });
        return true;
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
        if (adapter != null) {
            newsState.visibleNews.clear();
            newsState.visibleNews.addAll(adapter.getAllData());
        }
        super.onStop();
        if (networkObserver != null) {
            networkObserver.close();
            networkObserver = null;
        }
    }

    private void actualizarInterfazEstadoRed(ConnectivityAndInternetAccess.NetworkState state) {
        // Comprobaciones avanzadas de red usando los métodos relevantes de ConnectivityAndInternetAccess
        boolean isConnectedOrConnecting = ConnectivityAndInternetAccess.isConnectedOrConnecting(this);
        boolean hasUsableNetwork = RemoteOperationPolicy.hasUsableNetwork(this);
        boolean isConnected = hasUsableNetwork;
        boolean isWifi = ConnectivityAndInternetAccess.isConnectedWifi(this);
        boolean isMobile = ConnectivityAndInternetAccess.isConnectedMobile(this);
        boolean isVpn = ConnectivityAndInternetAccess.vpnActive(this);
        boolean isAirplane = ConnectivityAndInternetAccess.isAirplaneModeOn(this);
        boolean isFast = ConnectivityAndInternetAccess.isConnectedFast(this);
        boolean isCaptive = ConnectivityAndInternetAccess.isCaptivePortalDetected(this);
        boolean isValidated = ConnectivityAndInternetAccess.isInternetValidated(this);
        boolean hasValidatedInternet = hasUsableNetwork
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isValidated);

        Log.d(TAG, getString(R.string.main_text_01) + isConnectedOrConnecting +
                getString(R.string.main_text_02) + isConnected + getString(R.string.main_text_03) + isWifi + getString(R.string.main_text_04) + isMobile +
                getString(R.string.main_text_05) + isVpn + getString(R.string.main_text_06) + isAirplane + getString(R.string.main_text_07) + isFast);

        if (!hasUsableNetwork) {
            // Disconnected / Offline
            swipeRefreshLayout.setRefreshing(false);
            newsState.isLoadingMore = false;
            viewNetworkDot.setBackgroundResource(R.color.status_offline);
            tvNetworkStatusText.setText(isAirplane ? getString(R.string.main_text_08) : getString(R.string.main_text_09));
            tvNetworkStatusText.setTextColor(Color.WHITE);

            bannerNetworkNotice.setVisibility(View.VISIBLE);
            bannerNetworkNotice.setBackgroundResource(R.color.status_offline_bg);
            tvBannerText.setText(isAirplane ?
                    getString(R.string.main_text_10) :
                    getString(R.string.main_text_11));
        } else if (isCaptive) {
            // Captive Portal
            viewNetworkDot.setBackgroundResource(R.color.status_warning);
            tvNetworkStatusText.setText(getString(R.string.main_text_12));
            tvNetworkStatusText.setTextColor(Color.WHITE);

            bannerNetworkNotice.setVisibility(View.VISIBLE);
            bannerNetworkNotice.setBackgroundResource(R.color.status_warning_bg);
            tvBannerText.setText(getString(R.string.main_text_13));
        } else if (!hasValidatedInternet) {
            // Hay una interfaz activa, pero Android no ha verificado Internet.
            viewNetworkDot.setBackgroundResource(R.color.status_warning);
            tvNetworkStatusText.setText(getString(R.string.main_text_14));
            tvNetworkStatusText.setTextColor(Color.WHITE);

            bannerNetworkNotice.setVisibility(View.VISIBLE);
            bannerNetworkNotice.setBackgroundResource(R.color.status_warning_bg);
            tvBannerText.setText(getString(R.string.main_text_15));
        } else {
            // Fully connected & validated
            viewNetworkDot.setBackgroundResource(R.color.status_online);

            String statusType = getString(R.string.main_text_16);
            if (isVpn) {
                statusType = getString(R.string.main_text_17);
            } else if (isWifi) {
                statusType = getString(R.string.main_text_18);
            } else if (isMobile) {
                statusType = isFast ? getString(R.string.main_text_19) : getString(R.string.main_text_20);
            }
            tvNetworkStatusText.setText(statusType);
            tvNetworkStatusText.setTextColor(Color.WHITE);

            bannerNetworkNotice.setVisibility(View.GONE);
        }
    }

    private void cargarNoticiasIniciales() {
        swipeRefreshLayout.setRefreshing(false);
        NewsCacheManager.loadNewsFromCache(this, cached -> {
            if (isFinishing() || isDestroyed() || newsState.hasFreshFeed) return;
            if (cached != null && !cached.isEmpty() && adapter.getAllData().isEmpty()) {
                adapter.updateData(cached);
                layoutEmptyState.setVisibility(View.GONE);
                rvNoticias.setVisibility(View.VISIBLE);
            }
        });
        ejecutarDescargarNoticias();
    }


    private void ejecutarDescargarNoticias() {
        // Comprobación rápida inicial de estado de red antes del sondeador activo
        if (!RemoteOperationPolicy.hasUsableNetwork(this)) {
            swipeRefreshLayout.setRefreshing(false);
            mostrarToast(getString(R.string.main_text_21), Toast.LENGTH_SHORT);
            usarNoticiasOffline();
            return;
        }

        swipeRefreshLayout.setRefreshing(true);

        // La petición real al feed es la prueba definitiva del servicio.
        new DescargaNoticiasRSS(this, this).execute(RSS_URL, NoticiaRSS.RSS_MUY_INTERESANTE);
    }

    /**
     * Solicita una página adicional del feed oficial sin bloquear la interfaz con
     * un diálogo modal. Las páginas se acumulan en el adapter y se deduplican.
     */
    private void cargarMasNoticias() {
        if (newsState.isLoadingMore || !newsState.hasMoreNews || newsState.archiveBeforeDate == null || adapter == null) {
            return;
        }

        if (!RemoteOperationPolicy.hasUsableNetwork(this)) {
            Log.d(TAG, "No se cargan más noticias: sin conexión disponible.");
            mostrarToast(getString(R.string.main_text_22), Toast.LENGTH_SHORT);
            return;
        }

        newsState.isLoadingMore = true;
        final int pageToLoad = newsState.nextArchivePage;
        final Date requestedBeforeDate = newsState.archiveBeforeDate;
        final String archiveUrl = buildArchiveUrl(requestedBeforeDate);
        Log.d(TAG, "Solicitando noticias antiguas. Página RSS: " + pageToLoad
                + " (before=" + formatArchiveDate(requestedBeforeDate) + ")");

                new DescargaNoticiasRSS(this, new iNoticiaRSS() {
                    @Override
                    public void onError(DescargaNoticiasRSS.DownloadError error) {
                        newsState.isLoadingMore = false;
                        manejarFalloRemoto("la página antigua", error, false);
                    }

                    @Override
                    public void onRecibeNoticiasRSS(ArrayList<NoticiaRSS> listaNoticias) {
                        newsState.isLoadingMore = false;

                        if (listaNoticias == null) {
                            // Error transitorio: no avanzamos de página para poder reintentarlo.
                            Log.w(TAG, "Error descargando la página RSS " + pageToLoad + ". Se reintentará al volver al final.");
                            return;
                        }

                        if (listaNoticias.isEmpty()) {
                            newsState.hasMoreNews = false;
                            Log.d(TAG, "Fin del archivo RSS alcanzado en la página " + pageToLoad);
                            Toast.makeText(MainActivity.this, getString(R.string.main_text_23), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Date fetchedOldestDate = findOldestNewsDate(listaNoticias);
                        int added = adapter.appendData(listaNoticias);
                        newsState.nextArchivePage = pageToLoad + 1;

                        if (added > 0) {
                            if (fetchedOldestDate == null || !fetchedOldestDate.before(requestedBeforeDate)) {
                                newsState.hasMoreNews = false;
                                Log.d(TAG, "El feed no avanzó a noticias más antiguas; se detiene el scroll infinito.");
                                return;
                            }
                            newsState.archiveBeforeDate = fetchedOldestDate;
                            newsState.consecutiveDuplicatePages = 0;
                            newsState.hasMoreNews = true;
                            NewsCacheManager.saveNewsToCache(MainActivity.this, adapter.getAllData());
                            Log.d(TAG, "Página " + pageToLoad + " cargada: " + added + " noticias nuevas (" + listaNoticias.size() + " recibidas).");
                        } else {
                            newsState.consecutiveDuplicatePages++;
                            Log.d(TAG, "Página " + pageToLoad + " sin noticias nuevas tras deduplicar.");

                            if (fetchedOldestDate != null && fetchedOldestDate.before(requestedBeforeDate)) {
                                newsState.archiveBeforeDate = fetchedOldestDate;
                            }

                            // Algunos feeds pueden repetir una página al cambiar su contenido.
                            // Saltamos como máximo un pequeño número de páginas para evitar un bucle infinito.
                            if (newsState.consecutiveDuplicatePages >= MAX_CONSECUTIVE_DUPLICATE_PAGES) {
                                newsState.hasMoreNews = false;
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
                }, false).execute(archiveUrl, NoticiaRSS.RSS_MUY_INTERESANTE);
    }

    private String buildArchiveUrl(Date beforeDate) {
        return RSS_PAGE_URL + "%20before%3A" + formatArchiveDate(beforeDate)
                + "&hl=es&gl=ES&ceid=ES%3Aes";
    }

    private String formatArchiveDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(date);
    }

    private Date findOldestNewsDate(ArrayList<NoticiaRSS> noticias) {
        Date oldest = null;
        if (noticias != null) {
            for (NoticiaRSS noticia : noticias) {
                if (noticia != null && noticia.getFechaNoticia() != null
                        && (oldest == null || noticia.getFechaNoticia().before(oldest))) {
                    oldest = noticia.getFechaNoticia();
                }
            }
        }
        return oldest;
    }

    private void usarNoticiasOffline() {
        if (adapter != null && !adapter.getAllData().isEmpty()) {
            layoutEmptyState.setVisibility(View.GONE);
            rvNoticias.setVisibility(View.VISIBLE);
            return;
        }
        NewsCacheManager.loadNewsFromCache(this, cached -> {
            if (isFinishing() || isDestroyed() || newsState.hasFreshFeed) return;
            if (cached != null && !cached.isEmpty()) {
                adapter.updateData(cached);
                layoutEmptyState.setVisibility(View.GONE);
                rvNoticias.setVisibility(View.VISIBLE);
            } else {
                rvNoticias.setVisibility(View.GONE);
                layoutEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }


    @Override
    public void onRecibeNoticiasRSS(ArrayList<NoticiaRSS> listaNoticias) {
        swipeRefreshLayout.setRefreshing(false);

        if (listaNoticias != null && !listaNoticias.isEmpty()) {
            newsState.hasFreshFeed = true;
            adapter.updateData(listaNoticias);
            NewsCacheManager.saveNewsToCache(this, listaNoticias);
            layoutEmptyState.setVisibility(View.GONE);
            rvNoticias.setVisibility(View.VISIBLE);

            // Una actualización completa reinicia el recorrido del archivo desde
            // la noticia más antigua que acaba de llegar.
            newsState.nextArchivePage = 2;
            newsState.archiveBeforeDate = findOldestNewsDate(listaNoticias);
            newsState.hasMoreNews = newsState.archiveBeforeDate != null;
            newsState.isLoadingMore = false;
            newsState.consecutiveDuplicatePages = 0;

            Log.d(TAG, "Noticias recibidas con éxito: " + listaNoticias.size());
        } else if (listaNoticias == null) {
            // DescargaNoticiasRSS ya notificó el error clasificado a onError().
            return;
        } else {
            mostrarToast(getString(R.string.main_text_24), Toast.LENGTH_SHORT);
            usarNoticiasOffline();
        }
    }

    @Override
    public void onError(DescargaNoticiasRSS.DownloadError error) {
        swipeRefreshLayout.setRefreshing(false);
        manejarFalloRemoto("el feed de noticias", error, true);
    }

    private void manejarFalloRemoto(final String servicio,
                                    final DescargaNoticiasRSS.DownloadError error,
                                    final boolean mostrarCache) {
        if (error != null && error.isConnectivityFailure()) {
            new ConnectivityAndInternetAccess.Builder().build().checkInternetAsync(this,
                    new ConnectivityAndInternetAccess.InternetCallback() {
                        @Override
                        public void onResult(ConnectivityAndInternetAccess.InternetResult result) {
                            boolean internetGeneral = result != null && result.isReachable();
                            if (internetGeneral) {
                                mostrarToast(getString(R.string.main_text_25),
                                        Toast.LENGTH_LONG);
                            } else {
                                mostrarToast(getString(R.string.main_text_26),
                                        Toast.LENGTH_LONG);
                            }
                            if (mostrarCache) {
                                usarNoticiasOffline();
                            }
                        }
                    });
            return;
        }

        String status = error != null && error.getHttpStatus() > 0
                ? " (HTTP " + error.getHttpStatus() + ")" : "";
        mostrarToast(getString(R.string.main_text_27) + servicio + status + getString(R.string.main_text_28), Toast.LENGTH_LONG);
        if (mostrarCache) {
            usarNoticiasOffline();
        }
    }

    private void mostrarToast(CharSequence message, int duration) {
        if (activeToast != null) {
            activeToast.cancel();
        }
        activeToast = Toast.makeText(this, message, duration);
        activeToast.show();
    }

    private void ejecutarDiagnosticoRedCompleto() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.main_text_29))
                .setMessage(getString(R.string.main_text_30))
                .setPositiveButton(getString(R.string.main_text_31), null)
                .show();

        if (!RemoteOperationPolicy.hasUsableNetwork(this)) {
            dialog.setMessage(getString(R.string.main_text_32)
                    + getString(R.string.main_text_33)
                    + getString(R.string.main_text_34)
                    + getString(R.string.main_text_35)
                    + getString(R.string.main_text_36));
            return;
        }

        // Sondeo activo DNS/HTTP
        ConnectivityAndInternetAccess.checkInternetAsyncDefault(this, new ConnectivityAndInternetAccess.InternetCallback() {
            @Override
            public void onResult(ConnectivityAndInternetAccess.InternetResult result) {
                if (dialog != null && dialog.isShowing()) {
                    boolean connectedNow = RemoteOperationPolicy.hasUsableNetwork(MainActivity.this);
                    boolean validatedNow = connectedNow
                            && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                            || ConnectivityAndInternetAccess.isInternetValidated(MainActivity.this));
                    boolean isConnectedOrConnectingNow = connectedNow
                            && ConnectivityAndInternetAccess.isConnectedOrConnecting(MainActivity.this);
                    boolean isWifiNow = connectedNow
                            && ConnectivityAndInternetAccess.isConnectedWifi(MainActivity.this);
                    boolean isMobileNow = connectedNow
                            && ConnectivityAndInternetAccess.isConnectedMobile(MainActivity.this);
                    boolean isFastNow = connectedNow
                            && ConnectivityAndInternetAccess.isConnectedFast(MainActivity.this);
                    boolean isVpnNow = connectedNow
                            && ConnectivityAndInternetAccess.vpnActive(MainActivity.this);
                    boolean isAirplaneNow = ConnectivityAndInternetAccess.isAirplaneModeOn(MainActivity.this);
                    boolean reachable = result != null && result.isReachable();
                    String reachedHost = result != null ? result.getReachedHost() : getString(R.string.main_text_37);
                    long time = result != null ? result.getElapsedMilliseconds() : 0;

                    StringBuilder sb = new StringBuilder();
                    sb.append(getString(R.string.main_text_32));
                    String interfaceStatus = !connectedNow
                            ? (isConnectedOrConnectingNow ? getString(R.string.main_text_38) : getString(R.string.main_text_39))
                            : (validatedNow ? getString(R.string.main_text_40) : getString(R.string.main_text_41));
                    sb.append(getString(R.string.main_text_42)).append(interfaceStatus).append("\n");
                    sb.append(getString(R.string.main_text_43)).append(isWifiNow ? getString(R.string.main_text_44) : (isMobileNow ? getString(R.string.main_text_45) : getString(R.string.main_text_46))).append("\n");
                    sb.append(getString(R.string.main_text_47)).append(isFastNow ? getString(R.string.main_text_48) : getString(R.string.main_text_49)).append("\n");
                    sb.append(getString(R.string.main_text_50)).append(isVpnNow ? getString(R.string.main_text_51) : getString(R.string.main_text_52)).append("\n");
                    sb.append(getString(R.string.main_text_53)).append(isAirplaneNow ? getString(R.string.main_text_54) : getString(R.string.main_text_55)).append("\n\n");

                    sb.append(getString(R.string.main_text_56));
                    sb.append(getString(R.string.main_text_57)).append(reachable ? getString(R.string.main_text_58) : getString(R.string.main_text_59)).append("\n");
                    sb.append(getString(R.string.main_text_60)).append(reachedHost).append("\n");
                    sb.append(getString(R.string.main_text_61)).append(time).append(getString(R.string.main_text_62));

                    if (currentNetworkState != null) {
                        sb.append(getString(R.string.main_text_63));
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
                searchView.setQueryHint(getString(R.string.main_text_64));
                searchView.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
                searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                    @Override public boolean onMenuItemActionExpand(MenuItem item) {
                        searchView.post(new Runnable() {
                            @Override public void run() {
                                searchView.setIconified(false);
                                searchView.requestFocus();
                                android.view.inputmethod.InputMethodManager ime =
                                        (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                                if (ime != null) {
                                    View focused = searchView.findFocus();
                                    ime.showSoftInput(focused != null ? focused : searchView,
                                            android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                                }
                            }
                        });
                        return true;
                    }
                    @Override public boolean onMenuItemActionCollapse(MenuItem item) {
                        searchView.setQuery("", false);
                        if (adapter != null) adapter.filter("");
                        return true;
                    }
                });
                
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
