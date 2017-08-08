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
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.rx.client.ObservableRxInvoker;
import org.apache.cxf.jaxrs.rx.client.ObservableRxInvokerProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import rx.Observable;

public class JAXRSObservableTest extends AbstractBusClientServerTestBase {
    public static final String PORT = ObservableServer.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(ObservableServer.class, true));
        createStaticBus();
    }
    @Test
    public void testGetHelloWorldText() throws Exception {
        String address = "http://localhost:" + PORT + "/observable/text";
        WebClient wc = WebClient.create(address);
        String text = wc.accept("text/plain").get(String.class);
        assertEquals("Hello, world!", text);
    }
    @Test
    public void testGetHelloWorldAsyncText() throws Exception {
        String address = "http://localhost:" + PORT + "/observable/textAsync";
        WebClient wc = WebClient.create(address);
        String text = wc.accept("text/plain").get(String.class);
        assertEquals("Hello, world!", text);
    }

    @Test
    public void testGetHelloWorldJson() throws Exception {
        String address = "http://localhost:" + PORT + "/observable/textJson";
        WebClient wc = WebClient.create(address,
                                        Collections.singletonList(new JacksonJsonProvider()));
        HelloWorldBean bean = wc.accept("application/json").get(HelloWorldBean.class);
        assertEquals("Hello", bean.getGreeting());
        assertEquals("World", bean.getAudience());
    }
    @Test
    public void testGetHelloWorldJsonList() throws Exception {
        String address = "http://localhost:" + PORT + "/observable/textJsonList";
        doTestGetHelloWorldJsonList(address);
    }
    @Test
    public void testGetHelloWorldJsonImplicitListAsync() throws Exception {
        String address = "http://localhost:" + PORT + "/observable/textJsonImplicitListAsync";
        doTestGetHelloWorldJsonList(address);
    }
    @Test
    public void testGetHelloWorldJsonImplicitListAsyncStream() throws Exception {
        String address = "http://localhost:" + PORT + "/observable/textJsonImplicitListAsyncStream";
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
    public void testGetHelloWorldAsyncObservable() throws Exception {
        String address = "http://localhost:" + PORT + "/observable/textAsync";
        WebClient wc = WebClient.create(address,
                                        Collections.singletonList(new ObservableRxInvokerProvider()));
        Observable<String> obs = wc.accept("text/plain")
            .rx(ObservableRxInvoker.class)
            .get(String.class);
        obs.map(s -> {
            return s + s;
        });

        Thread.sleep(3000);

        obs.subscribe(s -> assertDuplicateResponse(s));
    }
    @Test
    public void testGetHelloWorldAsyncObservable404() throws Exception {
        String address = "http://localhost:" + PORT + "/observable/textAsync404";
        Invocation.Builder b = ClientBuilder.newClient().register(new ObservableRxInvokerProvider())
            .target(address).request();
        b.rx(ObservableRxInvoker.class).get(String.class).subscribe(
            s -> {
                fail("Exception expected");
            },
            t -> validateT((ExecutionException)t));
    }

    private void validateT(ExecutionException t) {
        assertTrue(t.getCause() instanceof NotFoundException);
    }
    private void assertDuplicateResponse(String s) {
        assertEquals("Hello, world!Hello, world!", s);
    }
}
