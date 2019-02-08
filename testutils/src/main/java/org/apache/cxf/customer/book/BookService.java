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

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

@WebService(targetNamespace = "http://book.acme.com")
public interface BookService {

    @WebResult(name = "Books")
    Books getBooks();

    @WebResult(name = "Book")
    Book getBook(@WebParam(name = "GetBook")
                         GetBook getBook) throws BookNotFoundFault;

    @WebResult(name = "Book")
    Book getAnotherBook(@WebParam(name = "GetAnotherBook")
                         GetAnotherBook getAnotherBook) throws BookNotFoundFault;


    void updateBook(@WebParam(name = "Book")
                        Book c);

    @WebResult(name = "book")
    long addBook(@WebParam(name = "Book")
                     Book c);

    void deleteBook(@WebParam(name = "id")
                        long id) throws BookNotFoundFault;

}
