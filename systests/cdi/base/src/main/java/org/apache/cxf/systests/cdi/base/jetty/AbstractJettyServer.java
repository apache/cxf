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
package org.apache.cxf.systests.cdi.base.jetty;

import java.util.EventListener;

import org.apache.cxf.cdi.CXFCdiServlet;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

import static org.junit.Assert.fail;

public abstract class AbstractJettyServer extends AbstractBusTestServerBase {

    private Server server;
    private final String resourcePath;
    private final String contextPath;
    private final int port;
    private final EventListener listener;

    protected AbstractJettyServer(final String contextPath, int portNumber, EventListener listener) {
        this(null, contextPath, portNumber, listener);
    }

    protected AbstractJettyServer(final String resourcePath, final String contextPath, int portNumber,
                                  EventListener listener) {
        this.resourcePath = resourcePath;
        this.contextPath = contextPath;
        this.port = portNumber;
        this.listener = listener;
    }

    protected void run() {
        System.setProperty("java.naming.factory.url", "org.eclipse.jetty.jndi");
        System.setProperty("java.naming.factory.initial", "org.eclipse.jetty.jndi.InitialContextFactory");

        server = new Server(port);

        try {
            if (resourcePath == null) {
                // Register and map the dispatcher servlet
                final ServletHolder servletHolder = new ServletHolder(new CXFCdiServlet());
                final ServletContextHandler context = new ServletContextHandler();
                context.setContextPath(contextPath);
                context.addEventListener(listener);
                context.addServlet(servletHolder, "/rest/*");
                server.setHandler(context);
            } else {
                final WebAppContext context = new WebAppContext();
                context.setContextPath(contextPath);
                context.setWar(getClass().getResource(resourcePath).toURI().getPath());
                context.setServerClasses(new String[] {
                    "org.eclipse.jetty.servlet.ServletContextHandler.Decorator"
                });

                HandlerCollection handlers = new HandlerCollection();
                handlers.setHandlers(new Handler[] {context, new DefaultHandler()});
                server.setHandler(handlers);
            }

            server.start();
        } catch (final Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }

    public void tearDown() throws Exception {
        super.tearDown();

        if (server != null) {
            server.stop();
            server.destroy();
            server = null;
        }
    }
}
