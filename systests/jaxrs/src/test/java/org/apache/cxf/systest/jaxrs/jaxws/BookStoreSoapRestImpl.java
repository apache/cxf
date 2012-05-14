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

package org.apache.cxf.systest.jaxrs.jaxws;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.cxf.annotations.SchemaValidation;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.systest.jaxrs.BookNotFoundDetails;
import org.apache.cxf.systest.jaxrs.BookNotFoundFault;
import org.apache.cxf.systest.jaxrs.BookSubresource;
import org.apache.cxf.systest.jaxrs.BookSubresourceImpl;

@SchemaValidation
public class BookStoreSoapRestImpl implements BookStoreJaxrsJaxws {

    private Map<Long, Book> books = new HashMap<Long, Book>();
    private boolean ignoreJaxrsClient;
    @Resource
    private MessageContext jaxrsContext;
    
    @Resource(name = "restClient")
    private BookStoreJaxrsJaxws webClient;
    private boolean invocationInProcess;
    
    public BookStoreSoapRestImpl() {
        init();
    }
    
    public void setIgnoreJaxrsClient(boolean ignore) {
        this.ignoreJaxrsClient = ignore;
    }
    
    @PostConstruct
    public void verifyWebClient() {
        if (!ignoreJaxrsClient) {
            if (webClient == null) {
                throw new RuntimeException();
            }
            WebClient.client(webClient).accept("application/xml");
        }
    }
    
    public Book getBook(Long id) throws BookNotFoundFault {
        
        if (books.get(id) == null) {
            if (id == 0) {
                try {
                    OutputStream os = jaxrsContext.getHttpServletResponse().getOutputStream();
                    JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
                    Marshaller m = c.createMarshaller();
                    m.marshal(books.get(123L), os);
                    os.flush();
                    return null;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new RuntimeException();
                }
            }
            int returnCode = 404;
            if (id == 321) {
                returnCode = 525;
            } else if (id == 322) {
                BookNotFoundDetails details = new BookNotFoundDetails();
                details.setId(id);
                throw new BookNotFoundFault(details);
            }
            String msg = "No Book with id " + id + " is available";
            ResponseBuilder builder = Response.status(returnCode).header("BOOK-HEADER", msg);
            
            if (returnCode == 404) {
                builder.type("text/plain").entity(msg);
            }
            throw new WebApplicationException(builder.build());
        }
     
        if (!ignoreJaxrsClient) {
            if (!invocationInProcess) {
                invocationInProcess = true;
                return webClient.getBook(id);
            }
            invocationInProcess = false;
        }
        
        
        return books.get(id);
    }
    
    public Book addBook(Book book) {
        book.setId(124);
        books.put(book.getId(), book);
        return books.get(book.getId());
    }
    
    private void init() {
        Book book = new Book();
        book.setId(new Long(123));
        book.setName("CXF in Action");
        books.put(book.getId(), book);
    }
 

    @WebMethod(exclude = true)
    public BookSubresource getBookSubresource(String id) {
        return new BookSubresourceImpl(Long.valueOf(id));
    }

    @WebMethod(exclude = true)
    public BookStoreJaxrsJaxws getBookStore(String id) {
        if (!"number1".equals(id)) {
            throw new WebApplicationException(404);
        }
        return this;
    }

    public Book addFastinfoBook(Book book) {
        return book;
    }
    
    public Book getFastinfoBook() {
        return new Book("CXF2", 2L);
    }
    
}
