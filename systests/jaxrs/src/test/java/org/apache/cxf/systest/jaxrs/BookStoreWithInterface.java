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

import org.apache.cxf.annotations.SchemaValidation;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;





public class BookStoreWithInterface extends BookStoreStorage implements BookInterface {

    public BookStoreWithInterface() {
        Book book = new Book();
        book.setId(bookId);
        book.setName("CXF in Action");
        books.put(book.getId(), book);
    }

    public Book getThatBook(Long id, String s) throws BookNotFoundFault {
        checkPostConstruct();
        if (!id.toString().equals(s)) {
            throw new RuntimeException();
        }
        return doGetBook(id);
    }

    @SchemaValidation
    public Book getThatBook(Long id) throws BookNotFoundFault {
        checkPostConstruct();
        return doGetBook(id);
    }

    public Book echoBook(Book b) {
        String ct = (String)JAXRSUtils.getCurrentMessage().get(Message.CONTENT_TYPE);
        if ("application/xml;a=b".equals(ct)) {
            return b;
        }
        throw new RuntimeException();
    }

    private Book doGetBook(Long id) throws BookNotFoundFault {
        //System.out.println("----invoking getBook with id: " + id);
        Book book = books.get(id);
        if (book != null) {
            return book;
        }
        BookNotFoundDetails details = new BookNotFoundDetails();
        details.setId(id);
        throw new BookNotFoundFault(details);
    }

    public Book getThatBook() throws BookNotFoundFault {
        return books.get(123L);
    }

}
