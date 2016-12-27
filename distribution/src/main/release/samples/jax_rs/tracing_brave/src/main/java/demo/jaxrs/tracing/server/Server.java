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

package demo.jaxrs.tracing.server;

import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Server {
    protected Server() throws Exception {
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(9000);

        // Register and map the dispatcher servlet
        final ServletHolder servletHolder = new ServletHolder(new CXFNonSpringJaxrsServlet());
        final ServletContextHandler context = new ServletContextHandler();      
        context.setContextPath("/");
        context.addServlet(servletHolder, "/*");
        
        servletHolder.setInitParameter("javax.ws.rs.Application", 
            CatalogApplication.class.getName());

        server.setHandler(context);
        server.start();
        server.join();
    }

    public static void main(String args[]) throws Exception {
        new Server();
        System.out.println("Server ready...");

        Thread.sleep(5 * 6000 * 1000);
        System.out.println("Server exiting");
        System.exit(0);
    }
}
