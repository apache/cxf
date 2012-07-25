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
package org.apache.cxf.rs.security.oauth2.tokens.mac;

import java.security.SecureRandom;
import java.util.Map;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.oauth2.client.HttpRequestProperties;
import org.apache.cxf.rs.security.oauth2.common.AccessToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public class MacAuthorizationScheme {
    private static final String SEPARATOR = "\n";
    
    private HttpRequestProperties props;
    private String macKey;
    private String timestamp;
    private String nonce;
    
    public MacAuthorizationScheme(HttpRequestProperties props,
                                  AccessToken token) {
        this.props = props;
        this.macKey = token.getTokenKey();
        this.timestamp = Long.toString(System.currentTimeMillis());
        this.nonce = generateNonce(token.getIssuedAt());
    }
    
    public MacAuthorizationScheme(HttpRequestProperties props,
                                  Map<String, String> schemeParams) {
        this.props = props;
        this.macKey = schemeParams.get(OAuthConstants.MAC_TOKEN_ID);
        this.timestamp = schemeParams.get(OAuthConstants.MAC_TOKEN_EXTENSION);
        this.nonce = schemeParams.get(OAuthConstants.MAC_TOKEN_NONCE);
    }
    
    public String getMacKey() {
        return macKey;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public String getNonce() {
        return nonce;
    }
    
    public String toAuthorizationHeader(String macAlgo, String macSecret) {
        
        String data = getNormalizedRequestString();
        String signature = HmacUtils.computeSignature(macAlgo, macSecret, data);
        
        StringBuilder sb = new StringBuilder();
        sb.append(OAuthConstants.MAC_AUTHORIZATION_SCHEME).append(" ");
        addParameter(sb, OAuthConstants.MAC_TOKEN_ID, macKey, false);
        addParameter(sb, OAuthConstants.MAC_TOKEN_NONCE, nonce, false);
        addParameter(sb, OAuthConstants.MAC_TOKEN_SIGNATURE, signature, false);
        // lets pass a timestamp via an extension parameter
        addParameter(sb, OAuthConstants.MAC_TOKEN_EXTENSION, timestamp, false);
        
        
        return sb.toString();
    }
    
    private static void addParameter(StringBuilder sb, String name, String value, boolean last) {
        sb.append(name).append('=')
          .append('\"').append(value).append('\"');
        if (!last) {
            sb.append(',');
        }
    }
    
    public String getNormalizedRequestString() {
        String requestURI = props.getRequestPath();
        if (!StringUtils.isEmpty(props.getRequestQuery())) {
            requestURI += "?" + normalizeQuery(props.getRequestQuery());
        }
        
        
        String value = nonce + SEPARATOR
            + props.getHttpMethod().toUpperCase() + SEPARATOR
            + requestURI + SEPARATOR 
            + props.getHostName() + SEPARATOR 
            + props.getPort() + SEPARATOR
            + "" + SEPARATOR
            + timestamp + SEPARATOR;

        return value;
    }
    
    private static String normalizeQuery(String query) {  
        return query;
    }
    
    private static String generateNonce(long issuedAt) {
        long ageInSecs = System.currentTimeMillis() / 1000 - issuedAt;
        if (ageInSecs == 0) {
            ageInSecs = 1;
        }
        byte[] randomBytes = new byte[20];
        new SecureRandom().nextBytes(randomBytes);
        String random = Base64Utility.encode(randomBytes);
        
        return Long.toString(ageInSecs) + ":" + random;
    }

}
