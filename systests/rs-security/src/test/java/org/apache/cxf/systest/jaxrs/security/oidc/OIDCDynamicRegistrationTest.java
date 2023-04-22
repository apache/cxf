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

import java.net.URL;
import java.util.Collections;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.json.JsonMapObjectProvider;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.services.ClientRegistration;
import org.apache.cxf.rs.security.oauth2.services.ClientRegistrationResponse;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OIDCDynamicRegistrationTest extends AbstractBusClientServerTestBase {
    private static final SpringBusTestServer DYNREG_SERVER = new SpringBusTestServer("oidc-server-dynreg");

    private static final String ACCESS_TOKEN = "123456789";

    @BeforeClass
    public static void startServers() throws Exception {
        System.setProperty("accessToken", ACCESS_TOKEN);
        assertTrue("server did not launch correctly", launchServer(DYNREG_SERVER));
    }

    @org.junit.Test
    public void testGetClientRegNotAvail() throws Exception {
        URL busFile = OIDCDynamicRegistrationTest.class.getResource("client.xml");
        String address = "https://localhost:" + DYNREG_SERVER.getPort() + "/services/dynamic/register";
        WebClient wc = WebClient.create(address, Collections.singletonList(new JsonMapObjectProvider()),
                         busFile.toString());
        Response r = wc.accept("application/json").path("some-client-id").get();
        assertEquals(401, r.getStatus());
    }

    @org.junit.Test
    public void testRegisterClientNoInitialAccessToken() throws Exception {
        URL busFile = OIDCDynamicRegistrationTest.class.getResource("client.xml");
        String address = "https://localhost:" + DYNREG_SERVER.getPort() + "/services/dynamic/register";
        WebClient wc = WebClient.create(address, Collections.singletonList(new JsonMapObjectProvider()),
                         busFile.toString());
        wc.accept("application/json").type("application/json");

        assertEquals(401, wc.post(newClientRegistrationCodeGrant()).getStatus());
    }

    @org.junit.Test
    public void testRegisterClientInitialAccessTokenCodeGrant() throws Exception {
        URL busFile = OIDCDynamicRegistrationTest.class.getResource("client.xml");
        String address = "https://localhost:" + DYNREG_SERVER.getPort() + "/services/dynamicWithAt/register";
        WebClient wc =
            WebClient.create(address, Collections.singletonList(new JsonMapObjectProvider()), busFile.toString())
            .accept("application/json").type("application/json")
            .authorization(new ClientAccessToken(OAuthConstants.BEARER_AUTHORIZATION_SCHEME, ACCESS_TOKEN));

        ClientRegistration reg = newClientRegistrationCodeGrant();
        ClientRegistrationResponse resp = wc.post(reg, ClientRegistrationResponse.class);

        assertNotNull(resp.getClientId());
        assertNotNull(resp.getClientSecret());
        assertEquals(address + "/" + resp.getClientId(),
                     resp.getRegistrationClientUri());
        String regAccessToken = resp.getRegistrationAccessToken();
        assertNotNull(regAccessToken);

        wc.path(resp.getClientId());
        assertEquals(401, wc.get().getStatus());

        ClientRegistration clientRegResp = wc
            .authorization(new ClientAccessToken(OAuthConstants.BEARER_AUTHORIZATION_SCHEME, regAccessToken))
            .get(ClientRegistration.class);
        testCommonRegCodeGrantProperties(clientRegResp);

        assertNull(clientRegResp.getTokenEndpointAuthMethod());

        assertEquals(200, wc.delete().getStatus());
    }

    @org.junit.Test
    public void testRegisterClientPasswordGrant() throws Exception {
        URL busFile = OIDCDynamicRegistrationTest.class.getResource("client.xml");
        String address = "https://localhost:" + DYNREG_SERVER.getPort() + "/services/dynamicWithAt/register";
        WebClient wc =
            WebClient.create(address, Collections.singletonList(new JsonMapObjectProvider()), busFile.toString())
            .accept("application/json").type("application/json")
            .authorization(new ClientAccessToken(OAuthConstants.BEARER_AUTHORIZATION_SCHEME, ACCESS_TOKEN));

        ClientRegistration reg = new ClientRegistration();
        reg.setClientName("dynamic_client");
        reg.setGrantTypes(Collections.singletonList(OAuthConstants.RESOURCE_OWNER_GRANT));

        ClientRegistrationResponse resp = wc.post(reg, ClientRegistrationResponse.class);

        assertNotNull(resp.getClientId());
        assertNotNull(resp.getClientSecret());
        assertEquals(address + "/" + resp.getClientId(),
                     resp.getRegistrationClientUri());
        String regAccessToken = resp.getRegistrationAccessToken();
        assertNotNull(regAccessToken);

        ClientRegistration clientRegResp = wc.path(resp.getClientId())
            .authorization(new ClientAccessToken(OAuthConstants.BEARER_AUTHORIZATION_SCHEME, regAccessToken))
            .get(ClientRegistration.class);

        assertEquals("web", clientRegResp.getApplicationType());
        assertEquals("dynamic_client", clientRegResp.getClientName());
        assertEquals(Collections.singletonList(OAuthConstants.RESOURCE_OWNER_GRANT),
                     clientRegResp.getGrantTypes());
        assertNull(clientRegResp.getTokenEndpointAuthMethod());
        assertNull(clientRegResp.getScope());
        assertNull(clientRegResp.getRedirectUris());

        assertEquals(200, wc.delete().getStatus());
    }

    @org.junit.Test
    public void testRegisterClientPasswordGrantPublic() throws Exception {
        URL busFile = OIDCDynamicRegistrationTest.class.getResource("client.xml");
        String address = "https://localhost:" + DYNREG_SERVER.getPort() + "/services/dynamicWithAt/register";
        WebClient wc =
            WebClient.create(address, Collections.singletonList(new JsonMapObjectProvider()), busFile.toString())
            .accept("application/json").type("application/json")
            .authorization(new ClientAccessToken(OAuthConstants.BEARER_AUTHORIZATION_SCHEME, ACCESS_TOKEN));

        ClientRegistration reg = new ClientRegistration();
        reg.setClientName("dynamic_client");
        reg.setGrantTypes(Collections.singletonList(OAuthConstants.RESOURCE_OWNER_GRANT));
        reg.setTokenEndpointAuthMethod(OAuthConstants.TOKEN_ENDPOINT_AUTH_NONE);
        ClientRegistrationResponse resp = wc.post(reg, ClientRegistrationResponse.class);

        assertNotNull(resp.getClientId());
        assertNull(resp.getClientSecret());
        assertEquals(address + "/" + resp.getClientId(), resp.getRegistrationClientUri());
        String regAccessToken = resp.getRegistrationAccessToken();
        assertNotNull(regAccessToken);

        ClientRegistration clientRegResp = wc.path(resp.getClientId())
            .authorization(new ClientAccessToken(OAuthConstants.BEARER_AUTHORIZATION_SCHEME, regAccessToken))
            .get(ClientRegistration.class);

        assertEquals("native", clientRegResp.getApplicationType());
        assertEquals("dynamic_client", clientRegResp.getClientName());
        assertEquals(Collections.singletonList(OAuthConstants.RESOURCE_OWNER_GRANT),
                     clientRegResp.getGrantTypes());
        assertEquals(OAuthConstants.TOKEN_ENDPOINT_AUTH_NONE, clientRegResp.getTokenEndpointAuthMethod());
        assertNull(clientRegResp.getScope());
        assertNull(clientRegResp.getRedirectUris());

        assertEquals(200, wc.delete().getStatus());
    }

    @org.junit.Test
    public void testRegisterClientInitialAccessTokenCodeGrantTls() throws Exception {
        URL busFile = OIDCDynamicRegistrationTest.class.getResource("client.xml");
        String address = "https://localhost:" + DYNREG_SERVER.getPort() + "/services/dynamicWithAt/register";
        WebClient wc =
            WebClient.create(address, Collections.singletonList(new JsonMapObjectProvider()), busFile.toString())
            .accept("application/json").type("application/json")
            .authorization(new ClientAccessToken(OAuthConstants.BEARER_AUTHORIZATION_SCHEME, ACCESS_TOKEN));

        ClientRegistration reg = newClientRegistrationCodeGrant();
        reg.setTokenEndpointAuthMethod(OAuthConstants.TOKEN_ENDPOINT_AUTH_TLS);
        reg.setProperty(OAuthConstants.TLS_CLIENT_AUTH_SUBJECT_DN,
                        "CN=whateverhost.com,OU=Morpit,O=ApacheTest,L=Syracuse,C=US");

        ClientRegistrationResponse resp = wc.post(reg, ClientRegistrationResponse.class);

        assertNotNull(resp.getClientId());
        assertNull(resp.getClientSecret());
        assertEquals(address + "/" + resp.getClientId(),
                     resp.getRegistrationClientUri());
        String regAccessToken = resp.getRegistrationAccessToken();
        assertNotNull(regAccessToken);

        ClientRegistration clientRegResp = wc.path(resp.getClientId())
            .authorization(new ClientAccessToken(OAuthConstants.BEARER_AUTHORIZATION_SCHEME, regAccessToken))
            .get(ClientRegistration.class);

        testCommonRegCodeGrantProperties(clientRegResp);
        assertEquals(OAuthConstants.TOKEN_ENDPOINT_AUTH_TLS, clientRegResp.getTokenEndpointAuthMethod());
        assertEquals("CN=whateverhost.com,OU=Morpit,O=ApacheTest,L=Syracuse,C=US",
                     clientRegResp.getProperty(OAuthConstants.TLS_CLIENT_AUTH_SUBJECT_DN));

        assertEquals(200, wc.delete().getStatus());
    }

    @org.junit.Test
    public void testUpdateClient() throws Exception {
        URL busFile = OIDCDynamicRegistrationTest.class.getResource("client.xml");
        String address = "https://localhost:" + DYNREG_SERVER.getPort() + "/services/dynamicWithAt/register";
        WebClient wc =
            WebClient.create(address, Collections.singletonList(new JsonMapObjectProvider()), busFile.toString())
            .accept("application/json").type("application/json")
            .authorization(new ClientAccessToken(OAuthConstants.BEARER_AUTHORIZATION_SCHEME, ACCESS_TOKEN));

        final ClientRegistration reg = newClientRegistrationCodeGrant();
        final ClientRegistrationResponse clientRegistrationResponse = wc
            .post(reg, ClientRegistrationResponse.class);

        final String regAccessToken = clientRegistrationResponse.getRegistrationAccessToken();
        assertNotNull(regAccessToken);

        reg.setScope(OidcUtils.getEmailScope());
        final ClientRegistration updatedClientRegistration = wc.path(clientRegistrationResponse.getClientId())
            .authorization(new ClientAccessToken(OAuthConstants.BEARER_AUTHORIZATION_SCHEME, regAccessToken))
            .put(reg, ClientRegistration.class);

        assertEquals(OidcUtils.getEmailScope(), updatedClientRegistration.getScope());
        // https://tools.ietf.org/html/rfc7592#section-2.2
        assertNull(updatedClientRegistration.getProperty("registration_access_token"));
        assertNull(updatedClientRegistration.getProperty("registration_client_uri"));
        assertNull(updatedClientRegistration.getProperty("client_secret_expires_at"));
        assertNull(updatedClientRegistration.getProperty("client_id_issued_at"));

        wc.authorization(null);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(),
            wc.put(reg).getStatus());
        assertEquals(Status.UNAUTHORIZED.getStatusCode(),
            wc.delete().getStatus());

        wc.authorization(new ClientAccessToken(OAuthConstants.BEARER_AUTHORIZATION_SCHEME, regAccessToken));
        assertEquals(200, wc.delete().getStatus());
    }

    private static ClientRegistration newClientRegistrationCodeGrant() {
        final ClientRegistration reg = new ClientRegistration();
        reg.setApplicationType("web");
        reg.setScope(OidcUtils.getOpenIdScope());
        reg.setClientName("dynamic_client");
        reg.setGrantTypes(Collections.singletonList(OAuthConstants.AUTHORIZATION_CODE_GRANT));
//        reg.setResponseTypes(Collections.singletonList(OAuthConstants.CODE_RESPONSE_TYPE));
        reg.setRedirectUris(Collections.singletonList("https://a/b/c"));

        reg.setProperty("post_logout_redirect_uris",
                        Collections.singletonList("https://rp/logout"));
        return reg;
    }

    private static void testCommonRegCodeGrantProperties(ClientRegistration clientRegResp) {
        assertNotNull(clientRegResp);
        assertEquals("web", clientRegResp.getApplicationType());
        assertEquals("openid", clientRegResp.getScope());
        assertEquals("dynamic_client", clientRegResp.getClientName());
        assertEquals(Collections.singletonList(OAuthConstants.AUTHORIZATION_CODE_GRANT),
                     clientRegResp.getGrantTypes());
//        assertEquals(Collections.singletonList(OAuthConstants.CODE_RESPONSE_TYPE),
//                     clientRegResp.getResponseTypes());
        assertEquals(Collections.singletonList("https://a/b/c"),
                     clientRegResp.getRedirectUris());
        assertEquals(Collections.singletonList("https://rp/logout"),
                     clientRegResp.getListStringProperty("post_logout_redirect_uris"));
    }

}
