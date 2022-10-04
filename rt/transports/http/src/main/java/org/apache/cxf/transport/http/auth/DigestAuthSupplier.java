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

import java.net.URI;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 *
 */
public class DigestAuthSupplier implements HttpAuthSupplier {

    Map<URI, DigestInfo> authInfo = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     * With digest, the nonce could expire and thus a rechallenge will be issued.
     * Thus, we need requests cached to be able to handle that
     */
    public boolean requiresRequestCaching() {
        return true;
    }

    public String getAuthorization(AuthorizationPolicy authPolicy,
                                   URI currentURI,
                                   Message message,
                                   String fullHeader) {
        if (authPolicy == null || (authPolicy.getUserName() == null && authPolicy.getPassword() == null)) {
            return null;
        }

        if (fullHeader == null) {
            DigestInfo di = authInfo.get(currentURI);
            if (di != null) {
                /* Preemptive authentication is only possible if we have a cached
                 * challenge
                 */
                return di.generateAuth(getAuthURI(currentURI),
                                       authPolicy.getUserName(),
                                       authPolicy.getPassword());
            }
            return null;
        }
        HttpAuthHeader authHeader = new HttpAuthHeader(fullHeader);
        if (authHeader.authTypeIsDigest()) {
            Map<String, String> map = authHeader.getParams();
            if ("auth".equals(map.get("qop"))
                || !map.containsKey("qop")) {
                DigestInfo di = new DigestInfo();
                di.qop = map.get("qop");
                di.realm = map.get("realm");
                di.nonce = map.get("nonce");
                di.opaque = map.get("opaque");
                if (map.containsKey("algorithm")) {
                    di.algorithm = map.get("algorithm");
                }
                if (map.containsKey("charset")) {
                    di.charset = map.get("charset");
                }
                di.method = (String)message.get(Message.HTTP_REQUEST_METHOD);
                if (di.method == null) {
                    di.method = "POST";
                }
                authInfo.put(currentURI, di);

                return di.generateAuth(getAuthURI(currentURI),
                                       authPolicy.getUserName(),
                                       authPolicy.getPassword());
            }

        }
        return null;
    }

    private static String getAuthURI(URI currentURI) {
        String authURI = currentURI.getRawPath();
        if (currentURI.getRawQuery() != null) {
            authURI += '?' + currentURI.getRawQuery();
        }
        return authURI;
    }

    public String createCnonce() {
        return Long.toString(System.currentTimeMillis());
    }

    class DigestInfo {
        String qop;
        String realm;
        String nonce;
        String opaque;
        int nc;
        String algorithm = "MD5";
        String charset = "ISO-8859-1";
        String method = "POST";

        synchronized String generateAuth(String uri, String username, String password) {
            try {
                String digAlg = algorithm;
                if ("MD5-sess".equalsIgnoreCase(digAlg)) {
                    digAlg = "MD5";
                }
                final MessageDigest digester = MessageDigest.getInstance(digAlg);
                String cnonce = createCnonce();
                String a1 = username + ':' + realm + ':' + password;
                if ("MD5-sess".equalsIgnoreCase(algorithm)) {
                    String tmp2 = StringUtils.toHexString(digester.digest(a1.getBytes(charset)));
                    a1 = tmp2 + ':' + nonce + ':' + cnonce;
                }
                String hasha1 = StringUtils.toHexString(digester.digest(a1.getBytes(charset)));
                String a2 = method + ':' + uri;
                String hasha2 = StringUtils.toHexString(digester.digest(a2.getBytes(US_ASCII)));
                final String serverDigestValue;
                final String ncstring;
                if (qop == null) {
                    ncstring = null;
                    serverDigestValue = hasha1 + ':' + nonce + ':' + hasha2;
                } else {
                    ncstring = StringUtils.toHexString(ByteBuffer.allocate(4).putInt(++nc).array());
                    serverDigestValue = hasha1 + ':' + nonce + ':' + ncstring + ':' + cnonce + ':'
                        + qop + ':' + hasha2;
                }
                String response = StringUtils.toHexString(digester.digest(serverDigestValue.getBytes(US_ASCII)));
                Map<String, String> outParams = new HashMap<>();
                if (qop != null) {
                    outParams.put("qop", "auth");
                }
                outParams.put("realm", realm);
                outParams.put("opaque", opaque);
                outParams.put("nonce", nonce);
                outParams.put("uri", uri);
                outParams.put("username", username);
                if (ncstring != null) {
                    outParams.put("nc", ncstring);
                }
                outParams.put("cnonce", cnonce);
                outParams.put("response", response);
                outParams.put("algorithm", algorithm);
                return new HttpAuthHeader(HttpAuthHeader.AUTH_TYPE_DIGEST, outParams).getFullHeader();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }


    }

}
