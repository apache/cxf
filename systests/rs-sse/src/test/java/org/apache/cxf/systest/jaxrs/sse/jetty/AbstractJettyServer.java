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

package org.apache.cxf.systest.jaxrs.sse.jetty;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.apache.cxf.systest.jaxrs.sse.BookStore;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.sse.SseHttpTransportFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

public abstract class AbstractJettyServer extends AbstractBusTestServerBase {

    private org.eclipse.jetty.server.Server server;
    private final String resourcePath;
    private final String contextPath;
    private final int port;
    
    protected AbstractJettyServer(final String contextPath, int portNumber) {
        this(null, contextPath, portNumber);
    }
    
    protected AbstractJettyServer(final String resourcePath, final String contextPath, int portNumber) {
        this.resourcePath = resourcePath; 
        this.contextPath = contextPath;
        this.port = portNumber;
    }
    
    protected void run() {
        server = new Server(port);
            
        try {
            if (resourcePath == null) {
                // Register and map the dispatcher servlet
                final ServletHolder holder = new ServletHolder(new CXFNonSpringJaxrsServlet());
                holder.setInitParameter(CXFNonSpringJaxrsServlet.TRANSPORT_ID, SseHttpTransportFactory.TRANSPORT_ID);
                holder.setInitParameter("jaxrs.serviceClasses", BookStore.class.getName());
                holder.setInitParameter("jaxrs.providers", JacksonJsonProvider.class.getName());
                final ServletContextHandler context = new ServletContextHandler();
                context.setContextPath(contextPath);
                context.addServlet(holder, "/rest/*");
                server.setHandler(context);
            } else {        
                final WebAppContext context = new WebAppContext();
                context.setContextPath(contextPath);
                context.setWar(getClass().getResource(resourcePath).toURI().getPath());
        
                HandlerCollection handlers = new HandlerCollection();
                handlers.setHandlers(new Handler[] {context, new DefaultHandler()});
                server.setHandler(handlers);
            }           
        
            configureServer(server);
            server.start();        
        } catch (final Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }
    
    protected void configureServer(org.eclipse.jetty.server.Server theserver) throws Exception {
        
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
