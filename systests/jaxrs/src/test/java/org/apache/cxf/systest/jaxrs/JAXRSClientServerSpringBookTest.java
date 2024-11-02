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
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.cxf.Bus;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.aegis.AegisElementProvider;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.ws.commons.schema.constants.Constants;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JAXRSClientServerSpringBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerSpring.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(BookServerSpring.class, true));
        final Bus bus = createStaticBus();
        // Make sure default JSON-P/JSON-B providers are not loaded
        bus.setProperty(ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);
    }

    @Test
    public void testGetGenericBook() throws Exception {
        String baseAddress = "http://localhost:" + PORT + "/the/thebooks8/books";
        WebClient wc = WebClient.create(baseAddress);
        Long id = wc.type("application/xml").accept("text/plain").post(new Book("CXF", 1L), Long.class);
        assertEquals(Long.valueOf(1), id);
        Book book = wc.replaceHeader("Accept", "application/xml").query("id", 1L).get(Book.class);
        assertEquals("CXF", book.getName());
    }

    @Test
    public void testGetDocuments() throws Exception {
        String baseAddress = "http://localhost:" + PORT + "/the/thedocs/resource/doc";
        WebClient wc = WebClient.create(baseAddress);
        Response r = wc.accept("application/json").get();
        assertEquals("[{\"t\":\"doc\"}]", r.readEntity(String.class));
    }

    @Test
    public void testGetBookWebEx() throws Exception {
        final String address = "http://localhost:" + PORT + "/the/thebooks/bookstore/books/webex";
        doTestGetBookWebEx(address);

    }

    @Test
    public void testGetBookText() throws Exception {
        final String address = "http://localhost:" + PORT + "/the/thebooks/bookstore/books/text";
        WebClient wc = WebClient.create(address).accept("text/*");
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000);
        assertEquals(406, wc.get().getStatus());

    }

    @Test
    public void testGetServicesPageNotFound() throws Exception {
        final String address = "http://localhost:" + PORT + "/the/services;a=b";
        WebClient wc = WebClient.create(address).accept("text/*");
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000);
        assertEquals(404, wc.get().getStatus());
    }
    @Test
    public void testGetServicesPage() throws Exception {
        final String address = "http://localhost:" + PORT + "/the/services";
        WebClient wc = WebClient.create(address).accept("text/*");
        String s = wc.get(String.class);
        assertTrue(s.contains("href=\"/the/services/?stylesheet=1\""));
        assertTrue(s.contains("<title>CXF - Service list</title>"));
        assertTrue(s.contains("<a href=\"http://localhost:" + PORT + "/the/"));
    }
    @Test
    public void testGetServicesPageWithServletPatternMatchOnly() throws Exception {
        final String address = "http://localhost:" + PORT + "/the/;a=b";
        WebClient wc = WebClient.create(address).accept("text/*");
        String s = wc.get(String.class);
        assertTrue(s.contains("href=\"/the/?stylesheet=1\""));
        assertTrue(s.contains("<title>CXF - Service list</title>"));
        assertFalse(s.contains(";a=b"));
        assertTrue(s.contains("<a href=\"http://localhost:" + PORT + "/the/"));
    }

    @Test
    public void testGetServicesPageWithServletPatternMatchOnly2() throws Exception {
        final String address = "http://localhost:" + PORT + "/services;a=b;/list;a=b/;a=b";
        WebClient wc = WebClient.create(address).accept("text/*");
        String s = wc.get(String.class);
        assertTrue(s.contains("href=\"/services/list/?stylesheet=1\""));
        assertTrue(s.contains("<title>CXF - Service list</title>"));
        assertFalse(s.contains(";a=b"));
        assertTrue(s.contains("<a href=\"http://localhost:" + PORT + "/services/list/"));
    }

    @Test
    public void testStaticResourcesWithRedirectQueryCheck() throws Exception {
        final String address = "http://localhost:" + PORT + "/services/?.html";
        WebClient wc = WebClient.create(address).accept("text/*");
        String s = wc.get(String.class);
        // Check we don't have a directory listing
        assertFalse(s.contains("META-INF"));
    }

    @Test
    public void testEchoBookForm() throws Exception {
        String address = "http://localhost:" + PORT + "/bus/thebooksform/bookform";
        doTestEchoBookForm(address);
    }
    @Test
    public void testEchoBookForm2() throws Exception {
        String address = "http://localhost:" + PORT + "/bus/thebooksform/bookform2";
        doTestEchoBookForm(address);
    }
    @Test
    public void testEchoBookForm3() throws Exception {
        String address = "http://localhost:" + PORT + "/bus/thebooksform/bookform3";
        doTestEchoBookForm(address);
    }
    @Test
    public void testEchoBookForm4() throws Exception {
        String address = "http://localhost:" + PORT + "/bus/thebooksform/bookform4";
        doTestEchoBookForm(address);
    }

    @Test
    public void testEchoBookForm5() throws Exception {
        String address = "http://localhost:" + PORT + "/bus/thebooksform/bookform5";
        doTestEchoBookForm(address);
    }

    private void doTestEchoBookForm(String address) throws Exception {
        WebClient wc = WebClient.create(address);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);

        Book b =
            wc.form(new Form().param("name", "CXFForm").param("id", "125"))
                .readEntity(Book.class);
        assertEquals("CXFForm", b.getName());
        assertEquals(125L, b.getId());
    }
    @Test
    public void testEchoBookFormXml() throws Exception {
        String address = "http://localhost:" + PORT + "/bus/thebooksform/bookform";
        WebClient wc = WebClient.create(address);
        Book b =
            wc.type("application/xml").post(new Book("CXFFormXml", 125L))
                .readEntity(Book.class);
        assertEquals("CXFFormXml", b.getName());
        assertEquals(125L, b.getId());
    }

    @Test
    public void testGetBookWebEx4() throws Exception {
        final String address = "http://localhost:" + PORT + "/the/thebooks%203/bookstore/books/webex2";
        doTestGetBookWebEx(address);

    }

    private void doTestGetBookWebEx(String address) throws Exception {
        WebClient wc = WebClient.create(address);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        try {
            wc.accept("text/plain", "application/json").get(Book.class);
            fail("InternalServerErrorException is expected");
        } catch (InternalServerErrorException ex) {
            String errorMessage = ex.getResponse().readEntity(String.class);
            assertEquals("Book web exception", errorMessage);
        }

    }

    @Test
    public void testPostGeneratedBook() throws Exception {
        String baseAddress = "http://localhost:" + PORT + "/the/generated";
        JAXBElementProvider<?> provider = new JAXBElementProvider<>();
        provider.setJaxbElementClassMap(Collections.singletonMap(
                                          "org.apache.cxf.systest.jaxrs.codegen.schema.Book",
                                          "{http://superbooks}thebook"));

        org.apache.cxf.systest.jaxrs.codegen.service.BookStore bookStore =
            JAXRSClientFactory.create(baseAddress,
                org.apache.cxf.systest.jaxrs.codegen.service.BookStore.class,
                Collections.singletonList(provider));

        org.apache.cxf.systest.jaxrs.codegen.schema.Book book =
            new org.apache.cxf.systest.jaxrs.codegen.schema.Book();
        book.setId(123);
        bookStore.addBook(123, book);
        Response r = WebClient.client(bookStore).getResponse();
        assertEquals(204, r.getStatus());
    }

    @Test
    public void testGetWadlFromWadlLocation() throws Exception {
        String address = "http://localhost:" + PORT + "/the/generated";
        WebClient client = WebClient.create(address + "/bookstore" + "?_wadl&_type=xml");
        Document doc = StaxUtils.read(new InputStreamReader(client.get(InputStream.class), StandardCharsets.UTF_8));
        List<Element> resources = checkWadlResourcesInfo(doc, address, "/schemas/book.xsd", 2);
        assertEquals("", resources.get(0).getAttribute("type"));
        String type = resources.get(1).getAttribute("type");
        String resourceTypeAddress = address + "/bookstoreImportResourceType.wadl#bookstoreType";
        assertEquals(resourceTypeAddress, type);

        checkSchemas(address, "/schemas/book.xsd", "/schemas/chapter.xsd", "include");
        checkSchemas(address, "/schemas/chapter.xsd", null, null);

        // check resource type resource
        checkWadlResourcesType(address, resourceTypeAddress, "/schemas/book.xsd");

        String templateRef = null;
        NodeList nd = doc.getChildNodes();
        for (int i = 0; i < nd.getLength(); i++) {
            Node n = nd.item(i);
            if (n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                String piData = ((ProcessingInstruction)n).getData();
                int hRefStart = piData.indexOf("href=\"");
                if (hRefStart > 0) {
                    int hRefEnd = piData.indexOf("\"", hRefStart + 6);
                    templateRef = piData.substring(hRefStart + 6, hRefEnd);
                }
            }
        }
        assertNotNull(templateRef);
        WebClient client2 = WebClient.create(templateRef);
        WebClient.getConfig(client2).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        String template = client2.get(String.class);
        assertNotNull(template);
        assertTrue(template.indexOf("<xsl:stylesheet") != -1);
    }

    @Test
    public void testGetGeneratedWadlWithExternalSchemas() throws Exception {
        String address = "http://localhost:" + PORT + "/the/bookstore";
        checkWadlResourcesInfo(address, address, "/book.xsd", 2);

        checkSchemas(address, "/book.xsd", "/bookid.xsd", "import");
        checkSchemas(address, "/bookid.xsd", null, null);
        checkWadlResourcesInfo(address, address, "/book.xsd", 2);
    }

    @Test
    public void testGetBookISOJson() throws Exception {
        doTestGetBookISO("ISO-8859-1", "1");
    }
    @Test
    public void testGetBookISO2Json() throws Exception {
        doTestGetBookISO("ISO-8859-1", "2");
    }
    @Test
    public void testGetBookISO3Json() throws Exception {
        doTestGetBookISO(null, "1");
    }
    @Test
    public void testGetBookISOXML() throws Exception {
        doTestGetBookISOXML("ISO-8859-1", "1");
    }
    @Test
    public void testGetBookISOXML2() throws Exception {
        doTestGetBookISOXML("ISO-8859-1", "2");
    }
    @Test
    public void testGetBookISOXML3() throws Exception {
        doTestGetBookISOXML(null, "1");
    }

    @Test
    public void testGetBookLink() throws Exception {
        String address = "http://localhost:" + PORT + "/the/bookstore/link";
        WebClient wc = WebClient.create(address);
        Response r = wc.get();
        Link l = r.getLink("self");
        assertEquals("<http://localhost:" + PORT + "/the/bookstore/>;rel=\"self\"",
                     l.toString());
    }
    private void doTestGetBookISO(String charset, String pathSegment) throws Exception {
        String address = "http://localhost:" + PORT + "/the/bookstore/ISO-8859-1/" + pathSegment;
        WebClient wc = WebClient.create(address);
        wc.accept("application/json" + (charset == null ? "" : ";charset=ISO-8859-1"));
        byte[] iso88591bytes = wc.get(byte[].class);
        String helloStringISO88591 = new String(iso88591bytes, "ISO-8859-1");

        String name = helloStringISO88591.substring(
            helloStringISO88591.indexOf("\"name\":\"") + "\"name\":\"".length(),
            helloStringISO88591.lastIndexOf('"'));

        compareNames(name);
    }
    private void doTestGetBookISOXML(String charset, String pathSegment) throws Exception {
        String address = "http://localhost:" + PORT + "/the/bookstore/ISO-8859-1/" + pathSegment;
        WebClient wc = WebClient.create(address);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        wc.accept("application/xml" + (charset == null ? "" : ";charset=ISO-8859-1"));
        byte[] iso88591bytes = wc.get(byte[].class);
        String helloStringISO88591 = new String(iso88591bytes, "ISO-8859-1");

        String name = helloStringISO88591.substring(
            helloStringISO88591.indexOf("<name>") + "<name>".length(),
            helloStringISO88591.indexOf("</name>"));

        compareNames(name);
    }

    private void compareNames(String name) throws Exception  {
        String eWithAcute = "\u00E9";
        String nameUTF16 = "F" + eWithAcute + "lix";
        String nameExpected = new String(nameUTF16.getBytes("ISO-8859-1"), "ISO-8859-1");
        assertEquals(nameExpected, name);
    }

    private void checkSchemas(String address, String schemaSegment,
                              String includedSchema,
                              String refAttrName) throws Exception {
        WebClient client = WebClient.create(address + schemaSegment);
        WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        Document doc = StaxUtils.read(new InputStreamReader(client.get(InputStream.class), StandardCharsets.UTF_8));
        Element root = doc.getDocumentElement();
        assertEquals(Constants.URI_2001_SCHEMA_XSD, root.getNamespaceURI());
        assertEquals("schema", root.getLocalName());
        if (includedSchema != null) {
            List<Element> includeEls = DOMUtils.getChildrenWithName(root,
                                                                    Constants.URI_2001_SCHEMA_XSD,
                                                                    refAttrName);
            assertEquals(1, includeEls.size());
            String href = includeEls.get(0).getAttribute("schemaLocation");
            assertEquals(address + includedSchema, href);
        }

    }

    private void checkWadlResourcesType(String baseURI, String requestTypeURI, String schemaRef) throws Exception {
        WebClient client = WebClient.create(requestTypeURI);
        WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(1000000);

        Document doc = StaxUtils.read(new InputStreamReader(client.get(InputStream.class), StandardCharsets.UTF_8));
        Element root = doc.getDocumentElement();
        assertEquals(WadlGenerator.WADL_NS, root.getNamespaceURI());
        assertEquals("application", root.getLocalName());
        List<Element> grammarEls = DOMUtils.getChildrenWithName(root,
            WadlGenerator.WADL_NS, "grammars");
        assertEquals(1, grammarEls.size());
        List<Element> includeEls = DOMUtils.getChildrenWithName(grammarEls.get(0),
            WadlGenerator.WADL_NS, "include");
        assertEquals(1, includeEls.size());
        String href = includeEls.get(0).getAttribute("href");
        assertEquals(baseURI + schemaRef, href);
        List<Element> resourcesEls = DOMUtils.getChildrenWithName(root,
            WadlGenerator.WADL_NS, "resources");
        assertEquals(0, resourcesEls.size());
        List<Element> resourceTypeEls =
            DOMUtils.getChildrenWithName(root, WadlGenerator.WADL_NS, "resource_type");
        assertEquals(1, resourceTypeEls.size());
    }

    private List<Element> checkWadlResourcesInfo(String baseURI, String requestURI,
                                        String schemaRef, int size) throws Exception {
        WebClient client = WebClient.create(requestURI + "?_wadl&_type=xml");
        Document doc = StaxUtils.read(new InputStreamReader(client.get(InputStream.class), StandardCharsets.UTF_8));
        return checkWadlResourcesInfo(doc, baseURI, schemaRef, size);
    }
    private List<Element> checkWadlResourcesInfo(Document doc, String baseURI, String schemaRef, int size)
        throws Exception {

        Element root = doc.getDocumentElement();
        assertEquals(WadlGenerator.WADL_NS, root.getNamespaceURI());
        assertEquals("application", root.getLocalName());
        List<Element> grammarEls = DOMUtils.getChildrenWithName(root,
                                                                WadlGenerator.WADL_NS, "grammars");
        assertEquals(1, grammarEls.size());
        List<Element> includeEls = DOMUtils.getChildrenWithName(grammarEls.get(0),
                                                                WadlGenerator.WADL_NS, "include");
        assertEquals(1, includeEls.size());
        String href = includeEls.get(0).getAttribute("href");
        assertEquals(baseURI + schemaRef, href);
        List<Element> resourcesEls = DOMUtils.getChildrenWithName(root,
                                                                  WadlGenerator.WADL_NS, "resources");
        assertEquals(1, resourcesEls.size());
        Element resourcesEl = resourcesEls.get(0);
        assertEquals(baseURI, resourcesEl.getAttribute("base"));
        List<Element> resourceEls =
            DOMUtils.getChildrenWithName(resourcesEl,
                                         WadlGenerator.WADL_NS, "resource");
        assertEquals(size, resourceEls.size());
        return resourceEls;
    }

    @Test
    public void testGetBookByUriInfo() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks/bookstore/bookinfo?"
                               + "param1=12&param2=3";
        getBook(endpointAddress, "resources/expected_get_book123json.txt");
    }

    @Test
    public void testGetBookWithEncodedSemicolon() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks/bookstore/semicolon%3B";
        WebClient client = WebClient.create(endpointAddress);
        Book book = client.get(Book.class);
        assertEquals(333L, book.getId());
        assertEquals(";", book.getName());
    }

    @Test
    public void testGetBookWithEncodedSemicolonAndMatrixParam() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks/bookstore/semicolon2%3B;a=b";
        WebClient client = WebClient.create(endpointAddress);
        WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(1000000);
        Book book = client.get(Book.class);
        assertEquals(333L, book.getId());
        assertEquals(";b", book.getName());
    }

    @Test
    @org.junit.Ignore("Fails after removing Xalan")
    public void testGetBookXSLTHtml() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks5/bookstore/books/xslt";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xhtml+xml").path(666).matrix("name2", 2).query("name", "Action - ");
        XMLSource source = wc.get(XMLSource.class);
        source.setBuffering();
        Map<String, String> namespaces = Collections.singletonMap("xhtml", "http://www.w3.org/1999/xhtml");
        Book2 b = source.getNode("xhtml:html/xhtml:body/xhtml:ul/xhtml:Book", namespaces, Book2.class);
        assertEquals(666, b.getId());
        assertEquals("CXF in Action - 2", b.getName());
    }

    @Test
    public void testGetBookByUriInfo2() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks%203/bookstore/bookinfo?"
                               + "param1=12&param2=3";
        getBook(endpointAddress, "resources/expected_get_book123json.txt");
    }

    @Test
    public void testGetBook123() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/bookstore/books/123";
        getBook(endpointAddress, "resources/expected_get_book123json.txt");
        getBook(endpointAddress, "resources/expected_get_book123json.txt",
                "application/vnd.example-com.foo+json");
    }

    @Test
    public void testBookDepthExceededXML() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks9/depth";
        WebClient wc = WebClient.create(endpointAddress);
        Response r = wc.type("application/xml").post(new Book("CXF", 123L));
        assertEquals(413, r.getStatus());
    }

    @Test
    public void testBookDepthExceededXMLStax() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks9stax/depth";
        WebClient wc = WebClient.create(endpointAddress);
        Response r = wc.type("application/xml").post(new Book("CXF", 123L));
        assertEquals(413, r.getStatus());
    }

    @Test
    public void testBookDepthExceededXMLSource() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks9/depth-source";
        WebClient wc = WebClient.create(endpointAddress);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        Response r = wc.type("application/xml").post(new Book("CXF", 123L));
        assertEquals(413, r.getStatus());
    }

    @Test
    public void testBookDepthExceededXMLSourceStax() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks9stax/depth-source";
        WebClient wc = WebClient.create(endpointAddress);
        Response r = wc.type("application/xml").post(new Book("CXF", 123L));
        assertEquals(413, r.getStatus());
    }

    @Test
    public void testBookDepthExceededXMLDom() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks9/depth-dom";
        WebClient wc = WebClient.create(endpointAddress);
        Response r = wc.type("application/xml").post(new Book("CXF", 123L));
        assertEquals(413, r.getStatus());
    }

    @Test
    public void testBookDepthExceededXMLDomStax() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks9stax/depth-dom";
        WebClient wc = WebClient.create(endpointAddress);
        Response r = wc.type("application/xml").post(new Book("CXF", 123L));
        assertEquals(413, r.getStatus());
    }

    @Test
    public void testBookDepthExceededJettison() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks10/depth";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/json").type("application/json");
        Response r = wc.post(new Book("CXF", 123L));
        assertEquals(413, r.getStatus());
    }

    @Test
    public void testTooManyFormParams() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks9/depth-form";
        WebClient wc = WebClient.create(endpointAddress);
        Response r = wc.form(new Form().param("a", "b"));
        assertEquals(204, r.getStatus());
        r = wc.form(new Form().param("a", "b").param("c", "b"));
        assertEquals(413, r.getStatus());
    }


    @Test
    public void testGetBookJsonp() throws Exception {
        String url = "http://localhost:" + PORT + "/the/jsonp/books/123";
        WebClient client = WebClient.create(url);
        client.accept("application/json, application/x-javascript");
        client.query("_jsonp", "callback");
        Response r = client.get();
        assertEquals("application/x-javascript", r.getMetadata().getFirst("Content-Type"));
        assertEquals("callback({\"Book\":{\"id\":123,\"name\":\"CXF in Action\"}});",
                     IOUtils.readStringFromStream((InputStream)r.getEntity()));
    }

    @Test
    public void testGetBookJsonpJackson() throws Exception {
        String url = "http://localhost:" + PORT + "/bus/jsonp2/books/123";
        WebClient client = WebClient.create(url);
        WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(10000000);
        client.accept("application/json, application/x-javascript");
        client.query("_jsonp", "callback");
        Response r = client.get();
        assertEquals("application/x-javascript", r.getMetadata().getFirst("Content-Type"));
        String response = IOUtils.readStringFromStream((InputStream)r.getEntity());
        assertTrue(response.startsWith("callback({\"class\":\"org.apache.cxf.systest.jaxrs.Book\","));
        assertTrue(response.endsWith("});"));
        assertTrue(response.contains("\"id\":123"));
        assertTrue(response.contains("\"name\":\"CXF in Action\""));
    }

    @Test
    public void testGetBookWithoutJsonpCallback() throws Exception {
        String url = "http://localhost:" + PORT + "/the/jsonp/books/123";
        WebClient client = WebClient.create(url);
        client.accept("application/json, application/x-javascript");
        WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        Response r = client.get();
        assertEquals("application/json", r.getMetadata().getFirst("Content-Type"));
        assertEquals("{\"Book\":{\"id\":123,\"name\":\"CXF in Action\"}}",
                     IOUtils.readStringFromStream((InputStream)r.getEntity()));
    }

    @Test
    public void testGetBookAsArray() throws Exception {
        URL url = new URL("http://localhost:" + PORT + "/the/bookstore/books/list/123");
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "application/json");
        InputStream in = connect.getInputStream();

        assertEquals("{\"Books\":{\"books\":[{\"id\":123,\"name\":\"CXF in Action\"}]}}",
                     getStringFromInputStream(in));

    }

    @Test
    public void testGetBookXsiType() throws Exception {
        String address = "http://localhost:" + PORT + "/the/thebooksxsi/bookstore/books/xsitype";
        WebClient wc = WebClient.create(address);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000);
        wc.accept("application/xml");
        Book book = wc.get(Book.class);
        assertEquals("SuperBook", book.getName());

    }

    @Test
    public void testPostBookXsiType() throws Exception {
        String address = "http://localhost:" + PORT + "/the/thebooksxsi/bookstore/books/xsitype";
        JAXBElementProvider<Book> provider = new JAXBElementProvider<>();
        provider.setExtraClass(new Class[]{SuperBook.class});
        provider.setJaxbElementClassNames(Collections.singletonList(Book.class.getName()));
        WebClient wc = WebClient.create(address, Collections.singletonList(provider));
        wc.accept("application/xml");
        wc.type("application/xml");
        SuperBook book = new SuperBook("SuperBook2", 999L, true);
        Book book2 = wc.invoke("POST", book, Book.class, Book.class);
        assertEquals("SuperBook2", book2.getName());

    }

    @Test
    public void testPostBookXsiTypeProxy() throws Exception {
        String address = "http://localhost:" + PORT + "/the/thebooksxsi/bookstore";
        JAXBElementProvider<Book> provider = new JAXBElementProvider<>();
        provider.setExtraClass(new Class[]{SuperBook.class});
        provider.setJaxbElementClassNames(Collections.singletonList(Book.class.getName()));
        BookStoreSpring bookStore = JAXRSClientFactory.create(address, BookStoreSpring.class,
                                                              Collections.singletonList(provider));
        SuperBook book = new SuperBook("SuperBook2", 999L, true);
        Book book2 = bookStore.postGetBookXsiType(book);
        assertEquals("SuperBook2", book2.getName());

    }

    @Test
    public void testGetBookWithEncodedQueryValue() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/bookstore/booksquery?id=12%2B3";
        getBook(endpointAddress, "resources/expected_get_book123json.txt");
    }

    @Test
    public void testGetBookWithEncodedPathValue() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/bookstore/id=12%2B3";
        getBook(endpointAddress, "resources/expected_get_book123json.txt");
    }

    @Test
    public void testGetBookWithEncodedPathValue2() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/bookstore/id=12+3";
        getBook(endpointAddress, "resources/expected_get_book123json.txt");
    }

    @Test
    public void testGetDefaultBook() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/the/bookstore";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/json");
        Book book = wc.get(Book.class);
        assertEquals(123L, book.getId());
    }
    @Test
    public void testGetDefaultBook2() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/the/bookstore/2/";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/json");
        Book book = wc.get(Book.class);
        assertEquals(123L, book.getId());
        assertEquals("Default", book.getName());
    }
    @Test
    public void testGetDefaultBookMatrixParam() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/the/bookstore/2/";
        WebClient wc = WebClient.create(endpointAddress);
        wc.matrix("a", "b");
        wc.accept("application/json");
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        Book book = wc.get(Book.class);
        assertEquals(123L, book.getId());
        assertEquals("Defaultb", book.getName());
    }
    @Test
    public void testGetBookById() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/the/bookstore/2/123";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/json");
        Book book = wc.get(Book.class);
        assertEquals(123L, book.getId());
        assertEquals("Id", book.getName());
    }

    @Test
    public void testGetDefaultBookJSessionID() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/bookstore/;JSESSIONID=123";
        getBook(endpointAddress, "resources/expected_get_book123json.txt");
    }


    private void getBook(String endpointAddress, String resource) throws Exception {
        getBook(endpointAddress, resource, "application/json");
    }

    private void getBook(String endpointAddress, String resource, String type) throws Exception {
        getBook(endpointAddress, resource, type, null);
    }

    private void getBook(String endpointAddress, String resource, String type, String mHeader)
        throws Exception {
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Content-Type", "*/*");
        connect.addRequestProperty("Accept", type);
        connect.addRequestProperty("Accept-Encoding", "gzip;q=1.0, identity; q=0.5, *;q=0");
        if (mHeader != null) {
            connect.addRequestProperty("X-HTTP-Method-Override", mHeader);
        }
        InputStream in = connect.getInputStream();

        InputStream expected = getClass().getResourceAsStream(resource);
        assertEquals(stripXmlInstructionIfNeeded(getStringFromInputStream(expected)),
                     stripXmlInstructionIfNeeded(getStringFromInputStream(in)));
    }

    private void getBookAegis(String endpointAddress, String type) throws Exception {
        WebClient client = WebClient.create(endpointAddress,
            Collections.singletonList(new AegisElementProvider<Object>()));
        Book book = client.accept(type).get(Book.class);

        assertEquals(124L, book.getId());
        assertEquals("CXF in Action - 2", book.getName());
    }

    @Test
    public void testAddInvalidXmlBook() throws Exception {

        doPost("http://localhost:" + PORT + "/the/bookstore/books/convert",
               400,
               "application/xml",
               "resources/add_book.txt",
               null);

        doPost("http://localhost:" + PORT + "/the/thebooks/bookstore/books/convert",
               400,
               "application/xml",
               "resources/add_book.txt",
               null);

    }

    @Test
    public void testAddInvalidJsonBook() throws Exception {

        doPost("http://localhost:" + PORT + "/the/bookstore/books/convert",
               400,
               "application/json",
               "resources/add_book2json_invalid.txt",
               null);

        doPost("http://localhost:" + PORT + "/the/thebooks/bookstore/books/convert",
               400,
               "application/json",
               "resources/add_book2json_invalid.txt",
               null);

    }

    @Test
    public void testAddValidXmlBook() throws Exception {

        doPost("http://localhost:" + PORT + "/the/bookstore/books/convert",
               200,
               "application/xml",
               "resources/add_book2.txt",
               "resources/expected_get_book123.txt");

        doPost("http://localhost:" + PORT + "/the/thebooks/bookstore/books/convert",
               200,
               "application/xml",
               "resources/add_book2.txt",
               "resources/expected_get_book123.txt");

    }

    @Test
    public void testGetBookAegis() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks4/bookstore/books/aegis";
        getBookAegis(endpointAddress, "application/xml");
    }

    @Test
    public void testRetrieveGetBookAegis() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks4/bookstore/books/aegis/retrieve/get";
        getBookAegis(endpointAddress, "application/xml");
    }

    @Test
    public void testRetrieveBookAegis3() throws Exception {
        try (Socket s = new Socket("localhost", Integer.parseInt(PORT))) {
            IOUtils.copy(getClass().getResourceAsStream("resources/retrieveRequest.txt"), s.getOutputStream());

            try (InputStream is = s.getInputStream()) {
                assertTrue(IOUtils.toString(is).contains("CXF in Action - 2"));
            }
        }
    }

    @Test
    public void testGetBookUserResource() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks6/bookstore/books/123";
        getBook(endpointAddress, "resources/expected_get_book123.txt", "application/xml");
    }

    @Test
    public void testGetBookUserResource2() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks7/bookstore/books/123";
        getBook(endpointAddress, "resources/expected_get_book123.txt", "application/xml");
    }

    @Test
    public void testGetBookUserResourceFromProxy() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks6";
        BookStoreNoAnnotations bStore = JAXRSClientFactory.createFromModel(
                                         endpointAddress,
                                         BookStoreNoAnnotations.class,
                                         "classpath:/org/apache/cxf/systest/jaxrs/resources/resources.xml",
                                         Collections.singletonList(new LongTypeParamConverterProvider()),
                                         null);
        Book b = bStore.getBook(null);
        assertNotNull(b);
        assertEquals(123L, b.getId());
        assertEquals("CXF in Action", b.getName());
        ChapterNoAnnotations proxy = bStore.getBookChapter(123L);
        ChapterNoAnnotations c = proxy.getItself();
        assertNotNull(c);
        assertEquals(1, c.getId());
        assertEquals("chapter 1", c.getTitle());
    }

    @Test
    public void testGetBookXSLTXml() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks5/bookstore/books/xslt";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml").path(666).matrix("name2", 2).query("name", "Action - ");
        Book b = wc.get(Book.class);
        assertEquals(666, b.getId());
        assertEquals("CXF in Action - 2", b.getName());
    }

    @Test
    public void testReaderWriterFromJaxrsFilters() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooksWithStax/bookstore/books/convert2/123";
        WebClient wc = WebClient.create(endpointAddress);
        wc.type("application/xml").accept("application/xml");
        Book2 b = new Book2();
        b.setId(777L);
        b.setName("CXF - 777");
        Book2 b2 = wc.invoke("PUT", b, Book2.class);
        assertNotSame(b, b2);
        assertEquals(777, b2.getId());
        assertEquals("CXF - 777", b2.getName());
    }

    @Test
    public void testReaderWriterFromInterceptors() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooksWithStax/bookstore/books/convert";
        WebClient wc = WebClient.create(endpointAddress);
        wc.type("application/xml").accept("application/xml");
        Book2 b = new Book2();
        b.setId(777L);
        b.setName("CXF - 777");
        Book2 b2 = wc.invoke("POST", b, Book2.class);
        assertNotSame(b, b2);
        assertEquals(777, b2.getId());
        assertEquals("CXF - 777", b2.getName());
    }

    @Test
    public void testAddValidBookJson() throws Exception {
        doPost("http://localhost:" + PORT + "/the/bookstore/books/convert",
               200,
               "application/json",
               "resources/add_book2json.txt",
               "resources/expected_get_book123.txt");

        doPost("http://localhost:" + PORT + "/the/thebooks/bookstore/books/convert",
               200,
               "application/json",
               "resources/add_book2json.txt",
               "resources/expected_get_book123.txt");

        doPost("http://localhost:" + PORT + "/the/thebooks/bookstore/books/convert",
               200,
               "application/vnd.example-com.foo+json",
               "resources/add_book2json.txt",
               "resources/expected_get_book123.txt");
    }

    @Test
    public void testAddInvalidBookDuplicateElementJson() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/the/bookstore/books/convert");
        wc.type("application/json");
        InputStream is = getClass().getResourceAsStream("resources/add_book2json_duplicate.txt");
        assertNotNull(is);
        Response r = wc.post(is);
        assertEquals(400, r.getStatus());
        String content = IOUtils.readStringFromStream((InputStream)r.getEntity());
        assertTrue(content, content.contains("Invalid content was found starting with element"));
    }

    private void doPost(String endpointAddress, int expectedStatus, String contentType,
                        String inResource, String expectedResource) throws Exception {

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(endpointAddress);
        post.setHeader("Content-Type", contentType);
        post.setEntity(new InputStreamEntity(getClass().getResourceAsStream(inResource), ContentType.TEXT_XML));

        try {
            CloseableHttpResponse response = client.execute(post);
            assertEquals(expectedStatus, response.getStatusLine().getStatusCode());

            if (expectedStatus != 400) {
                InputStream expected = getClass().getResourceAsStream(expectedResource);
                assertEquals(stripXmlInstructionIfNeeded(getStringFromInputStream(expected)),
                             stripXmlInstructionIfNeeded(EntityUtils.toString(response.getEntity())));
            } else {
                assertTrue(EntityUtils.toString(response.getEntity())
                               .contains("Cannot find the declaration of element"));
            }
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }

    private String stripXmlInstructionIfNeeded(String str) {
        if (str != null && str.startsWith("<?xml")) {
            int index = str.indexOf("?>");
            str = str.substring(index + 2);
        }
        return str;
    }

    private String getStringFromInputStream(InputStream in) throws Exception {
        return IOUtils.toString(in);
    }


    @Ignore
    @XmlRootElement(name = "Book", namespace = "http://www.w3.org/1999/xhtml")
    public static class Book2 {
        @XmlElement(name = "id", namespace = "http://www.w3.org/1999/xhtml")
        private long id1;
        @XmlElement(name = "name", namespace = "http://www.w3.org/1999/xhtml")
        private String name1;
        public Book2() {

        }
        public long getId() {
            return id1;
        }

        public void setId(Long theId) {
            id1 = theId;
        }

        public String getName() {
            return name1;
        }

        public void setName(String n) {
            name1 = n;
        }

    }
    static class LongTypeParamConverterProvider implements ParamConverterProvider, ParamConverter<Long> {

        @SuppressWarnings("unchecked")
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> cls, Type t, Annotation[] anns) {
            return cls == Long.class ? (ParamConverter<T>)this : null;
        }

        @Override
        public Long fromString(String s) {
            return null;
        }

        @Override
        public String toString(Long l) {
            return l == null ? "123" : String.valueOf(l);
        }

    }
}
