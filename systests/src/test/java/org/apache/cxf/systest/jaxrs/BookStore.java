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


import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.helpers.XMLUtils;

@Path("/bookstore/")
public class BookStore {

    private Map<Long, Book> books = new HashMap<Long, Book>();
    private Map<Long, CD> cds = new HashMap<Long, CD>();
    private long bookId = 123;
    private long cdId = 123;
    
    private String currentBookId;
    @PathParam("CDId")
    private String currentCdId;

    public BookStore() {
        init();
        System.out.println("----books: " + books.size());
    }
    
    @GET
    @Path("webappexception")
    public Book throwException() {
        
        Response response = Response.serverError().entity("This is a WebApplicationException").build();
        throw new WebApplicationException(response);
    }
    
    @GET
    @Path("books/check/{id}")
    @Produces("text/plain")
    public boolean checkBook(@PathParam("id") Long id) {
        return books.containsKey(id);
    }
    
    
    @GET
    @Path("timetable")
    public Calendar getTimetable() {
        return new GregorianCalendar();
    }
    
    @GET
    @Path("/bookurl/{URL}/")
    public Book getBookByURL(@PathParam("URL") String urlValue) throws Exception {
        String url2 = new URL(urlValue).toString();
        int index = url2.lastIndexOf('/');
        return doGetBook(url2.substring(index + 1));
    }
    
    @GET
    @Path("/segment/{pathsegment}/")
    public Book getBookBySegment(@PathParam("pathsegment") PathSegment segment) throws Exception {
        if (!"matrix".equals(segment.getPath())) {
            throw new RuntimeException();
        }
        MultivaluedMap<String, String> map = segment.getMatrixParameters();
        String s1 = map.getFirst("first").toString();
        String s2 = map.getFirst("second").toString();
        return doGetBook(s1 + s2);
    }
    
    @GET
    @Path("/bookquery")
    public Book getBookByURLQuery(@QueryParam("urlid") String urlValue) throws Exception {
        String url2 = new URL(urlValue).toString();
        int index = url2.lastIndexOf('/');
        return doGetBook(url2.substring(index + 1));
    } 

    @GET
    @Path("/books/{bookId}/")
    public Book getBook(@PathParam("bookId") String id) throws BookNotFoundFault {
        return doGetBook(id);
    }
    
    @GET
    @Path("/books/query")
    public Book getBookQuery(@QueryParam("bookId") long id) throws BookNotFoundFault {
        return doGetBook(Long.toString(id));
    }
    
    @GET
    @Path("/books/defaultquery")
    public Book getDefaultBookQuery(@DefaultValue("123") @QueryParam("bookId") String id) 
        throws BookNotFoundFault {
        return doGetBook(id);
    }
    
    @GET
    @Path("/books/missingquery")
    public Book getBookMissingQuery(@QueryParam("bookId") long id) 
        throws BookNotFoundFault {
        if (id != 0) {
            throw new RuntimeException();
        }
        return doGetBook("123");
    }
    
    @GET
    @Path("/books/element")
    public JAXBElement<Book> getBookElement() throws Exception {
        return new JAXBElement<Book>(new QName("", "Book"),
                                     Book.class,
                                     doGetBook("123"));
    }
    
    @GET
    @Path("/books/adapter")
    @XmlJavaTypeAdapter(BookInfoAdapter.class)
    public BookInfo getBookAdapter() throws Exception {
        return new BookInfo(doGetBook("123"));
    }
    
    @PathParam("bookId")
    public void setBookId(String id) {
        currentBookId = id;
    }
    
    @GET
    @Path("/books/{bookId}/")
    @Produces("application/json")
    public Book getBookAsJSON() throws BookNotFoundFault {
        return doGetBook(currentBookId);
    }
    
