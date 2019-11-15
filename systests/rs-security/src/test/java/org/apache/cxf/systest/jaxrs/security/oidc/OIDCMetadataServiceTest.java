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
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.systest.jaxrs.security.SecurityTestUtil;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for the OIDC/OAuth OidcConfigurationService
 */
public class OIDCMetadataServiceTest extends AbstractBusClientServerTestBase {

    static final String PORT = TestUtil.getPortNumber("metadata-server-jcache");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("Server failed to launch", launchServer(OIDCServer.class, true));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
    }

    @org.junit.Test
    public void testOIDCMetadataService() throws Exception {
        URL busFile = OIDCMetadataServiceTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/.well-known/openid-configuration";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());

        Response response = client.get();
        assertEquals(200, response.getStatus());
        String responseStr = response.readEntity(String.class);

        JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();
        Map<String, Object> json = reader.fromJson(responseStr);
        assertTrue(json.containsKey("issuer"));
        assertTrue(json.containsKey("response_types_supported"));
    }

    //
    // Server implementations
    //

    public static class OIDCServer extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            OIDCServer.class.getResource("metadata-server-jcache.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new OIDCServer();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

}
