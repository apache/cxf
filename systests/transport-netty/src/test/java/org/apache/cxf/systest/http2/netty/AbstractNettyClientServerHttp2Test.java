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
import org.apache.cxf.systest.http2.netty.Http2TestClient.ClientResponse;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.https.InsecureTrustManager;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

abstract class AbstractNettyClientServerHttp2Test extends AbstractBusClientServerTestBase {
    @Test
    public void testBookNotFoundWithHttp2() throws Exception {
        final Http2TestClient client = new Http2TestClient(isSecure());
        
        final ClientResponse response = client
            .request(getAddress())
            .accept("text/plain")
            .path(getContext() + "/web/bookstore/notFound")
            .http2()
            .get();
        
        assertThat(response.getResponseCode(), equalTo(404));
        assertThat(response.getProtocol(), equalTo("HTTP/2.0"));
    }
    
    @Test
    public void testBookTraceWithHttp2() throws Exception {
        final Http2TestClient client = new Http2TestClient(isSecure());
        
        final ClientResponse response = client
            .request(getAddress())
            .accept("text/plain")
            .path(getContext() + "/web/bookstore/trace")
            .http2()
            .trace();
        
        assertThat(response.getResponseCode(), equalTo(406));
        assertThat(response.getProtocol(), equalTo("HTTP/2.0"));
    }
    
    @Test
    public void testBookWithHttp2() throws Exception {
        final Http2TestClient client = new Http2TestClient(isSecure());
        
        final ClientResponse response = client
            .request(getAddress())
            .accept("text/plain")
            .path(getContext() + "/web/bookstore/booknames")
            .http2()
            .get();
        
        assertThat(response.getResponseCode(), equalTo(200));
        assertThat(response.getProtocol(), equalTo("HTTP/2.0"));
        assertEquals("CXF in Action", response.getBody());
    }

    @Test
    public void testGetBookStreamHttp2() throws Exception {
        final Http2TestClient client = new Http2TestClient(isSecure());
        
        final ClientResponse response = client
            .request(getAddress())
            .accept("application/xml")
            .path(getContext() + "/web/bookstore/bookstream")
            .http2()
            .get();

        assertThat(response.getResponseCode(), equalTo(200));
        assertThat(response.getProtocol(), equalTo("HTTP/2.0"));
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Book><id>1</id><name>Book1</name></Book>", response.getBody());
    }

    @Test
    public void testBookWithHttp() throws Exception {
        final WebClient wc = createWebClient("/web/bookstore/booknames");
        try (Response resp = wc.get()) {
            assertThat(resp.getStatus(), equalTo(200));
            assertEquals("CXF in Action", resp.readEntity(String.class));
        }
    }

    @Test
    public void testBookTraceWithHttp() throws Exception {
        final WebClient wc = createWebClient("/web/bookstore/trace");
        try (Response response = wc.invoke("TRACE", null)) {
            assertThat(response.getStatus(), equalTo(406));
        }        
    }

    private WebClient createWebClient(final String path) {
        final WebClient wc = WebClient
            .create(getAddress() + getContext() + path)
            .accept("text/plain");
        
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