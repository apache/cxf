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

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.json.JsonMapObjectProvider;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.services.ClientRegistration;
import org.apache.cxf.rs.security.oauth2.services.ClientRegistrationResponse;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;

public class OIDCDynamicRegistrationTest extends AbstractBusClientServerTestBase {
    public static final String PORT = OIDCDynRegistrationServer.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(OIDCDynRegistrationServer.class, true));
    }

    @org.junit.Test
    public void testGetClientRegNotAvail() throws Exception {
        URL busFile = OIDCDynamicRegistrationTest.class.getResource("client.xml");
        String address = "https://localhost:" + PORT + "/services/dynamic/register";
        WebClient wc = WebClient.create(address, Collections.singletonList(new JsonMapObjectProvider()), 
                         busFile.toString());
        Response r = wc.accept("application/json").path("some-client-id").get();
        assertEquals(401, r.getStatus());
    }
    @org.junit.Test
    public void testRegisterClient() throws Exception {
        doTestRegisterClient(null);
    }
    @org.junit.Test
    public void testRegisterClientInitialAccessToken() throws Exception {
        doTestRegisterClient("123456789");
    }
    
    private void doTestRegisterClient(String initialAccessToken) throws Exception {
        URL busFile = OIDCDynamicRegistrationTest.class.getResource("client.xml");
        String address = "https://localhost:" + PORT + "/services";
        if (initialAccessToken != null) {
            address = address + "/dynamicWithAt/register";
        } else {
            address = address + "/dynamic/register";
        }
        WebClient wc = WebClient.create(address, Collections.singletonList(new JsonMapObjectProvider()), 
                         busFile.toString());
        
        wc.accept("application/json").type("application/json");
        ClientRegistration reg = new ClientRegistration();
        reg.setApplicationType("web");
        reg.setScope("openid");
        reg.setClientName("dynamic_client");
        reg.setGrantTypes(Collections.singletonList("authorization_code"));
        reg.setRedirectUris(Collections.singletonList("https://a/b/c"));
        
        ClientRegistrationResponse resp = null;
        Response r = wc.post(reg);
        if (initialAccessToken == null) {
            resp = r.readEntity(ClientRegistrationResponse.class);
        } else {
            assertEquals(401, wc.get().getStatus());
            wc.authorization(new ClientAccessToken("Bearer", initialAccessToken));
            resp = wc.post(reg, ClientRegistrationResponse.class);
        }
        assertNotNull(resp.getClientId());
        assertNotNull(resp.getClientSecret());
        assertEquals(address + "/" + resp.getClientId(), 
                     resp.getRegistrationClientUri());
        String regAccessToken = resp.getRegistrationAccessToken();
        assertNotNull(regAccessToken);
        
        wc.reset();
        wc.path(resp.getClientId());
        assertEquals(401, wc.get().getStatus());
        
        wc.authorization(new ClientAccessToken("Bearer", regAccessToken));
        ClientRegistration clientRegResp = wc.get(ClientRegistration.class);
        assertNotNull(clientRegResp);
        assertEquals("web", clientRegResp.getApplicationType());
        assertEquals("dynamic_client", clientRegResp.getClientName());
        assertEquals("openid", clientRegResp.getScope());
        assertEquals(Collections.singletonList("authorization_code"), 
                     clientRegResp.getGrantTypes());
        assertEquals(Collections.singletonList("https://a/b/c"), 
                     clientRegResp.getRedirectUris());
        
        assertEquals(200, wc.delete().getStatus());
    }
        
}
