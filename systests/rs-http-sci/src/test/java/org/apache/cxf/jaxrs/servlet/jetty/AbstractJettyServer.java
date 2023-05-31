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

package org.apache.cxf.jaxrs.servlet.jetty;

import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.eclipse.jetty.ee10.annotations.AnnotationConfiguration;
import org.eclipse.jetty.ee10.webapp.Configuration;
import org.eclipse.jetty.ee10.webapp.WebAppConfiguration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.ee10.webapp.WebXmlConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;

import static org.junit.Assert.fail;

public abstract class AbstractJettyServer extends AbstractBusTestServerBase {
    private org.eclipse.jetty.server.Server server;
    private final Resource[] resources;
    private final String contextPath;
    private final int port;

    protected AbstractJettyServer(final String contextPath, final Resource[] resources, int portNumber) {
        this.contextPath = contextPath;
        this.resources = resources;
        this.port = portNumber;
    }

    protected void run() {
        server = new Server(port);

        try {
            final WebAppContext context = new WebAppContext();
            context.setContextPath(contextPath);
            context.setClassLoader(Thread.currentThread().getContextClassLoader());
            context.setConfigurations(new Configuration[] {
                new WebXmlConfiguration(),
                new WebAppConfiguration(),
                new AnnotationConfiguration()
            });

            for (final Resource resource: resources) {
                context.getMetaData().addContainerResource(resource);
            }

            configureContext(context);
            server.setHandler(context);

            configureServer(server);
            server.start();
        } catch (final Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }

    protected void configureServer(final Server theserver) throws Exception {
    }

    protected void configureContext(final WebAppContext context) throws Exception {
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
