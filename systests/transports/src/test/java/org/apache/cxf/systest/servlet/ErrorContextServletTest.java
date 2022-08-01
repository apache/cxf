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
package org.apache.cxf.systest.servlet;


import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ErrorContextServletTest extends AbstractServletTest {
    @Ignore
    public static class EmbeddedJettyServer extends AbstractJettyServer {
        public static final int PORT = allocatePortAsInt(EmbeddedJettyServer.class);

        public EmbeddedJettyServer() {
            super("/org/apache/cxf/systest/servlet/web-spring-error.xml", "/", CONTEXT, PORT);
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(EmbeddedJettyServer.class, true));
        createStaticBus();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopAllServers();
    }

    @Test
    public void testInvoke() throws Exception {
        try (CloseableHttpClient client = newClient()) {
            final HttpGet method = new HttpGet(uri("/services/greeter?wsdl"));
            // The Spring context contains errors, the service should not be available
            try (CloseableHttpResponse res = client.execute(method)) {
                assertEquals(503, res.getStatusLine().getStatusCode());
            }
        } 
    }

    @Override
    protected int getPort() {
        return EmbeddedJettyServer.PORT;
    }
}
