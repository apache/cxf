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

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.apache.cxf.Bus;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.AttachmentBuilder;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.util.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class JAXRSMultipartTest extends AbstractBusClientServerTestBase {
    public static final String PORT = MultipartServer.PORT;
    public static final String PORTINV = allocatePort(JAXRSMultipartTest.class, 1);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(MultipartServer.class, true));
        final Bus bus = createStaticBus();
        // Make sure default JSON-P/JSON-B providers are not loaded
        bus.setProperty(ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);
    }

    @Test
    public void testBookAsRootAttachmentStreamSource() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/stream";
        doAddBook(address, "attachmentData", 200);
    }

    @Test
    public void testBookAsRootAttachmentStreamSourceNoContentId() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/stream";
        doAddBook(address, "attachmentData3", 200);
    }

    @Test
    public void testBookAsRootAttachmentInputStream() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/istream";
        doAddBook(address, "attachmentData", 200);
    }

    @Test
    public void testBookAsRootAttachmentInputStreamReadItself() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/istream2";
        WebClient wc = WebClient.create(address);
        wc.type("multipart/mixed;type=text/xml");
        wc.accept("text/xml");
        WebClient.getConfig(wc).getRequestContext().put("support.type.as.multipart",
            "true");
        Book book = wc.post(new Book("CXF in Action - 2", 12345L), Book.class);
        assertEquals(432L, book.getId());
    }

    @Test
    public void testGetBookAsStringContent() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/content/string";
        doTestGetBookAsPlainContent(address);
    }

    @Test
    public void testGetBookAsByteContent() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/content/bytes";
        doTestGetBookAsPlainContent(address);
    }

    private void doTestGetBookAsPlainContent(String address) throws Exception {
        WebClient wc = WebClient.create(address);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000);
        wc.accept("multipart/mixed");
        MultipartBody book = wc.get(MultipartBody.class);
        Book b = book.getRootAttachment().getObject(Book.class);
        assertEquals(888L, b.getId());
    }

    @Test
    public void testBookAsMessageContextDataHandler() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/mchandlers";
        doAddBook(address, "attachmentData", 200);
    }

    @Test
    public void testBookAsMessageContextAttachments() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/attachments";
        doAddBook(address, "attachmentData", 200);
    }


    int countTempFiles() {
        File file = FileUtils.getDefaultTempDir();
        File[] files = file.listFiles();
        if (files == null) {
            return 0;
        }
        int count = 0;
        for (File f : files) {
            if (f.isFile()) {
                count++;
            }
        }
        return count;
    }
    @Test
    public void testBookAsMassiveAttachment() throws Exception {
        //CXF-5842
        int orig = countTempFiles();
        String address = "http://localhost:" + PORT + "/bookstore/books/attachments";
        InputStream is =
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/attachmentData");
        //create a stream that sticks a bunch of data for the attachment to cause the
        //server to buffer the attachment to disk.
        PushbackInputStream buf = new PushbackInputStream(is, 1024 * 20) {
            int bcount = -1;
            @Override
            public int read(byte[] b, int offset, int len) throws IOException {
                if (bcount >= 0 && bcount < 1024 * 50) {
                    for (int x = 0; x < len; x++) {
                        b[offset + x] = (byte)x;
                    }
                    bcount += len;
                    return len;
                }
                int i = super.read(b, offset, len);
                for (int x = 0; x < i - 5; x++) {
                    if (b[x + offset] == '*'
                        && b[x + offset + 1] == '*'
                        && b[x + offset + 2] == 'D'
                        && b[x + offset + 3] == '*'
                        && b[x + offset + 4] == '*') {
                        super.unread(b, x + offset + 5, i - x - 5);
                        i = x;
                        bcount = 0;
                    }
                }
                return i;
            }
        };
        doAddBook("multipart/related", address, buf, 413);
        assertEquals(orig, countTempFiles());
    }    
    @Test
    public void testBookAsMassiveAttachmentInvalidPath() throws Exception {
        int orig = countTempFiles();
        String address = "http://localhost:" + PORT + "/INVALID/bookstore/books/attachments";
        InputStream is =
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/attachmentData");
        //create a stream that sticks a bunch of data for the attachment to cause the
        //server to buffer the attachment to disk.
        PushbackInputStream buf = new PushbackInputStream(is, 1024 * 20) {
            int bcount = -1;
            @Override
            public int read(byte[] b, int offset, int len) throws IOException {
                if (bcount >= 0 && bcount < 1024 * 5000) {
                    for (int x = 0; x < len; x++) {
                        b[offset + x] = (byte)x;
                    }
                    bcount += len;
                    return len;
                }
                int i = super.read(b, offset, len);
                for (int x = 0; x < i - 5; x++) {
                    if (b[x + offset] == '*'
                        && b[x + offset + 1] == '*'
                        && b[x + offset + 2] == 'D'
                        && b[x + offset + 3] == '*'
                        && b[x + offset + 4] == '*') {
                        super.unread(b, x + offset + 5, i - x - 5);
                        i = x;
                        bcount = 0;
                    }
                }
                return i;
            }
        };
        doAddBook("multipart/related", address, buf, 404);
        assertEquals(orig, countTempFiles());
    }
    @Test
    public void testBookAsMassiveAttachmentInvalidPort() throws Exception {
        String address = "http://localhost:" + PORTINV + "/bookstore/books/attachments";
        InputStream is =
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/attachmentData");
        //create a stream that sticks a bunch of data for the attachment to cause the
        //server to buffer the attachment to disk.
        PushbackInputStream buf = new PushbackInputStream(is, 1024 * 20) {
            int bcount = -1;
            @Override
            public int read(byte[] b, int offset, int len) throws IOException {
                if (bcount >= 0 && bcount < 1024 * 5000) {
                    for (int x = 0; x < len; x++) {
                        b[offset + x] = (byte)x;
                    }
                    bcount += len;
                    return len;
                }
                int i = super.read(b, offset, len);
                for (int x = 0; x < i - 5; x++) {
                    if (b[x + offset] == '*'
                        && b[x + offset + 1] == '*'
                        && b[x + offset + 2] == 'D'
                        && b[x + offset + 3] == '*'
                        && b[x + offset + 4] == '*') {
                        super.unread(b, x + offset + 5, i - x - 5);
                        i = x;
                        bcount = 0;
                    }
                }
                return i;
            }
        };
        try {
            doAddBook("multipart/related", address, buf, 404);
            org.junit.Assert.fail("Should have thrown an exception");
        } catch (Exception ex) {
            Assert.hasText(ex.getMessage(), "Connection refused");
        }
    }

    @Test
    public void testBookJSONForm() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jsonform";
        doAddFormBook(address, "attachmentFormJson", 200);
    }

    @Test
    public void testBookJaxbForm() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jaxbform";
        doAddFormBook(address, "attachmentFormJaxb", 200);
    }

    @Test
    public void testBookJSONJAXBForm() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jsonjaxbform";
        doAddFormBook(address, "attachmentFormJsonJaxb", 200);
    }

    @Test
    public void testBookJSONJAXBFormEncoded() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jsonjaxbform";
        doAddFormBook(address, "attachmentFormJsonJaxbEncoded", 200);
    }

    @Test
    public void testBookJSONFormTwoFiles() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/filesform";
        doAddFormBook(address, "attachmentFormJsonFiles", 200);
    }

    @Test
    public void testBookJSONFormTwoFiles2() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/filesform2";
        doAddFormBook(address, "attachmentFormJsonFiles", 200);
    }

    @Test
    public void testBookJSONFormTwoFilesNotRecursive() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/filesform";
        doAddFormBook(address, "attachmentFormJsonFiles2", 200);
    }

    @Test
    public void testBookJSONFormOneFileWhereManyExpected() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/filesform/singlefile";
        doAddFormBook(address, "attachmentFormJsonFile", 200);
    }

    @Test
    public void testBookJSONFormTwoFilesMixUp() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/filesform/mixup";
        doAddFormBook(address, "attachmentFormJsonFiles", 200);
    }

    @Test
    public void testBookJSONFormOneFile() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/fileform";
        doAddFormBook(address, "attachmentFormJsonFile", 200);
    }

    @Test
    public void testBookAsMessageContextAttachment() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/attachment";
        doAddBook(address, "attachmentData", 200);
    }

    @Test
    public void testAddBookAsRootAttachmentJAXB() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jaxb";
        doAddBook(address, "attachmentData", 200);
    }

    @Test
    public void testAddBookAsDataSource() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/dsource";
        doAddBook(address, "attachmentData", 200);
    }

    @Test
    public void testAddBookAsDataSource2() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/dsource2";
        doAddBook(address, "attachmentData", 200);
    }

    @Test
    public void testAddBookAsBody() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/body";
        doAddBook(address, "attachmentData", 200);
    }

    @Test
    public void testAddBookFormData() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/form";
        doAddBook("multipart/form-data", address, "attachmentForm", 200);
    }

    @Test
    public void testAddBookFormParam() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/formparam";
        doAddBook("multipart/form-data", address, "attachmentForm", 200);
    }

    @Test
    public void testAddBookFormBody() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/formbody";
        doAddBook("multipart/form-data", address, "attachmentForm", 200);
    }

    @Test
    public void testAddBookFormBody2() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/formbody2";
        doAddBook("multipart/form-data", address, "attachmentForm", 200);
    }

    @Test
    public void testAddBookFormParamBean() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/formparambean";
        doAddBook("multipart/form-data", address, "attachmentForm", 200);
    }

    @Test
    public void testAddBookAsJAXB2() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jaxb2";
        doAddBook(address, "attachmentData", 200);
    }

    @Test
    public void testAddBookAsJAXBBody() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jaxb-body";
        doAddBook(address, "attachmentData", 200);
    }

    @Test
    public void testAddBookAsListOfAttachments() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/listattachments";
        doAddBook(address, "attachmentData", 200);
    }

    @Test
    public void testAddBookAsListOfStreams() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/lististreams";
        doAddBook(address, "attachmentData", 200);
    }

    @Test
    public void testAddBookMixedMultiValueMapParameter() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/mixedmultivaluedmap";

        InputStream is = getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/attachmentFormMixed");
        if (System.getProperty("os.name").startsWith("Windows")) {
            is = new NoCarriageReturnFileInputStream(is);
        } 

        doAddBook("multipart/mixed", address, is, 200);
    }

    @Test
    public void testAddBookAsJAXBJSON() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jaxbjson";
        doAddBook(address, "attachmentData2", 200);
    }

    @Test
    public void testAddBookAsJAXBJSONProxy() throws Exception {
        MultipartStore store =
            JAXRSClientFactory.create("http://localhost:" + PORT, MultipartStore.class);
        Book b = store.addBookJaxbJsonWithConsumes(new Book2("CXF in Action", 1L),
                                           new Book("CXF in Action - 2", 2L));
        assertEquals(124L, b.getId());
        assertEquals("CXF in Action - 2", b.getName());
    }

    @Test
    public void testNullPartProxy() throws Exception {
        MultipartStore store =
            JAXRSClientFactory.create("http://localhost:" + PORT, MultipartStore.class);
        assertEquals("nobody home2", store.testNullParts("value1", null));
    }

    @Test
    public void testUseProxyToAddBookAndSimpleParts() throws Exception {
        MultipartStore store =
            JAXRSClientFactory.create("http://localhost:" + PORT, MultipartStore.class);
        HTTPConduit conduit = WebClient.getConfig(store).getHttpConduit();
        conduit.getClient().setReceiveTimeout(1000000);
        Book b = store.testAddBookAndSimpleParts(new Book("CXF in Action", 124L), "1", 2);
        assertEquals(124L, b.getId());
        assertEquals("CXF in Action - 12", b.getName());
    }

    @Test
    public void testAddBookAsJAXBOnlyProxy() throws Exception {
        MultipartStore store =
            JAXRSClientFactory.create("http://localhost:" + PORT, MultipartStore.class);

        Book2 b = store.addBookJaxbOnlyWithConsumes(new Book2("CXF in Action", 1L));
        assertEquals(1L, b.getId());
        assertEquals("CXF in Action", b.getName());
    }

    @Test
    public void testAddBookAsJAXBJSONMixed() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jaxbjson";
        doAddBook("multipart/mixed", address, "attachmentData2", 200);
    }

    @Test
    public void testConsumesMismatch() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/mismatch1";
        doAddBook(address, "attachmentData2", 415);
    }

    @Test
    public void testConsumesMismatch2() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/mismatch2";
        doAddBook(address, "attachmentData2", 415);
    }

    @Test
    public void testAddBookAsDataHandler() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/dhandler";
        doAddBook(address, "attachmentData", 200);
    }

    @Test
    public void testAddBookWebClient() {
        InputStream is1 =
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/add_book.txt");
        String address = "http://localhost:" + PORT + "/bookstore/books/jaxb";
        WebClient client = WebClient.create(address);
        WebClient.getConfig(client).getRequestContext().put("support.type.as.multipart",
            "true");
        client.type("multipart/related;type=text/xml").accept("text/xml");
        Book book = client.post(is1, Book.class);
        assertEquals("CXF in Action - 2", book.getName());
    }

    @Test
    public void testAddCollectionOfBooksWithProxy() {
        doTestAddCollectionOfBooksWithProxy(true);
    }

    @Test
    public void testAddCollectionOfBooksWithProxyWithoutHeader() {
        doTestAddCollectionOfBooksWithProxy(false);
    }

    public void doTestAddCollectionOfBooksWithProxy(boolean addHeader) {
        String address = "http://localhost:" + PORT;
        MultipartStore client = JAXRSClientFactory.create(address, MultipartStore.class);

        if (addHeader) {
            WebClient.client(client).header("Content-Type", "multipart/mixed;type=application/xml");
        }

        List<Book> books = new ArrayList<>();
        books.add(new Book("CXF 1", 1L));
        books.add(new Book("CXF 2", 2L));
        List<Book> books2 = addHeader ? client.addBooks(books) : client.addBooksWithoutHeader(books);
        assertNotSame(books, books2);
        assertEquals(2, books2.size());
        assertEquals(books.get(0).getId(), books2.get(0).getId());
        assertEquals(books.get(1).getId(), books2.get(1).getId());
    }

    @Test
    public void testXopWebClient() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/xop";
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        bean.setProperties(Collections.singletonMap(org.apache.cxf.message.Message.MTOM_ENABLED,
                                                    (Object)"true"));
        WebClient client = bean.createWebClient();
        WebClient.getConfig(client).getInInterceptors().add(new LoggingInInterceptor());
        WebClient.getConfig(client).getOutInterceptors().add(new LoggingOutInterceptor());
        WebClient.getConfig(client).getRequestContext().put("support.type.as.multipart",
            "true");
        client.type("multipart/related").accept("multipart/related");

        XopType xop = new XopType();
        xop.setName("xopName");
        InputStream is =
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/book.xsd");
        byte[] data = IOUtils.readBytesFromStream(is);
        xop.setAttachinfo(new DataHandler(new ByteArrayDataSource(data, "application/octet-stream")));
        xop.setAttachInfoRef(new DataHandler(new ByteArrayDataSource(data, "application/octet-stream")));

        String bookXsd = IOUtils.readStringFromStream(getClass().getResourceAsStream(
            "/org/apache/cxf/systest/jaxrs/resources/book.xsd"));
        xop.setAttachinfo2(bookXsd.getBytes());

        xop.setImage(getImage("/org/apache/cxf/systest/jaxrs/resources/java.jpg"));

        XopType xop2 = client.post(xop, XopType.class);

        String bookXsdOriginal = IOUtils.readStringFromStream(getClass().getResourceAsStream(
                "/org/apache/cxf/systest/jaxrs/resources/book.xsd"));
        String bookXsd2 = IOUtils.readStringFromStream(xop2.getAttachinfo().getInputStream());
        assertEquals(bookXsdOriginal, bookXsd2);
        String bookXsdRef = IOUtils.readStringFromStream(xop2.getAttachInfoRef().getInputStream());
        assertEquals(bookXsdOriginal, bookXsdRef);

        String ctString =
            client.getResponse().getMetadata().getFirst("Content-Type").toString();
        MediaType mt = MediaType.valueOf(ctString);
        Map<String, String> params = mt.getParameters();
        assertEquals(4, params.size());
        assertNotNull(params.get("boundary"));
        assertNotNull(params.get("type"));
        assertNotNull(params.get("start"));
        assertNotNull(params.get("start-info"));
    }

    private Image getImage(String name) throws Exception {
        return ImageIO.read(getClass().getResource(name));
    }

    @Test
    public void testNullableParamsMultipartAnnotation() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/testnullpart";
        doTestNullPart(address);
    }

    @Test
    public void testNullableParamsFormParamAnnotation() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/testnullpartFormParam";
        doTestNullPart(address);
    }

    private void doTestNullPart(String address) throws Exception {
        WebClient client = WebClient.create(address);
        client.type("multipart/form-data").accept("text/plain");
        List<Attachment> atts = new LinkedList<>();
        atts.add(new Attachment("somepart", "text/plain", "hello there"));
        Response r = client.postCollection(atts, Attachment.class);
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("nobody home", IOUtils.readStringFromStream((InputStream)r.getEntity()));
    }

    @Test
    public void testNullableParamsPrimitive() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/testnullpartprimitive";
        WebClient client = WebClient.create(address);
        client.type("multipart/form-data").accept("text/plain");
        List<Attachment> atts = new LinkedList<>();
        atts.add(new Attachment("somepart", "text/plain", "hello there"));
        Response r = client.postCollection(atts, Attachment.class);
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals((Integer)0, Integer.valueOf(IOUtils.readStringFromStream((InputStream)r.getEntity())));
    }


    @Test
    public void testAddBookJaxbJsonImageWebClientMixed() throws Exception {
        Map<String, String> params =
            doTestAddBookJaxbJsonImageWebClient("multipart/mixed");
        assertEquals(1, params.size());
        assertNotNull(params.get("boundary"));

    }

    @Test
    public void testAddBookJaxbJsonImageWebClientRelated() throws Exception {
        Map<String, String> params =
            doTestAddBookJaxbJsonImageWebClient("multipart/related");
        assertEquals(3, params.size());
        assertNotNull(params.get("boundary"));
        assertNotNull(params.get("type"));
        assertNotNull(params.get("start"));
    }

    @Test
    public void testAddBookJaxbJsonImageWebClientRelated2() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jaxbimagejson";
        WebClient client = WebClient.create(address);
        WebClient.getConfig(client).getOutInterceptors().add(new LoggingOutInterceptor());
        WebClient.getConfig(client).getInInterceptors().add(new LoggingInInterceptor());
        client.type("multipart/mixed").accept("multipart/mixed");

        Book jaxb = new Book("jaxb", 1L);
        Book json = new Book("json", 2L);
        InputStream is1 =
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg");
        Map<String, Object> objects = new LinkedHashMap<>();

        MultivaluedMap<String, String> headers = new MetadataMap<>();
        headers.putSingle("Content-Type", "application/xml");
        headers.putSingle("Content-ID", "theroot");
        headers.putSingle("Content-Transfer-Encoding", "customxml");
        Attachment attJaxb = new Attachment(headers, jaxb);

        headers = new MetadataMap<>();
        headers.putSingle("Content-Type", "application/json");
        headers.putSingle("Content-ID", "thejson");
        headers.putSingle("Content-Transfer-Encoding", "customjson");
        Attachment attJson = new Attachment(headers, json);

        headers = new MetadataMap<>();
        headers.putSingle("Content-Type", "application/octet-stream");
        headers.putSingle("Content-ID", "theimage");
        headers.putSingle("Content-Transfer-Encoding", "customstream");
        Attachment attIs = new Attachment(headers, is1);

        objects.put(MediaType.APPLICATION_XML, attJaxb);
        objects.put(MediaType.APPLICATION_JSON, attJson);
        objects.put(MediaType.APPLICATION_OCTET_STREAM, attIs);

        Collection<? extends Attachment> coll = client.postAndGetCollection(objects, Attachment.class);
        List<Attachment> result = new ArrayList<>(coll);
        Book jaxb2 = readBookFromInputStream(result.get(0).getDataHandler().getInputStream());
        assertEquals("jaxb", jaxb2.getName());
        assertEquals(1L, jaxb2.getId());
        Book json2 = readJSONBookFromInputStream(result.get(1).getDataHandler().getInputStream());
        assertEquals("json", json2.getName());
        assertEquals(2L, json2.getId());
        InputStream is2 = result.get(2).getDataHandler().getInputStream();
        byte[] image1 = IOUtils.readBytesFromStream(
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg"));
        byte[] image2 = IOUtils.readBytesFromStream(is2);
        assertArrayEquals(image1, image2);
    }

    @Test
    public void testAddBookJsonImageStream() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jsonimagestream";
        WebClient client = WebClient.create(address);
        WebClient.getConfig(client).getOutInterceptors().add(new LoggingOutInterceptor());
        WebClient.getConfig(client).getInInterceptors().add(new LoggingInInterceptor());
        client.type("multipart/mixed").accept("multipart/mixed");

        Book json = new Book("json", 1L);
        InputStream is1 =
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg");
        Map<String, Object> objects = new LinkedHashMap<>();

        MultivaluedMap<String, String> headers = new MetadataMap<>();
        headers.putSingle("Content-Type", "application/json");
        headers.putSingle("Content-ID", "thejson");
        headers.putSingle("Content-Transfer-Encoding", "customjson");
        Attachment attJson = new Attachment(headers, json);

        headers = new MetadataMap<>();
        headers.putSingle("Content-Type", "application/octet-stream");
        headers.putSingle("Content-ID", "theimage");
        headers.putSingle("Content-Transfer-Encoding", "customstream");
        Attachment attIs = new Attachment(headers, is1);

        objects.put(MediaType.APPLICATION_JSON, attJson);
        objects.put(MediaType.APPLICATION_OCTET_STREAM, attIs);

        Collection<? extends Attachment> coll = client.postAndGetCollection(objects, Attachment.class);
        List<Attachment> result = new ArrayList<>(coll);
        assertEquals(2, result.size());
        Book json2 = readJSONBookFromInputStream(result.get(0).getDataHandler().getInputStream());
        assertEquals("json", json2.getName());
        assertEquals(1L, json2.getId());
        InputStream is2 = result.get(1).getDataHandler().getInputStream();
        byte[] image1 = IOUtils.readBytesFromStream(
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg"));
        byte[] image2 = IOUtils.readBytesFromStream(is2);
        assertArrayEquals(image1, image2);
    }

    private Map<String, String> doTestAddBookJaxbJsonImageWebClient(String multipartType) throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jaxbjsonimage";
        WebClient client = WebClient.create(address);
        WebClient.getConfig(client).getInInterceptors().add(new LoggingInInterceptor());
        client.type(multipartType).accept(multipartType);

        Book jaxb = new Book("jaxb", 1L);
        Book json = new Book("json", 2L);
        InputStream is1 =
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg");
        Map<String, Object> objects = new LinkedHashMap<>();
        objects.put(MediaType.APPLICATION_XML, jaxb);
        objects.put(MediaType.APPLICATION_JSON, json);
        objects.put(MediaType.APPLICATION_OCTET_STREAM, is1);
        Collection<? extends Attachment> coll = client.postAndGetCollection(objects, Attachment.class);
        List<Attachment> result = new ArrayList<>(coll);
        Book jaxb2 = readBookFromInputStream(result.get(0).getDataHandler().getInputStream());
        assertEquals("jaxb", jaxb2.getName());
        assertEquals(1L, jaxb2.getId());
        Book json2 = readJSONBookFromInputStream(result.get(1).getDataHandler().getInputStream());
        assertEquals("json", json2.getName());
        assertEquals(2L, json2.getId());
        InputStream is2 = result.get(2).getDataHandler().getInputStream();
        byte[] image1 = IOUtils.readBytesFromStream(
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg"));
        byte[] image2 = IOUtils.readBytesFromStream(is2);
        assertArrayEquals(image1, image2);

        String ctString =
            client.getResponse().getMetadata().getFirst("Content-Type").toString();
        MediaType mt = MediaType.valueOf(ctString);
        return mt.getParameters();
    }

    @Test
    public void testGetBookJaxbJsonProxy() throws Exception {
        String address = "http://localhost:" + PORT;
        MultipartStore client = JAXRSClientFactory.create(address, MultipartStore.class);

        Map<String, Book> map = client.getBookJaxbJson();
        List<Book> result = new ArrayList<>(map.values());
        Book jaxb = result.get(0);
        assertEquals("jaxb", jaxb.getName());
        assertEquals(1L, jaxb.getId());
        Book json = result.get(1);
        assertEquals("json", json.getName());
        assertEquals(2L, json.getId());

        String contentType =
            WebClient.client(client).getResponse().getMetadata().getFirst("Content-Type").toString();
        MediaType mt = MediaType.valueOf(contentType);
        assertEquals("multipart", mt.getType());
        assertEquals("mixed", mt.getSubtype());
    }

    @Test
    public void testGetBookJsonProxy() throws Exception {
        String address = "http://localhost:" + PORT;
        MultipartStore client = JAXRSClientFactory.create(address, MultipartStore.class);

        Map<String, Book> map = client.getBookJson();
        List<Book> result = new ArrayList<>(map.values());
        assertEquals(1, result.size());
        Book json = result.get(0);
        assertEquals("json", json.getName());
        assertEquals(1L, json.getId());

        String contentType =
            WebClient.client(client).getResponse().getMetadata().getFirst("Content-Type").toString();
        MediaType mt = MediaType.valueOf(contentType);
        assertEquals("multipart", mt.getType());
        assertEquals("mixed", mt.getSubtype());
    }

    @Test
    public void testGetBookJaxbJsonProxy2() throws Exception {
        String address = "http://localhost:" + PORT;
        MultipartStore client = JAXRSClientFactory.create(address, MultipartStore.class);

        Map<String, Object> map = client.getBookJaxbJsonObject();
        List<Object> result = new ArrayList<>(map.values());
        assertEquals(2, result.size());
        assertTrue(((Attachment)result.get(0)).getContentType().toString().contains("application/xml"));
        assertTrue(((Attachment)result.get(1)).getContentType().toString().contains("application/json"));
    }

    @Test
    public void testAddBookJaxbJsonImageAttachments() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jaxbimagejson";
        WebClient client = WebClient.create(address);
        client.type("multipart/mixed").accept("multipart/mixed");

        Book jaxb = new Book("jaxb", 1L);
        Book json = new Book("json", 2L);
        InputStream is1 =
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg");
        List<Attachment> objects = new ArrayList<>();
        objects.add(new Attachment("<theroot>", MediaType.APPLICATION_XML, jaxb));
        objects.add(new Attachment("thejson", MediaType.APPLICATION_JSON, json));
        objects.add(new Attachment("theimage", MediaType.APPLICATION_OCTET_STREAM, is1));
        Collection<? extends Attachment> coll = client.postAndGetCollection(objects, Attachment.class);
        List<Attachment> result = new ArrayList<>(coll);
        Book jaxb2 = readBookFromInputStream(result.get(0).getDataHandler().getInputStream());
        assertEquals("jaxb", jaxb2.getName());
        assertEquals(1L, jaxb2.getId());
        Book json2 = readJSONBookFromInputStream(result.get(1).getDataHandler().getInputStream());
        assertEquals("json", json2.getName());
        assertEquals(2L, json2.getId());
        InputStream is2 = result.get(2).getDataHandler().getInputStream();
        byte[] image1 = IOUtils.readBytesFromStream(
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg"));
        byte[] image2 = IOUtils.readBytesFromStream(is2);
        assertArrayEquals(image1, image2);
    }

    @Test
    public void testAddGetJaxbBooksWebClient() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jaxbonly";
        WebClient client = WebClient.create(address);

        client.type("multipart/mixed;type=application/xml").accept("multipart/mixed");

        Book b = new Book("jaxb", 1L);
        Book b2 = new Book("jaxb2", 2L);
        List<Book> books = new ArrayList<>();
        books.add(b);
        books.add(b2);
        Collection<? extends Book> coll = client.postAndGetCollection(books, Book.class);
        List<Book> result = new ArrayList<>(coll);
        Book jaxb = result.get(0);
        assertEquals("jaxb", jaxb.getName());
        assertEquals(1L, jaxb.getId());
        Book jaxb2 = result.get(1);
        assertEquals("jaxb2", jaxb2.getName());
        assertEquals(2L, jaxb2.getId());
    }

    @Test
    public void testAddGetImageWebClient() throws Exception {
        InputStream is1 =
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg");
        String address = "http://localhost:" + PORT + "/bookstore/books/image";
        WebClient client = WebClient.create(address);
        client.type("multipart/mixed").accept("multipart/mixed");
        WebClient.getConfig(client).getRequestContext().put("support.type.as.multipart",
            "true");
        InputStream is2 = client.post(is1, InputStream.class);
        byte[] image1 = IOUtils.readBytesFromStream(
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg"));
        byte[] image2 = IOUtils.readBytesFromStream(is2);
        assertArrayEquals(image1, image2);

    }

    @Test
    public void testUploadImageFromForm() throws Exception {
        InputStream is1 =
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg");
        String address = "http://localhost:" + PORT + "/bookstore/books/formimage";
        WebClient client = WebClient.create(address);
        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        conduit.getClient().setReceiveTimeout(1000000);
        conduit.getClient().setConnectionTimeout(1000000);
        client.type("multipart/form-data").accept("multipart/form-data");

        ContentDisposition cd = new ContentDisposition("attachment;filename=java.jpg");
        MultivaluedMap<String, String> headers = new MetadataMap<>();
        headers.putSingle("Content-ID", "image");
        headers.putSingle("Content-Disposition", cd.toString());
        headers.putSingle("Content-Location", "http://host/bar");
        headers.putSingle("custom-header", "custom");
        Attachment att = new Attachment(is1, headers);

        MultipartBody body = new MultipartBody(att);
        MultipartBody body2 = client.post(body, MultipartBody.class);
        InputStream is2 = body2.getRootAttachment().getDataHandler().getInputStream();
        byte[] image1 = IOUtils.readBytesFromStream(
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg"));
        byte[] image2 = IOUtils.readBytesFromStream(is2);
        assertArrayEquals(image1, image2);
        ContentDisposition cd2 = body2.getRootAttachment().getContentDisposition();
        assertEquals("attachment;filename=java.jpg", cd2.toString());
        assertEquals("java.jpg", cd2.getParameter("filename"));
        assertEquals("http://host/location", body2.getRootAttachment().getHeader("Content-Location"));
    }

    @Test
    public void testUploadFileWithSemicolonName() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/file/semicolon";
        WebClient client = WebClient.create(address);
        client.type("multipart/form-data").accept("text/plain");

        ContentDisposition cd = new ContentDisposition("attachment;name=\"a\";filename=\"a;txt\"");
        MultivaluedMap<String, String> headers = new MetadataMap<>();
        headers.putSingle("Content-Disposition", cd.toString());
        Attachment att = new Attachment(new ByteArrayInputStream("file name with semicolon".getBytes()),
                                        headers);

        MultipartBody body = new MultipartBody(att);
        String partContent = client.post(body, String.class);
        assertEquals("file name with semicolon, filename:" + "a;txt", partContent);
    }

    @Test
    public void testUploadImageFromForm2() throws Exception {
        File file =
            new File(getClass().getResource("/org/apache/cxf/systest/jaxrs/resources/java.jpg")
                               .toURI().getPath());
        String address = "http://localhost:" + PORT + "/bookstore/books/formimage2";
        WebClient client = WebClient.create(address);
        client.type("multipart/form-data").accept("multipart/form-data");
        WebClient.getConfig(client).getRequestContext().put("support.type.as.multipart",
                                                            "true");
        MultipartBody body2 = client.post(file, MultipartBody.class);
        InputStream is2 = body2.getRootAttachment().getDataHandler().getInputStream();
        byte[] image1 = IOUtils.readBytesFromStream(
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg"));
        byte[] image2 = IOUtils.readBytesFromStream(is2);
        assertArrayEquals(image1, image2);
        ContentDisposition cd2 = body2.getRootAttachment().getContentDisposition();
        assertEquals("form-data;name=file;filename=java.jpg", cd2.toString());
        assertEquals("java.jpg", cd2.getParameter("filename"));
    }

    @Test
    public void testMultipartRequestNoBody() throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("http://localhost:" + PORT + "/bookstore/books/image");
        String ct = "multipart/mixed";
        post.setHeader("Content-Type", ct);

        try {
            CloseableHttpResponse response = client.execute(post);
            assertEquals(400, response.getStatusLine().getStatusCode());
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }

    @Test
    public void testMultipartRequestTooLarge() throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("http://localhost:" + PORT + "/bookstore/books/image");
        String ct = "multipart/mixed";
        post.setHeader("Content-Type", ct);

        HttpEntity entity = MultipartEntityBuilder.create()
            .addPart("image", new ByteArrayBody(new byte[1024 * 11], "testfile.png"))
            .build();

        post.setEntity(entity);

        try {
            CloseableHttpResponse response = client.execute(post);
            assertEquals(413, response.getStatusLine().getStatusCode());
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }

    @Test
    public void testMultipartRequestTooLargeManyParts() throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("http://localhost:" + PORT + "/bookstore/books/image");
        String ct = "multipart/mixed";
        post.setHeader("Content-Type", ct);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        HttpEntity entity = builder.addPart("image", new ByteArrayBody(new byte[1024 * 9], "testfile.png"))
                                   .addPart("image", new ByteArrayBody(new byte[1024 * 11], "testfile2.png")).build();

        post.setEntity(entity);

        try {
            CloseableHttpResponse response = client.execute(post);
            assertEquals(413, response.getStatusLine().getStatusCode());
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }

    // The large Content Disposition header will be rejected here
    @Test
    public void testLargeHeader() throws Exception {
        InputStream is1 =
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg");
        String address = "http://localhost:" + PORT + "/bookstore/books/image";
        WebClient client = WebClient.create(address);
        client.type("multipart/mixed").accept("multipart/mixed");
        WebClient.getConfig(client).getRequestContext().put("support.type.as.multipart",
            "true");

        StringBuilder sb = new StringBuilder(100500);
        sb.append("form-data;");
        for (int i = 0; i < 10000; i++) {
            sb.append("aaaaaaaaaa");
        }

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("Content-ID", "root");
        headers.putSingle("Content-Type", "application/octet-stream");
        headers.putSingle("Content-Disposition", sb.toString());
        DataHandler handler = new DataHandler(new InputStreamDataSource(is1, "application/octet-stream"));

        Attachment att = new Attachment(headers, handler, null);
        Response response = client.post(att);
        assertEquals(response.getStatus(), 413);

        client.close();
    }

    // The Content Disposition header will be accepted here, even though it is larger than the default,
    // as we have configured a larger value on the service side
    @Test
    public void testLargerThanDefaultHeader() throws Exception {
        InputStream is1 =
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg");
        String address = "http://localhost:" + PORT + "/bookstore/books/image";
        WebClient client = WebClient.create(address);
        client.type("multipart/mixed").accept("multipart/mixed");
        WebClient.getConfig(client).getRequestContext().put("support.type.as.multipart",
            "true");

        StringBuilder sb = new StringBuilder(64);
        sb.append("form-data;");
        for (int i = 0; i < 35; i++) {
            sb.append("aaaaaaaaaa");
        }

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("Content-ID", "root");
        headers.putSingle("Content-Type", "application/octet-stream");
        headers.putSingle("Content-Disposition", sb.toString());
        DataHandler handler = new DataHandler(new InputStreamDataSource(is1, "application/octet-stream"));

        Attachment att = new Attachment(headers, handler, null);
        try (Response response = client.post(att)) {
            assertEquals(response.getStatus(), 200);
        }

        client.close();
    }

    @Test
    public void testUpdateBookMultipart() {
        final WebTarget target = ClientBuilder
            .newClient()
            .target("http://localhost:" + PORT + "/bookstore");

        final MultipartBody builder = new MultipartBody(Arrays.asList(
                new AttachmentBuilder()
                    .id("name")
                    .contentDisposition(new ContentDisposition("form-data; name=\"name\""))
                    .object("The Book")
                    .build()
            ));
        
        try (Response response = target
                .path("1")
                .request()
                .put(Entity.entity(builder, MediaType.MULTIPART_FORM_DATA))) {
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(Book.class).getName(), equalTo("The Book"));
        }
    }

    private void doAddBook(String address, String resourceName, int status) throws Exception {
        doAddBook("multipart/related", address, resourceName, status);
    }

    private void doAddBook(String type, String address, String resourceName, int status) throws Exception {
        InputStream is =
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/" + resourceName);
        doAddBook(type, address, is, status);
    }
    private void doAddBook(String type, String address, InputStream is, int status) throws Exception {

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(address);

        String ct = type + "; type=\"text/xml\"; " + "start=\"rootPart\"; "
            + "boundary=\"----=_Part_4_701508.1145579811786\"";
        post.setHeader("Content-Type", ct);

        post.setEntity(new InputStreamEntity(is));

        try {
            CloseableHttpResponse response = client.execute(post);
            final String body = EntityUtils.toString(response.getEntity());
            assertThat("Unexpected status code for response:" + response, 
                response.getStatusLine().getStatusCode(), equalTo(status));
            if (status == 200) {
                InputStream expected = getClass().getResourceAsStream("resources/expected_add_book.txt");
                assertEquals(stripXmlInstructionIfNeeded(getStringFromInputStream(expected)),
                             stripXmlInstructionIfNeeded(body));
            }
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }

    private void doAddFormBook(String address, String resourceName, int status) throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(address);

        String ct = "multipart/form-data; boundary=bqJky99mlBWa-ZuqjC53mG6EzbmlxB";
        post.setHeader("Content-Type", ct);
        InputStream is =
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/" + resourceName);
        post.setEntity(new InputStreamEntity(is));

        try {
            CloseableHttpResponse response = client.execute(post);
            assertEquals(status, response.getStatusLine().getStatusCode());
            if (status == 200) {
                InputStream expected = getClass().getResourceAsStream("resources/expected_add_book.txt");
                assertEquals(stripXmlInstructionIfNeeded(getStringFromInputStream(expected)),
                             stripXmlInstructionIfNeeded(EntityUtils.toString(response.getEntity())));
            }
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }

    private String getStringFromInputStream(InputStream in) throws Exception {
        return IOUtils.toString(in);
    }

    private Book readBookFromInputStream(InputStream is) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Unmarshaller u = c.createUnmarshaller();
        return (Book)u.unmarshal(is);
    }

    private Book readJSONBookFromInputStream(InputStream is) throws Exception {
        JSONProvider<Book> provider = new JSONProvider<>();
        return provider.readFrom(Book.class, Book.class, new Annotation[]{},
                                 MediaType.APPLICATION_JSON_TYPE, null, is);

    }
    private String stripXmlInstructionIfNeeded(String str) {
        if (str != null && str.startsWith("<?xml")) {
            int index = str.indexOf("?>");
            str = str.substring(index + 2);
        }
        return str;
    }

    /**
     * Windows attachment handling
     */
    private static class NoCarriageReturnFileInputStream extends InputStream {
        private final InputStream is;

        NoCarriageReturnFileInputStream(InputStream is) {
            this.is = is;
        }

        @Override
        public int read() throws IOException {
            int c;

            do {
                c = is.read();
            } while(c != -1 && c == 13);

            return c;
        }
    }
}
