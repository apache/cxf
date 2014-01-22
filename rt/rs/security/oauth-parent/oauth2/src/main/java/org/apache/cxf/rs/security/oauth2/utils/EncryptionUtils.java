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

package org.apache.cxf.rs.security.oauth2.utils;

import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;


/**
 * Encryption helpers
 */
public final class EncryptionUtils {
    private static final String SEP = "|";
    private EncryptionUtils() {
    }
    
    public static String getEncodedSecretKey(SecretKey key) throws Exception {
        try {
            return Base64UrlUtility.encode(key.getEncoded());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static SecretKey getSecretKey() throws Exception {
        return getSecretKey("AES");
    }
    
    public static SecretKey getSecretKey(String symEncAlgo) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(symEncAlgo);
        return keyGen.generateKey();
    }
    
    public static SecretKey getSecretKey(SecretKeyProperties props) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(props.getKeyAlgo());
        AlgorithmParameterSpec algoSpec = props.getAlgoSpec();
        SecureRandom random = props.getSecureRandom();
        if (algoSpec != null) {
            if (random != null) {
                keyGen.init(algoSpec, random);
            } else {
                keyGen.init(algoSpec);
            }
        } else {
            if (random != null) {
                keyGen.init(props.getKeySize(), random);
            } else {
                keyGen.init(props.getKeySize());
            }
        }
        
        return keyGen.generateKey();
    }
    
    public static String encryptTokenWithSecretKey(ServerAccessToken token, 
                                                      SecretKey symmetricKey) {
        return encryptTokenWithSecretKey(token, symmetricKey, null);
    }
    
    public static String encryptTokenWithSecretKey(ServerAccessToken token, 
                                                   SecretKey symmetricKey,
                                                   SecretKeyProperties props) {
        String tokenSequence = tokenizeServerToken(token);
        return encryptSequence(tokenSequence, symmetricKey, props);
    }
    
    public static String encryptRefreshTokenWithSecretKey(RefreshToken token, SecretKey symmetricKey) {
        return encryptRefreshTokenWithSecretKey(token, symmetricKey, null);
    }
    
    public static String encryptRefreshTokenWithSecretKey(RefreshToken token, 
                                                             SecretKey symmetricKey,
                                                             SecretKeyProperties props) {
        String tokenSequence = tokenizeRefreshToken(token);
        
        return encryptSequence(tokenSequence, symmetricKey, props);
    }
    
    public static String decryptTokenSequence(String encodedToken, 
                                              String encodedSecretKey) {
        return decryptTokenSequence(encodedToken, encodedSecretKey, "AES");
    }
    
