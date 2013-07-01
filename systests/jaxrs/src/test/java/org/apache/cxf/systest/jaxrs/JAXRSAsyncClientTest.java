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

package org.apache.cxf.systest.jaxrs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.xml.ws.Holder;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSAsyncClientTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerAsyncClient.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(BookServerAsyncClient.class, true));
        createStaticBus();
    }
    
    @Test
    public void testRetrieveBookCustomMethodAsyncSync() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/retrieve";
        WebClient wc = WebClient.create(address);
        wc.type("application/xml").accept("application/xml");
        WebClient.getConfig(wc).getRequestContext().put("use.async.http.conduit", true);
        Book book = wc.invoke("RETRIEVE", new Book("Retrieve", 123L), Book.class);
        assertEquals("Retrieve", book.getName());
    }
    
    @Test
    public void testRetrieveBookCustomMethodAsync() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/retrieve";
        WebClient wc = WebClient.create(address);
        wc.accept("application/xml");
        Future<Book> book = wc.async().method("RETRIEVE", Entity.xml(new Book("Retrieve", 123L)), 
                                              Book.class);
        assertEquals("Retrieve", book.get().getName());
    }
    
    @Test
    @Ignore
    public void testGetBookAsync404() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/404";
        WebClient wc = createWebClient(address);
        Future<Book> future = wc.async().get(Book.class);
        Book book = future.get();
        assertEquals(124L, book.getId());
    }
    
    @Test
    @Ignore
    public void testGetBookAsync404Callback() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/404";
        WebClient wc = createWebClient(address);
        final Holder<Book> holder = new Holder<Book>();
        InvocationCallback<Book> callback = createCallback(holder);
        Future<Book> future = wc.async().get(callback);
        Book book = future.get();
        assertEquals(124L, book.getId());
    }
    
    private WebClient createWebClient(String address) {
        List<Object> providers = new ArrayList<Object>();
        WebClient wc = WebClient.create(address, providers);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        return wc;
    }
    
    private InvocationCallback<Book> createCallback(final Holder<Book> holder) {
        return new InvocationCallback<Book>() {
            public void completed(Book response) {
                holder.value = response;
            }
            public void failed(Throwable error) {
                error.printStackTrace();
            }
        };
    }
    
}
