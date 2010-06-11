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

package org.apache.cxf.systest.http;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

/**
 * This server just instantiates a Bus, full stop.
 * Everything else is designed to be spring-loaded.
 */
public class BusServer extends AbstractBusTestServerBase {
    public static final String PORT0 = allocatePort(BusServer.class, 0);
    public static final String PORT1 = allocatePort(BusServer.class, 1);
    public static final String PORT2 = allocatePort(BusServer.class, 2);
    public static final String PORT3 = allocatePort(BusServer.class, 3);
    public static final String PORT4 = allocatePort(BusServer.class, 4);
    public static final String PORT5 = allocatePort(BusServer.class, 5);
    public static final String PORT6 = allocatePort(BusServer.class, 6);
    public static final String PORT7 = allocatePort(BusServer.class, 7);
    public static final String PORT8 = allocatePort(BusServer.class, 8);
    
    protected void run()  {
        //
        // Just instantiate the Bus; services will be instantiated
        // and published automatically through Spring
        //
        final BusFactory factory = BusFactory.newInstance();
        Bus bus = factory.createBus();
        setBus(bus);
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);
    }

    public static void main(String[] args) {
        try {
            BusServer s = new BusServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
