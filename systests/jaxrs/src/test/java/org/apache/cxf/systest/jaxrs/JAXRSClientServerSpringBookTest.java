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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.form.Form;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.provider.AegisElementProvider;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSClientServerSpringBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerSpring.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerSpring.class));
    }
    
    @Test
    public void testGetGenericBook() throws Exception {
        String baseAddress = "http://localhost:" + PORT + "/the/thebooks8/books"; 
        WebClient wc = WebClient.create(baseAddress);
        Long id = wc.type("application/xml").accept("text/plain").post(new Book("CXF", 1L), Long.class);
        assertEquals(new Long(1), id);
        Book book = wc.accept("application/xml").query("id", 1L).get(Book.class);
        assertEquals("CXF", book.getName());
    }
    
    @Test
    public void testPostGeneratedBook() throws Exception {
        String baseAddress = "http://localhost:" + PORT + "/the/generated/bookstore/books/1";
        JAXBElementProvider provider = new JAXBElementProvider();
        provider.setJaxbElementClassMap(Collections.singletonMap(
                                          "org.apache.cxf.systest.jaxrs.codegen.schema.Book", 
                                          "{http://superbooks}thebook"));
        
        WebClient wc = WebClient.create(baseAddress,
                                        Collections.singletonList(provider));
        wc.type("application/xml");
        
        org.apache.cxf.systest.jaxrs.codegen.schema.Book book = 
            new org.apache.cxf.systest.jaxrs.codegen.schema.Book();
        book.setId(123);
        Response r = wc.post(book);
        assertEquals(204, r.getStatus());
    }
    
    @Test
    public void testGetWadlFromWadlLocation() throws Exception {
        String address = "http://localhost:" + PORT + "/the/generated";    
        checkWadlResourcesInfo(address, address + "/bookstore", "/schemas/book.xsd", 1);
    
        checkSchemas(address, "/schemas/book.xsd", "/schemas/chapter.xsd", "include");
        checkSchemas(address, "/schemas/chapter.xsd", null, null);
    }
    
    @Test
    public void testGetGeneratedWadlWithExternalSchemas() throws Exception {
        String address = "http://localhost:" + PORT + "/the/bookstore";    
        checkWadlResourcesInfo(address, address, "/book.xsd", 1);
    
        checkSchemas(address, "/book.xsd", "/bookid.xsd", "import");
        checkSchemas(address, "/bookid.xsd", null, null);
        checkWadlResourcesInfo(address, address, "/book.xsd", 1);
    }
    
    private void checkSchemas(String address, String schemaSegment, 
                              String includedSchema,
                              String refAttrName) throws Exception {
        WebClient client = WebClient.create(address + schemaSegment);
        WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        Document doc = DOMUtils.readXml(new InputStreamReader(client.get(InputStream.class), "UTF-8"));
        Element root = doc.getDocumentElement();
        assertEquals(XmlSchemaConstants.XSD_NAMESPACE_URI, root.getNamespaceURI());
        assertEquals("schema", root.getLocalName());
        if (includedSchema != null) {
            List<Element> includeEls = DOMUtils.getChildrenWithName(root, 
                         XmlSchemaConstants.XSD_NAMESPACE_URI, refAttrName);
            assertEquals(1, includeEls.size());
            String href = includeEls.get(0).getAttribute("schemaLocation");
            assertEquals(address + includedSchema, href);
        }
        
    }
    
    private void checkWadlResourcesInfo(String baseURI, String requestURI, 
                                        String schemaRef, int size) throws Exception {
        WebClient client = WebClient.create(requestURI + "?_wadl&_type=xml");
        Document doc = DOMUtils.readXml(new InputStreamReader(client.get(InputStream.class), "UTF-8"));
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
        Element resourcesEl =  resourcesEls.get(0);
        assertEquals(baseURI, resourcesEl.getAttribute("base"));
        List<Element> resourceEls = 
            DOMUtils.getChildrenWithName(resourcesEl, 
                                         WadlGenerator.WADL_NS, "resource");
        assertEquals(size, resourceEls.size());
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
        WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(1000000);
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
    public void testGetBookXSLTHtml() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks5/bookstore/books/xslt";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xhtml+xml").path(666).matrix("name2", 2).query("name", "Action - ");
        XMLSource source = wc.get(XMLSource.class);
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xhtml", "http://www.w3.org/1999/xhtml");
        Book2 b = source.getNode("xhtml:html/xhtml:body/xhtml:ul/xhtml:Book", namespaces, Book2.class);
        assertEquals(666, b.getId());
        assertEquals("CXF in Action - 2", b.getName());
    }

    @Test
    public void testGetBookByUriInfo2() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks3/bookstore/bookinfo?"
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
        Response r = wc.post(new Book("CXF", 123L));
        assertEquals(413, r.getStatus());
    }
    
    @Test
    public void testBookDepthExceededXMLSource() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks9/depth-source"; 
        WebClient wc = WebClient.create(endpointAddress);
        Response r = wc.post(new Book("CXF", 123L));
        assertEquals(413, r.getStatus());
    }
    
    @Test
    public void testBookDepthExceededXMLDom() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks9/depth-dom"; 
        WebClient wc = WebClient.create(endpointAddress);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        Response r = wc.post(new Book("CXF", 123L));
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
        Response r = wc.form(new Form().set("a", "b"));
        assertEquals(204, r.getStatus());
        r = wc.form(new Form().set("a", "b").set("c", "b"));
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
        String endpointAddress =
            "http://localhost:" + PORT + "/the/bookstore"; 
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
        assertEquals(getStringFromInputStream(expected), getStringFromInputStream(in));
    }
    
    private void getBookAegis(String endpointAddress, String type) throws Exception {
        getBookAegis(endpointAddress, type, null);
    }
    
    private void getBookAegis(String endpointAddress, String type, String mHeader) throws Exception {
        WebClient client = WebClient.create(endpointAddress,
            Collections.singletonList(new AegisElementProvider<Object>()));
        if (mHeader != null) {
            client = client.header("X-HTTP-Method-Override", mHeader);
        }
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
    public void testRetrieveBookAegis1() throws Exception {
        
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks4/bookstore/books/aegis/retrieve?_method=RETRIEVE"; 
        getBookAegis(endpointAddress, "application/xml"); 
    }
    
    @Test
    public void testRetrieveBookAegis2() throws Exception {
        
        String endpointAddress =
            "http://localhost:" + PORT + "/the/thebooks4/bookstore/books/aegis/retrieve"; 
        getBookAegis(endpointAddress, "application/xml", "RETRIEVE"); 
    }
    
    @Test
    public void testRetrieveBookAegis3() throws Exception {
        
        Socket s = new Socket("localhost", Integer.parseInt(PORT));
        
        InputStream is = this.getClass().getResourceAsStream("resources/retrieveRequest.txt");
        byte[] bytes = IOUtils.readBytesFromStream(is);
        s.getOutputStream().write(bytes);
        s.getOutputStream().flush();
        
        BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String str = null;
        while ((str = r.readLine()) != null) {
            sb.append(str);
        }
        
        String aegisData = sb.toString();
        s.getInputStream().close();
        s.close();
        assertTrue(aegisData.contains("CXF in Action - 2"));
        
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
                                         null);
        Book b = bStore.getBook(123L);
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
    
    private void doPost(String endpointAddress, int expectedStatus, String contentType,
                        String inResource, String expectedResource) throws Exception {
        
        File input = new File(getClass().getResource(inResource).toURI());         
        PostMethod post = new PostMethod(endpointAddress);
        post.setRequestHeader("Content-Type", contentType);
        RequestEntity entity = new FileRequestEntity(input, "text/xml");
        post.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(expectedStatus, result);
            
            if (expectedStatus != 400) {
                InputStream expected = getClass().getResourceAsStream(expectedResource);
                assertEquals(getStringFromInputStream(expected), post.getResponseBodyAsString());
            } else {
                assertTrue(post.getResponseBodyAsString()
                               .contains("Cannot find the declaration of element"));
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
}
