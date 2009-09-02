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

package org.apache.cxf.systest.ws.addr_disable;

import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server extends AbstractBusTestServerBase {

    protected void run() {
        Object implementor = new AddNumberImpl();
        String address = "http://localhost:9095/jaxws/add";
        
        EndpointImpl ep = new EndpointImpl(BusFactory.getThreadDefaultBus(), 
                                           implementor, 
                                           null, 
                                           getWsdl());
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
