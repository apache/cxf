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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Endpoint;

import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.ws.addressing.soap.DecoupledFaultHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class FaultToEndpointServer extends AbstractBusTestServerBase {
    static final String FAULT_PORT = allocatePort(FaultToEndpointServer.class);
    static final String PORT = allocatePort(FaultToEndpointServer.class, 1);
    EndpointImpl ep;
    private org.eclipse.jetty.server.Server faultToserver;
    protected void run()  { 
        faultToserver = new org.eclipse.jetty.server.Server(Integer.parseInt(FAULT_PORT));
        faultToserver.setHandler(new HelloHandler());
        try {
            faultToserver.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        setBus(BusFactory.getDefaultBus());
        Object implementor = new AddNumberImpl();
        String address = "http://localhost:" + PORT + "/jaxws/add";
        //Endpoint.publish(address, implementor);

        ep = (EndpointImpl) Endpoint.create(implementor);
        ep.getInInterceptors().add(new DecoupledFaultHandler());
        ep.getFeatures().add(new WSAddressingFeature());
        ep.publish(address);     
    }
    
    public void tearDown() throws Exception {
        if (faultToserver != null) {
            faultToserver.stop();
            faultToserver.destroy();
            faultToserver = null;
        }
        
        ep.stop();
        ep = null;        
    }

    public static void main(String[] args) {
        try {
            FaultToEndpointServer server = new FaultToEndpointServer();
            server.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
    
    class HelloHandler extends AbstractHandler {
        public void handle(String target, Request baseRequest, HttpServletRequest request,
                           HttpServletResponse response) throws IOException, ServletException {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            response.getWriter().println("Received");
        }
    }
}   


