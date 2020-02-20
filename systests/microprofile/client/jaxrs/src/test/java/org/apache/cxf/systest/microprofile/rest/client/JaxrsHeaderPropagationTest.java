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
package org.apache.cxf.systest.microprofile.rest.client;

import java.util.Collections;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JaxrsHeaderPropagationTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(JaxrsHeaderPropagationTest.class);

    WebClient client;
    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(JaxrsResource.class);
            sf.setResourceProvider(JaxrsResource.class,
                new SingletonResourceProvider(new JaxrsResource()));
            sf.setAddress("http://localhost:" + PORT + "/");
            sf.setPublishedEndpointUrl("/");
            sf.create();
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        
        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
        System.out.println("Listening on port " + PORT);

        ConfigProviderResolver.setInstance(
            new MockConfigProviderResolver(Collections.singletonMap(
                "org.eclipse.microprofile.rest.client.propagateHeaders", "Header1,MultiHeader")));
    }

    @Before
    public void setUp() {
        final Response r = createWebClient("/jaxrs/check").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
    }

    @Test
    public void testHeadersArePropagated() throws Exception {
        Logger logger = 
            Logger.getLogger("org.eclipse.microprofile.rest.client.ext.DefaultClientHeadersFactoryImpl"); //NOPMD
        logger.setLevel(Level.ALL);
        ConsoleHandler h = new ConsoleHandler();
        h.setLevel(Level.ALL);
        logger.addHandler(new ConsoleHandler());
        final Response r = createWebClient("/jaxrs/propagate")
            .header("Header1", "Single")
            .header("MultiHeader", "value1", "value2", "value3")
            .get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        String propagatedHeaderContent = r.readEntity(String.class);
        System.out.println("propagatedHeaderContent: " + propagatedHeaderContent);
        assertTrue(propagatedHeaderContent.contains("Header1=Single"));
        assertTrue(propagatedHeaderContent.contains("MultiHeader=value1,value2,value3"));
    }

    @Test
    public void testInjectionOccursInClientHeadersFactory() throws Exception {
        final Response r = createWebClient("/jaxrs/inject").delete();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        String returnedHeaderContent = r.readEntity(String.class);
        System.out.println("returnedHeaderContent: " + returnedHeaderContent);
        assertTrue(returnedHeaderContent.contains("REQUEST_METHOD=DELETE"));
    }

    private static WebClient createWebClient(final String url) {
        return WebClient
            .create("http://localhost:" + PORT + url)
            .accept(MediaType.TEXT_PLAIN);
    }
}
