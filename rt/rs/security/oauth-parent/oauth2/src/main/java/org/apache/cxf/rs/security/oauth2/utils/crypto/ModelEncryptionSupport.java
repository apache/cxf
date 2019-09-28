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

package org.apache.cxf.rs.security.oauth2.utils.crypto;

import java.security.Key;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.grants.code.ServerAuthorizationCodeGrant;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.rt.security.crypto.KeyProperties;


/**
 * Default Model Encryption helpers
 */
public final class ModelEncryptionSupport {
    public static final String SEP = "|";
    private ModelEncryptionSupport() {
    }

    public static String encryptClient(Client client, Key secretKey) throws SecurityException {
        return encryptClient(client, secretKey, null);
    }

    public static String encryptClient(Client client, Key secretKey,
                                       KeyProperties props) throws SecurityException {
        String tokenSequence = tokenizeClient(client);
        return CryptoUtils.encryptSequence(tokenSequence, secretKey, props);
    }

    public static String encryptAccessToken(ServerAccessToken token, Key secretKey) throws SecurityException {
        return encryptAccessToken(token, secretKey, null);
    }

    public static String encryptAccessToken(ServerAccessToken token, Key secretKey,
                                            KeyProperties props) throws SecurityException {
        String tokenSequence = tokenizeServerToken(token);
        return CryptoUtils.encryptSequence(tokenSequence, secretKey, props);
    }

    public static String encryptRefreshToken(RefreshToken token, Key secretKey) throws SecurityException {
        return encryptRefreshToken(token, secretKey, null);
    }

    public static String encryptRefreshToken(RefreshToken token, Key secretKey,
                                             KeyProperties props) throws SecurityException {
        String tokenSequence = tokenizeRefreshToken(token);

        return CryptoUtils.encryptSequence(tokenSequence, secretKey, props);
    }

    public static String encryptCodeGrant(ServerAuthorizationCodeGrant grant, Key secretKey)
        throws SecurityException {
        return encryptCodeGrant(grant, secretKey, null);
    }

    public static String encryptCodeGrant(ServerAuthorizationCodeGrant grant, Key secretKey,
                                          KeyProperties props) throws SecurityException {
        String tokenSequence = tokenizeCodeGrant(grant);

        return CryptoUtils.encryptSequence(tokenSequence, secretKey, props);
    }

    public static Client decryptClient(String encodedSequence, String encodedSecretKey)
        throws SecurityException {
        return decryptClient(encodedSequence, encodedSecretKey, new KeyProperties("AES"));
    }

    public static Client decryptClient(String encodedSequence, String encodedSecretKey,
                                       KeyProperties props) throws SecurityException {
        SecretKey key = CryptoUtils.decodeSecretKey(encodedSecretKey, props.getKeyAlgo());
        Client client = decryptClient(encodedSequence, key, props);

        // Clean the secret key from memory when we're done
        try {
            key.destroy();
        } catch (DestroyFailedException ex) {
            // ignore
        }

        return client;
    }

    public static Client decryptClient(String encodedSequence, Key secretKey) throws SecurityException {
        return decryptClient(encodedSequence, secretKey, null);
    }

    public static Client decryptClient(String encodedData, Key secretKey,
                                       KeyProperties props) throws SecurityException {
        String decryptedSequence = CryptoUtils.decryptSequence(encodedData, secretKey, props);
        return recreateClient(decryptedSequence);
    }

    public static ServerAccessToken decryptAccessToken(OAuthDataProvider provider,
                                                 String encodedToken,
                                                 String encodedSecretKey) throws SecurityException {
        return decryptAccessToken(provider, encodedToken, encodedSecretKey, new KeyProperties("AES"));
    }

    public static ServerAccessToken decryptAccessToken(OAuthDataProvider provider,
                                                 String encodedToken,
                                                 String encodedSecretKey,
                                                 KeyProperties props) throws SecurityException {
        SecretKey key = CryptoUtils.decodeSecretKey(encodedSecretKey, props.getKeyAlgo());
        ServerAccessToken serverAccessToken = decryptAccessToken(provider, encodedToken, key, props);

        // Clean the secret key from memory when we're done
        try {
            key.destroy();
        } catch (DestroyFailedException ex) {
            // ignore
        }

        return serverAccessToken;
    }

    public static ServerAccessToken decryptAccessToken(OAuthDataProvider provider,
                                                 String encodedToken,
                                                 Key secretKey) throws SecurityException {
        return decryptAccessToken(provider, encodedToken, secretKey, null);
    }

    public static ServerAccessToken decryptAccessToken(OAuthDataProvider provider,
                                                 String encodedData,
                                                 Key secretKey,
                                                 KeyProperties props) throws SecurityException {
        String decryptedSequence = CryptoUtils.decryptSequence(encodedData, secretKey, props);
        return recreateAccessToken(provider, encodedData, decryptedSequence);
    }

