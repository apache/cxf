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

package org.apache.cxf.systest.http_undertow;


import java.net.URL;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;


public class UndertowBasicAuthServer extends AbstractBusTestServerBase  {
    static final String PORT = allocatePort(UndertowBasicAuthServer.class);
    static final String ADDRESS = "http://localhost:" + PORT + "/SoapContext/SoapPort";

    Endpoint ep;
    
    protected void run()  {
        String configurationFile = "undertowBasicAuthServer.xml";
        URL configure =
            UndertowBasicAuthServer.class.getResource(configurationFile);
        Bus bus = new SpringBusFactory().createBus(configure, true);
        bus.getInInterceptors().add(new LoggingInInterceptor());
        bus.getOutInterceptors().add(new LoggingOutInterceptor());
        SpringBusFactory.setDefaultBus(bus);
        setBus(bus);

        GreeterImpl implementor = new GreeterImpl();
        ep = Endpoint.publish(ADDRESS, implementor);
    }
    
    public void tearDown() throws Exception {
        if (ep != null) {
            ep.stop();
            ep = null;
        }
    }
    
    public static void main(String[] args) {
        try { 
            UndertowBasicAuthServer s = new UndertowBasicAuthServer(); 
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }
}
