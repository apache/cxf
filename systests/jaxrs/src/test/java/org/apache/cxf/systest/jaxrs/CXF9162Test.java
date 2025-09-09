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

import java.util.concurrent.CompletableFuture;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class CXF9162Test {

    static final String URL = "http://localhost:" 
        + TestUtil.getPortNumber(CXF9162Test.class) + "/";

    static Server server;

    @Before
    public void startServer() {
        JAXRSServerFactoryBean serverFactoryBean = new JAXRSServerFactoryBean();
        serverFactoryBean.setResourceClasses(MyServiceImpl.class);
        serverFactoryBean.setAddress(URL);
        server = serverFactoryBean.create();
    }

    @After
    public void stopServer() {
        server.stop();
    }

    @Test
    public void testMultipleProxyCalls() {
        JAXRSClientFactoryBean clientFactoryBean = new JAXRSClientFactoryBean();
        clientFactoryBean.setAddress(URL);
        clientFactoryBean.setResourceClass(MyService.class);

        // Create the first Client and call the RestService
        MyService myClient1 = clientFactoryBean.create(MyService.class);
        myClient1.hello();

        // Create a second Client, but do not call yet
        MyService myClient2 = clientFactoryBean.create(MyService.class);
        // Register an async GC with finalizer exec and make client1 eligible for gc
        CompletableFuture.runAsync(() -> {
            System.gc();
            System.runFinalization();
            System.gc();
        });
        myClient1 = null;

        try {
            myClient2.hello();
        } catch (Exception ex) {
            //the JDK HttpClient may being shutting down
            assertEquals(ex.getCause().getMessage(), "Client already shutting down");
        }

    }

    @Path("")
    public interface MyService {

        @GET
        @Path("/hello")
        void hello();
    }

    public static class MyServiceImpl implements MyService {

        @Override
        public void hello() {
            System.out.println("hello");
        }
    }
} 
