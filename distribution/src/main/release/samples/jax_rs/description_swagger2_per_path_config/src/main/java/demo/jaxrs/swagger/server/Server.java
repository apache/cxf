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

package demo.jaxrs.swagger.server;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.provider.MultipartProvider;
import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Server {

    protected Server() throws Exception {
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(9000);

        // Configuring all static web resource
        final ServletHolder staticHolder = new ServletHolder(new DefaultServlet());
        // Register and map the dispatcher servlet
        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(staticHolder, "/*");
        context.setResourceBase(
            getClass().getResource("/META-INF/resources/webjars/swagger-ui/2.1.0").toURI().toString());
        
        final ServletHolder servletHolderV1 = new ServletHolder(new CXFNonSpringJaxrsServlet());
        context.addServlet(servletHolderV1, "/v1/*");
        servletHolderV1.setInitParameter("redirects-list", 
            "/ /index.html /.*[.]js /css/.* /images/.* lib/.* .*ico /fonts/.*");
        servletHolderV1.setInitParameter("redirect-servlet-name", staticHolder.getName());
        servletHolderV1.setInitParameter("redirect-attributes", "javax.servlet.include.request_uri");
        servletHolderV1.setInitParameter("jaxrs.serviceClasses", demo.jaxrs.swagger.v1.Sample.class.getName());
        servletHolderV1.setInitParameter("jaxrs.features", 
            Swagger2Feature.class.getName() + "(basePath=/v1 usePathBasedConfig=true)");
        servletHolderV1.setInitParameter("jaxrs.providers", StringUtils.join(
            new String[] {
                MultipartProvider.class.getName(),
                JacksonJsonProvider.class.getName(),
                ApiOriginFilter.class.getName()
            }, ",") 
        );
        
        final ServletHolder servletHolderV2 = new ServletHolder(new CXFNonSpringJaxrsServlet());
        context.addServlet(servletHolderV2, "/v2/*");  
        servletHolderV2.setInitParameter("redirects-list", 
            "/ /index.html /.*[.]js /css/.* /images/.* lib/.* .*ico /fonts/.*");
        servletHolderV2.setInitParameter("redirect-servlet-name", staticHolder.getName());
        servletHolderV2.setInitParameter("redirect-attributes", "javax.servlet.include.request_uri");
        servletHolderV2.setInitParameter("jaxrs.serviceClasses", demo.jaxrs.swagger.v2.Sample.class.getName());
        servletHolderV2.setInitParameter("jaxrs.features", 
            Swagger2Feature.class.getName() + "(basePath=/v2 usePathBasedConfig=true)");
        servletHolderV2.setInitParameter("jaxrs.providers", StringUtils.join(
            new String[] {
                MultipartProvider.class.getName(),
                JacksonJsonProvider.class.getName(),
                ApiOriginFilter.class.getName()
            }, ",") 
        );

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
