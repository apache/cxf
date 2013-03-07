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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.fortest.BookEntity;
import org.apache.cxf.jaxrs.fortest.BookEntity2;
import org.apache.cxf.jaxrs.fortest.GenericEntityImpl;
import org.apache.cxf.jaxrs.fortest.GenericEntityImpl2;
import org.apache.cxf.jaxrs.fortest.GenericEntityImpl3;
import org.apache.cxf.jaxrs.fortest.GenericEntityImpl4;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.resources.Chapter;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.EasyMock;

import org.junit.Assert;
import org.junit.Test;

public class SelectMethodCandidatesTest extends Assert {
    
    @Test
    public void testFindFromAbstractGenericClass() throws Exception {
        doTestGenericSuperType(BookEntity.class, "POST");
    }
    
    @Test
    public void testFindFromAbstractGenericClass2() throws Exception {
        doTestGenericSuperType(BookEntity2.class, "POST");
    }
    
    @Test
    public void testFindFromAbstractGenericInterface() throws Exception {
        doTestGenericSuperType(GenericEntityImpl.class, "POST");
    }
    
    @Test
    public void testFindFromAbstractGenericInterface2() throws Exception {
        doTestGenericSuperType(GenericEntityImpl2.class, "POST");
    }
    
    @Test
    public void testFindFromAbstractGenericImpl3() throws Exception {
        doTestGenericSuperType(GenericEntityImpl3.class, "POST");
    }
    
    @Test
    public void testFindFromAbstractGenericImpl4() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(GenericEntityImpl4.class);
        sf.create();
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        String contentTypes = "text/xml";
        String acceptContentTypes = "text/xml";
        
