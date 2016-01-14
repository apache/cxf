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
package org.apache.cxf.transport.http.auth;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HttpAuthHeader {
    public static final String AUTH_TYPE_BASIC = "Basic";
    public static final String AUTH_TYPE_DIGEST = "Digest";
    public static final String AUTH_TYPE_NEGOTIATE = "Negotiate";

    private String fullHeader;
    private String authType;
    private String fullContent;
    private Map<String, String> params;

    public HttpAuthHeader(String fullHeader) {
        this.fullHeader = (fullHeader == null) ? "" : fullHeader;
        int spacePos = this.fullHeader.indexOf(' ');
        if (spacePos == -1) {
            this.authType = this.fullHeader;
            this.fullContent = "";
        } else {
            this.authType = this.fullHeader.substring(0, spacePos);
            this.fullContent = this.fullHeader.substring(spacePos + 1);
        }
        this.params = parseHeader();
    }
    public HttpAuthHeader(List<String> params) {
        boolean first = true;
        for (String s : params) {
            if (!first) {
                fullHeader += ", " + s;
            } else {
                first = false;
                fullHeader = s;
            }
        }
        int spacePos = this.fullHeader.indexOf(' ');
        if (spacePos == -1) {
            this.authType = this.fullHeader;
            this.fullContent = "";
        } else {
            this.authType = this.fullHeader.substring(0, spacePos);
            this.fullContent = this.fullHeader.substring(spacePos + 1);
        }
        this.params = parseHeader();
    }
    
    public HttpAuthHeader(String authType, Map<String, String> params) {
        this.authType = authType;
        this.params = params;
        this.fullContent = paramsToString();
        this.fullHeader = authType + " " + fullContent;
    }
    
    private String paramsToString() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String param = entry.getValue();
            if (param != null) {
                if (!first) {
                    builder.append(", ");
                }
                if (entry.getKey().equals("nc") 
                    || entry.getKey().equals("qop")
                    || entry.getKey().equals("algorithm")) {
                    builder.append(entry.getKey() + "=" + param + "");
                } else {
                    builder.append(entry.getKey() + "=\"" + param + "\"");
                }
                first = false;
            }
        }
        return builder.toString();
    }

    private Map<String, String> parseHeader() {
        Map<String, String> map = new HashMap<String, String>();
        try {
            StreamTokenizer tok = new StreamTokenizer(new StringReader(this.fullContent));
            tok.quoteChar('"');
            tok.quoteChar('\'');
            tok.whitespaceChars('=', '=');
            tok.whitespaceChars(',', ',');

            while (tok.nextToken() != StreamTokenizer.TT_EOF) {
                String key = tok.sval;
                if (tok.nextToken() == StreamTokenizer.TT_EOF) {
                    map.put(key, null);
                    return map;
                }
                String value = null;
                if ("nc".equals(key)) {
                    //nc is a 8 length HEX number so need get it as number
                    value = String.valueOf(tok.nval);
                    if (value.indexOf(".") > 0) {
                        value = value.substring(0, value.indexOf("."));
                    }
                    String pad = "";
                    for (int i = 0; i < 8 - value.length(); i++) {
                        pad = pad + "0";
                    }
                    value = pad + value;
                } else {
                    value = tok.sval;
                }
                map.put(key, value);
            }
        } catch (IOException ex) {
            //ignore can't happen for StringReader
        }
        return map;
    }

    /**
     * Extracts the authorization realm from the 
     * "WWW-Authenticate" Http response header.
     * 
     * @param authenticate content of the WWW-Authenticate header
     * @return The realm, or null if it is non-existent.
     */
    public String getRealm() {
        Map<String, String> map = parseHeader();
        return map.get("realm");
    }

    public boolean authTypeIsDigest() {
        return AUTH_TYPE_DIGEST.equals(this.authType);
    }
    
    public boolean authTypeIsBasic() {
        return AUTH_TYPE_BASIC.equals(this.authType);
    }
    
    public boolean authTypeIsNegotiate() {
        return AUTH_TYPE_DIGEST.equals(this.authType);
    }
    
    public String getAuthType() {
        return authType;
    }

    public String getFullContent() {
        return fullContent;
    }

    public String getFullHeader() {
        return this.fullHeader;
    }

    public Map<String, String> getParams() {
        return params;
    }

}
