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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.xml.namespace.QName;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.ResponseProcessingException;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Variant;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.client.cache.CacheControlFeature;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.XSLTJaxbProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.systest.jaxrs.BookStore.BookInfo;
import org.apache.cxf.systest.jaxrs.BookStore.BookInfoInterface;
import org.apache.cxf.systest.jaxrs.BookStore.BookNotReturnedException;
import org.apache.cxf.systest.jaxrs.CustomFaultInInterceptor.CustomRuntimeException;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

//CHECKSTYLE.OFF:JavaNCSS
public class JAXRSClientServerBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServer.PORT;
    public static final String PORT2 = allocatePort(JAXRSClientServerBookTest.class);

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(BookServer.class, true));
        final Bus bus = createStaticBus();
        // Make sure default JSON-P/JSON-B providers are not loaded
        bus.setProperty(ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);
    }

    @Test
    public void testRetrieveBookCustomMethodReflection() throws Exception {
        try {
            doRetrieveBook(false);
            fail("HTTPUrlConnection does not support custom verbs without the reflection");
        } catch (ProcessingException ex) {
            // continue
        }
        Book book = doRetrieveBook(true);
        assertEquals("Retrieve", book.getName());
    }

    private Book doRetrieveBook(boolean useReflection) {
        String address = "http://localhost:" + PORT + "/bookstore/retrieve";
        WebClient wc = WebClient.create(address);
        wc.type("application/xml").accept("application/xml");
        WebClient.getConfig(wc).getRequestContext().put("force.urlconnection.http.conduit", true);
        if (!useReflection) {
            WebClient.getConfig(wc).getRequestContext().put("use.httpurlconnection.method.reflection", false);
        }
        // CXF RS Client code will set this property to true if the http verb is unknown
        // and this property is not already set. The async conduit is loaded in the tests module
        // but we do want to test HTTPUrlConnection reflection hence we set this property to false
        WebClient.getConfig(wc).getRequestContext().put("use.async.http.conduit", false);
        WebClient.getConfig(wc).getRequestContext().put("response.stream.auto.close", true);
        return wc.invoke("RETRIEVE", new Book("Retrieve", 123L), Book.class);
    }

    @Test
    public void testBlockAndThrowException() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/blockAndThrowException";
        WebClient wc = WebClient.create(address);
        Response r = wc.get();
        assertEquals(500, r.getStatus());
    }
    @Test
    public void testUpdateBookWithProxy() throws Exception {
        String address = "http://localhost:" + PORT;
        BookStore store = JAXRSClientFactory.create(address, BookStore.class);
        Book b = store.updateEchoBook(new Book("CXF", 125L));
        assertEquals(125L, b.getId());
    }
    @Test
    public void testEchoXmlBookQuery() throws Exception {
        String address = "http://localhost:" + PORT;
        BookStore store = JAXRSClientFactory.create(address, BookStore.class,
            Collections.singletonList(new BookServer.ParamConverterImpl()));
        Book b = store.echoXmlBookQuery(new Book("query", 125L), (byte)125);
        assertEquals(125L, b.getId());
        assertEquals("query", b.getName());
    }

    @Test
    public void testGetBookRoot() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/;JSESSIONID=xxx";
        WebClient wc = WebClient.create(address);
        Book book = wc.get(Book.class);
        assertEquals(124L, book.getId());
        assertEquals("root", book.getName());
    }
    @Test
    public void testGetBookUntypedStreamingResponse() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/streamingresponse";
        WebClient wc = WebClient.create(address);
        Book book = wc.get(Book.class);
        assertEquals(124L, book.getId());
        assertEquals("stream", book.getName());
    }
    @Test
    public void testNonExistent() throws Exception {
        String address = "http://168.168.168.168/bookstore";
        WebClient wc = WebClient.create(address,
                                        Collections.singletonList(new BookServer.TestResponseFilter()));
        try {
            wc.get();
            fail();
        } catch (ProcessingException ex) {
            assertTrue(ex.getCause() instanceof IOException);
        }
    }
    @Test
    public void testGetBookQueryGZIP() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/";
        WebClient wc = WebClient.create(address);
        wc.acceptEncoding("gzip,deflate");
        wc.encoding("gzip");
        InputStream r = wc.get(InputStream.class);
        assertNotNull(r);
        GZIPInputStream in = new GZIPInputStream(r);
        String s = IOUtils.toString(in);
        in.close();
        assertTrue(s, s.contains("id>124"));
    }

    @Test
    public void testGetBookQueryDefault() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/query/default";
        WebClient wc = WebClient.create(address);
        Response r = wc.get();
        Book book = r.readEntity(Book.class);
        assertEquals(123L, book.getId());
    }

    @Test
    public void testGetBookAcceptWildcard() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/wildcard";
        WebClient wc = WebClient.create(address);
        Response r = wc.accept("text/*").get();
        assertEquals(406, r.getStatus());
    }

    @Test
    public void testGetBookSameUriAutoRedirect() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/redirect?sameuri=true";
        WebClient wc = WebClient.create(address);
        WebClient.getConfig(wc).getHttpConduit().getClient().setAutoRedirect(true);
        WebClient.getConfig(wc).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        //WebClient.getConfig(wc).getRequestContext().put("force.urlconnection.http.conduit", true);
        Response r = wc.get();
        Book book = r.readEntity(Book.class);
        assertEquals(123L, book.getId());
        String requestUri = r.getStringHeaders().getFirst("RequestURI");
        assertEquals("http://localhost:" + PORT + "/bookstore/redirect?redirect=true", requestUri);
        String theCookie = r.getStringHeaders().getFirst("TheCookie");
        assertEquals("b", theCookie);
    }

    @Test
    public void testGetBookDiffUriAutoRedirect() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/redirect?sameuri=false";
        WebClient wc = WebClient.create(address);
        WebClient.getConfig(wc).getRequestContext().put("http.redirect.same.host.only", "true");
        WebClient.getConfig(wc).getHttpConduit().getClient().setAutoRedirect(true);
        try {
            wc.get();
            fail("Redirect to different host is not allowed");
        } catch (ProcessingException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause.getMessage().contains("Different HTTP Scheme or Host Redirect detected on"));
        }
    }

    @Test
    public void testGetBookRelativeUriAutoRedirect() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/redirect/relative?loop=false";
        WebClient wc = WebClient.create(address);
        assertEquals(address, wc.getCurrentURI().toString());
        WebClient.getConfig(wc).getRequestContext().put("http.redirect.relative.uri", "true");
        WebClient.getConfig(wc).getHttpConduit().getClient().setAutoRedirect(true);
        Response r = wc.get();
        Book book = r.readEntity(Book.class);
        assertEquals(124L, book.getId());

        String newAddress = "http://localhost:" + PORT + "/bookstore/redirect/relative?redirect=true";
        assertEquals(newAddress, wc.getCurrentURI().toString());
    }

    @Test
    public void testGetBookRelativeUriAutoRedirectLoop() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/redirect/relative?loop=true";
        WebClient wc = WebClient.create(address);
        WebClient.getConfig(wc).getRequestContext().put("http.redirect.relative.uri", "true");
        WebClient.getConfig(wc).getHttpConduit().getClient().setAutoRedirect(true);
        try {
            wc.get();
            fail("Redirect loop must be detected");
        } catch (ProcessingException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause.getMessage().contains("Redirect loop detected on"));
        }
    }

    @Test
    public void testGetBookRelativeUriAutoRedirectNotAllowed() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/redirect/relative?loop=true";
        WebClient wc = WebClient.create(address);
        WebClient.getConfig(wc).getHttpConduit().getClient().setAutoRedirect(true);
        try {
            wc.get();
            fail("relative Redirect is not allowed");
        } catch (ProcessingException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause.getMessage().contains("Relative Redirect detected on"));
        }
    }

    @Test
    public void testPostEmptyForm() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/emptyform";
        WebClient wc = WebClient.create(address);
        Response r = wc.form(new Form());
        assertEquals("empty form", r.readEntity(String.class));
    }
    
    @Test
    public void testEchoForm() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/form";
        WebClient wc = WebClient.create(address, Collections.singletonList(new LoggingFeature()));
        Form formOut = new Form().param("a", "aValue").param("b", "b value")
            .param("c%", "cValue");
        Form formIn = wc.post(formOut, Form.class);
        assertEquals(3, formIn.asMap().size());
        assertEquals("aValue", formIn.asMap().getFirst("a"));
        assertEquals("b value", formIn.asMap().getFirst("b"));
        assertEquals("cValue", formIn.asMap().getFirst("c%"));
    }

    @Test
    public void testPostEmptyFormAsInStream() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/emptyform";
        WebClient wc = WebClient.create(address);
        WebClient.getConfig(wc).getRequestContext().put("org.apache.cxf.empty.request", true);
        wc.type(MediaType.APPLICATION_FORM_URLENCODED);
        Response r = wc.post(new ByteArrayInputStream("".getBytes()));
        assertEquals("empty form", r.readEntity(String.class));
    }

    @Test
    public void testGetBookDescriptionHttpResponse() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/httpresponse";
        WebClient wc = WebClient.create(address);
        WebClient.getConfig(wc).getInInterceptors().add(new LoggingInInterceptor());
        Response r = wc.get();
        assertEquals("text/plain", r.getMediaType().toString());
        assertEquals("Good Book", r.readEntity(String.class));
    }

    @Test
    public void testGetCustomBookResponse() {
        String address = "http://localhost:" + PORT + "/bookstore/customresponse";
        WebClient wc = WebClient.create(address);
        Response r = wc.accept("application/xml").get(Response.class);
        Book book = r.readEntity(Book.class);
        assertEquals(222L, book.getId());
        assertEquals("OK", r.getHeaderString("customresponse"));
    }

    @Test
    public void testGetCustomBookBufferedResponse() {
        String address = "http://localhost:" + PORT + "/bookstore/customresponse";
        WebClient wc = WebClient.create(address);
        Response r = wc.accept("application/xml").get(Response.class);
        r.bufferEntity();

        String bookStr = r.readEntity(String.class);
        assertTrue(bookStr.endsWith("</Book>"));

        Book book = r.readEntity(Book.class);
        assertEquals(222L, book.getId());
        assertEquals("OK", r.getHeaderString("customresponse"));
    }

    @Test
    public void testGetCustomBookText() {
        String address = "http://localhost:" + PORT + "/bookstore/customtext";
        WebClient wc = WebClient.create(address);
        Response r = wc.accept("text/custom").get();
        String name = r.readEntity(String.class);
        assertEquals("Good book", name);
        assertEquals("text/custom;charset=us-ascii", r.getMediaType().toString());
        assertEquals("CustomValue", r.getHeaderString("CustomHeader"));
    }

    @Test
    public void testGetBookNameAsByteArray() {
        String address = "http://localhost:" + PORT + "/bookstore/booknames/123";
        WebClient wc = WebClient.create(address);

        Response r = wc.accept("application/bar").get();
        String name = r.readEntity(String.class);
        assertEquals("CXF in Action", name);
        String lengthStr = r.getHeaderString(HttpHeaders.CONTENT_LENGTH);
        assertNotNull(lengthStr);
        long length = Long.valueOf(lengthStr);
        assertEquals(name.length(), length);
    }

    @Test
    public void testGetChapterFromSelectedBook() {
        String address = "http://localhost:" + PORT + "/bookstore/books/id=le=123/chapter/1";
        doTestGetChapterFromSelectedBook(address);
    }

    @Test
    public void testUseMapperOnBus() {
        String address = "http://localhost:" + PORT + "/bookstore/mapperonbus";
        WebClient wc = WebClient.create(address);
        Response r = wc.post(null);
        assertEquals(500, r.getStatus());
        MediaType mt = r.getMediaType();
        assertEquals("text/plain;charset=utf-8", mt.toString().toLowerCase());
        assertEquals("the-mapper", r.getHeaderString("BusMapper"));
        assertEquals("BusMapperException", r.readEntity(String.class));
    }

    @Test
    public void testUseParamBeanWebClient() {
        String address = "http://localhost:" + PORT + "/bookstore/beanparam";
        doTestUseParamBeanWebClient(address);
    }

    @Test
    public void testUseParamBeanWebClientSubResource() {
        String address = "http://localhost:" + PORT + "/bookstore/beanparamsub/beanparam";
        doTestUseParamBeanWebClient(address);
    }

    @Test
    public void testUseParamBeanWebClient2() {
        String address = "http://localhost:" + PORT + "/bookstore/beanparam2";
        doTestUseParamBeanWebClient(address);
    }

    private void doTestUseParamBeanWebClient(String address) {
        WebClient wc = WebClient.create(address);
        wc.path("100").query("id_2", "20").query("id3", "3").query("id4", "123");
        Book book = wc.get(Book.class);
        assertEquals(123L, book.getId());
    }

    @Test
    public void testGetIntroChapterFromSelectedBook() {
        String address = "http://localhost:" + PORT + "/bookstore/books(id=le=123)/chapter";
        doTestGetChapterFromSelectedBook(address);
    }

    @Test
    public void testGetIntroChapterFromSelectedBook2() {
        String address = "http://localhost:" + PORT + "/bookstore/";
        WebClient wc = WebClient.create(address);
        wc.path("books[id=le=123]").path("chapter");
        wc.accept("application/xml");
        Chapter chapter = wc.get(Chapter.class);
        assertEquals("chapter 1", chapter.getTitle());
    }

    private void doTestGetChapterFromSelectedBook(String address) {
        WebClient wc = WebClient.create(address);
        wc.accept("application/xml");
        Chapter chapter = wc.get(Chapter.class);
        assertEquals("chapter 1", chapter.getTitle());
    }

    @Test
    public void testWithComplexPath() {
        WebClient wc =
            WebClient.create("http://localhost:" + PORT + "/bookstore/allCharsButA-B/:@!$&'()*+,;=-._~");
        wc.accept("application/xml");
        Book book = wc.get(Book.class);
        assertEquals("Encoded Path", book.getName());
    }

    @Test
    public void testMalformedAcceptType() {
        WebClient wc =
            WebClient.create("http://localhost:" + PORT + "/bookstore/books/123");
        wc.accept("application");
        Response r = wc.get();
        assertEquals(406, r.getStatus());
    }

    @Test
    public void testProxyWrongAddress() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT2 + "/wrongaddress", BookStore.class);
        try {
            store.getBook("123");
            fail("ClientException expected");
        } catch (ProcessingException ex) {
            // expected
        }
    }

    @Test
    public void testProxyBeanParam() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        BookStore.BookBean bean = new BookStore.BookBean();
        bean.setId(100L);
        bean.setId2(23L);
        BookStore.BookBeanNested nested = new BookStore.BookBeanNested();
        nested.setId4(123);
        bean.setNested(nested);

        Book book = store.getBeanParamBook(bean);
        assertEquals(123L, book.getId());
    }
    
    @Test
    public void testProxyBeanParam2() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        BookStore.BookBean2 bean = new BookStore.BookBean2();
        bean.setId(100L);
        bean.setId2(23L);
        BookStore.BookBeanNested nested = new BookStore.BookBeanNested();
        nested.setId4(123);
        Book book = store.getTwoBeanParamsBook(bean, nested);
        assertEquals(123L, book.getId());
    }
    
    @Test
    public void testProxyBeanPostFormParam() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        BookStore.BookBeanForm bean = new BookStore.BookBeanForm();
        bean.setId(100L);
        bean.setId2(23L);
        bean.setId3(123);
        Book book = store.postFormBeanParamsBook(bean);
        assertEquals(123L, book.getId());
    }
    
    @Test
    public void testProxyBeanGetFormParam() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        BookStore.BookBeanForm bean = new BookStore.BookBeanForm();
        bean.setId(100L);
        bean.setId2(23L);
        bean.setId3(123);
        Book book = store.getFormBeanParamsBook(bean);
        assertEquals(123L, book.getId());
    }

    @Test
    public void testProxyPostFormParam() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Book book = store.postFormParamsBook(100L, 23L, 123L);
        assertEquals(123L, book.getId());
    }

    @Test
    public void testProxyGetFormParam() throws Exception {
        
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Book book = store.getFormParamsBook(100L, 23L, 123L);
        assertEquals(123L, book.getId());
    }

    @Test
    public void testGetBookWithCustomHeader() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/books/123";
        WebClient wc = WebClient.create(endpointAddress);
        Book b = wc.get(Book.class);
        assertEquals(123L, b.getId());

        MultivaluedMap<String, Object> headers = wc.getResponse().getMetadata();
        assertEquals("123", headers.getFirst("BookId"));
        assertEquals(MultivaluedMap.class.getName(), headers.getFirst("MAP-NAME"));
        assertNotNull(headers.getFirst("Date"));

        wc.header("PLAIN-MAP", "true");
        b = wc.get(Book.class);
        assertEquals(123L, b.getId());

        headers = wc.getResponse().getMetadata();
        assertEquals("321", headers.getFirst("BookId"));
        assertEquals(Map.class.getName(), headers.getFirst("MAP-NAME"));
        assertNotNull(headers.getFirst("Date"));
    }

    @Test
    public void testGetBookWithNameInQuery() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/name-in-query";
        WebClient wc = WebClient.create(endpointAddress);
        String name = "Many        spaces";
        wc.query("name", name);
        Book b = wc.get(Book.class);
        assertEquals(name, b.getName());
    }

    @Test
    public void testGetBookAsObject() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/object";
        WebClient wc = WebClient.create(endpointAddress);
        Book b = wc.get(Book.class);
        assertEquals("Book as Object", b.getName());
    }

    @Test
    public void testProcessingInstruction() throws Exception {
        String base = "http://localhost:" + PORT;
        String endpointAddress = base + "/bookstore/name-in-query";
        WebClient wc = WebClient.create(endpointAddress);
        String name = "Many        spaces";
        wc.query("name", name);
        String content = wc.get(String.class);
        assertTrue(content.contains("<!DOCTYPE Something SYSTEM 'my.dtd'>"));
        assertTrue(content.contains("<?xmlstylesheet href='" + base + "/common.css'?>"));
        assertTrue(content.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""));
        assertTrue(content.contains("xsi:schemaLocation=\"" + base + "/book.xsd\""));
    }

    @Test
    public void testGetBookWithColonMarks() throws Exception {
        // URLEncoder will turn ":" into "%3A" but ':' is actually
        // not disallowed in the path components
        String endpointAddressUrlEncoded =
            "http://localhost:" + PORT + "/bookstore/books/colon/"
            + URLEncoder.encode("1:2:3", StandardCharsets.UTF_8.name());

        Response r = WebClient.create(endpointAddressUrlEncoded).get();
        assertEquals(404, r.getStatus());

        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/books/colon/1:2:3";
        WebClient wc = WebClient.create(endpointAddress);
        Book b = wc.get(Book.class);
        assertEquals(123L, b.getId());
    }

    @Test
    public void testPostAnd401WithText() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/post401";
        WebClient wc = WebClient.create(endpointAddress);
        WebClient.getConfig(wc).getHttpConduit().getClient().setAllowChunking(false);
        assertFalse(WebClient.getConfig(wc).getHttpConduit().getClient().isAllowChunking());

        Response r = wc.post(null);
        assertEquals(401, r.getStatus());
        assertEquals("This is 401", IOUtils.toString((InputStream)r.getEntity()));
    }

    @Test
    public void testCapturedServerInFault() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/infault";
        WebClient wc = WebClient.create(endpointAddress);
        Response r = wc.get();
        assertEquals(401, r.getStatus());
    }

    @Test
    public void testCapturedServerOutFault() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/outfault";
        WebClient wc = WebClient.create(endpointAddress);
        Response r = wc.get();
        assertEquals(403, r.getStatus());
    }

    @Test
    public void testGetCollectionOfBooks() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/collections";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml");
        Collection<? extends Book> collection = wc.getCollection(Book.class);
        assertEquals(1, collection.size());
        Book book = collection.iterator().next();
        assertEquals(123L, book.getId());
    }

    @Test
    public void testPostCollectionGetBooksWebClient() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/collections3";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml").type("application/xml");
        Book b1 = new Book("CXF in Action", 123L);
        Book b2 = new Book("CXF Rocks", 124L);
        List<Book> books = new ArrayList<>();
        books.add(b1);
        books.add(b2);
        Book book = wc.postCollection(books, Book.class, Book.class);
        assertEquals(200, wc.getResponse().getStatus());
        assertNotSame(b1, book);
        assertEquals(b1.getName(), book.getName());
    }

    @Test
    public void testPostCollectionGenericEntityWebClient() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/collections3";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml").type("application/xml");
        Book b1 = new Book("CXF in Action", 123L);
        Book b2 = new Book("CXF Rocks", 124L);
        List<Book> books = new ArrayList<>();
        books.add(b1);
        books.add(b2);
        GenericEntity<List<Book>> genericCollectionEntity =
            new GenericEntity<List<Book>>(books) {
            };

        Book book = wc.post(genericCollectionEntity, Book.class);
        assertEquals(200, wc.getResponse().getStatus());
        assertNotSame(b1, book);
        assertEquals(b1.getName(), book.getName());
    }

    @Test
    public void testPostGetCollectionGenericEntityAndType() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/collections";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml").type("application/xml");
        Book b1 = new Book("CXF in Action", 123L);
        Book b2 = new Book("CXF Rocks", 124L);
        List<Book> books = new ArrayList<>();
        books.add(b1);
        books.add(b2);

        GenericEntity<List<Book>> genericCollectionEntity =
            new GenericEntity<List<Book>>(books) {
            };
        GenericType<List<Book>> genericResponseType =
            new GenericType<List<Book>>() {
            };

        List<Book> books2 = wc.post(genericCollectionEntity, genericResponseType);
        assertNotNull(books2);
        assertNotSame(books, books2);
        assertEquals(2, books2.size());
        Book b11 = books.get(0);
        assertEquals(123L, b11.getId());
        assertEquals("CXF in Action", b11.getName());
        Book b22 = books.get(1);
        assertEquals(124L, b22.getId());
        assertEquals("CXF Rocks", b22.getName());
        assertEquals(200, wc.getResponse().getStatus());
    }

    @Test
    public void testPostCollectionOfBooksWebClient() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/collections";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml").type("application/xml");
        Book b1 = new Book("CXF in Action", 123L);
        Book b2 = new Book("CXF Rocks", 124L);
        List<Book> books = new ArrayList<>();
        books.add(b1);
        books.add(b2);
        List<Book> books2 = new ArrayList<>(wc.postAndGetCollection(books, Book.class, Book.class));
        assertNotNull(books2);
        assertNotSame(books, books2);
        assertEquals(2, books2.size());
        Book b11 = books.get(0);
        assertEquals(123L, b11.getId());
        assertEquals("CXF in Action", b11.getName());
        Book b22 = books.get(1);
        assertEquals(124L, b22.getId());
        assertEquals("CXF Rocks", b22.getName());
        assertEquals(200, wc.getResponse().getStatus());
    }

    @Test
    public void testPostNullGetEmptyCollectionProxy() throws Exception {
        String endpointAddress = "http://localhost:" + PORT;
        BookStore bs = JAXRSClientFactory.create(endpointAddress, BookStore.class);
        List<Book> books = bs.postBookGetCollection(null);
        assertNotNull(books);
        assertEquals(0, books.size());

    }

    @Test
    public void testPostObjectGetCollection() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/collectionBook";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml").type("application/xml");
        Book b1 = new Book("Book", 666L);
        List<Book> books = new ArrayList<>(wc.postObjectGetCollection(b1, Book.class));
        assertNotNull(books);
        assertEquals(1, books.size());
        Book b = books.get(0);
        assertEquals(666L, b.getId());
        assertEquals("Book", b.getName());
    }

    @Test
    public void testCaching() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/books/response/123";

        // Add the CacheControlFeature to cache books returned by the service on the client side
        try (CacheControlFeature cacheControlFeature = new CacheControlFeature()) {
            cacheControlFeature.setCacheResponseInputStream(true);
            Client client = ClientBuilder.newBuilder()
                .register(cacheControlFeature)
                .build();
            WebTarget target = client.target(endpointAddress);

            // First call
            Response response = target.request().get();
            assertEquals(200, response.getStatus());
            Book book = response.readEntity(Book.class);
            assertEquals(123L, book.getId());

            MultivaluedMap<String, Object> headers = response.getMetadata();
            assertFalse(headers.isEmpty());
            Object etag = headers.getFirst("ETag");
            assertNotNull(etag);
            assertTrue(etag.toString().startsWith("\""));
            assertTrue(etag.toString().endsWith("\""));

            Object cacheControl = headers.getFirst("Cache-Control");
            assertNotNull(cacheControl);
            assertTrue(cacheControl.toString().contains("private"));
            assertTrue(cacheControl.toString().contains("max-age=100000"));

            // Now make a second call. This should be retrieved from the client's cache
            response = target.request().get();
            assertEquals(200, response.getStatus());
            book = response.readEntity(Book.class);
            assertEquals(123L, book.getId());
        }
    }

    @Test
    public void testCachingExpires() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/books/response2/123";

        // Add the CacheControlFeature to cache books returned by the service on the client side
        try (CacheControlFeature cacheControlFeature = new CacheControlFeature()) {
            cacheControlFeature.setCacheResponseInputStream(true);
            Client client = ClientBuilder.newBuilder()
                .register(cacheControlFeature)
                .build();
            WebTarget target = client.target(endpointAddress);

            // First call
            Response response = target.request().get();
            assertEquals(200, response.getStatus());
            Book book = response.readEntity(Book.class);
            assertEquals(123L, book.getId());

            MultivaluedMap<String, Object> headers = response.getMetadata();
            assertFalse(headers.isEmpty());
            Object etag = headers.getFirst("ETag");
            assertNotNull(etag);
            assertTrue(etag.toString().startsWith("\""));
            assertTrue(etag.toString().endsWith("\""));

            Object cacheControl = headers.getFirst("Cache-Control");
            assertNotNull(cacheControl);
            assertTrue(cacheControl.toString().contains("private"));
            assertTrue(cacheControl.toString().contains("max-age=1"));

            // Now make a second call. The value in the cache will have expired, so
            // it should call the service again
            Thread.sleep(1500L);
            response = target.request().get();
            assertEquals(200, response.getStatus());
            book = response.readEntity(Book.class);
            assertEquals(123L, book.getId());
        }
    }

    @Test
    public void testCachingExpiresUsingETag() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/books/response3/123";

        // Add the CacheControlFeature to cache books returned by the service on the client side
        try (CacheControlFeature cacheControlFeature = new CacheControlFeature()) {
            cacheControlFeature.setCacheResponseInputStream(true);
            Client client = ClientBuilder.newBuilder()
                .register(cacheControlFeature)
                .build();
            WebTarget target = client.target(endpointAddress);

            // First call
            Response response = target.request().get();
            assertEquals(200, response.getStatus());
            response.bufferEntity();
            Book book = response.readEntity(Book.class);
            assertEquals(123L, book.getId());

            MultivaluedMap<String, Object> headers = response.getMetadata();
            assertFalse(headers.isEmpty());
            Object etag = headers.getFirst("ETag");
            assertNotNull(etag);
            assertTrue(etag.toString().startsWith("\""));
            assertTrue(etag.toString().endsWith("\""));

            Object cacheControl = headers.getFirst("Cache-Control");
            assertNotNull(cacheControl);
            assertTrue(cacheControl.toString().contains("private"));
            assertTrue(cacheControl.toString().contains("max-age=1"));

            // Now make a second call. The value in the clients cache will have expired, so it should call
            // out to the service, which will return 304, and the client will re-use the cached payload
            Thread.sleep(1500L);
            Response response2 = target.request().get();
            assertEquals(304, response2.getStatus());
            assertFalse(response2.hasEntity());
            Book book2 = response.readEntity(Book.class);
            assertEquals(123L, book2.getId());
        }
    }

    @Test
    public void testOnewayWebClient() throws Exception {
        WebClient client = WebClient.create("http://localhost:" + PORT + "/bookstore/oneway");
        Response r = client.header("OnewayRequest", "true").post(null);
        assertEquals(202, r.getStatus());
        assertFalse(r.getHeaders().isEmpty());
    }
    
    @Test
    public void testOnewayWebClientWithResponseFilter() throws Exception {
        final ClientResponseFilter filter = new TestClientResponseFilter();
        WebClient client = WebClient.create("http://localhost:" + PORT + "/bookstore/oneway", Arrays.asList(filter));
        Response r = client.header("OnewayRequest", "true").post(null);
        assertEquals(202, r.getStatus());
        assertFalse(r.getHeaders().isEmpty());
        assertThat(r.getHeaderString("X-Filter"), equalTo("true"));
    }

    @Test
    public void testOnewayWebClient2() throws Exception {
        WebClient client = WebClient.create("http://localhost:" + PORT + "/bookstore/oneway");
        Response r = client.post(null);
        assertEquals(202, r.getStatus());
        assertFalse(r.getHeaders().isEmpty());
    }

    @Test
    public void testBookWithSpace() throws Exception {
        WebClient client = WebClient.create("http://localhost:" + PORT + "/bookstore/").path("the books/123");
        Book book = client.get(Book.class);
        assertEquals(123L, book.getId());
    }

    @Test
    public void testBookWithSpaceProxy() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Book book = store.getBookWithSpace("123");
        assertEquals(123L, book.getId());
        assertEquals("CXF in Action", book.getName());
    }

    @Test
    public void testBookWithSpaceProxyPathUrlEncoded() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setServiceClass(BookStore.class);
        bean.setAddress("http://localhost:" + PORT);
        bean.setProperties(Collections.<String, Object>singletonMap("url.encode.client.parameters", Boolean.TRUE));
        BookStore store = bean.create(BookStore.class);
        Book book = store.getBookWithSemicolon("123;:", "custom;:header");
        assertEquals(123L, book.getId());
        assertEquals("CXF in Action%3B%3A", book.getName());
    }

    @Test
    public void testBookWithSpaceProxyPathUrlEncodedSemicolonOnly() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setServiceClass(BookStore.class);
        bean.setAddress("http://localhost:" + PORT);
        bean.getProperties(true).put("url.encode.client.parameters", "true");
        bean.getProperties(true).put("url.encode.client.parameters.list", ";");
        BookStore store = bean.create(BookStore.class);
        Book book = store.getBookWithSemicolon("123;:", "custom;:header");
        assertEquals(123L, book.getId());
        assertEquals("CXF in Action%3B:", book.getName());
    }

    @Test
    public void testBookWithSpaceProxyNonEncodedSemicolon() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Book book = store.getBookWithSemicolon("123;", "custom;:header");
        assertEquals(123L, book.getId());
        assertEquals("CXF in Action;", book.getName());
    }

    @Test
    public void testBookWithSpaceProxyWithBufferedStream() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        WebClient.getConfig(store).getResponseContext().put("buffer.proxy.response", "true");
        Book book = store.getBookWithSpace("123");
        assertEquals(123L, book.getId());
        assertTrue(WebClient.client(store).getResponse().readEntity(String.class).contains("<Book"));
    }

    @Test
    public void testBookWithMultipleExceptions() throws Exception {
        List<Object> providers = new LinkedList<>();
        providers.add(new BookServer.NotReturnedExceptionMapper());
        providers.add(new BookServer.NotFoundExceptionMapper());
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class, providers);
        try {
            store.getBookWithExceptions(true);
            fail();
        } catch (BookNotReturnedException ex) {
            assertEquals("notReturned", ex.getMessage());
        }
        try {
            store.getBookWithExceptions(false);
            fail();
        } catch (BookNotFoundFault ex) {
            assertEquals("notFound", ex.getMessage());
        }
    }

    @Test
    public void testBookWithExceptionsNoMapper() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        try {
            store.getBookWithExceptions(true);
            fail();
        } catch (WebApplicationException ex) {
            assertEquals("notReturned", ex.getResponse().getHeaderString("Status"));
        }
    }

    @Test
    public void testBookWithMultipleExceptions2() throws Exception {
        List<Object> providers = new LinkedList<>();
        providers.add(new BookServer.NotReturnedExceptionMapper());
        providers.add(BookServer.NotFoundExceptionMapper.class);
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class, providers);
        try {
            store.getBookWithExceptions2(true);
            fail();
        } catch (BookNotReturnedException ex) {
            assertEquals("notReturned", ex.getMessage());
        }
        try {
            store.getBookWithExceptions2(false);
            fail();
        } catch (BookNotFoundFault ex) {
            assertEquals("notFound", ex.getMessage());
        }
    }

    @Test
    public void testTempRedirectWebClient() throws Exception {
        WebClient client = WebClient.create("http://localhost:" + PORT + "/bookstore/tempredirect");
        Response r = client.type("*/*").get();
        assertEquals(307, r.getStatus());
        MultivaluedMap<String, Object> map = r.getMetadata();
        assertEquals("http://localhost:" + PORT + "/whatever/redirection?css1=http%3A//bar",
                     map.getFirst("Location").toString());
        List<Object> cookies = r.getMetadata().get("Set-Cookie");
        assertNotNull(cookies);
        assertEquals(2, cookies.size());
    }

    @Test
    public void testSetCookieWebClient() throws Exception {
        WebClient client = WebClient.create("http://localhost:" + PORT + "/bookstore/setcookies");
        Response r = client.type("*/*").get();
        assertEquals(200, r.getStatus());
        List<Object> cookies = r.getMetadata().get("Set-Cookie");
        assertNotNull(cookies);
        assertEquals(1, cookies.size());
    }

    @Test
    public void testSetManyCookiesWebClient() throws Exception {
        WebClient client = WebClient.create("http://localhost:" + PORT + "/bookstore/setmanycookies");
        Response r = client.type("*/*").get();
        assertEquals(200, r.getStatus());
        List<Object> cookies = r.getMetadata().get("Set-Cookie");
        assertNotNull(cookies);
        assertEquals(3, cookies.size());

        boolean hasDummy1 = false;
        boolean hasDummy2 = false;
        boolean hasJSESSION = false;

        for (Object o : cookies) {
            String c = o.toString();
            hasDummy1 |= c.contains("=dummy;");
            hasDummy2 |= c.contains("=dummy2;");
            hasJSESSION |= c.contains("JSESSIONID");
        }
        assertTrue("Did not contain JSESSIONID", hasJSESSION);
        assertTrue("Did not contain dummy", hasDummy1);
        assertTrue("Did not contain dummy2", hasDummy2);
    }

    @Test
    public void testOnewayProxy() throws Exception {
        BookStore proxy = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        proxy.onewayRequest();
        assertEquals(202, WebClient.client(proxy).getResponse().getStatus());
    }

    @Test
    public void testProxyWithCollectionMatrixParams() throws Exception {
        BookStore proxy = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        List<String> params = new ArrayList<>();
        params.add("12");
        params.add("3");
        Book book = proxy.getBookByMatrixListParams(params);
        assertEquals(123L, book.getId());
    }

    @Test
    public void testPropogateException() throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + PORT + "/bookstore/propagate-exception");
        get.addHeader("Accept", "application/xml");
        get.addHeader("Cookie", "a=b;c=d");
        get.addHeader("Cookie", "e=f");
        get.addHeader("Accept-Language", "da;q=0.8,en");
        get.addHeader("Book", "1,2,3");
        try {
            CloseableHttpResponse response = client.execute(get);
            assertEquals(500, response.getStatusLine().getStatusCode());
            String content = EntityUtils.toString(response.getEntity());
            if (!StringUtils.isEmpty(content)) {
                assertTrue(content, content.contains("Error") && content.contains("500"));
            }
        } finally {
            get.releaseConnection();
        }
    }

    @Test
    public void testPropogateException2() throws Exception {
        String data = "<ns1:XMLFault xmlns:ns1=\"http://cxf.apache.org/bindings/xformat\">"
            + "<ns1:faultstring xmlns:ns1=\"http://cxf.apache.org/bindings/xformat\">"
            + "org.apache.cxf.systest.jaxrs.BookNotFoundFault: Book Exception</ns1:faultstring>"
            + "</ns1:XMLFault>";
        getAndCompare("http://localhost:" + PORT + "/bookstore/propagate-exception2",
                      data, "application/xml", null, 500);
    }

    @Test
    public void testPropogateException3() throws Exception {
        String data = "<nobook/>";
        getAndCompare("http://localhost:" + PORT + "/bookstore/propagate-exception3",
                      data, "application/xml", null, 500);
    }

    @Test
    public void testPropogateException4() throws Exception {
        String data = "<nobook/>";
        getAndCompare("http://localhost:" + PORT + "/bookstore/propogateExceptionVar/1",
                      data, "application/xml", null, 500);
    }

    @Test
    public void testServerWebApplicationException() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/webappexception");
        wc.accept("application/xml");
        try {
            wc.get(Book.class);
            fail("Exception expected");
        } catch (ServerErrorException ex) {
            assertEquals(500, ex.getResponse().getStatus());
            assertEquals("This is a WebApplicationException", ex.getResponse().readEntity(String.class));
        }
    }

    @Test
    public void testServerWebApplicationExceptionResponse() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/webappexception");
        wc.accept("application/xml");
        try {
            Response r = wc.get(Response.class);
            assertEquals(500, r.getStatus());
        } catch (WebApplicationException ex) {
            fail("Unexpected exception");
        }
    }

    @Test
    public void testServerWebApplicationExceptionXML() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/webappexceptionXML");
        wc.accept("application/xml");
        try {
            wc.get(Book.class);
            fail("Exception expected");
        } catch (NotAcceptableException ex) {
            assertEquals(406, ex.getResponse().getStatus());
            Book exBook = ex.getResponse().readEntity(Book.class);
            assertEquals("Exception", exBook.getName());
            assertEquals(999L, exBook.getId());
        }
    }

    @Test
    public void testServerWebApplicationExceptionXMLWithProxy() throws Exception {
        BookStore proxy = JAXRSClientFactory.create("http://localhost:" + PORT,
                                                    BookStore.class);
        try {
            proxy.throwExceptionXML();
            fail("Exception expected");
        } catch (NotAcceptableException ex) {
            assertEquals(406, ex.getResponse().getStatus());
            Book exBook = ex.getResponse().readEntity(Book.class);
            assertEquals("Exception", exBook.getName());
            assertEquals(999L, exBook.getId());
        }
    }

    @Test
    public void testServerWebApplicationExceptionWithProxy() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        try {
            store.throwException();
            fail("Exception expected");
        } catch (ServerErrorException ex) {
            assertEquals(500, ex.getResponse().getStatus());
            assertEquals("This is a WebApplicationException", ex.getResponse().readEntity(String.class));
        }
    }

    @Test
    public void testServerWebApplicationExceptionWithProxy2() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        try {
            store.throwException();
            fail("Exception expected");
        } catch (WebApplicationException ex) {
            assertEquals(500, ex.getResponse().getStatus());
            assertEquals("This is a WebApplicationException", ex.getResponse().readEntity(String.class));
        }
    }

    @Test
    public void testWebApplicationException() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/webappexception",
                      "This is a WebApplicationException",
                      "application/xml", null, 500);
    }

    @Test
    public void testAddBookProxyResponse() {
        Book b = new Book("CXF rocks", 123L);
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Response r = store.addBook(b);
        assertNotNull(r);
        InputStream is = (InputStream)r.getEntity();
        assertNotNull(is);
        XMLSource source = new XMLSource(is);
        source.setBuffering();
        assertEquals(124L, Long.parseLong(source.getValue("Book/id")));
        assertEquals("CXF rocks", source.getValue("Book/name"));
    }

    @Test
    public void testGetBookCollection() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Book b1 = new Book("CXF in Action", 123L);
        Book b2 = new Book("CXF Rocks", 124L);
        List<Book> books = new ArrayList<>();
        books.add(b1);
        books.add(b2);
        List<Book> books2 = store.getBookCollection(books);
        assertNotNull(books2);
        assertNotSame(books, books2);
        assertEquals(2, books2.size());
        Book b11 = books2.get(0);
        assertEquals(123L, b11.getId());
        assertEquals("CXF in Action", b11.getName());
        Book b22 = books2.get(1);
        assertEquals(124L, b22.getId());
        assertEquals("CXF Rocks", b22.getName());
    }

    @Test
    public void testGetJAXBElementXmlRootBookCollection() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Book b1 = new Book("CXF in Action", 123L);
        Book b2 = new Book("CXF Rocks", 124L);
        List<JAXBElement<Book>> books = new ArrayList<>();
        books.add(new JAXBElement<Book>(new QName("bookRootElement"), Book.class, b1));
        books.add(new JAXBElement<Book>(new QName("bookRootElement"), Book.class, b2));
        List<JAXBElement<Book>> books2 = store.getJAXBElementBookXmlRootCollection(books);
        assertNotNull(books2);
        assertNotSame(books, books2);
        assertEquals(2, books2.size());
        Book b11 = books2.get(0).getValue();
        assertEquals(123L, b11.getId());
        assertEquals("CXF in Action", b11.getName());
        Book b22 = books2.get(1).getValue();
        assertEquals(124L, b22.getId());
        assertEquals("CXF Rocks", b22.getName());
    }
    @Test
    public void testGetJAXBElementXmlRootBookCollectionWebClient() throws Exception {
        WebClient store = WebClient.create("http://localhost:" + PORT + "/bookstore/jaxbelementxmlrootcollections");
        Book b1 = new Book("CXF in Action", 123L);
        Book b2 = new Book("CXF Rocks", 124L);
        List<Book> books = new ArrayList<>();
        books.add(b1);
        books.add(b2);
        store.type("application/xml").accept("application/xml");
        List<Book> books2 = new ArrayList<>(store.postAndGetCollection(books, Book.class, Book.class));
        assertNotNull(books2);
        assertNotSame(books, books2);
        assertEquals(2, books2.size());
        Book b11 = books2.get(0);
        assertEquals(123L, b11.getId());
        assertEquals("CXF in Action", b11.getName());
        Book b22 = books2.get(1);
        assertEquals(124L, b22.getId());
        assertEquals("CXF Rocks", b22.getName());
    }

    @Test
    public void testGetJAXBElementBookCollection() throws Exception {
        JAXBElementProvider<?> provider = new JAXBElementProvider<>();
        provider.setMarshallAsJaxbElement(true);
        provider.setUnmarshallAsJaxbElement(true);
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT,
                                                    BookStore.class,
                                                    Collections.singletonList(provider));
        BookNoXmlRootElement b1 = new BookNoXmlRootElement("CXF in Action", 123L);
        BookNoXmlRootElement b2 = new BookNoXmlRootElement("CXF Rocks", 124L);
        List<JAXBElement<BookNoXmlRootElement>> books = new ArrayList<>();
        books.add(new JAXBElement<BookNoXmlRootElement>(new QName("bookNoXmlRootElement"),
            BookNoXmlRootElement.class, b1));
        books.add(new JAXBElement<BookNoXmlRootElement>(new QName("bookNoXmlRootElement"),
            BookNoXmlRootElement.class, b2));
        List<JAXBElement<BookNoXmlRootElement>> books2 = store.getJAXBElementBookCollection(books);
        assertNotNull(books2);
        assertNotSame(books, books2);
        assertEquals(2, books2.size());
        BookNoXmlRootElement b11 = books.get(0).getValue();
        assertEquals(123L, b11.getId());
        assertEquals("CXF in Action", b11.getName());
        BookNoXmlRootElement b22 = books.get(1).getValue();
        assertEquals(124L, b22.getId());
        assertEquals("CXF Rocks", b22.getName());
    }

    @Test
    public void testGetBookArray() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Book b1 = new Book("CXF in Action", 123L);
        Book b2 = new Book("CXF Rocks", 124L);
        Book[] books = new Book[2];
        books[0] = b1;
        books[1] = b2;
        Book[] books2 = store.getBookArray(books);
        assertNotNull(books2);
        assertNotSame(books, books2);
        assertEquals(2, books2.length);
        Book b11 = books2[0];
        assertEquals(123L, b11.getId());
        assertEquals("CXF in Action", b11.getName());
        Book b22 = books2[1];
        assertEquals(124L, b22.getId());
        assertEquals("CXF Rocks", b22.getName());
    }

    @Test
    public void testGetBookByURL() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT
                               + "/bookstore/bookurl/http%3A%2F%2Ftest.com%2Frss%2F123",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);
    }

    @Test
    public void testHeadBookByURL() throws Exception {
        WebClient wc =
            WebClient.create("http://localhost:" + PORT
                             + "/bookstore/bookurl/http%3A%2F%2Ftest.com%2Frss%2F123");
        Response response = wc.head();
        assertTrue(response.getMetadata().size() != 0);
        assertEquals(0, ((InputStream)response.getEntity()).available());
    }

    @Test
    public void testWebClientUnwrapBookWithXslt() throws Exception {
        XSLTJaxbProvider<Book> provider = new XSLTJaxbProvider<>();
        provider.setInTemplate("classpath:/org/apache/cxf/systest/jaxrs/resources/unwrapbook.xsl");
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/books/wrapper",
                             Collections.singletonList(provider));
        wc.path("{id}", 123);
        Book book = wc.get(Book.class);
        assertNotNull(book);
        assertEquals(123L, book.getId());
    }

    @Test
    public void testOptions() throws Exception {
        WebClient wc =
            WebClient.create("http://localhost:"
                             + PORT + "/bookstore/bookurl/http%3A%2F%2Ftest.com%2Frss%2F123");
        WebClient.getConfig(wc).getRequestContext().put("org.apache.cxf.http.header.split", true);
        Response response = wc.options();
        List<Object> values = response.getMetadata().get("Allow");
        assertNotNull(values);
        assertTrue(values.contains("POST") && values.contains("GET")
                   && values.contains("DELETE") && values.contains("PUT"));
        assertEquals(0, ((InputStream)response.getEntity()).available());
        List<Object> date = response.getMetadata().get("Date");
        assertNotNull(date);
        assertEquals(1, date.size());
    }

    @Test
    public void testExplicitOptions() throws Exception {
        doTestExplicitOptions("http://localhost:" + PORT + "/bookstore/options");
    }
    @Test
    public void testExplicitOptions2() throws Exception {
        doTestExplicitOptions("http://localhost:" + PORT + "/bookstore/options/2");
    }
    private void doTestExplicitOptions(String address) throws Exception {
        WebClient wc = WebClient.create(address);
        WebClient.getConfig(wc).getRequestContext().put("org.apache.cxf.http.header.split", true);
        Response response = wc.options();
        List<Object> values = response.getMetadata().get("Allow");
        assertNotNull(values);
        assertTrue(values.contains("POST") && values.contains("GET")
                   && values.contains("DELETE") && values.contains("PUT"));
        assertEquals(0, ((InputStream)response.getEntity()).available());
        List<Object> date = response.getMetadata().get("Date");
        assertNotNull(date);
        assertEquals(1, date.size());
    }

    @Test
    public void testExplicitOptionsNoSplitByDefault() throws Exception {
        WebClient wc =
            WebClient.create("http://localhost:"
                             + PORT + "/bookstore/options");
        Response response = wc.options();
        List<String> values = Arrays.asList(response.getHeaderString("Allow").split(","));
        assertNotNull(values);
        assertTrue(values.contains("POST") && values.contains("GET")
                   && values.contains("DELETE") && values.contains("PUT"));
        assertEquals(0, ((InputStream)response.getEntity()).available());
        List<Object> date = response.getMetadata().get("Date");
        assertNotNull(date);
        assertEquals(1, date.size());
    }

    @Test
    public void testOptionsOnSubresource() throws Exception {
        WebClient wc =
            WebClient.create("http://localhost:"
                             + PORT + "/bookstore/booksubresource/123");
        WebClient.getConfig(wc).getRequestContext().put("org.apache.cxf.http.header.split", true);
        Response response = wc.options();
        List<Object> values = response.getMetadata().get("Allow");
        assertNotNull(values);
        assertFalse(values.contains("POST") && values.contains("GET")
                   && !values.contains("DELETE") && values.contains("PUT"));
        assertEquals(0, ((InputStream)response.getEntity()).available());
        List<Object> date = response.getMetadata().get("Date");
        assertNotNull(date);
        assertEquals(1, date.size());
    }

    @Test
    public void testEmptyPost() throws Exception {
        WebClient wc =
            WebClient.create("http://localhost:"
                             + PORT + "/bookstore/emptypost");
        Response response = wc.post(null);
        assertEquals(204, response.getStatus());
        assertNull(response.getMetadata().getFirst("Content-Type"));
    }

    @Test
    public void testEmptyPostBytes() throws Exception {
        WebClient wc =
            WebClient.create("http://localhost:"
                             + PORT + "/bookstore/emptypost");
        Response response = wc.post(new byte[]{});
        assertEquals(204, response.getStatus());
        assertNull(response.getMetadata().getFirst("Content-Type"));
    }

    @Test
    public void testEmptyPut() throws Exception {
        WebClient wc =
            WebClient.create("http://localhost:"
                             + PORT + "/bookstore/emptyput");
        Response response = wc.type("application/json").put(null);
        assertEquals(204, response.getStatus());
        assertNull(response.getMetadata().getFirst("Content-Type"));

        response = wc.put("");
        assertEquals(204, response.getStatus());
        assertNull(response.getMetadata().getFirst("Content-Type"));
    }

    @Test
    public void testEmptyPutProxy() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        store.emptyput();
        assertEquals(204, WebClient.client(store).getResponse().getStatus());
    }

    @Test
    public void testEmptyPostProxy() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        store.emptypost();
        assertEquals(204, WebClient.client(store).getResponse().getStatus());
    }

    @Test
    public void testGetStringArray() throws Exception {
        String address = "http://localhost:" + PORT;
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setProvider(new BookStore.StringArrayBodyReaderWriter());
        bean.setAddress(address);
        bean.setResourceClass(BookStore.class);
        BookStore store = bean.create(BookStore.class);
        String[] str = store.getBookStringArray();
        assertEquals("Good book", str[0]);
    }

    @Test
    public void testGetPrimitiveIntArray() throws Exception {
        String address = "http://localhost:" + PORT;
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setProvider(new BookStore.PrimitiveIntArrayReaderWriter());
        bean.setAddress(address);
        bean.setResourceClass(BookStore.class);
        BookStore store = bean.create(BookStore.class);
        int[] arr = store.getBookIndexAsIntArray();
        assertEquals(3, arr.length);
        assertEquals(1, arr[0]);
        assertEquals(2, arr[1]);
        assertEquals(3, arr[2]);
    }

    @Test
    public void testGetPrimitiveDoubleArray() throws Exception {
        String address = "http://localhost:" + PORT;
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setProvider(new BookStore.PrimitiveDoubleArrayReaderWriter());
        bean.setAddress(address);
        bean.setResourceClass(BookStore.class);
        BookStore store = bean.create(BookStore.class);
        double[] arr = store.getBookIndexAsDoubleArray();
        assertEquals(3, arr.length);
        assertEquals(1, arr[0], 0.0);
        assertEquals(2, arr[1], 0.0);
        assertEquals(3, arr[2], 0.0);
    }

    @Test
    public void testGetStringList() throws Exception {
        String address = "http://localhost:" + PORT;
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setProvider(new BookStore.StringListBodyReaderWriter());
        bean.setAddress(address);
        bean.setResourceClass(BookStore.class);
        BookStore store = bean.create(BookStore.class);
        List<String> str = store.getBookListArray();
        assertEquals("Good book", str.get(0));
    }

    @Test
    public void testEmptyPostProxy2() throws Exception {
        String address = "http://localhost:" + PORT;
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        bean.setResourceClass(BookStore.class);
        BookStore store = bean.create(BookStore.class);
        store.emptypostNoPath();
        assertEquals(204, WebClient.client(store).getResponse().getStatus());
    }

    @Test
    public void testBookWithRegexThreadSafe() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setServiceClass(BookStoreRegex.class);
        bean.setAddress("http://localhost:" + PORT);
        bean.setProperties(Collections.<String, Object>singletonMap("url.encode.client.parameters", Boolean.TRUE));
        bean.setThreadSafe(true);
        
        final String[] templates = new String [] {"book", "store"};
        for (String template: templates) {
            BookStoreRegex store = bean.create(BookStoreRegex.class, template);
            Book book = store.getBook(123);
            assertEquals(123L, book.getId());
            assertEquals(template, book.getName());
        }
    }
    
    @Test
    public void testBookWithRegexNonThreadSafe() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setServiceClass(BookStoreRegex.class);
        bean.setAddress("http://localhost:" + PORT);
        bean.setProperties(Collections.<String, Object>singletonMap("url.encode.client.parameters", Boolean.TRUE));
        bean.setThreadSafe(false);
        
        final String[] templates = new String [] {"book", "store"};
        for (String template: templates) {
            BookStoreRegex store = bean.create(BookStoreRegex.class, template);
            Book book = store.getBook(123);
            assertEquals(123L, book.getId());
            assertEquals(template, book.getName());
        }
    }

    @Test
    public void testGetBookByEncodedQuery() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/bookquery?"
                               + "urlid=http%3A%2F%2Ftest.com%2Frss%2F123",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);
    }

    @Test
    public void testGetGenericBook() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/genericbooks/123",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);
    }

    @Test
    public void testGetGenericResponseBook() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/genericresponse/123",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);
    }

    @Test
    public void testGetBookByArrayQuery() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/bookidarray?"
                               + "id=1&id=2&id=3",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);
    }

    @Test
    public void testNoRootResourceException() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/nobookstore/webappexception",
                      "",
                      "application/xml", null, 404);
    }

    @Test
    public void testNoPathMatch() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/bookqueries",
                      "",
                      "application/xml", null, 404);
    }

    @Test
    public void testStatusAngHeadersFromStream() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/statusFromStream";
        WebClient wc = WebClient.create(address);
        wc.accept("text/xml");
        Response r = wc.get();
        assertEquals(503, r.getStatus());
        assertEquals("text/custom+plain", r.getMediaType().toString());
        assertEquals("CustomValue", r.getHeaderString("CustomHeader"));
        assertEquals("Response is not available", r.readEntity(String.class));
    }

    @Test
    public void testWriteAndFailEarly() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/books/fail-early",
                      "This is supposed to go on the wire",
                      "application/bar, text/plain", null, 410);
    }

    @Test
    public void testWriteAndFailLate() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/books/fail-late",
                      "", "application/bar", null, 410);
    }


    @Test
    public void testAcceptTypeMismatch() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/booknames/123",
                      "",
                      "foo/bar", null, 406);
    }

    @Test
    public void testWrongHttpMethod() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/unsupportedcontenttype",
                      "",
                      "foo/bar", null, 405);
    }

    @Test
    public void testWrongQueryParameterType() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/wrongparametertype?p=1",
                      "Parameter Class java.util.Map has no constructor with single String "
                      + "parameter, static valueOf(String) or fromString(String) methods",
                      "*/*", null, 500);
    }

    @Test
    public void testWrongContentType() throws Exception {
        // can't use WebClient here because WebClient plays around with the Content-Type
        // (and makes sure it's syntactically correct) before sending it to the server
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/unsupportedcontenttype";
        URL url = new URL(endpointAddress);
        HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        urlConnection.setReadTimeout(30000); // 30 seconds tops
        urlConnection.setConnectTimeout(30000); // 30 second tops
        urlConnection.addRequestProperty("Content-Type", "MissingSeparator");
        urlConnection.setRequestMethod("POST");
        assertEquals(415, urlConnection.getResponseCode());
    }

    @Test
    public void testExceptionDuringConstruction() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/exceptionconstruction?p=1",
                      "",
                      "foo/bar", null, 404);
    }

    @Test
    public void testSubresourceMethodNotFound() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/interface/thesubresource",
                      "",
                      "foo/bar", null, 404);
    }

    @Test
    public void testNoMessageWriterFound() throws Exception {
        String msg1 =
            "No message body writer has been found for class java.util.GregorianCalendar, ContentType: */*";
        String msg2 = "No message body writer has been found for class java.util.Calendar, ContentType: */*";
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/timetable");
        wc.accept("*/*");
        Response r = wc.get();
        assertEquals(500, r.getStatus());
        String s = r.readEntity(String.class);
        assertTrue(s.equals(msg1) || s.equals(msg2));
    }

    @Test
    public void testNoMessageReaderFound() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/binarybooks";

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(endpointAddress);
        post.addHeader("Content-Type", "application/octet-stream");
        post.addHeader("Accept", "text/xml");
        post.setEntity(new StringEntity("Bar"));

        try {
            CloseableHttpResponse response = client.execute(post);
            assertEquals(415, response.getStatusLine().getStatusCode());
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }

    @Test
    public void testConsumeTypeMismatch() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/unsupportedcontenttype";

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(endpointAddress);
        post.addHeader("Content-Type", "application/bar");
        post.addHeader("Accept", "text/xml");

        try {
            CloseableHttpResponse response = client.execute(post);
            assertEquals(415, response.getStatusLine().getStatusCode());
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }

    @Test
    public void testBookExists() throws Exception {
        checkBook("http://localhost:" + PORT + "/bookstore/books/check/123", true);
        checkBook("http://localhost:" + PORT + "/bookstore/books/check/125", false);
    }

    @Test
    public void testBookExistsWebClientPrimitiveBoolean() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/books/check/123");
        wc.accept("text/plain");
        assertTrue(wc.get(boolean.class));
    }

    @Test
    public void testBookExistsProxyPrimitiveBoolean() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        assertTrue(store.checkBook(123L));
    }

    @Test
    public void testBookExistsWebClientBooleanObject() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/books/check/123");
        wc.accept("text/plain");
        assertTrue(wc.get(Boolean.class));
    }

    @Test
    public void testBookExistsMalformedMt() throws Exception {
        WebClient wc =
            WebClient.create("http://localhost:" + PORT + "/bookstore/books/check/malformedmt/123");
        wc.accept(MediaType.TEXT_PLAIN);
        WebClient.getConfig(wc).getInInterceptors().add(new BookServer.ReplaceContentTypeInterceptor());
        assertTrue(wc.get(Boolean.class));
    }

    @Test
    public void testBookExists2() throws Exception {
        BookStore proxy = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        assertTrue(proxy.checkBook2(123L));
        assertFalse(proxy.checkBook2(125L));
    }

    private void checkBook(String address, boolean expected) throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(address);
        get.addHeader("Accept", "text/plain");

        try {
            CloseableHttpResponse response = client.execute(get);
            assertEquals(200, response.getStatusLine().getStatusCode());
            if (expected) {
                assertEquals("Book must be available",
                             "true", EntityUtils.toString(response.getEntity()));
            } else {
                assertEquals("Book must not be available",
                             "false", EntityUtils.toString(response.getEntity()));
            }
        } finally {
            // Release current connection to the connection pool once you are done
            get.releaseConnection();
        }
    }

    @Test
    public void testGetBookCustomExpression() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/custom/123",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);
    }

    @Test
    public void testGetHeadBook123WebClient() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/getheadbook/";
        WebClient client = WebClient.create(address);
        Response r = client.head();
        assertEquals("HEAD_HEADER_VALUE", r.getMetadata().getFirst("HEAD_HEADER"));
    }

    @Test
    public void testGetHeadBook123WebClient2() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/getheadbook/";
        WebClient client = WebClient.create(address);
        Book b = client.get(Book.class);
        assertEquals(b.getId(), 123L);
    }

    @Test
    public void testGetBook123WithProxy() throws Exception {
        BookStore bs = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Book b = bs.getBook("123");
        assertEquals(b.getId(), 123);
    }

    @Test
    public void testDeleteWithProxy() throws Exception {
        BookStore bs = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Response r = bs.deleteBook("123");
        assertEquals(200, r.getStatus());
    }

    @Test
    public void testCreatePutWithProxy() throws Exception {
        BookStore bs = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Response r = bs.createBook(777L);
        assertEquals(200, r.getStatus());
    }

    @Test
    public void testGetBookFromResponseWithProxyAndReader() throws Exception {
        BookStore bs = JAXRSClientFactory.create("http://localhost:" + PORT,
                                                 BookStore.class);
        Response r = bs.getGenericResponseBook("123");
        assertEquals(200, r.getStatus());
        Book book = r.readEntity(Book.class);
        assertEquals(123L, book.getId());
    }

    @Test
    public void testGetBookFromResponseWithProxy() throws Exception {
        BookStore bs = JAXRSClientFactory.create("http://localhost:" + PORT,
                                                 BookStore.class);
        Response r = bs.getGenericResponseBook("123");
        assertEquals(200, r.getStatus());
        Book book = r.readEntity(Book.class);
        assertEquals(123L, book.getId());
    }

    @Test
    public void testGetBookFromResponseWithWebClientAndReader() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/genericresponse/123";
        WebClient wc = WebClient.create(address);
        Response r = wc.accept("application/xml").get();
        assertEquals(200, r.getStatus());
        Book book = r.readEntity(Book.class);
        assertEquals(123L, book.getId());
    }

    @Test
    public void testGetBookFromResponseWithWebClient() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/genericresponse/123";
        WebClient wc = WebClient.create(address);
        Response r = wc.accept("application/xml").get();
        assertEquals(200, r.getStatus());
        Book book = r.readEntity(Book.class);
        assertEquals(123L, book.getId());
    }

    @Test
    public void testUpdateWithProxy() throws Exception {
        BookStore bs = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Book book = new Book();
        book.setId(888);
        bs.updateBook(book);
        assertEquals(304, WebClient.client(bs).getResponse().getStatus());
    }

    @Test
    public void testGetBookTypeAndWildcard() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/123",
                               "resources/expected_get_book123.txt",
                               "application/xml;q=0.8,*/*",
                               "application/xml", 200);
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/123",
                               "resources/expected_get_book123.txt",
                               "application/*",
                               "application/xml", 200);
    }

    @Test
    public void testSearchBook123() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/search"
                               + "?_s=name==CXF*;id=ge=123;id=lt=124",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);
    }

    @Test
    public void testSearchBook123WithWebClient() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/search";

        WebClient client = WebClient.create(address);
        Book b = client.query("_s", "name==CXF*;id=ge=123;id=lt=124").get(Book.class);
        assertEquals(b.getId(), 123L);
    }

    @Test
    public void testGetSearchBookSQL() throws Exception {
        String address = "http://localhost:" + PORT
            + "/bookstore/books/querycontext/id=ge=123";

        WebClient client = WebClient.create(address);
        client.accept("text/plain");
        String sql = client.get(String.class);
        assertEquals("SELECT * FROM books WHERE id >= '123'", sql);
    }

    @Test (expected = InternalServerErrorException.class)
    public void testSearchUnknownParameter() throws Exception {
        String address = "http://localhost:" + PORT
            + "/bookstore/books/querycontext/id=ge=123%2C1==1";

        WebClient client = WebClient.create(address);
        client.accept("text/plain");
        client.get(String.class);
    }

    @Test
    public void testGetBook123CGLIB() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/123/cglib",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);
    }

    @Test
    public void testGetBookSimple222() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/simplebooks/222");
        Book book = wc.get(Book.class);
        assertEquals(222L, book.getId());
    }

    @Test
    public void testGetBookLowCaseHeader() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/booksecho3");
        wc.type("text/plain").accept("text/plain").header("CustomHeader", "custom");
        String name = wc.post("book", String.class);
        assertEquals("book", name);
        assertEquals("custom", wc.getResponse().getHeaderString("CustomHeader"));
    }
    @Test
    public void testEchoBookName202() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/booksecho202");
        wc.type("text/plain").accept("text/plain");
        Response r = wc.post("book");
        assertEquals(202, r.getStatus());
        assertEquals("book", r.readEntity(String.class));
    }
    @Test
    public void testEchoBookWithLanguage() throws Exception {
        final Book book = new Book("CXF in Action", 100);
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/echoxmlbook-i18n");
        wc.type("application/xml").accept("application/xml").language("en_CA");
        Response r = wc.post(book);
        assertEquals(200, r.getStatus());
        assertEquals(book.getName() + "-en_CA", r.readEntity(Book.class).getName());
    }
    @Test
    public void testEchoBookEntityWithLocale() throws Exception {
        final Book book = new Book("CXF in Action", 100);
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/echoxmlbook-i18n");
        wc.type("application/xml").accept("application/xml").language("en_CA");
        Response r = wc.post(Entity.entity(book, new Variant(MediaType.APPLICATION_XML_TYPE, Locale.UK, null)));
        assertEquals(200, r.getStatus());
        assertEquals(book.getName() + "-en_GB", r.readEntity(Book.class).getName());
    }
    @Test
    public void testEmpty202() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/empty202");
        WebClient.getConfig(wc).getRequestContext().put(Message.PROCESS_202_RESPONSE_ONEWAY_OR_PARTIAL, false);
        wc.type("text/plain").accept("text/plain");
        Response r = wc.post("book");
        assertEquals(202, r.getStatus());
        assertEquals("", r.readEntity(String.class));
    }
    @Test
    public void testGetBookSimple() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/simplebooks/simple");
        Book book = wc.get(Book.class);
        assertEquals(444L, book.getId());
    }
    @Test(expected = ResponseProcessingException.class)
    public void testEmptyJSON() {
        doTestEmptyResponse("application/json");
    }
    @Test(expected = ResponseProcessingException.class)
    public void testEmptyJAXB() {
        doTestEmptyResponse("application/xml");
    }
    private void doTestEmptyResponse(String mt) {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/emptybook");
        WebClient.getConfig(wc).getInInterceptors().add(new BookServer.ReplaceStatusInterceptor());
        wc.accept(mt);
        wc.get(Book.class);
    }
    @Test(expected = ResponseProcessingException.class)
    public void testEmptyResponseProxy() {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        WebClient.getConfig(store).getInInterceptors().add(new BookServer.ReplaceStatusInterceptor());
        store.getEmptyBook();
    }
    @Test
    public void testEmptyResponseProxyNullable() {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        assertNull(store.getEmptyBookNullable());
    }

    @Test
    public void testDropJSONRootDynamically() {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/dropjsonroot");
        wc.accept("application/json");
        String response = wc.get(String.class);
        // with root: {"Book":{"id":123,"name":"CXF in Action"}}
        assertEquals("{\"id\":123,\"name\":\"CXF in Action\"}", response);
    }

    @Test
    public void testFormattedJSON() {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/books/123");
        wc.accept("application/json");
        String response = wc.get(String.class);
        // {"Book":{"id":123,"name":"CXF in Action"}}

        assertTrue(response.charAt(0) == '{');
        assertTrue(response.endsWith("}"));
        assertTrue(response.contains("\"Book\":{"));
        assertTrue(response.indexOf("\"Book\":{") == 1);

        wc.query("_format", "");
        response = wc.get(String.class);
        //{
        //    "Book":{
        //      "id":123,
        //      "name":"CXF in Action"
        //    }
        //}
        assertTrue(response.charAt(0) == '{');
        assertTrue(response.endsWith("}"));
        assertTrue(response.contains("\"Book\":{"));
        assertNotEquals(1, response.indexOf("\"Book\":{"));
    }

    @Test
    public void testGetBook123() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/123",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);

        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/query?bookId=123",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);

        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/defaultquery",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);

        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/missingquery",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);

        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/123",
                               "resources/expected_get_book123json.txt",
                               "application/json, application/xml;q=0.9",
                               "application/json", 200);

        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/123",
                               "resources/expected_get_book123.txt",
                               "application/xml, application/json",
                               "application/xml", 200);
    }

    @Test
    public void testGetBookXmlWildcard() throws Exception {

        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/123",
                               "resources/expected_get_book123.txt",
                               "*/*", "application/xml", 200);
    }

    @Test
    public void testGetBookBuffer() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/buffer",
                               "resources/expected_get_book123.txt",
                               "application/bar", "application/bar", 200);
    }

    @Test
    public void testGetBookBySegment() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/segment/matrix2;first=12;second=3",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore;bar/segment;foo/"
                               + "matrix2;first=12;second=3;third",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);
    }

    @Test
    public void testGetBookByListOfSegments() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/segment/list/1/2/3",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);
    }

    @Test
    public void testGetBookByMatrixParameters() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/segment/matrix;first=12;second=3",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore;bar;first=12/segment;foo;"
                               + "second=3/matrix;third",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);
    }

    @Test
    public void testGetBookByMatrixParametersInTheMiddle() throws Exception {
        getAndCompareAsStrings(
            "http://localhost:" + PORT + "/bookstore/segment;first=12;second=3/matrix-middle",
            "resources/expected_get_book123.txt",
            "application/xml", "application/xml", 200);
    }

    @Test
    public void testGetBookByHeader() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/bookheaders",
                               "resources/expected_get_book123.txt",
                               "application/xml;q=0.5,text/xml", "text/xml", 200);
    }

    @Test
    public void testGetBookByHeaderPerRequest() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore2/bookheaders",
                               "resources/expected_get_book123.txt",
                               "application/xml;q=0.5,text/xml", "text/xml", 200);
    }

    @Test
    public void testGetBookByHeaderPerRequestInjected() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore2/bookheaders/injected";
        WebClient wc = WebClient.create(address);
        wc.accept("application/xml");
        wc.header("BOOK", "1", "2", "3");
        Book b = wc.get(Book.class);
        assertEquals(123L, b.getId());
    }

    @Test
    public void testGetBookByHeaderPerRequestInjectedFault() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore2/bookheaders/injected";
        WebClient wc = WebClient.create(address);
        wc.accept("application/xml");
        wc.header("BOOK", "2", "3");
        Response r = wc.get();
        assertEquals(400, r.getStatus());
        assertEquals("Param setter: 3 header values are required", r.readEntity(String.class));
    }

    @Test
    public void testGetBookByHeaderPerRequestConstructorFault() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore2/bookheaders";
        WebClient wc = WebClient.create(address);
        wc.accept("application/xml");
        wc.header("BOOK", "1", "2", "4");
        Response r = wc.get();
        assertEquals(400, r.getStatus());
        assertEquals("Constructor: Header value 3 is required", r.readEntity(String.class));
    }

    @Test
    public void testGetBookByHeaderPerRequestContextFault() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore2/bookheaders";
        WebClient wc = WebClient.create(address);
        wc.accept("application/xml");
        wc.header("BOOK", "1", "3", "4");
        Response r = wc.get();
        assertEquals(400, r.getStatus());
        assertEquals("Context setter: unexpected header value", r.readEntity(String.class));
    }

    @Test
    public void testGetBookByHeaderDefault() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/bookheaders2",
                               "resources/expected_get_book123.txt",
                               "application/xml;q=0.5,text/xml", "text/xml", 200);
    }

    @Test
    public void testGetBookElement() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/element",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);
    }

    @Test
    public void testEchoBookElement() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        JAXBElement<Book> element = store.echoBookElement(new JAXBElement<Book>(new QName("", "Book"),
                                     Book.class,
                                     new Book("CXF", 123L)));
        Book book = element.getValue();
        assertEquals(123L, book.getId());
        assertEquals("CXF", book.getName());
    }

    @Test
    public void testEchoBookElementWebClient() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/books/element/echo");
        wc.type("application/xml").accept("application/json");
        Book book = wc.post(new Book("\"Jack\" & \"Jill\"", 123L), Book.class);
        assertEquals(123L, book.getId());
        assertEquals("\"Jack\" & \"Jill\"", book.getName());

        wc = WebClient.create("http://localhost:" + PORT + "/bookstore/books/element/echo");
        wc.type("application/xml").accept("application/xml");
        book = wc.post(new Book("Jack & Jill", 123L), Book.class);
        assertEquals(123L, book.getId());
        assertEquals("Jack & Jill", book.getName());
    }

    @Test
    public void testEchoBookElementWildcard() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        JAXBElement<? super Book> element = store.echoBookElementWildcard(
                                        new JAXBElement<Book>(new QName("", "Book"),
                                        Book.class,
                                        new Book("CXF", 123L)));
        Book book = (Book)element.getValue();
        assertEquals(123L, book.getId());
        assertEquals("CXF", book.getName());
    }

    @Test
    public void testGetBookAdapter() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/adapter",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);
    }

    @Test
    public void testPostGetBookAdapterList() throws Exception {
        JAXBElementProvider<?> provider = new JAXBElementProvider<>();
        Map<String, String> outMap = new HashMap<>();
        outMap.put("Books", "CollectionWrapper");
        outMap.put("books", "Book");
        provider.setOutTransformElements(outMap);
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/books/adapter-list",
                                        Collections.singletonList(provider));
        Collection<? extends Book> books = wc.type("application/xml").accept("application/xml")
            .postAndGetCollection(new Books(new Book("CXF", 123L)), Book.class);
        assertEquals(1, books.size());
        assertEquals(123L, books.iterator().next().getId());
    }

    @Test
    public void testPostGetBookAdapterListJSON() throws Exception {
        JAXBElementProvider<?> provider = new JAXBElementProvider<>();
        Map<String, String> outMap = new HashMap<>();
        outMap.put("Books", "CollectionWrapper");
        outMap.put("books", "Book");
        provider.setOutTransformElements(outMap);
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/books/adapter-list",
                                        Collections.singletonList(provider));
        Response r = wc.type("application/xml").accept("application/json")
            .post(new Books(new Book("CXF", 123L)));
        assertEquals("{\"Book\":[{\"id\":123,\"name\":\"CXF\"}]}",
                     IOUtils.readStringFromStream((InputStream)r.getEntity()));
    }

    @Test
    public void testGetBookAdapterInterface() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/interface/adapter",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);
    }

    @Test
    public void testGetBookAdapterInterfaceList() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        List<? extends BookInfoInterface> list = store.getBookAdapterInterfaceList();
        assertEquals(1, list.size());
        BookInfoInterface info = list.get(0);
        assertEquals(123L, info.getId());
    }

    @Test
    public void testGetBookAdapterInterfaceProxy() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        BookInfoInterface info = store.getBookAdapterInterface();
        assertEquals(123L, info.getId());
    }

    @Test
    public void testGetBookAdapterInfoList() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        List<? extends BookInfo> list = store.getBookAdapterList();
        assertEquals(1, list.size());
        BookInfo info = list.get(0);
        assertEquals(123L, info.getId());
    }

    @Test
    public void testGetBookAdapterInfoProxy() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        BookInfo info = store.getBookAdapter();
        assertEquals(123L, info.getId());
    }

    @Test
    public void testGetBook123FromSub() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/interface/subresource",
                               "resources/expected_get_book123.txt",
                               "application/xml", "application/xml", 200);

        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/123",
                               "resources/expected_get_book123json.txt",
                               "application/xml;q=0.1,application/json", "application/json", 200);
    }

    @Test
    public void testGetBook123FromSubObject() throws Exception {
        getAndCompareAsStrings(
            "http://localhost:" + PORT + "/bookstore/booksubresourceobject/123/chaptersobject/sub/1",
            "resources/expected_get_chapter1.txt", "application/xml",
            "application/xml;charset=ISO-8859-1", 200);
    }

    @Test
    public void testGetChapter() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/booksubresource/123/chapters/1",
                               "resources/expected_get_chapter1.txt",
                               "application/xml", "application/xml;charset=ISO-8859-1", 200);
    }

    @Test
    public void testGetBookWithResourceContext() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/booksubresource/context/rc";
        doTestGetBookWithResourceContext(address);
    }

    @Test
    public void testGetBookWithResourceContextBeanParam() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/booksubresource/context/rc/bean";
        doTestGetBookWithResourceContext(address);
    }

    @Test
    public void testGetBookWithResourceContextBeanParam2() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/booksubresource/context/rc/bean2";
        doTestGetBookWithResourceContext(address);
    }

    @Test
    public void testGetBookWithResourceContextInstance() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/booksubresource/instance/context/rc";
        doTestGetBookWithResourceContext(address);
    }

    @Test
    public void testGetBookWithResourceContextClass() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/booksubresource/class/context/rc";
        doTestGetBookWithResourceContext(address);
    }

    private void doTestGetBookWithResourceContext(String address) throws Exception {
        WebClient wc = WebClient.create(address);
        wc.accept("application/xml");
        wc.query("bookid", "12345");
        wc.query("bookname", "bookcontext");
        Book2 book = wc.get(Book2.class);
        assertEquals(12345L, book.getId());
        assertEquals("bookcontext", book.getName());
    }

    @Test
    public void testGetChapterEncodingDefault() throws Exception {
        getAndCompareAsStrings("http://localhost:"
                               + PORT + "/bookstore/booksubresource/123/chapters/badencoding/1",
                               "resources/expected_get_chapter1_utf.txt",
                               "application/xml", "application/xml;charset=UTF-8", 200);
    }

    @Test
    public void testGetChapterAcceptEncoding() throws Exception {
        getAndCompareAsStrings("http://localhost:"
                               + PORT + "/bookstore/booksubresource/123/chapters/acceptencoding/1",
                               "resources/expected_get_chapter1.txt",
                               "application/xml;charset=ISO-8859-1", "application/xml;charset=ISO-8859-1",
                               200);
    }

    @Test
    public void testGetChapterChapter() throws Exception {
        getAndCompareAsStrings("http://localhost:"
                               + PORT + "/bookstore/booksubresource/123/chapters/sub/1/recurse",
                               "resources/expected_get_chapter1_utf.txt",
                               "application/xml", "application/xml", 200);
        getAndCompareAsStrings("http://localhost:"
                               + PORT + "/bookstore/booksubresource/123/chapters/sub/1/recurse2",
                               "resources/expected_get_chapter1.txt",
                               "application/xml", "application/xml;charset=ISO-8859-1", 200);
    }

    @Test
    public void testGetChapterWithParentIds() throws Exception {
        getAndCompareAsStrings(
            "http://localhost:" + PORT + "/bookstore/booksubresource/123/chapters/sub/1/recurse2/ids",
            "resources/expected_get_chapter1.txt",
            "application/xml", "application/xml;charset=ISO-8859-1", 200);
    }

    @Test
    public void testGetBook123ReturnString() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/booknames/123",
                               "resources/expected_get_book123_returnstring.txt",
                               "text/plain", "text/plain", 200);
    }

    @Test
    public void testAddBookNoBody() throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("http://localhost:" + PORT + "/bookstore/books");
        post.addHeader("Content-Type", "application/xml");

        try {
            CloseableHttpResponse response = client.execute(post);
            assertEquals(400, response.getStatusLine().getStatusCode());
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }

    @Test
    public void testAddBookEmptyContent() throws Exception {
        Response r = WebClient.create("http://localhost:" + PORT + "/bookstore/books")
            .type("*/*").post(null);
        assertEquals(400, r.getStatus());
    }

    @Test
    public void testAddBookEmptyContentWithNullable() throws Exception {
        Book defaultBook = WebClient.create("http://localhost:" + PORT + "/bookstore/books/null")
            .type("*/*").post(null, Book.class);
        assertEquals("Default Book", defaultBook.getName());
    }

    @Test
    public void testAddBook() throws Exception {
        doAddBook("http://localhost:" + PORT + "/bookstore/books");
    }

    @Test
    public void testAddBookXmlAdapter() throws Exception {
        doAddBook("http://localhost:" + PORT + "/bookstore/booksinfo");
    }

    private void doAddBook(String address) throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(address);
        post.addHeader("Content-Type", "application/xml");

        try (InputStream input = getClass().getResourceAsStream("resources/add_book.txt");
            InputStream expected = getClass().getResourceAsStream("resources/expected_add_book.txt")) {
            post.setEntity(new InputStreamEntity(input, ContentType.TEXT_XML));

            CloseableHttpResponse response = client.execute(post);
            assertEquals(200, response.getStatusLine().getStatusCode());

            assertEquals(stripXmlInstructionIfNeeded(IOUtils.toString(expected)),
                         stripXmlInstructionIfNeeded(EntityUtils.toString(response.getEntity())));
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }

    @Test
    public void testAddBookCustomFailureStatus() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/books/customstatus";
        WebClient client = WebClient.create(endpointAddress);
        Book book = client.type("text/xml").accept("application/xml").post(new Book(), Book.class);
        assertEquals(888L, book.getId());
        Response r = client.getResponse();
        assertEquals("CustomValue", r.getMetadata().getFirst("CustomHeader"));
        assertEquals(233, r.getStatus());
        assertEquals("application/xml", r.getMetadata().getFirst("Content-Type"));
    }

    @Test
    public void testUpdateBook() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/books";

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPut put = new HttpPut(endpointAddress);

        try (InputStream input = getClass().getResourceAsStream("resources/update_book.txt");
            InputStream expected = getClass().getResourceAsStream("resources/expected_update_book.txt")) {
            put.setEntity(new InputStreamEntity(input, ContentType.TEXT_XML));

            CloseableHttpResponse response = client.execute(put);
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals(stripXmlInstructionIfNeeded(IOUtils.toString(expected)),
                         stripXmlInstructionIfNeeded(EntityUtils.toString(response.getEntity())));
        } finally {
            // Release current connection to the connection pool once you are done
            put.releaseConnection();
        }
    }

    @Test
    public void testUpdateBookWithDom() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/bookswithdom";

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPut put = new HttpPut(endpointAddress);
        try (InputStream input = getClass().getResourceAsStream("resources/update_book.txt");
            InputStream expected = getClass().getResourceAsStream("resources/update_book.txt")) {
            put.setEntity(new InputStreamEntity(input, ContentType.TEXT_XML));

            CloseableHttpResponse response = client.execute(put);
            assertEquals(200, response.getStatusLine().getStatusCode());
            String resp = EntityUtils.toString(response.getEntity());

            assertTrue(resp.contains(IOUtils.toString(expected)));
        } finally {
            // Release current connection to the connection pool once you are done
            put.releaseConnection();
        }
    }

    @Test
    public void testUpdateBookWithJSON() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/bookswithjson";

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPut put = new HttpPut(endpointAddress);

        try (InputStream input = getClass().getResourceAsStream("resources/update_book_json.txt");
            InputStream expected = getClass().getResourceAsStream("resources/expected_update_book.txt")) {
            put.setEntity(new InputStreamEntity(input, ContentType.APPLICATION_JSON));

            CloseableHttpResponse response = client.execute(put);
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals(stripXmlInstructionIfNeeded(IOUtils.toString(expected)),
                         stripXmlInstructionIfNeeded(EntityUtils.toString(response.getEntity())));
        } finally {
            // Release current connection to the connection pool once you are done
            put.releaseConnection();
        }
    }

    @Test
    public void testUpdateBookFailed() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/books";

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPut put = new HttpPut(endpointAddress);

        try (InputStream input = getClass().getResourceAsStream("resources/update_book_not_exist.txt")) {
            put.setEntity(new InputStreamEntity(input, ContentType.TEXT_XML));

            CloseableHttpResponse response = client.execute(put);
            assertEquals(304, response.getStatusLine().getStatusCode());
        } finally {
            // Release current connection to the connection pool once you are done
            put.releaseConnection();
        }
    }

    @Test
    public void testGetCDs() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/cds");
        CDs cds = wc.get(CDs.class);
        Collection<CD> collection = cds.getCD();
        assertEquals(2, collection.size());
        assertTrue(collection.contains(new CD("BICYCLE RACE", 124)));
        assertTrue(collection.contains(new CD("BOHEMIAN RHAPSODY", 123)));
    }

    @Test
    public void testGetCDJSON() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/cd/123",
                               "resources/expected_get_cdjson.txt",
                               "application/json", "application/json", 200);
    }

    @Test
    public void testGetPlainLong() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/booksplain";

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(endpointAddress);
        post.addHeader("Content-Type", "text/plain");
        post.addHeader("Accept", "text/plain");
        post.setEntity(new StringEntity("12345"));

        try {
            CloseableHttpResponse response = client.execute(post);
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals(EntityUtils.toString(response.getEntity()), "12345");
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }

    @Test
    public void testMutipleAcceptHeader() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/booksplain";

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(endpointAddress);
        post.addHeader("Content-Type", "text/plain");
        post.addHeader("Accept", "text/xml");
        post.addHeader("Accept", "text/plain");
        post.setEntity(new StringEntity("12345"));

        try {
            CloseableHttpResponse response = client.execute(post);
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals(EntityUtils.toString(response.getEntity()), "12345");
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }
    @Test
    public void testDeleteBook() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/books/123";

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpDelete delete = new HttpDelete(endpointAddress);

        try {
            CloseableHttpResponse response = client.execute(delete);
            assertEquals(200, response.getStatusLine().getStatusCode());
        } finally {
            // Release current connection to the connection pool once you are done
            delete.releaseConnection();
        }
    }

    @Test
    public void testDeleteBookByQuery() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/books/id?value=123";

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpDelete delete = new HttpDelete(endpointAddress);

        try {
            CloseableHttpResponse response = client.execute(delete);
            assertEquals(200, response.getStatusLine().getStatusCode());
        } finally {
            // Release current connection to the connection pool once you are done
            delete.releaseConnection();
        }
    }

    @Test
    public void testGetCDsJSON() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/cds";

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(endpointAddress);
        get.addHeader("Accept", "application/json");

        try {
            CloseableHttpResponse response = client.execute(get);
            assertEquals(200, response.getStatusLine().getStatusCode());

            InputStream expected123 = getClass().getResourceAsStream("resources/expected_get_cdsjson123.txt");
            InputStream expected124 = getClass().getResourceAsStream("resources/expected_get_cdsjson124.txt");

            String content = EntityUtils.toString(response.getEntity());
            assertTrue(content.indexOf(IOUtils.toString(expected123)) >= 0);
            assertTrue(content.indexOf(IOUtils.toString(expected124)) >= 0);

        } finally {
            // Release current connection to the connection pool once you are done
            get.releaseConnection();
        }
    }

    @Test
    public void testGetCDXML() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/cd/123",
                               "resources/expected_get_cd.txt",
                               "application/xml", "application/xml", 200);
    }

    @Test
    public void testGetCDWithMultiContentTypesXML() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/cdwithmultitypes/123",
                               "resources/expected_get_cd.txt",
                               "application/json;q=0.8,application/xml,*/*", "application/xml", 200);
    }

    @Test
    public void testGetCDWithMultiContentTypesCustomXML() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/cdwithmultitypes/123",
                               "resources/expected_get_cd.txt",
                               "application/bar+xml", "application/bar+xml", 200);
    }

    @Test
    public void testGetCDWithMultiContentTypesJSON() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/cdwithmultitypes/123",
                               "resources/expected_get_cdjson.txt",
                               "application/json", "application/json", 200);
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/cdwithmultitypes/123",
                               "resources/expected_get_cdjson.txt",
                               "*/*,application/xml;q=0.9,application/json", "application/json", 200);
    }

    @Test
    public void testUriInfoMatchedResources() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/"
                      + "booksubresource/123/chapters/sub/1/matched-resources",
                      "[class org.apache.cxf.systest.jaxrs.Chapter, "
                      + "class org.apache.cxf.systest.jaxrs.Book, "
                      + "class org.apache.cxf.systest.jaxrs.BookStore]",
                      "text/plain", "text/plain", 200);
    }

    @Test
    public void testUriInfoMatchedResourcesWithObject() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/"
                      + "booksubresource/123/chaptersobject/sub/1/matched-resources",
                      "[class org.apache.cxf.systest.jaxrs.Chapter, "
                      + "class org.apache.cxf.systest.jaxrs.Book, "
                      + "class org.apache.cxf.systest.jaxrs.BookStore]",
                      "text/plain", "text/plain", 200);
    }

    @Test
    public void testUriInfoMatchedUrisDecode() throws Exception {
        String expected = "[bookstore/booksubresource/123/chapters/sub/1/matched!uris, "
                          + "bookstore/booksubresource/123/chapters/sub/1/, "
                          + "bookstore/booksubresource/123/, "
                          + "bookstore]";
        getAndCompare("http://localhost:" + PORT + "/bookstore/"
                      + "booksubresource/123/chapters/sub/1/matched!uris?decode=true",
                      expected, "text/plain", "text/plain", 200);
    }

    @Test
    public void testQuotedHeaders() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/quotedheaders";
        WebClient wc = WebClient.create(endpointAddress);
        WebClient.getConfig(wc).getRequestContext().put("org.apache.cxf.http.header.split", true);
        Response r = wc.get();

        List<Object> header1 = r.getMetadata().get("SomeHeader1");
        assertEquals(1, header1.size());
        assertEquals("\"some text, some more text\"", header1.get(0));

        List<Object> header2 = r.getMetadata().get("SomeHeader2");
        assertEquals(3, header2.size());
        assertEquals("\"some text\"", header2.get(0));
        assertEquals("\"quoted,text\"", header2.get(1));
        assertEquals("\"and backslash\"", header2.get(2));

        List<Object> header3 = r.getMetadata().get("SomeHeader3");
        assertEquals(1, header3.size());
        assertEquals("\"some text, some more text with inlined \"\"", header3.get(0));

        List<Object> header4 = r.getMetadata().get("SomeHeader4");
        assertEquals(1, header4.size());
        assertEquals("\"\"", header4.get(0));
    }

    @Test
    public void testNonExistentWithGetCustomEx() throws Exception {
        String address = "http://localhostt/bookstore";
        BookStore c = JAXRSClientFactory.create(address, BookStore.class);
        WebClient.getConfig(c).getInFaultInterceptors().add(new CustomFaultInInterceptor(false));
        try {
            c.getBook("123");
            fail("Exception expected");
        } catch (CustomRuntimeException ex) {
            assertEquals("UnknownHostException: Microservice at http://localhostt/bookstore/bookstore/books/123/"
                          + " is not available", ex.getMessage());
        }
    }

    @Test
    public void testBadlyQuotedHeaders() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/badlyquotedheaders";

        String[] responses = new String[] {
            "\"some text",
            "\"some text, some more text with inlined \"",
            "\"some te\\",
        };

        // technically speaking, for these test cases, the client should return an error
        // however, servers do send bad data from time to time so we try to be forgiving
        for (int i = 0; i < 3; i++) {
            WebClient wc = WebClient.create(endpointAddress);
            WebClient.getConfig(wc).getRequestContext().put("org.apache.cxf.http.header.split", true);
            Response r = wc.query("type", Integer.toString(i)).get();
            assertEquals(responses[i], r.getMetadata().get("SomeHeader" + i).get(0));
        }

        // this test currently returns the WRONG result per RFC2616, however it is correct
        // per the discussion in CXF-3518
        WebClient wc = WebClient.create(endpointAddress);
        WebClient.getConfig(wc).getRequestContext().put("org.apache.cxf.http.header.split", true);
        Response r3 = wc.query("type", "3").get();
        List<Object> r3values = r3.getMetadata().get("SomeHeader3");
        assertEquals(4, r3values.size());
        assertEquals("some text", r3values.get(0));
        assertEquals("\"other quoted\"", r3values.get(1));
        assertEquals("text", r3values.get(2));
        assertEquals("blah", r3values.get(3));
    }

    @Test
    public void testGetBookWithReaderInterceptor() throws Exception {
        BookStore client = JAXRSClientFactory
                .create("http://localhost:" + PORT, BookStore.class, Collections.singletonList(getReaderInterceptor()));
        Book book = client.getBook(0);
        assertEquals(123, book.getId());
    }

    @Test
    public void testGetBookWithServerWebApplicationExceptionAndReaderInterceptor() throws Exception {
        BookStore client = JAXRSClientFactory
                .create("http://localhost:" + PORT, BookStore.class, Collections.singletonList(getReaderInterceptor()));
        try {
            client.throwException();
            fail("Exception expected");
        } catch (ServerErrorException ex) {
            assertEquals(500, ex.getResponse().getStatus());
            assertEquals("This is a WebApplicationException", ex.getResponse().readEntity(String.class));
        }
    }

    private ReaderInterceptor getReaderInterceptor() {
        return readerInterceptorContext -> {
            InputStream is = new BufferedInputStream(readerInterceptorContext.getInputStream());
            readerInterceptorContext.setInputStream(is);
            return readerInterceptorContext.proceed();
        };
    }

    private static void getAndCompareAsStrings(String address, String resourcePath, String acceptType,
            String expectedContentType, int status) throws Exception {
        String expected = IOUtils.toString(
            JAXRSClientServerBookTest.class.getResourceAsStream(resourcePath));
        getAndCompare(address,
                      expected,
                      acceptType,
                      expectedContentType,
                      status);
    }

    private static void getAndCompare(String address, String expectedValue, String acceptType, 
            String expectedContentType, int expectedStatus) throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(address);
        get.addHeader("Accept", acceptType);
        get.addHeader("Cookie", "a=b;c=d");
        get.addHeader("Cookie", "e=f");
        get.addHeader("Accept-Language", "da;q=0.8,en");
        get.addHeader("Book", "1,2,3");

        try {
            CloseableHttpResponse response = client.execute(get);
            assertEquals(expectedStatus, response.getStatusLine().getStatusCode());
            String content = EntityUtils.toString(response.getEntity());
            assertEquals("Expected value is wrong",
                         stripXmlInstructionIfNeeded(expectedValue), stripXmlInstructionIfNeeded(content));
            if (expectedStatus == 200) {
                assertEquals("123", response.getFirstHeader("BookId").getValue());
                assertNotNull(response.getFirstHeader("Date"));
            }
            if (expectedStatus == 405) {
                assertNotNull(response.getFirstHeader("Allow"));
            }
            if (expectedContentType != null) {
                Header ct = response.getFirstHeader("Content-Type");
                assertEquals("Wrong type of response", expectedContentType, ct.getValue());
            }
        } finally {
            get.releaseConnection();
        }
    }

    private static String stripXmlInstructionIfNeeded(String str) {
        if (str != null && str.startsWith("<?xml")) {
            int index = str.indexOf("?>");
            str = str.substring(index + 2);
        }
        return str;
    }
    
    @Provider
    private static final class TestClientResponseFilter implements ClientResponseFilter {
        @Override
        public void filter(ClientRequestContext requestContext,
                ClientResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("X-Filter", "true");
        }
    }
}