    public static RefreshToken decryptRefreshToken(OAuthDataProvider provider,
                                                   String encodedToken,
                                                   String encodedSecretKey) throws SecurityException {
        return decryptRefreshToken(provider, encodedToken, encodedSecretKey, new KeyProperties("AES"));
    }

    public static RefreshToken decryptRefreshToken(OAuthDataProvider provider,
                                                  String encodedToken,
                                                  String encodedSecretKey,
                                                  KeyProperties props) throws SecurityException {
        SecretKey key = CryptoUtils.decodeSecretKey(encodedSecretKey, props.getKeyAlgo());
        RefreshToken refreshToken = decryptRefreshToken(provider, encodedToken, key, props);

        // Clean the secret key from memory when we're done
        try {
            key.destroy();
        } catch (DestroyFailedException ex) {
            // ignore
        }

        return refreshToken;
    }

    public static RefreshToken decryptRefreshToken(OAuthDataProvider provider,
                                                   String encodedToken,
                                                   Key key) throws SecurityException {
        return decryptRefreshToken(provider, encodedToken, key, null);
    }

    public static RefreshToken decryptRefreshToken(OAuthDataProvider provider,
                                                   String encodedData,
                                                   Key key,
                                                   KeyProperties props) throws SecurityException {
        String decryptedSequence = CryptoUtils.decryptSequence(encodedData, key, props);
        return recreateRefreshToken(provider, encodedData, decryptedSequence);
    }

    public static ServerAuthorizationCodeGrant decryptCodeGrant(OAuthDataProvider provider,
                                                   String encodedToken,
                                                   String encodedSecretKey) throws SecurityException {
        return decryptCodeGrant(provider, encodedToken, encodedSecretKey, new KeyProperties("AES"));
    }

    public static ServerAuthorizationCodeGrant decryptCodeGrant(OAuthDataProvider provider,
                                                  String encodedToken,
                                                  String encodedSecretKey,
                                                  KeyProperties props) throws SecurityException {
        SecretKey key = CryptoUtils.decodeSecretKey(encodedSecretKey, props.getKeyAlgo());
        ServerAuthorizationCodeGrant authzCodeGrant = decryptCodeGrant(provider, encodedToken, key, props);

        // Clean the secret key from memory when we're done
        try {
            key.destroy();
        } catch (DestroyFailedException ex) {
            // ignore
        }

        return authzCodeGrant;
    }

    public static ServerAuthorizationCodeGrant decryptCodeGrant(OAuthDataProvider provider,
                                                   String encodedToken,
                                                   Key key) throws SecurityException {
        return decryptCodeGrant(provider, encodedToken, key, null);
    }

    public static ServerAuthorizationCodeGrant decryptCodeGrant(OAuthDataProvider provider,
                                                   String encodedData,
                                                   Key key,
                                                   KeyProperties props) throws SecurityException {
        String decryptedSequence = CryptoUtils.decryptSequence(encodedData, key, props);
        return recreateCodeGrant(provider, decryptedSequence);
    }

    public static ServerAccessToken recreateAccessToken(OAuthDataProvider provider,
                                                  String newTokenKey,
                                                  String decryptedSequence) throws SecurityException {
        return recreateAccessToken(provider, newTokenKey, getParts(decryptedSequence));
    }

    public static RefreshToken recreateRefreshToken(OAuthDataProvider provider,
                                                    String newTokenKey,
                                                    String decryptedSequence) throws SecurityException {
        String[] parts = getParts(decryptedSequence);
        ServerAccessToken token = recreateAccessToken(provider, newTokenKey, parts);
        return new RefreshToken(token,
                                newTokenKey,
                                parseSimpleList(parts[parts.length - 1]));
    }

    public static ServerAuthorizationCodeGrant recreateCodeGrant(OAuthDataProvider provider,
        String decryptedSequence) throws SecurityException {
        return recreateCodeGrantInternal(provider, decryptedSequence);
    }

    public static Client recreateClient(String sequence) throws SecurityException {
        return recreateClientInternal(sequence);
    }

