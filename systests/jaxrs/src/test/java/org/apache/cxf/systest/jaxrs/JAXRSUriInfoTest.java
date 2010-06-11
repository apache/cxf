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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSUriInfoTest extends AbstractClientServerTestBase {
    public static final String PORT = SpringServer.PORT;
    @BeforeClass
    public static void beforeClass() throws Exception {
        // must be 'in-process' to communicate with inner class in single JVM
        // and to spawn class SpringServer w/o using main() method
        launchServer(SpringServer.class, true);
    }

    @Ignore
    public static class SpringServer extends AbstractSpringServer {
        public SpringServer() {
            super("/jaxrs_uriinfo", "/app");
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
    
    @Ignore
    @Path("/")
    public static class Resource {
        
        @Context
        private UriInfo uriInfo;
        
        @GET
        @Path("/{path:.*}")
        @Produces("text/plain")
        public String getBasePathAndPathParam(@PathParam("path") String path) {
            StringBuilder sb = new StringBuilder();
            sb.append(uriInfo.getBaseUri());
            sb.append(",\"" + path + "\"");
            sb.append("," + uriInfo.getPath());
            return sb.toString();
        }

    }
}
