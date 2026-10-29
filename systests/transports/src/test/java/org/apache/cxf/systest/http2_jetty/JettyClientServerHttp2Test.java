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

package org.apache.cxf.systest.http2_jetty;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.MessageTrustDecider;
import org.apache.cxf.transport.http.URLConnectionInfo;
import org.apache.cxf.transport.http.UntrustedURLConnectionIOException;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.transport.https.InsecureTrustManager;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class JettyClientServerHttp2Test extends AbstractJettyClientServerHttp2Test {
    private static final String PORT = BookServerHttp2.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(new BookServerHttp2()));
        createStaticBus();
    }

    
    /**
     * The trust decision is made after the request is sent and before its body is written. Without
     * chunking, the HttpClient waits for the whole body before it sends the request, so it must let the
     * trust decision go ahead, over HTTP/2 as well.
     */
    @Test(timeout = 60000)
    public void testHttpsTrustWithoutChunkingHttp2() throws Exception {
        // the URLConnection conduit does not support HTTP/2
        assumeFalse(Boolean.getBoolean("org.apache.cxf.transport.http.forceURLConnection"));
        final WebClient wc = WebClient
            .create(getAddress() + getContext() + "/web/bookstore/echo")
            .type("text/plain")
            .accept("text/plain");

        final ClientConfiguration config = WebClient.getConfig(wc);
        config.getRequestContext().put(HTTPConduit.FORCE_HTTP_VERSION, "2");
        final HTTPConduit conduit = config.getHttpConduit();
        conduit.getClient().setAllowChunking(false);
        TLSClientParameters params = new TLSClientParameters();
        params.setTrustManagers(InsecureTrustManager.getNoOpX509TrustManagers());
        params.setDisableCNCheck(true);
        conduit.setTlsClientParameters(params);
        conduit.setTrustDecider(new MessageTrustDecider() {
            @Override
            public void establishTrust(String conduitName, URLConnectionInfo connectionInfo, Message message)
                throws UntrustedURLConnectionIOException {
                String peer = ((HttpsURLConnectionInfo)connectionInfo).getPeerPrincipal().getName();
                if (!peer.contains("OU=Bethal")) {
                    throw new UntrustedURLConnectionIOException("Untrusted peer " + peer);
                }
            }
        });

        try (Response resp = wc.post("CXF in Action")) {
            assertEquals(200, resp.getStatus());
            assertEquals("HTTP/2.0 CXF in Action", resp.readEntity(String.class));
        }
    }

    @Override
    protected String getAddress() {
        return "https://localhost:" + PORT;
    }
    
    @Override
    protected String getContext() {
        return "/http2";
    }
    
    @Override
    protected int getPort() {
        return Integer.valueOf(PORT);
    }
}
