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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSOverlappingDestinationsTest extends AbstractBusClientServerTestBase {
    public static final String PORT = SpringServer.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(SpringServer.class, true));
    }
    
    @Test
    public void testAbsolutePathOne() throws Exception {
        
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/one/bookstore/request");
        String path = wc.accept("text/plain").get(String.class);
        assertEquals("Absolute RequestURI is wrong", wc.getBaseURI().toString(), path);
    }
    
    @Test
    public void testAbsolutePathTwo() throws Exception {
        
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/two/bookstore/request");
        String path = wc.accept("text/plain").get(String.class);
        assertEquals("Absolute RequestURI is wrong", wc.getBaseURI().toString(), path);
    }
    
    @Test
    public void testAbsolutePathOneAndTwo() throws Exception {
        
        final String requestURI = "http://localhost:" + PORT + "/one/bookstore/request?delay";
        
        Callable<String> callable = new Callable<String>() {
            public String call() {
                WebClient wc = WebClient.create(requestURI);
                return wc.accept("text/plain").get(String.class);
                    
            }
        };
        FutureTask<String> task = new FutureTask<String>(callable);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(task);
        Thread.sleep(1000);
        
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    testAbsolutePathTwo();
                } catch (Exception ex) {
                    throw new RuntimeException("Concurrent testAbsolutePathTwo failed");
                }
            }
        };
        new Thread(runnable).start();
        Thread.sleep(2000);
        
        String path = task.get();
        assertEquals("Absolute RequestURI is wrong", requestURI, path);
        
        
    }
    
    @Test
    public void testAbsolutePathOneAndTwoWithLock() throws Exception {
        
        WebClient.create("http://localhost:" + PORT + "/one/bookstore/lock").accept("text/plain").get();
        
        final String requestURI = "http://localhost:" + PORT + "/one/bookstore/uris";
        
        Callable<String> callable = new Callable<String>() {
            public String call() {
                WebClient wc = WebClient.create(requestURI);
                return wc.accept("text/plain").get(String.class);
                
            }
        };
        FutureTask<String> task = new FutureTask<String>(callable);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(task);
        Thread.sleep(3000);
        
        WebClient wc2 = WebClient.create("http://localhost:" + PORT + "/two/bookstore/unlock");
        wc2.accept("text/plain").get();
        
        String path = task.get();
        assertEquals("Absolute RequestURI is wrong", requestURI, path);
    }
    
    @Ignore
    public static class SpringServer extends AbstractSpringServer {
        public static final String PORT = AbstractSpringServer.PORT;
        
        public SpringServer() {
            super("/jaxrs_many_destinations");
        }
    }
    
    @Path("/bookstore")
    public static class Resource {

        private volatile boolean locked;
        
        @GET
        @Produces("text/plain")
        @Path("request")
        public String getRequestPath(@Context UriInfo ui, @QueryParam("delay") String delay) 
            throws Exception {
            if (delay != null) {
                Thread.sleep(5000);
            }
            return ui.getRequestUri().toString();
        }
        
        
        @GET
        @Path("/uris")
        @Produces("text/plain")
        public String getUris(@Context UriInfo uriInfo) {
            String baseUriOnEntry = uriInfo.getRequestUri().toString();
            try {
                while (locked) { Thread.sleep(1000); }
            } catch (InterruptedException x) {
                // ignore
            }
            String baseUriOnExit = uriInfo.getRequestUri().toString();
            if (!baseUriOnEntry.equals(baseUriOnExit)) {
                throw new RuntimeException();
            }
            return baseUriOnExit;
        }

        @GET
        @Path("/lock")
        @Produces("text/plain")
        public String lock() { locked = true; return "locked"; }

        @GET
        @Path("/unlock")
        @Produces("text/plain")
        public String unlock() { locked = false; return "unlocked"; }
    }
}
