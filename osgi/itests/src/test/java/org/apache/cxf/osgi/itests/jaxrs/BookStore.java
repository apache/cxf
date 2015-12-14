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
import java.util.HashMap;
import java.util.Map;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.validation.BeanValidationProvider;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;

@Path("/bookstore")
@Produces("application/xml")
public class BookStore {
    private Map<Long, Book> books = new HashMap<Long, Book>();

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
        } else {
            b.setName(book.getName());
            return Response.ok().build();
        }
    }

    @POST
    @Path("/books-validate")
    public Response createBookValidate(Book book) {
        assertInjections();
        BeanValidationProvider prov;
        ClassLoader oldtccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(HibernateValidator.class.getClassLoader());
            HibernateValidatorConfiguration configuration =
                    Validation.byProvider(HibernateValidator.class)
                            .configure();
            ValidatorFactory factory = configuration.buildValidatorFactory();
            prov = new BeanValidationProvider(factory);
        } finally {
            Thread.currentThread().setContextClassLoader(oldtccl);
        }
        prov.setValidateContextClassloader(getClass().getClassLoader());
        try {
            prov.validateBean(book);
        } catch (ConstraintViolationException cve) {
            StringBuilder violationMessages = new StringBuilder();
            for (ConstraintViolation<?> constraintViolation : cve.getConstraintViolations()) {
                violationMessages.append(constraintViolation.getPropertyPath())
                        .append(": ").append(constraintViolation.getMessage()).append("\n");
            }
            return Response.status(Response.Status.BAD_REQUEST).type("text/plain")
                    .entity(violationMessages.toString()).build();
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
        } else {
            books.put(book.getId(), book);
            URI createdURI = UriBuilder.fromUri(ui.getAbsolutePath())
                .path(Long.toString(book.getId())).build();
            return Response.created(createdURI).build();
        }
    }

    @DELETE
    @Path("/books/{id}")
    public Response removeBook(@PathParam("id") Long id) {
        assertInjections();
        Book b = books.remove(id);
        if (b == null) {
            return Response.status(Status.NOT_FOUND).build();
        } else {
            return Response.ok().build();
        }
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


