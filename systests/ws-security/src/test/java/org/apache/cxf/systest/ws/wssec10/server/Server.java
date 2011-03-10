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
package org.apache.cxf.systest.ws.wssec10.server;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.wssec11.WSSecurity11Common;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server extends AbstractBusTestServerBase {
    static final String PORT = allocatePort(Server.class);
    static final String SSL_PORT = allocatePort(Server.class, 1);

    private static boolean unrestrictedPoliciesInstalled;
    private static String configFileName;
    
    static {
        unrestrictedPoliciesInstalled = WSSecurity11Common.checkUnrestrictedPoliciesInstalled();
        if (unrestrictedPoliciesInstalled) {
            configFileName = "org/apache/cxf/systest/ws/wssec10/server/server.xml";
        } else {
            configFileName = "org/apache/cxf/systest/ws/wssec10/server/server_restricted.xml";
        }
    };    

    public Server() throws Exception {
        
    }
    
    protected void run()  {
        Bus busLocal = new SpringBusFactory().createBus(configFileName);
        BusFactory.setDefaultBus(busLocal);
        setBus(busLocal);
    }

    public static void main(String args[]) throws Exception {
        new Server();
        new SpringBusFactory().createBus(configFileName);
        System.out.println("Server ready...");

        Thread.sleep(60 * 60 * 1000);
        System.out.println("Server exiting");
        System.exit(0);
    }
}

