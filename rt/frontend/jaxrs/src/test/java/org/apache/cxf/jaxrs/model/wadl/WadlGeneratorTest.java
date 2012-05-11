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
package org.apache.cxf.jaxrs.model.wadl;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.JSONProvider;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WadlGeneratorTest extends Assert {

    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        control.makeThreadSafe(true);
    }
    
    @Test
    public void testWadlInJsonFormat() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        wg.setUseJaxbContextForQnames(false);
        wg.setIgnoreMessageWriters(false);
        
        wg.setExternalLinks(Collections.singletonList("json.schema"));
        
        ClassResourceInfo cri = 
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, null);
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("Accept", Collections.singletonList("application/json"));
        m.put(Message.PROTOCOL_HEADERS, headers);
        Response r = wg.handleRequest(m, cri);
        assertEquals("application/json",
                r.getMetadata().getFirst("Content-Type").toString());
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        new JSONProvider().writeTo(
                (Document)r.getEntity(), Document.class, Document.class, 
                  new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE, 
                  new MetadataMap<String, Object>(), os);
        String s = os.toString();
        String expected1 = 
            "{\"application\":{\"grammars\":{\"include\":{\"@href\":\"http:\\/\\/localhost:8080\\/baz"
            + "\\/json.schema\"}},\"resources\":{\"@base\":\"http:\\/\\/localhost:8080\\/baz\","
            + "\"resource\":{\"@path\":\"\\/bookstore\\/{id}\"";
        assertTrue(s.startsWith(expected1));
        String expected2 =
            "\"response\":{\"representation\":[{\"@mediaType\":\"application\\/xml\"},"
            + "{\"@element\":\"Chapter\",\"@mediaType\":\"application\\/json\"}]}";
        assertTrue(s.contains(expected2));
    }
    
    @Test
    public void testNoWadl() {
        WadlGenerator wg = new WadlGenerator();
        assertNull(wg.handleRequest(new MessageImpl(), null));
    }
    
    @Test
    public void testCustomSchemaJaxbContextPrefixes() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        wg.setSchemaLocations(Collections.singletonList("classpath:/book1.xsd"));
        
        ClassResourceInfo cri = 
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, null);
        
        Response r = wg.handleRequest(m, cri);
        checkResponse(r);
        Document doc = DOMUtils.readXml(new StringReader(r.getEntity().toString()));
        checkGrammars(doc.getDocumentElement(), "thebook", "thebook2", "thechapter");
        List<Element> els = getWadlResourcesInfo(doc, "http://localhost:8080/baz", 1);
        checkBookStoreInfo(els.get(0), "prefix1:thebook", "prefix1:thebook2", "prefix1:thechapter");
    }
    
    @Test
    public void testCustomSchemaWithImportJaxbContextPrefixes() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        wg.setSchemaLocations(Collections.singletonList("classpath:/books.xsd"));
        
        ClassResourceInfo cri = 
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, null);
        
        Response r = wg.handleRequest(m, cri);
        checkResponse(r);
        Document doc = DOMUtils.readXml(new StringReader(r.getEntity().toString()));
        List<Element> grammarEls = DOMUtils.getChildrenWithName(doc.getDocumentElement(), 
            WadlGenerator.WADL_NS, "grammars");
        assertEquals(1, grammarEls.size());
        List<Element> schemasEls = DOMUtils.getChildrenWithName(grammarEls.get(0), 
            XmlSchemaConstants.XSD_NAMESPACE_URI, "schema");
        assertEquals(1, schemasEls.size());
        assertEquals("http://books", schemasEls.get(0).getAttribute("targetNamespace"));
        List<Element> elementEls = DOMUtils.getChildrenWithName(schemasEls.get(0), 
            XmlSchemaConstants.XSD_NAMESPACE_URI, "element");
        assertEquals(1, elementEls.size());
        assertTrue(checkElement(elementEls, "books", "tns:books"));

        List<Element> complexTypesEls = DOMUtils.getChildrenWithName(schemasEls.get(0), 
            XmlSchemaConstants.XSD_NAMESPACE_URI, "complexType");
        assertEquals(1, complexTypesEls.size());
        assertTrue(checkComplexType(complexTypesEls, "books"));
        
        List<Element> importEls = DOMUtils.getChildrenWithName(schemasEls.get(0), 
            XmlSchemaConstants.XSD_NAMESPACE_URI, "import");
        assertEquals(1, importEls.size());
        assertEquals("http://localhost:8080/baz/book1.xsd",
                     importEls.get(0).getAttribute("schemaLocation"));
    }
    
    @Test
    public void testExternalSchemaJaxbContextPrefixes() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        wg.setExternalLinks(Collections.singletonList("http://books.xsd"));
        
        ClassResourceInfo cri = 
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, null);
        
        Response r = wg.handleRequest(m, cri);
        checkResponse(r);
        Document doc = DOMUtils.readXml(new StringReader(r.getEntity().toString()));
        checkGrammarsWithLinks(doc.getDocumentElement(), Collections.singletonList("http://books.xsd"));
        List<Element> els = getWadlResourcesInfo(doc, "http://localhost:8080/baz", 1);
        checkBookStoreInfo(els.get(0), "prefix1:thebook", "prefix1:thebook2", "prefix1:thechapter");
    }
    
    @Test
    public void testExternalRelativeSchemaJaxbContextPrefixes() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        wg.setExternalLinks(Collections.singletonList("books.xsd"));
        
        ClassResourceInfo cri = 
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, null);
        
        Response r = wg.handleRequest(m, cri);
        checkResponse(r);
        Document doc = DOMUtils.readXml(new StringReader(r.getEntity().toString()));
        checkGrammarsWithLinks(doc.getDocumentElement(), 
                               Collections.singletonList("http://localhost:8080/baz/books.xsd"));
        List<Element> els = getWadlResourcesInfo(doc, "http://localhost:8080/baz", 1);
        checkBookStoreInfo(els.get(0), "prefix1:thebook", "prefix1:thebook2", "prefix1:thechapter");
    }
    
    @Test
    public void testExternalSchemaCustomPrefix() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        wg.setExternalLinks(Collections.singletonList("http://books"));
        wg.setUseJaxbContextForQnames(false);
        
        ClassResourceInfo cri = 
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, null);
        
        Response r = wg.handleRequest(m, cri);
        checkResponse(r);
        Document doc = DOMUtils.readXml(new StringReader(r.getEntity().toString()));
        checkGrammarsWithLinks(doc.getDocumentElement(), 
                               Collections.singletonList("http://books"));
        List<Element> els = getWadlResourcesInfo(doc, "http://localhost:8080/baz", 1);
        checkBookStoreInfo(els.get(0), "p1:thesuperbook", "p1:thesuperbook2", "p1:thesuperchapter");
    }
    
    @Test
    public void testCustomSchemaAndSchemaPrefixes() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        wg.setSchemaLocations(Collections.singletonList("classpath:/book2.xsd"));
        wg.setUseJaxbContextForQnames(false);
        
        ClassResourceInfo cri = 
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, null);
        
        Response r = wg.handleRequest(m, cri);
        checkResponse(r);
        Document doc = DOMUtils.readXml(new StringReader(r.getEntity().toString()));
        checkGrammars(doc.getDocumentElement(), "book", "book2", "chapter");
        List<Element> els = getWadlResourcesInfo(doc, "http://localhost:8080/baz", 1);
        checkBookStoreInfo(els.get(0), "prefix1:book", "prefix1:book2", "prefix1:chapter");
    }
    
    @Test
    public void testSingleRootResource() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        wg.setApplicationTitle("My Application");
        wg.setNamespacePrefix("ns");
        ClassResourceInfo cri = 
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, null);
        
        Response r = wg.handleRequest(m, cri);
        checkResponse(r);
        Document doc = DOMUtils.readXml(new StringReader(r.getEntity().toString()));
        checkDocs(doc.getDocumentElement(), "My Application", "", "");
        checkGrammars(doc.getDocumentElement(), "thebook", "thebook2", "thechapter");
        List<Element> els = getWadlResourcesInfo(doc, "http://localhost:8080/baz", 1);
        checkBookStoreInfo(els.get(0), "ns1:thebook", "ns1:thebook2", "ns1:thechapter");
    }
    
    @Test
    public void testTwoSchemasSameNs() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        wg.setApplicationTitle("My Application");
        wg.setNamespacePrefix("ns");
        ClassResourceInfo cri = 
            ResourceUtils.createClassResourceInfo(TestResource.class, TestResource.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, null);
        
        Response r = wg.handleRequest(m, cri);
        checkResponse(r);
        Document doc = DOMUtils.readXml(new StringReader(r.getEntity().toString()));
        checkDocs(doc.getDocumentElement(), "My Application", "", "");
        List<Element> grammarEls = DOMUtils.getChildrenWithName(doc.getDocumentElement(), 
                                                                WadlGenerator.WADL_NS, 
                                                                "grammars");
        assertEquals(1, grammarEls.size());
        List<Element> schemasEls = DOMUtils.getChildrenWithName(grammarEls.get(0), 
              XmlSchemaConstants.XSD_NAMESPACE_URI, "schema");
        assertEquals(2, schemasEls.size());
        assertEquals("http://example.com/test", schemasEls.get(0).getAttribute("targetNamespace"));
        assertEquals("http://example.com/test", schemasEls.get(1).getAttribute("targetNamespace"));
        List<Element> reps = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(), 
                                       WadlGenerator.WADL_NS, "representation");
        assertEquals(2, reps.size());
        assertEquals("ns1:testCompositeObject", reps.get(0).getAttribute("element"));
        assertEquals("ns1:testCompositeObject", reps.get(1).getAttribute("element"));
    }
    
    @Test
    public void testRootResourceWithSingleSlash() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        ClassResourceInfo cri = 
            ResourceUtils.createClassResourceInfo(BookStoreWithSingleSlash.class, 
                                                  BookStoreWithSingleSlash.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, null);
        
        Response r = wg.handleRequest(m, cri);
        checkResponse(r);
        Document doc = DOMUtils.readXml(new StringReader(r.getEntity().toString()));
        List<Element> rootEls = getWadlResourcesInfo(doc, "http://localhost:8080/baz", 1);
        assertEquals(1, rootEls.size());
        Element resource = rootEls.get(0);
        assertEquals("/", resource.getAttribute("path"));
        List<Element> resourceEls = DOMUtils.getChildrenWithName(resource, 
                                                                 WadlGenerator.WADL_NS, "resource");
        assertEquals(1, resourceEls.size());        
        assertEquals("book", resourceEls.get(0).getAttribute("path"));
        
        verifyParameters(resourceEls.get(0), 1, new Param("id", "template", "xs:int"));
        
        checkGrammars(doc.getDocumentElement(), "thebook", null, "thechapter");
    }
    
    private void checkResponse(Response r) throws Exception {
        assertNotNull(r);
        assertEquals(MediaType.APPLICATION_XML,
                     r.getMetadata().getFirst(HttpHeaders.CONTENT_TYPE));
//        File f = new File("test.xml");
//        f.delete();
//        f.createNewFile();
//        System.out.println(f.getAbsolutePath());
//        FileOutputStream fos = new FileOutputStream(f);
//        fos.write(r.getEntity().toString().getBytes());
//        fos.flush();
//        fos.close();
    }
    
    @Test
    public void testMultipleRootResources() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        wg.setDefaultMediaType(WadlGenerator.WADL_TYPE.toString());
        ClassResourceInfo cri1 = 
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        ClassResourceInfo cri2 = 
            ResourceUtils.createClassResourceInfo(Orders.class, Orders.class, true, true);
        List<ClassResourceInfo> cris = new ArrayList<ClassResourceInfo>();
        cris.add(cri1);
        cris.add(cri2);
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, cris);
        Response r = wg.handleRequest(m, null);
        assertEquals(WadlGenerator.WADL_TYPE.toString(),
                     r.getMetadata().getFirst(HttpHeaders.CONTENT_TYPE));
        String wadl = r.getEntity().toString();
        //System.out.println(wadl);
        Document doc = DOMUtils.readXml(new StringReader(wadl));
        checkGrammars(doc.getDocumentElement(), "thebook", "thebook2", "thechapter");
        List<Element> els = getWadlResourcesInfo(doc, "http://localhost:8080/baz", 2);
        checkBookStoreInfo(els.get(0), "prefix1:thebook", "prefix1:thebook2", "prefix1:thechapter");
        Element orderResource = els.get(1);
        assertEquals("/orders", orderResource.getAttribute("path"));
    }

    private void checkGrammars(Element appElement, String bookEl, String book2El, String chapterEl) {
        List<Element> grammarEls = DOMUtils.getChildrenWithName(appElement, WadlGenerator.WADL_NS, 
                                                                "grammars");
        assertEquals(1, grammarEls.size());
        List<Element> schemasEls = DOMUtils.getChildrenWithName(grammarEls.get(0), 
                                                          XmlSchemaConstants.XSD_NAMESPACE_URI, "schema");
        assertEquals(1, schemasEls.size());
        assertEquals("http://superbooks", schemasEls.get(0).getAttribute("targetNamespace"));
        List<Element> elementEls = DOMUtils.getChildrenWithName(schemasEls.get(0), 
                            XmlSchemaConstants.XSD_NAMESPACE_URI, "element");
        
        int size = book2El == null ? 2 : 3;
        
        assertEquals(size, elementEls.size());
        assertTrue(checkElement(elementEls, bookEl, "tns:book"));
        if (book2El != null) {
            assertTrue(checkElement(elementEls, book2El, "tns:book2"));
        }
        assertTrue(checkElement(elementEls, chapterEl, "tns:chapter"));
        
        List<Element> complexTypesEls = DOMUtils.getChildrenWithName(schemasEls.get(0), 
                                        XmlSchemaConstants.XSD_NAMESPACE_URI, "complexType");
        assertEquals(size, complexTypesEls.size());
        
        assertTrue(checkComplexType(complexTypesEls, "book"));
        if (book2El != null) {
            assertTrue(checkComplexType(complexTypesEls, "book2"));
        }
        assertTrue(checkComplexType(complexTypesEls, "chapter"));
    }
    
    private void checkGrammarsWithLinks(Element appElement, List<String> links) {
        assertTrue(links.size() > 0);
        List<Element> grammarEls = DOMUtils.getChildrenWithName(appElement, WadlGenerator.WADL_NS, 
                                                                "grammars");
        assertEquals(1, grammarEls.size());
        List<Element> schemasEls = DOMUtils.getChildrenWithName(grammarEls.get(0), 
                                                          XmlSchemaConstants.XSD_NAMESPACE_URI, "schema");
        assertEquals(0, schemasEls.size());
        
        List<Element> includeEls = DOMUtils.getChildrenWithName(grammarEls.get(0), WadlGenerator.WADL_NS,
                                                                "include");
        assertEquals(links.size(), includeEls.size());
        for (Element el : includeEls) {
            assertTrue(links.contains(el.getAttribute("href")));
        }
    }
    
    private boolean checkComplexType(List<Element> els, String name) {
        for (Element e : els) {
            if (name.equals(e.getAttribute("name"))) {
                return true;
            }
        }
        return false;
    }
    
    private boolean checkElement(List<Element> els, String name, String type) {
        for (Element e : els) {
            if (name.equals(e.getAttribute("name"))
                && type.equals(e.getAttribute("type"))) {
                return true;
            }
        }
        return false;
    }
    
    private void checkBookStoreInfo(Element resource, String bookEl, String book2El, String chapterEl) {
        assertEquals("/bookstore/{id}", resource.getAttribute("path"));
        
        checkDocs(resource, "book store resource", "super resource", "en-us");
        
        List<Element> resourceEls = getElements(resource, "resource", 8);
        
        assertEquals("/book2", resourceEls.get(0).getAttribute("path"));
        assertEquals("/books/{bookid}", resourceEls.get(1).getAttribute("path"));
        assertEquals("/chapter", resourceEls.get(2).getAttribute("path"));
        assertEquals("/chapter2", resourceEls.get(3).getAttribute("path"));
        assertEquals("/books/{bookid}", resourceEls.get(4).getAttribute("path"));
        assertEquals("/booksubresource", resourceEls.get(5).getAttribute("path"));
        assertEquals("/form", resourceEls.get(6).getAttribute("path"));
        assertEquals("/itself", resourceEls.get(7).getAttribute("path"));
        
        // verify root resource starting with "/"
        // must have a single template parameter
        verifyParameters(resource, 1, new Param("id", "template", "xs:long"));
        
        // must have 3 methods, GET, POST and PUT
        List<Element> methodEls = getElements(resource, "method", 3);
        
        // verify GET
        assertEquals("GET", methodEls.get(0).getAttribute("name"));
        assertEquals(0, DOMUtils.getChildrenWithName(methodEls.get(0), 
                        WadlGenerator.WADL_NS, "param").size());
        // check request 
        List<Element> requestEls = getElements(methodEls.get(0), "request", 1);
        
        // 4 parameters are expected
        verifyParameters(requestEls.get(0), 5, 
                         new Param("a", "query", "xs:int"),
                         new Param("c.a", "query", "xs:int"),
                         new Param("c.b", "query", "xs:int"),
                         new Param("c.d.a", "query", "xs:int"),
                         new Param("e", "query", "xs:string", Collections.singleton("A")));
        
        assertEquals(0, DOMUtils.getChildrenWithName(requestEls.get(0), 
                         WadlGenerator.WADL_NS, "representation").size());
        //check response
        verifyRepresentation(methodEls.get(0), "response", "text/plain", "");
        
        // verify POST
        assertEquals("POST", methodEls.get(1).getAttribute("name"));
        Element formRep = verifyRepresentation(methodEls.get(1), "request", "multipart/form-data", "");
        checkDocs(formRep, "", "Attachments", "");
        
        // verify PUT
        assertEquals("PUT", methodEls.get(2).getAttribute("name"));
        verifyRepresentation(methodEls.get(2), "request", "text/plain", "");
        
        verifyResponseWithStatus(methodEls.get(2), "204");
        
        // verify resource starting with /book2
        verifyGetResourceMethod(resourceEls.get(0), book2El, null);
        
        //verify resource starting with /books/{bookid}
        checkDocs(resourceEls.get(1), "", "Resource books/{bookid}", "");
        verifyParameters(resourceEls.get(1), 3, 
                         new Param("id", "template", "xs:int", "book id"),
                         new Param("bookid", "template", "xs:int"),
                         new Param("mid", "matrix", "xs:int"));
        
        // and 2 methods
        methodEls = getElements(resourceEls.get(1), "method", 2);
                
        // POST 
        assertEquals("POST", methodEls.get(0).getAttribute("name"));
        checkDocs(methodEls.get(0), "", "Update the books collection", "");
        requestEls = getElements(methodEls.get(0), "request", 1);
        
        checkDocs(requestEls.get(0), "", "Request", "");
        
        verifyParameters(requestEls.get(0), 4, 
                         new Param("hid", "header", "xs:int"),
                         new Param("provider.bar", "query", "xs:int"),
                         new Param("bookstate", "query", "xs:string",
                                 new HashSet<String>(Arrays.asList("NEW", "USED", "OLD"))),
                         new Param("a", "query", "xs:string", true));
        
        verifyXmlJsonRepresentations(requestEls.get(0), book2El, "InputBook");
        List<Element> responseEls = getElements(methodEls.get(0), "response", 1);
        checkDocs(responseEls.get(0), "", "Response", "");
        verifyXmlJsonRepresentations(responseEls.get(0), bookEl, "Requested Book");
        
        // PUT
        assertEquals("PUT", methodEls.get(1).getAttribute("name"));
        checkDocs(methodEls.get(1), "", "Update the book", "");
        requestEls = getElements(methodEls.get(1), "request", 1);
        assertEquals(1, requestEls.size());
        verifyXmlJsonRepresentations(requestEls.get(0), bookEl, null);
        verifyResponseWithStatus(methodEls.get(1), "204");
        
        // verify resource starting with /chapter
        verifyGetResourceMethod(resourceEls.get(2), chapterEl, null);
        // verify resource starting with /chapter2
        verifyGetResourceMethod(resourceEls.get(3), chapterEl, null);
        
        // verify resource starting from /booksubresource
        // should have 2 parameters
        verifyParameters(resourceEls.get(5), 2, 
                         new Param("id", "template", "xs:int"),
                         new Param("mid", "matrix", "xs:int"));
        checkDocs(resourceEls.get(5), "", "Book subresource", ""); 
        // should have 4 child resources
        List<Element> subResourceEls = getElements(resourceEls.get(5), "resource", 6);

        assertEquals("/book", subResourceEls.get(0).getAttribute("path"));
        assertEquals("/form1", subResourceEls.get(1).getAttribute("path"));
        assertEquals("/form2", subResourceEls.get(2).getAttribute("path"));
        assertEquals("/form3/{id}", subResourceEls.get(3).getAttribute("path"));
        assertEquals("/form4/{id}", subResourceEls.get(4).getAttribute("path"));
        assertEquals("/chapter/{cid}", subResourceEls.get(5).getAttribute("path"));
        checkDocs(subResourceEls.get(5), "", "Chapter subresource", "");
        // verify book-subresource /book resource
        // GET 
        verifyGetResourceMethod(subResourceEls.get(0), bookEl, null);
        
        verifyFormSubResources(subResourceEls);
        
        // verify subresource /chapter/{id}
        List<Element> chapterMethodEls = getElements(subResourceEls.get(5), "resource", 1);
        assertEquals("/id", chapterMethodEls.get(0).getAttribute("path"));
        verifyParameters(subResourceEls.get(5), 1, 
                         new Param("cid", "template", "xs:int"));
        // GET
        verifyGetResourceMethod(chapterMethodEls.get(0), chapterEl, "Get the chapter");
    }
    
    private void verifyFormSubResources(List<Element> subResourceEls) {
     // verify book-subresource /form1 resource
        List<Element> form1MethodEls = getElements(subResourceEls.get(1), "method", 1);
        
        assertEquals("POST", form1MethodEls.get(0).getAttribute("name"));
        verifyRepresentation(form1MethodEls.get(0), "request", MediaType.APPLICATION_FORM_URLENCODED, "");
        verifyResponseWithStatus(form1MethodEls.get(0), "204");
        
        // verify book-subresource /form2 resource
        List<Element> form2MethodEls = getElements(subResourceEls.get(2), "method", 1);
        assertEquals("POST", form2MethodEls.get(0).getAttribute("name"));
        verifyRepresentation(form2MethodEls.get(0), "response", MediaType.TEXT_PLAIN, "");
        verifyRepresentation(form2MethodEls.get(0), "request", MediaType.APPLICATION_FORM_URLENCODED, "");
        
        List<Element> form2RequestEls = getElements(form2MethodEls.get(0), "request", 1);
        List<Element> form2RequestRepEls = getElements(form2RequestEls.get(0), "representation", 1);
        verifyParameters(form2RequestRepEls.get(0), 2, 
                         new Param("field1", "query", "xs:string"),
                         new Param("field2", "query", "xs:string"));
        
        // verify book-subresource /form2 resource
        verifyParameters(subResourceEls.get(3), 1, 
                         new Param("id", "template", "xs:string"));
        List<Element> form3MethodEls = getElements(subResourceEls.get(3), "method", 1);
        List<Element> form3RequestEls = getElements(form3MethodEls.get(0), "request", 1);
        verifyParameters(form3RequestEls.get(0), 1, 
                         new Param("headerId", "header", "xs:string"));
        List<Element> form3RequestRepEls = getElements(form3RequestEls.get(0), "representation", 1);
        verifyParameters(form3RequestRepEls.get(0), 2, 
                         new Param("field1", "query", "xs:string"),
                         new Param("field2", "query", "xs:string"));
    }
    
    private List<Element> getElements(Element resource, String name, int expectedSize) {
        List<Element> elements = DOMUtils.getChildrenWithName(resource, 
                                     WadlGenerator.WADL_NS, name);
        assertEquals(expectedSize, elements.size());
        return elements;
    }
    
    private void verifyParameters(Element el, int number, Param... params) {
        List<Element> paramsEls = DOMUtils.getChildrenWithName(el, 
                                                 WadlGenerator.WADL_NS, "param");
        assertEquals(number, paramsEls.size());
        assertEquals(number, params.length);
        
        for (int i = 0; i < number; i++) {
            checkParameter(paramsEls.get(i), params[i]);
        }
    }
    
    private void checkDocs(Element el, String title, String value, String language) {
        List<Element> docsEls = DOMUtils.getChildrenWithName(el, 
                                                             WadlGenerator.WADL_NS, "doc");
        assertEquals(1, docsEls.size());
        assertEquals(title, docsEls.get(0).getAttribute("title"));
        assertEquals(value, docsEls.get(0).getTextContent());
        assertEquals(language, 
            docsEls.get(0).getAttributeNS("http://www.w3.org/XML/1998/namespace", "lang"));
    }
    
    private void verifyGetResourceMethod(Element element, String type, String docs) {
        List<Element> methodEls = DOMUtils.getChildrenWithName(element, WadlGenerator.WADL_NS, "method");
        assertEquals(1, methodEls.size());
        if (docs != null) {
            checkDocs(methodEls.get(0), "", docs, "");
        }
        assertEquals("GET", methodEls.get(0).getAttribute("name"));
        assertEquals(0, DOMUtils.getChildrenWithName(methodEls.get(0), 
                      WadlGenerator.WADL_NS, "request").size());
        List<Element> responseEls = DOMUtils.getChildrenWithName(methodEls.get(0), 
                                WadlGenerator.WADL_NS, "response");
        assertEquals(1, responseEls.size());
        verifyXmlJsonRepresentations(responseEls.get(0), type, null);
    }
    
    private void verifyResponseWithStatus(Element element, String status) {
        List<Element> responseEls = DOMUtils.getChildrenWithName(element, 
                                       WadlGenerator.WADL_NS, "response");
        assertEquals(1, responseEls.size());
        assertEquals(status, responseEls.get(0).getAttribute("status"));
        assertEquals(0, DOMUtils.getChildrenWithName(responseEls.get(0), 
            WadlGenerator.WADL_NS, "representation").size());
    }
    
    private Element verifyRepresentation(Element element, 
                                      String name, 
                                      String mediaType,
                                      String elementValue) {
        List<Element> elements = DOMUtils.getChildrenWithName(element, 
                                 WadlGenerator.WADL_NS, name);
        assertEquals(1, elements.size());
        List<Element> representationEls = DOMUtils.getChildrenWithName(elements.get(0), 
                    WadlGenerator.WADL_NS, "representation"); 
        assertEquals(1, representationEls.size());
        verifyMediTypeAndElementValue(representationEls.get(0), mediaType, elementValue, null);
        if ("text/plain".equals(mediaType)) { 
            String pName = "request".equals(name) ? "request" : "result";
            verifyParameters(representationEls.get(0), 1, new Param(pName, "plain", "xs:string"));
        }
        return representationEls.get(0);
    }
    
    private void verifyXmlJsonRepresentations(Element element, String type, String docs) {
        List<Element> repEls = DOMUtils.getChildrenWithName(element, 
                                        WadlGenerator.WADL_NS, "representation");
        assertEquals(2, repEls.size());
        verifyMediTypeAndElementValue(repEls.get(0), "application/xml", type, docs);
        verifyMediTypeAndElementValue(repEls.get(1), "application/json", "", docs);
    }
    
    private void verifyMediTypeAndElementValue(Element el, String mediaType, String elementValue, 
                                               String docs) {
        assertEquals(mediaType, el.getAttribute("mediaType"));
        assertEquals(elementValue, el.getAttribute("element"));
        if (docs != null) {
            checkDocs(el, "", docs, "");
        }
    }
    
    private void checkParameter(Element paramEl, Param p) {
        assertEquals(p.getName(), paramEl.getAttribute("name"));
        assertEquals(p.getType(), paramEl.getAttribute("style"));
        assertEquals(p.getSchemaType(), paramEl.getAttribute("type"));
        assertEquals(p.isRepeating(), Boolean.valueOf(paramEl.getAttribute("repeating")));
        Set<String> options = p.getOptions();
        if (options != null) {
            Set<String> actualOptions = new HashSet<String>();
            List<Element> els = DOMUtils.getChildrenWithNamespace(paramEl, WadlGenerator.WADL_NS);
            assertFalse(els.isEmpty());
            assertEquals(options.size(), els.size());
            for (Element op : els) {
                actualOptions.add(op.getAttribute("value"));
            }
            assertEquals(options, actualOptions);
        }
        String docs = p.getDocs();
        if (docs != null) {
            checkDocs(paramEl, "", docs, "");
        }
    }
    
    private List<Element> getWadlResourcesInfo(Document doc, String baseURI, int size) throws Exception {
        Element root = doc.getDocumentElement();
        assertEquals(WadlGenerator.WADL_NS, root.getNamespaceURI());
        assertEquals("application", root.getLocalName());
        List<Element> resourcesEls = DOMUtils.getChildrenWithName(root, 
                                                                  WadlGenerator.WADL_NS, "resources");
        assertEquals(1, resourcesEls.size());
        Element resourcesEl =  resourcesEls.get(0);
        assertEquals(baseURI, resourcesEl.getAttribute("base"));
        List<Element> resourceEls = 
            DOMUtils.getChildrenWithName(resourcesEl, 
                                         WadlGenerator.WADL_NS, "resource");
        assertEquals(size, resourceEls.size());
        return resourceEls;
    }
    
    
    private Message mockMessage(String baseAddress, String pathInfo, String query,
                                List<ClassResourceInfo> cris) {
        Message m = new MessageImpl();
        Exchange e = new ExchangeImpl();
        e.put(Service.class, new JAXRSServiceImpl(cris));
        
        m.setExchange(e);
        control.reset();
        ServletDestination d = control.createMock(ServletDestination.class);
        EndpointInfo epr = new EndpointInfo(); 
        epr.setAddress(baseAddress);
        d.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(epr).anyTimes();
        e.setDestination(d);
        m.put(Message.REQUEST_URI, pathInfo);
        m.put(Message.QUERY_STRING, query);
        m.put(Message.HTTP_REQUEST_METHOD, "GET");
        control.replay();
        return m;
    }

    private static class Param {
        private String name;
        private String type;
        private String schemaType;
        private String docs;
        private boolean repeating;
        private Set<String> options;
        public Param(String name, String type, String schemaType) {
            this(name, type, schemaType, false);
        }
        
        public Param(String name, String type, String schemaType, Set<String> opts) {
            this.name = name;
            this.type = type;
            this.schemaType = schemaType;
            this.options = opts;
        }
        
        public Param(String name, String type, String schemaType, boolean repeating) {
            this(name, type, schemaType, repeating, null);
        }
        
        public Param(String name, String type, String schemaType, String docs) {
            this(name, type, schemaType, false, null);
        }
        
        
        public Param(String name, String type, String schemaType, boolean repeating, String docs) {
            this.name = name;
            this.type = type;
            this.schemaType = schemaType;
            this.docs = docs;
            this.repeating = repeating;
        }
        
        public Set<String> getOptions() {
            return options;
        }
        
        public String getName() {
            return name;
        }
        
        public String getType() {
            return type;
        }
        
        public String getSchemaType() {
            return schemaType;
        }
        
        public String getDocs() {
            return docs;
        }
        
        public boolean isRepeating() {
            return repeating;
        }
    }
    
    @XmlRootElement(namespace = "http://example.com/test")
    public static class TestCompositeObject {
        private int id;
        private String name;
        public int getId() {
            return id;
        }
        public void setId(int id) {
            this.id = id;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }

    public static class TestResource {
    
        @PUT
        @Path("setTest3")
        @Produces("application/xml")
        @Consumes("application/xml")
        public TestCompositeObject setTest3(TestCompositeObject transfer) {
            return transfer;
        }
    }
}
