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

package org.apache.cxf.systest.jaxrs.sse.undertow;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.apache.cxf.systest.jaxrs.sse.BookStore;
import org.apache.cxf.systest.jaxrs.sse.BookStoreResponseFilter;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;
import static org.junit.Assert.fail;

public abstract class AbstractUndertowServer extends AbstractBusTestServerBase {
    private Undertow server;
    private final String contextPath;
    private final int port;

    protected AbstractUndertowServer(final String contextPath, int portNumber) {
        this.contextPath = contextPath;
        this.port = portNumber;
    }

    protected void run() {
        try {
            final DeploymentInfo servletBuilder = deployment()
                .setClassLoader(AbstractUndertowServer.class.getClassLoader())
                .setContextPath(contextPath)
                .setDeploymentName("sse-test")
                .addServlets(
                    servlet("MessageServlet", CXFNonSpringJaxrsServlet.class)
                        .addInitParam("jaxrs.providers", String.join(",",
                            JacksonJsonProvider.class.getName(),
                            BookStoreResponseFilter.class.getName()))
                        .addInitParam("jaxrs.serviceClasses", BookStore.class.getName())
                        .setAsyncSupported(true)
                        .setLoadOnStartup(1)
                        .addMapping("/rest/*")
                 );

            final DeploymentManager manager = defaultContainer().addDeployment(servletBuilder);
            manager.deploy();

            PathHandler path = Handlers
                .path(Handlers.redirect("/"))
                .addPrefixPath("/", manager.start());

            server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(path)
                .build();

            server.start();
        } catch (final Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }

    protected void configureServer(org.eclipse.jetty.server.Server theserver) throws Exception {

    }

    public void tearDown() throws Exception {
        super.tearDown();

        if (server != null) {
            server.stop();
            server = null;
        }
    }
}
