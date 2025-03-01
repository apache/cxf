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

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JAXRSClientServerResourceJacksonSpringProviderTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerResourceJacksonSpringProviders.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(BookServerResourceJacksonSpringProviders.class, true));
        createStaticBus();
        BusFactory.getDefaultBus().setProperty(ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);
        BusFactory.getDefaultBus().setProperty(ProviderFactory.SKIP_DEFAULT_JSON_PROVIDER_REGISTRATION, true);
    }
    @AfterClass
    public static void afterClass() throws Exception {
        BusFactory.getDefaultBus().getProperties().remove(ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION);
        BusFactory.getDefaultBus().getProperties().remove(ProviderFactory.SKIP_DEFAULT_JSON_PROVIDER_REGISTRATION);
    }

    @Test
    public void testGetBook123() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/store1/bookstore/books/123";
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "application/json");
        InputStream in = connect.getInputStream();
        assertNotNull(in);

        assertEquals("Jackson output not correct",
                     "{\"class\":\"org.apache.cxf.systest.jaxrs.Book\",\"name\":\"CXF in Action\",\"id\":123}",
                     getStringFromInputStream(in).trim());
    }

    @Test
    public void testGetSuperBookProxy() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/store2";
        BookStoreSpring proxy = JAXRSClientFactory.create(endpointAddress, BookStoreSpring.class,
            Collections.singletonList(new JacksonJsonProvider()));
        SuperBook book = proxy.getSuperBookJson();
        assertEquals(999L, book.getId());
    }

    @Test
    public void testMultipart() throws Exception {

        String endpointAddress = "http://localhost:" + PORT + "/webapp/multipart";
        MultipartStore proxy = JAXRSClientFactory.create(endpointAddress, MultipartStore.class,
            Collections.singletonList(new JacksonJsonProvider()));
        Book json = new Book("json", 1L);
        InputStream is1 = getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg");

        Map<String, Object> attachments = proxy.addBookJsonImageStream(json, is1);
        assertEquals(2, attachments.size());
        Book json2 = ((Attachment)attachments.get("application/json")).getObject(Book.class);
        assertEquals("json", json2.getName());
        assertEquals(1L, json2.getId());
        InputStream is2 = ((Attachment)attachments.get("application/octet-stream")).getObject(InputStream.class);
        byte[] image1 = IOUtils.readBytesFromStream(
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg"));
        byte[] image2 = IOUtils.readBytesFromStream(is2);
        assertArrayEquals(image1, image2);
    }

    @Test
    public void testGetSuperBookCollectionProxy() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/store2";
        BookStoreSpring proxy = JAXRSClientFactory.create(endpointAddress, BookStoreSpring.class,
            Collections.singletonList(new JacksonJsonProvider()));
        List<SuperBook> books = proxy.getSuperBookCollectionJson();
        assertEquals(999L, books.get(0).getId());
    }

    @Test
    public void testEchoSuperBookCollectionProxy() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/store2";
        BookStoreSpring proxy = JAXRSClientFactory.create(endpointAddress, BookStoreSpring.class,
            Collections.singletonList(new JacksonJsonProvider()));
        WebClient.getConfig(proxy).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        List<SuperBook> books =
            proxy.echoSuperBookCollectionJson(Collections.singletonList(new SuperBook("Super", 124L, true)));
        assertEquals(124L, books.get(0).getId());
        assertTrue(books.get(0).isSuperBook());
    }

    @Test
    public void testEchoSuperBookProxy() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/store2";
        BookStoreSpring proxy = JAXRSClientFactory.create(endpointAddress, BookStoreSpring.class,
            Collections.singletonList(new JacksonJsonProvider()));
        SuperBook book = proxy.echoSuperBookJson(new SuperBook("Super", 124L, true));
        assertEquals(124L, book.getId());
        assertTrue(book.isSuperBook());
    }

    @Test
    public void testEchoGenericSuperBookCollectionProxy() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/custombus/genericstore";
        GenericBookStoreSpring proxy = JAXRSClientFactory.create(endpointAddress,
            GenericBookStoreSpring.class, Collections.singletonList(new JacksonJsonProvider()));
        List<SuperBook> books =
            proxy.echoSuperBookCollectionJson(Collections.singletonList(new SuperBook("Super", 124L, true)));
        assertEquals(124L, books.get(0).getId());
        assertTrue(books.get(0).isSuperBook());
    }

    @Test
    public void testEchoGenericSuperBookProxy() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/custombus/genericstore";
        GenericBookStoreSpring proxy = JAXRSClientFactory.create(endpointAddress,
            GenericBookStoreSpring.class, Collections.singletonList(new JacksonJsonProvider()));
        WebClient.getConfig(proxy).getHttpConduit().getClient().setReceiveTimeout(1000000000L);
        SuperBook book = proxy.echoSuperBookJson(new SuperBook("Super", 124L, true));
        assertEquals(124L, book.getId());
        assertTrue(book.isSuperBook());
    }

    @Test
    public void testGetGenericSuperBookInt1() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/genericstoreInt1/int/books/superbook";
        WebClient wc = WebClient.create(endpointAddress,
            Collections.singletonList(new JacksonJsonProvider()));
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000000L);
        GenericType<List<SuperBook>> genericResponseType = new GenericType<List<SuperBook>>() {
        };
        List<SuperBook> books = wc.get(genericResponseType);
        assertEquals(1, books.size());
        assertEquals(111L, books.get(0).getId());

    }
    @Test
    public void testGetGenericSuperBookInt2() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/genericstoreInt2";
        GenericBookServiceInterface proxy = JAXRSClientFactory.create(endpointAddress,
            GenericBookServiceInterface.class, Collections.singletonList(new JacksonJsonProvider()));
        WebClient.getConfig(proxy).getHttpConduit().getClient().setReceiveTimeout(1000000000L);
        List<SuperBook> books = proxy.getSuperBook();
        assertEquals(1, books.size());
        assertEquals(111L, books.get(0).getId());

    }

    @Test
    public void testEchoGenericSuperBookProxy2Json() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/genericstore2";
        GenericBookStoreSpring2 proxy = JAXRSClientFactory.create(endpointAddress,
            GenericBookStoreSpring2.class, Collections.singletonList(new JacksonJsonProvider()));
        WebClient.getConfig(proxy).getHttpConduit().getClient().setReceiveTimeout(1000000000L);
        WebClient.client(proxy).type("application/json").accept("application/json");
        SuperBook book = proxy.echoSuperBook(new SuperBook("Super", 124L, true));
        assertEquals(124L, book.getId());
        assertTrue(book.isSuperBook());
    }

    @Test
    public void testEchoGenericSuperBookProxy2JsonType() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/genericstore2type";
        GenericBookStoreSpring2 proxy = JAXRSClientFactory.create(endpointAddress,
            GenericBookStoreSpring2.class, Collections.singletonList(new JacksonJsonProvider()));
        WebClient.getConfig(proxy).getHttpConduit().getClient().setReceiveTimeout(1000000000L);
        WebClient.client(proxy).type("application/json").accept("application/json");
        SuperBook2 book = proxy.echoSuperBookType(new SuperBook2("Super", 124L, true));
        assertEquals(124L, book.getId());
        assertTrue(book.isSuperBook());
    }

    @Test
    public void testEchoGenericSuperBookProxy2Xml() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/genericstore2";
        JAXBElementProvider<Object> jaxbProvider = new JAXBElementProvider<>();
        jaxbProvider.setXmlRootAsJaxbElement(true);
        jaxbProvider.setMarshallAsJaxbElement(true);
        GenericBookStoreSpring2 proxy = JAXRSClientFactory.create(endpointAddress,
            GenericBookStoreSpring2.class, Collections.singletonList(jaxbProvider));
        WebClient.getConfig(proxy).getHttpConduit().getClient().setReceiveTimeout(1000000000L);
        WebClient.client(proxy).type("application/xml").accept("application/xml");
        SuperBook book = proxy.echoSuperBook(new SuperBook("Super", 124L, true));
        assertEquals(124L, book.getId());
        assertTrue(book.isSuperBook());
    }

    @Test
    public void testEchoGenericSuperBookProxy2XmlType() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/genericstore2type";
        JAXBElementProvider<Object> jaxbProvider = new JAXBElementProvider<>();
        jaxbProvider.setMarshallAsJaxbElement(true);
        jaxbProvider.setUnmarshallAsJaxbElement(true);
        GenericBookStoreSpring2 proxy = JAXRSClientFactory.create(endpointAddress,
            GenericBookStoreSpring2.class, Collections.singletonList(jaxbProvider));
        WebClient.getConfig(proxy).getHttpConduit().getClient().setReceiveTimeout(1000000000L);
        WebClient.client(proxy).type("application/xml").accept("application/xml");
        SuperBook2 book = proxy.echoSuperBookType(new SuperBook2("Super", 124L, true));
        assertEquals(124L, book.getId());
        assertTrue(book.isSuperBook());
    }

    @Test
    public void testEchoGenericSuperBookCollectionProxy2Json() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/genericstore2";
        GenericBookStoreSpring2 proxy = JAXRSClientFactory.create(endpointAddress,
            GenericBookStoreSpring2.class, Collections.singletonList(new JacksonJsonProvider()));
        WebClient.client(proxy).type("application/json").accept("application/json");
        List<SuperBook> books =
            proxy.echoSuperBookCollection(Collections.singletonList(new SuperBook("Super", 124L, true)));
        assertEquals(124L, books.get(0).getId());
        assertTrue(books.get(0).isSuperBook());
    }

    @Test
    public void testEchoGenericSuperBookCollectionProxy2JsonType() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/genericstore2type";
        GenericBookStoreSpring2 proxy = JAXRSClientFactory.create(endpointAddress,
            GenericBookStoreSpring2.class, Collections.singletonList(new JacksonJsonProvider()));
        WebClient.client(proxy).type("application/json").accept("application/json");
        List<SuperBook2> books =
            proxy.echoSuperBookTypeCollection(Collections.singletonList(new SuperBook2("Super", 124L, true)));
        assertEquals(124L, books.get(0).getId());
        assertTrue(books.get(0).isSuperBook());
    }

    @Test
    public void testEchoGenericSuperBookCollectionProxy2Xml() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/genericstore2";
        JAXBElementProvider<Object> jaxbProvider = new JAXBElementProvider<>();
        jaxbProvider.setMarshallAsJaxbElement(true);
        jaxbProvider.setUnmarshallAsJaxbElement(true);
        GenericBookStoreSpring2 proxy = JAXRSClientFactory.create(endpointAddress,
            GenericBookStoreSpring2.class, Collections.singletonList(jaxbProvider));
        WebClient.client(proxy).type("application/xml").accept("application/xml");
        WebClient.getConfig(proxy).getHttpConduit().getClient().setReceiveTimeout(1000000000L);
        List<SuperBook> books =
            proxy.echoSuperBookCollection(Collections.singletonList(new SuperBook("Super", 124L, true)));
        assertEquals(124L, books.get(0).getId());
        assertTrue(books.get(0).isSuperBook());
    }

    @Test
    public void testEchoGenericSuperBookCollectionProxy2XmlType() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/genericstore2type";
        JAXBElementProvider<Object> jaxbProvider = new JAXBElementProvider<>();
        jaxbProvider.setMarshallAsJaxbElement(true);
        jaxbProvider.setUnmarshallAsJaxbElement(true);
        GenericBookStoreSpring2 proxy = JAXRSClientFactory.create(endpointAddress,
            GenericBookStoreSpring2.class, Collections.singletonList(jaxbProvider));
        WebClient.client(proxy).type("application/xml").accept("application/xml");
        WebClient.getConfig(proxy).getHttpConduit().getClient().setReceiveTimeout(1000000000L);
        List<SuperBook2> books =
            proxy.echoSuperBookTypeCollection(Collections.singletonList(new SuperBook2("Super", 124L, true)));
        assertEquals(124L, books.get(0).getId());
        assertTrue(books.get(0).isSuperBook());
    }

    @Test
    public void testEchoGenericSuperBookWebClient() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/custombus/genericstore/books/superbook";
        WebClient wc = WebClient.create(endpointAddress,
                                        Collections.singletonList(new JacksonJsonProvider()));
        wc.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON);
        SuperBook book = wc.post(new SuperBook("Super", 124L, true), SuperBook.class);
        assertEquals(124L, book.getId());
        assertTrue(book.isSuperBook());
    }

    @Test
    public void testEchoGenericSuperBookWebClientXml() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/custombus/genericstore/books/superbook";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept(MediaType.APPLICATION_XML).type(MediaType.APPLICATION_XML);
        SuperBook book = wc.post(new SuperBook("Super", 124L, true), SuperBook.class);
        assertEquals(124L, book.getId());
        assertTrue(book.isSuperBook());
    }

    @Test
    public void testEchoGenericSuperBookCollectionWebClient() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/custombus/genericstore/books/superbooks";
        WebClient wc = WebClient.create(endpointAddress,
                                        Collections.singletonList(new JacksonJsonProvider()));
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(100000000L);
        wc.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON);
        Collection<? extends SuperBook> books =
            wc.postAndGetCollection(Collections.singletonList(new SuperBook("Super", 124L, true)),
                                    SuperBook.class,
                                    SuperBook.class);
        SuperBook book = books.iterator().next();
        assertEquals(124L, book.getId());
        assertTrue(book.isSuperBook());
    }

    @Test
    public void testGetGenericSuperBookCollectionWebClient() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/custombus/genericstore/books/superbooks2";
        WebClient wc = WebClient.create(endpointAddress,
                                        Collections.singletonList(new JacksonJsonProvider()));
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(100000000L);
        wc.accept(MediaType.APPLICATION_JSON);
        List<SuperBook> books = wc.get(new GenericType<List<SuperBook>>() {
        });

        SuperBook book = books.iterator().next();
        assertEquals(124L, book.getId());
        assertTrue(book.isSuperBook());
    }

    @Test
    public void testEchoGenericSuperBookCollectionWebClientXml() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/custombus/genericstore/books/superbooks";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept(MediaType.APPLICATION_XML).type(MediaType.APPLICATION_XML);
        Collection<? extends SuperBook> books =
            wc.postAndGetCollection(Collections.singletonList(new SuperBook("Super", 124L, true)),
                                    SuperBook.class,
                                    SuperBook.class);
        SuperBook book = books.iterator().next();
        assertEquals(124L, book.getId());
        assertTrue(book.isSuperBook());
    }

    @Test
    public void testGetCollectionOfBooks() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/store1/bookstore/collections";
        WebClient wc = WebClient.create(endpointAddress,
            Collections.singletonList(new JacksonJsonProvider()));
        wc.accept("application/json");
        Collection<? extends Book> collection = wc.getCollection(Book.class);
        assertEquals(1, collection.size());
        Book book = collection.iterator().next();
        assertEquals(123L, book.getId());
    }

    @Test
    public void testGetCollectionOfSuperBooks() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/store2/books/superbooks";
        WebClient wc = WebClient.create(endpointAddress,
            Collections.singletonList(new JacksonJsonProvider()));
        wc.accept("application/json");
        Collection<? extends Book> collection = wc.getCollection(Book.class);
        assertEquals(1, collection.size());
        Book book = collection.iterator().next();
        assertEquals(999L, book.getId());
    }


    private String getStringFromInputStream(InputStream in) throws Exception {
        return IOUtils.toString(in);
    }

}
