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

package org.apache.cxf.systest.ws.addr_fromwsdl;


import javax.jws.WebService;

import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersPortType;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.addressing.WSAddressingFeature;

public class Server extends AbstractBusTestServerBase {
    @WebService(serviceName = "AddNumbersService",
                portName = "AddNumbersPort",
                targetNamespace = "http://apache.org/cxf/systest/ws/addr_feature/")
    public static class AddNumberReg extends AddNumberImpl implements AddNumbersPortType {
        
    }
    @WebService(serviceName = "AddNumbersService",
                portName = "AddNumbersNonAnonPort",
                targetNamespace = "http://apache.org/cxf/systest/ws/addr_feature/")
    public static class AddNumberNonAnon extends AddNumberImpl implements AddNumbersPortType {
        
    }
    @WebService(serviceName = "AddNumbersService",
                portName = "AddNumbersOnlyAnonPort",
                targetNamespace = "http://apache.org/cxf/systest/ws/addr_feature/")
    public static class AddNumberOnlyAnon extends AddNumberImpl implements AddNumbersPortType {
        
    }
    protected void run()  {    
        Object implementor = new AddNumberReg();
        String address = "http://localhost:9091/jaxws/add";
        EndpointImpl ep;
        ep = new EndpointImpl(BusFactory.getThreadDefaultBus(), 
                                           implementor, 
                                           null, 
                                           getWsdl());

        ep.getFeatures().add(new WSAddressingFeature());
        ep.publish(address);

        implementor = new AddNumberNonAnon();
        address = "http://localhost:9091/jaxws/addNonAnon";
        
        ep = new EndpointImpl(BusFactory.getThreadDefaultBus(), 
                                           implementor, 
                                           null, 
                                           getWsdl());

        ep.getFeatures().add(new WSAddressingFeature());
        ep.publish(address);

        implementor = new AddNumberOnlyAnon();
        address = "http://localhost:9091/jaxws/addAnon";
        
        ep = new EndpointImpl(BusFactory.getThreadDefaultBus(), 
                                           implementor, 
                                           null, 
                                           getWsdl());

        ep.getFeatures().add(new WSAddressingFeature());
        ep.publish(address);
    }

    private String getWsdl() {
        try {
            java.net.URL wsdl = getClass().getResource("/wsdl_systest_wsspec/add_numbers.wsdl");
            return wsdl.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
