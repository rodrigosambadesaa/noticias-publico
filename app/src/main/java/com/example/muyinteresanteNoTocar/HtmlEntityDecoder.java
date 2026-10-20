package com.example.muyinteresanteNoTocar;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decodes HTML character references in plain RSS text, without parsing markup.
 * XmlPullParser already decodes XML entities; some publishers embed a second
 * layer of HTML entities inside title/description text or CDATA sections.
 * Only two passes are allowed (ordinary and double-escaped input).
 */
public final class HtmlEntityDecoder {
    private static final Pattern REFERENCE = Pattern.compile(
            "&(#(?:[xX][0-9a-fA-F]{1,8}|[0-9]{1,10})|[A-Za-z][A-Za-z0-9]{1,31});");
    private static final Map<String, String> NAMED = createNamedEntities();

    private HtmlEntityDecoder() { }

    public static String decode(String value) {
        if (value == null || value.isEmpty() || value.indexOf('&') < 0) {
            return value == null ? "" : value;
        }
        String result = value;
        for (int pass = 0; pass < 2; pass++) {
            Matcher matcher = REFERENCE.matcher(result);
            StringBuffer output = new StringBuffer();
            boolean changed = false;
            while (matcher.find()) {
                String replacement = resolve(matcher.group(1));
                if (replacement == null) {
                    continue; // Unknown/malformed references remain readable.
                }
                matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
                changed = true;
            }
            if (!changed) {
                break;
            }
            matcher.appendTail(output);
            result = output.toString();
        }
        return result;
    }

