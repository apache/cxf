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

package org.apache.cxf.systest.hc.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.annotations.GZIP;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.Oneway;
import org.apache.cxf.jaxrs.ext.PATCH;
import org.apache.cxf.jaxrs.ext.StreamingResponse;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

@Path("/bookstore")
@GZIP(threshold = 1)
public class BookStore {

    private Map<Long, Book> books = new HashMap<>();
    private long bookId = 123;

    @Context
    private UriInfo ui;
    @Context
    private MessageContext messageContext;

    public BookStore() {
        init();
    }

    @GET
    @Path("/")
    public Book getBookRoot() {
        return new Book("root", 124L);
    }
    @PUT
    @Path("/updatebook/{id}")
    @Consumes("application/xml")
    @Produces("application/xml")
    public Book updateEchoBook(Book book) {
        if (book.getId() != Long.parseLong(ui.getPathParameters().getFirst("id"))) {
            throw new WebApplicationException(404);
        }
        return new Book("Updated " + book.getName(), book.getId());
    }

    @GET
    @Path("/books/wildcard")
    @Produces("text/*")
    public String getBookTextWildcard() {
        return "book";
    }

    @RETRIEVE
    @Path("/retrieve")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book retrieveBook(Book book) {
        return book;
    }

    @PATCH
    @Path("/patch")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Response patchBook(Book book) {
        if ("Timeout".equals(book.getName())) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            return Response.ok(book).build();
        }
        return Response.ok(book).build();
    }

    @DELETE
    @Path("/deletebody")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book deleteBodyBook(Book book) {
        return book;
    }

    @GET
    @Path("/getbody")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book getBodyBook(Book book) {
        return book;
    }

    @GET
    @Path("setcookies")
    public Response setComplexCookies() {
        return Response.ok().header("Set-Cookie",
                                    "bar.com.anoncart=107894933471602436; Domain=.bar.com;"
                                    + " Expires=Thu, 01-Oct-2020 23:44:22 GMT; Path=/")
                                    .build();
    }

    @GET
    @Path("books/check/{id}")
    @Produces("text/plain,text/boolean")
    public boolean checkBook(@PathParam("id") Long id) {
        return books.containsKey(id);
    }

    @GET
    @Path("/books/statusFromStream")
    @Produces("text/xml")
    public Response statusFromStream() {
        return Response.ok(new ResponseStreamingOutputImpl()).type("text/plain").build();
    }

    @SuppressWarnings("rawtypes")
    @GET
    @Path("/books/streamingresponse")
    @Produces("text/xml")
    public Response getBookStreamingResponse() {
        return Response.ok(new StreamingResponse() {

            @SuppressWarnings("unchecked")
            @Override
            public void writeTo(Writer writer) throws IOException {
                writer.write(new Book("stream", 124L));
            }

        }).build();
    }

    @POST
    @Path("/oneway")
    @Oneway
    public void onewayRequest() {
        if (!PhaseInterceptorChain.getCurrentMessage().getExchange().isOneWay()) {
            throw new WebApplicationException();
        }
    }
    
    @POST
    @Path("/no-content")
    public void noContent() {
    }

    @GET
    @Path("/books/{bookId}/")
    @Produces("application/xml")
    public Book getBook(@PathParam("bookId") String id) {
        return doGetBook(id);
    }

    @GET
    @Path("/segment/matrix")
    public Book getBookByMatrixParams(@MatrixParam("first") String s1,
                                      @MatrixParam("second") String s2) throws Exception {

        return doGetBook(s1 + s2);
    }

    public final String init() {
        books.clear();
        bookId = 123;

        Book book = new Book();
        book.setId(bookId);
        book.setName("CXF in Action");
        books.put(book.getId(), book);

        return "OK";
    }

    private final class ResponseStreamingOutputImpl implements StreamingOutput {
        public void write(OutputStream output) throws IOException, WebApplicationException {
            if (!"text/plain".equals(BookStore.this.messageContext.get("Content-Type"))) {
                throw new RuntimeException();
            }
            BookStore.this.messageContext.put(Message.RESPONSE_CODE, 503);
            MultivaluedMap<String, String> headers = new MetadataMap<>();
            headers.putSingle("Content-Type", "text/custom+plain");
            headers.putSingle("CustomHeader", "CustomValue");
            BookStore.this.messageContext.put(Message.PROTOCOL_HEADERS, headers);

            output.write("Response is not available".getBytes());
        }
    }

    private Book doGetBook(String id) {
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            return book;
        }
        
        throw new NotFoundException(Response
            .status(Status.NOT_FOUND)
            .entity("The book with ID '" + id + "' was not found")
            .build());
    }
}
