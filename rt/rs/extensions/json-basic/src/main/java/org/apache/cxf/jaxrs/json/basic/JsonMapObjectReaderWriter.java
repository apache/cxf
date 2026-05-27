/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxrs.json.basic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.*;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;



public class JsonMapObjectReaderWriter {
    private static final Set<Character> ESCAPED_CHARS;
    private static final char DQUOTE = '"';
    private static final char COMMA = ',';
    private static final char COLON = ':';
    private static final char OBJECT_START = '{';
    private static final char OBJECT_END = '}';
    private static final char ARRAY_START = '[';
    private static final char ARRAY_END = ']';
    private static final char ESCAPE = '\\';
    private static final String NULL_VALUE = "null";
    private boolean format;

    static {
        Set<Character> chars = new HashSet<>();
        chars.add('"');
        chars.add('\\');
        chars.add('/');
        chars.add('b');
        chars.add('f');
        chars.add('n');
        chars.add('r');
        chars.add('t');
        ESCAPED_CHARS = Collections.unmodifiableSet(chars);
    }

    public JsonMapObjectReaderWriter() {

    }
    public JsonMapObjectReaderWriter(boolean format) {
        this.format = format;
    }

    public String toJson(JsonMapObject obj) {
        return toJson(obj.asMap());
    }

