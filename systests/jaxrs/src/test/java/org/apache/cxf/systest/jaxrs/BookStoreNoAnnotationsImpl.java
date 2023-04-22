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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.common.util.PropertyUtils;

public class BookStoreNoAnnotationsImpl implements BookStoreNoAnnotationsInterface,
    HttpHeadersContext {

    private Map<Long, Book> books = new HashMap<>();
    @Context
    private UriInfo ui;

    private HttpHeaders hs;

    public BookStoreNoAnnotationsImpl() {
        Book b = new Book();
        b.setId(123L);
        b.setName("CXF in Action");
        books.put(b.getId(), b);
    }

    public void setHttpHeaders(HttpHeaders headers) {
        this.hs = headers;
    }

    public Book getBook(Long id) throws BookNotFoundFault {
        if (hs == null) {
            throw new WebApplicationException(Response.serverError().build());
        }
        boolean springProxy = PropertyUtils.isTrue(hs.getHeaderString("SpringProxy"));
        if (!springProxy && ui == null) {
            throw new WebApplicationException(Response.serverError().build());
        }
        return books.get(id);
    }

    public ChapterNoAnnotations getBookChapter(Long id) throws BookNotFoundFault {
        Book b = books.get(id);
        Chapter ch = b.getChapter(1);

        ChapterNoAnnotations ch2 = new ChapterNoAnnotations();
        ch2.setId(ch.getId());
        ch2.setTitle(ch.getTitle());
        return ch2;
    }

    public List<Book> getBooks(List<Book> thebooks) {
        return thebooks;
    }

    public void pingBookStore() {
        // complete
    }

}