    public static String decryptTokenSequence(String encodedToken, 
                                              String encodedSecretKey, 
                                              String algo) {
        try {
            SecretKey key = decodeSecretKey(encodedSecretKey, algo);
            return decryptTokenSequence(encodedToken, key);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static String decryptTokenSequence(String encodedToken, 
                                              String encodedSecretKey, 
                                              SecretKeyProperties props) {
        try {
            SecretKey key = decodeSecretKey(encodedSecretKey, props.getKeyAlgo());
            return decryptTokenSequence(encodedToken, key, props);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static String decryptTokenSequence(String encodedToken, 
                                              SecretKey key) {
        return decryptTokenSequence(encodedToken, key, null);
    }
    
    public static String decryptTokenSequence(String encodedToken, 
                                              SecretKey key,
                                              SecretKeyProperties props) {
        try {
            byte[] encryptedBytes = decodeSequence(encodedToken);
            byte[] bytes = processBytes(encryptedBytes, key, props, Cipher.DECRYPT_MODE);
            return new String(bytes, "UTF-8");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static ServerAccessToken decryptToken(OAuthDataProvider provider,
                                                 String encodedToken, 
                                                 String encodedSecretKey) {
        return decryptToken(provider, encodedToken, encodedSecretKey, "AES");
    }
    
    public static ServerAccessToken decryptToken(OAuthDataProvider provider,
                                                 String encodedToken, 
                                                 String encodedSecretKey,
                                                 String algo) {
        SecretKey key = decodeSecretKey(encodedSecretKey, algo);
        return decryptToken(provider, encodedToken, key);
    }
    
    public static ServerAccessToken decryptToken(OAuthDataProvider provider,
                                                 String encodedToken, 
                                                 String encodedSecretKey,
                                                 SecretKeyProperties props) {
        SecretKey key = decodeSecretKey(encodedSecretKey, props.getKeyAlgo());
        return decryptToken(provider, encodedToken, key, props);
    }
    
    public static ServerAccessToken decryptToken(OAuthDataProvider provider,
                                                 String encodedToken, 
                                                 SecretKey key) {
        return decryptToken(provider, encodedToken, key, null);
    }
    
    public static ServerAccessToken decryptToken(OAuthDataProvider provider,
                                                 String encodedToken, 
                                                 SecretKey key, 
                                                 SecretKeyProperties props) {
        try {
            String decryptedSequence = decryptTokenSequence(encodedToken, key, props);
            return recreateToken(provider, encodedToken, decryptedSequence);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static RefreshToken decryptRefreshToken(OAuthDataProvider provider,
                                                   String encodedToken, 
                                                   SecretKey key) {
        return decryptRefreshToken(provider, encodedToken, key, null);
    }
    
    public static RefreshToken decryptRefreshToken(OAuthDataProvider provider,
                                                   String encodedToken, 
                                                   SecretKey key, 
                                                   SecretKeyProperties props) {
        try {
            String decryptedSequence = decryptTokenSequence(encodedToken, key, props);
            return recreateRefreshToken(provider, encodedToken, decryptedSequence);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private static String encryptSequence(String sequence, SecretKey symmetricKey,
                                          SecretKeyProperties keyProps) {
        try {
            byte[] bytes = processBytes(sequence.getBytes("UTF-8"), 
                                        symmetricKey,
                                        keyProps,
                                        Cipher.ENCRYPT_MODE);
            return Base64UrlUtility.encode(bytes);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private static byte[] processBytes(byte[] bytes, SecretKey symmetricKey, 
                                       SecretKeyProperties keyProps, int mode) {
        try {
            Cipher c = Cipher.getInstance(symmetricKey.getAlgorithm());
            if (keyProps == null || keyProps.getAlgoSpec() == null && keyProps.getSecureRandom() == null) {
                c.init(mode, symmetricKey);
            } else {
                AlgorithmParameterSpec algoSpec = keyProps.getAlgoSpec();
                SecureRandom random = keyProps.getSecureRandom();
                if (algoSpec == null) {
                    c.init(mode, symmetricKey, random);
                } else if (random == null) {
                    c.init(mode, symmetricKey, algoSpec);
                } else {
                    c.init(mode, symmetricKey, algoSpec, random);
                }
            }
            return c.doFinal(bytes);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private static SecretKey decodeSecretKey(String encodedSecretKey, String algo) {
        try {
            byte[] secretKeyBytes = decodeSequence(encodedSecretKey);
            return new SecretKeySpec(secretKeyBytes, algo);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private static byte[] decodeSequence(String encodedSecretKey) {
        try {
            return Base64UrlUtility.decode(encodedSecretKey);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static ServerAccessToken recreateToken(OAuthDataProvider provider,
                                                  String newTokenKey,
                                                  String decryptedSequence) {
        return recreateToken(provider, newTokenKey, decryptedSequence.split("\\" + SEP));
    }
    
    public static RefreshToken recreateRefreshToken(OAuthDataProvider provider,
                                                    String newTokenKey,
                                                    String decryptedSequence) {
        String[] parts = decryptedSequence.split("\\" + SEP);
        ServerAccessToken token = recreateToken(provider, newTokenKey, parts);
        return new RefreshToken(token, 
                                newTokenKey, 
                                parseSimpleList(parts[parts.length - 1]));
    }
    
    public static ServerAccessToken recreateToken(OAuthDataProvider provider,
                                                  String newTokenKey,
                                                  String[] parts) {
        
        
        @SuppressWarnings("serial")
        final ServerAccessToken newToken = new ServerAccessToken(provider.getClient(parts[4]),
                                                                 parts[1],
                                                                 newTokenKey == null ? parts[0] : newTokenKey,
                                                                 Long.valueOf(parts[2]),
                                                                 Long.valueOf(parts[3])) {
        };  
        
        newToken.setRefreshToken(getStringPart(parts[5]));
        newToken.setGrantType(getStringPart(parts[6]));
        newToken.setAudience(getStringPart(parts[7]));
        newToken.setParameters(parseSimpleMap(parts[8]));
        
        // Permissions
        if (!parts[9].trim().isEmpty()) {
            List<OAuthPermission> perms = new LinkedList<OAuthPermission>(); 
            String[] allPermParts = parts[9].split("\\.");
            for (int i = 0; i + 4 < allPermParts.length; i = i + 5) {
                OAuthPermission perm = new OAuthPermission(allPermParts[i], allPermParts[i + 1]);
                perm.setDefault(Boolean.valueOf(allPermParts[i + 2]));
                perm.setHttpVerbs(parseSimpleList(allPermParts[i + 3]));
                perm.setUris(parseSimpleList(allPermParts[i + 4]));
                perms.add(perm);
            }
            newToken.setScopes(perms);
        }
        //UserSubject:
        if (!parts[10].trim().isEmpty()) {
            String[] subjectParts = parts[10].split("\\.");
            UserSubject subject = new UserSubject(subjectParts[0], getStringPart(subjectParts[1]));
            subject.setRoles(parseSimpleList(subjectParts[2]));
            subject.setProperties(parseSimpleMap(subjectParts[3]));
            newToken.setSubject(subject);
        }
        
        
        return newToken;
    }
    
    private static String getStringPart(String str) {
        return "null".equals(str) ? null : str;
    }
    
    private static String prepareSimpleString(String str) {
        return str.trim().isEmpty() ? "" : str.substring(1, str.length() - 1);
    }
    
    private static List<String> parseSimpleList(String listStr) {
        String pureStringList = prepareSimpleString(listStr);
        if (pureStringList.isEmpty()) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(pureStringList.split(","));
        }
    }
    
    private static Map<String, String> parseSimpleMap(String mapStr) {
        Map<String, String> props = new HashMap<String, String>();
        List<String> entries = parseSimpleList(mapStr);
        for (String entry : entries) {
            String[] pair = entry.split("=");
            props.put(pair[0], pair[1]);
        }
        return props;
    }
    private static String tokenizeRefreshToken(RefreshToken token) {
        String seq = tokenizeServerToken(token);
        return seq + SEP + token.getAccessTokens().toString();
    }
    private static String tokenizeServerToken(ServerAccessToken token) {
        StringBuilder state = new StringBuilder();
        // 0: key
        state.append(token.getTokenKey());
        // 1: type
        state.append(SEP);
        state.append(token.getTokenType());
        // 2: expiresIn 
        state.append(SEP);
        state.append(token.getExpiresIn());
        // 3: issuedAt
        state.append(SEP);
        state.append(token.getIssuedAt());
        // 4: client id
        state.append(SEP);
        state.append(token.getClient().getClientId());
        // 5: refresh token
        state.append(SEP);
        state.append(token.getRefreshToken());
        // 6: grant type
        state.append(SEP);
        state.append(token.getGrantType());
        // 7: audience
        state.append(SEP);
        state.append(token.getAudience());
        // 8: other parameters
        state.append(SEP);
        // {key=value, key=value}
        state.append(token.getParameters().toString());
        // 9: permissions
        state.append(SEP);
        if (token.getScopes().isEmpty()) {
            state.append(" ");
        } else {
            for (OAuthPermission p : token.getScopes()) {
                // 9.1
                state.append(p.getPermission());
                state.append(".");
                // 9.2
                state.append(p.getDescription());
                state.append(".");
                // 9.3
                state.append(p.isDefault());
                state.append(".");
                // 9.4
                state.append(p.getHttpVerbs().toString());
                state.append(".");
                // 9.5
                state.append(p.getUris().toString());
            }
        }
        // 10: user subject
        state.append(SEP);
        if (token.getSubject() != null) {
             // 10.1
            state.append(token.getSubject().getLogin());
            state.append(".");
             // 10.2
            state.append(token.getSubject().getId());
            state.append(".");
             // 10.3
            state.append(token.getSubject().getRoles().toString());
            state.append(".");
             // 10.4
            state.append(token.getSubject().getProperties().toString());
        } else {
            state.append(" ");
        }
        
        return state.toString();
    }
    

}
