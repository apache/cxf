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
package org.apache.cxf.systest.jaxrs.security.oidc;

import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import javax.ws.rs.core.Response;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.common.UserInfo;
import org.apache.cxf.systest.jaxrs.security.SecurityTestUtil;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for the UserInfo Service in OpenId Connect. This can be used to return the User's claims given
 * an access token. The tests are run multiple times with different OAuthDataProvider implementations:
 * a) JCACHE_PORT - JCache
 * b) JWT_JCACHE_PORT - JCache with useJwtFormatForAccessTokens enabled
 * c) JPA_PORT - JPA provider
 * d) JWT_NON_PERSIST_JCACHE_PORT-  JCache with useJwtFormatForAccessTokens + !persistJwtEncoding
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class UserInfoTest extends AbstractBusClientServerTestBase {

    static final String JCACHE_PORT = TestUtil.getPortNumber("jaxrs-userinfo-jcache");
    static final String JCACHE_JWT_PORT = TestUtil.getPortNumber("jaxrs-userinfo-jcache-jwt");
    static final String JPA_PORT = TestUtil.getPortNumber("jaxrs-userinfo-jpa");
    static final String JWT_NON_PERSIST_JCACHE_PORT =
        TestUtil.getPortNumber("jaxrs-userinfo-jcache-jwt-non-persist");

    final String port;

    public UserInfoTest(String port) {
        this.port = port;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(UserInfoServerJCache.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(UserInfoServerJCacheJWT.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(UserInfoServerJPA.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(UserInfoServerJCacheJWTNonPersist.class, true)
        );
    }

    @AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
    }

    @Parameters(name = "{0}")
    public static Collection<String> data() {

        return Arrays.asList(JCACHE_PORT, JCACHE_JWT_PORT, JPA_PORT, JWT_NON_PERSIST_JCACHE_PORT);
    }

    @org.junit.Test
    public void testPlainUserInfo() throws Exception {
        URL busFile = UserInfoTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/oidc";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());

        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));

        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);

        // Now invoke on the UserInfo service with the access token
        String userInfoAddress = "https://localhost:" + port + "/services/plain/userinfo";
        WebClient userInfoClient = WebClient.create(userInfoAddress, OAuth2TestUtils.setupProviders(),
                                                    busFile.toString());
        userInfoClient.accept("application/json");
        userInfoClient.header("Authorization", "Bearer " + accessToken.getTokenKey());

        Response serviceResponse = userInfoClient.get();
        assertEquals(serviceResponse.getStatus(), 200);

        UserInfo userInfo = serviceResponse.readEntity(UserInfo.class);
        assertNotNull(userInfo);

        assertEquals("alice", userInfo.getSubject());
        assertEquals("consumer-id", userInfo.getAudience());
    }

    @org.junit.Test
    public void testSignedUserInfo() throws Exception {
        URL busFile = UserInfoTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/oidc";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());

        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));

        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);

        // Now invoke on the UserInfo service with the access token
        String userInfoAddress = "https://localhost:" + port + "/services/signed/userinfo";
        WebClient userInfoClient = WebClient.create(userInfoAddress, OAuth2TestUtils.setupProviders(),
                                                    busFile.toString());
        userInfoClient.accept("application/jwt");
        userInfoClient.header("Authorization", "Bearer " + accessToken.getTokenKey());

        Response serviceResponse = userInfoClient.get();
        assertEquals(serviceResponse.getStatus(), 200);

        String token = serviceResponse.readEntity(String.class);
        assertNotNull(token);

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();

        assertEquals("alice", jwt.getClaim(JwtConstants.CLAIM_SUBJECT));
        assertEquals("consumer-id", jwt.getClaim(JwtConstants.CLAIM_AUDIENCE));

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        Certificate cert = keystore.getCertificate("alice");
        assertNotNull(cert);

        assertTrue(jwtConsumer.verifySignatureWith((X509Certificate)cert,
                                                          SignatureAlgorithm.RS256));
    }

    @org.junit.Test
    public void testEncryptedUserInfo() throws Exception {
        URL busFile = UserInfoTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/oidc";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());

        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));

        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);

        // Now invoke on the UserInfo service with the access token
        String userInfoAddress = "https://localhost:" + port + "/services/encrypted/userinfo";
        WebClient userInfoClient = WebClient.create(userInfoAddress, OAuth2TestUtils.setupProviders(),
                                                    busFile.toString());
        userInfoClient.accept("application/jwt");
        userInfoClient.header("Authorization", "Bearer " + accessToken.getTokenKey());

        Response serviceResponse = userInfoClient.get();
        assertEquals(200, serviceResponse.getStatus());

        String token = serviceResponse.readEntity(String.class);
        assertNotNull(token);

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());

        JweJwtCompactConsumer jwtConsumer = new JweJwtCompactConsumer(token);
        PrivateKey privateKey = (PrivateKey)keystore.getKey("alice", "password".toCharArray());
        JwtToken jwt = jwtConsumer.decryptWith(privateKey);

        assertEquals("alice", jwt.getClaim(JwtConstants.CLAIM_SUBJECT));
        assertEquals("consumer-id", jwt.getClaim(JwtConstants.CLAIM_AUDIENCE));
    }

    private void validateIdToken(String idToken, String nonce)
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(idToken);
        JwtToken jwt = jwtConsumer.getJwtToken();

        // Validate claims
        assertEquals("alice", jwt.getClaim(JwtConstants.CLAIM_SUBJECT));
        assertEquals("OIDC IdP", jwt.getClaim(JwtConstants.CLAIM_ISSUER));
        assertEquals("consumer-id", jwt.getClaim(JwtConstants.CLAIM_AUDIENCE));
        assertNotNull(jwt.getClaim(JwtConstants.CLAIM_EXPIRY));
        assertNotNull(jwt.getClaim(JwtConstants.CLAIM_ISSUED_AT));
        if (nonce != null) {
            assertEquals(nonce, jwt.getClaim(IdToken.NONCE_CLAIM));
        }

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        Certificate cert = keystore.getCertificate("alice");
        assertNotNull(cert);

        assertTrue(jwtConsumer.verifySignatureWith((X509Certificate)cert,
                                                          SignatureAlgorithm.RS256));
    }

    //
    // Server implementations
    //

    public static class UserInfoServerJCache extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            UserInfoServerJCache.class.getResource("userinfo-server-jcache.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new UserInfoServerJCache();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class UserInfoServerJCacheJWT extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            UserInfoServerJCacheJWT.class.getResource("userinfo-server-jcache-jwt.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new UserInfoServerJCacheJWT();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class UserInfoServerJPA extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            UserInfoServerJPA.class.getResource("userinfo-server-jpa.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new UserInfoServerJPA();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class UserInfoServerJCacheJWTNonPersist extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            UserInfoServerJCacheJWTNonPersist.class.getResource("userinfo-server-jcache-jwt-non-persist.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new UserInfoServerJCacheJWTNonPersist();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }
}
