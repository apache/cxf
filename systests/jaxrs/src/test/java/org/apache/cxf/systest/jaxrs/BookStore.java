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


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.annotations.GZIP;
import org.apache.cxf.common.util.ProxyHelper;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.jaxrs.ext.Nullable;
import org.apache.cxf.jaxrs.ext.Oneway;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.apache.cxf.phase.PhaseInterceptorChain;

@Path("/bookstore")
@GZIP(threshold = 1)
public class BookStore {

    private Map<Long, Book> books = new HashMap<Long, Book>();
    private Map<Long, CD> cds = new HashMap<Long, CD>();
    private long bookId = 123;
    private long cdId = 123;
    
    private String defaultName;
    private long defaultId;
    
    private String currentBookId;
    @PathParam("CDId")
    private String currentCdId;
    @Context
    private HttpHeaders httpHeaders;
    @Context 
    private SecurityContext securityContext;
    @Context 
    private UriInfo ui;
    
    public BookStore() {
        init();
    }
    
    @PostConstruct
    public void postConstruct() {
        System.out.println("PostConstruct called");
    }
    
    @PreDestroy
    public void preDestroy() {
        System.out.println("PreDestroy called");
    }
    
    @GET
    @Path("/default")
    @Produces("application/xml")
    public Book getDefaultBook() {
        return new Book(defaultName, defaultId);
    }
    
    @GET
    @Path("/books/colon/{a}:{b}:{c}")
    @Produces("application/xml")
    public Book getBookWithColonMarks(@PathParam("a") String id1,
                                      @PathParam("b") String id2,
                                      @PathParam("c") String id3) throws BookNotFoundFault {
        return doGetBook(id1 + id2 + id3);
    }
    
    @POST
    @Path("emptypost")
    public void emptypost() {
        String uri = ui.getAbsolutePath().toString();
        System.out.println(uri);
        if (uri.endsWith("/")) {
            throw new WebApplicationException(400);
        }
    }
    
    @PUT
    @Path("emptyput")
    public void emptyput() {
    }
    
    @POST
    public void emptypostNoPath() {
        emptypost();
    }
    
    @GET
    @Path("webappexception")
    public Book throwException() {
        
        Response response = Response.serverError().entity("This is a WebApplicationException").build();
        throw new WebApplicationException(response);
    }
    
    @GET
    @Path("webappexceptionXML")
    public Book throwExceptionXML() {
        
        Response response = Response.status(406).type("application/xml")
                            .entity("<Book><name>Exception</name><id>999</id></Book>")
                            .build();
        throw new WebApplicationException(response);
    }
    
    @GET
    @Path("tempredirect")
    public Response tempRedirectAndSetCookies() {
        URI uri = UriBuilder.fromPath("whatever/redirection")
            .queryParam("css1", "http://bar").build();
        return Response.temporaryRedirect(uri)
                       .header("Set-Cookie", "a=b").header("Set-Cookie", "c=d")
                       .build();
    }
    
    @GET
    @Path("setcookies")
    public Response setComplexCookies() {
        return Response.ok().header("Set-Cookie", 
                                    "bar.com.anoncart=107894933471602436; Domain=.bar.com;"
                                    + " Expires=Thu, 01-Oct-2020 23:44:22 GMT; Path=/")
                                    .build();
    }
    

    @GET
    @Path("setmanycookies")
    public Response setTwoCookies() {
        return Response.ok().header("Set-Cookie", "JSESSIONID=0475F7F30A26E5B0C15D69; Path=/")
            .header("Set-Cookie", "COOKIETWO=dummy; Expires=Sat, 20-Nov-2010 19:11:32 GMT; Path=/")
            .header("Set-Cookie", "COOKIETWO=dummy2; expires=Sat, 20-Nov-2010 19:11:32 GMT; Path=/")
            .build();
    }
    
    @GET
    @Path("propagate-exception")
    public Book propogateException() throws BookNotFoundFault {
        throw new BookNotFoundFault("Book Exception");
    }
    
    @GET
    @Path("name-in-query")
    @Produces("application/xml")
    public Book getBookFromQuery(@QueryParam("name") String name) {
        return new Book(name, 321L);
    }
    
    @GET
    @Path("propagate-exception2")
    public Book propogateException2() throws BookNotFoundFault {
        PhaseInterceptorChain.getCurrentMessage().put("org.apache.cxf.propagate.exception", Boolean.FALSE);
        throw new BookNotFoundFault("Book Exception");
    }
    
