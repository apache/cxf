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
package org.apache.cxf.jaxrs.model.wadl.petstore;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

/**
 * The Pet Store
 */
@Path("/")
public class PetStore {

    public static final String CLOSED = "The Pet Store is closed";

    /**
     * Return Pet Status with no params
     *
     * @return status
     * @throws Exception
     */
    @GET
    @Produces("text/plain")
    public Response getBaseStatus() throws Exception {
        return Response.ok(CLOSED).build();
    }

    /**
     * Return Pet Status with 2 params
     *
     * @param petId the pet id
     * @param query the query
     * @return status
     * @throws Exception
     */
    @GET
    @Path("/petstore/pets/{petId}/")
    @Produces("text/xml")
    public Response getStatus2Params(
            @PathParam("petId") String petId,
            @QueryParam("query") String query) throws Exception {

        return Response.ok(CLOSED).build();
    }

    /**
     * Return Pet Status With 1 Param
     *
     * @param petId the pet id
     * @return status
     * @throws Exception
     */
    @GET
    @Path("/petstore/pets/id/{petId}/")
    @Produces("text/xml")
    public Response getStatus1Param(@PathParam("petId") String petId) throws Exception {
        return Response.ok(CLOSED).build();
    }

    /**
     * Return Pet Status With 3 Params
     *
     * @param petId the pet id
     * @param query the query
     * @param query2 the query2
     * @return status
     * @throws Exception
     */
    @GET
    @Path("/petstore/pets/{petId}/")
    @Produces("text/xml")
    public Response getStatus3Params(
            @PathParam("petId") String petId,
            @QueryParam("query") String query,
            @QueryParam("query2") String query2) throws Exception {

        return Response.ok(CLOSED).build();
    }
}
