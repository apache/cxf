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

    private final String fullHeader;
    private final String authType;
    private final String fullContent;
    private final Map<String, String> params;

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
        fullHeader = String.join(", ", params);
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
                if ("nc".equals(entry.getKey())
                    || "qop".equals(entry.getKey())
                    || "algorithm".equals(entry.getKey())) {
                    builder.append(entry.getKey()).append('=').append(param);
                } else {
                    builder.append(entry.getKey()).append("=\"").append(param).append('"');
                }
                first = false;
            }
        }
        return builder.toString();
    }

    private Map<String, String> parseHeader() {
        Map<String, String> map = new HashMap<>();
        try {
            StreamTokenizer tok = new StreamTokenizer(new StringReader(this.fullContent)) {
                @Override
                public void parseNumbers() {
                    // skip parse numbers
                    wordChars('0', '9');
                    wordChars('.', '.');
                    wordChars('-', '-');
                }
            };
            tok.whitespaceChars('=', '=');
            tok.whitespaceChars(',', ',');

            while (tok.nextToken() != StreamTokenizer.TT_EOF) {
                map.put(tok.sval, tok.nextToken() != StreamTokenizer.TT_EOF ? tok.sval : null);
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
     * @return The realm, or null if it is non-existent.
     */
    public String getRealm() {
        return params.get("realm");
    }

    public boolean authTypeIsDigest() {
        return AUTH_TYPE_DIGEST.equals(this.authType);
    }

    public boolean authTypeIsBasic() {
        return AUTH_TYPE_BASIC.equals(this.authType);
    }

    public boolean authTypeIsNegotiate() {
        return AUTH_TYPE_NEGOTIATE.equals(this.authType);
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
