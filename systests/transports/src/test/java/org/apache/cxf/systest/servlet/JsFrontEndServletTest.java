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

import java.nio.charset.StandardCharsets;

import org.w3c.dom.Document;

import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JsFrontEndServletTest extends AbstractServletTest {
    @Ignore
    public static class EmbeddedJettyServer extends AbstractJettyServer {
        public static final int PORT = allocatePortAsInt(EmbeddedJettyServer.class);

        public EmbeddedJettyServer() {
            super("/org/apache/cxf/systest/servlet/web-js.xml", "/", CONTEXT, PORT);
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
    public void testPostInvokeServices() throws Exception {
        try (CloseableHttpClient client = newClient()) {
            final HttpPost method = new HttpPost(uri("/services/Greeter"));

            method.setEntity(new InputStreamEntity(getClass().getResourceAsStream("GreeterMessage.xml"),
                ContentType.create("text/xml", StandardCharsets.UTF_8)));

            try (CloseableHttpResponse response = client.execute(method)) {
        
                assertEquals("text/xml", getContentType(response));
                assertEquals(StandardCharsets.UTF_8.name(), getCharset(response));
        
                Document doc = StaxUtils.read(response.getEntity().getContent());
                assertNotNull(doc);
        
                addNamespace("h", "http://apache.org/hello_world_soap_http/types");
        
                assertValid("/s:Envelope/s:Body", doc);
                assertValid("//h:sayHiResponse", doc);
                assertValid("//h:responseType", doc);
            }
        }
    }

    @Override
    protected int getPort() {
        return EmbeddedJettyServer.PORT;
    }
}
