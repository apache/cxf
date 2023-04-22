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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JAXRSRequestDispatcherTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerRequestDispatch.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerRequestDispatch.class, true));
    }

    @Test
    public void testGetBookHTML() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/bookstore1/books/html/123";
        doTestGetBookHTML(endpointAddress);
    }
    @Test
    public void testGetBookHTMLInclude() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/bookstore5/books/html/123";
        doTestGetBookHTML(endpointAddress);
    }

    private void doTestGetBookHTML(String endpointAddress) throws Exception {

        WebClient client = WebClient.create(endpointAddress)
            .accept(MediaType.TEXT_HTML);

        XMLSource source = client.get(XMLSource.class);
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("xhtml", "http://www.w3.org/1999/xhtml");
        namespaces.put("books", "http://www.w3.org/books");
        String value = source.getValue("xhtml:html/xhtml:body/xhtml:ul/books:bookTag", namespaces);
        assertEquals("CXF Rocks", value);
        Object contentType = client.getResponse().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        assertNotNull("Content-Type should be present", contentType);
        assertEquals(MediaType.TEXT_HTML, contentType.toString());
        assertEquals(MediaType.TEXT_HTML_TYPE, client.getResponse().getMediaType());
    }

    @Test
    public void testGetBookJSPRequestScope() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/bookstore2/books/html/123";
        WebClient client = WebClient.create(endpointAddress)
            .accept(MediaType.TEXT_HTML);

        String data = client.get(String.class);
        assertTrue(data.contains("<h1>Request Book 123</h1>"));
        assertTrue(data.contains("<books:bookName>CXF in Action</books:bookName>"));

    }

    @Test
    public void testGetBookJSPSessionScope() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/bookstore3/books/html/456";
        WebClient client = WebClient.create(endpointAddress)
            .accept(MediaType.TEXT_HTML);

        String data = client.get(String.class);
        assertTrue(data.contains("<h1>Session Book 456</h1>"));
        assertTrue(data.contains("<books:bookName>CXF in Action</books:bookName>"));
    }

    @Test
    public void testGetBookHTMLFromDefaultServlet() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/bookstore4/books/html/123";
        WebClient client = WebClient.create(endpointAddress)
            .accept(MediaType.TEXT_HTML);

        XMLSource source = client.get(XMLSource.class);
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("xhtml", "http://www.w3.org/1999/xhtml");
        namespaces.put("books", "http://www.w3.org/books");
        String value = source.getValue("xhtml:html/xhtml:body/xhtml:ul/books:bookTag", namespaces);
        assertEquals("CXF Rocks", value);
    }
    @Test
    public void testGetBookHTMLFromWelcomeList() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/welcome";
        doTestGetBookHTMLFromWelcomeList(endpointAddress);
    }

    @Test
    public void testGetBookHTMLFromWelcomeList2() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/the";
        doTestGetBookHTMLFromWelcomeList(endpointAddress);
    }

    @Test
    public void testGetBookHTMLFromStaticWelcomeFile() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/welcome2";
        doTestGetBookHTMLFromWelcomeList(endpointAddress);
    }

    @Test
    public void testGetTextWelcomeFile() throws Exception {
        String address = "http://localhost:" + PORT + "/welcome2/welcome.txt";
        WebClient client = WebClient.create(address)
            .accept(MediaType.TEXT_PLAIN);
        String welcome = client.get(String.class);
        assertEquals("Welcome", welcome);
    }

    @Test
    public void testGetBookHTMLFromEnsureResponseStreamIsUnclosed() throws Exception {
        final ClientHttpConnectionOutInterceptor interceptor = new ClientHttpConnectionOutInterceptor();

        String endpointAddress = "http://localhost:" + PORT + "/the/bookstore4/books/html/123";
        WebClient client = WebClient.create(endpointAddress).accept(MediaType.TEXT_HTML);
        WebClient.getConfig(client).getOutInterceptors().add(interceptor);

        XMLSource source = client.get(XMLSource.class);
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("xhtml", "http://www.w3.org/1999/xhtml");
        namespaces.put("books", "http://www.w3.org/books");
        String value = source.getValue("xhtml:html/xhtml:body/xhtml:ul/books:bookTag", namespaces);
        assertEquals("CXF Rocks", value);
        
        assertThat(interceptor.checkAllClosed(), is(false));
    }
    
    @Test
    public void testGetBookHTMLFromEnsureResponseStreamIsAutoClosed() throws Exception {
        final ClientHttpConnectionOutInterceptor interceptor = new ClientHttpConnectionOutInterceptor();
        final Map<String, Object> properties = Collections.singletonMap("response.stream.auto.close", true);

        String endpointAddress = "http://localhost:" + PORT + "/the/bookstore4/books/html/123";
        WebClient client = WebClient.create(endpointAddress, properties).accept(MediaType.TEXT_HTML);
        WebClient.getConfig(client).getOutInterceptors().add(interceptor);

        final String source = client.get(String.class);
        assertThat(source, containsString("CXF Rocks"));
        assertThat(interceptor.checkAllClosed(), is(true));
    }
    
    private void doTestGetBookHTMLFromWelcomeList(String address) throws Exception {
        WebClient client = WebClient.create(address)
            .accept(MediaType.TEXT_HTML);

        XMLSource source = client.get(XMLSource.class);
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("xhtml", "http://www.w3.org/1999/xhtml");
        namespaces.put("books", "http://www.w3.org/books");
        String value = source.getValue("xhtml:html/xhtml:body/xhtml:ul/books:bookTag", namespaces);
        assertEquals("Welcome to CXF", value);
    }
    
}
