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

package org.apache.cxf.systest.jaxrs;

import java.net.URISyntaxException;

import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;

public abstract class AbstractSpringServer extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(AbstractSpringServer.class);

    private org.eclipse.jetty.server.Server server;
    private String resourcePath;
    private String contextPath;
    private int port;
    
    protected AbstractSpringServer(String path) {
        this(path, "/", Integer.parseInt(PORT));
    }
    
    protected AbstractSpringServer(String path, int portNumber) {
        this(path, "/", portNumber);
    }
    protected AbstractSpringServer(String path, String cPath) {
        this(path, cPath, Integer.parseInt(PORT));
    }
    
    protected AbstractSpringServer(String path, String cPath, int portNumber) {
        resourcePath = path;
        contextPath = cPath;
        port = portNumber;
    }
    
    protected void run() {
        System.out.println("Starting Server");

        server = new org.eclipse.jetty.server.Server();

        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(port);
        server.setConnectors(new Connector[] {connector});

        WebAppContext webappcontext = new WebAppContext();
        webappcontext.setContextPath(contextPath);

        String warPath = null;
        try {
            warPath = getClass().getResource(resourcePath).toURI().getPath();
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        
        webappcontext.setWar(warPath);

        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[] {webappcontext, new DefaultHandler()});

        server.setHandler(handlers);
        try {
            server.start();
                       
        } catch (Exception e) {
            e.printStackTrace();
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
