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

import javax.ws.rs.core.MediaType;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.fortest.BookEntity;
import org.apache.cxf.jaxrs.fortest.BookEntity2;
import org.apache.cxf.jaxrs.fortest.GenericEntityImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.resources.Chapter;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.classextension.EasyMock;

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
    public void testFindFromAbstractGenericClass3() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(BookEntity.class);
        sf.create();
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        String contentTypes = "text/xml";
        String acceptContentTypes = "text/xml";
        
        MetadataMap<String, String> values = new MetadataMap<String, String>();
        ClassResourceInfo resource = JAXRSUtils.selectResourceClass(resources, "/books", values);
        OperationResourceInfo ori = JAXRSUtils.findTargetMethod(resource, 
                                    values.getFirst(URITemplate.FINAL_MATCH_GROUP), 
                                    "PUT", values, contentTypes, 
                                    JAXRSUtils.sortMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("resourceMethod needs to be selected", "putEntity",
                     ori.getMethodToInvoke().getName());
        
        Message m = new MessageImpl();
        m.put(Message.CONTENT_TYPE, "text/xml");
        Exchange ex = new ExchangeImpl();
        m.setExchange(ex);
        Endpoint e = EasyMock.createMock(Endpoint.class);
        e.get(ProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(ProviderFactory.getInstance());
        EasyMock.replay(e);
        ex.put(Endpoint.class, e);
        
        String value = "<Chapter><title>The Book</title><id>2</id></Chapter>";
        m.setContent(InputStream.class, new ByteArrayInputStream(value.getBytes()));
        List<Object> params = JAXRSUtils.processParameters(ori, values, m);
        assertEquals(1, params.size());
        Chapter c = (Chapter)params.get(0);
        assertNotNull(c);
        assertEquals(2L, c.getId());
        assertEquals("The Book", c.getTitle());
    }
    
    private void doTestGenericSuperType(Class<?> serviceClass, String methodName) {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(serviceClass);
        sf.create();
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        String contentTypes = "text/xml";
        String acceptContentTypes = "text/xml";
        
        MetadataMap<String, String> values = new MetadataMap<String, String>();
        ClassResourceInfo resource = JAXRSUtils.selectResourceClass(resources, "/books", values);
        OperationResourceInfo ori = JAXRSUtils.findTargetMethod(resource, 
                                    values.getFirst(URITemplate.FINAL_MATCH_GROUP), 
                                    methodName, values, contentTypes, 
                                    JAXRSUtils.sortMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("resourceMethod needs to be selected", methodName.toLowerCase() + "Entity",
                     ori.getMethodToInvoke().getName());
        
        Message m = new MessageImpl();
        m.put(Message.CONTENT_TYPE, "text/xml");
        Exchange ex = new ExchangeImpl();
        m.setExchange(ex);
        Endpoint e = EasyMock.createMock(Endpoint.class);
        e.get(ProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(ProviderFactory.getInstance());
        EasyMock.replay(e);
        ex.put(Endpoint.class, e);
        
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
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.TestResource.class);
        sf.create();
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        String contentTypes = "*/*";
        String acceptContentTypes = "text/xml,*/*";
        
        MetadataMap<String, String> values = new MetadataMap<String, String>();
        ClassResourceInfo resource = JAXRSUtils.selectResourceClass(resources, "/1/2/3/d/resource", values);
        OperationResourceInfo ori = JAXRSUtils.findTargetMethod(resource, 
                                    values.getFirst(URITemplate.FINAL_MATCH_GROUP), 
                                    "GET", values, contentTypes, 
                                    JAXRSUtils.sortMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("resourceMethod needs to be selected", "resourceMethod",
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
        ClassResourceInfo resource = JAXRSUtils.selectResourceClass(resources, "/1/2/3/d/resource1", values);
        OperationResourceInfo ori = JAXRSUtils.findTargetMethod(resource, 
                                    values.getFirst(URITemplate.FINAL_MATCH_GROUP), 
                                    "GET", values, contentTypes, 
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
        ClassResourceInfo resource = JAXRSUtils.selectResourceClass(resources, "/1/2/3/d", values);
        OperationResourceInfo ori = JAXRSUtils.findTargetMethod(resource, 
                                    values.getFirst(URITemplate.FINAL_MATCH_GROUP), 
                                    "GET", values, contentTypes, 
                                    Collections.singletonList(MediaType.valueOf(acceptContentTypes)));
        assertNotNull(ori);
        assertEquals("listMethod needs to be selected", "listMethod", 
                     ori.getMethodToInvoke().getName());
        
        
        acceptContentTypes = "application/xml,application/json";
        resource = JAXRSUtils.selectResourceClass(resources, "/1/2/3/d/1", values);
        ori = JAXRSUtils.findTargetMethod(resource, 
                                        values.getFirst(URITemplate.FINAL_MATCH_GROUP), 
                                        "GET", values, contentTypes, 
                                        JAXRSUtils.parseMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("readMethod needs to be selected", "readMethod", 
                     ori.getMethodToInvoke().getName());
        
        
        contentTypes = "application/xml";
        acceptContentTypes = "application/xml";
        resource = JAXRSUtils.selectResourceClass(resources, "/1/2/3/d/1", values);
        ori = JAXRSUtils.findTargetMethod(resource, 
                                        values.getFirst(URITemplate.FINAL_MATCH_GROUP), 
                                        "GET", values, contentTypes, 
                                        Collections.singletonList(MediaType.valueOf(acceptContentTypes)));
        assertNotNull(ori);
        assertEquals("readMethod needs to be selected", "readMethod", 
                     ori.getMethodToInvoke().getName());
        
        contentTypes = "application/json";
        acceptContentTypes = "application/json";
        resource = JAXRSUtils.selectResourceClass(resources, "/1/2/3/d/1/bar/baz/baz", values);
        ori = JAXRSUtils.findTargetMethod(resource, 
                                        values.getFirst(URITemplate.FINAL_MATCH_GROUP), 
                                        "GET", values, contentTypes, 
                                        Collections.singletonList(MediaType.valueOf(acceptContentTypes)));
        assertNotNull(ori);
        assertEquals("readMethod2 needs to be selected", "readMethod2", 
                     ori.getMethodToInvoke().getName());
        
        contentTypes = "application/json";
        acceptContentTypes = "application/json";
        resource = JAXRSUtils.selectResourceClass(resources, "/1/2/3/d/1", values);
        ori = JAXRSUtils.findTargetMethod(resource, 
                                        values.getFirst(URITemplate.FINAL_MATCH_GROUP), 
                                        "GET", values, contentTypes, 
                                        Collections.singletonList(MediaType.valueOf(acceptContentTypes)));
        assertNotNull(ori);
        assertEquals("unlimitedPath needs to be selected", "unlimitedPath", 
                     ori.getMethodToInvoke().getName());
        
        resource = JAXRSUtils.selectResourceClass(resources, "/1/2/3/d/1/2", values);
        ori = JAXRSUtils.findTargetMethod(resource, 
                                        values.getFirst(URITemplate.FINAL_MATCH_GROUP), 
                                        "GET", values, contentTypes, 
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
        
        MetadataMap<String, String> values = new MetadataMap<String, String>();
        ClassResourceInfo resource = JAXRSUtils.selectResourceClass(resources, "/1/2/3/d/custom", values);
        
        String contentTypes = "*/*";
        String acceptContentTypes = "application/bar,application/foo";
        OperationResourceInfo ori = JAXRSUtils.findTargetMethod(resource, 
                                    values.getFirst(URITemplate.FINAL_MATCH_GROUP), 
                                    "GET", values, contentTypes, 
                                    JAXRSUtils.sortMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("readBar", ori.getMethodToInvoke().getName());
        acceptContentTypes = "application/foo,application/bar";
        resource = JAXRSUtils.selectResourceClass(resources, "/1/2/3/d/custom", values);
        ori = JAXRSUtils.findTargetMethod(resource, 
                                    values.getFirst(URITemplate.FINAL_MATCH_GROUP), 
                                    "GET", values, contentTypes, 
                                    JAXRSUtils.sortMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("readFoo", ori.getMethodToInvoke().getName());
        
        acceptContentTypes = "application/foo;q=0.5,application/bar";
        resource = JAXRSUtils.selectResourceClass(resources, "/1/2/3/d/custom", values);
        ori = JAXRSUtils.findTargetMethod(resource, 
                                    values.getFirst(URITemplate.FINAL_MATCH_GROUP), 
                                    "GET", values, contentTypes, 
                                    JAXRSUtils.sortMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("readBar", ori.getMethodToInvoke().getName());
        
        acceptContentTypes = "application/foo,application/bar;q=0.5";
        resource = JAXRSUtils.selectResourceClass(resources, "/1/2/3/d/custom", values);
        ori = JAXRSUtils.findTargetMethod(resource, 
                                    values.getFirst(URITemplate.FINAL_MATCH_GROUP), 
                                    "GET", values, contentTypes, 
                                    JAXRSUtils.sortMediaTypes(acceptContentTypes));
        assertNotNull(ori);
        assertEquals("readFoo", ori.getMethodToInvoke().getName());
        
    }
}
