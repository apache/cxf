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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSClientServerUserResourceTest extends AbstractBusClientServerTestBase {

    @Ignore
    public static class Server extends AbstractBusTestServerBase {        

        protected void run() {
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setAddress("http://localhost:9080/");
            
            UserResource ur = new UserResource();
            ur.setName(BookStoreNoAnnotations.class.getName());
            ur.setPath("/bookstoreNoAnnotations");
            UserOperation op = new UserOperation();
            op.setPath("/books/{id}");
            op.setName("getBook");
            op.setVerb("GET");
            op.setParameters(Collections.singletonList(new Parameter(ParameterType.PATH, "id")));
            
            UserOperation op2 = new UserOperation();
            op2.setPath("/books/{id}/chapter");
            op2.setName("getBookChapter");
            op2.setParameters(Collections.singletonList(new Parameter(ParameterType.PATH, "id")));
            
            List<UserOperation> ops = new ArrayList<UserOperation>();
            ops.add(op);
            ops.add(op2);
            
            ur.setOperations(ops);
            
            UserResource ur2 = new UserResource();
            ur2.setName(ChapterNoAnnotations.class.getName());
            UserOperation op3 = new UserOperation();
            op3.setPath("/");
            op3.setName("getItself");
            op3.setVerb("GET");
            ur2.setOperations(Collections.singletonList(op3));
            
            sf.setModelBeans(ur, ur2);
            
            sf.create();

        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(Server.class));
    }
    
    @Test
    public void testGetBook123() throws Exception {
        getAndCompare("http://localhost:9080/bookstoreNoAnnotations/books/123",
                      "application/xml", 200);
    }
    
    @Test
    public void testGetChapter() throws Exception {
        
        getAndCompareChapter("http://localhost:9080/bookstoreNoAnnotations/books/123/chapter",
                      "application/xml", 200);
    }
    
    private void getAndCompare(String address, 
                               String acceptType,
                               int expectedStatus) throws Exception {
        GetMethod get = new GetMethod(address);
        get.setRequestHeader("Accept", acceptType);
        HttpClient httpClient = new HttpClient();
        try {
            int result = httpClient.executeMethod(get);
            assertEquals(expectedStatus, result);
            Book book = readBook(get.getResponseBodyAsStream());
            assertEquals(123, book.getId());
            assertEquals("CXF in Action", book.getName());
        } finally {
            get.releaseConnection();
        }
    }
    
    private void getAndCompareChapter(String address, 
                               String acceptType,
                               int expectedStatus) throws Exception {
        GetMethod get = new GetMethod(address);
        get.setRequestHeader("Accept", acceptType);
        HttpClient httpClient = new HttpClient();
        try {
            int result = httpClient.executeMethod(get);
            assertEquals(expectedStatus, result);
            Chapter c = readChapter(get.getResponseBodyAsStream());
            assertEquals(1, c.getId());
            assertEquals("chapter 1", c.getTitle());
        } finally {
            get.releaseConnection();
        }
    }
    
    
    
    private Book readBook(InputStream is) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Unmarshaller u = c.createUnmarshaller();
        return (Book)u.unmarshal(is);
    }
    
    private Chapter readChapter(InputStream is) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Chapter.class});
        Unmarshaller u = c.createUnmarshaller();
        return (Chapter)u.unmarshal(is);
    }
}
