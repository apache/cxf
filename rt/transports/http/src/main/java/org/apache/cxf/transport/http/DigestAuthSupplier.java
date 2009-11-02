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

package org.apache.cxf.transport.http;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
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
public class DigestAuthSupplier extends HttpAuthSupplier {
    private static final char[] HEXADECIMAL = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
    private static final MessageDigest MD5_HELPER;
    static {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            md = null;
        }
        MD5_HELPER = md;
    }

    Map<URL, DigestInfo> authInfo = new ConcurrentHashMap<URL, DigestInfo>(); 

    /**
     * {@inheritDoc}
     * With digest, the nonce could expire and thus a rechallenge will be issued.
     * Thus, we need requests cached to be able to handle that
     */
    public boolean requiresRequestCaching() {
        return true;
    }
    
    static Map<String, String> parseHeader(String fullHeader) {
        
        Map<String, String> map = new HashMap<String, String>();
        fullHeader = fullHeader.substring(7);
        try {
            StreamTokenizer tok = new StreamTokenizer(new StringReader(fullHeader));
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
                String value = tok.sval;
                if (value.charAt(0) == '"') {
                    value = value.substring(1, value.length() - 1);
                }
                map.put(key, value);
            }
        } catch (IOException ex) {
            //ignore
        }
        return map;
    }
    
    @Override
    public String getAuthorizationForRealm(HTTPConduit conduit, URL currentURL,
                                           Message message,
                                           String realm, String fullHeader) {
        if (fullHeader.startsWith("Digest ")) {
            Map<String, String> map = parseHeader(fullHeader);
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
                authInfo.put(currentURL, di);
                return di.generateAuth(currentURL.getFile(), 
                                       getUsername(conduit, message),
                                       getPassword(conduit, message));
            }
            
        }
        return null;
    }
    @Override
    public String getPreemptiveAuthorization(HTTPConduit conduit, URL currentURL, Message message) {
        DigestInfo di = authInfo.get(currentURL);
        if (di != null) {
            return di.generateAuth(currentURL.getFile(), 
                                   getUsername(conduit, message),
                                   getPassword(conduit, message));            
        }
        return null;
    }

    private String getPassword(HTTPConduit conduit, Message message) {
        AuthorizationPolicy policy = getPolicy(conduit, message);
        return policy != null ? policy.getPassword() : null;
    }

    private String getUsername(HTTPConduit conduit, Message message) {
        AuthorizationPolicy policy = getPolicy(conduit, message);
        return policy != null ? policy.getUserName() : null;
    }

    private AuthorizationPolicy getPolicy(HTTPConduit conduit, Message message) {
        AuthorizationPolicy policy 
            = (AuthorizationPolicy)message.getContextualProperty(AuthorizationPolicy.class.getName());
        if (policy == null) {
            policy = conduit.getAuthorization();
        }
        if (policy != null
            && (!policy.isSetAuthorizationType()
                || "Digest".equals(policy.getAuthorizationType()))) {
            return policy;
        }
        return null;
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
                String ncstring = Integer.toString(nc);
                while (ncstring.length() < 8) {
                    ncstring = "0" + ncstring;
                }
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
                    StringBuilder tmp3 = new StringBuilder(
                            tmp2.length() + nonce.length() + cnonce.length() + 2);
                    tmp3.append(tmp2);
                    tmp3.append(':');
                    tmp3.append(nonce);
                    tmp3.append(':');
                    tmp3.append(cnonce);
                    a1 = tmp3.toString();
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
                serverDigestValue = encode(digester.digest(serverDigestValue.getBytes("US-ASCII")));
                StringBuilder builder = new StringBuilder("Digest ");
                if (qop != null) {
                    builder.append("qop=\"auth\", ");
                }  
                builder.append("realm=\"")
                    .append(realm);

                if (opaque != null) {
                    builder.append("\", opaque=\"")
                        .append(opaque);
                }

                builder.append("\", nonce=\"")
                    .append(nonce)
                    .append("\", uri=\"")
                    .append(uri)
                    .append("\", username=\"")
                    .append(username)
                    .append("\", nc=")
                    .append(ncstring)
                    .append(", cnonce=\"")
                    .append(cnonce)        
                    .append("\", response=\"")
                    .append(serverDigestValue)
                    .append("\"");
                
                return builder.toString();
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        
    }

    public static String createCnonce() throws UnsupportedEncodingException {
        String cnonce = Long.toString(System.currentTimeMillis());
        return encode(MD5_HELPER.digest(cnonce.getBytes("US-ASCII")));
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
