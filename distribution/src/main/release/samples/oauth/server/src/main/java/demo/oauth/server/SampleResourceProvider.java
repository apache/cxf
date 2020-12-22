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
package demo.oauth.server;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import org.springframework.security.access.annotation.Secured;

/**
 * Sample JAX-RS resource service
 */
@Path("/")
public class SampleResourceProvider {

    @GET
    @Produces("text/html")
    @Path("/person/get/{name}")
    @Secured({"ROLE_USER" })
    public Response getInfo(@PathParam("name") String name, @Context HttpServletRequest request) {
        return Response.ok("Successfully accessed OAuth protected person: " + name).build();
    }

    @POST
    @Produces("text/html")
    @Path("/person/modify/{name}")
    @Secured({"ROLE_ADMIN" })
    public Response modifyInfo(@PathParam("name") String name, @Context HttpServletRequest request) {
        return Response.ok("Successfully modified OAuth protected person: " + name).build();
    }
}
