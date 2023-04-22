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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JAXRSUriInfoTest extends AbstractClientServerTestBase {
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
            super("/jaxrs_uriinfo", "/app", PORT);
        }
    }

    /**
     * URI          | getBaseUri          | path param
-------------+---------------------+-----------
/app/v1      | http://host/        | "v1"
/app/v1/     | http://host/        | "v1/"
/app/v1/test | http://host/app/v1/ | "test"
/app/v1/     | http://host/app/v1/ | ""
/app/v1      | http://host/app/v1/ | "app/v1"
     * @throws Exception
     */
    @Test
    public void testBasePathAndPathAndPathParam() throws Exception {
        checkUriInfo("http://localhost:" + PORT + "/app/v1", "\"\"", "/");
        checkUriInfo("http://localhost:" + PORT + "/app/v1/", "\"\"", "/");
        checkUriInfo("http://localhost:" + PORT + "/app/v1/test", "\"test\"", "test");
        checkUriInfo("http://localhost:" + PORT + "/app/v1/", "\"\"", "/");
        checkUriInfo("http://localhost:" + PORT + "/app/v1", "\"\"", "/");

        checkUriInfo("http://localhost:" + PORT + "/app/v1/bar", "\"bar\"", "bar");
        checkUriInfo("http://localhost:" + PORT + "/app/v1/bar", "\"bar\"", "bar");
        checkUriInfo("http://localhost:" + PORT + "/app/v1/bar/test", "\"bar/test\"", "bar/test");
        checkUriInfo("http://localhost:" + PORT + "/app/v1/bar", "\"bar\"", "bar");
        checkUriInfo("http://localhost:" + PORT + "/app/v1/bar", "\"bar\"", "bar");
    }

    private void checkUriInfo(String address, String path, String pathParam) {
        WebClient wc = WebClient.create(address);
        wc.accept("text/plain");
        String data = wc.get(String.class);
        assertEquals("http://localhost:" + PORT + "/app/v1/," + path + "," + pathParam, data);
    }

    @Test
    public void testBasePathAndPathAndPathParamXForwarded() throws Exception {
        checkUriInfoXForwarded("http://localhost:" + PORT + "/app/v1", "\"\"", "/");
        checkUriInfoXForwarded("http://localhost:" + PORT + "/app/v1/", "\"\"", "/");
        checkUriInfoXForwarded("http://localhost:" + PORT + "/app/v1/test", "\"test\"", "test");
        checkUriInfoXForwarded("http://localhost:" + PORT + "/app/v1/", "\"\"", "/");
        checkUriInfoXForwarded("http://localhost:" + PORT + "/app/v1", "\"\"", "/");

        checkUriInfoXForwarded("http://localhost:" + PORT + "/app/v1/bar", "\"bar\"", "bar");
        checkUriInfoXForwarded("http://localhost:" + PORT + "/app/v1/bar", "\"bar\"", "bar");
        checkUriInfoXForwarded("http://localhost:" + PORT + "/app/v1/bar/test", "\"bar/test\"", "bar/test");
        checkUriInfoXForwarded("http://localhost:" + PORT + "/app/v1/bar", "\"bar\"", "bar");
        checkUriInfoXForwarded("http://localhost:" + PORT + "/app/v1/bar", "\"bar\"", "bar");
    }

    private void checkUriInfoXForwarded(String address, String path, String pathParam) {
        WebClient wc = WebClient.create(address);
        wc.accept("text/plain");
        wc.header("USE_XFORWARDED", true);
        String data = wc.get(String.class);
        assertEquals("https://external:8090/reverse/app/v1/," + path + "," + pathParam, data);
    }

    @Ignore
    @Path("/")
    public static class Resource {

        @Context
        private UriInfo uriInfo;

        @GET
        @Path("/{path:.*}")
        @Produces("text/plain")
        public String getBasePathAndPathParam(@PathParam("path") String path) {
            return  new StringBuilder(uriInfo.getBaseUri().toString())
                .append(",\"").append(path).append('"')
                .append(',').append(uriInfo.getPath()).toString();
        }

    }
}
