package com.example.muyinteresante.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.util.AtomicFile;
import com.example.muyinteresanteNoTocar.NoticiaRSS;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Todas las operaciones de JSON y disco están serializadas en un único worker. */
public final class NewsCacheManager {
    private static final String TAG = "NewsCacheManager";
    private static final String CACHE_FILE_NAME = "noticias_cache.json";
    private static final ExecutorService DISK = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private NewsCacheManager() { }

    public interface CacheCallback {
        void onLoaded(ArrayList<NoticiaRSS> noticias);
    }

    public static void saveNewsToCache(Context context, List<NoticiaRSS> list) {
        if (context == null || list == null) return;
        final Context app = context.getApplicationContext();
        // Solo una instantánea de referencias; serialización y escritura en DISK.
        final ArrayList<NoticiaRSS> snapshot = new ArrayList<>(list);
        DISK.execute(() -> {
            AtomicFile file = new AtomicFile(new File(app.getCacheDir(), CACHE_FILE_NAME));
            FileOutputStream output = null;
            try {
                JSONArray array = new JSONArray();
                for (NoticiaRSS n : snapshot) {
                    if (n == null) continue;
                    JSONObject obj = new JSONObject();
                    obj.put("titulo", n.getTitulo() != null ? n.getTitulo() : "");
                    obj.put("descripcion", n.getDescripcion() != null ? n.getDescripcion() : "");
                    obj.put("enlace", n.getEnlace() != null ? n.getEnlace() : "");
                    obj.put("urlImagen", n.getUrlImagen() != null ? n.getUrlImagen() : "");
                    obj.put("strFecha", n.getStrFecha() != null ? n.getStrFecha() : "");
                    Date fecha = n.getFechaNoticia();
                    obj.put("timestamp", fecha != null ? fecha.getTime() : 0L);
                    array.put(obj);
                }
                output = file.startWrite();
                byte[] json = array.toString().getBytes("UTF-8");
                output.write(json);
                file.finishWrite(output);
                output = null;
                Log.d(TAG, "Cache guardada: " + snapshot.size());
            } catch (Exception e) {
                if (output != null) file.failWrite(output);
                Log.e(TAG, "Error guardando cache", e);
            }
        });
    }

    public static void loadNewsFromCache(Context context, CacheCallback callback) {
        if (context == null || callback == null) return;
        final Context app = context.getApplicationContext();
        DISK.execute(() -> {
            ArrayList<NoticiaRSS> result = new ArrayList<>();
            try {
                AtomicFile file = new AtomicFile(new File(app.getCacheDir(), CACHE_FILE_NAME));
                if (file.getBaseFile().exists()) {
                    String data = new String(file.readFully(), "UTF-8");
                    JSONArray array = new JSONArray(data);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        long timestamp = obj.optLong("timestamp", 0L);
                        result.add(new NoticiaRSS(
                                obj.optString("titulo", ""),
                                obj.optString("descripcion", ""),
                                obj.optString("enlace", ""),
                                obj.optString("urlImagen", ""),
                                timestamp > 0 ? new Date(timestamp) : null));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error leyendo cache", e);
            }
            final ArrayList<NoticiaRSS> loaded = result;
            MAIN.post(() -> callback.onLoaded(loaded));
        });
    }
}
