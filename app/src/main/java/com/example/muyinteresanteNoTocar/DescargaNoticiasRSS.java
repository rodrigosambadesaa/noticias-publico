package com.example.muyinteresanteNoTocar;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import com.example.muyinteresante.util.RemoteOperationPolicy;

/* Parsea un canal RSS y devuelve sus items en un ArrayList */

public class DescargaNoticiasRSS extends AsyncTask<String,Integer,ArrayList<NoticiaRSS>>{

	private Context contexto=null;
	private iNoticiaRSS objetoReceptor=null;
	private ProgressDialog pd=null;
	private boolean mostrarProgreso=true;
	private DownloadError lastError;
	
	private static final String MENSAJE_PD="Descargando noticias...";

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
			pd.setMessage(MENSAJE_PD);
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
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setIgnoringComments(true);
			dbf.setCoalescing(true);
			DocumentBuilder db = dbf.newDocumentBuilder(); 
			
			 // Creamos objeto URL a partir de la direccion web para conectarnos con el servidor
			URL url = new URL(params[0]);
			conex = (HttpURLConnection) url.openConnection(); // La petición real es la prueba del feed.
			conex.setConnectTimeout(10000);
			conex.setReadTimeout(10000);
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

			Document arbolXML =db.parse(entrada);
			entrada.close();
			Element raiz = arbolXML.getDocumentElement(); 
			raiz.normalize(); 
			
			ArrayList<NoticiaRSS> noticias = new ArrayList<NoticiaRSS>();
			
			NodeList listaItems = raiz.getElementsByTagName("item");
			
			for (int i=0;i<listaItems.getLength();i++){
				try {
					Element item = (Element)listaItems.item(i);
					noticias.add(new NoticiaRSS(item, params[1]));
					
					publishProgress(noticias.size());
				}
				catch(Exception e){ e.printStackTrace();}
			}
			
			return noticias;
		}
		catch (Exception e){
			lastError = new DownloadError(
					RemoteOperationPolicy.classify(e, conex != null ? safeResponseCode(conex) : -1),
					conex != null ? safeResponseCode(conex) : -1,
					e.getMessage());
			e.printStackTrace();
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
			pd.setMessage(MENSAJE_PD + " " + values[0]);
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
