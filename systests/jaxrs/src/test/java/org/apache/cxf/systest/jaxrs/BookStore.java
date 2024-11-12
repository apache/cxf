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
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.cxf.annotations.GZIP;
import org.apache.cxf.common.util.ProxyHelper;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.Nullable;
import org.apache.cxf.jaxrs.ext.Oneway;
import org.apache.cxf.jaxrs.ext.PATCH;
import org.apache.cxf.jaxrs.ext.StreamingResponse;
import org.apache.cxf.jaxrs.ext.search.QueryContext;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.apache.cxf.jaxrs.ext.xml.XMLInstruction;
import org.apache.cxf.jaxrs.ext.xml.XSISchemaLocation;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.systest.jaxrs.BookServer20.CustomHeaderAdded;
import org.apache.cxf.systest.jaxrs.BookServer20.CustomHeaderAddedAsync;
import org.apache.cxf.systest.jaxrs.BookServer20.PostMatchMode;

import org.junit.Assert;

@Path("/bookstore")
@GZIP(threshold = 1)
public class BookStore {

    private Map<Long, Book> books = new HashMap<>();
    private Map<Long, CD> cds = new HashMap<>();
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
    private UriInfo uiFromConstructor;
    @Context
    private ResourceContext resourceContext;
    @Context
    private MessageContext messageContext;
    @Context
    private Configuration configuration;

    @BeanParam
    private BookBean theBookBean;

    private Book2 book2Sub = new Book2();

    public BookStore() {
        init();
    }
    public BookStore(UriInfo ui) {
        this.uiFromConstructor = ui;
        init();
    }

    @PostConstruct
    public void postConstruct() {
        //System.out.println("PostConstruct called");
    }

    @PreDestroy
    public void preDestroy() {
        //System.out.println("PreDestroy called");
    }
    @GET
    @Path("/booktype")
    @Produces("application/json")
    public BookType getBookType() {
        return new BookType("root", 124L);
    }
    @GET
    @Path("/listoflonganddouble")
    @Produces("text/xml")
    public Book getBookFromListOfLongAndDouble(@QueryParam("value") List<Long> lValue,
                                               @QueryParam("value") List<Double> dValue) {
        StringBuilder lBuilder = new StringBuilder();
        for (Long v : lValue) {
            lBuilder.append(v.longValue());
        }
        StringBuilder dBuilder = new StringBuilder();
        for (Double v : dValue) {
            dBuilder.append(v.longValue());
        }
        String lStr = lBuilder.toString();
        String dStr = dBuilder.toString();
        if (!lStr.equals(dStr)) {
            throw new InternalServerErrorException();
        } else if ("".equalsIgnoreCase(lStr)) {
            lStr = "0";
        }

        return new Book("cxf", Long.parseLong(lStr));
    }

    @GET
    @Path("/")
    public Book getBookRoot() {
        return new Book("root", 124L);
    }
    @PUT
    @Path("/updatebook/{id}")
    @Consumes("application/xml")
    @Produces("application/xml")
    public Book updateEchoBook(Book book) {
        if (book.getId() != Long.parseLong(ui.getPathParameters().getFirst("id"))) {
            throw new WebApplicationException(404);
        }
        return new Book("root", book.getId());
    }
    @GET
    @Path("/books/wildcard")
    @Produces("text/*")
    public String getBookTextWildcard() {
        return "book";
    }

    @GET
    @Path("/bookarray")
    public String[] getBookStringArray() {
        return new String[]{"Good book"};
    }

    @GET
    @Path("/bookindexintarray")
    @Produces("text/plain")
    public int[] getBookIndexAsIntArray() {
        return new int[]{1, 2, 3};
    }

    @GET
    @Path("/bookindexdoublearray")
    @Produces("text/plain")
    public double[] getBookIndexAsDoubleArray() {
        return new double[]{1, 2, 3};
    }

    @GET
    @Path("/uifromconstructor")
    @Produces("text/plain")
    public String getPathFromUriInfo() {
        return uiFromConstructor.getAbsolutePath().toString() + "?prop="
            + configuration.getProperty("book").toString();
    }

    @GET
    @Path("/redirect")
    public Response getBookRedirect(@QueryParam("redirect") Boolean done,
                                    @QueryParam("sameuri") Boolean sameuri,
                                    @CookieParam("a") String cookie) {
        if (done == null) {
            String uri = sameuri.equals(Boolean.TRUE)
                ? ui.getAbsolutePathBuilder().queryParam("redirect", "true").build().toString()
                : "http://otherhost/redirect";
            return Response.status(303).cookie(NewCookie.valueOf("a=b")).header("Location", uri).build();
        }
        return Response.ok(new Book("CXF", 123L), "application/xml")
            .header("RequestURI", this.ui.getRequestUri().toString())
            .header("TheCookie", cookie)
            .build();
    }

    @GET
    @Path("/redirect/relative")
    public Response getBookRedirectRel(@QueryParam("redirect") Boolean done,
                                       @QueryParam("loop") boolean loop) {
        if (done == null) {
            if (loop) {
                return Response.status(303).header("Location", "relative?loop=true").build();
            }
            return Response.status(303).header("Location", "relative?redirect=true").build();
        }
        return Response.ok(new Book("CXF", 124L), "application/xml").build();
    }

