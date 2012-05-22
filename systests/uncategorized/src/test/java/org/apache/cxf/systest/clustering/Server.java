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

package org.apache.cxf.systest.clustering;


import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(Server.class, 0);

    private static final String ADDRESS =
        "http://localhost:" + PORT + "/SoapContext/ControlPort";
    private static final String TARGETS_CONFIG =
        "org/apache/cxf/systest/clustering/targets.xml";

    Endpoint ep;
    
    protected void run()  {

        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus(TARGETS_CONFIG);
        BusFactory.setDefaultBus(bus);
        setBus(bus);

        ControlImpl implementor = new ControlImpl();
        ep = Endpoint.publish(ADDRESS, implementor);
    }
    public void tearDown() {
        ep.stop();
        ep = null;
    }

    public static void main(String[] args) {
        try { 
            Server s = new Server(); 
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally { 
            System.out.println("done!");
        }
    }
}
