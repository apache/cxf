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
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
    public void testSingleRootResource() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        ClassResourceInfo cri = 
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, null);
        
        Response r = wg.handleRequest(m, cri);
        checkResponse(r);
        List<Element> els = getWadlResourcesInfo("http://localhost:8080/baz", 1, r.getEntity().toString());
        checkBookStoreInfo(els.get(0));
        
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
        List<Element> els = getWadlResourcesInfo("http://localhost:8080/baz", 2, r.getEntity().toString());
        checkBookStoreInfo(els.get(0));
        Element orderResource = els.get(1);
        assertEquals("/orders", orderResource.getAttribute("path"));
    }

    private void checkBookStoreInfo(Element resource) {
        assertEquals("/bookstore/{id}", resource.getAttribute("path"));
        
        List<Element> resourceEls = DOMUtils.getChildrenWithName(resource, 
                  "http://research.sun.com/wadl/2006/10", "resource");
        assertEquals(3, resourceEls.size());        
        assertEquals("/", resourceEls.get(0).getAttribute("path"));
        assertEquals("/books/{bookid}", resourceEls.get(1).getAttribute("path"));
        assertEquals("/booksubresource", resourceEls.get(2).getAttribute("path"));
        
        List<Element> methodEls = DOMUtils.getChildrenWithName(resourceEls.get(0), 
            "http://research.sun.com/wadl/2006/10", "method");
        assertEquals(1, methodEls.size());
        assertEquals("GET", methodEls.get(0).getAttribute("name"));
                                                           
        
        List<Element> paramsEls = DOMUtils.getChildrenWithName(resourceEls.get(1), 
                                  "http://research.sun.com/wadl/2006/10", "param");
        assertEquals(3, paramsEls.size());
        checkParameter(paramsEls.get(0), "id", "template");
        checkParameter(paramsEls.get(1), "bookid", "template");
        checkParameter(paramsEls.get(2), "mid", "matrix");
    }
    
    private void checkParameter(Element paramEl, String name, String type) {
        assertEquals(name, paramEl.getAttribute("name"));
        assertEquals(type, paramEl.getAttribute("style"));    
    }
    
    private List<Element> getWadlResourcesInfo(String baseURI, int size, String value) throws Exception {
        Document doc = DOMUtils.readXml(new StringReader(value));
        Element root = doc.getDocumentElement();
        assertEquals("http://research.sun.com/wadl/2006/10", root.getNamespaceURI());
        assertEquals("application", root.getLocalName());
        List<Element> resourcesEls = DOMUtils.getChildrenWithName(root, 
                                            "http://research.sun.com/wadl/2006/10", "resources");
        assertEquals(1, resourcesEls.size());
        Element resourcesEl =  resourcesEls.get(0);
        assertEquals(baseURI, resourcesEl.getAttribute("base"));
        List<Element> resourceEls = 
            DOMUtils.getChildrenWithName(resourcesEl, 
                                                "http://research.sun.com/wadl/2006/10", "resource");
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
