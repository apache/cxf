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
import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.imageio.ImageIO;
import javax.mail.util.ByteArrayDataSource;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.provider.JSONProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSMultipartTest extends AbstractBusClientServerTestBase {
    public static final String PORT = MultipartServer.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(MultipartServer.class));
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
    public void testBookAsMessageContextDataHandler() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/mchandlers";
        doAddBook(address, "attachmentData", 200);               
    }
    
    @Test
    public void testBookAsMessageContextAttachments() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/attachments";
        doAddBook(address, "attachmentData", 200);               
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
    public void testBookJSONFormFiles() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/filesform";
        doAddFormBook(address, "attachmentFormJsonFiles", 200);               
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
    public void testAddBookAsJAXBJSON() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jaxbjson";
        doAddBook(address, "attachmentData2", 200);               
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
        
        List<Book> books = new ArrayList<Book>();
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
        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        conduit.getClient().setReceiveTimeout(1000000);
        conduit.getClient().setConnectionTimeout(1000000);
        
        client.type("multipart/related").accept("multipart/related");
        XopType xop = new XopType();
        xop.setName("xopName");
        InputStream is = 
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/book.xsd");
        byte[] data = IOUtils.readBytesFromStream(is);
        xop.setAttachinfo(new DataHandler(new ByteArrayDataSource(data, "application/octet-stream")));
        
        String bookXsd = IOUtils.readStringFromStream(getClass().getResourceAsStream(
            "/org/apache/cxf/systest/jaxrs/resources/book.xsd"));
        xop.setAttachinfo2(bookXsd.getBytes());
     
        if (Boolean.getBoolean("java.awt.headless")) {
            System.out.println("Running headless. Ignoring an Image property.");
        } else {
            xop.setImage(getImage("/org/apache/cxf/systest/jaxrs/resources/java.jpg"));
        }
        
        XopType xop2 = client.post(xop, XopType.class);
        
        String bookXsdOriginal = IOUtils.readStringFromStream(getClass().getResourceAsStream(
                "/org/apache/cxf/systest/jaxrs/resources/book.xsd"));
        String bookXsd2 = IOUtils.readStringFromStream(xop2.getAttachinfo().getInputStream());        
        assertEquals(bookXsdOriginal, bookXsd2);
    }
    
    private Image getImage(String name) throws Exception {
        return ImageIO.read(getClass().getResource(name));
    }
    
    @Test
    public void testAddBookJaxbJsonImageWebClient() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jaxbjsonimage";
        WebClient client = WebClient.create(address);
        client.type("multipart/mixed").accept("multipart/mixed");
        
        Book jaxb = new Book("jaxb", 1L);
        Book json = new Book("json", 2L);
        InputStream is1 = 
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg");
        Map<String, Object> objects = new LinkedHashMap<String, Object>();
        objects.put(MediaType.APPLICATION_XML, jaxb);
        objects.put(MediaType.APPLICATION_JSON, json);
        objects.put(MediaType.APPLICATION_OCTET_STREAM, is1);
        Collection<? extends Attachment> coll = client.postAndGetCollection(objects, Attachment.class);
        List<Attachment> result = new ArrayList<Attachment>(coll);
        Book jaxb2 = readBookFromInputStream(result.get(0).getDataHandler().getInputStream());
        assertEquals("jaxb", jaxb2.getName());
        assertEquals(1L, jaxb2.getId());
        Book json2 = readJSONBookFromInputStream(result.get(1).getDataHandler().getInputStream());
        assertEquals("json", json2.getName());
        assertEquals(2L, json2.getId());
        InputStream is2 = (InputStream)result.get(2).getDataHandler().getInputStream();
        byte[] image1 = IOUtils.readBytesFromStream(
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg"));
        byte[] image2 = IOUtils.readBytesFromStream(is2);
        assertTrue(Arrays.equals(image1, image2));
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
        List<Attachment> objects = new ArrayList<Attachment>();
        objects.add(new Attachment("<theroot>", MediaType.APPLICATION_XML, jaxb));
        objects.add(new Attachment("thejson", MediaType.APPLICATION_JSON, json));
        objects.add(new Attachment("theimage", MediaType.APPLICATION_OCTET_STREAM, is1));
        Collection<? extends Attachment> coll = client.postAndGetCollection(objects, Attachment.class);
        List<Attachment> result = new ArrayList<Attachment>(coll);
        Book jaxb2 = readBookFromInputStream(result.get(0).getDataHandler().getInputStream());
        assertEquals("jaxb", jaxb2.getName());
        assertEquals(1L, jaxb2.getId());
        Book json2 = readJSONBookFromInputStream(result.get(1).getDataHandler().getInputStream());
        assertEquals("json", json2.getName());
        assertEquals(2L, json2.getId());
        InputStream is2 = (InputStream)result.get(2).getDataHandler().getInputStream();
        byte[] image1 = IOUtils.readBytesFromStream(
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg"));
        byte[] image2 = IOUtils.readBytesFromStream(is2);
        assertTrue(Arrays.equals(image1, image2));
    }
    
    @Test
    public void testAddGetJaxbBooksWebClient() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jaxbonly";
        WebClient client = WebClient.create(address);
        
        client.type("multipart/mixed;type=application/xml").accept("multipart/mixed");
        
        Book b = new Book("jaxb", 1L);
        Book b2 = new Book("jaxb2", 2L);
        List<Book> books = new ArrayList<Book>();
        books.add(b);
        books.add(b2);
        Collection<? extends Book> coll = client.postAndGetCollection(books, Book.class);
        List<Book> result = new ArrayList<Book>(coll);
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
        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        conduit.getClient().setReceiveTimeout(1000000);
        conduit.getClient().setConnectionTimeout(1000000);
        client.type("multipart/mixed").accept("multipart/mixed");
        InputStream is2 = client.post(is1, InputStream.class);
        byte[] image1 = IOUtils.readBytesFromStream(
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg"));
        byte[] image2 = IOUtils.readBytesFromStream(is2);
        assertTrue(Arrays.equals(image1, image2));
        
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
        MultivaluedMap<String, String> headers = new MetadataMap<String, String>();
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
        assertTrue(Arrays.equals(image1, image2));
        ContentDisposition cd2 = body2.getRootAttachment().getContentDisposition();
        assertEquals("attachment;filename=java.jpg", cd2.toString());
        assertEquals("java.jpg", cd2.getParameter("filename"));
        assertEquals("http://host/location", body2.getRootAttachment().getHeader("Content-Location"));
    }
    
    @Test
    public void testUploadImageFromForm2() throws Exception {
        File file = 
            new File(getClass().getResource("/org/apache/cxf/systest/jaxrs/resources/java.jpg").getFile());
        String address = "http://localhost:" + PORT + "/bookstore/books/formimage2";
        WebClient client = WebClient.create(address);
        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        conduit.getClient().setReceiveTimeout(1000000);
        conduit.getClient().setConnectionTimeout(1000000);
        client.type("multipart/form-data").accept("multipart/form-data");
        
        MultipartBody body2 = client.post(file, MultipartBody.class);
        InputStream is2 = body2.getRootAttachment().getDataHandler().getInputStream();
        byte[] image1 = IOUtils.readBytesFromStream(
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg"));
        byte[] image2 = IOUtils.readBytesFromStream(is2);
        assertTrue(Arrays.equals(image1, image2));
        ContentDisposition cd2 = body2.getRootAttachment().getContentDisposition();
        assertEquals("attachment;filename=java.jpg", cd2.toString());
        assertEquals("java.jpg", cd2.getParameter("filename"));
    }
    
    @Test
    public void testMultipartRequestNoBody() throws Exception { 
        PostMethod post = new PostMethod("http://localhost:" + PORT + "/bookstore/books/image");
        String ct = "multipart/mixed";
        post.setRequestHeader("Content-Type", ct);
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(400, result);
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }
    
    private void doAddBook(String address, String resourceName, int status) throws Exception {
        doAddBook("multipart/related", address, resourceName, status);
    }

    private void doAddBook(String type, String address, String resourceName, int status) throws Exception {
        PostMethod post = new PostMethod(address);
        
        String ct = type + "; type=\"text/xml\"; " + "start=\"rootPart\"; "
            + "boundary=\"----=_Part_4_701508.1145579811786\"";
        post.setRequestHeader("Content-Type", ct);
        InputStream is = 
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/" + resourceName);
        RequestEntity entity = new InputStreamRequestEntity(is);
        post.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(status, result);
            if (status == 200) {
                InputStream expected = getClass().getResourceAsStream("resources/expected_add_book.txt");
                assertEquals(getStringFromInputStream(expected), post.getResponseBodyAsString());
            }
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }
    
    private void doAddFormBook(String address, String resourceName, int status) throws Exception {
        PostMethod post = new PostMethod(address);
        
        String ct = "multipart/form-data; boundary=bqJky99mlBWa-ZuqjC53mG6EzbmlxB";
        post.setRequestHeader("Content-Type", ct);
        InputStream is = 
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/" + resourceName);
        RequestEntity entity = new InputStreamRequestEntity(is);
        post.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(status, result);
            if (status == 200) {
                InputStream expected = getClass().getResourceAsStream("resources/expected_add_book.txt");
                assertEquals(getStringFromInputStream(expected), post.getResponseBodyAsString());
            }
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }
    
    private String getStringFromInputStream(InputStream in) throws Exception {        
        CachedOutputStream bos = new CachedOutputStream();
        IOUtils.copy(in, bos);
        in.close();
        bos.close();
        return bos.getOut().toString();        
    }

    private Book readBookFromInputStream(InputStream is) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Unmarshaller u = c.createUnmarshaller();
        return (Book)u.unmarshal(is);
    }
    
    @SuppressWarnings("unchecked")
    private Book readJSONBookFromInputStream(InputStream is) throws Exception {
        JSONProvider provider = new JSONProvider();
        return (Book)provider.readFrom((Class)Book.class, Book.class, new Annotation[]{}, 
                                 MediaType.APPLICATION_JSON_TYPE, null, is);
        
    }
}
