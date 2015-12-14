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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.staxutils.DepthExceededStaxException;
import org.apache.cxf.staxutils.StaxUtils;

@Path("/")
@Produces("application/json")
public class BookStoreSpring {

    private Map<Long, Book> books = new HashMap<Long, Book>();
    private Long mainId = 123L;
    @Context
    private UriInfo ui;    
    private boolean postConstructCalled;
    
    public BookStoreSpring() {
        init();
        //System.out.println("----books: " + books.size());
    }
    
    
    @PostConstruct
    public void postConstruct() {
        postConstructCalled = true;
    }
    
    @PreDestroy
    public void preDestroy() {
        //System.out.println("PreDestroy called");
    }
    
    @POST
    @Path("/bookform")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/xml")
    public Book echoBookForm(@Context HttpServletRequest req) {
        String name = req.getParameter("name");
        long id = Long.valueOf(req.getParameter("id"));
        return new Book(name, id);
    }
    @POST
    @Path("/bookform")
    @Consumes("application/xml")
    @Produces("application/xml")
    public String echoBookFormXml(@Context HttpServletRequest req) throws IOException {
        InputStream is = req.getInputStream();
        return IOUtils.readStringFromStream(is);
    }
    
    @GET
    @Path("/books/webex")
    public Books getBookWebEx() {
        throw new WebApplicationException(new RuntimeException("Book web exception")); 
    }
    
    @GET
    @Path("/books/redirectStart")
    public Book getBookRedirectStart() {
        return new Book("Redirect start", 123L); 
    }
    @GET
    @Path("/link")
    public Response getBookLink() {
        URI selfUri = ui.getBaseUriBuilder().path(BookStoreSpring.class).build();
        Link link = Link.fromUri(selfUri).rel("self").build();
        return Response.ok().links(link).build();
    }
    
    @GET
    @Path("/books/redirectComplete")
    public Book getBookRedirectComplete(@Context HttpServletRequest request) {
        Book book = (Book)request.getAttribute(Book.class.getSimpleName().toLowerCase());
        book.setName("Redirect complete: " + request.getRequestURI());
        return book; 
    }
    
    @GET
    @Path("/books/webex2")
    public Books getBookWebEx2() {
        throw new InternalServerErrorException(new RuntimeException("Book web exception")); 
    }
    
    @GET
    @Path("/books/list/{id}")
    public Books getBookAsJsonList(@PathParam("id") Long id) {
        return new Books(books.get(id));
    }
    
    @GET
    @Path("/books/xsitype")
    @Produces("application/xml")
    public Book getBookXsiType() {
        return new SuperBook("SuperBook", 999L, true);
    }
    
    @SuppressWarnings("unchecked")
    @GET
    @Path("/books/superbook")
    @Produces("application/json")
    public <T extends Book> T getSuperBookJson() {
        SuperBook book = new SuperBook("SuperBook", 999L, true);
        
        return (T)book;
    }
    
    @SuppressWarnings("unchecked")
    @GET
    @Path("/books/superbooks")
    @Produces("application/json")
    public <T extends Book> List<T> getSuperBookCollectionJson() {
        SuperBook book = new SuperBook("SuperBook", 999L, true);
        
        return Collections.singletonList((T)book);
    }
    
    @POST
    @Path("/books/superbook")
    @Consumes("application/json")
    @Produces("application/json")
    public <T extends Book> T echoSuperBookJson(T book) {
        if (((SuperBook)book).isSuperBook()) {
            return book;
        }
        throw new WebApplicationException(400);
    }
    
    @POST
    @Path("/books/superbooks")
    @Consumes("application/json")
    @Produces("application/json")
    public <T extends Book> List<T> echoSuperBookCollectionJson(List<T> book) {
        if (((SuperBook)book.get(0)).isSuperBook()) {
            return book;
        }
        throw new WebApplicationException(400);
    }
    
    @POST
    @Path("/books/xsitype")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book postGetBookXsiType(Book book) {
        return book;
    }
    
    @GET
    @Path("/books/{id}")
    @Produces({"application/json", "application/vnd.example-com.foo+json" })
    public Book getBookById(@PathParam("id") Long id) {
        return books.get(id);
    }
    
    @GET
    @Path("/bookstore/books/{id}")
    @Produces("application/xml")
    public Book getBookXml(@PathParam("id") Long id) {
        return books.get(id);
    }
    
    @GET
    @Path("/semicolon{id}")
    @Produces("application/xml")
    public Book getBookWithSemicoln(@PathParam("id") String name) {
        return new Book(name, 333L);
    }
    
