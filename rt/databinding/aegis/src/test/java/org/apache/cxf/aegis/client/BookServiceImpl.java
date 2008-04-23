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
package org.apache.cxf.aegis.client;

import java.util.ArrayList;
import java.util.List;

public class BookServiceImpl implements BookService {
    private List<Book> allBooks = new ArrayList<Book>();

    public BookServiceImpl() {
    }

    public void addBook(Book book) {
        allBooks.add(book);
    }

    public void addBooks(Book[] books) {
        for (int i = 0; i < books.length; i++) {
            allBooks.add(books[i]);
        }
    }

    public Book[] getBooks() {
        return allBooks.toArray(new Book[allBooks.size()]);
    }

    public Book findBook(String isbn) {
        for (int i = 0; i < allBooks.size(); i++) {
            Book book = allBooks.get(i);
            if (isbn.equals(book.getIsbn())) {
                return book;
            }
        }

        return null;
    }

}
