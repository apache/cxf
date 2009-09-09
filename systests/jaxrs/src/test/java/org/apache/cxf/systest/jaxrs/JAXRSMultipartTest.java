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

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.provider.JSONProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSMultipartTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(MultipartServer.class));
    }
    
    @Test
    public void testBookAsRootAttachmentStreamSource() throws Exception {
        String address = "http://localhost:9085/bookstore/books/stream";
        doAddBook(address, "attachmentData", 200);               
    }
    
    @Test
    public void testBookAsRootAttachmentStreamSourceNoContentId() throws Exception {
        String address = "http://localhost:9085/bookstore/books/stream";
        doAddBook(address, "attachmentData3", 200);               
    }
    
    @Test
    public void testBookAsRootAttachmentInputStream() throws Exception {
        String address = "http://localhost:9085/bookstore/books/istream";
        doAddBook(address, "attachmentData", 200);               
    }
    
    @Test
    public void testBookAsMessageContextDataHandler() throws Exception {
        String address = "http://localhost:9085/bookstore/books/mchandlers";
        doAddBook(address, "attachmentData", 200);               
    }
    
    @Test
    public void testBookAsMessageContextAttachments() throws Exception {
        String address = "http://localhost:9085/bookstore/books/attachments";
        doAddBook(address, "attachmentData", 200);               
    }
    
    @Test
    public void testBookAsMessageContextAttachment() throws Exception {
        String address = "http://localhost:9085/bookstore/books/attachment";
        doAddBook(address, "attachmentData", 200);               
    }
    
    @Test
    public void testAddBookAsRootAttachmentJAXB() throws Exception {
        String address = "http://localhost:9085/bookstore/books/jaxb";
        doAddBook(address, "attachmentData", 200);               
    }
    
    @Test
    public void testAddBookAsDataSource() throws Exception {
        String address = "http://localhost:9085/bookstore/books/dsource";
        doAddBook(address, "attachmentData", 200);               
    }
    
    @Test
    public void testAddBookAsDataSource2() throws Exception {
        String address = "http://localhost:9085/bookstore/books/dsource2";
        doAddBook(address, "attachmentData", 200);               
    }
    
    @Test
    public void testAddBookAsBody() throws Exception {
        String address = "http://localhost:9085/bookstore/books/body";
        doAddBook(address, "attachmentData", 200);               
    }
    
    @Test
    public void testAddBookFormData() throws Exception {
        String address = "http://localhost:9085/bookstore/books/form";
        doAddBook("multipart/form-data", address, "attachmentForm", 200);               
    }
    
    @Test
    public void testAddBookFormParam() throws Exception {
        String address = "http://localhost:9085/bookstore/books/formparam";
        doAddBook("multipart/form-data", address, "attachmentForm", 200);               
    }
    
    @Test
    public void testAddBookFormBody() throws Exception {
        String address = "http://localhost:9085/bookstore/books/formbody";
        doAddBook("multipart/form-data", address, "attachmentForm", 200);               
    }
    
    @Test
    public void testAddBookFormBody2() throws Exception {
        String address = "http://localhost:9085/bookstore/books/formbody2";
        doAddBook("multipart/form-data", address, "attachmentForm", 200);               
    }
    
    @Test
    public void testAddBookFormParamBean() throws Exception {
        String address = "http://localhost:9085/bookstore/books/formparambean";
        doAddBook("multipart/form-data", address, "attachmentForm", 200);               
    }
    
    @Test
    public void testAddBookAsJAXB2() throws Exception {
        String address = "http://localhost:9085/bookstore/books/jaxb2";
        doAddBook(address, "attachmentData", 200);               
    }
    
    @Test
    public void testAddBookAsListOfAttachments() throws Exception {
        String address = "http://localhost:9085/bookstore/books/listattachments";
        doAddBook(address, "attachmentData", 200);               
    }
    
    @Test
    public void testAddBookAsListOfStreams() throws Exception {
        String address = "http://localhost:9085/bookstore/books/lististreams";
        doAddBook(address, "attachmentData", 200);               
    }
    
    @Test
    public void testAddBookAsJAXBJSON() throws Exception {
        String address = "http://localhost:9085/bookstore/books/jaxbjson";
        doAddBook(address, "attachmentData2", 200);               
    }
    
    @Test
    public void testAddBookAsJAXBJSONMixed() throws Exception {
        String address = "http://localhost:9085/bookstore/books/jaxbjson";
        doAddBook("multipart/mixed", address, "attachmentData2", 200);               
    }
    
    @Test
    public void testConsumesMismatch() throws Exception {
        String address = "http://localhost:9085/bookstore/books/mismatch1";
        doAddBook(address, "attachmentData2", 415);               
    }
    
    @Test
    public void testConsumesMismatch2() throws Exception {
        String address = "http://localhost:9085/bookstore/books/mismatch2";
        doAddBook(address, "attachmentData2", 415);               
    }
    
    @Test
    public void testAddBookAsDataHandler() throws Exception {
        String address = "http://localhost:9085/bookstore/books/dhandler";
        doAddBook(address, "attachmentData", 200);               
    }
    
    @Test
    public void testAddBookWebClient() {
        InputStream is1 = 
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/add_book.txt");
        String address = "http://localhost:9085/bookstore/books/jaxb";
        WebClient client = WebClient.create(address);
        client.type("multipart/related;type=text/xml").accept("text/xml");
        Book book = client.post(is1, Book.class);
        assertEquals("CXF in Action - 2", book.getName());
    }
    
    @Test
    public void testAddBookJaxbJsonImageWebClient() throws Exception {
        String address = "http://localhost:9085/bookstore/books/jaxbjsonimage";
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
        Collection<Attachment> coll = client.postAndGetCollection(objects, Attachment.class);
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
        String address = "http://localhost:9085/bookstore/books/jaxbimagejson";
        WebClient client = WebClient.create(address);
        client.type("multipart/mixed").accept("multipart/mixed");
        
        Book jaxb = new Book("jaxb", 1L);
        Book json = new Book("json", 2L);
        InputStream is1 = 
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/java.jpg");
        List<Attachment> objects = new ArrayList<Attachment>();
        objects.add(new Attachment("theroot", MediaType.APPLICATION_XML, jaxb));
        objects.add(new Attachment("thejson", MediaType.APPLICATION_JSON, json));
        objects.add(new Attachment("theimage", MediaType.APPLICATION_OCTET_STREAM, is1));
        Collection<Attachment> coll = client.postAndGetCollection(objects, Attachment.class);
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
        String address = "http://localhost:9085/bookstore/books/jaxbonly";
        WebClient client = WebClient.create(address);
        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        conduit.getClient().setReceiveTimeout(1000000);
        conduit.getClient().setConnectionTimeout(1000000);
        client.type("multipart/mixed;type=application/xml").accept("multipart/mixed");
        
        Book b = new Book("jaxb", 1L);
        Book b2 = new Book("jaxb2", 2L);
        List<Book> books = new ArrayList<Book>();
        books.add(b);
        books.add(b2);
        Collection<Book> coll = client.postAndGetCollection(books, Book.class);
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
        String address = "http://localhost:9085/bookstore/books/image";
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
        String address = "http://localhost:9085/bookstore/books/formimage";
        WebClient client = WebClient.create(address);
        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        conduit.getClient().setReceiveTimeout(1000000);
        conduit.getClient().setConnectionTimeout(1000000);
        client.type("multipart/form-data").accept("multipart/form-data");
        
        ContentDisposition cd = new ContentDisposition("attachment;filename=java.jpg");
        Attachment att = new Attachment("image", is1, cd);
        
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
    }
    
    @Test
    public void testUploadImageFromForm2() throws Exception {
        File file = 
            new File(getClass().getResource("/org/apache/cxf/systest/jaxrs/resources/java.jpg").getFile());
        String address = "http://localhost:9085/bookstore/books/formimage";
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
