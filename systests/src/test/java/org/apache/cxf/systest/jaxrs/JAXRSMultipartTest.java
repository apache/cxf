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
                   launchServer(MultipartServer.class));
    }
    
    @Test
    public void testBookAsRootAttachmentStreamSource() throws Exception {
        String address = "http://localhost:9080/bookstore/books/stream";
        doAddBook(address);               
    }
    
    @Test
    public void testBookAsRootAttachmentInputStream() throws Exception {
        String address = "http://localhost:9080/bookstore/books/istream";
        doAddBook(address);               
    }
    
    @Test
    public void testBookAsMessageContextDataHandler() throws Exception {
        String address = "http://localhost:9080/bookstore/books/mchandlers";
        doAddBook(address);               
    }
    
    @Test
    public void testAddBookAsRootAttachmentJAXB() throws Exception {
        String address = "http://localhost:9080/bookstore/books/jaxb";
        doAddBook(address);               
    }
    
    @Test
    public void testAddBookAsDataSource() throws Exception {
        String address = "http://localhost:9080/bookstore/books/dsource";
        doAddBook(address);               
    }
    
    @Test
    public void testAddBookAsDataSource2() throws Exception {
        String address = "http://localhost:9080/bookstore/books/dsource2";
        doAddBook(address);               
    }
    
    @Test
    public void testAddBookAsJAXB2() throws Exception {
        String address = "http://localhost:9080/bookstore/books/jaxb2";
        doAddBook(address);               
    }
    
    @Test
    public void testAddBookAsDataHandler() throws Exception {
        String address = "http://localhost:9080/bookstore/books/dhandler";
        doAddBook(address);               
    }
    
    private void doAddBook(String address) throws Exception {
        PostMethod post = new PostMethod(address);
        
        String ct = "multipart/related; type=\"text/xml\"; " + "start=\"rootPart\"; "
            + "boundary=\"----=_Part_4_701508.1145579811786\"";
        post.setRequestHeader("Content-Type", ct);
        InputStream is = 
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/attachmentData");
        RequestEntity entity = new InputStreamRequestEntity(is);
        post.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(200, result);
            
            InputStream expected = getClass().getResourceAsStream("resources/expected_add_book.txt");
            
            assertEquals(getStringFromInputStream(expected), post.getResponseBodyAsString());
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
