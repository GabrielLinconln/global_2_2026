package br.com.plantaoclima.http;

import br.com.plantaoclima.exception.ValidacaoException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilitario leve para serializacao e leitura de JSON em Java puro (sem bibliotecas externas).
 */
public final class JsonUtil {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private JsonUtil() {
    }

    public static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) {
            return quote(s);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Enum<?> e) {
            return quote(e.name());
        }
        if (value instanceof LocalDateTime dt) {
            return quote(dt.format(ISO_FORMATTER));
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(quote(String.valueOf(entry.getKey()))).append(':').append(toJson(entry.getValue()));
            }
            sb.append('}');
            return sb.toString();
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(toJson(item));
            }
            sb.append(']');
            return sb.toString();
        }
        return quote(value.toString());
    }

    public static String quote(String text) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        Parser parser = new Parser(json);
        Object parsed = parser.parseValue();
        parser.skipWhitespace();
        if (!(parsed instanceof Map<?, ?>)) {
            throw new ValidacaoException("Corpo da requisicao deve ser um objeto JSON valido.");
        }
        return (Map<String, Object>) parsed;
    }

    public static String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val == null ? null : String.valueOf(val);
    }

    public static Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null || String.valueOf(val).trim().isEmpty()) {
            return null;
        }
        if (val instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(val).trim());
        } catch (NumberFormatException e) {
            throw new ValidacaoException("Campo numerico invalido: " + key);
        }
    }

    public static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Long value = getLong(map, key);
        return value != null ? value.intValue() : defaultValue;
    }

    public static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object val = map.get(key);
        if (val == null) {
            return defaultValue;
        }
        if (val instanceof Boolean b) {
            return b;
        }
        if (val instanceof Number n) {
            return n.intValue() != 0;
        }
        String s = String.valueOf(val).trim().toLowerCase();
        return "true".equals(s) || "1".equals(s) || "sim".equals(s);
    }

    private static final class Parser {
        private final String input;
        private int pos;

        Parser(String input) {
            this.input = input;
        }

        void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= input.length()) {
                throw new ValidacaoException("JSON incompleto.");
            }
            char c = input.charAt(pos);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // '{'
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (pos < input.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (pos >= input.length() || input.charAt(pos) != ':') {
                    throw new ValidacaoException("Esperado ':' no JSON.");
                }
                pos++;
                Object val = parseValue();
                map.put(key, val);
                skipWhitespace();
                if (pos < input.length() && input.charAt(pos) == ',') {
                    pos++;
                } else if (pos < input.length() && input.charAt(pos) == '}') {
                    pos++;
                    return map;
                } else {
                    throw new ValidacaoException("Objeto JSON malformado.");
                }
            }
            throw new ValidacaoException("Objeto JSON nao fechado.");
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // '['
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (pos < input.length()) {
                list.add(parseValue());
                skipWhitespace();
                if (pos < input.length() && input.charAt(pos) == ',') {
                    pos++;
                } else if (pos < input.length() && input.charAt(pos) == ']') {
                    pos++;
                    return list;
                } else {
                    throw new ValidacaoException("Array JSON malformado.");
                }
            }
            throw new ValidacaoException("Array JSON nao fechado.");
        }

        private String parseString() {
            if (input.charAt(pos) != '"') {
                throw new ValidacaoException("String JSON esperada.");
            }
            pos++;
            StringBuilder sb = new StringBuilder();
            while (pos < input.length()) {
                char c = input.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (pos >= input.length()) {
                        throw new ValidacaoException("Escape invalido no JSON.");
                    }
                    char esc = input.charAt(pos++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            String hex = input.substring(pos, pos + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                        }
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new ValidacaoException("String JSON nao fechada.");
        }

        private Boolean parseBoolean() {
            if (input.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (input.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new ValidacaoException("Valor booleano invalido no JSON.");
        }

        private Object parseNull() {
            if (input.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new ValidacaoException("Valor nulo invalido no JSON.");
        }

        private Number parseNumber() {
            int start = pos;
            if (pos < input.length() && input.charAt(pos) == '-') {
                pos++;
            }
            while (pos < input.length() && (Character.isDigit(input.charAt(pos))
                    || input.charAt(pos) == '.' || input.charAt(pos) == 'e'
                    || input.charAt(pos) == 'E' || input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
                if ((input.charAt(pos) == '+' || input.charAt(pos) == '-') && pos == start) {
                    pos++;
                    continue;
                }
                pos++;
            }
            String numStr = input.substring(start, pos);
            try {
                if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                    return Double.parseDouble(numStr);
                }
                return Long.parseLong(numStr);
            } catch (NumberFormatException e) {
                throw new ValidacaoException("Numero invalido no JSON: " + numStr);
            }
        }
    }
}
