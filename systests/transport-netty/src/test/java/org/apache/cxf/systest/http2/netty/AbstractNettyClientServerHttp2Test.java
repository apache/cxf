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

package org.apache.cxf.systest.http2.netty;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.netty.client.NettyHttpConduit;
import org.apache.cxf.transport.https.InsecureTrustManager;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

abstract class AbstractNettyClientServerHttp2Test extends AbstractBusClientServerTestBase {
    @Test
    public void testBookNotFoundWithHttp2() throws Exception {
        final WebClient client = createWebClient("/web/bookstore/notFound", true);
        assertThat(WebClient.getConfig(client).getHttpConduit(), instanceOf(NettyHttpConduit.class));

        final Response response = client
            .accept("text/plain")
            .get();
        
        assertThat(response.getStatus(), equalTo(404));
        client.close();
    }
    
    @Test
    public void testBookTraceWithHttp2() throws Exception {
        final WebClient client = createWebClient("/web/bookstore/trace", true);
        assertThat(WebClient.getConfig(client).getHttpConduit(), instanceOf(NettyHttpConduit.class));

        final Response response = client
            .accept("text/plain")
            .invoke("TRACE", null);
        
        assertThat(response.getStatus(), equalTo(406));

        client.close();
    }
    
    @Test
    public void testBookWithHttp2() throws Exception {
        final WebClient client = createWebClient("/web/bookstore/booknames", true);
        assertThat(WebClient.getConfig(client).getHttpConduit(), instanceOf(NettyHttpConduit.class));
        
        final Response response = client
            .accept("text/plain")
            .get();
        
        assertThat(response.getStatus(), equalTo(200));
        assertEquals("CXF in Action", response.readEntity(String.class));

        client.close();
    }

    @Test
    public void testBookEncodedWithHttp2() throws Exception {
        final WebClient client = createWebClient("/web/bookstore/book%20names", true);
        assertThat(WebClient.getConfig(client).getHttpConduit(), instanceOf(NettyHttpConduit.class));
        
        final Response response = client
            .accept("text/plain")
            .get();
        
        assertThat(response.getStatus(), equalTo(200));
        assertEquals("CXF in Action", response.readEntity(String.class));

        client.close();
    }

    @Test
    public void testGetBookStreamHttp2() throws Exception {
        final WebClient client = createWebClient("/web/bookstore/bookstream", true);
        assertThat(WebClient.getConfig(client).getHttpConduit(), instanceOf(NettyHttpConduit.class));
        
        final Response response = client
            .accept("application/xml")
            .get();

        assertThat(response.getStatus(), equalTo(200));
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Book><id>1</id><name>Book1</name></Book>", response.readEntity(String.class));

        client.close();
    }

    @Test
    public void testBookWithHttp() throws Exception {
        final WebClient client = createWebClient("/web/bookstore/booknames", false);
        
        try (Response resp = client.get()) {
            assertThat(resp.getStatus(), equalTo(200));
            assertEquals("CXF in Action", resp.readEntity(String.class));
        }
        
        client.close();
    }

    @Test
    public void testBookEncodedWithHttp() throws Exception {
        final WebClient client = createWebClient("/web/bookstore/book%20names", false);
        
        try (Response resp = client.get()) {
            assertThat(resp.getStatus(), equalTo(200));
            assertEquals("CXF in Action", resp.readEntity(String.class));
        }
        
        client.close();
    }

    @Test
    public void testBookTraceWithHttp() throws Exception {
        final WebClient client = createWebClient("/web/bookstore/trace", false);

        try (Response response = client.invoke("TRACE", null)) {
            assertThat(response.getStatus(), equalTo(406));
        }

        client.close();
    }

    private WebClient createWebClient(final String path, final boolean enableHttp2) {
        final WebClient wc = WebClient
            .create(getAddress() + getContext() + path)
            .accept("text/plain");

        WebClient.getConfig(wc).getRequestContext().put(NettyHttpConduit.ENABLE_HTTP2, enableHttp2);
        WebClient.getConfig(wc).getRequestContext().put(NettyHttpConduit.USE_ASYNC, "ALWAYS");

        if (isSecure()) {
            final HTTPConduit conduit = WebClient.getConfig(wc).getHttpConduit();
            TLSClientParameters params = conduit.getTlsClientParameters();

            if (params == null)  {
                params = new TLSClientParameters();
                conduit.setTlsClientParameters(params);
            }

            // Create TrustManager instance which trusts all clients and servers
            params.setTrustManagers(InsecureTrustManager.getNoOpX509TrustManagers()); 
            params.setDisableCNCheck(true);
        }
        
        return wc;
    }

    protected abstract String getAddress();
    protected abstract String getContext();

    protected boolean isSecure() {
        return getAddress().startsWith("https");
    }
}