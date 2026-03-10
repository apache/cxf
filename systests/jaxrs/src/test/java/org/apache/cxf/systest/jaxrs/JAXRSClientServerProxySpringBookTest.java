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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JAXRSClientServerProxySpringBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerProxySpring.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(BookServerProxySpring.class, true));
        final Bus bus = createStaticBus();
        // Make sure default JSON-P/JSON-B providers are not loaded
        bus.setProperty(ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);
    }

    @Test
    public void testGetWadlResourcesInfo() throws Exception {
        WebClient client = WebClient.create("http://localhost:" + PORT + "/test" + "?_wadl&_type=xml");
        WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(10000000);
        Document doc = StaxUtils.read(new InputStreamReader(client.get(InputStream.class), StandardCharsets.UTF_8));
        Element root = doc.getDocumentElement();
        assertEquals(WadlGenerator.WADL_NS, root.getNamespaceURI());
        assertEquals("application", root.getLocalName());
        List<Element> resourcesEls = DOMUtils.getChildrenWithName(root,
                                                                  WadlGenerator.WADL_NS, "resources");
        assertEquals(1, resourcesEls.size());
        Element resourcesEl = resourcesEls.get(0);
        assertEquals("http://localhost:" + PORT + "/test/", resourcesEl.getAttribute("base"));
        List<Element> resourceEls =
            DOMUtils.getChildrenWithName(resourcesEl,
                                         WadlGenerator.WADL_NS, "resource");
        assertEquals(3, resourceEls.size());
    }

    @Test
    public void testGetBookNotFound() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/test/bookstore/books/12345";
        URL url = new URL(endpointAddress);
        HttpURLConnection connect = (HttpURLConnection)url.openConnection();
        connect.addRequestProperty("Accept", "text/plain,application/xml");
        assertEquals(500, connect.getResponseCode());
        InputStream in = connect.getErrorStream();
        assertNotNull(in);

        InputStream expected = getClass()
            .getResourceAsStream("resources/expected_get_book_notfound_mapped.txt");

        assertEquals("Exception is not mapped correctly",
                     stripXmlInstructionIfNeeded(getStringFromInputStream(expected).trim()),
                     stripXmlInstructionIfNeeded(getStringFromInputStream(in).trim()));
    }

    @Test
    public void testGetThatBook123() throws Exception {
        getBook("http://localhost:" + PORT + "/test/bookstorestorage/thosebooks/123");
    }

    @Test
    public void testGetThatBookSingleton() throws Exception {
        getBook("http://localhost:" + PORT + "/test/4/bookstore/books/123");
        getBook("http://localhost:" + PORT + "/test/4/bookstore/books/123");
    }

    @Test
    public void testGetThatBookInterfaceSingleton() throws Exception {
        getBook("http://localhost:" + PORT + "/test/4/bookstorestorage/thosebooks/123");
    }

    @Test
    public void testGetThatBookPrototype() throws Exception {
        getBook("http://localhost:" + PORT + "/test/5/bookstore/books/123");
    }

    @Test
    public void testGetThatBookInterfacePrototype() throws Exception {

        URL url = new URL("http://localhost:" + PORT + "/test/5/bookstorestorage/thosebooks/123");
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Content-Type", "*/*");
        connect.addRequestProperty("Accept", "application/xml");
        connect.addRequestProperty("SpringProxy", "true");
        InputStream in = connect.getInputStream();

        InputStream expected = getClass()
            .getResourceAsStream("resources/expected_get_book123.txt");
        assertEquals(stripXmlInstructionIfNeeded(getStringFromInputStream(expected)),
                     stripXmlInstructionIfNeeded(getStringFromInputStream(in)));
        String ct = connect.getHeaderField("Content-Type");
        assertEquals("application/xml;a=b", ct);
    }

    @Test
    public void testEchoBook() throws Exception {

        URL url = new URL("http://localhost:" + PORT + "/test/5/bookstorestorage/thosebooks");
        WebClient wc = WebClient.create(url.toString(),
                                        Collections.singletonList(new CustomJaxbElementProvider()));
        Response r = wc.type("application/xml").post(new Book("proxy", 333L));
        Book book = r.readEntity(Book.class);
        assertEquals(333L, book.getId());
        String ct = r.getHeaderString("Content-Type");
        assertEquals("application/xml;a=b", ct);
    }

    @Test
    public void testGetThatBookInterface2Prototype() throws Exception {
        getBook("http://localhost:" + PORT + "/test/6/bookstorestorage/thosebooks/123");
    }

    @Test
    public void testGetThatBook123UserResource() throws Exception {
        getBook("http://localhost:" + PORT + "/test/2/bookstore/books/123");
    }

    @Test
    public void testGetThatBook123UserResourceInterface() throws Exception {
        getBook("http://localhost:" + PORT + "/test/3/bookstore2/books/123");
    }

    private void getBook(String endpointAddress) throws Exception {
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Content-Type", "*/*");
        connect.addRequestProperty("Accept", "application/xml");
        connect.addRequestProperty("SpringProxy", "true");
        InputStream in = connect.getInputStream();

        InputStream expected = getClass()
            .getResourceAsStream("resources/expected_get_book123.txt");
        assertEquals(stripXmlInstructionIfNeeded(getStringFromInputStream(expected)),
                     stripXmlInstructionIfNeeded(getStringFromInputStream(in)));
    }

    @Test
    public void testGetThatBookOverloaded() throws Exception {
        getBook("http://localhost:" + PORT + "/test/bookstorestorage/thosebooks/123/123");
    }

    @Test
    public void testGetThatBookOverloaded2() throws Exception {
        getBook("http://localhost:" + PORT + "/test/bookstorestorage/thosebooks");
    }

    @Test
    public void testGetBook123() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/test/bookstore/books/123";
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "application/json");
        InputStream in = connect.getInputStream();

        InputStream expected = getClass()
            .getResourceAsStream("resources/expected_get_book123json.txt");

        assertEquals(getStringFromInputStream(expected), getStringFromInputStream(in));
    }
    @Test
    public void testGetName() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/test/v1/names/1";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/json");
        String name = wc.get(String.class);
        assertEquals("{\"name\":\"Barry\"}", name);
    }
    @Test
    public void testPutName() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/test/v1/names/1";
        WebClient wc = WebClient.create(endpointAddress);
        wc.type("application/json").accept("application/json");
        String id = wc.put(null, String.class);
        assertEquals("1", id);
    }

    @Test
    public void testGetBookWithRequestScope() {
        // the BookStore method which will handle this request depends on the injected HttpHeaders
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/test/request/bookstore/booksecho2");
        wc.type("text/plain").accept("text/plain");
        wc.header("CustomHeader", "custom-header");
        String value = wc.post("CXF", String.class);
        assertEquals("CXF", value);
        assertEquals("custom-header", wc.getResponse().getMetadata().getFirst("CustomHeader"));
    }

    private String getStringFromInputStream(InputStream in) throws Exception {
        return IOUtils.toString(in);
    }

    private String stripXmlInstructionIfNeeded(String str) {
        if (str != null && str.startsWith("<?xml")) {
            int index = str.indexOf("?>");
            str = str.substring(index + 2);
        }
        return str;
    }
}