    private static ServerAccessToken recreateAccessToken(OAuthDataProvider provider,
                                                  String newTokenKey,
                                                  String[] parts) {


        @SuppressWarnings("serial")
        final ServerAccessToken newToken = new ServerAccessToken(provider.getClient(parts[4]),
                                                                 parts[1],
                                                                 newTokenKey == null ? parts[0] : newTokenKey,
                                                                 Long.parseLong(parts[2]),
                                                                 Long.parseLong(parts[3])) {
        };

        newToken.setRefreshToken(getStringPart(parts[5]));
        newToken.setGrantType(getStringPart(parts[6]));
        newToken.setAudiences(parseSimpleList(parts[7]));
        newToken.setParameters(parseSimpleMap(parts[8]));

        // Permissions
        if (!parts[9].trim().isEmpty()) {
            List<OAuthPermission> perms = new LinkedList<>();
            String[] allPermParts = parts[9].split("\\.");
            for (int i = 0; i + 4 < allPermParts.length; i = i + 5) {
                OAuthPermission perm = new OAuthPermission(allPermParts[i], allPermParts[i + 1]);
                perm.setDefaultPermission(Boolean.parseBoolean(allPermParts[i + 2]));
                perm.setHttpVerbs(parseSimpleList(allPermParts[i + 3]));
                perm.setUris(parseSimpleList(allPermParts[i + 4]));
                perms.add(perm);
            }
            newToken.setScopes(perms);
        }
        //Client verifier:
        newToken.setClientCodeVerifier(parts[10]);
        //UserSubject:
        newToken.setSubject(recreateUserSubject(parts[11]));

        newToken.setExtraProperties(parseSimpleMap(parts[12]));

        return newToken;
    }

    private static String tokenizeRefreshToken(RefreshToken token) {
        String seq = tokenizeServerToken(token);
        return seq + SEP + token.getAccessTokens().toString();
    }

    private static String tokenizeServerToken(ServerAccessToken token) {
        StringBuilder state = new StringBuilder();
        // 0: key
        state.append(tokenizeString(token.getTokenKey()));
        // 1: type
        state.append(SEP);
        state.append(tokenizeString(token.getTokenType()));
        // 2: expiresIn
        state.append(SEP);
        state.append(token.getExpiresIn());
        // 3: issuedAt
        state.append(SEP);
        state.append(token.getIssuedAt());
        // 4: client id
        state.append(SEP);
        state.append(tokenizeString(token.getClient().getClientId()));
        // 5: refresh token
        state.append(SEP);
        state.append(tokenizeString(token.getRefreshToken()));
        // 6: grant type
        state.append(SEP);
        state.append(tokenizeString(token.getGrantType()));
        // 7: audience
        state.append(SEP);
        state.append(token.getAudiences().toString());
        // 8: other parameters
        state.append(SEP);
        // {key=value, key=value}
        state.append(token.getParameters().toString());
        // 9: permissions
        state.append(SEP);
        if (token.getScopes().isEmpty()) {
            state.append(' ');
        } else {
            for (OAuthPermission p : token.getScopes()) {
                // 9.1
                state.append(tokenizeString(p.getPermission()));
                state.append('.');
                // 9.2
                state.append(tokenizeString(p.getDescription()));
                state.append('.');
                // 9.3
                state.append(p.isDefaultPermission());
                state.append('.');
                // 9.4
                state.append(p.getHttpVerbs().toString());
                state.append('.');
                // 9.5
                state.append(p.getUris().toString());
            }
        }
        state.append(SEP);
        // 10: code verifier
        state.append(tokenizeString(token.getClientCodeVerifier()));
        state.append(SEP);
        // 11: user subject
        tokenizeUserSubject(state, token.getSubject());
        // 13: extra properties
        state.append(SEP);
        // {key=value, key=value}
        state.append(token.getExtraProperties().toString());
        return state.toString();
    }


