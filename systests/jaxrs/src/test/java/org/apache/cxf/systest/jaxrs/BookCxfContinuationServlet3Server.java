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

package org.apache.cxf.systest.jaxrs;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;

public class BookCxfContinuationServlet3Server extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(BookCxfContinuationServlet3Server.class);

    protected void run() {
        String busFactory = System.getProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME);
        System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, "org.apache.cxf.bus.CXFBusFactory");
        try {
            CXFNonSpringServlet cxf = new CXFNonSpringServlet();
            httpServer(cxf).start();
            serverFactory(cxf.getBus()).create();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (busFactory != null) {
                System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, busFactory);
            } else {
                System.clearProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME);
            }
        }
    }

    private Server httpServer(CXFNonSpringServlet cxf) {
        Server server = new Server(Integer.parseInt(PORT));
        ServletContextHandler servletContextHandler = new ServletContextHandler();
        
        ServletHandler handler = new ServletHandler();
        servletContextHandler.setHandler(handler);
        server.setHandler(servletContextHandler);
        handler.addServletWithMapping(new ServletHolder(cxf), "/*");
        return server;
    }

    private JAXRSServerFactoryBean serverFactory(Bus bus) {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setBus(bus);
        sf.setResourceClasses(BookCxfContinuationStore.class);
        sf.setResourceProvider(BookCxfContinuationStore.class,
                               new SingletonResourceProvider(new BookCxfContinuationStore()));
        sf.setAddress("/");
        return sf;
    }

    public static void main(String[] args) {
        try {
            BookCxfContinuationServlet3Server s = new BookCxfContinuationServlet3Server();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
