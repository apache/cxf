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
public class BookServiceWrappedImpl implements BookServiceWrapped {
    long currentId = 1;
    Map books = new HashMap();
    
    @SuppressWarnings("unchecked")
    public BookServiceWrappedImpl() {
        Book book = createBook();
        System.out.println("Register the Book's id " + book.getId());
        books.put(book.getId(), book);
    }

    public Book getBook(long bookid) throws BookNotFoundFault {
        for (Iterator iter = books.entrySet().iterator(); iter.hasNext();) { 
            Map.Entry me = (Map.Entry)iter.next();
            System.out.println("getBook -> " + me.getKey() + " : " 
                               + ((Book)me.getValue()).getName() + ", " + ((Book)me.getValue()).getId());
        }
        System.out.println("The Book's id " + bookid);
        Book b = (Book)books.get(bookid);

        if (b == null) {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(bookid);
            throw new BookNotFoundFault(details);
        }
        return b;
    }
    
    final Book createBook() {
        Book b = new Book();
        b.setName("CXF in Action");
        b.setId(123);
        return b;
    }
}
