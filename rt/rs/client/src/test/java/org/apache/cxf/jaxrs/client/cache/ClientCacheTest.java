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


import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.transport.local.LocalConduit;
import org.apache.cxf.transport.local.LocalTransportFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ClientCacheTest {
    public static final String ADDRESS = "local://transport";
    private static Server server;

    @BeforeClass
    public static void bind() throws Exception {
        final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(TheServer.class);
        sf.setResourceProvider(TheServer.class, new SingletonResourceProvider(new TheServer(), false));
        sf.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        sf.setAddress(ADDRESS);
        server = sf.create();
    }

    @AfterClass
    public static void unbind() throws Exception {
        server.stop();
        server.destroy();
    }

    @Test
    public void testGetTimeString() {
        try (CacheControlFeature feature = new CacheControlFeature()) {
            final WebTarget base = ClientBuilder.newBuilder().register(feature).build().target(ADDRESS);
            final Invocation.Builder cached = base.request("text/plain").header(HttpHeaders.CACHE_CONTROL, "public");
            final Response r = cached.get();
            assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
            final String r1 = r.readEntity(String.class);
            waitABit();
            assertEquals(r1, cached.get().readEntity(String.class));
        }
    }

    @Test
    public void testGetTimeStringUsingClientFactory() {
        try (CacheControlFeature feature = new CacheControlFeature()) {
            JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
            bean.setAddress(ADDRESS);
            bean.setProviders(Collections.singletonList(feature));
            bean.setTransportId(LocalTransportFactory.TRANSPORT_ID);
            
            final WebClient cached = bean.createWebClient()
                .accept("text/plain")
                .header(HttpHeaders.CACHE_CONTROL, "public");
            
            final Response r = cached.get();
            assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
            final String r1 = r.readEntity(String.class);
            waitABit();
            assertEquals(r1, cached.get().readEntity(String.class));
        }
    }

    @Test
    public void testGetTimeStringAsInputStream() throws Exception {
        try (CacheControlFeature feature = new CacheControlFeature()) {
            final WebTarget base = ClientBuilder.newBuilder().register(feature).build().target(ADDRESS);
            final Invocation.Builder cached = base.request("text/plain").header(HttpHeaders.CACHE_CONTROL, "public");
            final Response r = cached.get();
            assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
            InputStream is = r.readEntity(InputStream.class);
            final String r1 = IOUtils.readStringFromStream(is);
            waitABit();
            is = cached.get().readEntity(InputStream.class);
            final String r2 = IOUtils.readStringFromStream(is);
            assertEquals(r1, r2);
        }
    }

    @Test
    public void testGetTimeStringAsInputStreamAndString() throws Exception {
        try (CacheControlFeature feature = new CacheControlFeature()) {
            feature.setCacheResponseInputStream(true);
            final WebTarget base = ClientBuilder.newBuilder().register(feature).build().target(ADDRESS);
            final Invocation.Builder cached = base.request("text/plain").header(HttpHeaders.CACHE_CONTROL, "public");
            final Response r = cached.get();
            assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
            InputStream is = r.readEntity(InputStream.class);
            final String r1 = IOUtils.readStringFromStream(is);
            waitABit();
            // CassCastException would occur without a cached stream support
            final String r2 = cached.get().readEntity(String.class);
            assertEquals(r1, r2);
        }
    }
    @Test
    public void testGetTimeStringAsStringAndInputStream() throws Exception {
        try (CacheControlFeature feature = new CacheControlFeature()) {
            feature.setCacheResponseInputStream(true);
            final WebTarget base = ClientBuilder.newBuilder().register(feature).build().target(ADDRESS);
            final Invocation.Builder cached = base.request("text/plain").header(HttpHeaders.CACHE_CONTROL, "public");
            final Response r = cached.get();
            assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
            final String r1 = r.readEntity(String.class);
            waitABit();
            // CassCastException would occur without a cached stream support
            InputStream is = cached.get().readEntity(InputStream.class);
            final String r2 = IOUtils.readStringFromStream(is);
            assertEquals(r1, r2);
        }
    }
    
    @Test
    public void testGetTimeStringAsStringAndInputStreamWithJAXRSClientFactoryBean() throws Exception {
        try (CXFCacheControlFeature feature = new CXFCacheControlFeature(true)) {
            WebClient client = createClient(feature);
            final Response r = client.get();
            assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
            final String r1 = r.readEntity(String.class);
            waitABit();
            InputStream is = client.get(InputStream.class);
            final String r2 = IOUtils.readStringFromStream(is);
            assertEquals(r1, r2);
        }
    }

    @Test
    public void testGetJaxbBookCache() {
        try (CacheControlFeature feature = new CacheControlFeature()) {
            final WebTarget base = ClientBuilder.newBuilder().register(feature).build().target(ADDRESS);
            final Invocation.Builder cached =
                setAsLocal(base.request("application/xml")).header(HttpHeaders.CACHE_CONTROL, "public");
            final Response r = cached.get();
            assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
            final Book b1 = r.readEntity(Book.class);
            assertEquals("JCache", b1.getName());
            assertNotNull(b1.getId());
            waitABit();
            assertEquals(b1, cached.get().readEntity(Book.class));
        }
    }
    

    @Test
    public void testGetJaxbBookCacheByValue() {
        // org.apache.cxf.jaxrs.client.cache.CacheControlFeature.storeByValue
        try (CacheControlFeature feature = new CacheControlFeature()) {
            final WebTarget base = ClientBuilder.newBuilder()
                .property("org.apache.cxf.jaxrs.client.cache.CacheControlFeature.storeByValue", "true")
                .register(feature).build().target(ADDRESS);
            final Invocation.Builder cached =
                setAsLocal(base.request("application/xml")).header(HttpHeaders.CACHE_CONTROL, "public");
            final Response r = cached.get();
            assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
            final Book b1 = r.readEntity(Book.class);
            assertEquals("JCache", b1.getName());
            assertNotNull(b1.getId());
            waitABit();
            assertEquals(b1, cached.get().readEntity(Book.class));
        }
    }

    private static Invocation.Builder setAsLocal(final Invocation.Builder client) {
        WebClient.getConfig(client).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
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
        @Produces("text/plain")
        public Response getString() {
            return Response.ok(Long.toString(System.currentTimeMillis()))
                .tag("123").cacheControl(CacheControl.valueOf("max-age=50000")).build();
        }
        @GET
        @Produces("application/xml")
        public Response getJaxbBook() {
            Book b = new Book();
            b.setId(System.currentTimeMillis());
            b.setName("JCache");
            return Response.ok(b).tag("123").cacheControl(CacheControl.valueOf("max-age=50000")).build();
        }
    }
    @XmlRootElement
    public static class Book implements Serializable {
        private static final long serialVersionUID = 4924824780883333782L;
        private String name;
        private Long id;
        public Book() {

        }
        public Book(String name, long id) {
            this.name = name;
            this.id = id;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public Long getId() {
            return id;
        }
        public void setId(Long id) {
            this.id = id;
        }
        public int hashCode() {
            return id.hashCode() + 37 * name.hashCode();
        }
        public boolean equals(Object o) {
            if (o instanceof Book) {
                Book other = (Book)o;
                return other.id.equals(id) && other.name.equals(name);
            }
            return false;
        }
    }
    
    private WebClient createClient(CXFCacheControlFeature feature) {
        Bus bus = BusFactory.newInstance().createBus();
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setBus(bus);
        sf.setResourceClasses(TheServer.class);
        sf.setResourceProvider(TheServer.class, new SingletonResourceProvider(new TheServer(), false));
        sf.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        sf.setAddress(ADDRESS);
        sf.create();
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setBus(bus);
        bean.setAddress(ADDRESS);
        bean.setFeatures(Collections.singletonList(feature));
        bean.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        return bean.createWebClient().accept("text/plain").header(HttpHeaders.CACHE_CONTROL, "public");
    }

}
