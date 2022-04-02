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

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.servlet.AbstractSciTest;
import org.eclipse.jetty.util.resource.Resource;

import org.junit.BeforeClass;
import org.junit.Ignore;

import static org.junit.Assert.assertTrue;

public class JettyEmptyApplicationTest extends AbstractSciTest {
    @Ignore
    public static class EmbeddedJettyServer extends AbstractJettyServer {
        public static final int PORT = allocatePortAsInt(EmbeddedJettyServer.class);

        public EmbeddedJettyServer() {
            super("/",
                new Resource[] {
                    // Limit the classpath scanning to org.apache.demo.resources package
                    Resource.newClassPathResource("/org/apache/demo/resources"),
                    // Include JAX-RS application from org.apache.applications.empty package
                    Resource.newClassPathResource("/org/apache/demo/applications/empty"),
                    // Include Jackson @Providers into classpath scanning
                    Resource.newResource(JacksonJsonProvider.class.getProtectionDomain().getCodeSource().getLocation())
                }, PORT);
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(EmbeddedJettyServer.class, true));
        createStaticBus();
    }

    @Override
    protected int getPort() {
        return EmbeddedJettyServer.PORT;
    }

    @Override
    protected String getContextPath() {
        return "/api";
    }
}
