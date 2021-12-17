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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSClientServerUserResourceTest extends AbstractClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);

    public static class Server extends AbstractServerTestServerBase {

        @Override
        protected org.apache.cxf.endpoint.Server createServer(Bus bus) throws Exception {
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setAddress("http://localhost:" + PORT + "/");

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

            List<UserOperation> ops = new ArrayList<>();
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

            String modelRef = "classpath:/org/apache/cxf/systest/jaxrs/resources/resources2.xml";
            sf.setModelRefWithServiceClass(modelRef, BookStoreNoAnnotationsInterface.class);
            sf.setServiceBean(new BookStoreNoAnnotationsImpl());
            return sf.create();
        }

        public static void main(String[] args) throws Exception {
            new Server().start();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(Server.class, true));
    }

    @Test
    public void testGetBook123() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstoreNoAnnotations/books/123",
                      "application/xml", 200);
    }

    @Test
    public void testGetBookInterface123() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore2/books/123",
                      "application/xml", 200);
    }

    @Test
    public void testGetChapter() throws Exception {

        getAndCompareChapter("http://localhost:" + PORT + "/bookstoreNoAnnotations/books/123/chapter",
                      "application/xml", 200);
    }

    private void getAndCompare(String address,
                               String acceptType,
                               int expectedStatus) throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(address);
        get.addHeader("Accept", acceptType);
        try {
            CloseableHttpResponse response = client.execute(get);
            assertEquals(expectedStatus, response.getStatusLine().getStatusCode());
            Book book = readBook(response.getEntity().getContent());
            assertEquals(123, book.getId());
            assertEquals("CXF in Action", book.getName());
        } finally {
            get.releaseConnection();
        }
    }

    private void getAndCompareChapter(String address,
                               String acceptType,
                               int expectedStatus) throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(address);
        get.addHeader("Accept", acceptType);
        try {
            CloseableHttpResponse response = client.execute(get);
            assertEquals(expectedStatus, response.getStatusLine().getStatusCode());
            Chapter c = readChapter(response.getEntity().getContent());
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
