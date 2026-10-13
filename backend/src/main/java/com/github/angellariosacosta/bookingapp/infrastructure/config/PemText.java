package com.github.angellariosacosta.bookingapp.infrastructure.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normaliza el texto PEM de una llave a su forma canonica (encabezado, base64 en lineas de 64, pie),
 * sin importar como llego: saltos reales (Parameter Store), "\n" literales (.env, plugins del IDE),
 * "\r\n" de Windows, comillas alrededor o todo en una sola linea separado por espacios.
 */
final class PemText {

    private static final Pattern BEGIN_MARKER = Pattern.compile("-----BEGIN ([A-Z0-9 ]+)-----");
    private static final Pattern ANY_MARKER = Pattern.compile("-----(BEGIN|END) [A-Z0-9 ]+-----");
    private static final Pattern BASE64_BODY = Pattern.compile("[A-Za-z0-9+/]+={0,2}");
    private static final String ESCAPED_NEWLINE = "\\n";
    private static final String ESCAPED_CARRIAGE_RETURN = "\\r";
    private static final int LINE_LENGTH = 64;

    private PemText() {
    }

    static String canonical(String rawPem, String defaultLabel) {
        String unescaped = unquote(rawPem.strip())
                .replace(ESCAPED_CARRIAGE_RETURN, "")
                .replace(ESCAPED_NEWLINE, "\n");
        String label = labelOf(unescaped, defaultLabel);
        String body = ANY_MARKER.matcher(unescaped).replaceAll("").replaceAll("\\s+", "");
        if (!BASE64_BODY.matcher(body).matches()) {
            throw new IllegalStateException(
                    "El texto PEM de la llave JWT (" + label + ") esta vacio o no contiene base64 valido");
        }
        return "-----BEGIN " + label + "-----\n" + wrap(body) + "-----END " + label + "-----\n";
    }

    private static String unquote(String value) {
        boolean quoted = value.length() >= 2
                && (value.startsWith("\"") && value.endsWith("\"") || value.startsWith("'") && value.endsWith("'"));
        return quoted ? value.substring(1, value.length() - 1).strip() : value;
    }

    private static String labelOf(String pem, String defaultLabel) {
        Matcher begin = BEGIN_MARKER.matcher(pem);
        return begin.find() ? begin.group(1) : defaultLabel;
    }

    private static String wrap(String body) {
        StringBuilder wrapped = new StringBuilder(body.length() + body.length() / LINE_LENGTH + 1);
        for (int start = 0; start < body.length(); start += LINE_LENGTH) {
            wrapped.append(body, start, Math.min(start + LINE_LENGTH, body.length())).append('\n');
        }
        return wrapped.toString();
    }
}
