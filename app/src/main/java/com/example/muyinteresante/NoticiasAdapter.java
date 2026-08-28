package com.example.muyinteresante;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.muyinteresanteNoTocar.AsignaImagenDeURL;
import com.example.muyinteresanteNoTocar.NoticiaRSS;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoticiasAdapter extends RecyclerView.Adapter<NoticiasAdapter.NoticiaViewHolder> {

    private final Context context;
    private final List<NoticiaRSS> listaOriginal;
    private final List<NoticiaRSS> listaFiltrada;
    private final OnNoticiaClickListener listener;
    private String currentQuery = "";

    public interface OnNoticiaClickListener {
        void onNoticiaClick(NoticiaRSS noticia);
    }

    public NoticiasAdapter(Context context, List<NoticiaRSS> lista, OnNoticiaClickListener listener) {
        this.context = context;
        this.listaOriginal = new ArrayList<>(lista != null ? lista : new ArrayList<NoticiaRSS>());
        this.listaFiltrada = new ArrayList<>(this.listaOriginal);
        this.listener = listener;
    }

    public void updateData(List<NoticiaRSS> nuevaLista) {
        this.listaOriginal.clear();
        if (nuevaLista != null) {
            this.listaOriginal.addAll(nuevaLista);
        }
        filter(currentQuery);
    }

    /**
     * Añade noticias nuevas conservando las ya cargadas. Se deduplica por URL y,
     * como respaldo, por título+fecha para evitar repetir elementos entre páginas RSS.
     *
     * @return número de noticias realmente añadidas.
     */
    public int appendData(List<NoticiaRSS> nuevasNoticias) {
        if (nuevasNoticias == null || nuevasNoticias.isEmpty()) {
            return 0;
        }

        Set<String> clavesExistentes = new HashSet<>();
        for (NoticiaRSS noticia : listaOriginal) {
            clavesExistentes.add(buildKey(noticia));
        }

        int added = 0;
        for (NoticiaRSS noticia : nuevasNoticias) {
            String key = buildKey(noticia);
            if (!clavesExistentes.contains(key)) {
                listaOriginal.add(noticia);
                clavesExistentes.add(key);
                added++;
            }
        }

        if (added > 0) {
            filter(currentQuery);
        }
        return added;
    }

    public ArrayList<NoticiaRSS> getAllData() {
        return new ArrayList<>(listaOriginal);
    }

    private String buildKey(NoticiaRSS noticia) {
        if (noticia == null) {
            return "null";
        }
        String enlace = noticia.getEnlace();
        if (enlace != null && !enlace.trim().isEmpty()) {
            return "url:" + enlace.trim();
        }
        String titulo = noticia.getTitulo() != null ? noticia.getTitulo().trim() : "";
        String fecha = noticia.getStrFecha() != null ? noticia.getStrFecha().trim() : "";
        return "fallback:" + titulo + "|" + fecha;
    }

    public void filter(String query) {
        currentQuery = query != null ? query : "";
        listaFiltrada.clear();
        if (currentQuery.trim().isEmpty()) {
            listaFiltrada.addAll(listaOriginal);
        } else {
            String lowerQuery = currentQuery.toLowerCase().trim();
            for (NoticiaRSS item : listaOriginal) {
                if ((item.getTitulo() != null && item.getTitulo().toLowerCase().contains(lowerQuery)) ||
                    (item.getDescripcion() != null && item.getDescripcion().toLowerCase().contains(lowerQuery))) {
                    listaFiltrada.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoticiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_noticia, parent, false);
        return new NoticiaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticiaViewHolder holder, int position) {
        final NoticiaRSS noticia = listaFiltrada.get(position);

        holder.tvTitulo.setText(noticia.getTitulo());
        holder.tvDescripcion.setText(noticia.getDescripcionSinHTML());

        String fechaText = noticia.getFechaComoStringFormateado();
        if (fechaText == null || fechaText.isEmpty()) {
            fechaText = noticia.getStrFecha();
        }
        holder.tvFecha.setText(fechaText != null ? fechaText : "");

        String urlImagen = noticia.getUrlImagen();
        if (urlImagen == null || urlImagen.trim().isEmpty()) {
            urlImagen = noticia.getEnlace();
        }
        holder.imgNoticia.setImageBitmap(null);
        if (urlImagen != null && !urlImagen.trim().isEmpty()) {
            holder.imgNoticia.setTag(urlImagen);
            new AsignaImagenDeURL(holder.imgNoticia, context).execute(urlImagen);
        } else {
            holder.imgNoticia.setTag(null);
            holder.imgNoticia.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onNoticiaClick(noticia);
                }
            }
        };

        holder.itemView.setOnClickListener(clickListener);
        holder.btnLeerMas.setOnClickListener(clickListener);

        if (holder.btnNavegador != null) {
            holder.btnNavegador.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (noticia != null && noticia.getEnlace() != null && !noticia.getEnlace().isEmpty()) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(noticia.getEnlace()));
                            Intent chooser = Intent.createChooser(intent, "Abrir noticia en...");
                            context.startActivity(chooser);
                        } catch (Exception e) {
                            Toast.makeText(context, "No se pudo abrir la noticia en el navegador", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }

        holder.btnCompartir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, noticia.getTitulo());
                    shareIntent.putExtra(Intent.EXTRA_TEXT, noticia.getTitulo() + "\n\n" + noticia.getEnlace());
                    context.startActivity(Intent.createChooser(shareIntent, "Compartir noticia vía"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaFiltrada.size();
    }

    public static class NoticiaViewHolder extends RecyclerView.ViewHolder {
        ImageView imgNoticia;
        TextView tvTitulo;
        TextView tvDescripcion;
        TextView tvFecha;
        Button btnLeerMas;
        Button btnNavegador;
        ImageButton btnCompartir;

        public NoticiaViewHolder(@NonNull View itemView) {
            super(itemView);
            imgNoticia = itemView.findViewById(R.id.imgNoticia);
            tvTitulo = itemView.findViewById(R.id.tvTituloNoticia);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionNoticia);
            tvFecha = itemView.findViewById(R.id.tvFechaNoticia);
            btnLeerMas = itemView.findViewById(R.id.btnLeerMas);
            btnNavegador = itemView.findViewById(R.id.btnNavegador);
            btnCompartir = itemView.findViewById(R.id.btnCompartir);
        }
    }
}