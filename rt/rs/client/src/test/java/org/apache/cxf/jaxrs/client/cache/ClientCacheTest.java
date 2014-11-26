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
package org.apache.cxf.jaxrs.client.cache;


import java.net.HttpURLConnection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.client.spec.InvocationBuilderImpl;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.transport.local.LocalConduit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class ClientCacheTest extends Assert {
    public static final String ADDRESS = "local://transport";
    private static Server server;

    @BeforeClass
    public static void bind() throws Exception {
        final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(TheServer.class);
        sf.setResourceProvider(TheServer.class, new SingletonResourceProvider(new TheServer(), false));
        sf.setAddress(ADDRESS);
        server = sf.create();
    }

    @AfterClass
    public static void unbind() throws Exception {
        server.stop();
        server.destroy();
    }

    @Test
    @Ignore
    public void testCache() {
        final WebTarget base = ClientBuilder.newBuilder().register(CacheControlFeature.class).build().target(ADDRESS);
        final Invocation.Builder cached = setAsLocal(base.request()).header(HttpHeaders.CACHE_CONTROL, "public");
        final Response r = cached.get();
        assertEquals(r.getStatus(), HttpURLConnection.HTTP_OK);
        final String r1 = r.readEntity(String.class);
        waitABit();
        assertEquals(r1, cached.get().readEntity(String.class));
    }

    private static Invocation.Builder setAsLocal(final Invocation.Builder client) {
        WebClient.getConfig(InvocationBuilderImpl.class.cast(client).getWebClient())
            .getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        return client;
    }

    private static void waitABit() {
        try { // just to be sure
            Thread.sleep(150);
        } catch (final InterruptedException e) {
            Thread.interrupted();
        }
    }

    @Path("/")
    public static class TheServer {
        @GET
        @ClientCache
        public Response array() {
            return Response.ok(Long.toString(System.currentTimeMillis()))
                .tag("123").cacheControl(CacheControl.valueOf("max-age=50000")).build();
        }
    }
}
