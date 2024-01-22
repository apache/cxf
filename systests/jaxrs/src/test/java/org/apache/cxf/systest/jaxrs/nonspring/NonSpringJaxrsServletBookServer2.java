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

package org.apache.cxf.systest.jaxrs.nonspring;

import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.apache.cxf.systest.jaxrs.BookApplicationNonSpring;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

public class NonSpringJaxrsServletBookServer2 extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(NonSpringJaxrsServletBookServer2.class);
    private org.eclipse.jetty.server.Server server;

    protected void run() {
        server = new org.eclipse.jetty.server.Server(Integer.parseInt(PORT));

        final ServletHolder servletHolder =
            new ServletHolder(new CXFNonSpringJaxrsServlet(new BookApplicationNonSpring()));
        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(servletHolder, "/*");
        //servletHolder.setInitParameter("jaxrs.serviceClasses", BookStore.class.getName());

        server.setHandler(context);
        try {
            server.start();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
            server.destroy();
            server = null;
        }
    }

    public static void main(String[] args) {
        try {
            NonSpringJaxrsServletBookServer2 s = new NonSpringJaxrsServletBookServer2();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
