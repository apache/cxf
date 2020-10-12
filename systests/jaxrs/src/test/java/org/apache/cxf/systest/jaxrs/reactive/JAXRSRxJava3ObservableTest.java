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

import javax.ws.rs.core.GenericType;
import javax.xml.ws.Holder;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.rx3.client.ObservableRxInvoker;
import org.apache.cxf.jaxrs.rx3.client.ObservableRxInvokerProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSRxJava3ObservableTest extends AbstractBusClientServerTestBase {
    public static final String PORT = RxJava3ObservableServer.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(RxJava3ObservableServer.class, true));
        createStaticBus();
    }
    @Test
    public void testGetHelloWorldText() throws Exception {
        String address = "http://localhost:" + PORT + "/rx3/observable/text";
        WebClient wc = WebClient.create(address);
        String text = wc.accept("text/plain").get(String.class);
        assertEquals("Hello, world!", text);
    }

    @Test
    public void testGetHelloWorldJson() throws Exception {
        String address = "http://localhost:" + PORT + "/rx3/observable/textJson";
        List<Object> providers = new LinkedList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new ObservableRxInvokerProvider());
        WebClient wc = WebClient.create(address, providers);
        Observable<HelloWorldBean> obs = wc.accept("application/json")
            .rx(ObservableRxInvoker.class)
            .get(HelloWorldBean.class);

        Holder<HelloWorldBean> holder = new Holder<>();
        Disposable d = obs.subscribe(v -> {
            holder.value = v;
        });
        if (d == null) {
            throw new IllegalStateException("Subscribe did not return a Disposable");
        }
        Thread.sleep(2000);
        assertEquals("Hello", holder.value.getGreeting());
        assertEquals("World", holder.value.getAudience());
    }

    @Test
    public void testGetHelloWorldJsonList() throws Exception {
        String address = "http://localhost:" + PORT + "/rx3/observable/textJsonList";
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
}
