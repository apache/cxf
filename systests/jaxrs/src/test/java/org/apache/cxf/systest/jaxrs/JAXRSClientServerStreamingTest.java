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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamWriter;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.staxutils.CachingXmlEventWriter;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSClientServerStreamingTest extends AbstractClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);

    public static class Server extends AbstractServerTestServerBase {

        @Override
        protected org.apache.cxf.endpoint.Server createServer(Bus bus) throws Exception {
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStore.class);
            sf.setResourceProvider(BookStore.class,
                                   new SingletonResourceProvider(new BookStore()));
            JAXBElementProvider<?> p1 = new JAXBElementProvider<>();
            p1.setEnableBuffering(true);
            p1.setEnableStreaming(true);

            JAXBElementProvider<?> p2 = new CustomJaxbProvider();
            p2.setProduceMediaTypes(Collections.singletonList("text/xml"));

            List<Object> providers = new ArrayList<>();
            providers.add(p1);
            providers.add(p2);
            sf.setProviders(providers);
            sf.setAddress("http://localhost:" + PORT + "/");
            Map<String, Object> properties = new HashMap<>();
            properties.put("org.apache.cxf.serviceloader-context", "true");
            sf.setProperties(properties);
            return sf.create();
        }

        public static void main(String[] args) throws Exception {
            new Server().start();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly",
                   launchServer(Server.class));
    }

    @Test
    public void testGetBook123() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/books/123",
                      "application/xml", 200);
    }

    @Test
    public void testGetBook123Fail() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/books/text/xml/123");
        wc.accept("text/xml");
        wc.header("fail-write", "yes");
        Response r = wc.get();
        assertEquals(500, r.getStatus());
    }

    @Test
    public void testGetBookUsingStaxWriter() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/books/text/xml/123",
                      "text/xml", 200);
    }

    private void getAndCompare(String address,
                               String acceptType,
                               int expectedStatus) throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(address);
        get.setHeader("Accept", acceptType);
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

    private Book readBook(InputStream is) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Unmarshaller u = c.createUnmarshaller();
        return (Book)u.unmarshal(is);
    }

    @Ignore
    public static class CustomJaxbProvider extends JAXBElementProvider<Object> {
        @Override
        protected XMLStreamWriter getStreamWriter(Object obj, OutputStream os, MediaType mt) {
            if (mt.equals(MediaType.TEXT_XML_TYPE)) {
                return new CachingXmlEventWriter();
            }
            throw new RuntimeException();
        }
        @Override
        public void writeTo(Object obj, Class<?> cls, Type genericType, Annotation[] anns,
            MediaType m, MultivaluedMap<String, Object> headers, OutputStream os) throws IOException {
            List<String> failHeaders = getContext().getHttpHeaders().getRequestHeader("fail-write");
            if (failHeaders != null && !failHeaders.isEmpty()) {
                os.write("fail".getBytes());
                throw new IOException();
            }
            super.writeTo(obj, cls, genericType, anns, m, headers, os);
        }
    }
}
