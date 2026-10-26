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

import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CXFStaticResourcesServletTest extends AbstractServletTest {

    @Ignore
    public static class CXFStaticServletEmbeddedJettyServer extends AbstractJettyServer {
        public static final int PORT = allocatePortAsInt(CXFStaticServletEmbeddedJettyServer.class);

        public CXFStaticServletEmbeddedJettyServer() {
            super(null, "/org/apache/cxf/systest/static/", CONTEXT, PORT);
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(CXFStaticServletEmbeddedJettyServer.class, true));
        createStaticBus();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopAllServers();
    }

    @Test
    public void testGetStaticResource() throws Exception {
        final String expected = new String(getClass()
            .getResourceAsStream("/org/apache/cxf/systest/static/test.xml")
            .readAllBytes(), StandardCharsets.UTF_8);

        try (CloseableHttpClient client = newClient()) {
            final HttpGet method = new HttpGet(uri("/services/test.xml"));
            try (CloseableHttpResponse res = client.execute(method)) {
                final String doc = new String(res.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                assertThat(doc, equalTo(expected));
            }
        }
    }

    @Test
    public void testGetNonExistingStaticResource() throws Exception {
        try (CloseableHttpClient client = newClient()) {
            final HttpGet method = new HttpGet(uri("/services/WEB-INF/beans.xml"));
            try (CloseableHttpResponse res = client.execute(method)) {
                assertThat(res.getStatusLine().getStatusCode(), equalTo(500));
            }
        }
    }

    @Test
    public void testGetProtectedStaticResource() throws Exception {
        try (CloseableHttpClient client = newClient()) {
            final HttpGet method = new HttpGet(uri("/services/WEB-INF/web.xml"));
            try (CloseableHttpResponse res = client.execute(method)) {
                assertThat(res.getStatusLine().getStatusCode(), equalTo(500));
            }
        }
    }

    @Override
    protected int getPort() {
        return CXFStaticServletEmbeddedJettyServer.PORT;
    }
}
