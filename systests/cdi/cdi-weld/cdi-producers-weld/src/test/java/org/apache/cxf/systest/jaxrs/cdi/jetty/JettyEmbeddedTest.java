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

import java.util.UUID;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systests.cdi.base.AbstractCdiSingleAppTest;
import org.apache.cxf.systests.cdi.base.jetty.AbstractJettyServer;
import org.jboss.weld.environment.Container;
import org.jboss.weld.environment.jetty.JettyContainer;
import org.jboss.weld.environment.servlet.Listener;

import org.junit.BeforeClass;
import org.junit.Test;

public class JettyEmbeddedTest extends AbstractCdiSingleAppTest {
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

    @Test
    public void testAddOneBookWithValidation() {
        final String id = UUID.randomUUID().toString();

        Response r = createWebClient(getBasePath() + "/books").post(
                new Form()
                        .param("id", id));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }
    
    @Test
    public void testResponseHasBeenReceivedWhenQueringAllBookAsAtomFeed() {
        Response r = createWebClient(getBasePath() + "/books/feed", "application/atom+xml").get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("application/atom+xml", r.getMediaType().toString());
    }
    
    @Test
    public void testBookHasBeenValidatedWhenPostedAsAtomFeed() {
        Response r = createWebClient(getBasePath() + "/books/feed", "application/atom+xml").post(
                new Form()
                        .param("name", "Book 1234"));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }
    
    @Test
    public void testBookHasBeenCreatedWhenPostedAsAtomFeed() {
        Response r = createWebClient(getBasePath() + "/books/feed", "application/atom+xml").post(
                new Form()
                        .param("id", "1234")
                        .param("name", "Book 1234"));

        assertEquals(Response.Status.CREATED.getStatusCode(), r.getStatus());
    }

    @Override
    protected int getPort() {
        return EmbeddedJettyServer.PORT;
    }
}
