package com.example.muyinteresanteNoTocar;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import com.example.muyinteresante.R;

import com.example.muyinteresante.util.RemoteOperationPolicy;

/* Parsea un canal RSS y devuelve sus items en un ArrayList */

public class DescargaNoticiasRSS extends AsyncTask<String,Integer,ArrayList<NoticiaRSS>>{

	private Context contexto=null;
	private iNoticiaRSS objetoReceptor=null;
	private ProgressDialog pd=null;
	private boolean mostrarProgreso=true;
	private DownloadError lastError;
	
	

	public static final class DownloadError {
		private final RemoteOperationPolicy.FailureKind kind;
		private final int httpStatus;
		private final String message;

		private DownloadError(RemoteOperationPolicy.FailureKind kind, int httpStatus, String message) {
			this.kind = kind;
			this.httpStatus = httpStatus;
			this.message = message != null ? message : "";
		}

		public boolean isConnectivityFailure() {
			return kind == RemoteOperationPolicy.FailureKind.CONNECTIVITY;
		}

		public boolean isServiceFailure() {
			return kind == RemoteOperationPolicy.FailureKind.SERVICE;
		}

		public int getHttpStatus() { return httpStatus; }
		public String getMessage() { return message; }
	}
	
	
	public DescargaNoticiasRSS(Context contexto, iNoticiaRSS objetoReceptor){
		this(contexto, objetoReceptor, true);
	}

	/**
	 * Permite reutilizar el descargador para paginación/infinite scroll sin abrir
	 * un ProgressDialog modal cada vez que se solicitan noticias antiguas.
	 */
	public DescargaNoticiasRSS(Context contexto, iNoticiaRSS objetoReceptor, boolean mostrarProgreso){
		this.contexto = contexto;
		this.objetoReceptor = objetoReceptor;
		this.mostrarProgreso = mostrarProgreso;
	}


	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		// Defensa final para cualquier caller: nunca mostramos progreso ni
		// iniciamos la petición si el dispositivo ya no tiene red activa.
		if (contexto != null && !RemoteOperationPolicy.hasUsableNetwork(contexto)) {
			cancel(false);
			return;
		}
		
		if (mostrarProgreso && contexto != null) {
			pd = new ProgressDialog(contexto);
			pd.setMessage(contexto.getString(R.string.downloading_news));
			pd.setCancelable(true);
			pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					DescargaNoticiasRSS.this.cancel(true);
				}
			});
			
			pd.show();
		}
	}

	
	@Override
	protected void onCancelled() {
		super.onCancelled();
		
		if (pd!=null) pd.dismiss();
	}
	
	 
	@Override							// Recibe URL y nombre Canal RSS.
	protected ArrayList<NoticiaRSS> doInBackground(String... params) {
		
		InputStream entrada = null;
		HttpURLConnection conex = null;
		
		try{
			 
			
			 // Creamos objeto URL a partir de la direccion web para conectarnos con el servidor
			URL url = new URL(params[0]);
			conex = (HttpURLConnection) url.openConnection(); // La petición real es la prueba del feed.
			conex.setConnectTimeout(15000);
			conex.setReadTimeout(30000);
			conex.setUseCaches(false); // Evitamos la cache de datos.
			conex.setInstanceFollowRedirects(true);
			conex.setRequestProperty("accept", "application/rss+xml, application/xml, text/xml, */*");
			conex.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) noticias-publico/1.0");
			int responseCode = conex.getResponseCode();
			if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
				lastError = new DownloadError(
						RemoteOperationPolicy.classify(null, responseCode),
						responseCode,
						"El feed respondió con HTTP " + responseCode);
				return null;
			}
			 
			 // Abrimos el fichero para su lectura/descarga
			entrada = conex.getInputStream();	

			final ArrayList<NoticiaRSS> noticias = new ArrayList<>();
            RssPullParser.parse(entrada, item -> {
                if (isCancelled()) return;
                try {
                    noticias.add(NoticiaRSS.fromRssFields(item.title, item.description,
                            item.link, item.pubDate, item.image));
                    publishProgress(noticias.size());
                } catch (Exception exception) {
                    Log.w("DescargaNoticiasRSS", "Noticia RSS inválida", exception);
                }
            });
            if (isCancelled()) return null;
            return noticias;
		}
		catch (Exception e){
			lastError = new DownloadError(
					RemoteOperationPolicy.classify(e, conex != null ? safeResponseCode(conex) : -1),
					conex != null ? safeResponseCode(conex) : -1,
					e.getMessage());
			android.util.Log.e("SecurityLog", "Excepción capturada de forma segura", e);
			return null;
		}
		finally {
			if (conex != null) conex.disconnect();
			if (entrada != null) {
				try {
					entrada.close();
				} catch (Exception ignored) { }
			}
		}

	}
	
	
	@Override
	protected void onPostExecute(ArrayList<NoticiaRSS> result) {
		super.onPostExecute(result);
		
		if (pd!=null) pd.dismiss();
		if (objetoReceptor != null) {
			if (result == null && lastError != null) {
				objetoReceptor.onError(lastError);
			}
			objetoReceptor.onRecibeNoticiasRSS(result);
		}
	}


	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		if (pd != null && values != null && values.length > 0) {
			pd.setMessage(contexto.getString(R.string.downloading_news_count, values[0]));
		}
	}

	private int safeResponseCode(HttpURLConnection connection) {
		try {
			return connection.getResponseCode();
		} catch (IOException ignored) {
			return -1;
		}
	}
}
