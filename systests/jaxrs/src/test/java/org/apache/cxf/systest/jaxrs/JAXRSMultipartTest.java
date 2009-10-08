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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSMultipartTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(MultipartServer.class, true));
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

}