    @GET
    @Path("propagate-exception3")
    public Book propogateException3() throws BookNotFoundFault {
        PhaseInterceptorChain.getCurrentMessage().getExchange()
            .put("org.apache.cxf.systest.for-out-fault-interceptor", Boolean.TRUE);
        throw new BookNotFoundFault("Book Exception");
    }
    
    @GET
    @Path("books/check/{id}")
    @Produces("text/plain")
    public boolean checkBook(@PathParam("id") Long id) {
        return books.containsKey(id);
    }
    
    @POST
    @Path("books/check2")
    @Produces("text/plain")
    @Consumes("text/plain")
    public Boolean checkBook2(Long id) {
        return books.containsKey(id);
    }
    
    
    @GET
    @Path("timetable")
    public Calendar getTimetable() {
        return new GregorianCalendar();
    }
    
    @GET
    @Path("wrongparametertype")
    public void wrongParameterType(@QueryParam("p") Map p) {
        throw new IllegalStateException("This op is not expected to be invoked");
    }
    
    @GET
    @Path("exceptionduringconstruction")
    public void wrongParameterType(@QueryParam("p") BadBook p) {
        throw new IllegalStateException("This op is not expected to be invoked");
    }
    
    @POST
    @Path("/unsupportedcontenttype")
    @Consumes("application/xml")
    public String unsupportedContentType() {
        throw new IllegalStateException("This op is not expected to be invoked");
    }
    
    @GET
    @Path("/bookurl/{URL}/")
    public Book getBookByURL(@PathParam("URL") String urlValue) throws Exception {
        String url2 = new URL(urlValue).toString();
        int index = url2.lastIndexOf('/');
        return doGetBook(url2.substring(index + 1));
    }
    
    @OPTIONS
    @Path("/options")
    public Response getOptions() throws Exception {
        return Response.ok().header("Allow", "POST")
                            .header("Allow", "PUT")
                            .header("Allow", "GET")
                            .header("Allow", "DELETE")
                            .build();
    }
    
    @POST
    @Path("post401")
    public Response get401WithText() throws Exception {
        return Response.status(401).entity("This is 401").build();
    }
    
    @POST
    @Path("/collections")
    @Produces({"application/xml", "application/json" })
    @Consumes({"application/xml", "application/json" })
    public List<Book> getBookCollection(List<Book> bs) throws Exception {
        if (bs == null || bs.size() != 2) {
            throw new RuntimeException();
        }
        return bs;
    }
    
    @POST
    @Path("/collections2")
    @Produces({"application/xml", "application/json" })
    @Consumes({"application/xml", "application/json" })
    public List<BookNoXmlRootElement> getBookCollection2(List<BookNoXmlRootElement> bs) throws Exception {
        if (bs == null || bs.size() != 2) {
            throw new RuntimeException();
        }
        return bs;
    }
    
    @GET
    @Path("/collections")
    @Produces({"application/xml", "application/json" })
    public List<Book> getBookCollection() throws Exception {
        return new ArrayList<Book>(books.values());
    }
    
    @POST
    @Path("/array")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book[] getBookArray(Book[] bs) throws Exception {
        if (bs == null || bs.length != 2) {
            throw new RuntimeException();
        }
        return bs;
    }
    
    @GET
    @Path("/segment/{pathsegment}/")
    public Book getBookBySegment(@PathParam("pathsegment") PathSegment segment) throws Exception {
        if (!"matrix2".equals(segment.getPath())) {
            throw new RuntimeException();
        }
        MultivaluedMap<String, String> map = segment.getMatrixParameters();
        String s1 = map.getFirst("first").toString();
        String s2 = map.getFirst("second").toString();
        return doGetBook(s1 + s2);
    }
    
    @GET
    @Path("/segment/list/{pathsegment:.+}/")
    public Book getBookBySegment(@PathParam("pathsegment") List<PathSegment> list) 
        throws Exception {
        return doGetBook(list.get(0).getPath()
                         + list.get(1).getPath()
                         + list.get(2).getPath());
    }
    
    @GET
    @Path("/segment/matrix")
    public Book getBookByMatrixParams(@MatrixParam("first") String s1,
                                      @MatrixParam("second") String s2) throws Exception {
        
        return doGetBook(s1 + s2);
    }
    
    @GET
    @Path("/segment/matrix-middle")
    public Book getBookByMatrixParamsMiddle(@MatrixParam("first") String s1,
                                      @MatrixParam("second") String s2) throws Exception {
        
        return doGetBook(s1 + s2);
    }
    
