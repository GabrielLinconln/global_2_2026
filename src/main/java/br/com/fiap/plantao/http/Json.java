package br.com.fiap.plantao.http;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Map;

// converte objetos do model, listas e mapas em JSON lendo os getters (sem biblioteca externa)
public final class Json {
    private Json() { }

    public static String of(Object o) {
        if (o == null) return "null";
        if (o instanceof Number || o instanceof Boolean) return o.toString();
        if (o instanceof String || o instanceof Enum || o instanceof Temporal) return texto(o.toString());
        StringBuilder sb = new StringBuilder();
        if (o instanceof Map) {
            sb.append('{');
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                if (sb.length() > 1) sb.append(',');
                sb.append(texto(String.valueOf(e.getKey()))).append(':').append(of(e.getValue()));
            }
            return sb.append('}').toString();
        }
        if (o instanceof Collection) {
            sb.append('[');
            for (Object item : (Collection<?>) o) {
                if (sb.length() > 1) sb.append(',');
                sb.append(of(item));
            }
            return sb.append(']').toString();
        }
        sb.append('{');
        for (Method m : o.getClass().getMethods()) {
            String n = m.getName();
            if (m.getParameterCount() != 0 || Modifier.isStatic(m.getModifiers()) || n.equals("getClass")) continue;
            String campo;
            if (n.startsWith("get") && n.length() > 3) campo = n.substring(3);
            else if (n.startsWith("is") && n.length() > 2) campo = n.substring(2);
            else continue;
            try {
                if (sb.length() > 1) sb.append(',');
                sb.append(texto(Character.toLowerCase(campo.charAt(0)) + campo.substring(1))).append(':').append(of(m.invoke(o)));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        return sb.append('}').toString();
    }

    // escapa aspas, barra e caracteres de controle (evita JSON quebrado e injecao)
    static String texto(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20 || c == '<' || c == '>') sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