    public String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        toJsonInternal(new StringBuilderOutput(sb), map);
        return sb.toString();
    }

    public String toJson(List<Object> list) {
        StringBuilder sb = new StringBuilder();
        toJsonInternal(new StringBuilderOutput(sb), list);
        return sb.toString();
    }

    public void toJson(JsonMapObject obj, OutputStream os) {
        toJson(obj.asMap(), os);
    }

    public void toJson(Map<String, Object> map, OutputStream os) {
        toJsonInternal(new StreamOutput(os), map);
    }

    protected void toJsonInternal(Output out, Map<String, Object> map) {
        out.append(OBJECT_START);
        for (Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Object> entry = it.next();
            out.append(DQUOTE).append(escapeJson(entry.getKey())).append(DQUOTE);
            out.append(COLON);
            toJsonInternal(out, entry.getValue(), it.hasNext());
        }
        out.append(OBJECT_END);
    }

    protected void toJsonInternal(Output out, Object[] array) {
        toJsonInternal(out, Arrays.asList(array));
    }

    protected void toJsonInternal(Output out, Collection<?> coll) {
        out.append(ARRAY_START);
        formatIfNeeded(out);
        for (Iterator<?> iter = coll.iterator(); iter.hasNext();) {
            toJsonInternal(out, iter.next(), iter.hasNext());
        }
        formatIfNeeded(out);
        out.append(ARRAY_END);
    }

    @SuppressWarnings("unchecked")
    protected void toJsonInternal(Output out, Object value, boolean hasNext) {
        if (value == null) {
            out.append(null);
        } else if (JsonMapObject.class.isAssignableFrom(value.getClass())) {
            out.append(toJson((JsonMapObject)value));
        } else if (value.getClass().isArray()) {
            toJsonInternal(out, (Object[])value);
        } else if (Collection.class.isAssignableFrom(value.getClass())) {
            toJsonInternal(out, (Collection<?>)value);
        } else if (Map.class.isAssignableFrom(value.getClass())) {
            toJsonInternal(out, (Map<String, Object>)value);
        } else {
            boolean quotesNeeded = checkQuotesNeeded(value);
            if (quotesNeeded) {
                out.append(DQUOTE);
            }
            String valueStr = value.toString();
            if (value instanceof String) {
                // If the value is a String, make sure to escape quotes
                valueStr = escapeJson(valueStr);
            }
            out.append(valueStr);
            if (quotesNeeded) {
                out.append(DQUOTE);
            }
        }
        if (hasNext) {
            out.append(COMMA);
            formatIfNeeded(out);
        }

    }

    private boolean checkQuotesNeeded(Object value) {
        Class<?> cls = value.getClass();
        return !(Number.class.isAssignableFrom(cls) || Boolean.class == cls
            || JsonObject.class.isAssignableFrom(cls));
    }
    protected void formatIfNeeded(Output out) {
        if (format) {
            out.append("\r\n ");
        }
    }
    public JsonMapObject fromJsonToJsonObject(InputStream is) throws IOException {
        return fromJsonToJsonObject(IOUtils.toString(is));
    }
    public JsonMapObject fromJsonToJsonObject(String json) {
        JsonMapObject obj = new JsonMapObject();
        fromJson(obj, json);
        return obj;
    }
    public void fromJson(JsonMapObject obj, String json) {
        String theJson = json.trim();
        JsonObjectSettable settable = new JsonObjectSettable(obj);
        readJsonObjectAsSettable(settable, theJson.substring(1, theJson.length() - 1));
    }
    public Map<String, Object> fromJson(InputStream is) throws IOException {
        return fromJson(IOUtils.toString(is));
    }
    public Map<String, Object> fromJson(String json) {
        String theJson = json.trim();
        MapSettable nextMap = new MapSettable();
        readJsonObjectAsSettable(nextMap, theJson.substring(1, theJson.length() - 1));
        return nextMap.map;
    }
    public List<Object> fromJsonAsList(String json) {
        return fromJsonAsList(null, json);
    }
    public List<Object> fromJsonAsList(String name, String json) {
        String theJson = json.trim();
        return internalFromJsonAsList(name, theJson.substring(1, theJson.length() - 1));
    }
    protected void readJsonObjectAsSettable(Settable values, String json) {
        for (int i = 0; i < json.length(); i++) {
            if (Character.isWhitespace(json.charAt(i))) {
                continue;
            }

            int closingQuote = json.charAt(i) == DQUOTE
                    ? findClosingQuote(json, i) : json.indexOf(DQUOTE, i + 1);
            int from = json.charAt(i) == DQUOTE ? i + 1 : i;
            String name = unescapeKeyName(json.substring(from, closingQuote));
            int sepIndex = json.indexOf(COLON, closingQuote + 1);
            if (sepIndex == -1) {
                throw new UncheckedIOException(new IOException("Error in parsing json"));
            }

            int j = 1;
            while (Character.isWhitespace(json.charAt(sepIndex + j))) {
                j++;
            }
            if (json.charAt(sepIndex + j) == OBJECT_START) {
                int closingIndex = getClosingIndex(json, OBJECT_START, OBJECT_END, sepIndex + j);
                String newJson = json.substring(sepIndex + j + 1, closingIndex);
                MapSettable nextMap = new MapSettable();
                readJsonObjectAsSettable(nextMap, newJson);
                values.put(name, nextMap.map);
                i = closingIndex + 1;
            } else if (json.charAt(sepIndex + j) == ARRAY_START) {
                int closingIndex = getClosingIndex(json, ARRAY_START, ARRAY_END, sepIndex + j);
                String newJson = json.substring(sepIndex + j + 1, closingIndex);
                values.put(name, internalFromJsonAsList(name, newJson));
                i = closingIndex + 1;
            } else {
                int commaIndex = getCommaIndex(json, sepIndex + j);
                Object value = readPrimitiveValue(name, json, sepIndex + j, commaIndex);
                values.put(name, value);
                i = commaIndex + 1;
            }

        }
    }
    protected List<Object> internalFromJsonAsList(String name, String json) {
        List<Object> values = new LinkedList<>();
        for (int i = 0; i < json.length(); i++) {
            if (Character.isWhitespace(json.charAt(i))) {
                continue;
            }
            if (json.charAt(i) == OBJECT_START) {
                int closingIndex = getClosingIndex(json, OBJECT_START, OBJECT_END, i);
                MapSettable nextMap = new MapSettable();
                readJsonObjectAsSettable(nextMap, json.substring(i + 1, closingIndex));
                values.add(nextMap.map);
                i = closingIndex + 1;
            } else {
                int commaIndex = getCommaIndex(json, i);
                Object value = readPrimitiveValue(name, json, i, commaIndex);
                values.add(value);
                i = commaIndex;
            }
        }

        return values;
    }
    protected Object readPrimitiveValue(String name, String json, int from, int to) {
        Object value = json.substring(from, to);
        String valueStr = value.toString().trim();
        if (valueStr.charAt(0) == DQUOTE) {
            value = valueStr.substring(1, valueStr.length() - 1);
        } else if ("true".equals(valueStr) || "false".equals(valueStr)) {
            value = Boolean.valueOf(valueStr);
        } else if (NULL_VALUE.equals(valueStr)) {
            return null;
        } else {
            try {
                value = Long.valueOf(valueStr);
            } catch (NumberFormatException ex) {
                Double doubleValue = Double.valueOf(valueStr);
                if (doubleValue.isInfinite() || doubleValue.isNaN()) {
                    throw new NumberFormatException("Non-finite numeric value is not allowed");
                }
                value = doubleValue;
            }
        }

        if (value instanceof String) {
            value = decodeEscapeSequences((String) value);
        }
        return value;
    }

    protected static int getCommaIndex(String json, int from) {
        int commaIndex = getNextSepCharIndex(json, COMMA, from);
        if (commaIndex == -1) {
            commaIndex = json.length();
        }
        return commaIndex;
    }
    protected static int getClosingIndex(String json, char openChar, char closeChar, int from) {
        int nextOpenIndex = getNextSepCharIndex(json, openChar, from + 1);
        int closingIndex = getNextSepCharIndex(json, closeChar, from + 1);
        while (nextOpenIndex != -1 && nextOpenIndex < closingIndex) {
            nextOpenIndex = getNextSepCharIndex(json, openChar, nextOpenIndex + 1);
            closingIndex = getNextSepCharIndex(json, closeChar, closingIndex + 1);
        }
        return closingIndex;
    }

    protected static int getNextSepCharIndex(String json, char curlyBracketChar, int from) {
        int nextCurlyBracketIndex = -1;
        boolean inString = false;
        for (int i = from; i < json.length(); i++) {
            char currentChar = json.charAt(i);
            if (currentChar == curlyBracketChar && !inString) {
                nextCurlyBracketIndex = i;
                break;
            } else if (currentChar == DQUOTE) {
                // Count how many consecutive backslashes precede this quote.
                // An odd count means the quote itself is escaped (e.g. \");
                // an even count means the backslashes are paired escape sequences
                // and the quote is a real string delimiter (e.g. \\" = escaped \ + closing ").
                int backslashCount = 0;
                int k = i - 1;
                while (k >= from && json.charAt(k) == ESCAPE) {
                    backslashCount++;
                    k--;
                }
                if (backslashCount % 2 != 0) {
                    continue;
                }
                inString = !inString;
            }
        }
        return nextCurlyBracketIndex;
    }

    public void setFormat(boolean format) {
        this.format = format;
    }

    private interface Settable {
        void put(String key, Object value);
    }
    private static final class MapSettable implements Settable {
        private Map<String, Object> map = new LinkedHashMap<>();
        public void put(String key, Object value) {
            map.put(key, value);
        }

    }
    private static class JsonObjectSettable implements Settable {
        private JsonMapObject obj;
        JsonObjectSettable(JsonMapObject obj) {
            this.obj = obj;
        }
        public void put(String key, Object value) {
            obj.setProperty(key, value);
        }
    }
    private interface Output {
        Output append(String str);
        Output append(char ch);
    }
    private class StringBuilderOutput implements Output {
        private StringBuilder sb;
        StringBuilderOutput(StringBuilder sb) {
            this.sb = sb;
        }
        @Override
        public Output append(String str) {
            sb.append(str);
            return this;
        }
        @Override
        public Output append(char ch) {
            sb.append(ch);
            return this;
        }

    }
    private class StreamOutput implements Output {
        private OutputStream os;
        StreamOutput(OutputStream os) {
            this.os = os;
        }
        @Override
        public Output append(String str) {
            try {
                os.write(StringUtils.toBytesUTF8(str != null ? str : NULL_VALUE));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            return this;
        }
        @Override
        public Output append(char ch) {
            try {
                os.write(ch);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            return this;
        }

    }

    /**
     * Decodes all RFC 8259 section 7 JSON string escape sequences in a single
     * left-to-right pass, producing the logical string value.
     *
     * <p>Recognised sequences: {@code \"}, {@code \\}, {@code \/}, {@code \b},
     * {@code \f}, {@code \n}, {@code \r}, {@code \t}, and four-digit hex Unicode
     * escapes (backslash + {@code u} + four hex digits).
     *
     * <p>A single pass is used deliberately: sequential {@code String.replace} calls
     * applied in separate passes can interact incorrectly (e.g. a raw {@code \\"}
     * sequence would have its {@code \"} consumed by a "decode quotes" pass before
     * the {@code \\} is consumed by a "decode backslashes" pass, yielding the wrong
     * result).
     */
    private static String decodeEscapeSequences(String s) {
        int backslashIdx = s.indexOf(ESCAPE);
        if (backslashIdx == -1) {
            return s; // fast path: nothing to decode
        }
        StringBuilder sb = new StringBuilder(s.length());
        sb.append(s, 0, backslashIdx);
        int i = backslashIdx;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c != ESCAPE || i + 1 >= s.length()) {
                sb.append(c);
                i++;
                continue;
            }
            char next = s.charAt(i + 1);
            switch (next) {
            case '"':  sb.append('"');  i += 2; break;
            case '\\': sb.append('\\'); i += 2; break;
            case '/':  sb.append('/');  i += 2; break;
            case 'b':  sb.append('\b'); i += 2; break;
            case 'f':  sb.append('\f'); i += 2; break;
            case 'n':  sb.append('\n'); i += 2; break;
            case 'r':  sb.append('\r'); i += 2; break;
            case 't':  sb.append('\t'); i += 2; break;
            case 'u':
                if (i + 5 < s.length()) {
                    String hex = s.substring(i + 2, i + 6);
                    try {
                        sb.append((char) Integer.parseInt(hex, 16));
                        i += 6;
                        break;
                    } catch (NumberFormatException ignored) {
                        // not a valid four-digit hex sequence — fall through and keep '\'
                    }
                }
                sb.append(c);
                i++;
                break;
            default:
                // unrecognised escape — keep the backslash as-is
                sb.append(c);
                i++;
                break;
            }
        }
        return sb.toString();
    }

    /**
     * Returns the index of the closing {@code "} that matches the opening quote at
     * {@code openQuoteIndex}, correctly skipping over escaped quotes ({@code \"}) and
     * escaped backslashes ({@code \\}) inside the string by counting consecutive
     * backslashes immediately before each candidate {@code "}: an odd count means the
     * quote is escaped; an even count means it is a real string delimiter.
     */
    private static int findClosingQuote(String json, int openQuoteIndex) {
        for (int i = openQuoteIndex + 1; i < json.length(); i++) {
            if (json.charAt(i) == DQUOTE) {
                int backslashCount = 0;
                int k = i - 1;
                while (k > openQuoteIndex && json.charAt(k) == ESCAPE) {
                    backslashCount++;
                    k--;
                }
                if (backslashCount % 2 == 0) {
                    return i;
                }
            }
        }
        return json.length(); // malformed — treat end-of-string as sentinel
    }

    /**
     * Decodes the JSON escape sequences that may appear in a key name by delegating
     * to the same single-pass decoder used for string values.
     */
    private static String unescapeKeyName(String name) {
        return decodeEscapeSequences(name);
    }

    private String escapeJson(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20) {
                // RFC 8259 section 7: all control characters (U+0000–U+001F) MUST be escaped.
                switch (c) {
                case '\b': sb.append("\\b");  break;
                case '\t': sb.append("\\t");  break;
                case '\n': sb.append("\\n");  break;
                case '\f': sb.append("\\f");  break;
                case '\r': sb.append("\\r");  break;
                default:   sb.append(String.format("\\u%04x", (int) c)); break;
                }
            // If we have " and the previous char was not \ then escape it
            } else if (c == '"' && (i == 0 || value.charAt(i - 1) != '\\')) {
                sb.append('\\').append(c);
            // If we have \ and the previous char was not \ and the next char is not an escaped char, then escape it
            } else if (c == '\\' && (i == 0 || value.charAt(i - 1) != '\\')
                    && (i == value.length() - 1 || !isEscapedChar(value.charAt(i + 1)))) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private boolean isEscapedChar(char c) {
        return ESCAPED_CHARS.contains(Character.valueOf(c));
    }

}
