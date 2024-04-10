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

package org.apache.cxf.systest.jaxrs.security;

import org.apache.cxf.systest.jaxrs.AbstractSpringServer;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;



public class BookServerSecuritySpringClass extends AbstractSpringServer {
    public static final int PORT = allocatePortAsInt(BookServerSecuritySpringClass.class);

    public BookServerSecuritySpringClass() {
        super("/jaxrs_security_cglib", PORT);
    }
    
    @Override
    protected void run() {
        server = new org.eclipse.jetty.server.Server(port);

        WebAppContext webappcontext = new TestWebAppContext();
        webappcontext.setContextPath(contextPath);
        webappcontext.setBaseResource(ResourceFactory.of(webappcontext).newClassPathResource(resourcePath));
        webappcontext.setThrowUnavailableOnStartupException(true);
        server.setHandler(new Handler.Sequence(webappcontext, new DefaultHandler()));

        try {
            configureServer(server);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected void configureServer(Server server) throws Exception {
        final Handler.Collection collection = (Handler.Collection) server.getHandler();
        for (Handler handler: collection.getHandlers()) {
            if (handler instanceof WebAppContext) {
                final WebAppContext webappcontext = (WebAppContext) handler;
                //this is important however this classloader isn't used by 
                //jetty12 by default as it's wrappped in WebAppClassLoader
                //as parent. That's why we need TestWebAppContext to override
                //the behaviour
                webappcontext.setClassLoader(getClass().getClassLoader());
                
            }
        }
    }

    public static void main(String[] args) {
        try {
            BookServerSecuritySpringClass s = new BookServerSecuritySpringClass();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }

    class TestWebAppContext extends WebAppContext {
        @Override
        protected ClassLoader configureClassLoader(ClassLoader loader) {
            return loader;
        }

    }
}
