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

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;

public final class StatsServer {
    private StatsServer() {
    }

    public static void main(final String[] args) throws Exception {
        final DeploymentInfo servletBuilder = deployment()
            .setClassLoader(StatsServer.class.getClassLoader())
            .setContextPath("/")
            .setDeploymentName("sse-demo")
            .addServlets(
                servlet("MessageServlet", CXFNonSpringJaxrsServlet.class)
                    .addInitParam("jaxrs.providers", JacksonJsonProvider.class.getName())
                    .addInitParam("jaxrs.serviceClasses", StatsRestServiceImpl.class.getName())
                    .setAsyncSupported(true)
                    .setLoadOnStartup(1)
                    .addMapping("/rest/*")
            );

        final DeploymentManager manager = defaultContainer().addDeployment(servletBuilder);
        manager.deploy();

        final PathHandler path = Handlers
           .path(Handlers.redirect("/"))
           .addPrefixPath("/", manager.start());
        
        final Undertow server = Undertow.builder()
            .addHttpListener(8686, "localhost")
            .setHandler(path)
            .build();
        
        server.start();
    }
}

