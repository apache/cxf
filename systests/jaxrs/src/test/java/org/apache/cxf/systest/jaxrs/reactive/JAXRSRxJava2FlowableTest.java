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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.rx2.client.FlowableRxInvoker;
import org.apache.cxf.jaxrs.rx2.client.FlowableRxInvokerProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSRxJava2FlowableTest extends AbstractBusClientServerTestBase {
    public static final String PORT = RxJava2FlowableServer.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(RxJava2FlowableServer.class, true));
        final Bus bus = createStaticBus();
        // Make sure default JSON-P/JSON-B providers are not loaded
        bus.setProperty(ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);
    }
    @Test
    public void testGetHelloWorldAsyncText() throws Exception {
        String address = "http://localhost:" + PORT + "/rx2/flowable/textAsync";
        WebClient wc = WebClient.create(address);
        String text = wc.accept("text/plain").get(String.class);
        assertEquals("Hello, world!", text);
    }

    @Test
    public void testGetHelloWorldJson() throws Exception {
        String address = "http://localhost:" + PORT + "/rx2/flowable/textJson";
        List<Object> providers = new LinkedList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new FlowableRxInvokerProvider());
        WebClient wc = WebClient.create(address, providers);
        Flowable<HelloWorldBean> obs = wc.accept("application/json")
            .rx(FlowableRxInvoker.class)
            .get(HelloWorldBean.class);

        final TestSubscriber<HelloWorldBean> subscriber = new TestSubscriber<>();
        obs.subscribe(subscriber);

        subscriber.await(3, TimeUnit.SECONDS);
        subscriber.assertResult(new HelloWorldBean("Hello", "World"));
    }

    @Test
    public void testGetHelloWorldJsonImplicitListAsync() throws Exception {
        String address = "http://localhost:" + PORT + "/rx2/flowable/textJsonImplicitListAsync";
        doTestGetHelloWorldJsonList(address);
    }
    @Test
    public void testGetHelloWorldJsonImplicitListAsyncStream() throws Exception {
        String address = "http://localhost:" + PORT + "/rx2/flowable/textJsonImplicitListAsyncStream";
        doTestGetHelloWorldJsonList(address);
    }
    @Test
    public void testGetHelloWorldJsonImplicitList() throws Exception {
        String address = "http://localhost:" + PORT + "/rx22/flowable/textJsonImplicitList";
        doTestGetHelloWorldJsonList(address);
    }
    private void doTestGetHelloWorldJsonList(String address) throws Exception {
        WebClient wc = WebClient.create(address,
                                        Collections.singletonList(new JacksonJsonProvider()));
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000);
        GenericType<List<HelloWorldBean>> genericResponseType = new GenericType<List<HelloWorldBean>>() {
        };

        List<HelloWorldBean> beans = wc.accept("application/json").get(genericResponseType);
        assertEquals(2, beans.size());
        assertEquals("Hello", beans.get(0).getGreeting());
        assertEquals("World", beans.get(0).getAudience());
        assertEquals("Ciao", beans.get(1).getGreeting());
        assertEquals("World", beans.get(1).getAudience());
    }

    @Test
    public void testGetHelloWorldJsonSingle() throws Exception {
        String address = "http://localhost:" + PORT + "/rx22/flowable/textJsonSingle";
        WebClient wc = WebClient.create(address,
                                        Collections.singletonList(new JacksonJsonProvider()));

        HelloWorldBean bean = wc.accept("application/json").get(HelloWorldBean.class);
        assertEquals("Hello", bean.getGreeting());
        assertEquals("World", bean.getAudience());
    }

    @Test
    public void testGetHelloWorldAsyncObservable() throws Exception {
        String address = "http://localhost:" + PORT + "/rx2/flowable/textAsync";
        WebClient wc = WebClient.create(address,
                                        Collections.singletonList(new FlowableRxInvokerProvider()));
        Flowable<String> obs = wc.accept("text/plain")
            .rx(FlowableRxInvoker.class)
            .get(String.class);

        final TestSubscriber<String> subscriber = new TestSubscriber<>();
        obs.map(s -> s + s).subscribe(subscriber);
        
        subscriber.await(2, TimeUnit.SECONDS);
        subscriber.assertResult("Hello, world!Hello, world!");
    }
    
    @Test
    public void testGetHelloWorldAsyncObservable404() throws Exception {
        String address = "http://localhost:" + PORT + "/rx2/flowable/textAsync404";
        Invocation.Builder b = ClientBuilder.newClient().register(new FlowableRxInvokerProvider())
            .target(address).request();

        final TestSubscriber<String> subscriber = new TestSubscriber<>();
        b.rx(FlowableRxInvoker.class)
            .get(String.class)
            .subscribe(subscriber);
        
        subscriber.await(1, TimeUnit.SECONDS);
        subscriber.assertError(NotFoundException.class);
    }
    
    @Test
    public void testGetHelloWorldEmpty() throws Exception {
        String address = "http://localhost:" + PORT + "/rx2/flowable/empty";

        final Flowable<Response> obs = ClientBuilder
            .newClient()
            .register(new JacksonJsonProvider())
            .register(new FlowableRxInvokerProvider())
            .target(address)
            .request(MediaType.APPLICATION_JSON)
            .rx(FlowableRxInvoker.class)
            .get();

        final TestSubscriber<Response> subscriber = new TestSubscriber<>();
        obs.subscribe(subscriber);

        subscriber.await(3, TimeUnit.SECONDS);
        subscriber
            .assertValue(r -> "[]".equals(r.readEntity(String.class)))
            .assertComplete();
    }

    @Test
    public void testGetHelloWorldEmpty2() throws Exception {
        String address = "http://localhost:" + PORT + "/rx22/flowable/empty";

        final Flowable<Response> obs = ClientBuilder
            .newClient()
            .register(new JacksonJsonProvider())
            .register(new FlowableRxInvokerProvider())
            .target(address)
            .request(MediaType.APPLICATION_JSON)
            .rx(FlowableRxInvoker.class)
            .get();

        final TestSubscriber<Response> subscriber = new TestSubscriber<>();
        obs.subscribe(subscriber);

        subscriber.await(3, TimeUnit.SECONDS);
        subscriber
            .assertValue(r -> "[]".equals(r.readEntity(String.class)))
            .assertComplete();
    }

    @Test
    public void testFlowableImmediateErrors() throws Exception {
        String address = "http://localhost:" + PORT + "/rx22/flowable/immediate/errors";

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
        subscriber.assertError(InternalServerErrorException.class);
    }

    @Test
    public void testFlowableErrorWithExceptionMapperReturnsNoExceptionPayload() throws Exception {
        String address = "http://localhost:" + PORT + "/rx22/flowable/mixed/error";

        final Flowable<Response> obs = ClientBuilder
            .newClient()
            .register(new JacksonJsonProvider())
            .register(new FlowableRxInvokerProvider())
            .target(address)
            .request(MediaType.APPLICATION_JSON)
            .rx(FlowableRxInvoker.class)
            .get();

        final TestSubscriber<Response> subscriber = new TestSubscriber<>();
        obs.subscribe(subscriber);

        // The response should not include the exception payload (injected by exception mapper)
        // if some elements have been emitted before
        subscriber.await(3, TimeUnit.SECONDS);
        subscriber
            .assertValue(r -> r.getStatus() == 409 && !r.readEntity(String.class).contains("stackTrace"))
            .assertComplete();
    }

    @Test
    public void testFlowableErrorWithExceptionMapperReturnsContentPayload() throws Exception {
        GenericType<List<HelloWorldBean>> helloWorldBeanListType = new GenericType<List<HelloWorldBean>>() {  };
        String address = "http://localhost:" + PORT + "/rx22/flowable/mixed/error";

        final Flowable<Response> obs = ClientBuilder
            .newClient()
            .register(new JacksonJsonProvider())
            .register(new FlowableRxInvokerProvider())
            .target(address)
            .request(MediaType.APPLICATION_JSON)
            .rx(FlowableRxInvoker.class)
            .get();

        final TestSubscriber<Response> subscriber = new TestSubscriber<>();
        obs.subscribe(subscriber);

        // The response should include the emitted elements prior the error
        subscriber.await(3, TimeUnit.SECONDS);
        subscriber
            .assertValue(r -> r.getStatus() == 409 && r.readEntity(helloWorldBeanListType).size() == 4)
            .assertComplete();
    }

    @Test
    public void testFlowableErrorsResponseWithMapper() throws Exception {
        String address = "http://localhost:" + PORT + "/rx22/flowable/mapper/errors";

        final Flowable<Response> obs = ClientBuilder
            .newClient()
            .register(new JacksonJsonProvider())
            .register(new FlowableRxInvokerProvider())
            .target(address)
            .request(MediaType.APPLICATION_JSON)
            .rx(FlowableRxInvoker.class)
            .get();

        final TestSubscriber<Response> subscriber = new TestSubscriber<>();
        obs.subscribe(subscriber);

        subscriber.await(3, TimeUnit.SECONDS);
        subscriber
            .assertValue(r -> r.getStatus() == 400)
            .assertComplete();
    }

    @Test
    public void testFlowableErrorWithWebException() throws Exception {
        String address = "http://localhost:" + PORT + "/rx22/flowable/web/errors";

        final Flowable<Response> obs = ClientBuilder
            .newClient()
            .register(new JacksonJsonProvider())
            .register(new FlowableRxInvokerProvider())
            .target(address)
            .request(MediaType.APPLICATION_JSON)
            .rx(FlowableRxInvoker.class)
            .get();

        final TestSubscriber<Response> subscriber = new TestSubscriber<>();
        obs.subscribe(subscriber);

        // The response should not include the exception payload (injected by exception mapper)
        // if some elements have been emitted before
        subscriber.await(3, TimeUnit.SECONDS);
        subscriber
            .assertValue(r -> r.getStatus() == 403 && !r.readEntity(String.class).contains("stackTrace"))
            .assertComplete();
    }

    @Test
    public void testFlowableImmediateErrorsWithExceptionMapper() throws Exception {
        String address = "http://localhost:" + PORT + "/rx22/flowable/immediate/mapper/errors";

        final Flowable<Response> obs = ClientBuilder
                .newClient()
                .register(new JacksonJsonProvider())
                .register(new FlowableRxInvokerProvider())
                .target(address)
                .request(MediaType.APPLICATION_JSON)
                .rx(FlowableRxInvoker.class)
                .get();

        final TestSubscriber<Response> subscriber = new TestSubscriber<>();
        obs.subscribe(subscriber);

        subscriber.await(3, TimeUnit.SECONDS);
        subscriber
            .assertValue(r -> r.getStatus() == 409 && r.readEntity(String.class).contains("stackTrace"))
            .assertComplete();
    }
}
