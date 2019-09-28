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

package org.apache.cxf.systest.jaxrs.security.oauth2.grants;

import java.net.URL;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.code.CodeVerifierTransformer;
import org.apache.cxf.rs.security.oauth2.grants.code.DigestCodeVerifier;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.systest.jaxrs.security.SecurityTestUtil;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils.AuthorizationCodeParameters;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Some tests for public clients.
 */
public class PublicClientTest extends AbstractBusClientServerTestBase {
    public static final String JCACHE_PORT = TestUtil.getPortNumber("jaxrs-oauth2-grants-jcache-public");
    public static final String JCACHE_PORT2 = TestUtil.getPortNumber("jaxrs-oauth2-grants2-jcache-public");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2GrantsJCache.class, true));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
    }

    @org.junit.Test
    public void testAuthorizationCodeGrant() throws Exception {
        URL busFile = PublicClientTest.class.getResource("publicclient.xml");

        String address = "https://localhost:" + JCACHE_PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client);
        assertNotNull(code);

        // Now get the access token - note services2 doesn't require basic auth
        String address2 = "https://localhost:" + JCACHE_PORT + "/services2/";
        client = WebClient.create(address2, OAuth2TestUtils.setupProviders(), busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
    }

    @org.junit.Test
    public void testPKCEPlain() throws Exception {
        URL busFile = PublicClientTest.class.getResource("publicclient.xml");

        String address = "https://localhost:" + JCACHE_PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
        parameters.setConsumerId("consumer-id");
        String codeVerifier = Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(32));
        parameters.setCodeChallenge(codeVerifier);
        parameters.setCodeChallengeMethod("plain");
        parameters.setResponseType("code");
        parameters.setPath("authorize/");

        String location = OAuth2TestUtils.getLocation(client, parameters);
        String code = OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);

        // Now get the access token - note services2 doesn't require basic auth
        String address2 = "https://localhost:" + JCACHE_PORT + "/services2/";
        client = WebClient.create(address2, OAuth2TestUtils.setupProviders(), busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code, "consumer-id", null, codeVerifier);
        assertNotNull(accessToken.getTokenKey());
    }

    @org.junit.Test
    public void testPKCEPlainMissingVerifier() throws Exception {
        URL busFile = PublicClientTest.class.getResource("publicclient.xml");

        String address = "https://localhost:" + JCACHE_PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
        parameters.setConsumerId("consumer-id");
        String codeVerifier = Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(32));
        parameters.setCodeChallenge(codeVerifier);
        parameters.setCodeChallengeMethod("plain");
        parameters.setResponseType("code");
        parameters.setPath("authorize/");

        String location = OAuth2TestUtils.getLocation(client, parameters);
        String code = OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);

        // Now get the access token - note services2 doesn't require basic auth
        String address2 = "https://localhost:" + JCACHE_PORT + "/services2/";
        client = WebClient.create(address2, OAuth2TestUtils.setupProviders(), busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        try {
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code, "consumer-id", null);
            fail("Failure expected on a missing verifier");
        } catch (Exception ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testPKCEPlainDifferentVerifier() throws Exception {
        URL busFile = PublicClientTest.class.getResource("publicclient.xml");

        String address = "https://localhost:" + JCACHE_PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
        parameters.setConsumerId("consumer-id");
        String codeVerifier = Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(32));
        parameters.setCodeChallenge(codeVerifier);
        parameters.setCodeChallengeMethod("plain");
        parameters.setResponseType("code");
        parameters.setPath("authorize/");

        String location = OAuth2TestUtils.getLocation(client, parameters);
        String code = OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);

        // Now get the access token - note services2 doesn't require basic auth
        String address2 = "https://localhost:" + JCACHE_PORT + "/services2/";
        client = WebClient.create(address2, OAuth2TestUtils.setupProviders(), busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        try {
            codeVerifier = Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(32));
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code, "consumer-id", null, codeVerifier);
            fail("Failure expected on a different verifier");
        } catch (Exception ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testPKCEDigest() throws Exception {
        URL busFile = PublicClientTest.class.getResource("publicclient.xml");

        String address = "https://localhost:" + JCACHE_PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
        parameters.setConsumerId("consumer-id");
        String codeVerifier = Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(32));
        CodeVerifierTransformer transformer = new DigestCodeVerifier();
        String codeChallenge = transformer.transformCodeVerifier(codeVerifier);
        parameters.setCodeChallenge(codeChallenge);
        parameters.setCodeChallengeMethod(transformer.getChallengeMethod());
        parameters.setResponseType("code");
        parameters.setPath("authorize/");

        String location = OAuth2TestUtils.getLocation(client, parameters);
        String code = OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);

        // Now get the access token - note services3 doesn't require basic auth
        String address2 = "https://localhost:" + JCACHE_PORT + "/services3/";
        client = WebClient.create(address2, OAuth2TestUtils.setupProviders(), busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code, "consumer-id", null, codeVerifier);
        assertNotNull(accessToken.getTokenKey());
    }

    @org.junit.Test
    public void testPKCEDigestMissingVerifier() throws Exception {
        URL busFile = PublicClientTest.class.getResource("publicclient.xml");

        String address = "https://localhost:" + JCACHE_PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
        parameters.setConsumerId("consumer-id");
        String codeVerifier = Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(32));
        CodeVerifierTransformer transformer = new DigestCodeVerifier();
        String codeChallenge = transformer.transformCodeVerifier(codeVerifier);
        parameters.setCodeChallenge(codeChallenge);
        parameters.setCodeChallengeMethod(transformer.getChallengeMethod());
        parameters.setResponseType("code");
        parameters.setPath("authorize/");

        String location = OAuth2TestUtils.getLocation(client, parameters);
        String code = OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);

        // Now get the access token - note services3 doesn't require basic auth
        String address2 = "https://localhost:" + JCACHE_PORT + "/services3/";
        client = WebClient.create(address2, OAuth2TestUtils.setupProviders(), busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        try {
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code, "consumer-id", null);
            fail("Failure expected on a missing verifier");
        } catch (Exception ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testPKCEDigestDifferentVerifier() throws Exception {
        URL busFile = PublicClientTest.class.getResource("publicclient.xml");

        String address = "https://localhost:" + JCACHE_PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
        parameters.setConsumerId("consumer-id");
        String codeVerifier = Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(32));
        CodeVerifierTransformer transformer = new DigestCodeVerifier();
        String codeChallenge = transformer.transformCodeVerifier(codeVerifier);
        parameters.setCodeChallenge(codeChallenge);
        parameters.setCodeChallengeMethod(transformer.getChallengeMethod());
        parameters.setResponseType("code");
        parameters.setPath("authorize/");

        String location = OAuth2TestUtils.getLocation(client, parameters);
        String code = OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);

        // Now get the access token - note services3 doesn't require basic auth
        String address2 = "https://localhost:" + JCACHE_PORT + "/services3/";
        client = WebClient.create(address2, OAuth2TestUtils.setupProviders(), busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        try {
            codeVerifier = Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(32));
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code, "consumer-id", null, codeVerifier);
            fail("Failure expected on a different verifier");
        } catch (Exception ex) {
            // expected
        }
    }

    //
    // Server implementations
    //

    public static class BookServerOAuth2GrantsJCache extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            BookServerOAuth2GrantsJCache.class.getResource("grants-server-public.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new BookServerOAuth2GrantsJCache();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

}
