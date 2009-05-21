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
package org.apache.cxf.jaxrs.utils;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import javax.xml.bind.JAXBContext;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.Customer;
import org.apache.cxf.jaxrs.CustomerGender;
import org.apache.cxf.jaxrs.CustomerParameterHandler;
import org.apache.cxf.jaxrs.JAXBContextProvider;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.SimpleFactory;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.HttpServletResponseFilter;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.PathSegmentImpl;
import org.apache.cxf.jaxrs.impl.ProvidersImpl;
import org.apache.cxf.jaxrs.impl.RequestImpl;
import org.apache.cxf.jaxrs.impl.SecurityContextImpl;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalUriInfo;
import org.apache.cxf.jaxrs.lifecycle.PerRequestResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.MethodDispatcher;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.easymock.EasyMock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JAXRSUtilsTest extends Assert {
    
    @Before
    public void setUp() {
    }

    @Test
    public void testSelectBetweenMultipleResourceClasses() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStoreNoSubResource.class,
                              org.apache.cxf.jaxrs.resources.BookStore.class);
        sf.create();        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        MultivaluedMap<String, String> map = new MetadataMap<String, String>();
        ClassResourceInfo bStore = JAXRSUtils.selectResourceClass(resources, "/bookstore", map);
        assertEquals(bStore.getResourceClass(), org.apache.cxf.jaxrs.resources.BookStore.class);
        
        bStore = JAXRSUtils.selectResourceClass(resources, "/bookstore/", map);
        assertEquals(bStore.getResourceClass(), 
                     org.apache.cxf.jaxrs.resources.BookStore.class);
        
        bStore = JAXRSUtils.selectResourceClass(resources, "/bookstore/bar", map);
        assertEquals(bStore.getResourceClass(), 
                     org.apache.cxf.jaxrs.resources.BookStoreNoSubResource.class);
    }
    
    @Test
    public void testSelectBetweenMultipleResourceClasses2() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.TestResourceTemplate1.class,
                              org.apache.cxf.jaxrs.resources.TestResourceTemplate2.class);
        sf.create();        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        MultivaluedMap<String, String> map = new MetadataMap<String, String>();
        ClassResourceInfo bStore = JAXRSUtils.selectResourceClass(resources, "/1", map);
        assertEquals(bStore.getResourceClass(), org.apache.cxf.jaxrs.resources.TestResourceTemplate1.class);
        
        bStore = JAXRSUtils.selectResourceClass(resources, "/1/", map);
        assertEquals(bStore.getResourceClass(), 
                     org.apache.cxf.jaxrs.resources.TestResourceTemplate1.class);
        
        bStore = JAXRSUtils.selectResourceClass(resources, "/1/foo", map);
        assertEquals(bStore.getResourceClass(), 
                     org.apache.cxf.jaxrs.resources.TestResourceTemplate2.class);
        
        bStore = JAXRSUtils.selectResourceClass(resources, "/1/foo/bar", map);
        assertEquals(bStore.getResourceClass(), 
                     org.apache.cxf.jaxrs.resources.TestResourceTemplate2.class);
    }
    
    @Test
    public void testSelectBetweenMultipleResourceClasses3() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.TestResourceTemplate4.class,
                              org.apache.cxf.jaxrs.resources.TestResourceTemplate3.class);
        sf.create();        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        MultivaluedMap<String, String> map = new MetadataMap<String, String>();
        ClassResourceInfo bStore = JAXRSUtils.selectResourceClass(resources, "/", map);
        assertEquals(bStore.getResourceClass(), org.apache.cxf.jaxrs.resources.TestResourceTemplate3.class);
        
        bStore = JAXRSUtils.selectResourceClass(resources, "/test", map);
        assertEquals(bStore.getResourceClass(), 
                     org.apache.cxf.jaxrs.resources.TestResourceTemplate4.class);
        
    }
    
    @Test
    public void testFindTargetResourceClass() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStoreNoSubResource.class);
        sf.create();        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();

        String contentTypes = "*/*";
        
        //If acceptContentTypes does not specify a specific Mime type, the  
        //method is declared with a most specific ProduceMime type is selected.
        OperationResourceInfo ori = findTargetResourceClass(resources, null, 
             "/bookstore/1/books/123/", "GET", new MetadataMap<String, String>(), contentTypes, 
             getTypes("application/json,application/xml"));       
        assertNotNull(ori);
        assertEquals("getBookJSON", ori.getMethodToInvoke().getName());
        
        //test
        ori = findTargetResourceClass(resources, null, "/bookstore/1/books/123",
             "GET", new MetadataMap<String, String>(), contentTypes, getTypes("application/json"));        
        assertNotNull(ori);
        assertEquals("getBookJSON", ori.getMethodToInvoke().getName());
        
        //test 
        ori = findTargetResourceClass(resources, null, "/bookstore/1/books/123",
              "GET", new MetadataMap<String, String>(), contentTypes, getTypes("application/xml"));        
        assertNotNull(ori);
        assertEquals("getBook", ori.getMethodToInvoke().getName());
        
        //test 
        ori = findTargetResourceClass(resources, null, "/bookstore/1/books",
                      "GET", new MetadataMap<String, String>(), contentTypes, 
                      getTypes("application/xml"));        
        assertNotNull(ori);
        assertEquals("getBooks", ori.getMethodToInvoke().getName());
        
        //test find POST
        ori = findTargetResourceClass(resources, null, "/bookstore/1/books",
                 "POST", new MetadataMap<String, String>(), contentTypes, getTypes("application/xml"));       
        assertNotNull(ori);
        assertEquals("addBook", ori.getMethodToInvoke().getName());
        
        //test find PUT
        ori = findTargetResourceClass(resources, null, "/bookstore/1/books",
            "PUT", new MetadataMap<String, String>(), contentTypes, getTypes("application/xml"));  
        assertEquals("updateBook", ori.getMethodToInvoke().getName());
        
        //test find DELETE
        ori = findTargetResourceClass(resources, null, "/bookstore/1/books/123",
             "DELETE", new MetadataMap<String, String>(), contentTypes, getTypes("application/xml"));        
        assertNotNull(ori);
        assertEquals("deleteBook", ori.getMethodToInvoke().getName());     
        
    }
    
    private List<MediaType> getTypes(String types) {
        return JAXRSUtils.parseMediaTypes(types);
    }
    
    @Test
    public void testFindTargetResourceClassWithTemplates() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStoreTemplates.class);
        sf.create();        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();

        String contentTypes = "*/*";
        
        //If acceptContentTypes does not specify a specific Mime type, the  
        //method is declared with a most specific ProduceMime type is selected.
        MetadataMap<String, String> values = new MetadataMap<String, String>();
        OperationResourceInfo ori = findTargetResourceClass(resources, null, "/1/2/",
             "GET", values, contentTypes, getTypes("*/*"));       
        assertNotNull(ori);
        assertEquals("getBooks", ori.getMethodToInvoke().getName());
        assertEquals("Only id and final match groups should be there", 2, values.size());
        assertEquals("2 {id} values should've been picked up", 2, values.get("id").size());
        assertEquals("FINAL_MATCH_GROUP should've been picked up", 1, 
                     values.get(URITemplate.FINAL_MATCH_GROUP).size());
        assertEquals("First {id} is 1", "1", values.getFirst("id"));
        assertEquals("Second id is 2", "2", values.get("id").get(1));
        
        values = new MetadataMap<String, String>();
        ori = findTargetResourceClass(resources, null, "/2",
             "POST", values, contentTypes, getTypes("*/*"));       
        assertNotNull(ori);
        assertEquals("updateBookStoreInfo", ori.getMethodToInvoke().getName());
        assertEquals("Only id and final match groups should be there", 2, values.size());
        assertEquals("Only single {id} should've been picked up", 1, values.get("id").size());
        assertEquals("FINAL_MATCH_GROUP should've been picked up", 1, 
                     values.get(URITemplate.FINAL_MATCH_GROUP).size());
        assertEquals("Only the first {id} should've been picked up", "2", values.getFirst("id"));
        
        values = new MetadataMap<String, String>();
        ori = findTargetResourceClass(resources, null, "/3/4",
             "PUT", values, contentTypes, getTypes("*/*"));       
        assertNotNull(ori);
        assertEquals("updateBook", ori.getMethodToInvoke().getName());
        assertEquals("Only the first {id} should've been picked up", 3, values.size());
        assertEquals("Only the first {id} should've been picked up", 1, values.get("id").size());
        assertEquals("Only the first {id} should've been picked up", 1, values.get("bookId").size());
        assertEquals("Only the first {id} should've been picked up", 1, 
                     values.get(URITemplate.FINAL_MATCH_GROUP).size());
        assertEquals("Only the first {id} should've been picked up", "3", values.getFirst("id"));
        assertEquals("Only the first {id} should've been picked up", "4", values.getFirst("bookId"));
    }
    
    @Test
    public void testFindTargetResourceClassWithSubResource() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStore.class);
        sf.create();        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();

        String contentTypes = "*/*";
        
        OperationResourceInfo ori = findTargetResourceClass(resources,
               null, "/bookstore/books/123", "GET", new MetadataMap<String, String>(), contentTypes,
               getTypes("*/*"));       
        assertNotNull(ori);
        assertEquals("getBook", ori.getMethodToInvoke().getName());
        
        ori = findTargetResourceClass(resources, null, 
            "/bookstore/books/123/true/chapter/1", "GET", new MetadataMap<String, String>(), contentTypes,
            getTypes("*/*"));       
        assertNotNull(ori);
        assertEquals("getNewBook", ori.getMethodToInvoke().getName());
        
        ori = findTargetResourceClass(resources, null, "/bookstore/books",
            "POST", new MetadataMap<String, String>(), contentTypes, getTypes("*/*"));      
        assertNotNull(ori);
        assertEquals("addBook", ori.getMethodToInvoke().getName());
        
        ori = findTargetResourceClass(resources, null, "/bookstore/books",
             "PUT", new MetadataMap<String, String>(), contentTypes, getTypes("*/*"));        
        assertNotNull(ori);
        assertEquals("updateBook", ori.getMethodToInvoke().getName());
        
        ori = findTargetResourceClass(resources, null, "/bookstore/books/123",
            "DELETE", new MetadataMap<String, String>(), contentTypes, getTypes("*/*"));        
        assertNotNull(ori);
        assertEquals("deleteBook", ori.getMethodToInvoke().getName());
    }

    @Test
    public void testIntersectMimeTypesCompositeSubtype() throws Exception {
        List <MediaType> candidateList = 
            JAXRSUtils.intersectMimeTypes("application/bar+xml", "application/*+xml");  

        assertEquals(1, candidateList.size());
        assertEquals("application/bar+xml", candidateList.get(0).toString());
    }
    
    @Test
    public void testIntersectMimeTypes() throws Exception {
        //test basic
        List<MediaType> methodMimeTypes = new ArrayList<MediaType>(
             JAXRSUtils.parseMediaTypes("application/mytype,application/xml,application/json"));
        
        MediaType acceptContentType = MediaType.valueOf("application/json");
        List <MediaType> candidateList = JAXRSUtils.intersectMimeTypes(methodMimeTypes, 
                                                 MediaType.valueOf("application/json"));  

        assertEquals(1, candidateList.size());
        assertTrue(candidateList.get(0).toString().equals("application/json"));
        
        //test basic       
        methodMimeTypes = JAXRSUtils.parseMediaTypes(
            "application/mytype, application/json, application/xml");
        candidateList = JAXRSUtils.intersectMimeTypes(methodMimeTypes, 
                                                      MediaType.valueOf("application/json"));  

        assertEquals(1, candidateList.size());
        assertTrue(candidateList.get(0).toString().equals("application/json"));
        
        //test accept wild card */*       
        candidateList = JAXRSUtils.intersectMimeTypes(
            "application/mytype,application/json,application/xml", "*/*");  

        assertEquals(3, candidateList.size());
        
        //test accept wild card application/*       
        methodMimeTypes = JAXRSUtils.parseMediaTypes("text/html,text/xml,application/xml");
        acceptContentType = MediaType.valueOf("text/*");
        candidateList = JAXRSUtils.intersectMimeTypes(methodMimeTypes, acceptContentType);  

        assertEquals(2, candidateList.size());
        for (MediaType type : candidateList) {
            assertTrue("text/html".equals(type.toString()) 
                       || "text/xml".equals(type.toString()));            
        }
        
        //test produce wild card */*
        candidateList = JAXRSUtils.intersectMimeTypes("*/*", "application/json");

        assertEquals(1, candidateList.size());
        assertTrue("application/json".equals(candidateList.get(0).toString()));
        
        //test produce wild card application/*
        candidateList = JAXRSUtils.intersectMimeTypes("application/*", "application/json");  

        assertEquals(1, candidateList.size());
        assertTrue("application/json".equals(candidateList.get(0).toString()));        
        
        //test produce wild card */*, accept wild card */*
        candidateList = JAXRSUtils.intersectMimeTypes("*/*", "*/*");  

        assertEquals(1, candidateList.size());
        assertTrue("*/*".equals(candidateList.get(0).toString()));
    }
    
    @Test
    public void testIntersectMimeTypesTwoArray() throws Exception {
        //test basic
        List <MediaType> acceptedMimeTypes = 
            JAXRSUtils.parseMediaTypes("application/mytype, application/xml, application/json");
        
        List <MediaType> candidateList = 
            JAXRSUtils.intersectMimeTypes(acceptedMimeTypes, JAXRSUtils.ALL_TYPES);

        assertEquals(3, candidateList.size());
        for (MediaType type : candidateList) {
            assertTrue("application/mytype".equals(type.toString()) 
                       || "application/xml".equals(type.toString())
                       || "application/json".equals(type.toString()));
        }
        
        //test basic
        acceptedMimeTypes = Collections.singletonList(JAXRSUtils.ALL_TYPES);
        List<MediaType> providerMimeTypes = 
            JAXRSUtils.parseMediaTypes("application/mytype, application/xml, application/json");

        candidateList = JAXRSUtils.intersectMimeTypes(acceptedMimeTypes, providerMimeTypes);

        assertEquals(3, candidateList.size());
        for (MediaType type : candidateList) {
            assertTrue("application/mytype".equals(type.toString()) 
                       || "application/xml".equals(type.toString())
                       || "application/json".equals(type.toString()));
        }
        
        //test empty
        acceptedMimeTypes = JAXRSUtils.parseMediaTypes("application/mytype,application/xml");
        
        candidateList = JAXRSUtils.intersectMimeTypes(acceptedMimeTypes, 
                                                      MediaType.valueOf("application/json"));

        assertEquals(0, candidateList.size());
    }
    
    @Test
    public void testParseMediaTypes() throws Exception {
        List<MediaType> types = JAXRSUtils.parseMediaTypes("*");
        assertTrue(types.size() == 1 
                   && types.get(0).equals(JAXRSUtils.ALL_TYPES));
        types = JAXRSUtils.parseMediaTypes("text/*");
        assertTrue(types.size() == 1 && types.get(0).equals(new MediaType("text", "*")));
        types = JAXRSUtils.parseMediaTypes("text/*,text/plain;q=.2,text/xml,TEXT/BAR");
        assertTrue(types.size() == 4
                   && "text/*".equals(types.get(0).toString())
                   && "text/plain;q=.2".equals(types.get(1).toString())
                   && "text/xml".equals(types.get(2).toString())
                   && "text/bar".equals(types.get(3).toString()));
        
    }
    
    @Test
    public void testSortMediaTypes() throws Exception {
        List<MediaType> types = 
            JAXRSUtils.sortMediaTypes("text/*,text/plain;q=.2,text/xml,TEXT/BAR");
        assertTrue(types.size() == 4
                   && "text/xml".equals(types.get(0).toString())
                   && "text/bar".equals(types.get(1).toString())
                   && "text/plain;q=.2".equals(types.get(2).toString())
                   && "text/*".equals(types.get(3).toString()));
    }
    
    @Test
    public void testCompareMediaTypes() throws Exception {
        MediaType m1 = MediaType.valueOf("text/xml");
        MediaType m2 = MediaType.valueOf("text/*");
        assertTrue("text/xml is more specific than text/*", 
                   JAXRSUtils.compareMediaTypes(m1, m2) < 0);
        assertTrue("text/* is less specific than text/*", 
                   JAXRSUtils.compareMediaTypes(m2, m1) > 0);
        assertTrue("text/xml should be equal to itself", 
                   JAXRSUtils.compareMediaTypes(m1, new MediaType("text", "xml")) == 0);
        assertTrue("text/* should be equal to itself", 
                   JAXRSUtils.compareMediaTypes(m2, new MediaType("text", "*")) == 0);
        
        assertTrue("text/plain and text/xml are just two specific media types", 
                   JAXRSUtils.compareMediaTypes(MediaType.valueOf("text/plain"), m1) == 0);
        assertTrue("text/xml and text/plain are just two specific media types", 
                   JAXRSUtils.compareMediaTypes(m1, MediaType.valueOf("text/plain")) == 0);
        assertTrue("*/* is less specific than text/xml", 
                   JAXRSUtils.compareMediaTypes(JAXRSUtils.ALL_TYPES, m1) > 0);
        assertTrue("*/* is less specific than text/xml", 
                   JAXRSUtils.compareMediaTypes(m1, JAXRSUtils.ALL_TYPES) < 0);
        assertTrue("*/* is less specific than text/*", 
                   JAXRSUtils.compareMediaTypes(JAXRSUtils.ALL_TYPES, m2) > 0);
        assertTrue("*/* is less specific than text/*", 
                   JAXRSUtils.compareMediaTypes(m2, JAXRSUtils.ALL_TYPES) < 0);
        
        MediaType m3 = MediaType.valueOf("text/xml;q=0.2");
        assertTrue("text/xml should be more preferred than text/xml;q=0.2", 
                   JAXRSUtils.compareMediaTypes(m1, m3) < 0);
        MediaType m4 = MediaType.valueOf("text/xml;q=.3");
        assertTrue("text/xml;q=.3 should be more preferred than text/xml;q=0.2", 
                   JAXRSUtils.compareMediaTypes(m4, m3) < 0);
        
        assertTrue("text/xml;q=.3 should be more preferred than than text/xml;q=0.2", 
                  JAXRSUtils.compareMediaTypes(m3, m4) > 0);
    }
    
    @Test
    public void testCompareSortedMediaTypes() throws Exception {
        MediaType m1 = MediaType.valueOf("text/xml");
        MediaType m2 = MediaType.valueOf("text/*");
        assertTrue("text/xml is more specific than text/*", 
                   JAXRSUtils.compareSortedMediaTypes(Collections.singletonList(m1), 
                                                      Collections.singletonList(m2)) < 0);
        assertTrue("text/* is less specific than text/xml", 
                   JAXRSUtils.compareSortedMediaTypes(Collections.singletonList(m2), 
                                                      Collections.singletonList(m1)) > 0);
        
        assertTrue("text/xml is the same as text/xml", 
                   JAXRSUtils.compareSortedMediaTypes(Collections.singletonList(m1), 
                                                      Collections.singletonList(m1)) == 0);
        
        List<MediaType> sortedList1 = new ArrayList<MediaType>();
        sortedList1.add(m1);
        sortedList1.add(m2);
                
        List<MediaType> sortedList2 = new ArrayList<MediaType>();
        sortedList2.add(m1);
        sortedList2.add(m2);
        
        assertTrue("lists should be equal", 
                   JAXRSUtils.compareSortedMediaTypes(sortedList1, sortedList2) == 0);
        
        sortedList1.add(MediaType.WILDCARD_TYPE);
        assertTrue("first list should be less specific", 
                   JAXRSUtils.compareSortedMediaTypes(sortedList1, sortedList2) > 0);
        sortedList1.add(MediaType.WILDCARD_TYPE);
        assertTrue("second list should be more specific", 
                   JAXRSUtils.compareSortedMediaTypes(sortedList2, sortedList1) < 0);
    }
    
    @Test
    public void testAcceptTypesMatch() throws Exception {
        
        Method m = Customer.class.getMethod("test", new Class[]{});
        ClassResourceInfo cr = new ClassResourceInfo(Customer.class);
        
        assertTrue("text/xml can not be matched",
                   JAXRSUtils.matchMimeTypes(JAXRSUtils.ALL_TYPES, 
                                             new MediaType("text", "xml"), 
                                             new OperationResourceInfo(m, cr)));
        assertTrue("text/xml can not be matched",
                   JAXRSUtils.matchMimeTypes(JAXRSUtils.ALL_TYPES, 
                                             new MediaType("text", "*"), 
                                             new OperationResourceInfo(m, cr)));
        assertTrue("text/xml can not be matched",
                   JAXRSUtils.matchMimeTypes(JAXRSUtils.ALL_TYPES, 
                                             new MediaType("*", "*"), 
                                             new OperationResourceInfo(m, cr)));
        assertFalse("text/plain was matched",
                   JAXRSUtils.matchMimeTypes(JAXRSUtils.ALL_TYPES, 
                                             new MediaType("text", "plain"), 
                                             new OperationResourceInfo(m, cr)));
    }
 
    
    @Test
    public void testQueryParameters() throws Exception {
        Class[] argType = {String.class, Integer.TYPE};
        Method m = Customer.class.getMethod("testQuery", argType);
        MessageImpl messageImpl = new MessageImpl();
        
        messageImpl.put(Message.QUERY_STRING, "query=24");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m, null),
                                                           null, 
                                                           messageImpl);
        assertEquals("Query Parameter was not matched correctly", "24", params.get(0));
        assertEquals("Primitive Query Parameter was not matched correctly", 24, params.get(1));
        
        
    }
    
    @Test
    public void testCookieParameters() throws Exception {
        Class[] argType = {String.class, String.class};
        Method m = Customer.class.getMethod("testCookieParam", argType);
        MessageImpl messageImpl = new MessageImpl();
        MultivaluedMap<String, String> headers = new MetadataMap<String, String>();
        headers.add("Cookie", "c1=c1Value");
        messageImpl.put(Message.PROTOCOL_HEADERS, headers);
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m, null),
                                                           null, 
                                                           messageImpl);
        assertEquals(params.size(), 2);
        assertEquals("c1Value", params.get(0));
        assertEquals("c2Value", params.get(1));
        
        
    }
    
    @Test
    public void testFromStringParameters() throws Exception {
        Class[] argType = {UUID.class, CustomerGender.class, CustomerGender.class};
        Method m = Customer.class.getMethod("testFromStringParam", argType);
        UUID u = UUID.randomUUID();
        Message messageImpl = createMessage();
        messageImpl.put(Message.QUERY_STRING, "p1=" + u.toString() + "&p2=1&p3=2");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m, null),
                                                           null, 
                                                           messageImpl);
        assertEquals(3, params.size());
        assertEquals("Query UUID Parameter was not matched correctly", 
                     u.toString(), params.get(0).toString());
        assertSame(CustomerGender.FEMALE, params.get(1));
        assertSame(CustomerGender.MALE, params.get(2));
    }
    
    @Test
    public void testCustomerParameter() throws Exception {
        Message messageImpl = createMessage();
        ProviderFactory.getInstance(messageImpl).registerUserProvider(
            new CustomerParameterHandler());
        Class[] argType = {Customer.class, Customer[].class};
        Method m = Customer.class.getMethod("testCustomerParam", argType);
        
        messageImpl.put(Message.QUERY_STRING, "p1=Fred&p2=Barry");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m, null),
                                                           null, 
                                                           messageImpl);
        assertEquals(2, params.size());
        Customer c = (Customer)params.get(0);
        assertEquals("Fred", c.getName());
        Customer c2 = ((Customer[])params.get(1))[0];
        assertEquals("Barry", c2.getName());
    }
    
    @Test
    public void testArrayParamNoProvider() throws Exception {
        Message messageImpl = createMessage();
        Class[] argType = {String[].class};
        Method m = Customer.class.getMethod("testCustomerParam2", argType);
        
        messageImpl.put(Message.QUERY_STRING, "p1=Fred&p1=Barry");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m, null),
                                                           null, 
                                                           messageImpl);
        assertEquals(1, params.size());
        String[] values = (String[])params.get(0);
        assertEquals("Fred", values[0]);
        assertEquals("Barry", values[1]);
    }
    
    @Test
    public void testWrongType() throws Exception {
        Class[] argType = {HashMap.class};
        Method m = Customer.class.getMethod("testWrongType", argType);
        Message messageImpl = createMessage();
        messageImpl.put(Message.QUERY_STRING, "p1=1");
        try {
            JAXRSUtils.processParameters(new OperationResourceInfo(m, null),
                                                           null, 
                                                           messageImpl);
            fail("HashMap can not be handled as parameter");
        } catch (WebApplicationException ex) {
            assertEquals(500, ex.getResponse().getStatus());
            assertEquals("Parameter Class java.util.HashMap has no constructor with "
                         + "single String parameter, static valueOf(String) or fromString(String) methods", 
                         ex.getResponse().getEntity().toString());
        }
        
    }
    
    @Test
    public void testExceptionDuringConstruction() throws Exception {
        Class[] argType = {CustomerGender.class};
        Method m = Customer.class.getMethod("testWrongType2", argType);
        MessageImpl messageImpl = new MessageImpl();
        messageImpl.put(Message.QUERY_STRING, "p1=3");
        try {
            JAXRSUtils.processParameters(new OperationResourceInfo(m, null),
                                                           null, 
                                                           messageImpl);
            fail("CustomerGender have no instance with name 3");
        } catch (WebApplicationException ex) {
            assertEquals(404, ex.getResponse().getStatus());
        }
        
    }
    
    
    @Test
    public void testQueryParametersBean() throws Exception {
        Class[] argType = {Customer.CustomerBean.class};
        Method m = Customer.class.getMethod("testQueryBean", argType);
        MessageImpl messageImpl = new MessageImpl();
        messageImpl.put(Message.QUERY_STRING, "a=aValue&b=123");

        MessageImpl complexMessageImpl = new MessageImpl();
        complexMessageImpl.put(Message.QUERY_STRING, "c=1&a=A&b=123&c=2&c=3&"
                                + "d.c=4&d.a=B&d.b=456&d.c=5&d.c=6&"
                                + "e.c=41&e.a=B1&e.b=457&e.c=51&e.c=61&"
                                + "e.c=42&e.a=B2&e.b=458&e.c=52&e.c=62&"
                                + "d.d.c=7&d.d.a=C&d.d.b=789&d.d.c=8&d.d.c=9&"
                                + "d.e.c=71&d.e.a=C1&d.e.b=790&d.e.c=81&d.e.c=91&"
                                + "d.e.c=72&d.e.a=C2&d.e.b=791&d.e.c=82&d.e.c=92");

        verifyParametersBean(m, null, messageImpl, null, complexMessageImpl);
    }
    
    @Test
    public void testPathParametersBean() throws Exception {
        Class[] argType = {Customer.CustomerBean.class};
        Method m = Customer.class.getMethod("testPathBean", argType);
        
        MultivaluedMap<String, String> pathTemplates = new MetadataMap<String, String>();
        pathTemplates.add("a", "aValue");
        pathTemplates.add("b", "123");

        MultivaluedMap<String, String> complexPathTemplates = new MetadataMap<String, String>();
        complexPathTemplates.add("c", "1");
        complexPathTemplates.add("a", "A");
        complexPathTemplates.add("b", "123");
        complexPathTemplates.add("c", "2");
        complexPathTemplates.add("c", "3");

        complexPathTemplates.add("d.c", "4");
        complexPathTemplates.add("d.a", "B");
        complexPathTemplates.add("d.b", "456");
        complexPathTemplates.add("d.c", "5");
        complexPathTemplates.add("d.c", "6");

        complexPathTemplates.add("e.c", "41");
        complexPathTemplates.add("e.a", "B1");
        complexPathTemplates.add("e.b", "457");
        complexPathTemplates.add("e.c", "51");
        complexPathTemplates.add("e.c", "61");

        complexPathTemplates.add("e.c", "42");
        complexPathTemplates.add("e.a", "B2");
        complexPathTemplates.add("e.b", "458");
        complexPathTemplates.add("e.c", "52");
        complexPathTemplates.add("e.c", "62");

        complexPathTemplates.add("d.d.c", "7");
        complexPathTemplates.add("d.d.a", "C");
        complexPathTemplates.add("d.d.b", "789");
        complexPathTemplates.add("d.d.c", "8");
        complexPathTemplates.add("d.d.c", "9");

        complexPathTemplates.add("d.e.c", "71");
        complexPathTemplates.add("d.e.a", "C1");
        complexPathTemplates.add("d.e.b", "790");
        complexPathTemplates.add("d.e.c", "81");
        complexPathTemplates.add("d.e.c", "91");

        complexPathTemplates.add("d.e.c", "72");
        complexPathTemplates.add("d.e.a", "C2");
        complexPathTemplates.add("d.e.b", "791");
        complexPathTemplates.add("d.e.c", "82");
        complexPathTemplates.add("d.e.c", "92");

        verifyParametersBean(m, pathTemplates, new MessageImpl(), complexPathTemplates, new MessageImpl());
    }
    
    @Test
    public void testMatrixParametersBean() throws Exception {
        Class[] argType = {Customer.CustomerBean.class};
        Method m = Customer.class.getMethod("testMatrixBean", argType);
        MessageImpl messageImpl = new MessageImpl();
        messageImpl.put(Message.REQUEST_URI, "/bar;a=aValue/baz;b=123");

        MessageImpl complexMessageImpl = new MessageImpl();
        complexMessageImpl.put(Message.REQUEST_URI, "/bar;c=1/bar;a=A/bar;b=123/bar;c=2/bar;c=3"
                                + "/bar;d.c=4/bar;d.a=B/bar;d.b=456/bar;d.c=5/bar;d.c=6"
                                + "/bar;e.c=41/bar;e.a=B1/bar;e.b=457/bar;e.c=51/bar;e.c=61"
                                + "/bar;e.c=42/bar;e.a=B2/bar;e.b=458/bar;e.c=52/bar;e.c=62"
                                + "/bar;d.d.c=7/bar;d.d.a=C/bar;d.d.b=789/bar;d.d.c=8/bar;d.d.c=9"
                                + "/bar;d.e.c=71/bar;d.e.a=C1/bar;d.e.b=790/bar;d.e.c=81/bar;d.e.c=91"
                                + "/bar;d.e.c=72/bar;d.e.a=C2/bar;d.e.b=791/bar;d.e.c=82/bar;d.e.c=92");

        verifyParametersBean(m, null, messageImpl, null, complexMessageImpl);
    }
    
    @Test
    public void testFormParametersBean() throws Exception {
        Class[] argType = {Customer.CustomerBean.class};
        Method m = Customer.class.getMethod("testFormBean", argType);
        MessageImpl messageImpl = new MessageImpl();
        messageImpl.put(Message.REQUEST_URI, "/bar");
        String body = "a=aValue&b=123";
        messageImpl.setContent(InputStream.class, new ByteArrayInputStream(body.getBytes()));

        MessageImpl complexMessageImpl = new MessageImpl();
        complexMessageImpl.put(Message.REQUEST_URI, "/bar");
        body = "c=1&a=A&b=123&c=2&c=3&"
                                + "d.c=4&d.a=B&d.b=456&d.c=5&d.c=6&"
                                + "e.c=41&e.a=B1&e.b=457&e.c=51&e.c=61&"
                                + "e.c=42&e.a=B2&e.b=458&e.c=52&e.c=62&"
                                + "d.d.c=7&d.d.a=C&d.d.b=789&d.d.c=8&d.d.c=9&"
                                + "d.e.c=71&d.e.a=C1&d.e.b=790&d.e.c=81&d.e.c=91&"
                                + "d.e.c=72&d.e.a=C2&d.e.b=791&d.e.c=82&d.e.c=92";
        complexMessageImpl.setContent(InputStream.class, new ByteArrayInputStream(body.getBytes()));

        verifyParametersBean(m, null, messageImpl, null, complexMessageImpl);
    }

    private void verifyParametersBean(Method m,
                                      MultivaluedMap<String, String> simpleValues,
                                      MessageImpl simpleMessageImpl,
                                      MultivaluedMap<String, String> complexValues,
                                      MessageImpl complexMessageImpl) throws Exception {
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m, null),
                                                           simpleValues, 
                                                           simpleMessageImpl);
        assertEquals("Bean should be created", 1, params.size());
        Customer.CustomerBean cb = (Customer.CustomerBean)params.get(0);
        assertNotNull(cb);
        
        assertEquals("aValue", cb.getA());
        assertEquals(new Long(123), cb.getB());

        params = JAXRSUtils.processParameters(new OperationResourceInfo(m, null),
                                                       complexValues, 
                                                       complexMessageImpl);
        assertEquals("Bean should be created", 1, params.size());
        Customer.CustomerBean cb1 = (Customer.CustomerBean)params.get(0);
        assertNotNull(cb1);

        assertEquals("A", cb1.getA());
        assertEquals(new Long(123), cb1.getB());
        List<String> list1 = (List<String>)cb1.getC();
        assertEquals(3, list1.size());
        assertEquals("1", list1.get(0));
        assertEquals("2", list1.get(1));
        assertEquals("3", list1.get(2));

        Customer.CustomerBean cb2 = cb1.getD();
        assertNotNull(cb2);

        assertEquals("B", cb2.getA());
        assertEquals(new Long(456), cb2.getB());
        List<String> list2 = (List<String>)cb2.getC();
        assertEquals(3, list2.size());
        assertEquals("4", list2.get(0));
        assertEquals("5", list2.get(1));
        assertEquals("6", list2.get(2));

        List<Customer.CustomerBean> cb2List = cb1.e;
        assertEquals(2, cb2List.size());

        int idx = 1;
        for (Customer.CustomerBean cb2E : cb2List) {
            assertNotNull(cb2E);

            assertEquals("B" + idx, cb2E.getA());
            assertEquals(new Long(456 + idx), cb2E.getB());
            // ensure C was stripped properly since lists within lists are not supported
            assertNull(cb2E.getC());
            assertNull(cb2E.getD());
            assertNull(cb2E.e);

            idx++;
        }

        Customer.CustomerBean cb3 = cb2.getD();
        assertNotNull(cb3);

        assertEquals("C", cb3.getA());
        assertEquals(new Long(789), cb3.getB());
        List<String> list3 = (List<String>)cb3.getC();
        assertEquals(3, list3.size());
        assertEquals("7", list3.get(0));
        assertEquals("8", list3.get(1));
        assertEquals("9", list3.get(2));

        List<Customer.CustomerBean> cb3List = cb2.e;
        assertEquals(2, cb3List.size());

        idx = 1;
        for (Customer.CustomerBean cb3E : cb3List) {
            assertNotNull(cb3E);

            assertEquals("C" + idx, cb3E.getA());
            assertEquals(new Long(789 + idx), cb3E.getB());
            // ensure C was stripped properly since lists within lists are not supported
            assertNull(cb3E.getC());
            assertNull(cb3E.getD());
            assertNull(cb3E.e);

            idx++;
        }
    }
    
    @Test
    public void testMultipleQueryParameters() throws Exception {
        Class[] argType = {String.class, String.class, Long.class, 
                           Boolean.TYPE, String.class};
        Method m = Customer.class.getMethod("testMultipleQuery", argType);
        MessageImpl messageImpl = new MessageImpl();
        
        messageImpl.put(Message.QUERY_STRING, 
                        "query=first&query2=second&query3=3&query4=true&query5");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m, null), 
                                                           null, messageImpl);
        assertEquals("First Query Parameter of multiple was not matched correctly", "first", 
                     params.get(0));
        assertEquals("Second Query Parameter of multiple was not matched correctly", 
                     "second", params.get(1));
        assertEquals("Third Query Parameter of multiple was not matched correctly", 
                     new Long(3), params.get(2));
        assertEquals("Fourth Query Parameter of multiple was not matched correctly", 
                     Boolean.TRUE, params.get(3));
        assertEquals("Fourth Query Parameter of multiple was not matched correctly", 
                     "", params.get(4));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testMatrixParameters() throws Exception {
        Class[] argType = {String.class, String.class, String.class, String.class, List.class};
        Method m = Customer.class.getMethod("testMatrixParam", argType);
        MessageImpl messageImpl = new MessageImpl();
        
        messageImpl.put(Message.REQUEST_URI, "/foo;p4=0;p3=3/bar;p1=1;p2/baz;p4=4;p4=5");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m, null), 
                                                           null, messageImpl);
        assertEquals("5 Matrix params should've been identified", 5, params.size());
        
        assertEquals("First Matrix Parameter not matched correctly", 
                     "1", params.get(0));
        assertEquals("Second Matrix Parameter was not matched correctly", 
                     "", params.get(1));
        assertEquals("Third Matrix Parameter was not matched correctly", 
                     "3", params.get(2));
        assertEquals("Fourth Matrix Parameter was not matched correctly", 
                     "0", params.get(3));
        List<String> list = (List<String>)params.get(4);
        assertEquals(3, list.size());
        assertEquals("0", list.get(0));
        assertEquals("4", list.get(1));
        assertEquals("5", list.get(2));
    }
    
    @Test
    public void testMatrixAndPathSegmentParameters() throws Exception {
        Class[] argType = {PathSegment.class, String.class};
        Method m = Customer.class.getMethod("testPathSegment", argType);
        MessageImpl messageImpl = new MessageImpl();
        messageImpl.put(Message.REQUEST_URI, "/bar%20foo;p4=0%201");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();
        values.add("ps", "bar%20foo;p4=0%201");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m, null), 
                                                           values, 
                                                           messageImpl);
        assertEquals("2 params should've been identified", 2, params.size());
        
        PathSegment ps = (PathSegment)params.get(0);
        assertEquals("bar foo", ps.getPath());
        assertEquals(1, ps.getMatrixParameters().size());
        assertEquals("0 1", ps.getMatrixParameters().getFirst("p4"));
        assertEquals("bar foo", params.get(1));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testFormParameters() throws Exception {
        Class[] argType = {String.class, List.class};
        Method m = Customer.class.getMethod("testFormParam", argType);
        MessageImpl messageImpl = new MessageImpl();
        String body = "p1=1&p2=2&p2=3";
        messageImpl.put(Message.REQUEST_URI, "/foo");
        messageImpl.setContent(InputStream.class, new ByteArrayInputStream(body.getBytes()));
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m, null), 
                                                           null, messageImpl);
        assertEquals("2 form params should've been identified", 2, params.size());
        
        assertEquals("First Form Parameter not matched correctly", 
                     "1", params.get(0));
        List<String> list = (List<String>)params.get(1);
        assertEquals(2, list.size());
        assertEquals("2", list.get(0));
        assertEquals("3", list.get(1));
    }
    
    @Test
    public void testSelectResourceMethod() throws Exception {
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class);
        OperationResourceInfo ori1 = new OperationResourceInfo(
                                         Customer.class.getMethod("getItAsXML", new Class[]{}), 
                                         cri);
        ori1.setHttpMethod("GET");
        ori1.setURITemplate(new URITemplate("/"));
        OperationResourceInfo ori2 = new OperationResourceInfo(
                                         Customer.class.getMethod("getItPlain", new Class[]{}), 
                                         cri);
        ori2.setHttpMethod("GET");
        ori2.setURITemplate(new URITemplate("/"));
        MethodDispatcher md = new MethodDispatcher(); 
        md.bind(ori1, Customer.class.getMethod("getItAsXML", new Class[]{}));
        md.bind(ori2, Customer.class.getMethod("getItPlain", new Class[]{}));
        cri.setMethodDispatcher(md);
        
        OperationResourceInfo ori = JAXRSUtils.findTargetMethod(cri, "/", "GET", 
              new MetadataMap<String, String>(), "*/*", getTypes("text/plain"));
        
        assertSame(ori, ori2);
        
        ori = JAXRSUtils.findTargetMethod(cri, "/", "GET", new MetadataMap<String, String>(), 
                                              "*/*", getTypes("text/xml"));
                         
        assertSame(ori, ori1);
        
        ori = JAXRSUtils.findTargetMethod(cri, "/", "GET", new MetadataMap<String, String>(), 
                                          "*/*", 
                                          JAXRSUtils.sortMediaTypes(getTypes("*,text/plain,text/xml")));
                     
        assertSame(ori, ori2);
        ori = JAXRSUtils.findTargetMethod(cri, "/", "GET", new MetadataMap<String, String>(), 
                                          "*/*", 
                                          JAXRSUtils.sortMediaTypes(getTypes("*,text/plain, text/xml,x/y")));
                     
        assertSame(ori, ori2);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testHttpContextParameters() throws Exception {
        
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        OperationResourceInfo ori = 
            new OperationResourceInfo(
                Customer.class.getMethod("testParams", 
                                         new Class[]{UriInfo.class, 
                                                     HttpHeaders.class, 
                                                     Request.class,
                                                     SecurityContext.class,
                                                     Providers.class,
                                                     String.class,
                                                     List.class}), 
                cri);
        ori.setHttpMethod("GET");
        MultivaluedMap<String, String> headers = new MetadataMap<String, String>();
        headers.add("Foo", "bar, baz");
        
        Message m = new MessageImpl();
        m.put(Message.PROTOCOL_HEADERS, headers);
        
        List<Object> params = 
            JAXRSUtils.processParameters(ori, new MetadataMap<String, String>(), m);
        assertEquals("7 parameters expected", 7, params.size());
        assertSame(UriInfoImpl.class, params.get(0).getClass());
        assertSame(HttpHeadersImpl.class, params.get(1).getClass());
        assertSame(RequestImpl.class, params.get(2).getClass());
        assertSame(SecurityContextImpl.class, params.get(3).getClass());
        assertSame(ProvidersImpl.class, params.get(4).getClass());
        assertSame(String.class, params.get(5).getClass());
        assertEquals("Wrong header param", "bar", params.get(5));
        List<String> values = (List<String>)params.get(6);
        assertEquals("Wrong headers size", 2, values.size());
        assertEquals("Wrong 1st header param", "bar", values.get(0));
        assertEquals("Wrong 2nd header param", "baz", values.get(1));
    }
    
    @Test
    public void testHttpContextParametersFromInterface() throws Exception {
        
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        Method methodToInvoke = 
            Customer.class.getMethod("setUriInfoContext", 
                                     new Class[]{UriInfo.class});
        OperationResourceInfo ori = 
            new OperationResourceInfo(methodToInvoke,
                                      AnnotationUtils.getAnnotatedMethod(methodToInvoke), cri);
        ori.setHttpMethod("GET");
        
        Message m = new MessageImpl();
        
        List<Object> params = 
            JAXRSUtils.processParameters(ori, new MetadataMap<String, String>(), m);
        assertEquals("1 parameters expected", 1, params.size());
        assertSame(UriInfoImpl.class, params.get(0).getClass());
    }
    
    @Test
    public void testServletContextParameters() throws Exception {
        
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        OperationResourceInfo ori = 
            new OperationResourceInfo(
                Customer.class.getMethod("testServletParams", 
                                         new Class[]{HttpServletRequest.class, 
                                                     HttpServletResponse.class, 
                                                     ServletContext.class,
                                                     ServletConfig.class}), 
                cri);
        ori.setHttpMethod("GET");
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        HttpServletResponse response = new HttpServletResponseFilter(
                                           EasyMock.createMock(HttpServletResponse.class), null);
        ServletContext context = EasyMock.createMock(ServletContext.class);
        ServletConfig config = EasyMock.createMock(ServletConfig.class);        
        
        EasyMock.replay(request);
        EasyMock.replay(context);
        EasyMock.replay(config);
        
        Message m = new MessageImpl();
        m.put(AbstractHTTPDestination.HTTP_REQUEST, request);
        m.put(AbstractHTTPDestination.HTTP_RESPONSE, response);
        m.put(AbstractHTTPDestination.HTTP_CONTEXT, context);
        m.put(AbstractHTTPDestination.HTTP_CONFIG, config);
        
        List<Object> params = 
            JAXRSUtils.processParameters(ori, new MetadataMap<String, String>(), m);
        assertEquals("4 parameters expected", 4, params.size());
        assertSame(request.getClass(), params.get(0).getClass());
        assertSame(response.getClass(), params.get(1).getClass());
        assertSame(context.getClass(), params.get(2).getClass());
        assertSame(config.getClass(), params.get(3).getClass());
        
    }
    
    @Test
    public void testPerRequestHttpContextFields() throws Exception {
        
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        cri.setResourceProvider(new PerRequestResourceProvider(Customer.class));
        OperationResourceInfo ori = new OperationResourceInfo(null, cri);
        
        Customer c = new Customer();
        
        Message m = createMessage();
        m.put(Message.PROTOCOL_HEADERS, new HashMap<String, List<String>>());
        HttpServletResponse response = EasyMock.createMock(HttpServletResponse.class);
        m.put(AbstractHTTPDestination.HTTP_RESPONSE, response);
        
        InjectionUtils.injectContextFields(c, ori.getClassResourceInfo(), m);
        assertSame(UriInfoImpl.class, c.getUriInfo2().getClass());
        assertSame(HttpHeadersImpl.class, c.getHeaders().getClass());
        assertSame(RequestImpl.class, c.getRequest().getClass());
        assertSame(SecurityContextImpl.class, c.getSecurityContext().getClass());
        assertSame(ProvidersImpl.class, c.getBodyWorkers().getClass());
        
    }
    
    @Test
    public void testSingletonHttpContextFields() throws Exception {
        
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        Customer c = new Customer();
        cri.setResourceProvider(new SingletonResourceProvider(c));
                
        Message m = createMessage();
        m.put(Message.PROTOCOL_HEADERS, new HashMap<String, List<String>>());
        ServletContext servletContextMock = EasyMock.createNiceMock(ServletContext.class);
        m.put(AbstractHTTPDestination.HTTP_CONTEXT, servletContextMock);
        HttpServletRequest httpRequest = EasyMock.createNiceMock(HttpServletRequest.class);
        m.put(AbstractHTTPDestination.HTTP_REQUEST, httpRequest);
        HttpServletResponse httpResponse = EasyMock.createMock(HttpServletResponse.class);
        m.put(AbstractHTTPDestination.HTTP_RESPONSE, httpResponse);
        
        InjectionUtils.injectContextProxies(cri, cri.getResourceProvider().getInstance(null));
        InjectionUtils.injectContextFields(c, cri, m);
        InjectionUtils.injectContextMethods(c, cri, m);
        assertSame(ThreadLocalUriInfo.class, c.getUriInfo2().getClass());
        assertSame(UriInfoImpl.class, 
                   ((ThreadLocalProxy)c.getUriInfo2()).get().getClass());
        assertSame(HttpHeadersImpl.class, 
                   ((ThreadLocalProxy)c.getHeaders()).get().getClass());
        assertSame(RequestImpl.class, 
                   ((ThreadLocalProxy)c.getRequest()).get().getClass());
        assertSame(SecurityContextImpl.class, 
                   ((ThreadLocalProxy)c.getSecurityContext()).get().getClass());
        assertSame(ProvidersImpl.class, 
                   ((ThreadLocalProxy)c.getBodyWorkers()).get().getClass());
        assertSame(ProvidersImpl.class, 
                   ((ThreadLocalProxy)c.getBodyWorkers()).get().getClass());
  
        assertSame(servletContextMock, 
                   ((ThreadLocalProxy)c.getThreadLocalServletContext()).get());
        assertSame(servletContextMock, 
                   ((ThreadLocalProxy)c.getServletContext()).get());
        assertSame(servletContextMock, 
                   ((ThreadLocalProxy)c.getSuperServletContext()).get());
        assertSame(httpRequest, 
                   ((ThreadLocalProxy)c.getServletRequest()).get());
        HttpServletResponseFilter filter = (
            HttpServletResponseFilter)((ThreadLocalProxy)c.getServletResponse()).get();
        assertSame(httpResponse, filter.getResponse());
    }
    
    @Test
    public void testSingletonHttpResourceFields() throws Exception {
        
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        Customer c = new Customer();
        cri.setResourceProvider(new SingletonResourceProvider(c));
                
        Message m = new MessageImpl();
        ServletContext servletContextMock = EasyMock.createNiceMock(ServletContext.class);
        m.put(AbstractHTTPDestination.HTTP_CONTEXT, servletContextMock);
        HttpServletRequest httpRequest = EasyMock.createNiceMock(HttpServletRequest.class);
        m.put(AbstractHTTPDestination.HTTP_REQUEST, httpRequest);
        HttpServletResponse httpResponse = EasyMock.createMock(HttpServletResponse.class);
        m.put(AbstractHTTPDestination.HTTP_RESPONSE, httpResponse);
        InjectionUtils.injectContextProxies(cri, cri.getResourceProvider().getInstance(null));
        InjectionUtils.injectResourceFields(c, cri, m);
        assertSame(servletContextMock, 
                   ((ThreadLocalProxy)c.getServletContextResource()).get());
        assertSame(httpRequest, 
                   ((ThreadLocalProxy)c.getServletRequestResource()).get());
        HttpServletResponseFilter filter = (
            HttpServletResponseFilter)((ThreadLocalProxy)c.getServletResponseResource()).get();
        assertSame(httpResponse, filter.getResponse());
    }
    
    @Test
    public void testContextAnnotationOnMethod() throws Exception {
        
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        Customer c = new Customer();
        cri.setResourceProvider(new SingletonResourceProvider(c));
        InjectionUtils.injectContextProxies(cri, cri.getResourceProvider().getInstance(null));
        
        OperationResourceInfo ori = new OperationResourceInfo(Customer.class.getMethods()[0],
                                                              cri); 
        
        
        JAXRSUtils.handleSetters(ori, c, new MessageImpl());
        assertNotNull(c.getUriInfo());
        assertSame(ThreadLocalUriInfo.class, c.getUriInfo().getClass());
        assertSame(UriInfoImpl.class, 
                   ((ThreadLocalProxy)c.getUriInfo()).get().getClass());
    }
    
    @Test
    public void testParamAnnotationOnMethod() throws Exception {

        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        Customer c = new Customer();
        OperationResourceInfo ori = new OperationResourceInfo(Customer.class.getMethods()[0],
                                                              cri);
        Message m = new MessageImpl();
        m.put(Message.QUERY_STRING, "a=aValue&query2=b");
        JAXRSUtils.handleSetters(ori, c, m);
        assertEquals("aValue", c.getQueryParam());
        
    }
    
    @Test
    public void testParamAnnotationOnField() throws Exception {

        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        Customer c = new Customer();
        OperationResourceInfo ori = new OperationResourceInfo(Customer.class.getMethods()[0],
                                                              cri);
        Message m = new MessageImpl();
        m.put(Message.QUERY_STRING, "b=bValue");
        JAXRSUtils.handleSetters(ori, c, m);
        assertEquals("bValue", c.getB());
        
    }

    @Test
    public void testContextResolverParam() throws Exception {
        
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        OperationResourceInfo ori = 
            new OperationResourceInfo(
                Customer.class.getMethod("testContextResolvers", 
                                         new Class[]{ContextResolver.class}), 
                                         cri);
        ori.setHttpMethod("GET");
        
        Message m = createMessage();
        ContextResolver<JAXBContext> cr = new JAXBContextProvider();
        ProviderFactory.getInstance(m).registerUserProvider(cr);
        
        m.put(Message.BASE_PATH, "/");    
        List<Object> params = 
            JAXRSUtils.processParameters(ori, new MetadataMap<String, String>(), m);
        assertEquals("1 parameters expected", 1, params.size());
        assertSame(cr.getClass(), params.get(0).getClass());
    }
    
    @Test
    public void testContextResolverFields() throws Exception {
        
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        cri.setResourceProvider(new PerRequestResourceProvider(Customer.class));
        OperationResourceInfo ori = new OperationResourceInfo(null, cri);
        
        Message m = createMessage();
        HttpServletResponse response = EasyMock.createMock(HttpServletResponse.class);
        m.put(AbstractHTTPDestination.HTTP_RESPONSE, response);
        Customer c = new Customer();
        ContextResolver<JAXBContext> cr = new JAXBContextProvider();
        ProviderFactory.getInstance(m).registerUserProvider(cr);
        
        m.put(Message.BASE_PATH, "/");    
        InjectionUtils.injectContextFields(c, ori.getClassResourceInfo(), m);
        assertSame(cr.getClass(), c.getContextResolver().getClass());
    }
    
    @Test
    public void testServletResourceFields() throws Exception {
        
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        cri.setResourceProvider(new PerRequestResourceProvider(Customer.class));
        OperationResourceInfo ori = new OperationResourceInfo(null, cri);
        
        Customer c = new Customer();
        
        // Creating mocks for the servlet request, response and context
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        HttpServletResponse response = EasyMock.createMock(HttpServletResponse.class);
        ServletContext context = EasyMock.createMock(ServletContext.class);
        EasyMock.replay(request);
        EasyMock.replay(response);
        EasyMock.replay(context);
        
        Message m = createMessage();
        m.put(AbstractHTTPDestination.HTTP_REQUEST, request);
        m.put(AbstractHTTPDestination.HTTP_RESPONSE, response);
        m.put(AbstractHTTPDestination.HTTP_CONTEXT, context);
        
        InjectionUtils.injectResourceFields(c, ori.getClassResourceInfo(), m);
        assertSame(request.getClass(), c.getServletRequestResource().getClass());
        HttpServletResponseFilter filter = (HttpServletResponseFilter)c.getServletResponseResource();
        assertSame(response.getClass(), filter.getResponse().getClass());
        assertSame(context.getClass(), c.getServletContextResource().getClass());
        assertNull(c.getServletRequest());
        assertNull(c.getServletResponse());
        assertNull(c.getServletContext());
        
        c = new Customer();
        InjectionUtils.injectContextFields(c, ori.getClassResourceInfo(), m);
        assertNull(c.getServletRequestResource());
        assertNull(c.getServletResponseResource());
        assertNull(c.getServletContextResource());
        assertSame(request.getClass(), c.getServletRequest().getClass());
        filter = (HttpServletResponseFilter)c.getServletResponse();
        assertSame(response.getClass(), filter.getResponse().getClass());
        assertSame(context.getClass(), c.getServletContext().getClass());
    }
    
    @Test
    public void testConversion() throws Exception {
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        OperationResourceInfo ori = 
            new OperationResourceInfo(
                Customer.class.getMethod("testConversion", 
                                         new Class[]{PathSegmentImpl.class, 
                                                     SimpleFactory.class}), 
                cri);
        ori.setHttpMethod("GET");
        ori.setURITemplate(new URITemplate("{id1}/{id2}"));
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();
        values.putSingle("id1", "1");
        values.putSingle("id2", "2");
        
        Message m = createMessage();
        
        
        List<Object> params = 
            JAXRSUtils.processParameters(ori, values, m);
        PathSegment ps = (PathSegment)params.get(0);
        assertEquals("1", ps.getPath());
        
        SimpleFactory sf = (SimpleFactory)params.get(1);
        assertEquals(2, sf.getId());
    }
    
    private static OperationResourceInfo findTargetResourceClass(List<ClassResourceInfo> resources,
                                                                Message message,
                                                                String path, 
                                                                String httpMethod,
                                                                MultivaluedMap<String, String> values,
                                                                String requestContentType, 
                                                                List<MediaType> acceptContentTypes) {
        
        ClassResourceInfo resource = JAXRSUtils.selectResourceClass(resources, path, values);
        
        if (resource != null) {
            String subResourcePath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
            OperationResourceInfo ori = JAXRSUtils.findTargetMethod(resource, subResourcePath, httpMethod, 
                                                   values, requestContentType, acceptContentTypes);
            if (ori != null) {
                return ori;
            }
        }
        
        return null;
    }
    
    private Message createMessage() {
        ProviderFactory factory = ProviderFactory.getInstance();
        Message m = new MessageImpl();
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        Endpoint endpoint = EasyMock.createMock(Endpoint.class);
        endpoint.get(ProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(factory).anyTimes();
        EasyMock.replay(endpoint);
        e.put(Endpoint.class, endpoint);
        return m;
    }
}
