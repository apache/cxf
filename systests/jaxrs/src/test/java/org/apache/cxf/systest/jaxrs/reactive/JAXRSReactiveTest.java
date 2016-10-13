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
import javax.ws.rs.core.GenericType;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.rx.client.ObservableRxInvoker;
import org.apache.cxf.jaxrs.rx.provider.ObservableReader;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import rx.Observable;

public class JAXRSReactiveTest extends AbstractBusClientServerTestBase {
    public static final String PORT = ReactiveServer.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(ReactiveServer.class, true));
        createStaticBus();
    }
    @Test
    public void testGetHelloWorldText() throws Exception {
        String address = "http://localhost:" + PORT + "/reactive/text";
        WebClient wc = WebClient.create(address);
        String text = wc.accept("text/plain").get(String.class);
        assertEquals("Hello, world!", text);
    }
    @Test
    public void testGetHelloWorldAsyncText() throws Exception {
        String address = "http://localhost:" + PORT + "/reactive/textAsync";
        WebClient wc = WebClient.create(address);
        String text = wc.accept("text/plain").get(String.class);
        assertEquals("Hello, world!", text);
    }
    
    @Test
    public void testGetHelloWorldTextObservableSync() throws Exception {
        String address = "http://localhost:" + PORT + "/reactive/text";
        WebClient wc = WebClient.create(address, Collections.singletonList(
            new ObservableReader<Object>()));
        GenericType<Observable<String>> genericResponseType = 
            new GenericType<Observable<String>>() {        
            };
        Observable<String> obs = wc.accept("text/plain").get(genericResponseType);
        obs.subscribe(s -> assertResponse(s));
    }
    
    private void assertResponse(String s) {
        assertEquals("Hello, world!", s);
    }
    @Test
    public void testGetHelloWorldJson() throws Exception {
        String address = "http://localhost:" + PORT + "/reactive/textJson";
        WebClient wc = WebClient.create(address,
                                        Collections.singletonList(new JacksonJsonProvider()));
        HelloWorldBean bean = wc.accept("application/json").get(HelloWorldBean.class);
        assertEquals("Hello", bean.getGreeting());
        assertEquals("World", bean.getAudience());
    }
    @Test
    public void testGetHelloWorldJsonList() throws Exception {
        String address = "http://localhost:" + PORT + "/reactive/textJsonList";
        doTestGetHelloWorldJsonList(address);
    }
    @Test
    public void testGetHelloWorldJsonImplicitList() throws Exception {
        String address = "http://localhost:" + PORT + "/reactive/textJsonImplicitList";
        doTestGetHelloWorldJsonList(address);
    }
    @Test
    public void testGetHelloWorldJsonImplicitListAsync() throws Exception {
        String address = "http://localhost:" + PORT + "/reactive/textJsonImplicitListAsync";
        doTestGetHelloWorldJsonList(address);
    }
    @Test
    public void testGetHelloWorldJsonImplicitListAsyncStream() throws Exception {
        String address = "http://localhost:" + PORT + "/reactive/textJsonImplicitListAsyncStream";
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
        String address = "http://localhost:" + PORT + "/reactive/textAsync";
        WebClient wc = WebClient.create(address);
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
        String address = "http://localhost:" + PORT + "/reactive/textAsync404";
        WebClient wc = WebClient.create(address);
        wc.rx(ObservableRxInvoker.class).get(String.class).subscribe(
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
