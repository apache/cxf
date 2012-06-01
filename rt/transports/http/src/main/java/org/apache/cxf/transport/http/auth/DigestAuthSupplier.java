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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;

/**
 * 
 */
public class DigestAuthSupplier implements HttpAuthSupplier {
    private static final char[] HEXADECIMAL = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    final MessageDigest md5Helper;
    Map<URI, DigestInfo> authInfo = new ConcurrentHashMap<URI, DigestInfo>(); 

    public DigestAuthSupplier() {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            md = null;
        }
        md5Helper = md;
    }

    /**
     * {@inheritDoc}
     * With digest, the nonce could expire and thus a rechallenge will be issued.
     * Thus, we need requests cached to be able to handle that
     */
    public boolean requiresRequestCaching() {
        return true;
    }

    public String getAuthorization(AuthorizationPolicy authPolicy,
                                   URL currentURL,
                                   Message message,
                                   String fullHeader) {
        if (authPolicy.getUserName() == null && authPolicy.getPassword() == null) {
            return null;
        }
        URI currentURI = URI.create(currentURL.toString());
        if (fullHeader == null) {
            DigestInfo di = authInfo.get(currentURI);
            if (di != null) {
                /* Preemptive authentication is only possible if we have a cached
                 * challenge
                 */
                return di.generateAuth(currentURL.getFile(), 
                                       authPolicy.getUserName(),
                                       authPolicy.getPassword());            
            } else {
                return null;
            }
        }
        HttpAuthHeader authHeader = new HttpAuthHeader(fullHeader);
        if (authHeader.authTypeIsDigest() && authPolicy != null) {
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
                
                return di.generateAuth(currentURL.getFile(), 
                                       authPolicy.getUserName(),
                                       authPolicy.getPassword());
            }
            
        }
        return null;
    }

    public String createCnonce() throws UnsupportedEncodingException {
        String cnonce = Long.toString(System.currentTimeMillis());
        byte[] bytes = cnonce.getBytes("US-ASCII");
        synchronized (md5Helper) {
            bytes = md5Helper.digest(bytes);
        }
        return encode(bytes);
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
                nc++;
                String ncstring = String.format("%08d", nc);
                String cnonce = createCnonce();
                
                String digAlg = algorithm;
                if (digAlg.equalsIgnoreCase("MD5-sess")) {
                    digAlg = "MD5";
                }
                MessageDigest digester = MessageDigest.getInstance(digAlg);
                String a1 = username + ":" + realm + ":" + password;
                if ("MD5-sess".equalsIgnoreCase(algorithm)) {
                    algorithm = "MD5";
                    String tmp2 = encode(digester.digest(a1.getBytes(charset)));
                    a1 = tmp2 + ':' + nonce + ':' + cnonce;
                }
                String hasha1 = encode(digester.digest(a1.getBytes(charset)));
                String a2 = method + ":" + uri;
                String hasha2 = encode(digester.digest(a2.getBytes("US-ASCII")));
                String serverDigestValue = null;
                if (qop == null) {
                    serverDigestValue = hasha1 + ":" + nonce + ":" + hasha2;
                } else {
                    serverDigestValue = hasha1 + ":" + nonce + ":" + ncstring + ":" + cnonce + ":" 
                        + qop + ":" + hasha2;
                }
                String response = encode(digester.digest(serverDigestValue.getBytes("US-ASCII")));
                Map<String, String> outParams = new HashMap<String, String>();
                if (qop != null) {
                    outParams.put("qop", "auth");
                }
                outParams.put("realm", realm);
                outParams.put("opaque", opaque);
                outParams.put("nonce", nonce);
                outParams.put("uri", uri);
                outParams.put("username", username);
                outParams.put("nc", ncstring);
                outParams.put("cnonce", cnonce);
                outParams.put("response", response);
                return new HttpAuthHeader(HttpAuthHeader.AUTH_TYPE_DIGEST, outParams).getFullHeader();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        
    }

    /**
     * Encodes the 128 bit (16 bytes) MD5 digest into a 32 characters long 
     * <CODE>String</CODE> according to RFC 2617.
     * 
     * @param binaryData array containing the digest
     * @return encoded MD5, or <CODE>null</CODE> if encoding failed
     */
    private static String encode(byte[] binaryData) {
        int n = binaryData.length; 
        char[] buffer = new char[n * 2];
        for (int i = 0; i < n; i++) {
            int low = binaryData[i] & 0x0f;
            int high = (binaryData[i] & 0xf0) >> 4;
            buffer[i * 2] = HEXADECIMAL[high];
            buffer[(i * 2) + 1] = HEXADECIMAL[low];
        }

        return new String(buffer);
    }

}