    private static Client recreateClientInternal(String sequence) {
        String[] parts = getParts(sequence);
        Client c = new Client(parts[0],
                              parts[1],
                              Boolean.parseBoolean(parts[2]),
                              getStringPart(parts[3]), getStringPart(parts[4]));
        c.setApplicationDescription(getStringPart(parts[5]));
        c.setApplicationLogoUri(getStringPart(parts[6]));
        c.setApplicationCertificates(parseSimpleList(parts[7]));
        c.setAllowedGrantTypes(parseSimpleList(parts[8]));
        c.setRedirectUris(parseSimpleList(parts[9]));
        c.setRegisteredScopes(parseSimpleList(parts[10]));
        c.setRegisteredAudiences(parseSimpleList(parts[11]));
        c.setProperties(parseSimpleMap(parts[12]));
        c.setSubject(recreateUserSubject(parts[13]));
        return c;
    }
    private static String tokenizeClient(Client client) {
        StringBuilder state = new StringBuilder();
        // 0: id
        state.append(tokenizeString(client.getClientId()));
        state.append(SEP);
        // 1: secret
        state.append(tokenizeString(client.getClientSecret()));
        state.append(SEP);
        // 2: confidentiality
        state.append(client.isConfidential());
        state.append(SEP);
        // 3: app name
        state.append(tokenizeString(client.getApplicationName()));
        state.append(SEP);
        // 4: app web URI
        state.append(tokenizeString(client.getApplicationWebUri()));
        state.append(SEP);
        // 5: app description
        state.append(tokenizeString(client.getApplicationDescription()));
        state.append(SEP);
        // 6: app logo URI
        state.append(tokenizeString(client.getApplicationLogoUri()));
        state.append(SEP);
        // 7: app certificates
        state.append(client.getApplicationCertificates());
        state.append(SEP);
        // 8: grants
        state.append(client.getAllowedGrantTypes().toString());
        state.append(SEP);
        // 9: redirect URIs
        state.append(client.getRedirectUris().toString());
        state.append(SEP);
        // 10: registered scopes
        state.append(client.getRegisteredScopes().toString());
        state.append(SEP);
        // 11: registered audiences
        state.append(client.getRegisteredAudiences().toString());
        state.append(SEP);
        // 12: properties
        state.append(client.getProperties().toString());
        state.append(SEP);
        // 13: subject
        tokenizeUserSubject(state, client.getSubject());

        return state.toString();
    }
    private static ServerAuthorizationCodeGrant recreateCodeGrantInternal(OAuthDataProvider provider,
                                                                          String sequence) {
        String[] parts = getParts(sequence);
        ServerAuthorizationCodeGrant grant = new ServerAuthorizationCodeGrant(provider.getClient(parts[0]),
                                                                              parts[1],
                                                                              Long.parseLong(parts[2]),
                                                                              Long.parseLong(parts[3]));
        grant.setRedirectUri(getStringPart(parts[4]));
        grant.setAudience(getStringPart(parts[5]));
        grant.setClientCodeChallenge(getStringPart(parts[6]));
        grant.setApprovedScopes(parseSimpleList(parts[7]));
        grant.setSubject(recreateUserSubject(parts[8]));
        grant.setExtraProperties(parseSimpleMap(parts[9]));
        return grant;
    }
    private static String tokenizeCodeGrant(ServerAuthorizationCodeGrant grant) {
        StringBuilder state = new StringBuilder();
        // 0: client id
        state.append(grant.getClient().getClientId());
        state.append(SEP);
        // 1: code
        state.append(tokenizeString(grant.getCode()));
        state.append(SEP);
        // 2: expiresIn
        state.append(grant.getExpiresIn());
        state.append(SEP);
        // 3: issuedAt
        state.append(grant.getIssuedAt());
        state.append(SEP);
        // 4: redirect URI
        state.append(tokenizeString(grant.getRedirectUri()));
        state.append(SEP);
        // 5: audience
        state.append(tokenizeString(grant.getAudience()));
        state.append(SEP);
        // 6: code challenge
        state.append(tokenizeString(grant.getClientCodeChallenge()));
        state.append(SEP);
        // 7: approved scopes
        state.append(grant.getApprovedScopes().toString());
        state.append(SEP);
        // 8: subject
        tokenizeUserSubject(state, grant.getSubject());
        // 9: extra properties
        state.append(SEP);
        // {key=value, key=value}
        state.append(grant.getExtraProperties().toString());
        return state.toString();
    }

    public static String getStringPart(String str) {
        return " ".equals(str) ? null : str;
    }

    private static String prepareSimpleString(String str) {
        return str.trim().isEmpty() ? "" : str.substring(1, str.length() - 1);
    }

    private static List<String> parseSimpleList(String listStr) {
        String pureStringList = prepareSimpleString(listStr);
        if (pureStringList.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(pureStringList.split(","));
    }

    public static Map<String, String> parseSimpleMap(String mapStr) {
        Map<String, String> props = new HashMap<>();
        List<String> entries = parseSimpleList(mapStr);
        for (String entry : entries) {
            String[] pair = entry.split("=");
            props.put(pair[0], pair[1]);
        }
        return props;
    }

    public static String[] getParts(String sequence) {
        return sequence.split("\\" + SEP);
    }

    private static UserSubject recreateUserSubject(String sequence) {
        UserSubject subject = null;
        if (!sequence.trim().isEmpty()) {
            String[] subjectParts = sequence.split("\\.");
            subject = new UserSubject(getStringPart(subjectParts[0]), getStringPart(subjectParts[1]));
            subject.setRoles(parseSimpleList(subjectParts[2]));
            subject.setProperties(parseSimpleMap(subjectParts[3]));
        }
        return subject;


    }

    private static void tokenizeUserSubject(StringBuilder state, UserSubject subject) {
        if (subject != null) {
            // 1
            state.append(tokenizeString(subject.getLogin()));
            state.append('.');
            // 2
            state.append(tokenizeString(subject.getId()));
            state.append('.');
            // 3
            state.append(subject.getRoles().toString());
            state.append('.');
            // 4
            state.append(subject.getProperties().toString());
        } else {
            state.append(' ');
        }
    }

    public static String tokenizeString(String str) {
        return str != null ? str : " ";
    }
}
