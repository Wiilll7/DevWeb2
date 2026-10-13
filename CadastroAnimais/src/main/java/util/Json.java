package util;

import java.util.Collection;
import java.util.Map;

/** Gerador mínimo de JSON (Map, Collection, String, Number, Boolean, null). */
public final class Json {

    private Json() { }

    public static String escrever(Object o) {
        StringBuilder sb = new StringBuilder();
        escrever(o, sb);
        return sb.toString();
    }

    private static void escrever(Object o, StringBuilder sb) {
        if (o == null) {
            sb.append("null");
        } else if (o instanceof Number || o instanceof Boolean) {
            sb.append(o);
        } else if (o instanceof Map) {
            sb.append('{');
            boolean primeiro = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                if (!primeiro) sb.append(',');
                primeiro = false;
                texto(String.valueOf(e.getKey()), sb);
                sb.append(':');
                escrever(e.getValue(), sb);
            }
            sb.append('}');
        } else if (o instanceof Collection) {
            sb.append('[');
            boolean primeiro = true;
            for (Object x : (Collection<?>) o) {
                if (!primeiro) sb.append(',');
                primeiro = false;
                escrever(x, sb);
            }
            sb.append(']');
        } else {
            texto(o.toString(), sb);
        }
    }

    private static void texto(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '<': sb.append("\\u003c"); break;
                case '>': sb.append("\\u003e"); break;
                case '&': sb.append("\\u0026"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }
}
