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

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;


public class BookStoreWithInterface2 extends BookStoreStorage implements BookInterface {

    private ServletContext servletContext; 
    
    public BookStoreWithInterface2() {
        Book book = new Book();
        book.setId(bookId);
        book.setName("CXF in Action");
        books.put(book.getId(), book);
    }
    
    public BookStoreWithInterface2(@Context ServletContext scontext) {
        this();
        this.servletContext = scontext;
    }
    
    public Book getThatBook(Long id, String s) throws BookNotFoundFault {
        if (servletContext == null) {
            throw new RuntimeException();
        }
        if (!id.toString().equals(s)) {
            throw new RuntimeException();
        }
        return doGetBook(id);
    }
    
    public Book getThatBook(Long id) throws BookNotFoundFault {
        if (servletContext == null) {
            throw new RuntimeException();
        }
        return doGetBook(id);
    }
    
    private Book doGetBook(Long id) throws BookNotFoundFault {
        System.out.println("----invoking getBook with id: " + id);
        Book book = books.get(id);
        if (book != null) {
            return book;
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(id);
            throw new BookNotFoundFault(details);
        }
    }

    public Book getThatBook() throws BookNotFoundFault {
        if (servletContext == null) {
            throw new RuntimeException();
        }
        return books.get(123L);
    }

}
