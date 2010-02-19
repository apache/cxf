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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

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
    }
    
    private void checkResponse(Response r) throws Exception {
        assertNotNull(r);
        assertEquals(WadlGenerator.WADL_TYPE.toString(),
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
        ClassResourceInfo cri1 = 
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        ClassResourceInfo cri2 = 
            ResourceUtils.createClassResourceInfo(Orders.class, Orders.class, true, true);
        List<ClassResourceInfo> cris = new ArrayList<ClassResourceInfo>();
        cris.add(cri1);
        cris.add(cri2);
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, cris);
        Response r = wg.handleRequest(m, null);
        checkResponse(r);
        Document doc = DOMUtils.readXml(new StringReader(r.getEntity().toString()));
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
        assertEquals(3, elementEls.size());
        assertTrue(checkElement(elementEls, bookEl, "tns:book"));
        assertTrue(checkElement(elementEls, book2El, "tns:book2"));
        assertTrue(checkElement(elementEls, chapterEl, "tns:chapter"));
        
        List<Element> complexTypesEls = DOMUtils.getChildrenWithName(schemasEls.get(0), 
                                        XmlSchemaConstants.XSD_NAMESPACE_URI, "complexType");
        assertEquals(3, complexTypesEls.size());
        
        assertTrue(checkComplexType(complexTypesEls, "book"));
        assertTrue(checkComplexType(complexTypesEls, "book2"));
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
        
        checkRootDocs(resource);
        
        List<Element> resourceEls = DOMUtils.getChildrenWithName(resource, 
                                         WadlGenerator.WADL_NS, "resource");
        assertEquals(6, resourceEls.size());        
        assertEquals("/book2", resourceEls.get(0).getAttribute("path"));
        assertEquals("/books/{bookid}", resourceEls.get(1).getAttribute("path"));
        assertEquals("/chapter", resourceEls.get(2).getAttribute("path"));
        assertEquals("/books/{bookid}", resourceEls.get(3).getAttribute("path"));
        assertEquals("/booksubresource", resourceEls.get(4).getAttribute("path"));
        assertEquals("/itself", resourceEls.get(5).getAttribute("path"));
        
        // verify root resource starting with "/"
        // must have a single template parameter
        List<Element> paramsEls = DOMUtils.getChildrenWithName(resource, 
                                                               WadlGenerator.WADL_NS, "param");
        assertEquals(1, paramsEls.size());
        checkParameter(paramsEls.get(0), "id", "template", "xs:long");
        
        // must have 2 methods, GET and PUT
        List<Element> methodEls = DOMUtils.getChildrenWithName(resource, 
                                                               WadlGenerator.WADL_NS, "method");
        assertEquals(2, methodEls.size());
        
        // verify GET
        assertEquals("GET", methodEls.get(0).getAttribute("name"));
        assertEquals(0, DOMUtils.getChildrenWithName(methodEls.get(0), 
                        WadlGenerator.WADL_NS, "param").size());
        // check request 
        List<Element> requestEls = DOMUtils.getChildrenWithName(methodEls.get(0), 
                                                               WadlGenerator.WADL_NS, "request");
        assertEquals(1, requestEls.size());
        
        // 6 parameters are expected
        paramsEls = DOMUtils.getChildrenWithName(requestEls.get(0), 
                                                 WadlGenerator.WADL_NS, "param");
        assertEquals(4, paramsEls.size());
        checkParameter(paramsEls.get(0), "a", "query", "xs:int");
        checkParameter(paramsEls.get(1), "c.a", "query", "xs:int");
        checkParameter(paramsEls.get(2), "c.b", "query", "xs:int");
        checkParameter(paramsEls.get(3), "c.d.a", "query", "xs:int");
        assertEquals(0, DOMUtils.getChildrenWithName(requestEls.get(0), 
                         WadlGenerator.WADL_NS, "representation").size());
        //check response
        verifyPlainRepresentation(methodEls.get(0), "response");
        
        // verify PUT
        assertEquals("PUT", methodEls.get(1).getAttribute("name"));
        verifyPlainRepresentation(methodEls.get(1), "request");
        
        verifyResponseWithStatus(methodEls.get(1), "204");
        
        // verify resource starting with /book2
        verifyGetResourceMethod(resourceEls.get(0), book2El);
        
        //verify resource starting with /books/{bookid}
        paramsEls = DOMUtils.getChildrenWithName(resourceEls.get(1), 
                                                               WadlGenerator.WADL_NS, "param");
        // should have 3 parameters
        assertEquals(3, paramsEls.size());
        checkParameter(paramsEls.get(0), "id", "template", "xs:int");
        checkParameter(paramsEls.get(1), "bookid", "template", "xs:int");
        checkParameter(paramsEls.get(2), "mid", "matrix", "xs:int");
        
        // and 2 methods
        methodEls = DOMUtils.getChildrenWithName(resourceEls.get(1), 
                                                 WadlGenerator.WADL_NS, "method");
        assertEquals(2, methodEls.size());
        
        // POST 
        assertEquals("POST", methodEls.get(0).getAttribute("name"));
        
        requestEls = DOMUtils.getChildrenWithName(methodEls.get(0), 
                                             WadlGenerator.WADL_NS, "request");
        assertEquals(1, requestEls.size());
        paramsEls = DOMUtils.getChildrenWithName(requestEls.get(0), 
                                                 WadlGenerator.WADL_NS, "param");
        // should have 2 parameters
        assertEquals(2, paramsEls.size());
        checkParameter(paramsEls.get(0), "hid", "header", "xs:int");
        checkParameter(paramsEls.get(1), "provider.bar", "query", "xs:int");
        verifyXmlJsonRepresentations(requestEls.get(0), book2El);
        
        // PUT
        assertEquals("PUT", methodEls.get(1).getAttribute("name"));
        requestEls = DOMUtils.getChildrenWithName(methodEls.get(1), 
                                                                WadlGenerator.WADL_NS, "request");
        assertEquals(1, requestEls.size());
        verifyXmlJsonRepresentations(requestEls.get(0), bookEl);
        verifyResponseWithStatus(methodEls.get(1), "204");
        
        // verify resource starting with /chapter
        verifyGetResourceMethod(resourceEls.get(2), chapterEl);
        
        // verify resource starting from /booksubresource
        // should have 2 parameters
        paramsEls = DOMUtils.getChildrenWithName(resourceEls.get(4), 
                         WadlGenerator.WADL_NS, "param");
        assertEquals(2, paramsEls.size());
        checkParameter(paramsEls.get(0), "id", "template", "xs:int");
        checkParameter(paramsEls.get(1), "mid", "matrix", "xs:int");
        
        // should have 4 child resources
        List<Element> subResourceEls = DOMUtils.getChildrenWithName(resourceEls.get(4), 
                                         WadlGenerator.WADL_NS, "resource");
        assertEquals(4, subResourceEls.size());        
        assertEquals("/book", subResourceEls.get(0).getAttribute("path"));
        assertEquals("/form1", subResourceEls.get(1).getAttribute("path"));
        assertEquals("/form2", subResourceEls.get(2).getAttribute("path"));
        assertEquals("/chapter/{cid}", subResourceEls.get(3).getAttribute("path"));
        // verify subresource /book
        // GET 
        verifyGetResourceMethod(subResourceEls.get(0), bookEl);
        
        // verify subresource /chapter/{id}
        List<Element> chapterMethodEls = DOMUtils.getChildrenWithName(subResourceEls.get(3), 
                                                                    WadlGenerator.WADL_NS, "resource");
        assertEquals(1, chapterMethodEls.size());        
        assertEquals("/id", chapterMethodEls.get(0).getAttribute("path"));
        paramsEls = DOMUtils.getChildrenWithName(subResourceEls.get(3), 
                                                 WadlGenerator.WADL_NS, "param");
        assertEquals(1, paramsEls.size());
        checkParameter(paramsEls.get(0), "cid", "template", "xs:int");
        // GET
        verifyGetResourceMethod(chapterMethodEls.get(0), chapterEl);
    }
    
    private void checkRootDocs(Element el) {
        List<Element> docsEls = DOMUtils.getChildrenWithName(el, 
                                                             WadlGenerator.WADL_NS, "doc");
        assertEquals(1, docsEls.size());
        assertEquals("book store resource", docsEls.get(0).getAttribute("title"));
        assertEquals("en-us", 
            docsEls.get(0).getAttributeNS("http://www.w3.org/XML/1998/namespace", "lang"));
    }
    
    private void verifyGetResourceMethod(Element element, String type) {
        List<Element> methodEls = DOMUtils.getChildrenWithName(element, WadlGenerator.WADL_NS, "method");
        assertEquals(1, methodEls.size());
        assertEquals("GET", methodEls.get(0).getAttribute("name"));
        assertEquals(0, DOMUtils.getChildrenWithName(methodEls.get(0), 
                      WadlGenerator.WADL_NS, "request").size());
        List<Element> responseEls = DOMUtils.getChildrenWithName(methodEls.get(0), 
                                WadlGenerator.WADL_NS, "response");
        assertEquals(1, responseEls.size());
        verifyXmlJsonRepresentations(responseEls.get(0), type);
    }
    
    private void verifyResponseWithStatus(Element element, String status) {
        List<Element> responseEls = DOMUtils.getChildrenWithName(element, 
                                       WadlGenerator.WADL_NS, "response");
        assertEquals(1, responseEls.size());
        assertEquals(status, responseEls.get(0).getAttribute("status"));
        assertEquals(0, DOMUtils.getChildrenWithName(responseEls.get(0), 
            WadlGenerator.WADL_NS, "representation").size());
    }
    
    private void verifyPlainRepresentation(Element element, String name) {
        List<Element> responseEls = DOMUtils.getChildrenWithName(element, 
                                 WadlGenerator.WADL_NS, name);
        assertEquals(1, responseEls.size());
        List<Element> representationEls = DOMUtils.getChildrenWithName(responseEls.get(0), 
                    WadlGenerator.WADL_NS, "representation"); 
        assertEquals(1, representationEls.size());
        assertEquals("text/plain", representationEls.get(0).getAttribute("mediaType"));
        assertEquals("", representationEls.get(0).getAttribute("element"));
    }
    
    private void verifyXmlJsonRepresentations(Element element, String type) {
        List<Element> repEls = DOMUtils.getChildrenWithName(element, 
                                                            WadlGenerator.WADL_NS, "representation");
        assertEquals(2, repEls.size());
        assertEquals("application/xml", repEls.get(0).getAttribute("mediaType"));
        assertEquals(type, repEls.get(0).getAttribute("element"));
        assertEquals("application/json", repEls.get(1).getAttribute("mediaType"));
        assertEquals("", repEls.get(1).getAttribute("element"));
    }
    
    private void checkParameter(Element paramEl, String name, String style, String type) {
        assertEquals(name, paramEl.getAttribute("name"));
        assertEquals(style, paramEl.getAttribute("style"));
        assertEquals(type, paramEl.getAttribute("type"));
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
    
}
