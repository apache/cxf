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

package org.apache.cxf.systest.http_undertow.websocket;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.StreamingResponseProvider;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class BookServerWebSocket extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(BookServerWebSocket.class, 1);
    public static final String PORT_SPRING = allocatePort(BookServerWebSocket.class, 2);
    public static final String PORT_WAR = allocatePort(BookServerWebSocket.class, 3);
    public static final String PORT2 = allocatePort(BookServerWebSocket.class, 4);
    public static final String PORT2_SPRING = allocatePort(BookServerWebSocket.class, 5);
    public static final String PORT2_WAR = allocatePort(BookServerWebSocket.class, 6);

    org.apache.cxf.endpoint.Server server;

    private String port;

    public BookServerWebSocket() {
        this(PORT);
    }

    public BookServerWebSocket(String port) {
        this.port = port;
    }

    protected void run() {
        Bus bus = BusFactory.getDefaultBus();
        setBus(bus);
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setBus(bus);
        sf.setResourceClasses(BookStoreWebSocket.class, BookStorePerRequest.class);
        sf.setProvider(new StreamingResponseProvider<Book>());
        sf.setResourceProvider(BookStoreWebSocket.class,
                               new SingletonResourceProvider(new BookStoreWebSocket(), true));
        sf.setAddress("ws://localhost:" + port + "/websocket");
        server = sf.create();

        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
    }

    public void tearDown() throws Exception {
        server.stop();
        server.destroy();
        server = null;
    }

    public static void main(String[] args) {
        try {
            BookServerWebSocket s = new BookServerWebSocket();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }

}