    @GET
    @Path("/segment/matrix-list")
    public Book getBookByMatrixListParams(@MatrixParam("first") List<String> list) throws Exception {
        if (list.size() != 2) {
            throw new RuntimeException();
        }
        return doGetBook(list.get(0) + list.get(1));
    }
    
    @GET
    @Path("/bookheaders/")
    public Book getBookByHeader(@HeaderParam("BOOK") List<String> ids) throws Exception {
        List<MediaType> types = httpHeaders.getAcceptableMediaTypes();
        if (types.size() != 2 
            || !"text/xml".equals(types.get(0).toString())
            || !MediaType.APPLICATION_XML_TYPE.isCompatible(types.get(1))) {
            throw new WebApplicationException();
        }
        List<Locale> locales = httpHeaders.getAcceptableLanguages();
        if (locales.size() != 2 
            || !"en".equals(locales.get(0).getLanguage())
            || !"da".equals(locales.get(1).getLanguage())) {
            throw new WebApplicationException();
        }
        Map<String, Cookie> cookies = httpHeaders.getCookies();
        if (cookies.size() != 3
            || !cookies.containsKey("a")
            || !cookies.containsKey("c")
            || !cookies.containsKey("e")) {
            throw new WebApplicationException();
        }
        List<String> cookiesList = httpHeaders.getRequestHeader(HttpHeaders.COOKIE);
        if (cookiesList.size() != 3
            || !cookiesList.contains("a=b")
            || !cookiesList.contains("c=d")
            || !cookiesList.contains("e=f")) {
            throw new WebApplicationException();
        }
        return doGetBook(ids.get(0) + ids.get(1) + ids.get(2));
    }
    
    @GET
    @Path("/bookheaders2/")
    public Book getBookByHeader(@DefaultValue("123") @HeaderParam("BOOK2") String id) 
        throws Exception {
        return doGetBook(id);
    }
    
    @GET
    @Path("/getheadbook/")
    public Book getBookGetHead() throws Exception {
        return doGetBook("123");
    }
    
    @HEAD
    @Path("/getheadbook/")
    public Response getBookGetHead2() throws Exception {
        return Response.ok().header("HEAD_HEADER", "HEAD_HEADER_VALUE").build();
    }
    
    
    @GET
    @Path("/bookquery")
    public Book getBookByURLQuery(@QueryParam("urlid") String urlValue) throws Exception {
        String url2 = new URL(urlValue).toString();
        int index = url2.lastIndexOf('/');
        return doGetBook(url2.substring(index + 1));
    }
    
    @GET
    @Path("/bookidarray")
    public Book getBookByURLQuery(@QueryParam("id") String[] ids) throws Exception {
        if (ids == null || ids.length != 3) {
            throw new WebApplicationException(); 
        }
        return doGetBook(ids[0] + ids[1] + ids[2]);
    }

    @GET
    @Path("/securebooks/{bookId}/")
    @Produces("application/xml")
    public Book getSecureBook(@PathParam("bookId") String id) throws BookNotFoundFault {
        if (!securityContext.isSecure()) {
            throw new WebApplicationException(Response.status(403).entity("Unsecure link").build());
        }
        return doGetBook(id);
    }
    
    @GET
    @Path("/genericbooks/{bookId}/")
    @Produces("application/xml")
    public GenericEntity<GenericHandler<Book>> getGenericBook(@PathParam("bookId") String id) 
        throws BookNotFoundFault {
        return new GenericEntity<GenericHandler<Book>>(new GenericHandler<Book>(doGetBook(id))) { };
    }
    
    @GET
    @Path("/genericresponse/{bookId}/")
    @Produces("application/xml")
    public Response getGenericResponseBook(@PathParam("bookId") String id) 
        throws BookNotFoundFault {
        return Response.ok(getGenericBook(id)).build();
    }
    
    @GET
    @Path("/books/{bookId}/")
    @Produces("application/xml")
    public Book getBook(@PathParam("bookId") String id) throws BookNotFoundFault {
        return doGetBook(id);
    }
    
    
    
    @GET
    @Path("/books/response/{bookId}/")
    @Produces("application/xml")
    public Response getBookAsResponse(@PathParam("bookId") String id) throws BookNotFoundFault {
        Book entity = doGetBook(id);
        EntityTag etag = new EntityTag(Integer.toString(entity.hashCode()));
        return Response.ok().tag(etag).entity(entity).build();
    }
    
