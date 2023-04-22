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

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

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
