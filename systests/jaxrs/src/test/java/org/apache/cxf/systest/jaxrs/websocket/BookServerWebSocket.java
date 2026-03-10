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

package org.apache.cxf.systest.jaxrs.websocket;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.StreamingResponseProvider;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.systest.jaxrs.BookStorePerRequest;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;

public class BookServerWebSocket extends AbstractServerTestServerBase {
    public static final String PORT = allocatePort(BookServerWebSocket.class, 1);
    public static final String PORT_SPRING = allocatePort(BookServerWebSocket.class, 2);
    public static final String PORT_WAR = allocatePort(BookServerWebSocket.class, 3);
    public static final String PORT2 = allocatePort(BookServerWebSocket.class, 4);
    public static final String PORT2_SPRING = allocatePort(BookServerWebSocket.class, 5);
    public static final String PORT2_WAR = allocatePort(BookServerWebSocket.class, 6);

    private String port;

    public BookServerWebSocket() {
        this(PORT);
    }

    public BookServerWebSocket(String port) {
        this.port = port;
    }

    @Override
    protected Server createServer(Bus bus) throws Exception {
        // Make sure default JSON-P/JSON-B providers are not loaded
        bus.setProperty(ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);

        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(BookStoreWebSocket.class, BookStorePerRequest.class);
        sf.setProvider(new StreamingResponseProvider<Book>());
        sf.setResourceProvider(BookStoreWebSocket.class,
                               new SingletonResourceProvider(new BookStoreWebSocket(), true));
        sf.setAddress("ws://localhost:" + port + "/websocket");
        return sf.create();
    }

    public static void main(String[] args) throws Exception {
        new BookServerWebSocket().start();
    }

}