    @GET
    @Path("/books/{bookId}/cglib")
    @Produces("application/xml")
    public Book getBookCGLIB(@PathParam("bookId") String id) throws BookNotFoundFault {
        return createCglibProxy(doGetBook(id));
    }
    
    @GET
    @Path("/the books/{bookId}/")
    @Produces("application/xml")
    public Book getBookWithSpace(@PathParam("bookId") String id) throws BookNotFoundFault {
        return doGetBook(id);
    }
    
    @GET
    @Path("/books/search")
    @Produces("application/xml")
    public Book getBook(@Context SearchContext searchContext) 
        throws BookNotFoundFault {
        
        SearchCondition<Book> sc = searchContext.getCondition(Book.class);
        if (sc == null) {
            throw new BookNotFoundFault("Search exception");
        }
        List<Book> found = sc.findAll(books.values());
        if (found.size() != 1) {
            throw new BookNotFoundFault("Single book is expected");
        }
        return found.get(0);
    }
    
    @GET
    @Path("/books/{bookId}/")
    @Produces("text/xml")
    public Book getBookTextXml(@PathParam("bookId") String id) throws BookNotFoundFault {
        return doGetBook(id);
    }
    
    @GET
    @Path("/books/wrapper/{bookId}/")
    @Produces("application/xml")
    public BookWrapper getWrappedBook(@PathParam("bookId") Long id) throws BookNotFoundFault {
        BookWrapper bw = new BookWrapper();
        Book b = new Book("CXF in Action", 99999L);
        bw.setBook(b);
        return bw;
    }
    
    @GET
    @Path("/books/wrapper2/{bookId}/")
    @Produces("application/xml")
    public Book getWrappedBook2(@PathParam("bookId") Long id) throws BookNotFoundFault {
        return new Book("CXF in Action", 99999L);
    }
    
    @GET
    @Path("books/custom/{bookId:\\d\\d\\d}")
    public Book getBookCustom(@PathParam("bookId") String id) throws BookNotFoundFault {
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
    
    @POST
    @Path("/books/element/echo")
    public JAXBElement<Book> echoBookElement(JAXBElement<Book> element) throws Exception {
        return element;
    }
    
    @SuppressWarnings("unchecked")
    @POST
    @Path("/books/element/echo")
    public JAXBElement<? super Book> echoBookElementWildcard(JAXBElement<? extends Book> element) 
        throws Exception {
        return (JAXBElement<? super Book>)element;
    }
    
    @GET
    @Path("/books/adapter")
    @XmlJavaTypeAdapter(BookInfoAdapter.class)
    public BookInfo getBookAdapter() throws Exception {
        return new BookInfo(doGetBook("123"));
    }
    @POST
    @Path("/books/adapter-list")
    @XmlJavaTypeAdapter(BookInfoAdapter.class)
    @Consumes("application/xml")
    @Produces({"application/xml", "application/json" })
    public List<BookInfo> getBookAdapterList(@XmlJavaTypeAdapter(BookInfoAdapter.class) 
                                              List<BookInfo> collection) 
        throws Exception {
        if (collection.size() != 1) {
            throw new WebApplicationException(400);
        }
        return collection;
    }
    
    @GET
    @Path("/books/interface/adapter")
    public BookInfoInterface getBookAdapterInterface() throws Exception {
        return new BookInfo2(doGetBook("123"));
    }
    
    @GET
    @Path("/books/interface/adapter-list")
    public List<? extends BookInfoInterface> getBookAdapterInterfaceList() throws Exception {
        List<BookInfoInterface> list = new ArrayList<BookInfoInterface>();
        list.add(new BookInfo2(doGetBook("123")));
        return list;
    }
    
    @GET
    @Path("/books/adapter-list")
    @XmlJavaTypeAdapter(BookInfoAdapter.class)
    public List<? extends BookInfo> getBookAdapterList() throws Exception {
        List<BookInfo> list = new ArrayList<BookInfo>();
        list.add(new BookInfo(doGetBook("123")));
        return list;
    }
    
    @PathParam("bookId")
    public void setBookId(String id) {
        currentBookId = id;
    }
    
    public void setDefaultNameAndId(String name, long id) {
        defaultName = name;
        defaultId = id;
    }
    
    @GET
    @Path("/books/{bookId}/")
    @Produces("application/json;q=0.9")
    public Book getBookAsJSON() throws BookNotFoundFault {
        return doGetBook(currentBookId);
    }
    
    
    @GET
    @Path("/books/buffer")
    @Produces("application/bar")
    public InputStream getBufferedBook() {
        return getClass().getResourceAsStream("resources/expected_get_book123.txt");
    }
    
    @GET
    @Path("/books/fail-early")
    @Produces("application/bar")
    public StreamingOutput failEarlyInWrite() {
        return new StreamingOutputImpl(true);
    }
    
    @GET
    @Path("/books/fail-late")
    @Produces("application/bar")
    public StreamingOutput writeToStreamAndFail() {
        return new StreamingOutputImpl(false);
    }
    
    private Book doGetBook(String id) throws BookNotFoundFault {
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
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            return book;
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(Long.parseLong(id));
            throw new BookNotFoundFault(details);
        }
    }
    
