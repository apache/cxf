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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlType;

/**
 * The Pet Store
 */
@Path("/")
public class PetStore {

    public static final String CLOSED = "The Pet Store is closed";

    public PetStore() {
    }

    @GET
    @Produces("text/plain")
    public Response getBaseStatus() throws Exception {

        return Response.ok(CLOSED).build();
    }
    
    /**
     * Return Pet Status
     * @param petId the pet id
     * @param query the query
     * @return status 
     * @throws Exception
     */
    @GET
    @Path("/petstore/pets/{petId}/")
    @Produces("text/xml")
    public Response getStatus(@PathParam("petId") String petId,
                              @QueryParam("query") String query) throws Exception {

        return Response.ok(CLOSED).build();
    }
    
    
    @GET
    @Path("/petstore/jaxb/status/")
    @Produces("text/xml")
    public PetStoreStatus getJaxbStatus() {

        return new PetStoreStatus();
    }
    
    
    @GET
    @POST
    @Path("/petstore/pets/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/xml")
    public Response updateStatus(MultivaluedMap<String, String> params) throws Exception {
        return Response.ok(params.getFirst("status")).build();
    }
    
    @XmlType(name = "status", namespace = "http://pets")
    public static class PetStoreStatus {
        private String status = PetStore.CLOSED;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
        
    }
}
