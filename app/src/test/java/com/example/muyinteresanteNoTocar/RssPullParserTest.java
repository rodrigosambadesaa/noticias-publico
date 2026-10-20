package com.example.muyinteresanteNoTocar;

import org.junit.Test;
import org.kxml2.io.KXmlParser;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import static org.junit.Assert.*;

public class RssPullParserTest {
    private ArrayList<RssPullParser.RssItem> parse(String xml) throws Exception {
        ArrayList<RssPullParser.RssItem> items = new ArrayList<>();
        RssPullParser.parse(new KXmlParser(),
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), items::add);
        return items;
    }

    @Test public void parsesMultipleItemsAndImagesIncrementally() throws Exception {
        String xml = "<?xml version='1.0'?><rss xmlns:media='http://search.yahoo.com/mrss/'>"
                + "<channel><title>Portada</title>"
                + "<item><title>Noticia A</title><link>https://example.org/a</link>"
                + "<description><![CDATA[Hola <b>mundo</b>]]></description>"
                + "<pubDate>Mon, 19 Oct 2026 12:00:00 +0200</pubDate>"
                + "<media:thumbnail url='https://img.example.org/a.jpg'/></item>"
                + "<item><title>Noticia B</title><link>https://example.org/b</link>"
                + "<enclosure url='https://img.example.org/b.jpg' type='image/jpeg'/>"
                + "</item></channel></rss>";
        ArrayList<RssPullParser.RssItem> result = parse(xml);
        assertEquals(2, result.size());
        assertEquals("Noticia A", result.get(0).title);
        assertEquals("Hola <b>mundo</b>", result.get(0).description);
        assertEquals("https://img.example.org/a.jpg", result.get(0).image);
        assertEquals("https://img.example.org/b.jpg", result.get(1).image);
    }

    @Test public void skipsIncompleteItems() throws Exception {
        ArrayList<RssPullParser.RssItem> result = parse(
                "<rss><channel><item><title>Sin link</title></item>"
                        + "<item><title>Completa</title><link>https://a</link></item>"
                        + "</channel></rss>");
        assertEquals(1, result.size());
    }

    @Test(expected = org.xmlpull.v1.XmlPullParserException.class)
    public void rejectsDoctype() throws Exception {
        parse("<!DOCTYPE rss [<!ENTITY x 'attack'>]><rss><channel>"
                + "<item><title>&x;</title><link>https://a</link></item>"
                + "</channel></rss>");
    }
}
