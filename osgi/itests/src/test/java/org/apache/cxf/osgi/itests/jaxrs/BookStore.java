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


import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationProviderResolver;
import jakarta.validation.spi.ValidationProvider;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.validation.BeanValidationProvider;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;

@Path("/bookstore")
@Produces("application/xml")
public class BookStore {
    private Map<Long, Book> books = new HashMap<>();

    @Context
    private UriInfo ui;

    @Context
    private ResourceInfo rcInfo;

    @Context
    private ResourceContext resourceContext;

    @Context
    private SecurityContext securityContext;

    public BookStore() {
        init();
    }

    @GET
    @Path("/books/{id}")
    public Response getBookRoot(@PathParam("id") Long id) {
        assertInjections();
        Book b = books.get(id);
        if (b == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(b).build();
    }

    @PUT
    @Path("/books/{id}")
    public Response updateBook(@PathParam("id") Long id, Book book) {
        assertInjections();
        Book b = books.get(id);
        if (b == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        b.setName(book.getName());
        return Response.ok().build();
    }

    @POST
    @Path("/books-validate")
    public Response createBookValidate(Book book) {
        assertInjections();

        BeanValidationProvider prov = new BeanValidationProvider(
            new ValidationProviderResolver() {
                @Override
                public List<ValidationProvider<?>> getValidationProviders() {
                    ValidationProvider<HibernateValidatorConfiguration> prov = new HibernateValidator();
                    List<ValidationProvider<?>> provs = new ArrayList<>();
                    provs.add(prov);
                    return provs;
                }
            }, HibernateValidator.class);
        try {
            prov.validateBean(book);
        } catch (ConstraintViolationException cve) {
            StringBuilder violationMessages = new StringBuilder();
            for (ConstraintViolation<?> constraintViolation : cve.getConstraintViolations()) {
                violationMessages.append(constraintViolation.getPropertyPath())
                        .append(": ").append(constraintViolation.getMessage()).append('\n');
            }
            return Response.status(Response.Status.BAD_REQUEST)
                    .type("text/plain").entity(violationMessages.toString()).build();
        }
        return createBook(book);
    }

    @POST
    @Path("/books")
    public Response createBook(Book book) {
        assertInjections();
        Book b = books.get(book.getId());
        if (b != null) {
            return Response.status(Status.CONFLICT).build();
        }
        books.put(book.getId(), book);
        URI createdURI = UriBuilder.fromUri(ui.getAbsolutePath())
            .path(Long.toString(book.getId())).build();
        return Response.created(createdURI).build();
    }

    @DELETE
    @Path("/books/{id}")
    public Response removeBook(@PathParam("id") Long id) {
        assertInjections();
        Book b = books.remove(id);
        if (b == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok().build();
    }

    private void init() {
        books.clear();

        Book book = new Book();
        book.setId(123);
        book.setName("CXF in Action");
        books.put(book.getId(), book);
    }

    private void assertInjections() {
        if (ui.getAbsolutePath() == null) {
            throw new IllegalArgumentException("UriInfo absolute path is null");
        }
        if (rcInfo.getResourceMethod() == null) {
            throw new IllegalArgumentException("ResourceInfo resource method is null");
        }
        if (resourceContext.getResource(BookStore.class) == null) {
            throw new IllegalArgumentException("ResourceContext returns null resource");
        }
        if (securityContext.isSecure()) {
            throw new IllegalArgumentException("Expected unsecure communication");
        }
    }
}


