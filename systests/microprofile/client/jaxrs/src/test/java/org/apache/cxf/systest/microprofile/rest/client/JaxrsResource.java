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

import java.net.URI;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

@Path("/jaxrs")
@Produces("text/plain")
public class JaxrsResource {

    @Path("/check")
    @GET
    public Response statusCheck() {
        return Response.ok("up").build();
    }

    @Path("/propagate")
    @GET
    public String propagateHeadersToRestClient() {
        RestClient client = RestClientBuilder.newBuilder()
                                             .baseUri(URI.create("http://localhost:8080/ignored"))
                                             .register(ReturnAllOutboundHeadersFilter.class)
                                             .build(RestClient.class);
        return client.getAllHeadersToBeSent();
    }

    @Path("/inject")
    @DELETE
    public String injectContextsIntoClientHeadersFactory() {
        InjectRestClient client = RestClientBuilder.newBuilder()
                                                   .baseUri(URI.create("http://localhost:8080/ignored"))
                                                   .register(ReturnAllOutboundHeadersFilter.class)
                                                   .build(InjectRestClient.class);
        return client.getAllHeadersToBeSent();
    }
}
