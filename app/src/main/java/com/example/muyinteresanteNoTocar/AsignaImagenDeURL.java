package com.example.muyinteresanteNoTocar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.example.muyinteresante.util.ConnectivityAndInternetAccess;

public class AsignaImagenDeURL extends AsyncTask<String,Void,Void> {
	private static final String TAG = "AsignaImagenDeURL";
	private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

	ImageView img;
	Bitmap mapaDeBits;
	File f;
	Context contexto;
	private String currentUrl;

	public AsignaImagenDeURL(ImageView img, Context c){
		this.img = img;
		contexto = c;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mapaDeBits = null;
		f = null;
		if (contexto != null) {
			ConnectivityAndInternetAccess.beginConnectionAttempt(contexto);
		}
	}

	@Override
	protected Void doInBackground(String... param) {
		if (param == null || param.length == 0 || param[0] == null || param[0].trim().isEmpty()) return null;
		currentUrl = param[0].trim();
		try {
			f = Utilidades.getDiretorioCache(contexto);
			if (f != null) {
				f = new File(f, "imagenes");
				if (!f.exists()) f.mkdirs();
				f = new File(f, String.valueOf(currentUrl.hashCode()));

				if (f.exists() && f.length() > 0) {
					mapaDeBits = BitmapFactory.decodeStream(new FileInputStream(f));
					if (mapaDeBits != null) return null;
				}

				if (contexto != null && !ConnectivityAndInternetAccess.isConnectedOrConnecting(contexto)) {
					return null;
				}

				String finalImageUrl = currentUrl;
				boolean isWebPage = currentUrl.contains(".html") || currentUrl.contains(".php") || !isDirectImageUrl(currentUrl);

				if (isWebPage) {
					String extracted = fetchOgImageFromArticleUrl(currentUrl);
					if (extracted != null && !extracted.trim().isEmpty()) {
						finalImageUrl = extracted.trim();
					}
				}

				mapaDeBits = downloadBitmapWithRedirects(finalImageUrl);

				if (mapaDeBits != null && f != null) {
					try {
						FileOutputStream fos = new FileOutputStream(f);
						mapaDeBits.compress(CompressFormat.JPEG, 90, fos);
						fos.flush();
						fos.close();
					} catch (Exception e) {
						e.printStackTrace();
						try { f.delete(); } catch(Exception ex){}
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Error cargando imagen de URL: " + currentUrl, e);
			mapaDeBits = null;
		}
		return null;
	}

	private boolean isDirectImageUrl(String url) {
		if (url == null) return false;
		String lower = url.toLowerCase();
		return lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".png") || lower.contains(".webp") || lower.contains(".gif");
	}

	private String fetchOgImageFromArticleUrl(String articleUrl) {
		try {
			HttpURLConnection conn = openConnectionWithUserAgent(articleUrl);
			int status = conn.getResponseCode();
			if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
				String newUrl = conn.getHeaderField("Location");
				if (newUrl != null && !newUrl.isEmpty()) {
					conn.disconnect();
					conn = openConnectionWithUserAgent(newUrl);
				}
			}

			InputStream in = conn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			StringBuilder sb = new StringBuilder();
			String line;
			int lines = 0;
			while ((line = reader.readLine()) != null && lines < 400) {
				sb.append(line).append("\n");
				lines++;
			}
			reader.close();
			conn.disconnect();

			String html = sb.toString();

			// Regex 1: <meta property="og:image" content="..." />
			Pattern patternOg = Pattern.compile("<meta\\s+property=[\"']og:image[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
			Matcher matcherOg = patternOg.matcher(html);
			if (matcherOg.find()) {
				return matcherOg.group(1);
			}

			// Regex 2: <meta content="..." property="og:image" />
			Pattern patternOg2 = Pattern.compile("<meta\\s+content=[\"']([^\"']+)[\"']\\s+property=[\"']og:image[\"']", Pattern.CASE_INSENSITIVE);
			Matcher matcherOg2 = patternOg2.matcher(html);
			if (matcherOg2.find()) {
				return matcherOg2.group(1);
			}

			// Regex 3: <meta name="twitter:image" content="..." />
			Pattern patternTwitter = Pattern.compile("<meta\\s+name=[\"']twitter:image[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
			Matcher matcherTwitter = patternTwitter.matcher(html);
			if (matcherTwitter.find()) {
				return matcherTwitter.group(1);
			}
		} catch (Exception e) {
			Log.w(TAG, "No se pudo extraer og:image de: " + articleUrl, e);
		}
		return null;
	}

	private Bitmap downloadBitmapWithRedirects(String imageUrl) {
		HttpURLConnection conn = null;
		InputStream in = null;
		try {
			String target = imageUrl;
			int redirects = 0;
			while (redirects < 5) {
				conn = openConnectionWithUserAgent(target);
				int code = conn.getResponseCode();
				if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) {
					String loc = conn.getHeaderField("Location");
					conn.disconnect();
					if (loc != null && !loc.isEmpty()) {
						target = loc;
						redirects++;
						continue;
					}
				}
				if (code == HttpURLConnection.HTTP_OK) {
					in = conn.getInputStream();
					Bitmap bmp = BitmapFactory.decodeStream(in);
					in.close();
					conn.disconnect();
					return bmp;
				}
				break;
			}
		} catch (Exception e) {
			Log.e(TAG, "Error descargando bitmap de: " + imageUrl, e);
		} finally {
			if (in != null) try { in.close(); } catch (Exception ignored) {}
			if (conn != null) try { conn.disconnect(); } catch (Exception ignored) {}
		}
		return null;
	}

	private HttpURLConnection openConnectionWithUserAgent(String urlStr) throws Exception {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(8000);
		conn.setReadTimeout(8000);
		conn.setInstanceFollowRedirects(true);
		conn.setRequestProperty("User-Agent", USER_AGENT);
		conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
		return conn;
	}

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		ConnectivityAndInternetAccess.endConnectionAttempt();
		if (img != null) {
			Object tag = img.getTag();
			if (mapaDeBits != null && (tag == null || tag.equals(currentUrl))) {
				img.setImageBitmap(mapaDeBits);
			}
		}
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
		ConnectivityAndInternetAccess.endConnectionAttempt();
		if (f != null && f.exists()) {
			try { f.delete(); } catch(Exception ex){} 
		}
	}
}
