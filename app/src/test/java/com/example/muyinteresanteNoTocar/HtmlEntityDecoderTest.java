package com.example.muyinteresanteNoTocar;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HtmlEntityDecoderTest {
    @Test public void decodesTheReportedMarcaHeadline() {
        NoticiaRSS noticia = new NoticiaRSS();
        noticia.setTitulo("El &#039;pecho de paloma&#039; vuelve a ponerse de moda");
        assertEquals("El 'pecho de paloma' vuelve a ponerse de moda", noticia.getTitulo());
    }

    @Test public void decodesNamedSpanishEntitiesAndTypographicQuotes() {
        assertEquals("«España»: ‘última’ noticia — 20 €",
                HtmlEntityDecoder.decode("&laquo;Espa&ntilde;a&raquo;: &lsquo;&uacute;ltima&rsquo; noticia &mdash; 20 &euro;"));
    }

    @Test public void decodesHexadecimalAndSupplementaryUnicode() {
        assertEquals("Corazón ❤️ 😀", HtmlEntityDecoder.decode(
                "Coraz&#xF3;n &#x2764;&#xFE0F; &#x1F600;"));
    }

    @Test public void decodesDoubleEscapedHtmlWithoutLosingAmpersands() {
        assertEquals("El 'pecho' & más",
                HtmlEntityDecoder.decode("El &amp;#039;pecho&amp;#039; &amp;amp; m&aacute;s"));
    }

    @Test public void preservesUnknownReferencesAndRawText() {
        assertEquals("A & B &unknown; &#x110000; &#xD800; &#0;",
                HtmlEntityDecoder.decode("A & B &unknown; &#x110000; &#xD800; &#0;"));
        assertEquals("<script>alert('x')</script>",
                HtmlEntityDecoder.decode("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;"));
    }

    @Test public void handlesNullAndOrdinaryAccents() {
        assertEquals("", HtmlEntityDecoder.decode(null));
        assertEquals("mañana a las 10:00", HtmlEntityDecoder.decode("mañana a las 10:00"));
    }
}
