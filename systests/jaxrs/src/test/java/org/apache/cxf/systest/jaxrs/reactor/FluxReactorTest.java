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

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.reactor.client.ReactorInvoker;
import org.apache.cxf.jaxrs.reactor.client.ReactorInvokerProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import reactor.test.StepVerifier;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class FluxReactorTest extends AbstractBusClientServerTestBase {
    public static final String PORT = ReactorServer.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(ReactorServer.class, true));
        createStaticBus();
    }
    @Test
    public void testGetHelloWorldJson() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor/flux/textJson";
        StepVerifier
            .create(ClientBuilder
                .newClient()
                .register(new JacksonJsonProvider())
                .register(new ReactorInvokerProvider())
                .target(address)
                .request(MediaType.APPLICATION_JSON)
                .rx(ReactorInvoker.class)
                .get(HelloWorldBean.class))
            .expectNextMatches(bean -> bean.getGreeting().equals("Hello") && bean.getAudience().equals("World"))
            .expectComplete()
            .verify();
    }

    @Test
    public void testTextJsonImplicitListAsyncStream() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor/flux/textJsonImplicitListAsyncStream";
        doTestTextJsonImplicitListAsyncStream(address);
    }
    @Test
    public void testTextJsonImplicitListAsyncStream2() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor2/flux/textJsonImplicitListAsyncStream2";
        doTestTextJsonImplicitListAsyncStream(address);
    }
    
    @Test
    public void testFluxEmpty() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor2/flux/empty";
        
        StepVerifier
            .create(ClientBuilder
                .newClient()
                .register(new JacksonJsonProvider())
                .register(new ReactorInvokerProvider())
                .target(address)
                .request(MediaType.APPLICATION_JSON)
                .rx(ReactorInvoker.class)
                .getFlux(HelloWorldBean.class))
            .expectComplete()
            .verify();
    }
    
    @Test
    public void testFluxErrors() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor2/flux/errors";
        
        StepVerifier
            .create(ClientBuilder
                .newClient()
                .register(new JacksonJsonProvider())
                .register(new ReactorInvokerProvider())
                .target(address)
                .request(MediaType.APPLICATION_JSON)
                .rx(ReactorInvoker.class)
                .getFlux(HelloWorldBean.class))
            .expectNextMatches(b -> b.getGreeting().equalsIgnoreCase("Person 1"))
            .expectComplete()
            .verify();
    }
    
    @Test
    public void testFluxErrorsResponse() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor2/flux/errors";
        
        StepVerifier
            .create(ClientBuilder
                .newClient()
                .register(new JacksonJsonProvider())
                .register(new ReactorInvokerProvider())
                .target(address)
                .request(MediaType.APPLICATION_JSON)
                .rx(ReactorInvoker.class)
                .get())
            .expectNextMatches(r -> r.getStatus() == 500)
            .expectComplete()
            .verify();
    }
    
    @Test
    public void testFluxErrorsResponseWithMapper() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor2/flux/mapper/errors";
        
        StepVerifier
            .create(ClientBuilder
                .newClient()
                .register(new JacksonJsonProvider())
                .register(new ReactorInvokerProvider())
                .target(address)
                .request(MediaType.APPLICATION_JSON)
                .rx(ReactorInvoker.class)
                .get())
            .expectNextMatches(r -> r.getStatus() == 400)
            .expectComplete()
            .verify();
    }
    
    @Test
    public void testFluxImmediateErrors() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor2/flux/immediate/errors";
        
        StepVerifier
            .create(ClientBuilder
                .newClient()
                .register(new JacksonJsonProvider())
                .register(new ReactorInvokerProvider())
                .target(address)
                .request(MediaType.APPLICATION_JSON)
                .rx(ReactorInvoker.class)
                .getFlux(HelloWorldBean.class))
            .expectError()
            .verify();
    }

    @Test
    public void testFluxImmediateErrorsResponse() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor2/flux/immediate/errors";
        
        StepVerifier
            .create(ClientBuilder
                .newClient()
                .register(new JacksonJsonProvider())
                .register(new ReactorInvokerProvider())
                .target(address)
                .request(MediaType.APPLICATION_JSON)
                .rx(ReactorInvoker.class)
                .get())
            .expectNextMatches(r -> r.getStatus() == 500)
            .expectComplete()
            .verify();
    }

    private void doTestTextJsonImplicitListAsyncStream(String address) throws Exception {
        StepVerifier
            .create(ClientBuilder
                .newClient()
                .register(new JacksonJsonProvider())
                .register(new ReactorInvokerProvider())
                .target(address)
                .request(MediaType.APPLICATION_JSON)
                .rx(ReactorInvoker.class)
                .getFlux(HelloWorldBean.class))
            .expectNextMatches(bean -> bean.getGreeting().equals("Hello") && bean.getAudience().equals("World"))
            .expectNextMatches(bean -> bean.getGreeting().equals("Ciao") && bean.getAudience().equals("World"))
            .expectComplete()
            .verify();
    }
}
