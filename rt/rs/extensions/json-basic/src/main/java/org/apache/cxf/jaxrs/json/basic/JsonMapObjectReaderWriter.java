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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;



public class JsonMapObjectReaderWriter {
    private static final String NULL_VALUE = "null";
    private boolean format;
    
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
        out.append("{");
        for (Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Object> entry = it.next();
            out.append("\"").append(entry.getKey()).append("\"");
            out.append(":");
            toJsonInternal(out, entry.getValue(), it.hasNext());
        }
        out.append("}");
    }
    
    protected void toJsonInternal(Output out, Object[] array) {
        toJsonInternal(out, Arrays.asList(array));
    }
    
    protected void toJsonInternal(Output out, Collection<?> coll) {
        out.append("[");
        formatIfNeeded(out);
        for (Iterator<?> iter = coll.iterator(); iter.hasNext();) {
            toJsonInternal(out, iter.next(), iter.hasNext());
        }
        formatIfNeeded(out);
        out.append("]");
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
            boolean stringOrEnum = value.getClass() == String.class || value.getClass().isEnum();
            if (stringOrEnum) {
                out.append("\"");
            }
            out.append(value.toString());
            if (stringOrEnum) {
                out.append("\"");
            }
        }
        if (hasNext) {
            out.append(",");
            formatIfNeeded(out);
        }
        
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
            if (isWhiteSpace(json.charAt(i))) {
                continue;
            }
            
            int closingQuote = json.indexOf('"', i + 1);
            int from = json.charAt(i) == '"' ? i + 1 : i;
            String name = json.substring(from, closingQuote);
            int sepIndex = json.indexOf(':', closingQuote + 1);
            
            int j = 1;
            while (isWhiteSpace(json.charAt(sepIndex + j))) {
                j++;
            }
            if (json.charAt(sepIndex + j) == '{') {
                int closingIndex = getClosingIndex(json, '{', '}', sepIndex + j);
                String newJson = json.substring(sepIndex + j + 1, closingIndex);
                MapSettable nextMap = new MapSettable();
                readJsonObjectAsSettable(nextMap, newJson);
                values.put(name, nextMap.map);
                i = closingIndex + 1;
            } else if (json.charAt(sepIndex + j) == '[') {
                int closingIndex = getClosingIndex(json, '[', ']', sepIndex + j);
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
        List<Object> values = new LinkedList<Object>();
        for (int i = 0; i < json.length(); i++) {
            if (isWhiteSpace(json.charAt(i))) {
                continue;
            }
            if (json.charAt(i) == '{') {
                int closingIndex = getClosingIndex(json, '{', '}', i);
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
        if (valueStr.startsWith("\"")) {
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
        return value;
    }
    
    protected static int getCommaIndex(String json, int from) {
        int commaIndex = json.indexOf(",", from);
        if (commaIndex == -1) {
            commaIndex = json.length();
        }
        return commaIndex;
    }
    protected int getClosingIndex(String json, char openChar, char closeChar, int from) {
        int nextOpenIndex = json.indexOf(openChar, from + 1);
        int closingIndex = json.indexOf(closeChar, from + 1);
        while (nextOpenIndex != -1 && nextOpenIndex < closingIndex) {
            nextOpenIndex = json.indexOf(openChar, nextOpenIndex + 1);
            closingIndex = json.indexOf(closeChar, closingIndex + 1);
        }
        return closingIndex;
    }
    protected boolean isWhiteSpace(char jsonChar) {
        return jsonChar == ' ' || jsonChar == '\r' || jsonChar == '\n' || jsonChar == '\t';
    }

    public void setFormat(boolean format) {
        this.format = format;
    }

    private interface Settable {
        void put(String key, Object value);
    }
    private static class MapSettable implements Settable {
        private Map<String, Object> map = new LinkedHashMap<String, Object>();
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
        
    }
    private class StreamOutput implements Output {
        private OutputStream os;
        StreamOutput(OutputStream os) {
            this.os = os;
        }
        @Override
        public Output append(String str) {
            try {
                os.write(StringUtils.toBytesUTF8(str));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            return this;
        }
        
    }
    
}