        Message m = new MessageImpl();
        m.put(Message.CONTENT_TYPE, "text/xml");
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(m);
        m.setExchange(ex);
        Endpoint e = EasyMock.createMock(Endpoint.class);
        e.isEmpty();
        EasyMock.expectLastCall().andReturn(true).anyTimes();
        e.size();
        EasyMock.expectLastCall().andReturn(0).anyTimes();
        e.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(null).anyTimes();
        e.get(ServerProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(ServerProviderFactory.getInstance()).times(2);
        e.get("org.apache.cxf.jaxrs.comparator");
        EasyMock.expectLastCall().andReturn(null);
        EasyMock.replay(e);
        ex.put(Endpoint.class, e);
        
        MetadataMap<String, String> values = new MetadataMap<String, String>();
        OperationResourceInfo ori = findTargetResourceClass(resources, m, 
                                                            "/books",
                                                            "POST",
                                                            values, contentTypes, 
                                                            JAXRSUtils.sortMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("resourceMethod needs to be selected", "postEntity",
                     ori.getMethodToInvoke().getName());
        
        String value = "<Books><Book><name>The Book</name><id>2</id></Book></Books>";
        m.setContent(InputStream.class, new ByteArrayInputStream(value.getBytes()));
        List<Object> params = JAXRSUtils.processParameters(ori, values, m);
        assertEquals(1, params.size());
        List<?> books = (List<?>)params.get(0);
        assertEquals(1, books.size());
        Book book = (Book)books.get(0);
        assertNotNull(book);
        assertEquals(2L, book.getId());
        assertEquals("The Book", book.getName());
    }
    
    @Test
    public void testFindFromAbstractGenericClass3() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(BookEntity.class);
        sf.create();
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        String contentTypes = "text/xml";
        String acceptContentTypes = "text/xml";
        
        Message m = new MessageImpl();
        m.put(Message.CONTENT_TYPE, "text/xml");
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(m);
        m.setExchange(ex);
        Endpoint e = EasyMock.createMock(Endpoint.class);
        e.isEmpty();
        EasyMock.expectLastCall().andReturn(true).anyTimes();
        e.size();
        EasyMock.expectLastCall().andReturn(0).anyTimes();
        e.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(null).anyTimes();
        e.get(ServerProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(ServerProviderFactory.getInstance()).times(3);
        e.get("org.apache.cxf.jaxrs.comparator");
        EasyMock.expectLastCall().andReturn(null);
        EasyMock.replay(e);
        ex.put(Endpoint.class, e);
        
        MetadataMap<String, String> values = new MetadataMap<String, String>();
        OperationResourceInfo ori = findTargetResourceClass(resources, m, 
                                                            "/books",
                                                            "PUT",
                                                            values, contentTypes, 
                                                            JAXRSUtils.sortMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("resourceMethod needs to be selected", "putEntity",
                     ori.getMethodToInvoke().getName());
        
        String value = "<Chapter><title>The Book</title><id>2</id></Chapter>";
        m.setContent(InputStream.class, new ByteArrayInputStream(value.getBytes()));
        List<Object> params = JAXRSUtils.processParameters(ori, values, m);
        assertEquals(1, params.size());
        Chapter c = (Chapter)params.get(0);
        assertNotNull(c);
        assertEquals(2L, c.getId());
        assertEquals("The Book", c.getTitle());
    }
    
    @Test
    public void testRootResourcesWithSameName() throws Exception {
        doTestRootResourcesWithSameName("/a/books", "put", RootResource.class);
        doTestRootResourcesWithSameName("/a1/books", "put", RootResource.class);
    }
    
    @Test
    public void testRootResourcesWithSameName2() throws Exception {
        doTestRootResourcesWithSameName("/a/books/1", "put", RootResource2.class);
        doTestRootResourcesWithSameName("/c/thebooks", "put2", RootResource2.class);
        doTestRootResourcesWithSameName("/b/books", "put", RootResource3.class);
    }
    
    
    private void doTestRootResourcesWithSameName(String path, String methodName, Class<?> expectedRoot) 
        throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(RootResource.class, RootResource2.class, RootResource3.class);
        sf.create();
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        assertEquals(3, resources.size());
        String contentTypes = "text/xml";
        String acceptContentTypes = "text/xml";
        
        Message m = prepareMessage();
        
        MetadataMap<String, String> values = new MetadataMap<String, String>();
        OperationResourceInfo ori = findTargetResourceClass(resources, m, 
                                                            path,
                                                            "PUT",
                                                            values, contentTypes, 
                                                            JAXRSUtils.sortMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("resourceMethod needs to be selected", methodName,
                     ori.getMethodToInvoke().getName());
        
        assertSame(expectedRoot, ori.getClassResourceInfo().getServiceClass());
    }
    
    private Message prepareMessage() {
        Message m = new MessageImpl();
        m.put(Message.CONTENT_TYPE, "text/xml");
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(m);
        m.setExchange(ex);
        Endpoint e = EasyMock.createMock(Endpoint.class);
        e.isEmpty();
        EasyMock.expectLastCall().andReturn(true).anyTimes();
        e.size();
        EasyMock.expectLastCall().andReturn(0).anyTimes();
        e.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(null).anyTimes();
        e.get(ServerProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(ServerProviderFactory.getInstance()).times(3);
        e.get("org.apache.cxf.jaxrs.comparator");
        EasyMock.expectLastCall().andReturn(null);
        EasyMock.replay(e);
        ex.put(Endpoint.class, e);
        return m;
    }
    
    private void doTestGenericSuperType(Class<?> serviceClass, String methodName) throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(serviceClass);
        sf.create();
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        String contentTypes = "text/xml";
        String acceptContentTypes = "text/xml";
        
        Message m = new MessageImpl();
        m.put(Message.CONTENT_TYPE, "text/xml");
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(m);
        m.setExchange(ex);
        Endpoint e = EasyMock.createMock(Endpoint.class);
        e.isEmpty();
        EasyMock.expectLastCall().andReturn(true).anyTimes();
        e.size();
        EasyMock.expectLastCall().andReturn(0).anyTimes();
        e.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(null).anyTimes();
        e.get(ServerProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(ServerProviderFactory.getInstance()).times(3);
        e.get("org.apache.cxf.jaxrs.comparator");
        EasyMock.expectLastCall().andReturn(null);
        EasyMock.replay(e);
        ex.put(Endpoint.class, e);
        
        MetadataMap<String, String> values = new MetadataMap<String, String>();
        OperationResourceInfo ori = findTargetResourceClass(resources, m, 
                                                            "/books",
                                                            methodName,
                                                            values, contentTypes, 
                                                            JAXRSUtils.sortMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("resourceMethod needs to be selected", methodName.toLowerCase() + "Entity",
                     ori.getMethodToInvoke().getName());
        
        String value = "<Book><name>The Book</name><id>2</id></Book>";
        m.setContent(InputStream.class, new ByteArrayInputStream(value.getBytes()));
        List<Object> params = JAXRSUtils.processParameters(ori, values, m);
        assertEquals(1, params.size());
        Book book = (Book)params.get(0);
        assertNotNull(book);
        assertEquals(2L, book.getId());
        assertEquals("The Book", book.getName());
    }
    
    @Test
    public void testFindTargetSubResource() throws Exception {
        doTestFindTargetSubResource("/1/2/3/d/resource", "resourceMethod");
    }
    
    @Test
    public void testFindTargetSubResource2() throws Exception {
        doTestFindTargetSubResource("/1/2/3/d/resource/sub", "subresource");
    }
    
    @Test
    public void testFindTargetSubResource3() throws Exception {
        doTestFindTargetSubResource("/1/2/3/d/resource2/2/2", "resourceMethod2");
    }
    
    @Test
    public void testFindTargetSubResource4() throws Exception {
        doTestFindTargetSubResource("/1/2/3/d/resource2/1/2", "subresource2");
    }
    
    public void doTestFindTargetSubResource(String path, String method) throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.TestResource.class);
        sf.create();
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        String contentTypes = "*/*";
        String acceptContentTypes = "text/xml,*/*";
        
        MetadataMap<String, String> values = new MetadataMap<String, String>();
        OperationResourceInfo ori = findTargetResourceClass(resources, null, 
                                                            path,
                                                            "GET",
                                                            values, contentTypes, 
                                                            JAXRSUtils.sortMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("resourceMethod needs to be selected", method,
                     ori.getMethodToInvoke().getName());
    }
    
    @Test
    public void testSelectUsingQualityFactors() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.TestResource.class);
        sf.create();
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        String contentTypes = "*/*";
        String acceptContentTypes = "application/xml;q=0.5,application/json";
        
        MetadataMap<String, String> values = new MetadataMap<String, String>();
        OperationResourceInfo ori = findTargetResourceClass(resources, null, 
                                                            "/1/2/3/d/resource1",
                                                            "GET",
                                                            values, contentTypes, 
                                                            JAXRSUtils.sortMediaTypes(acceptContentTypes));
        
        assertNotNull(ori);
        assertEquals("jsonResource needs to be selected", "jsonResource",
                     ori.getMethodToInvoke().getName());
    }
    
    @Test
    public void testFindTargetResourceClassWithTemplates() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.TestResource.class);
        sf.create();
        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();

