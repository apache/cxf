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

import java.util.Iterator;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JAXRSUriInfoMatchTest extends AbstractClientServerTestBase {
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
            super("/jaxrs_uriinfo_match", "/match", PORT);
        }
    }


    @Test
    public void testMatchedUris() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/match/my/resource/1/matched/uris");
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(100000000L);
        wc.accept("text/plain");
        String data = wc.get(String.class);
        assertEquals("my/resource/1/matched/uris,my/resource/1", data);
    }
    @Test
    public void testMatchedUrisParam() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT
                                        + "/match/my/resource/1/matched/uris/param");
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(100000000L);
        wc.accept("text/plain");
        String data = wc.get(String.class);
        assertEquals("my/resource/1/matched/uris/param,my/resource/1", data);
    }
    @Test
    public void testMatchedUrisParam2() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT
                                        + "/match/my/resource/1/matched/uris/param/2");
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(100000000L);
        wc.accept("text/plain");
        String data = wc.get(String.class);
        assertEquals("my/resource/1/matched/uris/param/2,my/resource/1", data);
    }
    @Test
    public void testMatchedResources() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/match/my/resource/1/matched/resources");
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(100000000L);
        wc.accept("text/plain");
        String data = wc.get(String.class);
        assertEquals("class org.apache.cxf.systest.jaxrs.JAXRSUriInfoMatchTest$Resource", data);
    }

    @Ignore
    @Path("my/resource/{param}")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Resource {

        @Context
        private UriInfo uriInfo;

        @GET
        @Path("matched/uris")
        public Object getMatchedUris() {
            return concat(uriInfo.getMatchedURIs());
        }
        @GET
        @Path("matched/uris/param")
        public Object getMatchedUrisParam(@PathParam("param") String param) {
            return concat(uriInfo.getMatchedURIs());
        }
        @GET
        @Path("matched/uris/param/{param2}")
        public Object getMatchedUrisParam2() {
            return concat(uriInfo.getMatchedURIs());
        }
        @GET
        @Path("matched/resources")
        public Object getMatchedResources() {
            return concat(uriInfo.getMatchedResources());
        }

    }

    private static String concat(List<?> data) {
        StringBuilder builder = new StringBuilder();
        Iterator<?> iterator = data.iterator();
        while (iterator.hasNext()) {
            builder.append(iterator.next().toString());
            if (iterator.hasNext()) {
                builder.append(',');
            }
        }
        return builder.toString();
    }


}
