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

import java.util.concurrent.Future;

import javax.ws.rs.core.GenericType;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.emory.mathcs.backport.java.util.Collections;
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
    
    @Test
    public void testGetHelloWorldAsyncObservable() throws Exception {
        String address = "http://localhost:" + PORT + "/reactive/textAsync";
        WebClient wc = WebClient.create(address);
        Observable<String> obs = 
            getObservable(wc.accept("text/plain").async().get(String.class));
        obs.subscribe(s -> assertResponse(s));
    }
    
    private void assertResponse(String s) {
        assertEquals("Hello, world!", s);
    }
    @Test
    public void testGetHelloWorldJson() throws Exception {
        String address = "http://localhost:" + PORT + "/reactive/textJson";
        WebClient wc = WebClient.create(address);
        String text = wc.accept("application/json").get(String.class);
        assertTrue("{\"audience\":\"World\",\"greeting\":\"Hello\"}".equals(text)
                   || "{\"greeting\":\"Hello\",\"audience\":\"World\"}".equals(text));
    }
    
    private Observable<String> getObservable(Future<String> future) {
        return Observable.from(future);
    }
}