        String contentTypes = "*/*";
        String acceptContentTypes = "application/xml";

        //If acceptContentTypes does not specify a specific Mime type, the  
        //method is declared with a most specific ProduceMime type is selected.
        MetadataMap<String, String> values = new MetadataMap<String, String>();
        OperationResourceInfo ori = findTargetResourceClass(resources, null, 
                                                            "/1/2/3/d",
                                                            "GET",
                                                            values, contentTypes, 
            Collections.singletonList(MediaType.valueOf(acceptContentTypes)));
        
        assertNotNull(ori);
        assertEquals("listMethod needs to be selected", "listMethod", 
                     ori.getMethodToInvoke().getName());
        
        
        acceptContentTypes = "application/xml,application/json";
        ori = findTargetResourceClass(resources, null, 
                                                            "/1/2/3/d/1",
                                                            "GET",
                                                            values, contentTypes, 
                                                            JAXRSUtils.parseMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("readMethod needs to be selected", "readMethod", 
                     ori.getMethodToInvoke().getName());
        
        
        contentTypes = "application/xml";
        acceptContentTypes = "application/xml";
        ori = findTargetResourceClass(resources, null, 
                                                            "/1/2/3/d/1",
                                                            "GET",
                                                            values, contentTypes, 
            Collections.singletonList(MediaType.valueOf(acceptContentTypes)));
        assertNotNull(ori);
        assertEquals("readMethod needs to be selected", "readMethod", 
                     ori.getMethodToInvoke().getName());
        
        contentTypes = "application/json";
        acceptContentTypes = "application/json";
        ori = findTargetResourceClass(resources, null, 
                                      "/1/2/3/d/1/bar/baz/baz",
                                      "GET",
                                      values, contentTypes, 
            Collections.singletonList(MediaType.valueOf(acceptContentTypes)));
        assertNotNull(ori);
        assertEquals("readMethod2 needs to be selected", "readMethod2", 
                     ori.getMethodToInvoke().getName());
        
        contentTypes = "application/json";
        acceptContentTypes = "application/json";
        ori = findTargetResourceClass(resources, null, 
                                      "/1/2/3/d/1",
                                      "GET",
                                      values, contentTypes, 
            Collections.singletonList(MediaType.valueOf(acceptContentTypes)));
        assertNotNull(ori);
        assertEquals("unlimitedPath needs to be selected", "unlimitedPath", 
                     ori.getMethodToInvoke().getName());
        
        ori = findTargetResourceClass(resources, null, 
                                      "/1/2/3/d/1/2",
                                      "GET",
                                      values, contentTypes, 
            Collections.singletonList(MediaType.valueOf(acceptContentTypes)));
        assertNotNull(ori);
        assertEquals("limitedPath needs to be selected", "limitedPath", 
                     ori.getMethodToInvoke().getName());
        
    }
    
