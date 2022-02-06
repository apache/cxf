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
import java.nio.ByteBuffer;
import java.util.*;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;



public class JsonMapObjectReaderWriter {
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
    private final static int[] sOutputEscapes128;
    private final static char[] HC = "0123456789ABCDEF".toCharArray();

    /**
     * Value used for lookup tables to indicate that matching characters
     * are to be escaped using standard escaping; for JSON this means
     * (for example) using "backslash - u" escape method.
     */

    public final static int ESCAPE_STANDARD = -1;
    static {
        int[] table = new int[128];
        // Control chars need generic escape sequence
        for (int i = 0; i < 32; ++i) {
            // 04-Mar-2011, tatu: Used to use "-(i + 1)", replaced with constant
            table[i] = ESCAPE_STANDARD;
        }
        /* Others (and some within that range too) have explicit shorter
         * sequences
         */
        table['"'] = '"';
        table['\\'] = '\\';
        // Escaping of slash is optional, so let's not add it
        table[0x08] = 'b';
        table[0x09] = 't';
        table[0x0C] = 'f';
        table[0x0A] = 'n';
        table[0x0D] = 'r';
        sOutputEscapes128 = table;
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

            int closingQuote = json.indexOf(DQUOTE, i + 1);
            int from = json.charAt(i) == DQUOTE ? i + 1 : i;
            String name = json.substring(from, closingQuote);
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
                value = Double.valueOf(valueStr);
            }
        }

        if (value instanceof String) {
            if (((String) value).contains("\\/")) {
                // Escape an encoded forward slash
                value = ((String) value).replace("\\/", "/");
            }
            if (((String) value).contains("\\\"")) {
                // Escape an encoded quotation mark
                value = ((String) value).replace("\\\"", "\"");
            }
            if (((String) value).contains("\\\\")) {
                // Escape an encoded backslash
                value = ((String) value).replace("\\\\", "\\");
            }
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
                if (i > from && json.charAt(i - 1) == ESCAPE) {
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
    private static class MapSettable implements Settable {
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

    private String escapeJson(String content) {
        StringBuilder sb = new StringBuilder();
        final int[] escCodes = sOutputEscapes128;
        int escLen = escCodes.length;
        for (int i = 0, len = content.length(); i < len; ++i) {
            char c = content.charAt(i);
            if (c >= escLen || escCodes[c] == 0) {
                sb.append(c);
                continue;
            }
            sb.append('\\');
            int escCode = escCodes[c];
            if (escCode < 0) { // generic quoting (hex value)
                // The only negative value sOutputEscapes128 returns
                // is CharacterEscapes.ESCAPE_STANDARD, which mean
                // appendQuotes should encode using the Unicode encoding;
                // not sure if this is the right way to encode for
                // CharacterEscapes.ESCAPE_CUSTOM or other (future)
                // CharacterEscapes.ESCAPE_XXX values.

                // We know that it has to fit in just 2 hex chars
                sb.append('u');
                sb.append('0');
                sb.append('0');
                int value = c;  // widening
                sb.append(HC[value >> 4]);
                sb.append(HC[value & 0xF]);
            } else { // "named", i.e. prepend with slash
                sb.append((char) escCode);
            }
        }
        return sb.toString();
    }

}
