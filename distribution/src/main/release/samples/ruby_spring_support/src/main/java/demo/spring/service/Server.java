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
package demo.spring.service;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;

public class Server {

    protected Server() throws Exception {
        System.out.println("Starting Server");

        /**
         * Important: This code simply starts up a servlet container and adds
         * the web application in src/webapp to it. Normally you would be using
         * Jetty or Tomcat and have the webapp packaged as a WAR. This is simply
         * as a convenience so you do not need to configure your servlet
         * container to see CXF in action!
         */
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(9002);

        WebAppContext webappcontext = new WebAppContext();
        webappcontext.setContextPath("/");

        webappcontext.setWar("target/RubySpringSupport.war");

        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[] {webappcontext, new DefaultHandler()});

        server.setHandler(handlers);
        server.start();
        System.out.println("Server ready...");
        server.join();
    }

    public static void main(String args[]) throws Exception {
        new Server();
    }

}
