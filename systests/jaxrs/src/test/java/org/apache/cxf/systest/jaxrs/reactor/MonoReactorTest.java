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

package org.apache.cxf.systest.jaxrs.reactor;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.reactor.client.ReactorInvoker;
import org.apache.cxf.jaxrs.reactor.client.ReactorInvokerProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import reactor.test.StepVerifier;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class MonoReactorTest extends AbstractBusClientServerTestBase {
    public static final String PORT = ReactorServer.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(ReactorServer.class, true));
        createStaticBus();
    }
    @Test
    public void testGetHelloWorldJson() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor/mono/textJson";

        StepVerifier
            .create(ClientBuilder
                .newClient()
                .register(new JacksonJsonProvider())
                .register(new ReactorInvokerProvider())
                .target(address)
                .request(MediaType.APPLICATION_JSON)
                .rx(ReactorInvoker.class)
                .get(HelloWorldBean.class))
            .expectNextMatches(r -> "Hello".equals(r.getGreeting()) && "World".equals(r.getAudience()))
            .expectComplete()
            .verify();
    }

    @Test
    public void testTextJsonImplicitListAsyncStream() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor/mono/textJsonImplicitListAsyncStream";
        
        StepVerifier
            .create(ClientBuilder
                .newClient()
                .register(new JacksonJsonProvider())
                .register(new ReactorInvokerProvider())
                .target(address)
                .request(MediaType.APPLICATION_JSON)
                .rx(ReactorInvoker.class)
                .get(HelloWorldBean.class))
            .expectNextMatches(r -> "Hello".equals(r.getGreeting()) && "World".equals(r.getAudience()))
            .expectComplete()
            .verify();
    }

    @Test
    public void testGetString() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor/mono/textAsync";
        
        StepVerifier
            .create(ClientBuilder
                .newClient()
                .register(new ReactorInvokerProvider())
                .target(address)
                .request(MediaType.TEXT_PLAIN)
                .rx(ReactorInvoker.class)
                .get(String.class))
            .expectNextMatches(r -> "Hello, world!".equals(r))
            .expectComplete()
            .verify();
    }
    
    @Test
    public void testMonoEmpty() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor/mono/empty";
        
        StepVerifier
            .create(ClientBuilder
                .newClient()
                .register(new JacksonJsonProvider())
                .register(new ReactorInvokerProvider())
                .target(address)
                .request(MediaType.APPLICATION_JSON)
                .rx(ReactorInvoker.class)
                .get(HelloWorldBean.class))
            .expectComplete()
            .verify();
    }
    
    @Test
    public void testGetError() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor/mono/error";
        
        StepVerifier
        .create(ClientBuilder
            .newClient()
            .register(new JacksonJsonProvider())
            .register(new ReactorInvokerProvider())
            .target(address)
            .request(MediaType.APPLICATION_JSON)
            .rx(ReactorInvoker.class)
            .get(HelloWorldBean.class))
        .expectErrorMatches(ex -> ex.getCause() instanceof InternalServerErrorException)
        .verify();
    }
}
