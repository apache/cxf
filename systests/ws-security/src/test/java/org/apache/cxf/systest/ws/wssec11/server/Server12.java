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
package org.apache.cxf.systest.ws.wssec11.server;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;

public class Server12 extends AbstractServer {
    public static final String PORT = allocatePort(Server12.class);

    public Server12() throws Exception {
        super("http://localhost:" + PORT);
    }
    
    public Server12(String baseUrl) throws Exception {
        super(baseUrl);
    }
    
    protected void run()  {
        Bus busLocal = new SpringBusFactory().createBus(
            "org/apache/cxf/systest/ws/wssec11/server/server.xml");
        BusFactory.setDefaultBus(busLocal);
        setBus(busLocal);

        try {
            new Server12("http://localhost:" + PORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String args[]) throws Exception {
        new SpringBusFactory().createBus("org/apache/cxf/systest/ws/wssec11/server/server.xml");
        new Server12("http://localhost:" + PORT);
        System.out.println("Server ready...");

        Thread.sleep(60 * 60 * 10000);
        System.out.println("Server exiting");
        System.exit(0);
    }
    
}
