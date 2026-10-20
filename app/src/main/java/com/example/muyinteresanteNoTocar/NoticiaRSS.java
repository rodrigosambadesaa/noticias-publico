package com.example.muyinteresanteNoTocar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import android.text.Html;


public class NoticiaRSS {
	/* Datos del item noticia RSS */
	private String titulo="";
	private String descripcion="";
	private String descripcionLimpia = "";
	private String fechaFormateada = "";
	private String enlace="";
	private String strFecha="";
	private String urlImagen="";
	
	private Date fecha; // == strFecha

	// Constantes
	public static final String RSS_MUY_INTERESANTE="Muy interesante"; 				// Formato fecha item RSS estandard

	
	
    private Date convierteFechaRSSaDate() throws ParseException {
        ParseException ultimaExcepcion = null;
        // Instancias locales: SimpleDateFormat NO es seguro entre hilos.
        String[] formatos = {
            "EEE,dd MMM yyyy HH:mm:ss",
            "EEE, dd MMM yyyy HH:mm:ss",
            "EEE, dd MMM yyyy HH:mm:ss zzz"
        };
        for (String formato : formatos) {
            try {
                return new SimpleDateFormat(formato, Locale.US).parse(strFecha);
            } catch (ParseException e) {
                ultimaExcepcion = e;
            }
        }
        throw ultimaExcepcion != null ? ultimaExcepcion : new ParseException(strFecha, 0);
    }
	
	
	public NoticiaRSS() {}

	public NoticiaRSS(String titulo, String descripcion, String enlace, String urlImagen, Date fecha) {
		this.titulo = HtmlEntityDecoder.decode(titulo);
		this.descripcion = descripcion != null ? descripcion : "";
		this.descripcionLimpia = limpiarDescripcion(this.descripcion);
		this.enlace = enlace != null ? enlace : "";
		this.urlImagen = urlImagen != null ? urlImagen : "";
		this.fecha = fecha;
		this.fechaFormateada = formatearFecha(fecha);
		if (fecha != null) {
			Calendar c = GregorianCalendar.getInstance();
			c.setTime(fecha);
			this.strFecha = NoticiaRSS.creaStrPubDate(c);
		}
	}

    /** Constructor para resultados del parser en streaming, sin dependencias DOM. */
    public static NoticiaRSS fromRssFields(String title, String description, String link,
                                          String rawDate, String imageUrl) {
        Date parsed = null;
        if (rawDate != null && !rawDate.trim().isEmpty()) {
            String[] patterns = {
                "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss zzz",
                "EEE,dd MMM yyyy HH:mm:ss", "EEE, dd MMM yyyy HH:mm:ss"
            };
            for (String pattern : patterns) {
                try {
                    SimpleDateFormat fmt = new SimpleDateFormat(pattern, Locale.US);
                    fmt.setLenient(false);
                    parsed = fmt.parse(rawDate.trim());
                    break;
                } catch (ParseException ignored) { }
            }
        }
        String url = imageUrl != null ? imageUrl.trim() : "";
        if (url.isEmpty() && description != null) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                    "(?is)<img\\b[^>]*?\\bsrc\\s*=\\s*['\\\"]([^'\\\"]+)['\\\"]")
                    .matcher(description);
            if (matcher.find()) url = matcher.group(1);
        }
        NoticiaRSS noticia = new NoticiaRSS(title, description, link, url, parsed);
        if (rawDate != null && !rawDate.isEmpty()) noticia.strFecha = rawDate;
        return noticia;
    }

    public Date getFechaNoticia() {
		return fecha;
	}

	public String getStrFecha() {
		return strFecha;
	}
	
    public void setFechaNoticia(Date fechaNoticia) {
        fecha = fechaNoticia;
        fechaFormateada = formatearFecha(fechaNoticia);
        if (fechaNoticia != null) {
            Calendar c = GregorianCalendar.getInstance();
            c.setTime(fechaNoticia);
            strFecha = NoticiaRSS.creaStrPubDate(c);
        } else {
            strFecha = "";
        }
    }


	public String getTitulo() {
		return titulo;
	}
	
	public void setTitulo(String titulo) {
		this.titulo = HtmlEntityDecoder.decode(titulo);
	}
	
	public String getDescripcion() {
		return descripcion;
	}
	
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion != null ? descripcion : "";
		this.descripcionLimpia = limpiarDescripcion(this.descripcion);
	}
	
	public String getEnlace() {
		return enlace;
	}
	
	
	public void setEnlace(String enlace) {
		this.enlace = enlace;
	}
		
	public String getUrlImagen() {
		return urlImagen;
	}


	public void setUrlImagen(String urlImagen) {
		this.urlImagen = urlImagen;
	}

	/* Algunos canales RSS, introducen etiquetas HTML en la descripci�n de la noticia */
    public String getDescripcionSinHTML() {
        return descripcionLimpia;
    }

    public String getDescripcionLimpia() {
        return descripcionLimpia;
    }

    private static String limpiarDescripcion(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        return HtmlEntityDecoder.decode(Html.fromHtml(
                texto.replaceAll("<[^>]*>", "").replace("\t", ""))
                .toString()).trim();
    }

    private static String formatearFecha(Date fecha) {
        if (fecha == null) {
            return "";
        }
        return new SimpleDateFormat("EEEE, dd 'de' MMMM 'del' yyyy, HH:mm",
                Locale.getDefault()).format(fecha);
    }


    public String getFechaComoStringFormateado() {
        return fechaFormateada;
    }
	
	public static String creaStrPubDate(Calendar fecha){
		// A partir de la fecha recibida se crea un String en formato fecha "pubDate" RSS
		Date d = fecha.getTime();

	    return new SimpleDateFormat("EEE,dd MMM yyyy HH:mm:ss", Locale.US).format(d);
	}
	

	
	@Override
	public String toString() {
		
		return "Titulo: " +this.getTitulo() + "\n" + "Enlace: " + this.getEnlace() + "\n" + "Fecha: " +this.getFechaComoStringFormateado() + "\n" +
				"Descripcion:" + this.getDescripcion() + "\n" + "UrlImagen:" + this.getUrlImagen();
	}

}
