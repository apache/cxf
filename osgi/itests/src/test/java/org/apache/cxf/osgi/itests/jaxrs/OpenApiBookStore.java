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

package org.apache.cxf.osgi.itests.jaxrs;


import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "BookStore")
public class OpenApiBookStore extends BookStore {
    @Operation(
            summary = "Get book by ID",
            description = "Get operation with path parameter",
            responses = {
                @ApiResponse(content = @Content(schema = @Schema(implementation = Book.class)),
                    description = "Book found", responseCode = "200"),
                @ApiResponse(description = "Book not found", responseCode = "404")
            }
    )
    public Response getBookRoot(@PathParam("id") Long id) {
        return super.getBookRoot(id);
    }

    @Operation(
            summary = "Update book by ID",
            description = "Put operation with path parameter",
            responses = {
                @ApiResponse(description = "Book found and updated", responseCode = "200"),
                @ApiResponse(description = "Book not found", responseCode = "404")
            }
    )
    public Response updateBook(@PathParam("id") Long id, Book book) {
        return super.updateBook(id, book);
    }

    @Operation(
            summary = "Create a book with validation",
            description = "Post operation with entity in body and validation",
            responses = {
                @ApiResponse(description = "Book created", responseCode = "201"),
                @ApiResponse(description = "Validation failed", responseCode = "400")
            }
    )
    public Response createBookValidate(Book book) {
        return super.createBookValidate(book);
    }


    @Operation(
            summary = "Create new book",
            description = "Post operation with entity in body",
            responses = {
                @ApiResponse(description = "Book created", responseCode = "201"),
                @ApiResponse(description = "Book with given ID already exists", responseCode = "409")
            }
    )
    public Response createBook(Book book) {
        return super.createBook(book);
    }

    @Operation(
            summary = "Delete a book",
            description = "Delete operation with path param",
            responses = {
                @ApiResponse(description = "Book deleted", responseCode = "200"),
                @ApiResponse(description = "Book not found", responseCode = "404")
            }
    )
    @DELETE
    @Path("/books/{id}")
    public Response removeBook(@PathParam("id") Long id) {
        return super.removeBook(id);
    }
}


