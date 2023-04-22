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

package org.apache.cxf.systest.ws.addr_feature;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.ws.addressing.WSAddressingFeature;

public class Server extends AbstractBusTestServerBase {
    public static final String PORT = TestUtil.getPortNumber(Server.class, 1);
    public static final String PORT2 = TestUtil.getPortNumber(Server.class, 2);
    EndpointImpl ep;
    protected void run()  {
        setBus(BusFactory.getDefaultBus());
        Object implementor = new AddNumberImpl();
        String address = "http://localhost:" + PORT + "/jaxws/add";
        //Endpoint.publish(address, implementor);

        ep = (EndpointImpl) Endpoint.create(implementor);
        ep.getFeatures().add(new WSAddressingFeature());
        ep.publish(address);


        ep = new EndpointImpl(BusFactory.getThreadDefaultBus(),
                                           implementor,
                                           null,
                                           getWsdl());
        ep.setServiceName(new QName("http://apache.org/cxf/systest/ws/addr_feature/", "AddNumbersService"));
        ep.setEndpointName(new QName("http://apache.org/cxf/systest/ws/addr_feature/",
                                     "AddNumbersNonAnonPort"));
        String address12 = "http://localhost:" + PORT2 + "/jaxws/soap12/add";
        ep.getFeatures().add(new WSAddressingFeature());
        ep.publish(address12);


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
    private String getWsdl() {
        try {
            java.net.URL wsdl = getClass().getResource("/wsdl_systest_soap12/add_numbers_soap12.wsdl");
            return wsdl.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
