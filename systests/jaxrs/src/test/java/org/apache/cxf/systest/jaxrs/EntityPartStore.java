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


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

@Path("/bookstore")
public class EntityPartStore {
    @POST
    @Path("/books/images")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public List<EntityPart> postImags(List<EntityPart> parts) throws IOException {
        final List<EntityPart> replies = new ArrayList<>(parts.size());

        for (EntityPart part : parts) {
            final Optional<String> fileName = part.getFileName();
            final MultivaluedMap<String, String> headers = part.getHeaders();
            final MediaType mediaType = part.getMediaType();
            try (InputStream is = part.getContent()) {
                replies.add(
                    EntityPart
                        .withFileName(fileName.get())
                        .headers(headers)
                        .mediaType(mediaType)
                        .content(is.readAllBytes())
                        .build());
            }
        }

        return replies;
    }
    
    @POST
    @Path("/books/jaxbform")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addBookJaxbFromForm(@FormParam("bookXML") EntityPart part) throws Exception {
        Book b1 = part.getContent(Book.class);
        b1.setId(124);
        return Response.ok(b1).build();
    }

    @POST
    @Path("/books/jsonform")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addBookJsonFromForm(@FormParam("gazetteer") EntityPart part) throws Exception {
        Book b1 = part.getContent(Book.class);
        b1.setId(124);
        return Response.ok(b1).build();
    }

    @POST
    @Path("/books/jsonjaxbform")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addBookJaxbJsonForm(@FormParam("bookXML") EntityPart part1,
            @FormParam("gazetteer") EntityPart part2) throws Exception {
        final Book b1 = part1.getContent(Book.class);
        final Book b2 = part2.getContent(Book.class);
        if (!"CXF in Action - 2".equals(b1.getName())
            || !"CXF in Action - 2".equals(b2.getName())) {
            throw new WebApplicationException();
        }
        b2.setId(124);
        return Response.ok(b2).build();
    }
}