    @Path("/booksubresourceobject/{bookId}/")
    public Object getBookSubResourceObject(@PathParam("bookId") String id) throws BookNotFoundFault {
        return getBookSubResource(id);
    }
    
    @GET
    @Path("/booknames/{bookId}/")
    @Produces("text/*")
    public String getBookName(@PathParam("bookId") int id) throws BookNotFoundFault {
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
    @Path("/books/null")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book handleNullBook(@Nullable Book book) {
        if (book != null) {
            throw new WebApplicationException(400);
        }
        return new Book("Default Book", 222L);
    }
    
    @POST
    @Path("/books")
    @Produces("text/xml")
    @Consumes("application/xml")
    public Response addBook(Book book) {
        String ct1 = httpHeaders.getMediaType().toString();
        String ct2 = httpHeaders.getRequestHeader("Content-Type").get(0);
        String ct3 = httpHeaders.getRequestHeaders().getFirst("Content-Type");
        if (!("application/xml".equals(ct1) && ct1.equals(ct2) && ct1.equals(ct3))) {
            throw new RuntimeException("Unexpected content type");
        }
        
        book.setId(bookId + 1);
        books.put(book.getId(), book);

        return Response.ok(book).build();
    }
    
    @POST
    @Path("/oneway")
    @Oneway
    public void onewayRequest() {
        if (!PhaseInterceptorChain.getCurrentMessage().getExchange().isOneWay()) {
            throw new WebApplicationException();
        }
    }
    
    @POST
    @Path("/books/customstatus")
    @Produces("application/xml")
    @Consumes("text/xml")
    public Book addBookCustomFailure(Book book, @Context HttpServletResponse response) {
        response.setStatus(233);
        response.addHeader("CustomHeader", "CustomValue");
        book.setId(888);
        return book;
    }
    
    @POST
    @Path("/booksinfo")
    @Produces("text/xml")
    @Consumes("application/xml")
    public Response addBook(@XmlJavaTypeAdapter(BookInfoAdapter.class) 
                            BookInfo bookInfo) {
        return Response.ok(bookInfo.asBook()).build();
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
    @Path("/books/{id}")
    public Response createBook(@PathParam("id") Long id) {
        Book b = books.get(id);

        Response r;
        if (b == null) {
            Book newBook = new Book();
            newBook.setId(id);
            books.put(newBook.getId(), newBook);
            r = Response.ok().build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }
    
    @PUT
    @Path("/bookswithdom/")
    public DOMSource updateBook(DOMSource ds) {
        XMLUtils.printDOM(ds.getNode());
        return ds;
    }
    
    @PUT
    @Path("/bookswithjson/")
    @Consumes("application/json")
    public Response updateBookJSON(Book book) {
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
    
    @POST
    @Path("/booksecho")
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response echoBookNameAndHeader(@HeaderParam("CustomHeader") String headerValue, String name) {
        return Response.ok().entity(name).header("CustomHeader", headerValue).build();
    }
    
    @Path("/bookstoresub")
    public BookStore echoThroughBookStoreSub() {
        return this;
    }
    
    @POST
    @Path("/booksecho2")
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response echoBookNameAndHeader2(String name) {
        return echoBookNameAndHeader(httpHeaders.getRequestHeader("CustomHeader").get(0), name);
    }
    
    @GET
    @Path("/cd/{CDId}/")
    public CD getCD() {
        CD cd = cds.get(Long.parseLong(currentCdId));

        return cd;
    }

    @GET
    @Path("/cdwithmultitypes/{CDId}/")
    @Produces({"application/xml", "application/bar+xml", "application/json" }) 
    public CD getCDWithMultiContentTypes(@PathParam("CDId") String id) {
        CD cd = cds.get(Long.parseLong(id));

        return cd;
    }
    
    @GET
    @Path("/cds/")
    public CDs getCDs() {
        CDs c = new CDs();
        c.setCD(cds.values());
        return c;
    }

    @GET
    @Path("quotedheaders")
    public Response getQuotedHeader() {
        return Response.
                ok().
                header("SomeHeader1", "\"some text, some more text\"").
                header("SomeHeader2", "\"some text\"").
                header("SomeHeader2", "\"quoted,text\"").
                header("SomeHeader2", "\"even more text\"").
                header("SomeHeader3", "\"some text, some more text with inlined \\\"\"").
                header("SomeHeader4", "\"\"").
                build();
    }

    @GET
    @Path("badlyquotedheaders")
    public Response getBadlyQuotedHeader(@QueryParam("type")int t) {
        Response.ResponseBuilder rb = Response.ok();
        switch(t) {
        case 0:
            // problem: no trailing quote - doesn't trigger AbstractClient.parseQuotedHeaderValue
            rb.header("SomeHeader0", "\"some text");
            break;
        case 1:
            // problem: text doesn't end with " - triggers AbstractClient.parseQuotedHeaderValue
            rb.header("SomeHeader1", "\"some text, some more text with inlined \\\"");
            break;
        case 2:
            // problem: no character after \ - doesn't trigger AbstractClient.parseQuotedHeaderValue
            rb.header("SomeHeader2", "\"some te\\");
            break;
        case 3:
            // problem: mix of plain text and quoted text in same line - doesn't trigger
            // AbstractClient.parseQuotedHeaderValue
            rb.header("SomeHeader3", "some text").header("SomeHeader3", "\"other quoted\", text").
                header("SomeHeader3", "blah");
            break;
        default:
            throw new RuntimeException("Don't know how to handle type: " + t);
        }
        return rb.build();
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
    
    @XmlJavaTypeAdapter(BookInfoAdapter2.class)
    static interface BookInfoInterface {
        String getName();
        
        long getId();
    }
    
    static class BookInfo {
        private String name;
        private long id;
        
        public BookInfo() {
            
        }
        
        public BookInfo(Book b) {
            this.name = b.getName();
            this.id = b.getId();
            if (id == 0) {
                id = 124;
            }
        }
        
        public String getName() {
            return name;
        }
        
        public long getId() {
            return id;
        }
       
        public Book asBook() {
            Book b = new Book();
            b.setId(id);
            b.setName(name);
            return b;
        }
    }
    
    static class BookInfo2 extends BookInfo implements BookInfoInterface {
        public BookInfo2() {
            
        }
        
        public BookInfo2(Book b) {
            super(b);
        }
    }
        
    public static class BookInfoAdapter2 extends XmlAdapter<Book, BookInfo2> {
        @Override
        public Book marshal(BookInfo2 v) throws Exception {
            return new Book(v.getName(), v.getId());
        }

        @Override
        public BookInfo2 unmarshal(Book b) throws Exception {
            return new BookInfo2(b);
        }
    }
    
    public static class BookInfoAdapter extends XmlAdapter<Book, BookInfo> {

        @Override
        public Book marshal(BookInfo v) throws Exception {
            return new Book(v.getName(), v.getId());
        }

        @Override
        public BookInfo unmarshal(Book b) throws Exception {
            return new BookInfo(b);
        }
        
    }
    
    static class BadBook {
        public BadBook(String s) {
            throw new RuntimeException("The bad book");
        }
    }
    
    private static class StreamingOutputImpl implements StreamingOutput {

        private boolean failEarly;
        
        public StreamingOutputImpl(boolean failEarly) {
            this.failEarly = failEarly;
        }
        
        public void write(OutputStream output) throws IOException, WebApplicationException {
            if (failEarly) {
                throw new WebApplicationException(
                     Response.status(410).type("text/plain")
                     .entity("This is supposed to go on the wire").build());
            } else {
                output.write("This is not supposed to go on the wire".getBytes());
                throw new WebApplicationException(410);
            }
        } 
        
    }
    
    private Book createCglibProxy(final Book book) {
        final InvocationHandler handler = new InvocationHandler() {

            public Object invoke(Object object, Method method, Object[] args) throws Throwable {
                return method.invoke(book, args);
            }
            
        };
        return (Book)ProxyHelper.getProxy(this.getClass().getClassLoader(), 
                                    new Class[]{Book.class}, 
                                    handler);
    }
}


