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

package org.apache.cxf.systest.jaxrs.reactive;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.rx3.client.FlowableRxInvoker;
import org.apache.cxf.jaxrs.rx3.client.FlowableRxInvokerProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


public class JAXRSRxJava3SingleTest extends AbstractBusClientServerTestBase {
    public static final String PORT = RxJava3SingleServer.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(RxJava3SingleServer.class, true));
        createStaticBus();
    }
    @Test
    public void testGetHelloWorldJson() throws Exception {
        String address = "http://localhost:" + PORT + "/rx3/single/textJson";

        final Flowable<HelloWorldBean> obs = ClientBuilder
            .newClient()
            .register(new JacksonJsonProvider())
            .register(new FlowableRxInvokerProvider())
            .target(address)
            .request(MediaType.APPLICATION_JSON)
            .rx(FlowableRxInvoker.class)
            .get(HelloWorldBean.class);
        
        final TestSubscriber<HelloWorldBean> subscriber = new TestSubscriber<>();
        obs.subscribe(subscriber);

        subscriber.await(3, TimeUnit.SECONDS);
        subscriber
            .assertValue(r -> "Hello".equals(r.getGreeting()) && "World".equals(r.getAudience()))
            .assertComplete();
    }

    @Test
    public void testGetString() throws Exception {
        String address = "http://localhost:" + PORT + "/rx3/single/textAsync";
        
        final Flowable<String> obs = ClientBuilder
            .newClient()
            .register(new FlowableRxInvokerProvider())
            .target(address)
            .request(MediaType.TEXT_PLAIN)
            .rx(FlowableRxInvoker.class)
            .get(String.class);
        
        final TestSubscriber<String> subscriber = new TestSubscriber<>();
        obs.subscribe(subscriber);

        subscriber.await(3, TimeUnit.SECONDS);
        subscriber
            .assertValue(r -> "Hello, world!".equals(r))
            .assertComplete();
    }
    
    @Test
    public void testGetError() throws Exception {
        String address = "http://localhost:" + PORT + "/rx3/single/error";
        
        final Flowable<String> obs = ClientBuilder
            .newClient()
            .register(new FlowableRxInvokerProvider())
            .target(address)
            .request(MediaType.APPLICATION_JSON)
            .rx(FlowableRxInvoker.class)
            .get(String.class);
        
        final TestSubscriber<String> subscriber = new TestSubscriber<>();
        obs.subscribe(subscriber);

        subscriber.await(3, TimeUnit.SECONDS);
        subscriber.assertError(InternalServerErrorException.class);
    }
}
