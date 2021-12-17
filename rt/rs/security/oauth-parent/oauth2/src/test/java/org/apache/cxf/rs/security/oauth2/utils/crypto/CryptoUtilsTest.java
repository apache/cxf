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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;

import javax.crypto.SecretKey;

import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeRegistration;
import org.apache.cxf.rs.security.oauth2.grants.code.ServerAuthorizationCodeGrant;
import org.apache.cxf.rs.security.oauth2.tokens.bearer.BearerAccessToken;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.rt.security.crypto.KeyProperties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CryptoUtilsTest {

    private CodeGrantEncryptingDataProvider p;

    @Before
    public void setUp() throws Exception {
        p = new CodeGrantEncryptingDataProvider();
    }

    @After
    public void tearDown() {
        p = null;
    }

    @Test
    public void testEncryptDecryptToken() throws Exception {
        AccessTokenRegistration atr = prepareTokenRegistration();

        // encrypt
        ServerAccessToken token = p.createAccessToken(atr);
        ServerAccessToken token2 = p.getAccessToken(token.getTokenKey());

        // compare tokens
        compareAccessTokens(token, token2);
    }

    @Test
    public void testEncryptDecryptCodeGrant() throws Exception {
        AuthorizationCodeRegistration codeReg = new AuthorizationCodeRegistration();
        codeReg.setAudience("http://bar");
        codeReg.setClient(p.getClient("1"));
        ServerAuthorizationCodeGrant grant = p.createCodeGrant(codeReg);
        ServerAuthorizationCodeGrant grant2 = p.removeCodeGrant(grant.getCode());
        assertEquals("http://bar", grant2.getAudience());
        assertEquals("1", grant2.getClient().getClientId());
    }

    @Test
    public void testBearerTokenCertAndSecretKey() throws Exception {
        AccessTokenRegistration atr = prepareTokenRegistration();
        BearerAccessToken token = p.createAccessTokenInternal(atr);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = kpg.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        SecretKey secretKey = CryptoUtils.getSecretKey("AES");
        String encryptedSecretKey = CryptoUtils.encryptSecretKey(secretKey, publicKey);

        String encryptedToken = ModelEncryptionSupport.encryptAccessToken(token, secretKey);
        token.setTokenKey(encryptedToken);
        SecretKey decryptedSecretKey = CryptoUtils.decryptSecretKey(encryptedSecretKey, privateKey);
        ServerAccessToken token2 = ModelEncryptionSupport.decryptAccessToken(p, encryptedToken, decryptedSecretKey);
        // compare tokens
        compareAccessTokens(token, token2);
    }

    @Test
    public void testBearerTokenJSON() throws Exception {
        AccessTokenRegistration atr = prepareTokenRegistration();

        BearerAccessToken token = p.createAccessTokenInternal(atr);
        JSONProvider<BearerAccessToken> jsonp = new JSONProvider<>();
        jsonp.setMarshallAsJaxbElement(true);
        jsonp.setUnmarshallAsJaxbElement(true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        jsonp.writeTo(token, BearerAccessToken.class, new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE,
                      new MetadataMap<String, Object>(), bos);

        String encrypted = CryptoUtils.encryptSequence(bos.toString(), p.key);
        String decrypted = CryptoUtils.decryptSequence(encrypted, p.key);
        ServerAccessToken token2 = jsonp.readFrom(BearerAccessToken.class, BearerAccessToken.class,
                                                  new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE,
                                                  new MetadataMap<String, String>(),
                                                  new ByteArrayInputStream(decrypted.getBytes()));

        // compare tokens
        compareAccessTokens(token, token2);
    }

    @Test
    public void testBearerTokenJSONCertificate() throws Exception {
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            return;
        }
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = kpg.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        AccessTokenRegistration atr = prepareTokenRegistration();

        BearerAccessToken token = p.createAccessTokenInternal(atr);
        JSONProvider<BearerAccessToken> jsonp = new JSONProvider<>();
        jsonp.setMarshallAsJaxbElement(true);
        jsonp.setUnmarshallAsJaxbElement(true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        jsonp.writeTo(token, BearerAccessToken.class, new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE,
                      new MetadataMap<String, Object>(), bos);

        KeyProperties props1 = new KeyProperties(publicKey.getAlgorithm());
        String encrypted = CryptoUtils.encryptSequence(bos.toString(), publicKey, props1);
        KeyProperties props2 = new KeyProperties(privateKey.getAlgorithm());
        String decrypted = CryptoUtils.decryptSequence(encrypted, privateKey, props2);
        ServerAccessToken token2 = jsonp.readFrom(BearerAccessToken.class, BearerAccessToken.class,
                                                  new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE,
                                                  new MetadataMap<String, String>(),
                                                  new ByteArrayInputStream(decrypted.getBytes()));

        // compare tokens
        compareAccessTokens(token, token2);
    }

    @Test
    public void testClientJSON() throws Exception {
        Client c = new Client("client", "secret", true);
        c.setSubject(new UserSubject("subject", "id"));
        JSONProvider<Client> jsonp = new JSONProvider<>();
        jsonp.setMarshallAsJaxbElement(true);
        jsonp.setUnmarshallAsJaxbElement(true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        jsonp.writeTo(c, Client.class, new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE,
                      new MetadataMap<String, Object>(), bos);
        String encrypted = CryptoUtils.encryptSequence(bos.toString(), p.key);
        String decrypted = CryptoUtils.decryptSequence(encrypted, p.key);
        Client c2 = jsonp.readFrom(Client.class, Client.class,
                                                  new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE,
                                                  new MetadataMap<String, String>(),
                                                  new ByteArrayInputStream(decrypted.getBytes()));

        assertEquals(c.getClientId(), c2.getClientId());
        assertEquals(c.getClientSecret(), c2.getClientSecret());
        assertTrue(c2.isConfidential());
        assertEquals("subject", c2.getSubject().getLogin());
        assertEquals("id", c2.getSubject().getId());
    }


    @Test
    public void testCodeGrantJSON() throws Exception {
        Client c = new Client("client", "secret", true);
        ServerAuthorizationCodeGrant grant = new ServerAuthorizationCodeGrant(c, "code", 1, 2);
        JSONProvider<ServerAuthorizationCodeGrant> jsonp = new JSONProvider<>();
        jsonp.setMarshallAsJaxbElement(true);
        jsonp.setUnmarshallAsJaxbElement(true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        jsonp.writeTo(grant, ServerAuthorizationCodeGrant.class, new Annotation[]{},
                      MediaType.APPLICATION_JSON_TYPE,
                      new MetadataMap<String, Object>(), bos);

        String encrypted = CryptoUtils.encryptSequence(bos.toString(), p.key);
        String decrypted = CryptoUtils.decryptSequence(encrypted, p.key);
        ServerAuthorizationCodeGrant grant2 = jsonp.readFrom(ServerAuthorizationCodeGrant.class,
                                                             Client.class,
                                                  new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE,
                                                  new MetadataMap<String, String>(),
                                                  new ByteArrayInputStream(decrypted.getBytes()));
        assertEquals("code", grant2.getCode());
        assertEquals(1, grant2.getExpiresIn());
        assertEquals(2, grant2.getIssuedAt());
    }

    private void compareAccessTokens(ServerAccessToken token, ServerAccessToken token2) {
        assertEquals(token.getTokenKey(), token2.getTokenKey());
        assertEquals(token.getTokenType(), token2.getTokenType());
        assertEquals(token.getIssuedAt(), token2.getIssuedAt());
        assertEquals(token.getExpiresIn(), token2.getExpiresIn());
        Client regClient1 = token.getClient();
        Client regClient2 = token2.getClient();
        assertEquals(regClient1.getClientId(), regClient2.getClientId());
        assertNull(regClient2.getApplicationDescription());
        UserSubject endUser1 = token.getSubject();
        UserSubject endUser2 = token2.getSubject();
        assertEquals(endUser1.getLogin(), endUser2.getLogin());
        assertEquals(endUser1.getId(), endUser2.getId());
        assertEquals(endUser1.getRoles(), endUser2.getRoles());

        assertEquals(token.getRefreshToken(), token2.getRefreshToken());
        assertEquals(token.getAudiences(), token2.getAudiences());
        assertEquals(token.getGrantType(), token2.getGrantType());
        assertEquals(token.getParameters(), token2.getParameters());

        List<OAuthPermission> permissions = token.getScopes();
        List<OAuthPermission> permissions2 = token2.getScopes();
        assertEquals(1, permissions.size());
        assertEquals(1, permissions2.size());
        OAuthPermission perm1 = permissions.get(0);
        OAuthPermission perm2 = permissions2.get(0);
        assertEquals(perm1.getPermission(), perm2.getPermission());
        assertEquals(perm1.getDescription(), perm2.getDescription());

        RefreshToken refreshToken =
            ModelEncryptionSupport.decryptRefreshToken(p, token2.getRefreshToken(), p.key);
        assertEquals(1200L, refreshToken.getExpiresIn());
    }

    private AccessTokenRegistration prepareTokenRegistration() {
        AccessTokenRegistration atr = new AccessTokenRegistration();
        Client regClient = p.getClient("1");
        atr.setClient(regClient);
        atr.setGrantType("code");
        atr.setAudiences(Collections.singletonList("http://localhost"));
        UserSubject endUser = new UserSubject("Barry", "BarryId");
        atr.setSubject(endUser);
        endUser.setRoles(Collections.singletonList("role1"));
        return atr;
    }

// TODO: remove once the wiki documentation is updated
//  KeyStore keyStore = loadKeyStore();
//  Certificate cert = keyStore.getCertificate("alice");
//  PublicKey publicKey = cert.getPublicKey();
//  KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry)
//      keyStore.getEntry("alice", new KeyStore.PasswordProtection(
//           new char[]{'p', 'a', 's', 's', 'w', 'o', 'r', 'd'}));
//  PrivateKey privateKey = pkEntry.getPrivateKey();


//    private KeyStore loadKeyStore() throws Exception {
//        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
//        InputStream is = this.getClass().getResourceAsStream("alice.jks");
//        ks.load(is, new char[]{'p', 'a', 's', 's', 'w', 'o', 'r', 'd'});
//        return ks;
//    }
}