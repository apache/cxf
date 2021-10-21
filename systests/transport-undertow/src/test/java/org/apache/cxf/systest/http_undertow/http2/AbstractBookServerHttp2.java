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

package org.apache.cxf.systest.http_undertow.http2;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.customer.book.Book;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.StreamingResponseProvider;
import org.apache.cxf.systest.http_undertow.websocket.BookStorePerRequest;
import org.apache.cxf.systest.http_undertow.websocket.BookStoreWebSocket;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.http.HttpServerEngineSupport;

abstract class AbstractBookServerHttp2 extends AbstractBusTestServerBase {
    org.apache.cxf.endpoint.Server server;

    private final String port;
    private final String scheme;
    private final String context;

    AbstractBookServerHttp2(String port, String context, String scheme) {
        this.port = port;
        this.context = context;
        this.scheme = scheme;
    }

    protected void run() {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus(context);
        bus.setProperty(HttpServerEngineSupport.ENABLE_HTTP2, true);
        setBus(bus);
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setBus(bus);
        sf.setResourceClasses(BookStoreWebSocket.class, BookStorePerRequest.class);
        sf.setProvider(new StreamingResponseProvider<Book>());
        sf.setResourceProvider(BookStoreWebSocket.class,
            new SingletonResourceProvider(new BookStoreWebSocket(), true));
        sf.setAddress(scheme + "://localhost:" + port + "/http2");
        server = sf.create();
    }

    public void tearDown() throws Exception {
        server.stop();
        server.destroy();
        server = null;
    }
}
