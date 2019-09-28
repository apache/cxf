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
package org.apache.cxf.rs.security.oauth2.tokens.hawk;

import java.security.SecureRandom;
import java.util.Map;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.oauth2.client.HttpRequestProperties;
import org.apache.cxf.rs.security.oauth2.common.AccessToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rt.security.crypto.HmacUtils;
// https://tools.ietf.org/html/draft-hammer-oauth-v2-mac-token-05
// ->
// https://github.com/hueniverse/hawk/blob/master/README.md
public class HawkAuthorizationScheme {
    private static final String SEPARATOR = "\n";
    private static final String HAWK_1_HEADER = "hawk.1.header";

    private HttpRequestProperties props;
    private String macKey;
    private String timestamp;
    private String nonce;

    public HawkAuthorizationScheme(HttpRequestProperties props,
                                  AccessToken token) {
        this.props = props;
        this.macKey = token.getTokenKey();
        this.timestamp = Long.toString(System.currentTimeMillis());
        this.nonce = generateNonce();
    }

    public HawkAuthorizationScheme(HttpRequestProperties props,
                                  Map<String, String> schemeParams) {
        this.props = props;
        this.macKey = schemeParams.get(OAuthConstants.HAWK_TOKEN_ID);
        this.timestamp = schemeParams.get(OAuthConstants.HAWK_TOKEN_TIMESTAMP);
        this.nonce = schemeParams.get(OAuthConstants.HAWK_TOKEN_NONCE);
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
        String signature = HmacUtils.encodeHmacString(macSecret,
                                                      HmacAlgorithm.toHmacAlgorithm(macAlgo).getJavaName(),
                                                      data);

        StringBuilder sb = new StringBuilder();
        sb.append(OAuthConstants.HAWK_AUTHORIZATION_SCHEME).append(' ');
        addParameter(sb, OAuthConstants.HAWK_TOKEN_ID, macKey, false);
        addParameter(sb, OAuthConstants.HAWK_TOKEN_TIMESTAMP, timestamp, false);
        addParameter(sb, OAuthConstants.HAWK_TOKEN_NONCE, nonce, false);
        addParameter(sb, OAuthConstants.HAWK_TOKEN_SIGNATURE, signature, true);

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


        return HAWK_1_HEADER + SEPARATOR
            + timestamp + SEPARATOR
            + nonce + SEPARATOR
            + props.getHttpMethod().toUpperCase() + SEPARATOR
            + requestURI + SEPARATOR
            + props.getHostName() + SEPARATOR
            + props.getPort() + SEPARATOR
            + SEPARATOR
            + SEPARATOR;
    }

    private static String normalizeQuery(String query) {
        return query;
    }

    private static String generateNonce() {
        byte[] randomBytes = new byte[20];
        new SecureRandom().nextBytes(randomBytes);
        return Base64Utility.encode(randomBytes);
    }

}
