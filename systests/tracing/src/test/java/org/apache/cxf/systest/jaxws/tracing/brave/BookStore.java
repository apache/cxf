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
package org.apache.cxf.systest.jaxws.tracing.brave;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import javax.jws.WebMethod;
import javax.jws.WebService;

import brave.Span;
import brave.Tracer.SpanInScope;
import brave.Tracing;
import org.apache.cxf.systest.Book;
import org.apache.cxf.systest.brave.TestSpanReporter;
import org.apache.cxf.systest.jaxws.tracing.BookStoreService;

@WebService(endpointInterface = "org.apache.cxf.systest.jaxws.tracing.BookStoreService", serviceName = "BookStore")
public class BookStore implements BookStoreService {
    private final Tracing brave;

    public BookStore() {
        brave = Tracing.newBuilder()
            .localServiceName("book-store")
            .spanReporter(new TestSpanReporter())
            .build();
    }

    @WebMethod
    public Collection< Book > getBooks() {
        final Span span = brave.tracer().nextSpan().name("Get Books").start();
        try (SpanInScope scope = brave.tracer().withSpanInScope(span)) {
            return Arrays.asList(
                    new Book("Apache CXF in Action", UUID.randomUUID().toString()),
                    new Book("Mastering Apache CXF", UUID.randomUUID().toString())
                );
        } finally {
            span.finish();
        }
    }

    @WebMethod
    public int removeBooks() {
        throw new RuntimeException("Unable to remove books");
    }
}
