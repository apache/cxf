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
package org.apache.cxf.jaxrs;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ConsumeMime;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.MethodDispatcher;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.HttpHeadersImpl;
import org.apache.cxf.jaxrs.provider.PathSegmentImpl;
import org.apache.cxf.jaxrs.provider.RequestImpl;
import org.apache.cxf.jaxrs.provider.SecurityContextImpl;
import org.apache.cxf.jaxrs.provider.UriInfoImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSUtilsTest extends Assert {
    
    public class Customer {
        
        @Context private UriInfo uriInfo;
        @Context private HttpHeaders headers;
        @Context private Request request;
        @Context private SecurityContext sContext;
        @Resource private HttpServletRequest servletRequest;
        @Resource private HttpServletResponse servletResponse;
        @Resource private ServletContext servletContext;
        
               
        public UriInfo getUriInfo() {
            return uriInfo;
        }
        
        public HttpHeaders getHeaders() {
            return headers;
        }
        
        public Request getRequest() {
            return request;
        }
        
        public SecurityContext getSecurityContext() {
            return sContext;
        }
        
        public HttpServletRequest getServletRequest() {
            return servletRequest;
        }
        
        public HttpServletResponse getServletResponse() {
            return servletResponse;
        }
        
        public ServletContext getServletContext() {
            return servletContext;
        }

        @ProduceMime("text/xml")
        @ConsumeMime("text/xml")
        public void test() {
            // complete
        }
        
        @ProduceMime("text/xml")   
        public void getItAsXML() {
            // complete
        }
        @ProduceMime("text/plain")   
        public void getItPlain() {
            // complete
        }
        
        @ProduceMime("text/xml")   
        public void testQuery(@QueryParam("query") String queryString, 
                              @QueryParam("query") int queryInt) {
            // complete
        }
        
        @ProduceMime("text/xml")   
        public void testMultipleQuery(@QueryParam("query")  String queryString, 
                                      @QueryParam("query2") String queryString2,
                                      @QueryParam("query3") Long queryString3,
                                      @QueryParam("query4") boolean queryBoolean4,
                                      @QueryParam("query5") String queryString4) {
            // complete
        }
        
        @ProduceMime("text/xml")   
        public void testMatrixParam(@MatrixParam("p1")  String queryString, 
                                    @MatrixParam("p2") String queryString2) {
            // complete
        }
        
        public void testParams(@Context UriInfo info,
                               @Context HttpHeaders hs,
                               @Context Request r,
                               @Context SecurityContext s,
                               @HeaderParam("Foo") String h) {
            // complete
        }
        
        @Path("{id1}/{id2}")
        public void testConversion(@PathParam("id1") PathSegmentImpl id1,
                                   @PathParam("id2") SimpleFactory f) {
            // complete
        }
    };
    
    @Before
    public void setUp() {
    }

    @Test
    public void testFindTargetResourceClass() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStoreNoSubResource.class);
        sf.create();        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();

        String contentTypes = "*/*";
        String acceptContentTypes = "*/*";

        //If acceptContentTypes does not specify a specific Mime type, the  
        //method is declared with a most specific ProduceMime type is selected.
        OperationResourceInfo ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books/123/",
             "GET", new MetadataMap<String, String>(), contentTypes, acceptContentTypes);       
        assertNotNull(ori);
        assertEquals("getBookJSON", ori.getMethod().getName());
        
        //test
        acceptContentTypes = "application/json";
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books/123",
             "GET", new MetadataMap<String, String>(), contentTypes, acceptContentTypes);        
        assertNotNull(ori);
        assertEquals("getBookJSON", ori.getMethod().getName());
        
        //test 
        acceptContentTypes = "application/xml";
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books/123",
              "GET", new MetadataMap<String, String>(), contentTypes, acceptContentTypes);        
        assertNotNull(ori);
        assertEquals("getBook", ori.getMethod().getName());
        
        //test 
        acceptContentTypes = "application/xml";
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books",
                      "GET", new MetadataMap<String, String>(), contentTypes, acceptContentTypes);        
        assertNotNull(ori);
        assertEquals("getBooks", ori.getMethod().getName());
        
        //test find POST
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books",
                 "POST", new MetadataMap<String, String>(), contentTypes, acceptContentTypes);       
        assertNotNull(ori);
        assertEquals("addBook", ori.getMethod().getName());
        
        //test find PUT
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books",
            "PUT", new MetadataMap<String, String>(), contentTypes, acceptContentTypes);  
        assertEquals("updateBook", ori.getMethod().getName());
        
        //test find DELETE
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books/123",
             "DELETE", new MetadataMap<String, String>(), contentTypes, acceptContentTypes);        
        assertNotNull(ori);
        assertEquals("deleteBook", ori.getMethod().getName());     
        
    }
    
    @Test
    public void testFindTargetResourceClassWithTemplates() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStoreTemplates.class);
        sf.create();        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();

        String contentTypes = "*/*";
        String acceptContentTypes = "*/*";

        //If acceptContentTypes does not specify a specific Mime type, the  
        //method is declared with a most specific ProduceMime type is selected.
        MetadataMap<String, String> values = new MetadataMap<String, String>();
        OperationResourceInfo ori = JAXRSUtils.findTargetResourceClass(resources, "/1/2/",
             "GET", values, contentTypes, acceptContentTypes);       
        assertNotNull(ori);
        assertEquals("getBooks", ori.getMethod().getName());
        assertEquals("Only id and final match groups should be there", 2, values.size());
        assertEquals("2 {id} values should've been picked up", 2, values.get("id").size());
        assertEquals("FINAL_MATCH_GROUP should've been picked up", 1, 
                     values.get(URITemplate.FINAL_MATCH_GROUP).size());
        assertEquals("First {id} is 1", "1", values.getFirst("id"));
        assertEquals("Second id is 2", "2", values.get("id").get(1));
        
        values = new MetadataMap<String, String>();
        ori = JAXRSUtils.findTargetResourceClass(resources, "/2",
             "POST", values, contentTypes, acceptContentTypes);       
        assertNotNull(ori);
        assertEquals("updateBookStoreInfo", ori.getMethod().getName());
        assertEquals("Only id and final match groups should be there", 2, values.size());
        assertEquals("Only single {id} should've been picked up", 1, values.get("id").size());
        assertEquals("FINAL_MATCH_GROUP should've been picked up", 1, 
                     values.get(URITemplate.FINAL_MATCH_GROUP).size());
        assertEquals("Only the first {id} should've been picked up", "2", values.getFirst("id"));
        
        values = new MetadataMap<String, String>();
        ori = JAXRSUtils.findTargetResourceClass(resources, "/3/4",
             "PUT", values, contentTypes, acceptContentTypes);       
        assertNotNull(ori);
        assertEquals("updateBook", ori.getMethod().getName());
        assertEquals("Only the first {id} should've been picked up", 3, values.size());
        assertEquals("Only the first {id} should've been picked up", 1, values.get("id").size());
        assertEquals("Only the first {id} should've been picked up", 1, values.get("bookId").size());
        assertEquals("Only the first {id} should've been picked up", 1, 
                     values.get(URITemplate.FINAL_MATCH_GROUP).size());
        assertEquals("Only the first {id} should've been picked up", "3", values.getFirst("id"));
        assertEquals("Only the first {id} should've been picked up", "4", values.getFirst("bookId"));
    }
    
    @Test
    @Ignore
    public void testFindTargetResourceClassWithSubResource() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStore.class);
        sf.create();        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();

        String contentTypes = "*/*";
        String acceptContentTypes = "*/*";

        OperationResourceInfo ori = JAXRSUtils.findTargetResourceClass(resources,
            "/bookstore/books/123/chapter/1", "GET", new MetadataMap<String, String>(), contentTypes,
                                                                       acceptContentTypes);       
        assertNotNull(ori);
        assertEquals("getBook", ori.getMethod().getName());
        
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books",
            "POST", new MetadataMap<String, String>(), contentTypes, acceptContentTypes);      
        assertNotNull(ori);
        assertEquals("addBook", ori.getMethod().getName());
        
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books",
             "PUT", new MetadataMap<String, String>(), contentTypes, acceptContentTypes);        
        assertNotNull(ori);
        assertEquals("updateBook", ori.getMethod().getName());
        
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books/123",
            "DELETE", new MetadataMap<String, String>(), contentTypes, acceptContentTypes);        
        assertNotNull(ori);
        assertEquals("deleteBook", ori.getMethod().getName());
    }

    @Test
    public void testIntersectMimeTypes() throws Exception {
        //test basic
        List<MediaType> methodMimeTypes = new ArrayList<MediaType>(
             JAXRSUtils.parseMediaTypes("application/mytype,application/xml,application/json"));
        
        MediaType acceptContentType = MediaType.parse("application/json");
        List <MediaType> candidateList = JAXRSUtils.intersectMimeTypes(methodMimeTypes, 
                                                 MediaType.parse("application/json"));  

        assertEquals(1, candidateList.size());
        assertTrue(candidateList.get(0).toString().equals("application/json"));
        
        //test basic       
        methodMimeTypes = JAXRSUtils.parseMediaTypes(
            "application/mytype, application/json, application/xml");
        candidateList = JAXRSUtils.intersectMimeTypes(methodMimeTypes, 
                                                      MediaType.parse("application/json"));  

        assertEquals(1, candidateList.size());
        assertTrue(candidateList.get(0).toString().equals("application/json"));
        
        //test accept wild card */*       
        candidateList = JAXRSUtils.intersectMimeTypes(
            "application/mytype,application/json,application/xml", "*/*");  

        assertEquals(3, candidateList.size());
        
        //test accept wild card application/*       
        methodMimeTypes = JAXRSUtils.parseMediaTypes("text/html,text/xml,application/xml");
        acceptContentType = MediaType.parse("text/*");
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
                                                      MediaType.parse("application/json"));

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
                   && "text/bar".equals(types.get(0).toString())
                   && "text/plain;q=.2".equals(types.get(1).toString())
                   && "text/xml".equals(types.get(2).toString())
                   && "text/*".equals(types.get(3).toString()));
    }
    
    @Test
    public void testCompareMediaTypes() throws Exception {
        MediaType m1 = MediaType.parse("text/xml");
        MediaType m2 = MediaType.parse("text/*");
        assertTrue("text/xml is more specific than text/*", 
                   JAXRSUtils.compareMediaTypes(m1, m2) < 0);
        assertTrue("text/* is less specific than text/*", 
                   JAXRSUtils.compareMediaTypes(m2, m1) > 0);
        assertTrue("text/xml should be equal to itself", 
                   JAXRSUtils.compareMediaTypes(m1, new MediaType("text", "xml")) == 0);
        assertTrue("text/* should be equal to itself", 
                   JAXRSUtils.compareMediaTypes(m2, new MediaType("text", "*")) == 0);
        
        assertTrue("text/plain is alphabetically earlier than text/xml", 
                   JAXRSUtils.compareMediaTypes(MediaType.parse("text/plain"), m1) < 0);
        assertTrue("text/xml is alphabetically later than text/plain", 
                   JAXRSUtils.compareMediaTypes(m1, MediaType.parse("text/plain")) > 0);
        assertTrue("*/* is less specific than text/xml", 
                   JAXRSUtils.compareMediaTypes(JAXRSUtils.ALL_TYPES, m1) > 0);
        assertTrue("*/* is less specific than text/xml", 
                   JAXRSUtils.compareMediaTypes(m1, JAXRSUtils.ALL_TYPES) < 0);
        assertTrue("*/* is less specific than text/*", 
                   JAXRSUtils.compareMediaTypes(JAXRSUtils.ALL_TYPES, m2) > 0);
        assertTrue("*/* is less specific than text/*", 
                   JAXRSUtils.compareMediaTypes(m2, JAXRSUtils.ALL_TYPES) < 0);
        
        MediaType m3 = MediaType.parse("text/xml;q=0.2");
        assertTrue("text/xml should be more preferred than than text/xml;q=0.2", 
                   JAXRSUtils.compareMediaTypes(m1, m3) < 0);
        MediaType m4 = MediaType.parse("text/xml;q=.3");
        assertTrue("text/xml;q=.3 should be more preferred than than text/xml;q=0.2", 
                   JAXRSUtils.compareMediaTypes(m4, m3) < 0);
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
    
    @Test
    public void testMatrixParameters() throws Exception {
        Class[] argType = {String.class, String.class};
        Method m = Customer.class.getMethod("testMatrixParam", argType);
        MessageImpl messageImpl = new MessageImpl();
        
        messageImpl.put(Message.PATH_INFO, "/foo/bar;p1=1;p2");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m, null), 
                                                           null, messageImpl);
        assertEquals("2 Matrix params should've been identified", 2, params.size());
        
        assertEquals("First Matrix Parameter of multiple was not matched correctly", 
                     "1", params.get(0));
        assertEquals("Second Matrix Parameter of multiple was not matched correctly", 
                     "", params.get(1));
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
                                              new MetadataMap<String, String>(), "*/*", "text/plain");
        
        assertSame(ori, ori2);
        
        ori = JAXRSUtils.findTargetMethod(cri, "/", "GET", new MetadataMap<String, String>(), 
                                              "*/*", "text/xml");
                         
        assertSame(ori, ori1);
        
        ori = JAXRSUtils.findTargetMethod(cri, "/", "GET", new MetadataMap<String, String>(), 
                                          "*/*", "*,text/plain,text/xml");
                     
        assertSame(ori, ori2);
        ori = JAXRSUtils.findTargetMethod(cri, "/", "GET", new MetadataMap<String, String>(), 
                                          "*/*", "*,x/y,text/xml,text/plain");
                     
        assertSame(ori, ori2);
    }
    
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
                                                     String.class}), 
                cri);
        ori.setHttpMethod("GET");
        MultivaluedMap<String, String> headers = new MetadataMap<String, String>();
        headers.add("Foo", "bar");
        headers.add("Foo", "baz");
        
        Message m = new MessageImpl();
        m.put(Message.PROTOCOL_HEADERS, headers);
        
        List<Object> params = 
            JAXRSUtils.processParameters(ori, new MetadataMap<String, String>(), m);
        assertEquals("5 parameters expected", 5, params.size());
        assertSame(UriInfoImpl.class, params.get(0).getClass());
        assertSame(HttpHeadersImpl.class, params.get(1).getClass());
        assertSame(RequestImpl.class, params.get(2).getClass());
        assertSame(SecurityContextImpl.class, params.get(3).getClass());
        assertSame(String.class, params.get(4).getClass());
        assertEquals("Wrong header param", "bar,baz", params.get(4));
    }
    
    @Test
    public void testHttpContextFields() throws Exception {
        
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        OperationResourceInfo ori = new OperationResourceInfo(null, cri);
        
        Customer c = new Customer();
        
        Message m = new MessageImpl();
        m.put(Message.PROTOCOL_HEADERS, new HashMap<String, List<String>>());
        
        JAXRSUtils.injectHttpContextValues(c, ori, m);
        assertSame(UriInfoImpl.class, c.getUriInfo().getClass());
        assertSame(HttpHeadersImpl.class, c.getHeaders().getClass());
        assertSame(RequestImpl.class, c.getRequest().getClass());
        assertSame(SecurityContextImpl.class, c.getSecurityContext().getClass());
        
    }

    @Test
    public void testServletResourceFields() throws Exception {
        
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class, true);
        OperationResourceInfo ori = new OperationResourceInfo(null, cri);
        
        Customer c = new Customer();
        
        // Creating mocks for the servlet request, response and context
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        HttpServletResponse response = EasyMock.createMock(HttpServletResponse.class);
        ServletContext context = EasyMock.createMock(ServletContext.class);
        EasyMock.replay(request);
        EasyMock.replay(response);
        EasyMock.replay(context);
        
        Message m = new MessageImpl();
        m.put(AbstractHTTPDestination.HTTP_REQUEST, request);
        m.put(AbstractHTTPDestination.HTTP_RESPONSE, response);
        m.put(AbstractHTTPDestination.HTTP_CONTEXT, context);
        
        JAXRSUtils.injectServletResourceValues(c, ori, m);
        assertSame(request.getClass(), c.getServletRequest().getClass());
        assertSame(response.getClass(), c.getServletResponse().getClass());
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
        
        Message m = new MessageImpl();
        
        
        List<Object> params = 
            JAXRSUtils.processParameters(ori, values, m);
        PathSegment ps = (PathSegment)params.get(0);
        assertEquals("1", ps.getPath());
        
        SimpleFactory sf = (SimpleFactory)params.get(1);
        assertEquals(2, sf.getId());
    }
}
