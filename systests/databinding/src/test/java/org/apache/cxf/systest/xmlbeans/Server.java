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

package org.apache.cxf.systest.xmlbeans;

import javax.xml.ws.Endpoint;


import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server extends AbstractBusTestServerBase {

    protected void run() {
        // set the configuration file
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/systest/xmlbeans/cxf.xml");
        BusFactory.setDefaultBus(bus);
        setBus(bus);
        Endpoint.publish("http://localhost:9000/SoapContext/SoapPort",
                         new GreeterImpl());
        
        Endpoint.publish("http://localhost:9000/SOAPDocLitBareService/SoapPort", 
                         new PutLastTradePriceImpl());
    }

    public static void main(String args[]) {
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
