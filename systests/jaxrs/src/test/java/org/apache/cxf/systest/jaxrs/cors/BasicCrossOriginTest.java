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

package org.apache.cxf.systest.jaxrs.cors;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.cors.CorsHeaderConstants;
import org.apache.cxf.systest.jaxrs.AbstractSpringServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class BasicCrossOriginTest extends AbstractBusClientServerTestBase {
    public static final String PORT = SpringServer.PORT;
    private WebClient configClient;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(SpringServer.class, true));
    }

    @Before
    public void before() {
        List<Object> providers = new ArrayList<Object>();
        providers.add(new org.codehaus.jackson.jaxrs.JacksonJsonProvider());
        configClient = WebClient.create("http://localhost:" + PORT + "/config", providers);
    }

    @Test
    public void testSimpleGet() throws Exception {
        String origin = "http://localhost:" + PORT;
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(origin + "/test/simpleGet/HelloThere");
        httpget.addHeader("Origin", origin);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String e = IOUtils.toString(entity.getContent(), "utf-8");

        assertEquals("HelloThere", e);
        Header[] aaoHeaders = response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_ORIGIN);
        assertNotNull(aaoHeaders);
        assertEquals(1, aaoHeaders.length);
        assertEquals("*", aaoHeaders[0].getValue());
    }

    @Test
    public void specificOriginSimpleGet() throws Exception {
        String origin = "http://localhost:" + PORT;

        String confResult = configClient.accept("text/plain").replacePath("/setOriginList")
            .type("application/json")
            .post(new String[] {
                origin
            }, String.class);
        assertEquals("ok", confResult);

        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(origin + "/test/simpleGet/HelloThere");
        httpget.addHeader("Origin", origin);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String e = IOUtils.toString(entity.getContent(), "utf-8");

        assertEquals("HelloThere", e);
        Header[] aaoHeaders = response.getHeaders(CorsHeaderConstants.HEADER_AC_ALLOW_ORIGIN);
        assertNotNull(aaoHeaders);
        assertEquals(1, aaoHeaders.length);
        assertEquals(origin, aaoHeaders[0].getValue());
    }

    @Ignore
    public static class SpringServer extends AbstractSpringServer {
        public static final String PORT = AbstractSpringServer.PORT;

        public SpringServer() {
            super("/jaxrs_cors");
        }
    }
}
