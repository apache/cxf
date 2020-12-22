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

package demo.jaxrs.openapi.server;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;


@Path("/sample")
public class Sample {
    private Map<String, Item> items;

    public Sample() {
        items = Collections.synchronizedMap(new TreeMap<String, Item>(String.CASE_INSENSITIVE_ORDER));
        items.put("Item 1", new Item("Item 1", "Value 1"));
        items.put("Item 2", new Item("Item 2", "Value 2"));
    }

    @Produces({ MediaType.APPLICATION_JSON })
    @GET
    @Operation(
        summary = "Get all items",
        description = "Get operation with Response and @Default value"
    )
    @APIResponses(
        @APIResponse(
            content = @Content(schema = @Schema(implementation = Item.class, type = SchemaType.ARRAY)),
            responseCode = "200"
        )
    )
    public Response getItems(@Parameter(required = true) @QueryParam("page") @DefaultValue("1") int page) {
        return Response.ok(items.values()).build();
    }

    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/{name}")
    @GET
    @Operation(
        summary = "Get item by name",
        description = "Get operation with type and headers"
    )
    @APIResponses({
        @APIResponse(content = @Content(schema = @Schema(implementation = Item.class)), responseCode = "200"),
        @APIResponse(responseCode = "404")
    })
    public Response getItem(
            @Parameter(required = true) @HeaderParam("Accept-Language") final String language,
            @Parameter(required = true) @PathParam("name") String name) {
        return items.containsKey(name) 
            ? Response.ok().entity(items.get(name)).build() 
                : Response.status(Status.NOT_FOUND).build();
    }

    @Consumes({ MediaType.APPLICATION_JSON })
    @POST
    @Operation(
        summary = "Create new item",
        description = "Post operation with entity in a body"
    )
    @APIResponses(
        @APIResponse(
            content = @Content(
                schema = @Schema(implementation = Item.class), 
                mediaType = MediaType.APPLICATION_JSON
            ),
            headers = @Header(name = "Location"),
            responseCode = "201"
        )
    )
    public Response createItem(
        @Context final UriInfo uriInfo,
        @Parameter(required = true) final Item item) {
        items.put(item.getName(), item);
        return Response
            .created(uriInfo.getBaseUriBuilder().path(item.getName()).build())
            .entity(item).build();
    }

    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/{name}")
    @PUT
    @Operation(
        summary = "Update an existing new item",
        description = "Put operation with form parameter"
    )
    @APIResponse(
        content = @Content(schema = @Schema(implementation = Item.class)),
        responseCode = "200"
    )
    public Item updateItem(
            @Parameter(required = true) @PathParam("name") String name,
            @Parameter(required = true) @FormParam("value") String value) {
        Item item = new Item(name, value);
        items.put(name,  item);
        return item;
    }

    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/{name}")
    @DELETE
    @Operation(
        summary = "Delete an existing new item",
        description = "Delete operation with implicit header"
    )
    @Parameter(
       name = "Accept-Language",
       description = "language",
       required = true,
       schema = @Schema(implementation = String.class),
       in = ParameterIn.HEADER
    )
    @APIResponse(responseCode = "204")
    public void delete(@Parameter(required = true) @PathParam("name") String name) {
        items.remove(name);
    }
}
