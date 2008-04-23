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

package org.apache.cxf.systest.jaxws;

import javax.xml.ws.Endpoint;

import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.headers.HeaderTesterImpl;
import org.apache.hello_world_xml_http.bare.GreeterImpl;
import org.apache.hello_world_xml_http.wrapped.GreeterFaultImpl;


public class ServerXMLBinding extends AbstractBusTestServerBase {

    protected void run() {
        Object implementor = new GreeterImpl();
        String address = "http://localhost:9031/XMLService/XMLPort";
        Endpoint.publish(address, implementor);

        Object implementor1 = new org.apache.hello_world_xml_http.wrapped.GreeterImpl();
        address = "http://localhost:9032/XMLService/XMLPort";
        Endpoint.publish(address, implementor1);

        Object faultImplementor = new GreeterFaultImpl();
        String faultAddress = "http://localhost:9033/XMLService/XMLFaultPort";
        Endpoint.publish(faultAddress, faultImplementor);

        Object implementor2 = new HeaderTesterImpl();
        address = "http://localhost:9034/XMLContext/XMLPort";
        Endpoint.publish(address, implementor2);
        
        Object implementor3 = new org.apache.hello_world_xml_http.mixed.GreeterImpl();
        address = "http://localhost:9028/XMLService/XMLPort";
        Endpoint.publish(address, implementor3);
    }

    public static void main(String[] args) {
        try {
            ServerXMLBinding s = new ServerXMLBinding();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
