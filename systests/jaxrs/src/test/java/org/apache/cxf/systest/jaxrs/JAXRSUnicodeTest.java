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
package org.apache.cxf.systest.jaxrs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JAXRSUnicodeTest extends AbstractBusClientServerTestBase {
    public static final int PORT = SpringServer.PORT;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // must be 'in-process' to communicate with inner class in single JVM
        // and to spawn class SpringServer w/o using main() method
        launchServer(SpringServer.class, true);
    }
    @Ignore
    public static class SpringServer extends AbstractSpringServer {
        public static final int PORT = allocatePortAsInt(SpringServer.class);
        public SpringServer() {
            super("/jaxrs_unicode", PORT);
        }
    }


    @Test
    public void testGetHelloMessage() {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/кирилица");
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        wc.accept("text/plain");
        assertEquals("Hello", wc.get(String.class));
    }

    @Ignore
    @Path("/")
    public static class Resource {

        @GET
        @Produces("text/plain")
        public String getHello() {
            return "Hello";
        }

    }

}

