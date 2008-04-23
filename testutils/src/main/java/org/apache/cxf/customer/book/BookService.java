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

import org.codehaus.jra.Delete;
import org.codehaus.jra.Get;
import org.codehaus.jra.HttpResource;
import org.codehaus.jra.Post;
import org.codehaus.jra.Put;

@WebService(targetNamespace = "http://book.acme.com")
public interface BookService {

    @Get
    @HttpResource(location = "/books")
    @WebResult(name = "Books")
    Books getBooks();

    @Get
    @HttpResource(location = "/books/{id}")
    @WebResult(name = "Book")
    Book getBook(@WebParam(name = "GetBook")
                         GetBook getBook) throws BookNotFoundFault;

    @Get
    @HttpResource(location = "/books/another/{id}")
    @WebResult(name = "Book")
    Book getAnotherBook(@WebParam(name = "GetAnotherBook")
                         GetAnotherBook getAnotherBook) throws BookNotFoundFault;

    
    @Put
    @HttpResource(location = "/books/{id}")
    void updateBook(@WebParam(name = "Book")
                        Book c);

    @Post
    @HttpResource(location = "/books")
    @WebResult(name = "book")
    long addBook(@WebParam(name = "Book")
                     Book c);

    @Delete
    @HttpResource(location = "/books/{id}")
    void deleteBook(@WebParam(name = "id")
                        long id) throws BookNotFoundFault;

}
