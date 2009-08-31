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


import java.io.StringReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.cxf.customer.book.BookNotFoundDetails;
import org.apache.cxf.customer.book.BookNotFoundFault;

@Path("/")
public class AtomBookStore {

    @Context protected UriInfo uField;
    private HttpHeaders headers;
    private Map<Long, Book> books = new HashMap<Long, Book>();
    private Map<Long, CD> cds = new HashMap<Long, CD>();
    private long bookId = 123;
    private long cdId = 123;
    
    public AtomBookStore() {
        init();
        System.out.println("----books: " + books.size());
    }
    
    @Context
    public void setHttpHeaders(HttpHeaders theHeaders) {
        headers = theHeaders;
    }
    
    @GET
    @Path("/books/jsonfeed")
    @Produces({"application/xml", "application/json", "text/html", "application/atom+xml" })
    public Feed getBooksAsJsonFeed(@Context UriInfo uParam) {
        return getBooksAsFeed(uParam);    
    }
    
    
    @GET
    @Path("/books/feed")
    @Produces({"application/atom+xml", "application/json" })
    public Feed getBooksAsFeed(@Context UriInfo uParam) {
        
        MediaType mt = headers.getMediaType();
        if (!mt.equals(MediaType.valueOf(MediaType.MEDIA_TYPE_WILDCARD))
            && !mt.equals(MediaType.APPLICATION_JSON_TYPE) 
            && !mt.equals(MediaType.APPLICATION_ATOM_XML_TYPE)) {
            throw new WebApplicationException();
        }
        
        return doGetBookAsFeed(uParam);
    }
    
    private Feed doGetBookAsFeed(@Context UriInfo uParam) {
        Factory factory = Abdera.getNewFactory();
        Feed f = factory.newFeed();
        f.setBaseUri(uParam.getAbsolutePath().toString());
        f.setTitle("Collection of Books");
        f.setId("http://www.books.com");
        f.addAuthor("BookStore Management Company");
        try {
            for (Book b : books.values()) {
                
                Entry e = AtomUtils.createBookEntry(b);
                
                f.addEntry(e);
            }
        } catch (Exception ex) {
            // ignore
        }
        return f;
    }
    
    @POST
    @Path("/books/feed")
    @Consumes("application/atom+xml")
    public Response addBookAsEntry(Entry e) {
        try {
            String text = e.getContentElement().getValue();
            StringReader reader = new StringReader(text);
            JAXBContext jc = JAXBContext.newInstance(Book.class);
            Book b = (Book)jc.createUnmarshaller().unmarshal(reader);
            books.put(b.getId(), b);
            
            URI uri = 
                uField.getBaseUriBuilder().path("books").path("entries") 
                                                .path(Long.toString(b.getId())).build();
            return Response.created(uri).entity(e).build();
        } catch (Exception ex) {
            return Response.serverError().build();
        }
    }
    
    @POST
    @Path("/books/feed/relative")
    @Consumes("application/atom+xml")
    public Response addBookAsEntryRelativeURI(Entry e) throws Exception {
        try {
            String text = e.getContentElement().getValue();
            StringReader reader = new StringReader(text);
            JAXBContext jc = JAXBContext.newInstance(Book.class);
            Book b = (Book)jc.createUnmarshaller().unmarshal(reader);
            books.put(b.getId(), b);
            
            URI uri = URI.create("books/entries/" + Long.toString(b.getId()));
            return Response.created(uri).entity(e).build();
        } catch (Exception ex) {
            return Response.serverError().build();
        }
    }
    
    
    @GET
    @Path("/books/entries/{bookId}/")
    @Produces({"application/atom+xml", "application/json" })
    public Entry getBookAsEntry(@PathParam("bookId") String id) throws BookNotFoundFault {
        System.out.println("----invoking getBook with id: " + id);
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            try {
                return AtomUtils.createBookEntry(book, uField.getAbsolutePath().toString());
            } catch (Exception ex) {
                // ignore
            }
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(Long.parseLong(id));
            throw new BookNotFoundFault(details);
        }
        return null;
    }
    
    @Path("/books/subresources/{bookId}/")
    public AtomBook getBook(@PathParam("bookId") String id) throws BookNotFoundFault {
        System.out.println("----invoking getBook with id: " + id);
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            try {
                return new AtomBook(book);
            } catch (Exception ex) {
                // ignore
            }
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(Long.parseLong(id));
            throw new BookNotFoundFault(details);
        }
        return null;
    }
    
    
    
    
    final void init() {
        Book book = new Book();
        book.setId(bookId);
        book.setName("CXF in Action");
        books.put(book.getId(), book);

        CD cd = new CD();
        cd.setId(cdId);
        cd.setName("BOHEMIAN RHAPSODY");
        cds.put(cd.getId(), cd);
        CD cd1 = new CD();
        cd1.setId(++cdId);
        cd1.setName("BICYCLE RACE");
        cds.put(cd1.getId(), cd1);
    }
}