    @GET
    @Path("/booklist")
    public List<String> getBookListArray() {
        return Collections.singletonList("Good book");
    }

    @GET
    @Path("/customtext")
    @Produces("text/custom")
    public String getCustomBookTest() {
        return "Good book";
    }

    @GET
    @Path("/dropjsonroot")
    @Produces("application/json")
    public Book getBookDropJsonRoot(@Context MessageContext mc) throws BookNotFoundFault {
        mc.put("drop.json.root.element", "true");
        return doGetBook("123");
    }

    @GET
    @Path("/httpresponse")
    public void getBookDesciptionHttpResponse(@Context HttpServletResponse response) {
        response.setContentType("text/plain");
        try {
            response.getOutputStream().write("Good Book".getBytes());
        } catch (IOException ex) {
            throw new WebApplicationException(ex);
        }
    }

    @RETRIEVE
    @Path("/retrieve")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book retrieveBook(Book book) {
        return book;
    }

    @PATCH
    @Path("/patch")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Response patchBook(Book book) {
        if ("Timeout".equals(book.getName())) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            return Response.ok(book).build();
        }
        return Response.ok(book).build();
    }

    @DELETE
    @Path("/deletebody")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book deleteBodyBook(Book book) {
        return book;
    }

    @GET
    @Path("/getbody")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book getBodyBook(Book book) {
        return book;
    }

    @POST
    @Path("/echoxmlbookquery")
    @Produces("application/xml")
    public Book echoXmlBookQuery(@QueryParam("book") Book book, @QueryParam("id") byte id) {
        if (book.getId() != id) {
            throw new RuntimeException();
        }
        return book;
    }

    @POST
    @Path("/echoxmlbook")
    @Produces("application/xml")
    public Book echoXmlBook(Book book) {
        return book;
    }
    
    @POST
    @Path("/echoxmlbook-i18n")
    @Produces("application/xml")
    public Response echoXmlBooki18n(Book book, @HeaderParam(HttpHeaders.CONTENT_LANGUAGE) String language) {
        return Response.ok(new Book(book.getName() + "-" + language, book.getId())).build();
    }

    // Only books with id consisting of 3 or 4 digits of the numbers between 5 and 9 are accepted
    @POST
    @Path("/echoxmlbookregex/{id : [5-9]{3,4}}")
    @Produces("application/xml")
    public Book echoXmlBookregex(Book book, @PathParam("id") String id) {
        return book;
    }

    @POST
    @Path("/emptyform")
    @Produces("text/plain")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String postEmptyForm(Form form) {
        if (!form.asMap().isEmpty()) {
            throw new WebApplicationException(400);
        }
        return "empty form";
    }

    @POST
    @Path("/form")
    @Produces(MediaType.APPLICATION_FORM_URLENCODED)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Form echoForm(Form form) {
        return form;
    }

    @GET
    @Path("/booknames/123")
    @Produces("application/bar")
    public byte[] getBookName123() {
        Long l = Long.parseLong("123");
        return books.get(l).getName().getBytes();
    }

    @GET
    @Path("/beanparam/{id}")
    @Produces("application/xml")
    public Book getBeanParamBook(@BeanParam BookBean bean) {

        long id = bean.getId() + bean.getId2() + bean.getId3();
        if (bean.getNested().getId4() != id) {
            throw new RuntimeException();
        }
        return books.get(id);
    }

    @Path("/beanparamsub")
    public BookStoreSub getBeanParamBookSub() {
        return new BookStoreSub(this);
    }

    @Path("/querysub")
    public BookStoreQuerySub getQuerySub() {
        return new BookStoreQuerySub();
    }

    @GET
    @Path("/twoBeanParams/{id}")
    @Produces("application/xml")
    public Book getTwoBeanParamsBook(@BeanParam BookBean2 bean1,
                                     @BeanParam BookBeanNested bean2) {

        long id = bean1.getId() + bean1.getId2() + bean1.getId3();
        if (bean2.getId4() != id) {
            throw new RuntimeException("id4 != id");
        }
        return books.get(id);
    }

    @POST
    @Path("/formBeanParams/{id}")
    @Produces("application/xml")
    public Book postFormBeanParamsBook(@BeanParam BookBeanForm bean) {
        long id = bean.getId() + bean.getId2();
        if (bean.getId3() != id) {
            throw new RuntimeException("id3 != id");
        }
        return books.get(id);
    }

    @POST
    @Path("/formParams/{id}")
    @Produces("application/xml")
    public Book postFormParamsBook(@PathParam("id") long id, @QueryParam("id2") long id2, @FormParam("id3") long id3) {
        long theBookId = id + id2;
        if (id3 != theBookId) {
            throw new RuntimeException("id3 != id");
        }
        return books.get(theBookId);
    }

    @GET
    @Path("/formBeanParams/{id}")
    @Produces("application/xml")
    public Book getFormBeanParamsBook(@BeanParam BookBeanForm bean) {
        long id = bean.getId() + bean.getId2();
        if (bean.getId3() != 0) {
            throw new RuntimeException("id3 != 0");
        }
        return books.get(id);
    }

    @GET
    @Path("/formParams/{id}")
    @Produces("application/xml")
    public Book getFormParamsBook(@PathParam("id") long id, @QueryParam("id2") long id2, @FormParam("id3") long id3) {
        long theBookId = id + id2;
        if (id3 != 0) {
            throw new RuntimeException("id3 != 0");
        }
        return books.get(theBookId);
    }

    @POST
    @Path("/mapperonbus")
    public void mapperOnBus() {
        throw new BusMapperException();
    }

    @GET
    @Path("/beanparam2/{id}")
    @Produces("application/xml")
    public Book getBeanParamBook2() {
        return getBeanParamBook(theBookBean);
    }

    @GET
    @Path("emptybook")
    @Produces({"application/xml", "application/json" })
    public Book getEmptyBook() {
        return null;
    }

    @GET
    @Path("emptybook/nillable")
    @Produces({"application/xml", "application/json" })
    @Nullable
    public Book getEmptyBookNullable() {
        return null;
    }

    @GET
    @Path("allCharsButA-B/:@!$&'()*+,;=-._~")
    public Book getWithComplexPath() {
        return new Book("Encoded Path", 125L);
    }

    @GET
    @Path("object")
    public Object getBookAsObject() {
        return new Book("Book as Object", 125L);
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
        //System.out.println(uri);
        if (uri.endsWith("/")) {
            throw new WebApplicationException(400);
        }
    }

    @PUT
    @Path("emptyput")
    @Consumes("application/json")
    public void emptyput() {
        if (!"application/json".equals(httpHeaders.getMediaType().toString())) {
            throw new RuntimeException();
        }
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
            .header("Set-cookie", "COOKIETWO=dummy; Expires=Sat, 20-Nov-2010 19:11:32 GMT; Path=/")
            .header("set-cookie", "COOKIETWO=dummy2; expires=Sat, 20-Nov-2010 19:11:32 GMT; Path=/")
            .build();
    }

    @GET
    @Path("propagate-exception")
    public Book propogateException() throws BookNotFoundFault {
        throw new BookNotFoundFault("Book Exception");
    }

    @GET
    @Path("multipleexceptions")
    public Response getBookWithExceptions(@QueryParam("exception") boolean notReturned)
        throws BookNotFoundFault, BookNotReturnedException {
        if (notReturned) {
            throw new WebApplicationException(Response.status(404).header("Status", "notReturned").build());
        }
        throw new WebApplicationException(Response.status(404).header("Status", "notFound").build());
    }

    @GET
    @Path("multipleexceptions2")
    public Response getBookWithExceptions2(@QueryParam("exception") boolean notReturned)
        throws BookNotReturnedException, BookNotFoundFault {
        return getBookWithExceptions(notReturned);
    }

    @GET
    @Path("propogateExceptionVar/{i}")
    public Book propogateExceptionWithVar() throws BookNotFoundFault {
        return null;
    }

    @GET
    @Path("name-in-query")
    @Produces("application/xml")
    @XMLInstruction("<!DOCTYPE Something SYSTEM 'my.dtd'><?xmlstylesheet href='common.css' ?>")
    @XSISchemaLocation("book.xsd")
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
    @Produces("text/plain,text/boolean")
    public boolean checkBook(@PathParam("id") Long id) {
        return books.containsKey(id);
    }
    
    @GET
    @Path("books/check/uuid/{uuid}")
    @Produces("text/plain,text/boolean")
    public boolean checkBookUuid(@PathParam("uuid") BookId id) {
        return books.containsKey(id.getId());
    }

    @GET
    @Path("books/check/malformedmt/{id}")
    @Produces("text/plain")
    public Response checkBookMalformedMT(@PathParam("id") Long id,
                                         @Context MessageContext mc) {
        mc.put("org.apache.cxf.jaxrs.mediaTypeCheck.strict", false);
        return Response.ok(books.containsKey(id)).type("text").build();
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
    public void wrongParameterType(@QueryParam("p") Map<?, ?> p) {
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

    @GET
    @Path("/options/2")
    public Response getOptions2() throws Exception {
        return getOptions();
    }
    @Path("/options/{id}")
    public int getOptions2Sub(@PathParam("id") int id) {
        throw new RuntimeException();
    }

    @POST
    @Path("post401")
    public Response get401WithText() throws Exception {
        return Response.status(401).entity("This is 401").build();
    }

    @GET
    @Path("infault")
    public Response infault() {
        throw new RuntimeException();
    }

    @GET
    @Path("infault2")
    public Response infault2() {
        throw new RuntimeException();
    }

    @GET
    @Path("outfault")
    public Response outfault() {
        return Response.ok().build();
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
    @Path("/collectionBook")
    @Produces({"application/xml", "application/json" })
    @Consumes({"application/xml", "application/json" })
    public List<Book> postBookGetCollection(@Nullable Book book) throws Exception {
        List<Book> list = new ArrayList<>();
        if (book != null) {
            list.add(book);
        }
        return list;
    }

    @POST
    @Path("/collections3")
    @Produces({"application/xml", "application/json" })
    @Consumes({"application/xml", "application/json" })
    public Book postCollectionGetBook(List<Book> bs) throws Exception {
        if (bs == null || bs.size() != 2) {
            throw new RuntimeException();
        }
        return bs.get(0);
    }

    @POST
    @Path("/jaxbelementcollections")
    @Produces({"application/xml", "application/json" })
    @Consumes({"application/xml", "application/json" })
    public List<JAXBElement<BookNoXmlRootElement>> getJAXBElementBookCollection(
        List<JAXBElement<BookNoXmlRootElement>> bs) throws Exception {
        if (bs == null || bs.size() != 2) {
            throw new RuntimeException();
        }
        BookNoXmlRootElement b11 = bs.get(0).getValue();
        Assert.assertEquals(123L, b11.getId());
        Assert.assertEquals("CXF in Action", b11.getName());
        BookNoXmlRootElement b22 = bs.get(1).getValue();
        Assert.assertEquals(124L, b22.getId());
        Assert.assertEquals("CXF Rocks", b22.getName());
        return bs;
    }
    @POST
    @Path("/jaxbelementxmlrootcollections")
    @Produces({"application/xml", "application/json" })
    @Consumes({"application/xml", "application/json" })
    public List<JAXBElement<Book>> getJAXBElementBookXmlRootCollection(
        List<JAXBElement<Book>> bs) throws Exception {
        if (bs == null || bs.size() != 2) {
            throw new RuntimeException();
        }
        Book b11 = bs.get(0).getValue();
        Assert.assertEquals(123L, b11.getId());
        Assert.assertEquals("CXF in Action", b11.getName());
        Book b22 = bs.get(1).getValue();
        Assert.assertEquals(124L, b22.getId());
        Assert.assertEquals("CXF Rocks", b22.getName());
        return bs;
    }

    @GET
    @Path("/collections")
    @Produces({"application/xml", "application/json" })
    public List<Book> getBookCollection() throws Exception {
        return new ArrayList<>(books.values());
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
        String s1 = map.getFirst("first");
        String s2 = map.getFirst("second");
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
    @Path("/bookheaders/simple/")
    @CustomHeaderAdded
    @PostMatchMode
    public Response getBookByHeaderSimple(@HeaderParam("BOOK") String headerBook,
        @HeaderParam("Simple") String headerSimple) throws Exception {

        ResponseBuilder builder = getBookByHeaderSimpleBuilder(headerBook, headerSimple);
        return builder.build();
    }

    @POST
    @Path("/bookheaders/simple/")
    @CustomHeaderAdded
    @PostMatchMode
    @Consumes("application/xml")
    public Response echoBookByHeaderSimple(Book book,
        @HeaderParam("Content-type") String ct,
        @HeaderParam("BOOK") String headerBook,
        @HeaderParam("Simple") String headerSimple,
        @HeaderParam("ServerReaderInterceptor") String serverInterceptorHeader,
        @HeaderParam("ClientWriterInterceptor") String clientInterceptorHeader,
        @HeaderParam("EmptyRequestStreamDetected") String emptyStreamHeader) throws Exception {
        if (!"application/xml".equals(ct)) {
            throw new RuntimeException();
        }
        ResponseBuilder builder = getBookByHeaderSimpleBuilder(headerBook, headerSimple);
        if (serverInterceptorHeader != null) {
            builder.header("ServerReaderInterceptor", serverInterceptorHeader);
        }
        if (clientInterceptorHeader != null) {
            builder.header("ClientWriterInterceptor", clientInterceptorHeader);
        }
        if (emptyStreamHeader != null) {
            builder.header("EmptyRequestStreamDetected", emptyStreamHeader);
        }
        return builder.build();
    }

    @POST
    @Path("/bookheaders/simple/")
    @CustomHeaderAdded
    @PostMatchMode
    @Consumes("application/v1+xml")
    public Response echoBookByHeaderSimple2(Book book,
        @HeaderParam("Content-type") String ct,
        @HeaderParam("BOOK") String headerBook,
        @HeaderParam("Simple") String headerSimple,
        @HeaderParam("ServerReaderInterceptor") String serverInterceptorHeader,
        @HeaderParam("ClientWriterInterceptor") String clientInterceptorHeader) throws Exception {
        if (!"application/v1+xml".equals(ct)) {
            throw new RuntimeException();
        }
        ResponseBuilder builder = getBookByHeaderSimpleBuilder(headerBook, headerSimple);
        if (serverInterceptorHeader != null) {
            builder.header("ServerReaderInterceptor", serverInterceptorHeader);
        }
        if (clientInterceptorHeader != null) {
            builder.header("ClientWriterInterceptor", clientInterceptorHeader);
        }
        builder.header("newmediatypeused", ct);
        return builder.build();
    }

    @POST
    @Path("/bookheaders/simple/async")
    @PostMatchMode
    @CustomHeaderAdded
    @CustomHeaderAddedAsync
    public Response echoBookByHeaderSimpleAsync(Book book,
                                       @HeaderParam("Content-type") String ct,
                                       @HeaderParam("BOOK") String headerBook,
                                       @HeaderParam("Simple") String headerSimple,
                                       @HeaderParam("ServerReaderInterceptor") String serverInterceptorHeader,
                                       @HeaderParam("ClientWriterInterceptor") String clientInterceptorHeader)
        throws Exception {

        return echoBookByHeaderSimple(book, ct, headerBook, headerSimple, serverInterceptorHeader,
                                      clientInterceptorHeader, null);
    }

    private ResponseBuilder getBookByHeaderSimpleBuilder(@HeaderParam("BOOK") String headerBook,
        @HeaderParam("Simple") String headerSimple) throws Exception {

        ResponseBuilder builder = Response.ok(doGetBook(headerBook));
        if (headerSimple != null) {
            builder.header("Simple", headerSimple);
        }
        String aHeaderValue = httpHeaders.getHeaderString("a");
        if (aHeaderValue != null) {
            builder.header("a", aHeaderValue);
        }
        return builder;
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
    @Path("/genericbooks2/{bookId}/")
    @Produces("application/xml")
    public Response getGenericBook2(@PathParam("bookId") String id)
        throws BookNotFoundFault {
        return Response.ok().entity(getGenericBook(id), getExtraAnnotations()).build();
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
    @Path("/books/query/default")
    @Produces("application/xml")
    public Book getBook(@QueryParam("bookId") long id) throws BookNotFoundFault {
        return books.get(id + 123);
    }

    @GET
    @Path("/books/response/{bookId}/")
    @Produces("application/xml")
    public Response getBookAsResponse(@PathParam("bookId") String id) throws BookNotFoundFault {
        Book entity = doGetBook(id);
        EntityTag etag = new EntityTag(Integer.toString(entity.hashCode()));

        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(100000);
        cacheControl.setPrivate(true);

        return Response.ok().tag(etag).entity(entity).cacheControl(cacheControl).build();
    }

    @GET
    @Path("/books/response2/{bookId}/")
    @Produces("application/xml")
    public Response getBookAsResponse2(@PathParam("bookId") String id) throws BookNotFoundFault {
        Book entity = doGetBook(id);
        EntityTag etag = new EntityTag(Integer.toString(entity.hashCode()));

        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(1);
        cacheControl.setPrivate(true);

        return Response.ok().tag(etag).entity(entity).cacheControl(cacheControl).build();
    }

    @GET
    @Path("/books/response3/{bookId}/")
    @Produces("application/xml")
    public Response getBookAsResponse3(@PathParam("bookId") String id,
                                       @HeaderParam("If-Modified-Since") String modifiedSince
    ) throws BookNotFoundFault {
        Book entity = doGetBook(id);

        EntityTag etag = new EntityTag(Integer.toString(entity.hashCode()));

        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(1);
        cacheControl.setPrivate(true);

        if (modifiedSince != null) {
            return Response.status(304).tag(etag)
                .cacheControl(cacheControl).lastModified(new Date()).build();
        } else {
            return Response.ok().tag(etag).entity(entity)
                .cacheControl(cacheControl).lastModified(new Date()).build();
        }
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
    @Path("/thebooks/{bookId}/")
    @Produces("application/xml")
    public Book getBookWithSemicolon(@Encoded @PathParam("bookId") String id,
                                     @HeaderParam("customheader") String custom) {
        if (!"custom;:header".equals(custom)) {
            throw new RuntimeException();
        }
        Book b = new Book();
        b.setId(Long.valueOf(id.substring(0, 3)));
        b.setName("CXF in Action" + id.substring(3));
        String absPath = ui.getAbsolutePath().toString();
        if (absPath.contains("123;")) {
            b.setName(b.getName() + ";");
        }
        return b;
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
    @Path("/books/querycontext/{expression}")
    @Produces("text/plain")
    public String getBookQueryContext(@PathParam("expression") String expression,
                                      @Context QueryContext searchContext)
        throws BookNotFoundFault {
        return searchContext.getConvertedExpression(expression, Book.class);
    }

    @GET
    @Path("/books/{search}/chapter/{chapter}")
    @Produces("application/xml")
    public Chapter getChapterFromSelectedBook(@Context SearchContext searchContext,
                                              @PathParam("search") String expression,
                                              @PathParam("chapter") int chapter) {

        SearchCondition<Book> sc = searchContext.getCondition(expression, Book.class);
        if (sc == null) {
            throw new WebApplicationException(404);
        }
        List<Book> found = sc.findAll(books.values());
        if (found.size() != 1) {
            throw new WebApplicationException(404);
        }
        Book selectedBook = found.get(0);

        return selectedBook.getChapter(chapter);
    }

    @GET
    @Path("/books({search})/chapter")
    @Produces("application/xml")
    public Chapter getIntroChapterFromSelectedBook(@Context SearchContext searchContext,
                                                   @PathParam("search") String expression) {

        return getChapterFromSelectedBook(searchContext, expression, 1);
    }

    @GET
    @Path("/books[{search}]/chapter")
    @Produces("application/xml")
    public Chapter getIntroChapterFromSelectedBook2(@Context SearchContext searchContext,
                                                   @PathParam("search") String expression) {

        return getChapterFromSelectedBook(searchContext, expression, 1);
    }

    @GET
    @Path("/books/text/xml/{bookId}")
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
    @POST
    @Path("/books/echo")
    public Book echoBookElement(Book element) throws Exception {
        return element;
    }
    @POST
    @Path("/books/json/echo")
    @Consumes("application/json")
    @Produces("application/json")
    public Book echoBookElementJson(Book element) throws Exception {
        return element;
    }

    @SuppressWarnings("unchecked")
    @POST
    @Path("/books/element/echo/wildcard")
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
        List<BookInfoInterface> list = new ArrayList<>();
        list.add(new BookInfo2(doGetBook("123")));
        return list;
    }

    @GET
    @Path("/books/adapter-list")
    @XmlJavaTypeAdapter(BookInfoAdapter.class)
    public List<? extends BookInfo> getBookAdapterList() throws Exception {
        List<BookInfo> list = new ArrayList<>();
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
    @Produces("application/json;qs=0.9")
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
    @Path("/books/statusFromStream")
    @Produces("text/xml")
    public Response statusFromStream() {
        return Response.ok(new ResponseStreamingOutputImpl()).type("text/plain").build();
    }

    @SuppressWarnings("rawtypes")
    @GET
    @Path("/books/streamingresponse")
    @Produces("text/xml")
    public Response getBookStreamingResponse() {
        return Response.ok(new StreamingResponse() {

            @SuppressWarnings("unchecked")
            @Override
            public void writeTo(Writer writer) throws IOException {
                writer.write(new Book("stream", 124L));
            }

        }).build();
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
        }
        BookNotFoundDetails details = new BookNotFoundDetails();
        details.setId(Long.parseLong(id));
        throw new BookNotFoundFault(details);
    }

    @Path("/booksubresource/{bookId}/")
    public Book getBookSubResource(@PathParam("bookId") String id) throws BookNotFoundFault {
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            return book;
        }
        BookNotFoundDetails details = new BookNotFoundDetails();
        details.setId(Long.parseLong(id));
        throw new BookNotFoundFault(details);
    }

    @Path("/booksubresource/context")
    public Book2 getBookSubResourceRC() {
        Book2 book = resourceContext.getResource(Book2.class);
        book.checkContext();
        return book;
    }

    @Path("/booksubresource/instance/context")
    public Book2 getBookSubResourceInstanceRC(@Context ResourceContext rc) {
        return rc.initResource(book2Sub);
    }

    @Path("/booksubresource/class/context")
    public Class<Book2> getBookSubResourceClass() {
        return Book2.class;
    }

    @Path("/booksubresourceobject/{bookId}/")
    public Object getBookSubResourceObject(@PathParam("bookId") String id) throws BookNotFoundFault {
        return getBookSubResource(id);
    }

    @GET
    @Path("/booknames/{bookId}/")
    @Produces("text/*")
    public String getBookName(@PathParam("bookId") int id) throws BookNotFoundFault {
        Book book = books.get(Long.valueOf(id));
        if (book != null) {
            return book.getName();
        }
        BookNotFoundDetails details = new BookNotFoundDetails();
        details.setId(id);
        throw new BookNotFoundFault(details);
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
        if (!(ct1.startsWith("application/xml") && ct2.startsWith("application/xml")
            && ct3.startsWith("application/xml"))) {
            throw new RuntimeException("Unexpected content type");
        }

        book.setId(bookId + 1);
        URI uri = ui.getRequestUriBuilder().path(Long.toString(book.getId())).build();
        return Response.ok(book).location(uri).build();
    }

    @POST
    @Path("/books2")
    @Produces("text/xml")
    @Consumes("application/xml")
    public Book addBook2(Book book) {
        return new Book("Book echo", book.getId() + 1);
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
    @Path("/no-content")
    public void noContent() {
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
            r = Response.ok(book).build();
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
            r = Response.ok(newBook).build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }

    @PUT
    @Path("/bookswithdom/")
    public DOMSource updateBook(DOMSource ds) {
        //XMLUtils.printDOM(ds.getNode());
        return ds;
    }

    @PUT
    @Path("/bookswithjson/")
    @Consumes("application/json")
    public Response updateBookJSON(Book book) {
        Book b = books.get(book.getId());

        Response r;
        if (b != null) {
            r = Response.ok(book).build();
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
        Book b = books.get(Long.valueOf(id));

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
        return Long.valueOf(theBookId);
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

    @Path("/customresponse")
    @GET
    @Produces("application/xml")
    public Response getCustomBook() {
        return new CustomResponse(
            Response.ok().entity(new Book("Book", 222L)).header("customresponse", "OK").build());
    }

    @POST
    @Path("/booksecho2")
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response echoBookNameAndHeader2(String name) {
        return echoBookNameAndHeader(httpHeaders.getRequestHeader("CustomHeader").get(0), name);
    }

    @POST
    @Path("/booksecho3")
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response echoBookNameAndHeader3(String name) {
        return echoBookNameAndHeader(httpHeaders.getRequestHeader("customheader").get(0), name);
    }

    @POST
    @Path("/booksecho202")
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response echoBookName202(String name) {
        return Response.accepted(name).build();
    }


    @POST
    @Path("/empty202")
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response empty202(String name) {
        return Response.accepted().build();
    }


    @GET
    @Path("/cd/{CDId}/")
    public CD getCD() {
        return cds.get(Long.parseLong(currentCdId));
    }

    @GET
    @Path("/cdwithmultitypes/{CDId}/")
    @Produces({"application/xml", "application/bar+xml", "application/json" })
    public CD getCDWithMultiContentTypes(@PathParam("CDId") String id) {
        return cds.get(Long.parseLong(id));
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
                header("SomeHeader2", "\"and backslash\\\"").
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

    @POST
    @Path("/entityecho")
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response echoEntity(String entity) {
        return Response.ok().entity(entity).build();
    }
    
    @GET
    @Path("/queryParamSpecialCharacters")
    @Produces("text/plain")
    @SuppressWarnings({"checkstyle:linelength"})
    public Response queryParamSpecialCharacters(@QueryParam("/?abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~%1A!$'()*+,;:@") String queryParm1) {
        return Response
            .ok(queryParm1)
            .type(MediaType.TEXT_PLAIN)
            .build();
    }

    @GET
    @Path("/annotated/{bookId}/")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getGenericBookDate(@PathParam("bookId") String id) {
        @Provider
        @Consumes
        class AnnotatedClass {
        }
        
        return Response.ok().entity(new GregorianCalendar(2020, 00, 01),
            AnnotatedClass.class.getAnnotations()).build();
    }
    
    @GET
    @Path("/headers")
    @Produces("application/json")
    public Map<String, List<String>> getHeaders() {
        return httpHeaders.getRequestHeaders();
    }
    
    public final String init() {
        books.clear();
        cds.clear();
        bookId = 123;
        cdId = 123;

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
        return "OK";
    }

    @XmlJavaTypeAdapter(BookInfoAdapter2.class)
    interface BookInfoInterface {
        String getName();

        long getId();
    }

    static class BookInfo {
        private String name;
        private long id;

        BookInfo() {

        }

        BookInfo(Book b) {
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
        BookInfo2() {

        }

        BookInfo2(Book b) {
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
        BadBook(String s) {
            throw new RuntimeException("The bad book");
        }
    }

    private static class StreamingOutputImpl implements StreamingOutput {

        private boolean failEarly;

        StreamingOutputImpl(boolean failEarly) {
            this.failEarly = failEarly;
        }

        public void write(OutputStream output) throws IOException, WebApplicationException {
            if (failEarly) {
                throw new WebApplicationException(
                     Response.status(410).header("content-type", "text/plain")
                     .entity("This is supposed to go on the wire").build());
            }
            output.write("This is not supposed to go on the wire".getBytes());
            throw new WebApplicationException(410);
        }

    }
    private final class ResponseStreamingOutputImpl implements StreamingOutput {
        public void write(OutputStream output) throws IOException, WebApplicationException {
            if (!"text/plain".equals(BookStore.this.messageContext.get("Content-Type"))) {
                throw new RuntimeException();
            }
            BookStore.this.messageContext.put(Message.RESPONSE_CODE, 503);
            MultivaluedMap<String, String> headers = new MetadataMap<>();
            headers.putSingle("Content-Type", "text/custom+plain");
            headers.putSingle("CustomHeader", "CustomValue");
            BookStore.this.messageContext.put(Message.PROTOCOL_HEADERS, headers);

            output.write("Response is not available".getBytes());
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

    public abstract static class AbstractBookBean {
        @QueryParam("id_2")
        private long id2;
        public long getId2() {
            return id2;
        }

        public void setId2(long id2) {
            this.id2 = id2;
        }
    }

    public static class BookBean extends AbstractBookBean {
        private long id;


        private long id3;
        private BookBeanNested nested;

        public long getId() {
            return id;
        }

        @PathParam("id")
        public void setId(long id) {
            this.id = id;
        }

        @Context
        public void setUriInfo(UriInfo ui) {
            String id3Value = ui.getQueryParameters().getFirst("id3");
            if (id3Value != null) {
                this.id3 = Long.valueOf(id3Value);
            }
        }

        public long getId3() {
            return id3;
        }

        public BookBeanNested getNested() {
            return nested;
        }

        @BeanParam
        public void setNested(BookBeanNested nested) {
            this.nested = nested;
        }


    }

    public static class BookBeanExtended extends BookBean {

    }

    public static class BookBeanNested {
        private long id4;

        public long getId4() {
            return id4;
        }

        @QueryParam("id4")
        public void setId4(long id4) {
            this.id4 = id4;
        }
    }

    public static class BookBeanForm {
        private long id;
        private long id2;
        private long id3;

        public long getId() {
            return id;
        }

        @PathParam("id")
        public void setId(long id) {
            this.id = id;
        }

        @QueryParam("id2")
        public void setId2(long id2) {
            this.id2 = id2;
        }

        public long getId2() {
            return id2;
        }

        @FormParam("id3")
        public void setId3(long id3) {
            this.id3 = id3;
        }

        public long getId3() {
            return id3;
        }
    }

    public static class BookBean2 {
        private long id;
        @QueryParam("id_2")
        private long mId2;
        private long id3;
        public long getId() {
            return id;
        }

        @PathParam("id")
        public void setId(long id) {
            this.id = id;
        }

        public long getId2() {
            return mId2;
        }

        public void setId2(long id2) {
            this.mId2 = id2;
        }

        @Context
        public void setUriInfo(UriInfo ui) {
            String id3Value = ui.getQueryParameters().getFirst("id3");
            if (id3Value != null) {
                this.id3 = Long.valueOf(id3Value);
            }
        }

        public long getId3() {
            return id3;
        }
    }

    public static class BookNotReturnedException extends RuntimeException {

        private static final long serialVersionUID = 4935423670510083220L;

        public BookNotReturnedException(String errorMessage) {
            super(errorMessage);
        }

    }

    private Annotation[] getExtraAnnotations() {
        try {
            Method m = BookBean.class.getMethod("setUriInfo", new Class[]{UriInfo.class});
            return m.getAnnotations();
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static class StringArrayBodyReaderWriter
        implements MessageBodyReader<String[]>, MessageBodyWriter<String[]> {
        public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return String[].class.isAssignableFrom(arg0);
        }

        public String[] readFrom(Class<String[]> arg0, Type arg1,
            Annotation[] arg2, MediaType arg3, MultivaluedMap<String, String> arg4, InputStream arg5)
            throws IOException, WebApplicationException {
            return new String[] {IOUtils.readStringFromStream(arg5)};
        }

        public long getSize(String[] arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
            return -1;
        }

        public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return String[].class.isAssignableFrom(arg0);
        }

        public void writeTo(String[] arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
                            MultivaluedMap<String, Object> arg5, OutputStream arg6) throws IOException,
            WebApplicationException {
            arg6.write(arg0[0].getBytes());
        }

    }

    public static class StringListBodyReaderWriter
        implements MessageBodyReader<List<String>>, MessageBodyWriter<List<String>> {
        public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return List.class.isAssignableFrom(arg0)
                && String.class == InjectionUtils.getActualType(arg1);
        }

        public List<String> readFrom(Class<List<String>> arg0, Type arg1,
            Annotation[] arg2, MediaType arg3, MultivaluedMap<String, String> arg4, InputStream arg5)
            throws IOException, WebApplicationException {
            return Collections.singletonList(IOUtils.readStringFromStream(arg5));
        }

        public long getSize(List<String> arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
            return -1;
        }

        public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return List.class.isAssignableFrom(arg0)
                && String.class == InjectionUtils.getActualType(arg1);
        }

        public void writeTo(List<String> arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
                            MultivaluedMap<String, Object> arg5, OutputStream arg6) throws IOException,
            WebApplicationException {
            arg6.write(arg0.get(0).getBytes());
        }

    }

    public static class PrimitiveIntArrayReaderWriter
        implements MessageBodyReader<int[]>, MessageBodyWriter<int[]> {
        public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return int[].class.isAssignableFrom(arg0);
        }

        public int[] readFrom(Class<int[]> arg0, Type arg1,
            Annotation[] arg2, MediaType arg3, MultivaluedMap<String, String> arg4, InputStream arg5)
            throws IOException, WebApplicationException {
            String[] stringArr = IOUtils.readStringFromStream(arg5).split(",");
            int[] intArr = new int[stringArr.length];
            for (int i = 0; i < stringArr.length; i++) {
                intArr[i] = Integer.valueOf(stringArr[i]);
            }
            return intArr;

        }

        public long getSize(int[] arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
            return -1;
        }

        public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return int[].class.isAssignableFrom(arg0);
        }

        public void writeTo(int[] arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
                            MultivaluedMap<String, Object> arg5, OutputStream arg6) throws IOException,
            WebApplicationException {

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arg0.length; i++) {
                sb.append(Integer.toString(arg0[i]));
                if (i + 1 < arg0.length) {
                    sb.append(',');
                }
            }
            arg6.write(sb.toString().getBytes());
        }

    }
    public static class PrimitiveDoubleArrayReaderWriter
        implements MessageBodyReader<Object>, MessageBodyWriter<Object> {
        public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return double[].class.isAssignableFrom(arg0);
        }

        public Object readFrom(Class<Object> arg0, Type arg1,
            Annotation[] arg2, MediaType arg3, MultivaluedMap<String, String> arg4, InputStream arg5)
            throws IOException, WebApplicationException {
            String[] stringArr = IOUtils.readStringFromStream(arg5).split(",");
            double[] intArr = new double[stringArr.length];
            for (int i = 0; i < stringArr.length; i++) {
                intArr[i] = Double.valueOf(stringArr[i]);
            }
            return intArr;

        }

        public long getSize(Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
            return -1;
        }

        public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return double[].class.isAssignableFrom(arg0);
        }

        public void writeTo(Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
                            MultivaluedMap<String, Object> arg5, OutputStream arg6) throws IOException,
            WebApplicationException {

            double[] arr = (double[])arg0;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length; i++) {
                sb.append(Double.toString(arr[i]));
                if (i + 1 < arr.length) {
                    sb.append(',');
                }
            }
            arg6.write(sb.toString().getBytes());
        }

    }
    public static class BookStoreSub {
        BookStore bookStore;
        public BookStoreSub(BookStore bookStore) {
            this.bookStore = bookStore;
        }
        @GET
        @Path("/beanparam/{id}")
        @Produces("application/xml")
        public Book getBeanParamBook(@BeanParam BookBeanExtended bean) {
            return bookStore.getBeanParamBook(bean);
        }
    }

    public static class BookStoreQuerySub {
        @GET
        @Path("/listofstrings")
        @Produces("text/xml")
        public Book getBookFromListStrings(@QueryParam("value") List<String> value) {
            final StringBuilder builder = new StringBuilder();

            for (String v : value) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }

                builder.append(v);
            }

            return new Book(builder.toString(), 0L);
        }
    }

    public abstract static class AbstractBookId {
        public static BookId fromString(String id) {
            return BookId.of(UUID.fromString(id));
        }
    }

    public static final class BookId extends AbstractBookId {
        private final UUID uuid;

        private BookId(UUID uuid) {
            this.uuid = uuid;
        }

        public long getId() {
            return uuid.getMostSignificantBits();
        }

        public static BookId of(UUID uuid) {
            return new BookId(uuid);
        }
    }
}


