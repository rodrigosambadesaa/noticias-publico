package com.example.muyinteresanteNoTocar;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.InputStream;
import java.io.IOException;

/**
 * Procesamiento incremental RSS: nunca materializa un Document/DOM.
 * Se rechazan declaraciones DOCTYPE para evitar XXE y expansión de entidades.
 */
public final class RssPullParser {
    private RssPullParser() { }

    public interface ItemConsumer {
        void onItem(RssItem item);
    }

    public static final class RssItem {
        public final String title, description, link, pubDate, image;
        RssItem(String title, String description, String link, String pubDate, String image) {
            this.title = title;
            this.description = description;
            this.link = link;
            this.pubDate = pubDate;
            this.image = image;
        }
    }

    public static void parse(InputStream input, ItemConsumer consumer)
            throws IOException, XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parse(parser, input, consumer);
    }

    /** Sobrecarga con parser inyectado: permite pruebas JVM sin entorno Android. */
    public static void parse(XmlPullParser parser, InputStream input, ItemConsumer consumer)
            throws IOException, XmlPullParserException {
        parser.setInput(input, null); // Respeta la codificación declarada en el XML.
        int itemDepth = -1;
        int fieldDepth = -1;
        String field = null;
        StringBuilder value = new StringBuilder();
        String title = "", description = "", link = "", pubDate = "", image = "";
        int imagePriority = 0;

        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.DOCDECL) {
                throw new XmlPullParserException("DOCTYPE no permitido en RSS");
            }
            if (event == XmlPullParser.START_TAG) {
                String name = parser.getName();
                // Con o sin procesamiento de namespaces, usar el nombre local.
                if (name != null && name.indexOf(':') >= 0) {
                    name = name.substring(name.lastIndexOf(':') + 1);
                }
                int depth = parser.getDepth();
                if (itemDepth == -1 && "item".equalsIgnoreCase(name)) {
                    itemDepth = depth;
                    title = description = link = pubDate = image = "";
                    imagePriority = 0;
                } else if (itemDepth != -1 && depth == itemDepth + 1) {
                    if ("enclosure".equalsIgnoreCase(name)) {
                        String type = parser.getAttributeValue(null, "type");
                        String url = parser.getAttributeValue(null, "url");
                        if (url != null && !url.isEmpty()
                                && (type == null || type.startsWith("image/"))) {
                            image = url; imagePriority = 3;
                        }
                    } else if ("content".equalsIgnoreCase(name) || "thumbnail".equalsIgnoreCase(name)) {
                        String url = parser.getAttributeValue(null, "url");
                        int priority = "content".equalsIgnoreCase(name) ? 2 : 1;
                        String type = parser.getAttributeValue(null, "type");
                        if (priority > imagePriority && url != null && !url.isEmpty()
                                && (type == null || type.startsWith("image/"))) {
                            image = url; imagePriority = priority;
                        }
                    } else if ("title".equalsIgnoreCase(name)
                            || "description".equalsIgnoreCase(name)
                            || "link".equalsIgnoreCase(name)
                            || "pubDate".equalsIgnoreCase(name)) {
                        field = name.toLowerCase(java.util.Locale.ROOT);
                        fieldDepth = depth;
                        value.setLength(0);
                    }
                }
            } else if ((event == XmlPullParser.TEXT || event == XmlPullParser.CDSECT
                    || event == XmlPullParser.ENTITY_REF) && field != null) {
                if (parser.getText() != null) value.append(parser.getText());
            } else if (event == XmlPullParser.END_TAG && itemDepth != -1) {
                int depth = parser.getDepth();
                if (field != null && depth == fieldDepth) {
                    String content = value.toString().trim();
                    switch (field) {
                        case "title": title = content; break;
                        case "description": description = content; break;
                        case "link": link = content; break;
                        case "pubdate": pubDate = content; break;
                        default: break;
                    }
                    field = null;
                    fieldDepth = -1;
                } else if (depth == itemDepth && "item".equalsIgnoreCase(parser.getName())) {
                    if (!title.isEmpty() && !link.isEmpty()) {
                        consumer.onItem(new RssItem(title, description, link, pubDate, image));
                    }
                    itemDepth = -1;
                }
            }
            event = parser.nextToken();
        }
    }
}
