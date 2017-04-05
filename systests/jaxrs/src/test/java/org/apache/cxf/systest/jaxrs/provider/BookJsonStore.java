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

package org.apache.cxf.systest.jaxrs.provider;

import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/bookstore/")
public class BookJsonStore {
    private Map< Long, Book > books = new HashMap<>();

    @GET
    @Path("/books/{bookId}")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getBook(@PathParam("bookId") Long id) {
        final Book book = books.get(id);

        if (book == null) {
            return null;
        }

        return bookToJson(book);
    }

    @GET
    @Path("/books")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonArray getBooks() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();

        for (final Book book: books.values()) {
            builder.add(bookToJson(book));
        }

        return builder.build();
    }

    @POST
    @Path("/books")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addBook(@Context final UriInfo uriInfo, JsonObject obj) {
        final Book book = bookFromJson(obj);
        books.put(book.getId(), book);

        return Response.created(
            uriInfo
                .getRequestUriBuilder()
                .path(Long.toString(book.getId()))
                .build()).build();
    }

    @DELETE
    @Path("/books")
    public Response deleteAll() {
        books.clear();
        return Response.ok().build();
    }

    private JsonObject bookToJson(final Book book) {
        final JsonObjectBuilder builder = Json
                .createObjectBuilder()
                .add("id", book.getId())
                .add("name", book.getName());

        if (!book.getChapters().isEmpty()) {
            final JsonArrayBuilder chapters = Json.createArrayBuilder();

            for (final BookChapter chapter: book.getChapters()) {
                chapters.add(Json.createObjectBuilder()
                    .add("id", chapter.getId())
                    .add("title", chapter.getTitle())
                );
            }

            builder.add("chapters", chapters);
        }

        return builder.build();
    }

    private Book bookFromJson(JsonObject obj) {
        final Book book = new Book(obj.getString("name"), obj.getInt("id"));
        final JsonArray chapters = (JsonArray)obj.get("chapters");
        if (chapters != null && !chapters.isEmpty()) {
            for (final JsonObject chapter: chapters.getValuesAs(JsonObject.class)) {
                book.addChapter(chapter.getInt("id"), chapter.getString("title"));
            }
        }

        return book;
    }

}
