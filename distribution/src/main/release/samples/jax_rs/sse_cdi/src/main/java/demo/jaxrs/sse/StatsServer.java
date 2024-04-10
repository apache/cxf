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
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
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
        staticContext.setBaseResourceAsString(StatsServer.class.getResource("/web-ui").toURI().toString());

         // Register and map the dispatcher servlet
        final CXFCdiServlet cxfServlet = new CXFCdiServlet();
        final ServletHolder cxfServletHolder = new ServletHolder(cxfServlet);
        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addEventListener(new Listener());
        context.addEventListener(new BeanManagerResourceBindingListener());
        context.addServlet(cxfServletHolder, "/rest/*");

        Handler.Collection handlers = new Handler.Sequence(staticContext, context);
        server.setHandler(handlers);
        server.start();
        server.join();
    }
}

