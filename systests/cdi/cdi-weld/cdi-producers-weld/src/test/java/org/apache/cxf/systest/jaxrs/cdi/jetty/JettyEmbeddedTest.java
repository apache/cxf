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
package org.apache.cxf.systest.jaxrs.cdi.jetty;

import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaxrs.cdi.AbstractBookStoreCdiTest;
import org.apache.cxf.systests.cdi.base.jetty.AbstractJettyServer;
import org.jboss.weld.environment.jetty.JettyContainer;
import org.jboss.weld.environment.servlet.Container;
import org.jboss.weld.environment.servlet.Listener;

import org.junit.BeforeClass;

import static org.junit.Assert.assertTrue;

public class JettyEmbeddedTest extends AbstractBookStoreCdiTest {
    public static class EmbeddedJettyServer extends AbstractJettyServer {
        public static final int PORT = allocatePortAsInt(EmbeddedJettyServer.class);

        public EmbeddedJettyServer() {
            super("/", PORT, new Listener());
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        System.setProperty(Container.class.getName(), JettyContainer.class.getName());
        assertTrue("server did not launch correctly", launchServer(EmbeddedJettyServer.class, true));
        createStaticBus();
    }

    @Override
    protected int getPort() {
        return EmbeddedJettyServer.PORT;
    }
}
