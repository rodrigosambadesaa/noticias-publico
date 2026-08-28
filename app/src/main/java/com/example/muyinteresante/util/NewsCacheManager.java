package com.example.muyinteresante.util;

import android.content.Context;
import android.util.Log;
import com.example.muyinteresanteNoTocar.NoticiaRSS;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.util.ArrayList;
import java.util.Date;

public class NewsCacheManager {
    private static final String TAG = "NewsCacheManager";
    private static final String CACHE_FILE_NAME = "noticias_cache.json";

    public static void saveNewsToCache(Context context, ArrayList<NoticiaRSS> list) {
        if (context == null || list == null) return;
        try {
            File cacheFile = new File(context.getCacheDir(), CACHE_FILE_NAME);
            JSONArray array = new JSONArray();

            for (NoticiaRSS n : list) {
                JSONObject obj = new JSONObject();
                obj.put("titulo", n.getTitulo() != null ? n.getTitulo() : "");
                obj.put("descripcion", n.getDescripcion() != null ? n.getDescripcion() : "");
                obj.put("enlace", n.getEnlace() != null ? n.getEnlace() : "");
                obj.put("urlImagen", n.getUrlImagen() != null ? n.getUrlImagen() : "");
                obj.put("strFecha", n.getStrFecha() != null ? n.getStrFecha() : "");
                if (n.getFechaNoticia() != null) {
                    obj.put("timestamp", n.getFechaNoticia().getTime());
                } else {
                    obj.put("timestamp", 0L);
                }
                array.put(obj);
            }

            FileOutputStream fos = new FileOutputStream(cacheFile);
            OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
            writer.write(array.toString());
            writer.close();
            fos.close();
            Log.d(TAG, "Noticias guardadas en caché: " + list.size());
        } catch (Exception e) {
            Log.e(TAG, "Error guardando noticias en caché", e);
        }
    }

    public static ArrayList<NoticiaRSS> loadNewsFromCache(Context context) {
        ArrayList<NoticiaRSS> result = new ArrayList<>();
        if (context == null) return result;
        try {
            File cacheFile = new File(context.getCacheDir(), CACHE_FILE_NAME);
            if (!cacheFile.exists()) return result;

            FileInputStream fis = new FileInputStream(cacheFile);
            InputStreamReader reader = new InputStreamReader(fis, "UTF-8");
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
            reader.close();
            fis.close();

            JSONArray array = new JSONArray(sb.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String titulo = obj.optString("titulo", "");
                String descripcion = obj.optString("descripcion", "");
                String enlace = obj.optString("enlace", "");
                String urlImagen = obj.optString("urlImagen", "");
                long timestamp = obj.optLong("timestamp", 0L);

                NoticiaRSS noticia = new NoticiaRSS(titulo, descripcion, enlace, urlImagen, timestamp > 0 ? new Date(timestamp) : null);
                result.add(noticia);
            }
            Log.d(TAG, "Noticias cargadas desde caché: " + result.size());
        } catch (Exception e) {
            Log.e(TAG, "Error cargando noticias desde caché", e);
        }
        return result;
    }
}