    @Test
    public void testSelectBar() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.TestResource.class);
        sf.create();
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        
        String contentTypes = "*/*";
        String acceptContentTypes = "application/bar,application/foo";
        
        MetadataMap<String, String> values = new MetadataMap<String, String>();
        OperationResourceInfo ori = findTargetResourceClass(resources, null, 
                                      "/1/2/3/d/custom",
                                      "GET",
                                      values, contentTypes, 
                                      JAXRSUtils.sortMediaTypes(acceptContentTypes));
        
        assertNotNull(ori);
        assertEquals("readBar", ori.getMethodToInvoke().getName());
        acceptContentTypes = "application/foo,application/bar";
        ori = findTargetResourceClass(resources, null, 
                                      "/1/2/3/d/custom",
                                      "GET",
                                      values, contentTypes, 
                                      JAXRSUtils.sortMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("readFoo", ori.getMethodToInvoke().getName());
        
        acceptContentTypes = "application/foo;q=0.5,application/bar";
        ori = findTargetResourceClass(resources, null, 
                                      "/1/2/3/d/custom",
                                      "GET",
                                      values, contentTypes, 
                                      JAXRSUtils.sortMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("readBar", ori.getMethodToInvoke().getName());
        
        acceptContentTypes = "application/foo,application/bar;q=0.5";
        ori = findTargetResourceClass(resources, null, 
                                      "/1/2/3/d/custom",
                                      "GET",
                                      values, contentTypes, 
                                      JAXRSUtils.sortMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("readFoo", ori.getMethodToInvoke().getName());
        
    }
    
    private static OperationResourceInfo findTargetResourceClass(List<ClassResourceInfo> resources,
                                                                 Message message,
                                                                 String path, 
                                                                 String httpMethod,
                                                                 MultivaluedMap<String, String> values,
                                                                 String requestContentType, 
                                                                 List<MediaType> acceptContentTypes) {
        message = message == null ? new MessageImpl() : message; 
        Map<ClassResourceInfo, MultivaluedMap<String, String>> mResources 
            = JAXRSUtils.selectResourceClass(resources, path, message);
         
        if (mResources != null) {
            OperationResourceInfo ori = JAXRSUtils.findTargetMethod(mResources, null, httpMethod, 
                                                    values, requestContentType, acceptContentTypes);
            if (ori != null) {
                return ori;
            }
        }
         
        return null;
    }
    @Path("{a}")
    @Produces("text/xml")
    @Consumes("text/xml")
    public static class RootResource {
        @PUT
        @Path("books")
        public void put() {
            
        }
    }
    
    @Path("{b}")
    @Produces("text/xml")
    @Consumes("text/xml")
    public static class RootResource2 {
        @PUT
        @Path("books/1")
        public void put() {
            
        }
        
        @PUT
        @Path("thebooks")
        public void put2() {
            
        }
    }
    
    @Path("b")
    @Produces("text/xml")
    @Consumes("text/xml")
    public static class RootResource3 {
        @PUT
        @Path("books")
        public void put() {
            
        }
    }
}
