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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.staxutils.CachingXmlEventWriter;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSClientServerStreamingTest extends AbstractBusClientServerTestBase {

    @Ignore
    public static class Server extends AbstractBusTestServerBase {        

        protected void run() {
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStore.class);
            sf.setResourceProvider(BookStore.class,
                                   new SingletonResourceProvider(new BookStore()));
            JAXBElementProvider p1 = new JAXBElementProvider();
            p1.setEnableBuffering(true);
            p1.setEnableStreaming(true);
            
            JAXBElementProvider p2 = new CustomJaxbProvider();
            p2.setProduceMediaTypes(Collections.singletonList("text/xml"));
            
            List<Object> providers = new ArrayList<Object>();
            providers.add(p1);
            providers.add(p2);
            sf.setProviders(providers);
            sf.setAddress("http://localhost:9080/");
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("org.apache.cxf.serviceloader-context", "true");
            sf.setProperties(properties);
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
        getAndCompare("http://localhost:9080/bookstore/books/123",
                      "application/xml", 200);
    }
    
    @Test
    public void testGetBookUsingStaxWriter() throws Exception {
        getAndCompare("http://localhost:9080/bookstore/books/123",
                      "text/xml", 200);
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
    
    private Book readBook(InputStream is) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Unmarshaller u = c.createUnmarshaller();
        return (Book)u.unmarshal(is);
    }
    
    @Ignore
    public static class CustomJaxbProvider extends JAXBElementProvider {
        @Override
        protected XMLStreamWriter getStreamWriter(Object obj, OutputStream os, MediaType mt) {
            if (mt.equals(MediaType.TEXT_XML_TYPE)) {
                return new CachingXmlEventWriter();
            } else {
                throw new RuntimeException();
            }
        }
    }
}
