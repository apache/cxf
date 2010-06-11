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

package org.apache.cxf.systest.versioning;

import javax.xml.ws.Endpoint;

import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.MultipleEndpointObserver;
import org.apache.hello_world_mixedstyle.GreeterImplMixedStyle;


public class Server extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(Server.class);
    
    protected void run() {
        String address = "http://localhost:" + PORT + "/SoapContext/SoapPort";

        Object implementor1 = new GreeterImplMixedStyle(" version1");
        EndpointImpl ep1 = (EndpointImpl) Endpoint.publish(address, implementor1);

        ep1.getServer().getEndpoint().put("version", "1");
        ep1.getServer().getEndpoint().put("allow-multiplex-endpoint", Boolean.TRUE);

        //Register a MediatorInInterceptor on this dummy service

        Object implementor2 = new GreeterImplMixedStyle(" version2");
        EndpointImpl ep2 = (EndpointImpl) Endpoint.publish(address, implementor2);
        ep2.getServer().getEndpoint().put("version", "2");
        
        MultipleEndpointObserver meo = (MultipleEndpointObserver)
            ep1.getServer().getDestination().getMessageObserver();
        meo.getRoutingInterceptors().clear();
        meo.getRoutingInterceptors().add(new MediatorInInterceptor());
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
