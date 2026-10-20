package com.example.muyinteresante;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.muyinteresante.util.NewsCacheManager;
import com.example.muyinteresanteNoTocar.NoticiaRSS;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/** Estado del feed independiente de Activity: sobrevive a los cambios de configuración. */
public final class NewsViewModel extends AndroidViewModel {
    public final ArrayList<NoticiaRSS> noticiasDisponibles = new ArrayList<>();
    public final ArrayList<NoticiaRSS> visibleNews = new ArrayList<>();
    public Date archiveBeforeDate;
    public int noticiasVisibles = 0;
    public int nextArchivePage = 2;
    public int consecutiveDuplicatePages = 0;
    public boolean isLoadingMore = false;
    public boolean hasMoreNews = false;
    public boolean hasFreshFeed = false;
    public String searchQuery = "";
    private boolean cacheRequested = false;
    private final MutableLiveData<Integer> cacheRevision = new MutableLiveData<>();

    public NewsViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Integer> getCacheRevision() {
        return cacheRevision;
    }

    public void loadCacheOnce(int pageSize) {
        if (cacheRequested) return;
        cacheRequested = true;
        NewsCacheManager.loadNewsFromCache(getApplication(), cached -> {
            // Una lectura atrasada jamás puede sustituir noticias recibidas de la red.
            if (hasFreshFeed || !noticiasDisponibles.isEmpty() || cached.isEmpty()) return;
            noticiasDisponibles.addAll(cached);
            noticiasVisibles = Math.min(pageSize, noticiasDisponibles.size());
            hasMoreNews = noticiasVisibles < noticiasDisponibles.size();
            cacheRevision.setValue(noticiasVisibles);
        });
    }

    public void saveCache(List<NoticiaRSS> items) {
        NewsCacheManager.saveNewsToCache(getApplication(), items);
    }
}
