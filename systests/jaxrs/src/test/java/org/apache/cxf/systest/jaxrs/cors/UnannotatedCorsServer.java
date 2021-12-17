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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;

/**
 * Service bean with no class-level annotation for cross-script control.
 */
public class UnannotatedCorsServer {

    @GET
    @Produces("text/plain")
    @Path("/simpleGet/{echo}")
    public String simpleGet(@PathParam("echo") String echo) {
        return echo;
    }

    @DELETE
    @Path("/delete")
    public Response deleteSomething() {
        return Response.ok().build();
    }

    @GET
    @CrossOriginResourceSharing(
        allowOrigins = {
            "http://area51.mil:31415" 
        },
        exposeHeaders = {
            "X-custom-3", 
            "X-custom-4" 
        })
    @Produces("text/plain")
    @Path("/annotatedGet/{echo}")
    public String annotatedGet(@PathParam("echo") String echo) {
        return echo;
    }

    /**
     * A method annotated to test preflight.
     * @param input
     * @return
     */
    @PUT
    @Consumes("text/plain")
    @Produces("text/plain")
    @Path("/annotatedPut")
    @CrossOriginResourceSharing(
        allowOrigins = { "http://area51.mil:31415" },
        allowCredentials = true,
        maxAge = 1,
        allowHeaders = { "X-custom-1", "X-custom-2" },
        exposeHeaders = {"X-custom-3", "X-custom-4" }
    )
    public String annotatedPut(String input) {
        return input;
    }

    @PUT
    @Consumes("text/plain")
    @Produces("text/plain")
    @Path("/annotatedPut2")
    @CrossOriginResourceSharing(
        allowAllOrigins = true,
        allowCredentials = true,
        maxAge = 1,
        allowHeaders = { "X-custom-1", "X-custom-2" },
        exposeHeaders = {"X-custom-3", "X-custom-4" }
    )
    public String annotatedPut2(String input) {
        return input;
    }
}
