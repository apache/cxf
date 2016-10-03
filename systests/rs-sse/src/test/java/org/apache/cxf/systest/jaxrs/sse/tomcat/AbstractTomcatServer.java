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

package org.apache.cxf.systest.jaxrs.sse.tomcat;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.apache.cxf.systest.jaxrs.sse.BookStore;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.sse.SseHttpTransportFactory;

public abstract class AbstractTomcatServer extends AbstractBusTestServerBase {

    private Tomcat server;
    private final String resourcePath;
    private final String contextPath;
    private final int port;
    
    protected AbstractTomcatServer(final String contextPath, int portNumber) {
        this(null, contextPath, portNumber);
    }
    
    protected AbstractTomcatServer(final String resourcePath, final String contextPath, int portNumber) {
        this.resourcePath = resourcePath; 
        this.contextPath = contextPath;
        this.port = portNumber;
    }
    
    protected void run() {
        server = new Tomcat();
        server.setPort(port);

        try {
            final File base = createTemporaryDirectory();
            server.setBaseDir(base.getAbsolutePath());

            if (resourcePath == null) {
                final Context context = server.addContext("/", base.getAbsolutePath());
                final Wrapper cxfServlet = Tomcat.addServlet(context, "cxfServlet", new CXFNonSpringJaxrsServlet());
                cxfServlet.addInitParameter(CXFNonSpringJaxrsServlet.TRANSPORT_ID, 
                    SseHttpTransportFactory.TRANSPORT_ID);
                cxfServlet.addInitParameter("jaxrs.serviceClasses", BookStore.class.getName());
                cxfServlet.addInitParameter("jaxrs.providers", JacksonJsonProvider.class.getName());
                cxfServlet.setAsyncSupported(true);
                context.addServletMapping("/rest/*", "cxfServlet");
            } else {
                server.getHost().setAppBase(base.getAbsolutePath());
                server.getHost().setAutoDeploy(true);
                server.getHost().setDeployOnStartup(true);
                server.addWebapp(contextPath, getClass().getResource(resourcePath).toURI().getPath().toString());   
            }
            
            server.start();
        } catch (final Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }
    
    protected void configureServer(org.eclipse.jetty.server.Server theserver) throws Exception {
        
    }
    
    private static File createTemporaryDirectory() throws IOException {
        final File base = File.createTempFile("tmp-", "");

        if (!base.delete()) {
            throw new IOException("Cannot (re)create base folder: " + base.getAbsolutePath());
        }

        if (!base.mkdir()) {
            throw new IOException("Cannot create base folder: " + base.getAbsolutePath());           
        }

        base.deleteOnExit();
        return base;
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
