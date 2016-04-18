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

import java.io.File;

import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.apache.cxf.transport.sse.SseHttpTransportFactory;
import org.springframework.web.context.ContextLoaderListener;

public final class StatsServer {
    private StatsServer() {
    }
    
    public static void main(final String[] args) throws Exception {
        // Register and map the dispatcher servlet
        final File base = new File(System.getProperty("java.io.tmpdir"));
        
        final Tomcat server = new Tomcat();
        server.setPort(8686);
        server.setBaseDir(base.getAbsolutePath());
        
        final StandardContext context = (StandardContext)server.addWebapp("/", base.getAbsolutePath());
        context.setConfigFile(StatsServer.class.getResource("/META-INF/context.xml"));
        context.addApplicationListener(ContextLoaderListener.class.getName());
        context.setAddWebinfClassesResources(true);
        context.setResources(resourcesFrom(context, "target/classes"));

        final Wrapper cxfServlet = Tomcat.addServlet(context, "cxfServlet", new CXFServlet());
        cxfServlet.addInitParameter(CXFServlet.TRANSPORT_ID, SseHttpTransportFactory.TRANSPORT_ID);
        context.addServletMapping("/rest/*", "cxfServlet");

        final Context staticContext = server.addWebapp("/static", base.getAbsolutePath());
        Tomcat.addServlet(staticContext, "cxfStaticServlet", new DefaultServlet());
        staticContext.addServletMapping("/static/*", "cxfStaticServlet");
        staticContext.setResources(resourcesFrom(staticContext, "target/classes/web-ui"));
        staticContext.setParentClassLoader(Thread.currentThread().getContextClassLoader());       
        
        server.start();
        server.getServer().await();
    }

    private static WebResourceRoot resourcesFrom(final Context context, final String path) {
        final File additionResources = new File(path);
        final WebResourceRoot resources = new StandardRoot(context);
        resources.addPreResources(new DirResourceSet(resources, "/", additionResources.getAbsolutePath(), "/"));
        return resources;
    }
}