    private Book doGetBook(String id) throws BookNotFoundFault {
        System.out.println("----invoking getBook with id: " + id);
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            return book;
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(Long.parseLong(id));
            throw new BookNotFoundFault(details);
        }
    }
    
    @Path("/booksubresource/{bookId}/")
    public Book getBookSubResource(@PathParam("bookId") String id) throws BookNotFoundFault {
        System.out.println("----invoking getBookSubResource with id: " + id);
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            return book;
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(Long.parseLong(id));
            throw new BookNotFoundFault(details);
        }
    }
    
    @GET
    @Path("/booknames/{bookId}/")
    @Produces("text/*")
    public String getBookName(@PathParam("bookId") int id) throws BookNotFoundFault {
        System.out.println("----invoking getBookName with id: " + id);
        Book book = books.get(new Long(id));
        if (book != null) {
            return book.getName();
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(id);
            throw new BookNotFoundFault(details);
        }
    }

    @POST
    @Path("/books")
    @Produces("text/xml")
    @Consumes("application/xml")
    public Response addBook(Book book) {
        System.out.println("----invoking addBook, book name is: " + book.getName());
        book.setId(++bookId);
        books.put(book.getId(), book);

        return Response.ok(book).build();
    }

    @POST
    @Path("/binarybooks")
    @Produces("text/xml")
    @Consumes("application/octet-stream")
    public Response addBinaryBook(long[] book) {
        return Response.ok(book).build();
    }
    
    @PUT
    @Path("/books/")
    public Response updateBook(Book book) {
        System.out.println("----invoking updateBook, book name is: " + book.getName());
        Book b = books.get(book.getId());

        Response r;
        if (b != null) {
            books.put(book.getId(), book);
            r = Response.ok().build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }
    
    @PUT
    @Path("/bookswithdom/")
    public DOMSource updateBook(DOMSource ds) {
        System.out.println("----invoking updateBook with DOMSource");
        XMLUtils.printDOM(ds.getNode());
        return ds;
    }
    
    @PUT
    @Path("/bookswithjson/")
    @Consumes("application/json")
    public Response updateBookJSON(Book book) {
        System.out.println("----invoking updateBook, book name is: " + book.getName());
        Book b = books.get(book.getId());

        Response r;
        if (b != null) {
            books.put(book.getId(), book);
            r = Response.ok().build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }

    @DELETE
    @Path("/books/{bookId}/")
    public Response deleteBook(@PathParam("bookId") String id) {
        System.out.println("----invoking deleteBook with bookId: " + id);
        Book b = books.get(Long.parseLong(id));

        Response r;
        if (b != null) {
            r = Response.ok().build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }
    
    @DELETE
    @Path("/books/id")
    public Response deleteWithQuery(@QueryParam("value") @DefaultValue("-1") int id) {
        if (id != 123) {
            throw new WebApplicationException();
        }
        Book b = books.get(new Long(id));

        Response r;
        if (b != null) {
            r = Response.ok().build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }

    @POST
    @Path("/booksplain")
    @Consumes("text/plain")
    @Produces("text/plain")
    public Long echoBookId(long theBookId) {
        return new Long(theBookId);
    }
    
    @GET
    @Path("/cd/{CDId}/")
    public CD getCD() {
        System.out.println("----invoking getCD with cdId: " + currentCdId);
        CD cd = cds.get(Long.parseLong(currentCdId));

        return cd;
    }

    @GET
    @Path("/cdwithmultitypes/{CDId}/")
    @Produces({"application/xml", "application/json" }) 
    public CD getCDWithMultiContentTypes(@PathParam("CDId") String id) {
        System.out.println("----invoking getCDWithMultiContentTypes with cdId: " + id);
        CD cd = cds.get(Long.parseLong(id));

        return cd;
    }
    
    @GET
    @Path("/cds/")
    public CDs getCDs() {
        System.out.println("----invoking getCDs");
        CDs c = new CDs();
        c.setCD(cds.values());
        return c;
    }
    
    @Path("/interface")
    public BookSubresource getBookFromSubresource() {
        return new BookSubresourceImpl();
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
    
    private static class BookInfo {
        private String name;
        private long id;
        
        public BookInfo(Book b) {
            this.name = b.getName();
            this.id = b.getId();
        }
        
        public String getName() {
            return name;
        }
        
        public long getId() {
            return id;
        }
    }
    
    public static class BookInfoAdapter extends XmlAdapter<Book, BookInfo> {

        @Override
        public Book marshal(BookInfo v) throws Exception {
            return new Book(v.getName(), v.getId());
        }

        @Override
        public BookInfo unmarshal(Book v) throws Exception {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
}


