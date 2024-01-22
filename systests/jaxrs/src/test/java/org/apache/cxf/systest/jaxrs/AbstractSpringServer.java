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

import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;

public abstract class AbstractSpringServer extends AbstractBusTestServerBase {

    protected org.eclipse.jetty.server.Server server;
    protected String resourcePath;
    protected String contextPath;
    protected int port;

    protected AbstractSpringServer(String path, int portNumber) {
        this(path, "/", portNumber);
    }

    protected AbstractSpringServer(String path, String cPath, int portNumber) {
        resourcePath = path;
        contextPath = cPath;
        port = portNumber;
    }

    protected void run() {
        server = new org.eclipse.jetty.server.Server(port);

        WebAppContext webappcontext = new WebAppContext();
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
