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

package org.apache.cxf.systest.jaxrs.security.oauth2.tls;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.provider.json.JsonMapObjectProvider;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oauth2.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.services.ClientRegistration;
import org.apache.cxf.rs.security.oauth2.services.ClientRegistrationResponse;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JAXRSOAuth2TlsTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerOAuth2Tls.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2Tls.class, true));
    }


    @Test
    public void testTwoWayTLSClientIdIsSubjectDn() throws Exception {
        String atServiceAddress = "https://localhost:" + PORT + "/oauth2/token";
        WebClient wc = createOAuth2WebClient(atServiceAddress);

        ClientAccessToken at = OAuthClientUtils.getAccessToken(wc, new CustomGrant());
        assertNotNull(at.getTokenKey());

        String protectedRsAddress = "https://localhost:" + PORT + "/rs/bookstore/books/123";
        WebClient wcRs = createRsWebClient(protectedRsAddress, at, "client.xml");
        Book book = wcRs.get(Book.class);
        assertEquals(123L, book.getId());

        String protectedRsAddress2 = "https://localhost:" + PORT + "/rs2/bookstore/books/123";
        WebClient wcRs2 = createRsWebClient(protectedRsAddress2, at, "client.xml");
        book = wcRs2.get(Book.class);
        assertEquals(123L, book.getId());

        String unprotectedRsAddress = "https://localhost:" + PORT + "/rsUnprotected/bookstore/books/123";
        WebClient wcRsDiffClientCert = createRsWebClient(unprotectedRsAddress, at, "client2.xml");
        // Unprotected resource
        book = wcRsDiffClientCert.get(Book.class);
        assertEquals(123L, book.getId());

        // Protected resource, access token was created with Morpit.jks key, RS is accessed with
        // Bethal.jks key, thus 401 is expected
        wcRsDiffClientCert = createRsWebClient(protectedRsAddress, at, "client2.xml");
        assertEquals(401, wcRsDiffClientCert.get().getStatus());
        wcRsDiffClientCert = createRsWebClient(protectedRsAddress2, at, "client2.xml");
        assertEquals(401, wcRsDiffClientCert.get().getStatus());
    }

    @Test
    public void testTwoWayTLSClientIdBound() throws Exception {
        String atServiceAddress = "https://localhost:" + PORT + "/oauth2/token";
        WebClient wc = createOAuth2WebClient(atServiceAddress);

        ClientAccessToken at = OAuthClientUtils.getAccessToken(wc, new Consumer("bound"),
                                                               new CustomGrant());
        assertNotNull(at.getTokenKey());
    }

    @Test
    public void testTwoWayTLSClientIdBoundJwt() throws Exception {
        doTestTwoWayTLSClientIdBoundJwt("boundJwt");
    }

    @Test
    public void testRegisterClientTwoWayTLSClientIdBoundDynReg() throws Exception {
        String dynRegAddress = "https://localhost:" + PORT + "/oauth2Jwt/register";
        WebClient wcDynReg = createDynRegWebClient(dynRegAddress);

        wcDynReg.accept("application/json").type("application/json");
        ClientRegistration reg = newClientRegistration();
        wcDynReg.authorization(new ClientAccessToken("Bearer", "123456789"));
        ClientRegistrationResponse resp = wcDynReg.post(reg, ClientRegistrationResponse.class);

        doTestTwoWayTLSClientIdBoundJwt(resp.getClientId());

        // delete the client
        String regAccessToken = resp.getRegistrationAccessToken();
        assertNotNull(regAccessToken);
        wcDynReg.path(resp.getClientId());


        wcDynReg.authorization(new ClientAccessToken("Bearer", regAccessToken));

        assertEquals(200, wcDynReg.delete().getStatus());
        assertNotNull(regAccessToken);
    }

    private void doTestTwoWayTLSClientIdBoundJwt(String clientId) throws Exception {
        String atServiceAddress = "https://localhost:" + PORT + "/oauth2Jwt/token";
        WebClient wc = createOAuth2WebClient(atServiceAddress);

        ClientAccessToken at = OAuthClientUtils.getAccessToken(wc, new Consumer(clientId),
                                                               new CustomGrant());
        assertNotNull(at.getTokenKey());
        JwsJwtCompactConsumer c = new JwsJwtCompactConsumer(at.getTokenKey());
        JwtClaims claims = JwtUtils.jsonToClaims(c.getDecodedJwsPayload());

        Map<String, Object> cnfs = claims.getMapProperty(JwtConstants.CLAIM_CONFIRMATION);
        assertNotNull(cnfs);
        assertNotNull(cnfs.get(JoseConstants.HEADER_X509_THUMBPRINT_SHA256));

        String protectedRsAddress = "https://localhost:" + PORT + "/rsJwt/bookstore/books/123";
        WebClient wcRs = createRsWebClient(protectedRsAddress, at, "client.xml");
        Book book = wcRs.get(Book.class);
        assertEquals(123L, book.getId());

        String protectedRsAddress2 = "https://localhost:" + PORT + "/rsJwt2/bookstore/books/123";
        WebClient wcRs2 = createRsWebClient(protectedRsAddress2, at, "client.xml");
        book = wcRs2.get(Book.class);
        assertEquals(123L, book.getId());

        String unprotectedRsAddress = "https://localhost:" + PORT + "/rsUnprotected/bookstore/books/123";
        WebClient wcRsDiffClientCert = createRsWebClient(unprotectedRsAddress, at, "client2.xml");
        // Unprotected resource
        book = wcRsDiffClientCert.get(Book.class);
        assertEquals(123L, book.getId());

        // Protected resource, access token was created with Morpit.jks key, RS is accessed with
        // Bethal.jks key, thus 401 is expected
        wcRsDiffClientCert = createRsWebClient(protectedRsAddress, at, "client2.xml");
        assertEquals(401, wcRsDiffClientCert.get().getStatus());
        wcRsDiffClientCert = createRsWebClient(protectedRsAddress2, at, "client2.xml");
        assertEquals(401, wcRsDiffClientCert.get().getStatus());
    }

    private ClientRegistration newClientRegistration() {
        ClientRegistration reg = new ClientRegistration();
        reg.setApplicationType("web");
        reg.setScope("openid");
        reg.setClientName("dynamic_client");
        reg.setGrantTypes(Collections.singletonList("custom_grant"));
        reg.setRedirectUris(Collections.singletonList("https://a/b/c"));
        reg.setTokenEndpointAuthMethod(OAuthConstants.TOKEN_ENDPOINT_AUTH_TLS);
        reg.setProperty(OAuthConstants.TLS_CLIENT_AUTH_SUBJECT_DN,
                        "CN=whateverhost.com,OU=Morpit,O=ApacheTest,L=Syracuse,C=US");
        return reg;
    }

    @Test
    public void testTwoWayTLSClientUnbound() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2/token";
        WebClient wc = createOAuth2WebClient(address);
        try {
            OAuthClientUtils.getAccessToken(wc,
                                            new Consumer("unbound"),
                                            new CustomGrant());
            fail("exception_expected");
        } catch (OAuthServiceException ex) {
            assertEquals("invalid_client", ex.getError().getError());
        }

    }


    private WebClient createOAuth2WebClient(String address) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSOAuth2TlsTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        WebClient wc = bean.createWebClient();
        wc.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_JSON);
        return wc;
    }
    private WebClient createDynRegWebClient(String address) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        bean.setProvider(new JsonMapObjectProvider());

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSOAuth2TlsTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        WebClient wc = bean.createWebClient();
        wc.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);
        return wc;
    }
    private WebClient createRsWebClient(String address, ClientAccessToken at, String clientContext) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSOAuth2TlsTest.class.getResource(clientContext);
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        WebClient wc = bean.createWebClient();
        wc.accept(MediaType.APPLICATION_XML);
        wc.authorization(at);
        return wc;
    }


    private static final class CustomGrant implements AccessTokenGrant {

        private static final long serialVersionUID = -4007538779198315873L;

        @Override
        public String getType() {
            return "custom_grant";
        }

        @Override
        public MultivaluedMap<String, String> toMap() {
            MultivaluedMap<String, String> map = new MetadataMap<>();
            map.putSingle(OAuthConstants.GRANT_TYPE, "custom_grant");
            return map;
        }

    }

}
