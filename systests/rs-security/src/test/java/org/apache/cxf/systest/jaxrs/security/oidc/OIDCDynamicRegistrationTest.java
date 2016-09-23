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
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;

/**
 * Some tests for the OAuth 2.0 filters
 */
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
        String address = "https://localhost:" + PORT + "/services/register";
        WebClient wc = WebClient.create(address, Collections.singletonList(new JsonMapObjectProvider()), 
                         busFile.toString());
        Response r = wc.accept("application/json").path("some-client-id").get();
        assertEquals(401, r.getStatus());
    }
        
}
