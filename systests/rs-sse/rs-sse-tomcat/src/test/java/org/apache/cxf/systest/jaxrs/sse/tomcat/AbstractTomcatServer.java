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

import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.apache.cxf.systest.jaxrs.sse.BookStore;
import org.apache.cxf.systest.jaxrs.sse.BookStoreResponseFilter;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import static org.junit.Assert.fail;

public abstract class AbstractTomcatServer extends AbstractBusTestServerBase {

    private final String resourcePath;
    private final String contextPath;
    private final int port;
    private Tomcat server;
    private Path base;

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
        server.getConnector();

        try {
            base = Files.createTempDirectory("tmp-");
            server.setBaseDir(base.toString());

            if (resourcePath == null) {
                final Context context = server.addContext("", base.toString());
                final Wrapper cxfServlet = Tomcat.addServlet(context, "cxfServlet", new CXFNonSpringJaxrsServlet());
                cxfServlet.addInitParameter("jaxrs.serviceClasses", BookStore.class.getName());
                cxfServlet.addInitParameter("jaxrs.providers", String.join(",",
                    JacksonJsonProvider.class.getName(),
                    BookStoreResponseFilter.class.getName()
                ));
                cxfServlet.setAsyncSupported(true);
                context.addServletMappingDecoded("/rest/*", "cxfServlet");
            } else {
                server.getHost().setAppBase(base.toString());
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

    public void tearDown() throws Exception {
        super.tearDown();

        if (server != null) {
            server.stop();
            server.destroy();
            server = null;
            FileUtils.removeDir(base.toFile());
        }
    }
}
