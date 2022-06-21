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

package org.apache.cxf.systest.kerberos.jaxrs.kerberos;


import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

public class BookStoreImpl implements BookStore {
    private Map<Long, Book> books = new HashMap<>();
    private long bookId = 123;

    private String defaultName;
    private long defaultId;

    private String currentBookId;

    public BookStoreImpl() {
        init();
    }

    @Override
    public Book getBookRoot() {
        return new Book("root", 124L);
    }

    @Override
    public Book getDefaultBook() {
        return new Book(defaultName, defaultId);
    }

    @Override
    public Book getBook(@PathParam("bookId") String id) throws BookNotFoundFault {
        return doGetBook(id);
    }

    @Override
    public Book getBook(@QueryParam("bookId") long id) throws BookNotFoundFault {
        return books.get(id + 123);
    }

    @Override
    public Book getBookWithSpace(@PathParam("bookId") String id) throws BookNotFoundFault {
        return doGetBook(id);
    }

    @Override
    public void setBookId(String id) {
        currentBookId = id;
    }

    public void setDefaultNameAndId(String name, long id) {
        defaultName = name;
        defaultId = id;
    }

    @Override
    public Book getBookAsJSON() throws BookNotFoundFault {
        return doGetBook(currentBookId);
    }

    private Book doGetBook(String id) throws BookNotFoundFault {
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            return book;
        }
        BookNotFoundDetails details = new BookNotFoundDetails();
        details.setId(Long.parseLong(id));
        throw new BookNotFoundFault(details);
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

}


