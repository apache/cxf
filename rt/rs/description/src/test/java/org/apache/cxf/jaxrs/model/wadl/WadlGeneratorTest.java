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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.impl.ContainerRequestContextImpl;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.apache.ws.commons.schema.constants.Constants;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WadlGeneratorTest {

    private IMocksControl control;

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        control.makeThreadSafe(true);
    }

    @Test
    public void testNoWadl() {
        WadlGenerator wg = new WadlGenerator();
        Message m = new MessageImpl();
        m.setExchange(new ExchangeImpl());
        assertNull(handleRequest(wg, m));
    }

    @Test
    public void testAllowList() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        List<String> allowList = new ArrayList<>();
        allowList.add("123.123.123.123");
        wg.setAllowList(allowList);
        wg.setExternalLinks(Collections.singletonList("http://books.xsd"));

        ClassResourceInfo cri =
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/bookstore/1", WadlGenerator.WADL_QUERY, cri);
        Response response = handleRequest(wg, m);
        assertEquals(response.getStatus(), 404);
    }
    
    @Test
    public void testCustomSchemaJaxbContextPrefixes() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        wg.setSchemaLocations(Collections.singletonList("classpath:/book1.xsd"));

        ClassResourceInfo cri =
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/bookstore/1", WadlGenerator.WADL_QUERY, cri);
        Response r = handleRequest(wg, m);
        checkResponse(r);
        Document doc = StaxUtils.read(new StringReader(r.getEntity().toString()));
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
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, cri);
        Response r = handleRequest(wg, m);
        checkResponse(r);
        Document doc = StaxUtils.read(new StringReader(r.getEntity().toString()));
        List<Element> grammarEls = DOMUtils.getChildrenWithName(doc.getDocumentElement(),
            WadlGenerator.WADL_NS, "grammars");
        assertEquals(1, grammarEls.size());
        List<Element> schemasEls = DOMUtils.getChildrenWithName(grammarEls.get(0),
                                                                Constants.URI_2001_SCHEMA_XSD, "schema");
        assertEquals(1, schemasEls.size());
        assertEquals("http://books", schemasEls.get(0).getAttribute("targetNamespace"));
        List<Element> elementEls = DOMUtils.getChildrenWithName(schemasEls.get(0),
                                                                Constants.URI_2001_SCHEMA_XSD, "element");
        assertEquals(1, elementEls.size());
        assertTrue(checkElement(elementEls, "books", "books"));

        List<Element> complexTypesEls = DOMUtils.getChildrenWithName(schemasEls.get(0),
                                                                     Constants.URI_2001_SCHEMA_XSD, "complexType");
        assertEquals(1, complexTypesEls.size());
        assertTrue(checkComplexType(complexTypesEls, "books"));

        List<Element> importEls = DOMUtils.getChildrenWithName(schemasEls.get(0),
                                                               Constants.URI_2001_SCHEMA_XSD, "import");
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
        Message m = mockMessage("http://localhost:8080/baz", "/bookstore/1", WadlGenerator.WADL_QUERY, cri);
        Response r = handleRequest(wg, m);
        checkResponse(r);
        Document doc = StaxUtils.read(new StringReader(r.getEntity().toString()));
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
        Message m = mockMessage("http://localhost:8080/baz", "/bookstore/1", WadlGenerator.WADL_QUERY, cri);
        Response r = handleRequest(wg, m);
        checkResponse(r);
        Document doc = StaxUtils.read(new StringReader(r.getEntity().toString()));
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
        Message m = mockMessage("http://localhost:8080/baz", "/bookstore/1", WadlGenerator.WADL_QUERY, cri);
        Response r = handleRequest(wg, m);
        checkResponse(r);
        Document doc = StaxUtils.read(new StringReader(r.getEntity().toString()));
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
        Message m = mockMessage("http://localhost:8080/baz", "/bookstore/1", WadlGenerator.WADL_QUERY, cri);
        Response r = handleRequest(wg, m);
        checkResponse(r);
        Document doc = StaxUtils.read(new StringReader(r.getEntity().toString()));
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
        Message m = mockMessage("http://localhost:8080/baz", "/bookstore/1", WadlGenerator.WADL_QUERY, cri);
        Response r = handleRequest(wg, m);
        checkResponse(r);
        Document doc = StaxUtils.read(new StringReader(r.getEntity().toString()));
        checkDocs(doc.getDocumentElement(), "My Application", "", "");
        checkGrammars(doc.getDocumentElement(), "thebook", "books", "thebook2s", "thebook2", "thechapter");
        List<Element> els = getWadlResourcesInfo(doc, "http://localhost:8080/baz", 1);
        checkBookStoreInfo(els.get(0),
                           "ns1:thebook",
                           "ns1:thebook2",
                           "ns1:thechapter",
                           "ns1:books");
    }

    @Test
    public void testSingleRootResourceNoPrefixIncrement() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        wg.setApplicationTitle("My Application");
        wg.setNamespacePrefix("ns");
        wg.setIncrementNamespacePrefix(false);
        ClassResourceInfo cri =
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/bookstore/1", WadlGenerator.WADL_QUERY, cri);
        Response r = handleRequest(wg, m);
        checkResponse(r);
        Document doc = StaxUtils.read(new StringReader(r.getEntity().toString()));
        checkDocs(doc.getDocumentElement(), "My Application", "", "");
        checkGrammars(doc.getDocumentElement(), "thebook", "books", "thebook2s", "thebook2", "thechapter");
        List<Element> els = getWadlResourcesInfo(doc, "http://localhost:8080/baz", 1);
        checkBookStoreInfo(els.get(0),
                           "ns:thebook",
                           "ns:thebook2",
                           "ns:thechapter",
                           "ns:books");
    }

    @Test
    public void testTwoSchemasSameNs() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        wg.setApplicationTitle("My Application");
        wg.setNamespacePrefix("ns");
        ClassResourceInfo cri =
            ResourceUtils.createClassResourceInfo(TestResource.class, TestResource.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/", WadlGenerator.WADL_QUERY, cri);
        Response r = handleRequest(wg, m);
        checkResponse(r);
        Document doc = StaxUtils.read(new StringReader(r.getEntity().toString()));
        checkDocs(doc.getDocumentElement(), "My Application", "", "");
        List<Element> grammarEls = DOMUtils.getChildrenWithName(doc.getDocumentElement(),
                                                                WadlGenerator.WADL_NS,
                                                                "grammars");
        assertEquals(1, grammarEls.size());
        List<Element> schemasEls = DOMUtils.getChildrenWithName(grammarEls.get(0),
                                                                Constants.URI_2001_SCHEMA_XSD,
                                                                "schema");
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
        Message m = mockMessage("http://localhost:8080/baz", "/", WadlGenerator.WADL_QUERY, cri);
        Response r = handleRequest(wg, m);
        checkResponse(r);
        Document doc = StaxUtils.read(new StringReader(r.getEntity().toString()));
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
                     r.getMetadata().getFirst(HttpHeaders.CONTENT_TYPE).toString());
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
        List<ClassResourceInfo> cris = new ArrayList<>();
        cris.add(cri1);
        cris.add(cri2);
        Message m = mockMessage("http://localhost:8080/baz", "", WadlGenerator.WADL_QUERY, cris);
        Response r = handleRequest(wg, m);
        assertEquals(WadlGenerator.WADL_TYPE.toString(),
                     r.getMetadata().getFirst(HttpHeaders.CONTENT_TYPE).toString());
        String wadl = r.getEntity().toString();
        Document doc = StaxUtils.read(new StringReader(wadl));
        checkGrammars(doc.getDocumentElement(), "thebook", "books", "thebook2s", "thebook2", "thechapter");
        List<Element> els = getWadlResourcesInfo(doc, "http://localhost:8080/baz", 2);
        checkBookStoreInfo(els.get(0), "prefix1:thebook", "prefix1:thebook2", "prefix1:thechapter");
        Element orderResource = els.get(1);
        assertEquals("/orders", orderResource.getAttribute("path"));
    }

    private Response handleRequest(WadlGenerator wg, Message m) {
        wg.doFilter(new ContainerRequestContextImpl(m, true, false), m);
        return m.getExchange().get(Response.class);
    }

    private void checkGrammars(Element appElement,
                               String bookEl,
                               String book2El,
                               String chapterEl) {
        checkGrammars(appElement, bookEl, null, null, book2El, chapterEl);
    }
    private void checkGrammars(Element appElement,
                               String bookEl,
                               String booksEl,
                               String booksEl2,
                               String book2El,
                               String chapterEl) {
        List<Element> grammarEls = DOMUtils.getChildrenWithName(appElement, WadlGenerator.WADL_NS,
                                                                "grammars");
        assertEquals(1, grammarEls.size());
        List<Element> schemasEls = DOMUtils.getChildrenWithName(grammarEls.get(0),
                                                                Constants.URI_2001_SCHEMA_XSD,
                                                                "schema");
        assertEquals(1, schemasEls.size());
        assertEquals("http://superbooks", schemasEls.get(0).getAttribute("targetNamespace"));
        List<Element> elementEls = DOMUtils.getChildrenWithName(schemasEls.get(0),
                                                                Constants.URI_2001_SCHEMA_XSD,
                                                                "element");

        int size = book2El == null ? 2 : 3;
        int elementSize = size;
        if (booksEl != null) {
            elementSize += 2;
        }

        assertEquals(elementSize, elementEls.size());

        assertTrue(checkElement(elementEls, bookEl, "book"));
        if (book2El != null) {
            assertTrue(checkElement(elementEls, book2El, "book2"));
        }
        assertTrue(checkElement(elementEls, chapterEl, "chapter"));
        if (booksEl != null) {
            assertTrue(checkElement(elementEls, booksEl, "books"));
        }
        if (booksEl2 != null) {
            assertTrue(checkElement(elementEls, booksEl2, "thebook2s"));
        }

        List<Element> complexTypesEls = DOMUtils.getChildrenWithName(schemasEls.get(0),
                                                                     Constants.URI_2001_SCHEMA_XSD,
                                                                     "complexType");
        assertEquals(size, complexTypesEls.size());

        assertTrue(checkComplexType(complexTypesEls, "book"));
        if (book2El != null) {
            assertTrue(checkComplexType(complexTypesEls, "book2"));
        }
        assertTrue(checkComplexType(complexTypesEls, "chapter"));
    }

    private void checkGrammarsWithLinks(Element appElement, List<String> links) {
        assertFalse(links.isEmpty());
        List<Element> grammarEls = DOMUtils.getChildrenWithName(appElement, WadlGenerator.WADL_NS,
                                                                "grammars");
        assertEquals(1, grammarEls.size());
        List<Element> schemasEls = DOMUtils.getChildrenWithName(grammarEls.get(0),
                                                                Constants.URI_2001_SCHEMA_XSD,
                                                                "schema");
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

    private boolean checkTypeName(Element el, String typeName, String name) {
        String pfx = "";
        String tn = typeName;
        if (tn.contains(":")) {
            pfx = tn.substring(0, tn.indexOf(':'));
            tn = tn.substring(tn.indexOf(':') + 1);
        }
        pfx = el.lookupNamespaceURI(pfx);

        return tn.equals(name) && pfx.length() > 5;
    }

    private boolean checkElement(List<Element> els, String name, String localTypeName) {
        for (Element e : els) {
            if (name.equals(e.getAttribute("name"))) {
                String type = e.getAttribute("type");
                if (!StringUtils.isEmpty(type)) {
                    if (checkTypeName(e, type, localTypeName)) {
                        return true;
                    }
                } else if ("books".equals(name) || "thebook2s".equals(name)) {
                    boolean thebooks2 = "thebook2s".equals(name);
                    Element ctElement =
                        (Element)e.getElementsByTagNameNS(Constants.URI_2001_SCHEMA_XSD,
                                                          "complexType").item(0);
                    Element seqElement =
                        (Element)ctElement.getElementsByTagNameNS(Constants.URI_2001_SCHEMA_XSD,
                                                          "sequence").item(0);
                    Element xsElement =
                        (Element)seqElement.getElementsByTagNameNS(Constants.URI_2001_SCHEMA_XSD,
                                                          "element").item(0);
                    String ref = xsElement.getAttribute("ref");
                    if (checkTypeName(e, ref, thebooks2 ? "thebook2" : "thebook")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void checkBookStoreInfo(Element resource,
                                    String bookEl,
                                    String book2El,
                                    String chapterEl) {
        checkBookStoreInfo(resource, bookEl, book2El, chapterEl, null);
    }

    private void checkBookStoreInfo(Element resource,
                                    String bookEl,
                                    String book2El,
                                    String chapterEl,
                                    String booksEl) {
        assertEquals("/bookstore/{id}", resource.getAttribute("path"));

        checkDocs(resource, "book store \"resource\"", "super resource", "en-us");

        List<Element> resourceEls = getElements(resource, "resource", 10);

        assertEquals("/book2", resourceEls.get(0).getAttribute("path"));
        assertEquals("/books/{bookid}", resourceEls.get(1).getAttribute("path"));
        assertEquals("/chapter", resourceEls.get(2).getAttribute("path"));
        assertEquals("/chapter2", resourceEls.get(3).getAttribute("path"));
        assertEquals("/thebooks2", resourceEls.get(4).getAttribute("path"));
        assertEquals("/thestore", resourceEls.get(5).getAttribute("path"));
        assertEquals("/books/\"{bookid}\"", resourceEls.get(6).getAttribute("path"));
        assertEquals("/booksubresource", resourceEls.get(7).getAttribute("path"));
        assertEquals("/form", resourceEls.get(8).getAttribute("path"));
        assertEquals("/itself", resourceEls.get(9).getAttribute("path"));

        // verify root resource starting with "/"
        // must have a single template parameter
        verifyParameters(resource, 2,
                         new Param("id", "template", "xs:long"),
                         new Param("a", "template", "xs:int"));

        // must have 4 methods, 2 GETs, POST and PUT
        List<Element> methodEls = getElements(resource, "method", 4);

        // verify 1st Root GET
        try {
            verifyFirstRootGet(methodEls.get(0));
        } catch (Throwable ex) {
            verifyFirstRootGet(methodEls.get(1));
        }

        // verify 2nd Root GET
        try {
            verifySecondRootGet(methodEls.get(1), booksEl);
        } catch (Throwable ex) {
            verifySecondRootGet(methodEls.get(0), booksEl);
        }

        // verify POST
        assertEquals("POST", methodEls.get(2).getAttribute("name"));
        Element formRep = verifyRepresentation(methodEls.get(2), "request", "multipart/form-data", "");
        checkDocs(formRep, "", "Attachments, max < 10", "");

        // verify PUT
        assertEquals("PUT", methodEls.get(3).getAttribute("name"));
        verifyRepresentation(methodEls.get(3), "request", "text/plain", "");

        verifyResponseWithStatus(methodEls.get(3), "204");

        // verify resource starting with /book2
        verifyGetResourceMethod(resourceEls.get(0), book2El, null);

        //verify resource starting with /books/{bookid}
        checkDocs(resourceEls.get(1), "", "Resource books/{bookid}", "");
        verifyParameters(resourceEls.get(1), 3,
                         new Param("id", "template", "xs:int", "book id"),
                         new Param("bookid", "template", "xs:int"),
                         new Param("mid", "matrix", "xs:string", false, null, "mid > 5"));

        // and 2 methods
        methodEls = getElements(resourceEls.get(1), "method", 2);

        // POST
        assertEquals("POST", methodEls.get(0).getAttribute("name"));
        checkDocs(methodEls.get(0), "", "Update the books collection", "");
        List<Element> requestEls = getElements(methodEls.get(0), "request", 1);

        checkDocs(requestEls.get(0), "", "Request", "");

        verifyParameters(requestEls.get(0), 5,
                         new Param("hid", "header", "xs:int"),
                         new Param("provider.bar", "query", "xs:int"),
                         new Param("bookstate", "query", "xs:string",
                                 new HashSet<>(Arrays.asList("NEW", "USED", "OLD"))),
                         new Param("orderstatus", "query", "xs:string",
                                 new HashSet<>(Arrays.asList("INVOICED", "NOT_INVOICED"))),
                         new Param("a", "query", "xs:string", true));

        verifyXmlJsonRepresentations(requestEls.get(0), book2El, "InputBook");
        List<Element> responseEls = getElements(methodEls.get(0), "response", 1);
        checkDocs(responseEls.get(0), "", "Response", "");
        String status = responseEls.get(0).getAttribute("status");
        assertTrue("201 200".equals(status) || "200 201".equals(status));
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

        verifyGetResourceMethod(resourceEls.get(5), bookEl, null);

        // verify resource starting from /booksubresource
        // should have 2 parameters
        verifyParameters(resourceEls.get(7), 2,
                         new Param("id", "template", "xs:int"),
                         new Param("mid", "matrix", "xs:int"));
        checkDocs(resourceEls.get(7), "", "Book subresource", "");
        // should have 4 child resources
        List<Element> subResourceEls = getElements(resourceEls.get(7), "resource", 6);

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

    private void verifyFirstRootGet(Element methodEl) {
        assertEquals("GET", methodEl.getAttribute("name"));
        assertEquals(0, DOMUtils.getChildrenWithName(methodEl,
                        WadlGenerator.WADL_NS, "param").size());
        // check request
        List<Element> requestEls = getElements(methodEl, "request", 1);

        // 6 parameters are expected
        verifyParameters(requestEls.get(0), 7,
                         new Param("b", "query", "xs:int"),
                         new Param("aProp", "query", "xs:int"),
                         new Param("c.a", "query", "xs:int"),
                         new Param("c.b", "query", "xs:int"),
                         new Param("c.d.a", "query", "xs:boolean"),
                         new Param("c.d2.a", "query", "xs:boolean"),
                         new Param("e", "query", "xs:string", Collections.singleton("A")));

        assertEquals(0, DOMUtils.getChildrenWithName(requestEls.get(0),
                         WadlGenerator.WADL_NS, "representation").size());
        //check response
        verifyRepresentation(methodEl, "response", "text/plain", "");
    }

    private void verifySecondRootGet(Element methodEl, String booksEl) {
        assertEquals("GET", methodEl.getAttribute("name"));
        checkDocs(methodEl, "", "Get Books", "");
        if (booksEl != null) {
            verifyRepresentation(methodEl, "response", "application/xml", booksEl);
        }
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
            boolean found = false;
            for (int y = 0; y < params.length; y++) {
                if (params[y].getName().equals(paramsEls.get(i).getAttribute("name"))) {
                    checkParameter(paramsEls.get(i), params[y]);
                    found = true;
                }
            }
            assertTrue(found);
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
        assertEquals(p.getDefaultValue(), paramEl.getAttribute("default"));
        Set<String> options = p.getOptions();
        if (options != null) {
            Set<String> actualOptions = new HashSet<>();
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
        Element resourcesEl = resourcesEls.get(0);
        assertEquals(baseURI, resourcesEl.getAttribute("base"));
        List<Element> resourceEls =
            DOMUtils.getChildrenWithName(resourcesEl,
                                         WadlGenerator.WADL_NS, "resource");
        assertEquals(size, resourceEls.size());
        return resourceEls;
    }

    private Message mockMessage(String baseAddress, String pathInfo, String query,
                                ClassResourceInfo cri) throws Exception {
        return mockMessage(baseAddress, pathInfo, query, Collections.singletonList(cri));

    }
    private Message mockMessage(String baseAddress, String pathInfo, String query,
                                List<ClassResourceInfo> cris) throws Exception {
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

        Endpoint endpoint = new EndpointImpl(null, null, epr);
        e.put(Endpoint.class, endpoint);
        endpoint.put(ServerProviderFactory.class.getName(), ServerProviderFactory.getInstance());
        e.setDestination(d);
        BindingInfo bi = control.createMock(BindingInfo.class);
        epr.setBinding(bi);
        bi.getProperties();
        EasyMock.expectLastCall().andReturn(Collections.emptyMap()).anyTimes();
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
        private String defaultValue = "";
        private boolean repeating;
        private Set<String> options;
        Param(String name, String type, String schemaType) {
            this(name, type, schemaType, false);
        }

        Param(String name, String type, String schemaType, Set<String> opts) {
            this.name = name;
            this.type = type;
            this.schemaType = schemaType;
            this.options = opts;
        }

        Param(String name, String type, String schemaType, boolean repeating) {
            this(name, type, schemaType, repeating, null);
        }

        Param(String name, String type, String schemaType, String docs) {
            this(name, type, schemaType, false, docs);
        }


        Param(String name, String type, String schemaType, boolean repeating, String docs) {
            this.name = name;
            this.type = type;
            this.schemaType = schemaType;
            this.docs = docs;
            this.repeating = repeating;
        }

        Param(String name, String type, String schemaType, boolean repeating, String docs,
              String defaultValue) {
            this(name, type, schemaType, repeating, docs);
            this.defaultValue = defaultValue;
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

        public String getDefaultValue() {
            return defaultValue;
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

    @XmlRootElement(namespace = "http://example.com")
    public static class Super {
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

    public static class SuperResource<T extends Super> {

        @PUT
        @Path("set")
        @Produces("application/xml")
        @Consumes("application/xml")
        public T set(T transfer) {
            return transfer;
        }

    }

    @XmlRootElement(namespace = "http://example.com")
    public static class Actual extends Super { }

    public static class ActualResource extends SuperResource<Actual> { }

    private void setUpGenericImplementationTest() {
        ServerProviderFactory.getInstance().clearProviders();
        AbstractResourceInfo.clearAllMaps();
    }

    @Test
    public void testGenericImplementation() throws Exception {
        setUpGenericImplementationTest();

        WadlGenerator wg = new WadlGenerator();
        wg.setApplicationTitle("My Application");
        wg.setNamespacePrefix("ns");
        ClassResourceInfo cri =
            ResourceUtils.createClassResourceInfo(ActualResource.class, ActualResource.class, true, true);
        Message m = mockMessage("http://example.com", "/", WadlGenerator.WADL_QUERY, cri);
        Response r = handleRequest(wg, m);
        checkResponse(r);
        Document doc = StaxUtils.read(new StringReader(r.getEntity().toString()));
        checkDocs(doc.getDocumentElement(), "My Application", "", "");
        List<Element> grammarEls = DOMUtils.getChildrenWithName(doc.getDocumentElement(),
                                                                WadlGenerator.WADL_NS,
                                                                "grammars");
        assertEquals(1, grammarEls.size());
        List<Element> schemasEls = DOMUtils.getChildrenWithName(grammarEls.get(0),
                                                                Constants.URI_2001_SCHEMA_XSD,
                                                                "schema");
        assertEquals(2, schemasEls.size());
        
        List<Element> importEls = DOMUtils.getChildrenWithName(schemasEls.get(0),
                                                               Constants.URI_2001_SCHEMA_XSD,
                                                               "import");
        int schemaElementsIndex = !importEls.isEmpty() ? 0 : 1;
        int schemaTypesIndex = schemaElementsIndex == 0 ? 1 : 0;
        
        checkGenericImplSchemaWithTypes(schemasEls.get(schemaTypesIndex));
        checkGenericImplSchemaWithElements(schemasEls.get(schemaElementsIndex));

        List<Element> reps = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                       WadlGenerator.WADL_NS, "representation");
        assertEquals(2, reps.size());
        assertEquals("ns1:actual", reps.get(0).getAttribute("element"));
        assertEquals("ns1:actual", reps.get(1).getAttribute("element"));

    }

    private void checkGenericImplSchemaWithTypes(Element schemaEl) {
        List<Element> complexTypeEls = DOMUtils.getChildrenWithName(schemaEl,
                                                                    Constants.URI_2001_SCHEMA_XSD,
                                                                    "complexType");
        assertEquals(2, complexTypeEls.size());
        int actualTypeIndex = "actual".equals(complexTypeEls.get(0).getAttribute("name")) ? 0 : 1;
        int superTypeIndex = actualTypeIndex == 0 ? 1 : 0;
        
        assertEquals("actual", complexTypeEls.get(actualTypeIndex).getAttribute("name"));
        
        Element ccActualElement =
                (Element)complexTypeEls.get(actualTypeIndex).getElementsByTagNameNS(Constants.URI_2001_SCHEMA_XSD,
                                                  "complexContent").item(0);
        Element extensionActualElement =
            (Element)ccActualElement.getElementsByTagNameNS(Constants.URI_2001_SCHEMA_XSD,
                                              "extension").item(0);
        Element sequenceActualElement =
                (Element)ccActualElement.getElementsByTagNameNS(Constants.URI_2001_SCHEMA_XSD,
                                                  "sequence").item(0);
        assertEquals("super", extensionActualElement.getAttribute("base"));
        assertEquals(0, sequenceActualElement.getChildNodes().getLength());

        assertEquals("super", complexTypeEls.get(superTypeIndex).getAttribute("name"));

        Element sequenceSuperElement =
                (Element)complexTypeEls.get(superTypeIndex).getElementsByTagNameNS(Constants.URI_2001_SCHEMA_XSD,
                                                  "sequence").item(0);
        List<Element> superEls = DOMUtils.getChildrenWithName(sequenceSuperElement,
                Constants.URI_2001_SCHEMA_XSD,
                "element");
        assertEquals(2, superEls.size());
        assertEquals("id", superEls.get(0).getAttribute("name"));
        assertEquals("xs:int", superEls.get(0).getAttribute("type"));
        assertEquals("name", superEls.get(1).getAttribute("name"));
        assertEquals("xs:string", superEls.get(1).getAttribute("type"));
        
    }

    private void checkGenericImplSchemaWithElements(Element schemaEl) {
        assertEquals("http://example.com", schemaEl.getAttribute("targetNamespace"));

        List<Element> importEls = DOMUtils.getChildrenWithName(schemaEl,
                                                               Constants.URI_2001_SCHEMA_XSD,
                                                               "import");
                                                       
        assertEquals(1, importEls.size());

        List<Element> typeEls = DOMUtils.getChildrenWithName(schemaEl,
                Constants.URI_2001_SCHEMA_XSD,
                "element");
        assertEquals(2, typeEls.size());
        assertEquals("actual", typeEls.get(0).getAttribute("name"));
        assertEquals("actual", typeEls.get(0).getAttribute("type"));
        assertEquals("super", typeEls.get(1).getAttribute("name"));
        assertEquals("super", typeEls.get(1).getAttribute("type"));
        
    }

}