    private static String resolve(String name) {
        if (name.charAt(0) != '#') {
            return NAMED.get(name);
        }
        try {
            boolean hex = name.length() > 2
                    && (name.charAt(1) == 'x' || name.charAt(1) == 'X');
            long code = Long.parseLong(name.substring(hex ? 2 : 1), hex ? 16 : 10);
            // HTML numeric references in the Windows-1252 compatibility range.
            if (code == 0x80) code = 0x20AC;
            else if (code == 0x82) code = 0x201A;
            else if (code == 0x83) code = 0x0192;
            else if (code == 0x84) code = 0x201E;
            else if (code == 0x85) code = 0x2026;
            else if (code == 0x86) code = 0x2020;
            else if (code == 0x87) code = 0x2021;
            else if (code == 0x88) code = 0x02C6;
            else if (code == 0x89) code = 0x2030;
            else if (code == 0x8A) code = 0x0160;
            else if (code == 0x8B) code = 0x2039;
            else if (code == 0x8C) code = 0x0152;
            else if (code == 0x8E) code = 0x017D;
            else if (code == 0x91) code = 0x2018;
            else if (code == 0x92) code = 0x2019;
            else if (code == 0x93) code = 0x201C;
            else if (code == 0x94) code = 0x201D;
            else if (code == 0x95) code = 0x2022;
            else if (code == 0x96) code = 0x2013;
            else if (code == 0x97) code = 0x2014;
            else if (code == 0x98) code = 0x02DC;
            else if (code == 0x99) code = 0x2122;
            else if (code == 0x9A) code = 0x0161;
            else if (code == 0x9B) code = 0x203A;
            else if (code == 0x9C) code = 0x0153;
            else if (code == 0x9E) code = 0x017E;
            else if (code == 0x9F) code = 0x0178;

            // Reject invalid code points and unsafe invisible control characters.
            if (code > 0x10FFFF || code < 0x20
                    && code != '\n' && code != '\r' && code != '\t'
                    || code >= 0xD800 && code <= 0xDFFF
                    || code >= 0x7F && code <= 0x9F) {
                return null;
            }
            return new String(Character.toChars((int) code));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Map<String, String> createNamedEntities() {
        Map<String, String> values = new HashMap<>();
        String[][] entries = {
            {"amp","&"},{"lt","<"},{"gt",">"},{"quot","\""},{"apos","'"},
            {"nbsp","\u00A0"},{"iexcl","¡"},{"iquest","¿"},{"laquo","«"},
            {"raquo","»"},{"copy","©"},{"reg","®"},{"trade","™"},
            {"euro","€"},{"pound","£"},{"yen","¥"},{"cent","¢"},
            {"sect","§"},{"para","¶"},{"middot","·"},{"bull","•"},
            {"hellip","…"},{"ndash","–"},{"mdash","—"},{"lsquo","‘"},
            {"rsquo","’"},{"ldquo","“"},{"rdquo","”"},{"sbquo","‚"},
            {"bdquo","„"},{"lsaquo","‹"},{"rsaquo","›"},{"times","×"},
            {"divide","÷"},{"deg","°"},{"plusmn","±"},{"micro","µ"},
            {"ordm","º"},{"ordf","ª"},{"sup1","¹"},{"sup2","²"},
            {"sup3","³"},{"frac12","½"},{"frac14","¼"},{"frac34","¾"},
            {"permil","‰"},{"dagger","†"},{"Dagger","‡"},{"prime","′"},
            {"Prime","″"},{"oline","‾"},{"frasl","⁄"},{"minus","−"},
            {"Aacute","Á"},{"Eacute","É"},{"Iacute","Í"},{"Oacute","Ó"},
            {"Uacute","Ú"},{"Yacute","Ý"},{"Ntilde","Ñ"},{"Ccedil","Ç"},
            {"aacute","á"},{"eacute","é"},{"iacute","í"},{"oacute","ó"},
            {"uacute","ú"},{"yacute","ý"},{"ntilde","ñ"},{"ccedil","ç"},
            {"Agrave","À"},{"Egrave","È"},{"Igrave","Ì"},{"Ograve","Ò"},
            {"Ugrave","Ù"},{"agrave","à"},{"egrave","è"},{"igrave","ì"},
            {"ograve","ò"},{"ugrave","ù"},{"Acirc","Â"},{"Ecirc","Ê"},
            {"Icirc","Î"},{"Ocirc","Ô"},{"Ucirc","Û"},{"acirc","â"},
            {"ecirc","ê"},{"icirc","î"},{"ocirc","ô"},{"ucirc","û"},
            {"Auml","Ä"},{"Euml","Ë"},{"Iuml","Ï"},{"Ouml","Ö"},
            {"Uuml","Ü"},{"Yuml","Ÿ"},{"auml","ä"},{"euml","ë"},
            {"iuml","ï"},{"ouml","ö"},{"uuml","ü"},{"yuml","ÿ"},
            {"Atilde","Ã"},{"Otilde","Õ"},{"atilde","ã"},{"otilde","õ"},
            {"Aring","Å"},{"aring","å"},{"AElig","Æ"},{"aelig","æ"},
            {"Oslash","Ø"},{"oslash","ø"},{"szlig","ß"},{"ETH","Ð"},
            {"eth","ð"},{"THORN","Þ"},{"thorn","þ"},{"scaron","š"},
            {"Scaron","Š"},{"OElig","Œ"},{"oelig","œ"},{"fnof","ƒ"},
            {"larr","←"},{"rarr","→"},{"uarr","↑"},{"darr","↓"},
            {"harr","↔"},{"lArr","⇐"},{"rArr","⇒"},{"hArr","⇔"},
            {"hearts","♥"},{"clubs","♣"},{"spades","♠"},{"diams","♦"},
            {"alpha","α"},{"beta","β"},{"gamma","γ"},{"pi","π"},
            {"Delta","Δ"},{"Omega","Ω"},{"infin","∞"},{"ne","≠"},
            {"le","≤"},{"ge","≥"},{"radic","√"},{"copy","©"}
        };
        for (String[] entry : entries) {
            values.put(entry[0], entry[1]);
        }
        return Collections.unmodifiableMap(values);
    }
}
