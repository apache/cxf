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

package org.apache.cxf.customer.book;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jws.WebService;


@WebService(endpointInterface = "org.apache.cxf.customer.book.BookService")
public class BookServiceImpl implements BookService {
    long currentId = 1;
    Map books = new HashMap();

    @SuppressWarnings("unchecked")
    public BookServiceImpl() {
        Book book = createBook();
        System.out.println("Enregistre Book de id " + book.getId());
        books.put(book.getId(), book);
    }

    @SuppressWarnings("unchecked")
    public Books getBooks() {
        for (Iterator iter = books.entrySet().iterator(); iter.hasNext();) { 
            Map.Entry me = (Map.Entry)iter.next();
            System.out.println("getBooks -> " + me.getKey() + " : " + me.getValue());
        }
        Books b = new Books();
        b.setBooks((Book[])books.values().toArray(new Book[books.size()]));
        return b;
    }

    public Book getBook(GetBook getBook) throws BookNotFoundFault {
        for (Iterator iter = books.entrySet().iterator(); iter.hasNext();) { 
            Map.Entry me = (Map.Entry)iter.next();
            System.out.println("getBook -> " + me.getKey() + " : " 
                               + ((Book)me.getValue()).getName() + ", " + ((Book)me.getValue()).getId());
        }
        System.out.println("Book de id " + getBook.getId());
        Book b = (Book)books.get(((Long)getBook.getId()).longValue());

        if (b == null) {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(getBook.getId());
            throw new BookNotFoundFault(details);
        }
        return b;
    }

    public Book getAnotherBook(GetAnotherBook getAnotherBook) throws BookNotFoundFault {
        for (Iterator iter = books.entrySet().iterator(); iter.hasNext();) { 
            Map.Entry me = (Map.Entry)iter.next();
            System.out.println("getBook -> " + me.getKey() + " : " 
                               + ((Book)me.getValue()).getName() + ", " + ((Book)me.getValue()).getId());
        }
        System.out.println("Book de id " + getAnotherBook.getId());
        Book b = (Book)books.get(((Long)getAnotherBook.getId()).longValue());

        if (b == null) {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(getAnotherBook.getId());
            throw new BookNotFoundFault(details);
        }
        return b;
    }

    @SuppressWarnings("unchecked")
    public void updateBook(Book b) {
        books.put(b.getId(), b);
        for (Iterator iter = books.entrySet().iterator(); iter.hasNext();) { 
            Map.Entry me = (Map.Entry)iter.next();
            System.out.println("updateBook -> " + me.getKey() + " : " + me.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    public long addBook(Book b) {
        long id = ++currentId;
        System.out.println("addBook : " + b.getName());
        b.setId(id);
        books.put(id, b);
        for (Iterator iter = books.entrySet().iterator(); iter.hasNext();) { 
            Map.Entry me = (Map.Entry)iter.next();
            System.out.println("addBook -> " + me.getKey() + " : " 
                               + ((Book)me.getValue()).getName() + ", " + ((Book)me.getValue()).getId());
        }

        return b.getId();
    }

    public void deleteBook(long id) {
        books.remove(id);
        for (Iterator iter = books.entrySet().iterator(); iter.hasNext();) { 
            Map.Entry me = (Map.Entry)iter.next();
            System.out.println("deleteBook -> " + me.getKey() + " : " + me.getValue());
        }
    }

    final Book createBook() {
        Book b = new Book();
        b.setName("CXF in Action");
        b.setId(123);
        return b;
    }
}
