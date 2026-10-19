package com.example.muyinteresanteNoTocar;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Scanner;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

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
		this.titulo = titulo != null ? titulo : "";
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

    /* Crea una noticia a partir del itemXML y el nombre del canal */
	public NoticiaRSS(Element itemXML, String canal) throws ParserConfigurationException, SAXException, IOException, ParseException{
		titulo=itemXML.getElementsByTagName("title").item(0).getFirstChild().getNodeValue().trim();
		descripcion=itemXML.getElementsByTagName("description").item(0).getFirstChild().getNodeValue().trim();
		enlace=itemXML.getElementsByTagName("link").item(0).getFirstChild().getNodeValue().trim();
		strFecha=itemXML.getElementsByTagName("pubDate").item(0).getFirstChild().getNodeValue().trim();
		
		fecha = convierteFechaRSSaDate();
		fechaFormateada = formatearFecha(fecha);
		
		if (canal.equalsIgnoreCase(RSS_MUY_INTERESANTE)){
		  try {  
			  // 1. Intentar desde enclosure / media:content / media:thumbnail
			  if (itemXML.getElementsByTagName("enclosure").getLength() > 0) {
				  Element enclosure = (Element) itemXML.getElementsByTagName("enclosure").item(0);
				  if (enclosure.hasAttribute("url")) {
					  urlImagen = enclosure.getAttribute("url");
				  }
			  }
			  if ((urlImagen == null || urlImagen.isEmpty()) && itemXML.getElementsByTagName("media:content").getLength() > 0) {
				  Element media = (Element) itemXML.getElementsByTagName("media:content").item(0);
				  if (media.hasAttribute("url")) {
					  urlImagen = media.getAttribute("url");
				  }
			  }
			  if ((urlImagen == null || urlImagen.isEmpty()) && itemXML.getElementsByTagName("media:thumbnail").getLength() > 0) {
				  Element thumb = (Element) itemXML.getElementsByTagName("media:thumbnail").item(0);
				  if (thumb.hasAttribute("url")) {
					  urlImagen = thumb.getAttribute("url");
				  }
			  }

			  // 2. Extraer de la descripción si contiene etiquetas de imagen
			  if (urlImagen == null || urlImagen.isEmpty()) {
				  Scanner s = new Scanner(descripcion);
				  s.useDelimiter("\\s*>\\s*");
				  String imagen = "";
				  if (s.hasNext()) {
					 imagen = s.next();
				  }
				  s.close();
				  if (imagen.length() > 0 && (imagen.contains("http://") || imagen.contains("https://"))) {
					  Scanner s2 = new Scanner(imagen);
					  s2.useDelimiter("\\s*'|\"\\s*");
					  while (s2.hasNext()) {
						  String str = s2.next();
						  if (str.contains("http://") || str.contains("https://")) {
							  urlImagen = str;
							  break;
						  }
					  }
					  s2.close();
				  }
			  }

              // 3. La descripción se normaliza una sola vez tras extraer la imagen.
              // Si el feed no facilita una imagen real, Glide utilizará el placeholder.

          } catch (Exception e) { 
			  android.util.Log.e("SecurityLog", "Excepción capturada de forma segura", e);
		  }
		}

        descripcionLimpia = limpiarDescripcion(descripcion);
        if (RSS_MUY_INTERESANTE.equalsIgnoreCase(canal)) {
            // Conserva el contrato anterior para las búsquedas y la caché JSON.
            descripcion = descripcionLimpia;
        }
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
		this.titulo = titulo;
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
        return Html.fromHtml(texto.replaceAll("<[^>]*>", "").replace("\t", ""))
                .toString().trim();
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
