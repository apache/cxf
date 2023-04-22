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

import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.code.CodeVerifierTransformer;
import org.apache.cxf.rs.security.oauth2.grants.code.DigestCodeVerifier;
import org.apache.cxf.rs.security.oauth2.grants.code.PlainCodeVerifier;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils.AuthorizationCodeParameters;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Some tests for public clients.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class PublicClientTest extends AbstractClientServerTestBase {
    public static final String JCACHE_PORT = TestUtil.getPortNumber("jaxrs-oauth2-grants-jcache-public");
    public static final String JCACHE_PORT2 = TestUtil.getPortNumber("jaxrs-oauth2-grants2-jcache-public");

    public static final String JCACHE_PORT_SESSION =
            TestUtil.getPortNumber("jaxrs-oauth2-grants-jcache-public-session");
    public static final String JCACHE_PORT_SESSION2 =
            TestUtil.getPortNumber("jaxrs-oauth2-grants2-jcache-public-session");

    final String port;

    private final String tokenServiceAddress;

    public PublicClientTest(String port) {
        this.port = port;
        // services2 doesn't require basic auth
        tokenServiceAddress = "https://localhost:" + port + "/services2/";
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                launchServer(BookServerOAuth2GrantsJCache.class, true));
        assertTrue("server did not launch correctly",
                launchServer(BookServerOAuth2GrantsJCacheSession.class, true));
    }

    @Parameterized.Parameters(name = "{0}")
    public static String[] data() {
        return new String[] {
            JCACHE_PORT,
            JCACHE_PORT_SESSION};
    }

    @org.junit.Test
    public void testAuthorizationCodeGrantNoRedirectURI() throws Exception {
        URL busFile = PublicClientTest.class.getResource("publicclient.xml");

        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        try {
            // Get Authorization Code
            AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
            parameters.setConsumerId("fredPublic");
            String codeVerifier = Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(32));
            CodeVerifierTransformer transformer = new PlainCodeVerifier();
            parameters.setCodeChallenge(transformer.transformCodeVerifier(codeVerifier));
            parameters.setCodeChallengeMethod(transformer.getChallengeMethod());
            parameters.setResponseType(OAuthConstants.CODE_RESPONSE_TYPE);
            parameters.setPath("authorize/");

            OAuth2TestUtils.getLocation(client, parameters);
            fail("Failure expected on a missing (registered) redirectURI");
        } catch (Exception ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testPKCEPlain() throws Exception {
        testPKCE(new PlainCodeVerifier());
    }

    @org.junit.Test
    public void testPKCEPlainMissingVerifier() throws Exception {
        testPKCEMissingVerifier(new PlainCodeVerifier());
    }

    @org.junit.Test
    public void testPKCEPlainDifferentVerifier() throws Exception {
        testPKCEDifferentVerifier(new PlainCodeVerifier());
    }

    @org.junit.Test
    public void testPKCEDigest() {
        testPKCE(new DigestCodeVerifier());
    }

    @org.junit.Test
    public void testPKCEDigestMissingVerifier() {
        testPKCEMissingVerifier(new DigestCodeVerifier());
    }

    @org.junit.Test
    public void testPKCEDigestDifferentVerifier() {
        testPKCEDifferentVerifier(new DigestCodeVerifier());
    }

    private void testPKCE(CodeVerifierTransformer transformer) {
        URL busFile = PublicClientTest.class.getResource("publicclient.xml");

        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
        parameters.setConsumerId("consumer-id");
        String codeVerifier = Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(32));
        parameters.setCodeChallenge(transformer.transformCodeVerifier(codeVerifier));
        parameters.setCodeChallengeMethod(transformer.getChallengeMethod());
        parameters.setResponseType(OAuthConstants.CODE_RESPONSE_TYPE);
        parameters.setPath("authorize/");

        String location = OAuth2TestUtils.getLocation(client, parameters);
        String code = OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(tokenServiceAddress, busFile.toString());
        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code, "consumer-id", null, codeVerifier);
        assertNotNull(accessToken.getTokenKey());
    }

    private void testPKCEMissingVerifier(CodeVerifierTransformer transformer) {
        URL busFile = PublicClientTest.class.getResource("publicclient.xml");

        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
        parameters.setConsumerId("consumer-id");
        String codeVerifier = Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(32));
        parameters.setCodeChallenge(transformer.transformCodeVerifier(codeVerifier));
        parameters.setCodeChallengeMethod(transformer.getChallengeMethod());
        parameters.setResponseType(OAuthConstants.CODE_RESPONSE_TYPE);
        parameters.setPath("authorize/");

        String location = OAuth2TestUtils.getLocation(client, parameters);
        String code = OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(tokenServiceAddress, busFile.toString());
        try {
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code, "consumer-id", null);
            fail("Failure expected on a missing verifier");
        } catch (OAuthServiceException ex) {
            assertFalse(ex.getError().getError().isEmpty());
        }
    }

    private void testPKCEDifferentVerifier(CodeVerifierTransformer transformer) {
        URL busFile = PublicClientTest.class.getResource("publicclient.xml");

        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
        parameters.setConsumerId("consumer-id");
        String codeVerifier = Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(32));
        parameters.setCodeChallenge(transformer.transformCodeVerifier(codeVerifier));
        parameters.setCodeChallengeMethod(transformer.getChallengeMethod());
        parameters.setResponseType(OAuthConstants.CODE_RESPONSE_TYPE);
        parameters.setPath("authorize/");

        String location = OAuth2TestUtils.getLocation(client, parameters);
        String code = OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(tokenServiceAddress, busFile.toString());

        codeVerifier = Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(32));
        try {
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code, "consumer-id", null, codeVerifier);
            fail("Failure expected on a different verifier");
        } catch (OAuthServiceException ex) {
            assertFalse(ex.getError().getError().isEmpty());
        }
    }

    //
    // Server implementations
    //
    public static class BookServerOAuth2GrantsJCache extends AbstractBusTestServerBase {
        protected void run() {
            setBus(new SpringBusFactory().createBus(getClass().getResource("grants-server-public.xml")));
        }
    }

    public static class BookServerOAuth2GrantsJCacheSession extends AbstractBusTestServerBase {
        protected void run() {
            setBus(new SpringBusFactory().createBus(getClass().getResource("grants-server-public-session.xml")));
        }
    }

}
