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

import java.util.Collections;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.systest.jaxrs.BookStorePerRequest;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
    
public class BookServerWebSocket extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(BookServerWebSocket.class);
     
    org.apache.cxf.endpoint.Server server;
    private Map< ? extends String, ? extends Object > properties;
    
    public BookServerWebSocket() {
        this(Collections.< String, Object >emptyMap());
    }
    
    /**
     * Allow to specified custom contextual properties to be passed to factory bean
     */
    public BookServerWebSocket(final Map< ? extends String, ? extends Object > properties) {
        this.properties = properties;
    }
    
    protected void run() {
        Bus bus = BusFactory.getDefaultBus();
        setBus(bus);
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setBus(bus);
        sf.setResourceClasses(BookStoreWebSocket.class, BookStorePerRequest.class);
        sf.setResourceProvider(BookStoreWebSocket.class,
                               new SingletonResourceProvider(new BookStoreWebSocket(), true));
        sf.setAddress("http://localhost:" + PORT + "/");
        server = sf.create();
        ((JettyHTTPDestination)server.getDestination())
            .setEnableWebSocket(Boolean.parseBoolean((String)properties.get("enableWebSocket")));
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
