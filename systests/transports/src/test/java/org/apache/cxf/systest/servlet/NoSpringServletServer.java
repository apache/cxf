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
package org.apache.cxf.systest.servlet;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class NoSpringServletServer extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(NoSpringServletServer.class);

    
    Server httpServer;
    @Override
    protected void run() {
        // setup the system properties
        String busFactory = System.getProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME);
        System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, "org.apache.cxf.bus.CXFBusFactory");
        try {
            httpServer = new Server(Integer.parseInt(PORT));
            ContextHandlerCollection contexts = new ContextHandlerCollection();
            httpServer.setHandler(contexts);

            ServletContextHandler root = new ServletContextHandler(contexts, "/",
                                                                   ServletContextHandler.SESSIONS);

            CXFNonSpringServlet cxf = new CXFNonSpringServlet();
            ServletHolder servlet = new ServletHolder(cxf);
            servlet.setName("soap");
            servlet.setForcedPath("soap");
            root.addServlet(servlet, "/soap/*");

            httpServer.start();

            Bus bus = cxf.getBus();
            setBus(bus);
            BusFactory.setDefaultBus(bus);
            GreeterImpl impl = new GreeterImpl();
            Endpoint.publish("/Greeter", impl);
            HelloImpl helloImpl = new HelloImpl();
            Endpoint.publish("/Hello", helloImpl);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // clean up the system properties
            if (busFactory != null) {
                System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, busFactory);
            } else {
                System.clearProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME);
            }
        }
    }

    public void tearDown() throws Exception {
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    public static void main(String[] args) {
        try {
            NoSpringServletServer s = new NoSpringServletServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }

}