    @GET
    @Path("/ISO-8859-1/1")
    @Produces({"application/json;charset=ISO-8859-1", "application/xml;charset=ISO-8859-1" })
    public Book getBookISO() throws Exception {
        String eWithAcute = "\u00E9";
        String helloStringUTF16 = "F" + eWithAcute + "lix";
        byte[] iso88591bytes = helloStringUTF16.getBytes("ISO-8859-1");
        String helloStringISO88591 = new String(iso88591bytes, "ISO-8859-1");
        return new Book(helloStringISO88591, 333L);
    }
    
    @GET
    @Path("/ISO-8859-1/2")
    @Produces({"application/json", "application/xml" })
    public Book getBookISO2() throws Exception {
        return getBookISO();
    }
    
    @GET
    @Path("/semicolon2{id}")
    @Produces("application/xml")
    public Book getBookWithSemicolnAndMatrixParam(@PathParam("id") String name,
                                                  @MatrixParam("a") String matrixParam) {
        return new Book(name + matrixParam, 333L);
    }
    
    @GET
    @Path("/bookinfo")
    public Book getBookByUriInfo() throws Exception {
        MultivaluedMap<String, String> params = ui.getQueryParameters();
        String id = params.getFirst("param1") + params.getFirst("param2");
        return books.get(Long.valueOf(id));
    }
    
    @GET
    @Path("/booksquery")
    public Book getBookByQuery(@QueryParam("id") String id) {
        if (!postConstructCalled) {
            throw new RuntimeException();
        }
        String[] values = id.split("\\+");
        StringBuilder b = new StringBuilder();
        b.append(values[0]).append(values[1]);        
        return books.get(Long.valueOf(b.toString()));
    }
     
    @GET
    @Path("id={id}")
    public Book getBookByEncodedId(@PathParam("id") String id) {
        String[] values = id.split("\\+");
        StringBuilder b = new StringBuilder();
        b.append(values[0]).append(values[1]);        
        return books.get(Long.valueOf(b.toString()));
    }
    
    
    @GET
    public Book getDefaultBook() {
        return books.get(mainId);
    }
    
    @POST
    @Path("depth")
    @Produces({"application/xml", "application/json" })
    @Consumes({"application/xml", "application/json" })
    public Book echoBook(Book book) {
        return book;
    }
    
    @POST
    @Path("depth-source")
    @Consumes({"application/xml" })
    public void postSourceBook(Source source) {
        try {
            StaxUtils.copy(source, new ByteArrayOutputStream());
        } catch (DepthExceededStaxException ex) {
            throw new WebApplicationException(413); 
        } catch (XMLStreamException ex) {
            if (ex.getMessage().startsWith("Maximum Number")) {
                throw new WebApplicationException(413);
            }
        }
        throw new WebApplicationException(500);
    }
    
    @POST
    @Path("depth-dom")
    @Consumes({"application/xml" })
    public void postDomBook(DOMSource source) {
        // complete
    }
    
    @POST
    @Path("depth-form")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void depthForm(MultivaluedMap<String, String> map) {
    }

    @POST
    @Path("books/convert")
    @Consumes({"application/xml", "application/json", "application/vnd.example-com.foo+json" })
    @Produces({"application/xml", "application/vnd.example-com.foo+json" })
    public Book convertBook(Book2 book) {
        // how to have Book2 populated ?
        Book b = new Book();
        b.setId(book.getId());
        b.setName(book.getName());
        return b;
    }
    
    @PUT
    @Path("books/convert2/{id}")
    @Consumes({"application/xml", "application/json", "application/jettison" })
    @Produces("application/xml")
    public Book convertBook2(Book2 book) {
        return convertBook(book);
    }
    
    @GET
    @Path("books/aegis")
    @Produces({"application/html;q=1.0", "application/xml;q=0.5", "application/json;q=0.5" })
    public Book getBookAegis() {
        // how to have Book2 populated ?
        Book b = new Book();
        b.setId(124);
        b.setName("CXF in Action - 2");
        return b;
    }
    
    @RETRIEVE
    @Path("books/aegis/retrieve")
    @Produces({"application/html;q=0.5", "application/xml;q=1.0", "application/json;q=0.5" })
    public Book getBookAegisRetrieve() {
        return getBookAegis();
    }
    
    @RETRIEVE_get
    @Path("books/aegis/retrieve/get")
    @Produces({"application/html;q=0.5", "application/xml;q=1.0", "application/json;q=0.5" })
    public Book getBookAegisRetrieveGet() {
        return getBookAegis();
    }
    
    @GET
    @Path("books/xslt/{id}")
    @Produces({"text/html", "application/xhtml+xml", "application/xml" })
    public Book getBookXSLT(@PathParam("id") long id, 
                            @QueryParam("name") String name,
                            @MatrixParam("name2") String name2) {
        // how to have Book2 populated ?
        Book b = new Book();
        b.setId(999);
        b.setName("CXF in ");
        return b;
    }
    
    final void init() {
        Book book = new Book();
        book.setId(mainId);
        book.setName("CXF in Action");
        books.put(book.getId(), book);
    }
    
}


