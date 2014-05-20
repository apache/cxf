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
package org.apache.cxf.rs.security.oauth2.jwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;



public class JwtTokenReaderWriter implements JwtTokenReader, JwtTokenWriter {
    private static final Set<String> DATE_PROPERTIES = 
        new HashSet<String>(Arrays.asList(JwtConstants.CLAIM_EXPIRY, 
                                          JwtConstants.CLAIM_ISSUED_AT, 
                                          JwtConstants.CLAIM_NOT_BEFORE));
    private boolean format;
    
    @Override
    public String headersToJson(JwtHeaders headers) {
        return toJson(headers);
    }

    @Override
    public String claimsToJson(JwtClaims claims) {
        return toJson(claims);
    }

    @Override
    public JwtTokenJson tokenToJson(JwtToken token) {
        return new JwtTokenJson(toJson(token.getHeaders()),
                                    toJson(token.getClaims()));
    }
    
    @Override
    public JwtHeaders fromJsonHeaders(String headersJson) {
        JwtHeaders headers = new JwtHeaders();
        fromJsonInternal(headers, headersJson);
        return headers;
    }
    
    @Override
    public JwtClaims fromJsonClaims(String claimsJson) {
        JwtClaims claims = new JwtClaims();
        fromJsonInternal(claims, claimsJson);
        return claims;
        
    }
    
    @Override
    public JwtToken fromJson(String headersJson, String claimsJson) {
        JwtHeaders headers = fromJsonHeaders(headersJson);
        JwtClaims claims = fromJsonClaims(claimsJson);
        return new JwtToken(headers, claims);
    }
    
    @Override
    public JwtToken fromJson(JwtTokenJson pair) {
        return fromJson(pair.getHeadersJson(), pair.getClaimsJson());
    }
    
    private String toJson(AbstractJwtObject jwt) {
        StringBuilder sb = new StringBuilder();
        toJsonInternal(sb, jwt.asMap());
        return sb.toString();
    }

    private void toJsonInternal(StringBuilder sb, Map<String, Object> map) {
        sb.append("{");
        for (Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Object> entry = it.next();
            sb.append("\"").append(entry.getKey()).append("\"");
            sb.append(":");
            toJsonInternal(sb, entry.getValue(), it.hasNext());
        }
        sb.append("}");
    }
    
    private void toJsonInternal(StringBuilder sb, Object[] array) {
        toJsonInternal(sb, Arrays.asList(array));
    }
    
    private void toJsonInternal(StringBuilder sb, Collection<?> coll) {
        sb.append("[");
        formatIfNeeded(sb);
        for (Iterator<?> iter = coll.iterator(); iter.hasNext();) {
            toJsonInternal(sb, iter.next(), iter.hasNext());
        }
        formatIfNeeded(sb);
        sb.append("]");
    }
    
    @SuppressWarnings("unchecked")
    private void toJsonInternal(StringBuilder sb, Object value, boolean hasNext) {
        if (AbstractJwtObject.class.isAssignableFrom(value.getClass())) {
            sb.append(toJson((AbstractJwtObject)value));
        } else if (value.getClass().isArray()) {
            toJsonInternal(sb, (Object[])value);
        } else if (Collection.class.isAssignableFrom(value.getClass())) {
            toJsonInternal(sb, (Collection<?>)value);
        } else if (Map.class.isAssignableFrom(value.getClass())) {
            toJsonInternal(sb, (Map<String, Object>)value);
        } else {
            if (value.getClass() == String.class) {
                sb.append("\"");
            }
            sb.append(value);
            if (value.getClass() == String.class) {
                sb.append("\"");
            }
        }
        if (hasNext) {
            sb.append(",");
            formatIfNeeded(sb);
        }
        
    }
    
    private void formatIfNeeded(StringBuilder sb) {
        if (format) {
            sb.append("\r\n ");
        }
    }
        
    private void fromJsonInternal(AbstractJwtObject jwt, String json) {
        Map<String, Object> values = readJwtObjectAsMap(json.substring(1, json.length() - 1));
        fromJsonInternal(jwt, values);
    }
    
    private void fromJsonInternal(AbstractJwtObject jwt, Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            jwt.setValue(entry.getKey(), entry.getValue());
        }
    }
    
    private Map<String, Object> readJwtObjectAsMap(String json) {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
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
                values.put(name, readJwtObjectAsMap(newJson));
                i = closingIndex + 1;
            } else if (json.charAt(sepIndex + j) == '[') {
                int closingIndex = getClosingIndex(json, '[', ']', sepIndex + j);
                String newJson = json.substring(sepIndex + j + 1, closingIndex);
                values.put(name, readJwtObjectAsList(newJson));
                i = closingIndex + 1;
            } else {
                int commaIndex = getCommaIndex(json, sepIndex + j);
                Object value = readPrimitiveValue(json, sepIndex + j, commaIndex);
                if (DATE_PROPERTIES.contains(name)) {
                    value = Integer.valueOf(value.toString());
                }
                values.put(name, value);
                i = commaIndex + 1;
            }
            
        }
        return values;
    }
    private List<Object> readJwtObjectAsList(String json) {
        List<Object> values = new LinkedList<Object>();
        for (int i = 0; i < json.length(); i++) {
            if (isWhiteSpace(json.charAt(i))) {
                continue;
            }
            if (json.charAt(i) == '{') {
                int closingIndex = getClosingIndex(json, '{', '}', i);
                values.add(readJwtObjectAsMap(json.substring(i + 1, closingIndex - 1)));
                i = closingIndex + 1;
            } else {
                int commaIndex = getCommaIndex(json, i);
                Object value = readPrimitiveValue(json, i, commaIndex);
                values.add(value);
                i = commaIndex + 1;
            }
        }
        
        return values;
    }
    private Object readPrimitiveValue(String json, int from, int to) {
        Object value = json.substring(from, to);
        String valueStr = value.toString().trim(); 
        if (valueStr.startsWith("\"")) {
            value = valueStr.substring(1, valueStr.length() - 1);
        } else if ("true".equals(value) || "false".equals(value)) {
            value = Boolean.valueOf(valueStr);
        }
        return value;
    }
    
    private static int getCommaIndex(String json, int from) {
        int commaIndex = json.indexOf(",", from);
        if (commaIndex == -1) {
            commaIndex = json.length();
        }
        return commaIndex;
    }
    private int getClosingIndex(String json, char openChar, char closeChar, int from) {
        int nextOpenIndex = json.indexOf(openChar, from + 1);
        int closingIndex = json.indexOf(closeChar, from + 1);
        while (nextOpenIndex != -1 && nextOpenIndex < closingIndex) {
            nextOpenIndex = json.indexOf(openChar, closingIndex + 1);
            closingIndex = json.indexOf(closeChar, closingIndex + 1);
        }
        return closingIndex;
    }
    private boolean isWhiteSpace(char jsonChar) {
        return jsonChar == ' ' || jsonChar == '\r' || jsonChar == '\n' || jsonChar == '\t';
    }

    public void setFormat(boolean format) {
        this.format = format;
    }

    

    
}
