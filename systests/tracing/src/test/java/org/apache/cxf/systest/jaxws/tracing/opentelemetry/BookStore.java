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
package org.apache.cxf.systest.jaxws.tracing.opentelemetry;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import jakarta.annotation.Resource;
import jakarta.jws.Oneway;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.handler.MessageContext;
import org.apache.cxf.systest.Book;
import org.apache.cxf.systest.jaxws.tracing.BookStoreService;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

@WebService(endpointInterface = "org.apache.cxf.systest.jaxws.tracing.BookStoreService", serviceName = "BookStore")
public class BookStore implements BookStoreService {
    private final Tracer tracer;

    @Resource
    private WebServiceContext context;

    public BookStore() {
        tracer = GlobalOpenTelemetry.getTracer(BookStore.class.getName());
    }

    @WebMethod
    public Collection<Book> getBooks() {
        final Span span = tracer.spanBuilder("Get Books").startSpan();
        try (Scope scope = span.makeCurrent()) {
            return Arrays.asList(new Book("Apache CXF in Action", UUID.randomUUID().toString()),
                                 new Book("Mastering Apache CXF", UUID.randomUUID().toString()));
        } finally {
            span.end();
        }
    }

    @WebMethod
    public int removeBooks() {
        throw new RuntimeException("Unable to remove books");
    }

    @WebMethod
    public void addBooks() {
        final MessageContext ctx = context.getMessageContext();
        ctx.put(MessageContext.HTTP_RESPONSE_CODE, 202);
    }

    @WebMethod @Oneway
    public void orderBooks() {
    }
}
