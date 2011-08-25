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
package org.apache.cxf.systest.jms;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;


public class Soap12Server extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(Soap12Server.class);
   
    protected void run()  {
        Object impleDoc = new GreeterImplSoap12();
        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf.createBus("org/apache/cxf/systest/jms/soap12Bus.xml");
        BusFactory.setDefaultBus(bus);
        EmbeddedJMSBrokerLauncher.updateWsdlExtensors(bus, "testutils/hello_world_doc_lit.wsdl");
        Endpoint.publish(null, impleDoc);
    }


    public static void main(String[] args) {
        try {
            Soap12Server s = new Soap12Server();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
