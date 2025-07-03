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

package org.apache.cxf.systest.servlet;

import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;


import static org.junit.Assert.fail;

public abstract class AbstractJettyServer extends AbstractBusTestServerBase {
    private org.eclipse.jetty.server.Server server;
    private final String descriptor;
    private final String resourcePath;
    private final String contextPath;
    private final int port;

    protected AbstractJettyServer(final String descriptor, final String resourcePath,
            final String contextPath, int portNumber) {
        this.descriptor = descriptor;
        this.resourcePath = resourcePath;
        this.contextPath = contextPath;
        this.port = portNumber;
    }

    protected void run() {
        server = new Server(port);

        try {
            final WebAppContext context = new WebAppContext();
            context.setContextPath(contextPath);
            context.setBaseResource(ResourceFactory.of(new ContextHandler()).
                                    newClassPathResource(resourcePath));
            context.setDescriptor(ResourceFactory.of(new ContextHandler()).
                                  newClassPathResource(descriptor).getURI().toString());

            Handler.Collection handlers = new Handler.Sequence();
            handlers.setHandlers(new Handler[] {context, new DefaultHandler()});
            server.setHandler(handlers);
            server.start();
        } catch (final Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
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
