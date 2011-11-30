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

package org.apache.cxf.systest.jaxrs.cors;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.cors.CorsHeaderConstants;
import org.apache.cxf.systest.jaxrs.AbstractSpringServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class BasicCrossOriginTest extends AbstractBusClientServerTestBase {
    public static final String PORT = SpringServer.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(SpringServer.class, true));
    }
    
    @org.junit.Ignore
    @SuppressWarnings("unchecked")
    @Test
    public void testSimpleGet() throws Exception {
        String origin = "http://localhost:" + PORT;
        WebClient wc = WebClient.create(origin + "/");
        WebClient.getConfig(wc).getOutInterceptors().add(new LoggingOutInterceptor());
        // Since our WebClient doesn't know from Origin, we need to do this ourselves.
        wc.header("Origin", origin);
        Response r = wc.replacePath("/simpleGet/HelloThere").accept("text/plain").get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        String echo = IOUtils.toString((InputStream)r.getEntity());
        assertEquals("HelloThere", echo);
        MultivaluedMap<String, Object> m = r.getMetadata();
        Object acAllowed = m.get(CorsHeaderConstants.HEADER_AC_ALLOW_ORIGIN);
        assertNotNull(acAllowed);
        List<String> origins = (List<String>)acAllowed;
        assertEquals(1, origins.size());
        assertEquals(origin, origins.get(0));
    }
    
    @Ignore
    public static class SpringServer extends AbstractSpringServer {
        public static final String PORT = AbstractSpringServer.PORT;
        
        public SpringServer() {
            super("/jaxrs_cors");
        }
    }
}
