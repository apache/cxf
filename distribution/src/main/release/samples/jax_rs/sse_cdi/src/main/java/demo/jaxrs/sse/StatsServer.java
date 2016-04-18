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
package demo.jaxrs.sse;

import org.apache.cxf.cdi.CXFCdiServlet;
import org.apache.cxf.transport.sse.SseHttpTransportFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.weld.environment.servlet.BeanManagerResourceBindingListener;
import org.jboss.weld.environment.servlet.Listener;

public final class StatsServer {
    private StatsServer() {
    }
    
    public static void main(final String[] args) throws Exception {
        final Server server = new Server(8686);

        final ServletHolder staticHolder = new ServletHolder(new DefaultServlet());
        final ServletContextHandler staticContext = new ServletContextHandler();
        staticContext.setContextPath("/static");
        staticContext.addServlet(staticHolder, "/*");
        staticContext.setResourceBase(StatsServer.class.getResource("/web-ui").toURI().toString());

         // Register and map the dispatcher servlet
        final CXFCdiServlet cxfServlet = new CXFCdiServlet();
        final ServletHolder cxfServletHolder = new ServletHolder(cxfServlet);
        cxfServletHolder.setInitParameter(CXFCdiServlet.TRANSPORT_ID, SseHttpTransportFactory.TRANSPORT_ID);
        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addEventListener(new Listener());
        context.addEventListener(new BeanManagerResourceBindingListener());
        context.addServlet(cxfServletHolder, "/rest/*");

        HandlerList handlers = new HandlerList();
        handlers.addHandler(staticContext);
        handlers.addHandler(context);
        
        server.setHandler(handlers);
        server.start();
        server.join();
    }
}

