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

import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.MethodDispatcher;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JAXRSServiceFactoryBeanTest extends Assert {

    @Before
    public void setUp() throws Exception {

    }
    @Test
    public void testNoSubResources() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setEnableStaticResolution(true);
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStoreNoSubResource.class);
        sf.create();
        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        assertEquals(1, resources.size());
        
        //Verify root ClassResourceInfo: BookStoreNoSubResource
        ClassResourceInfo rootCri = resources.get(0);
        assertNotNull(rootCri.getURITemplate());
        URITemplate template = rootCri.getURITemplate();
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();
        assertTrue(template.match("/bookstore/books/123", values));     
        assertFalse(rootCri.hasSubResources());   
        MethodDispatcher md = rootCri.getMethodDispatcher();
        assertEquals(7, md.getOperationResourceInfos().size());  
        Set<OperationResourceInfo> ops = md.getOperationResourceInfos();
        assertTrue("No operation found", verifyOp(ops, "getBook", "GET", false));
        assertTrue("No operation found", verifyOp(ops, "getBookStoreInfo", "GET", false));
        assertTrue("No operation found", verifyOp(ops, "getBooks", "GET", false));
        assertTrue("No operation found", verifyOp(ops, "getBookJSON", "GET", false));
        assertTrue("No operation found", verifyOp(ops, "addBook", "POST", false));
        assertTrue("No operation found", verifyOp(ops, "updateBook", "PUT", false));
        assertTrue("No operation found", verifyOp(ops, "deleteBook", "DELETE", false));
        
    }

    @Test
    public void testSubresourcesOnlyDynamicResolution() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStoreSubresourcesOnly.class);
        sf.create();
        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        assertEquals(1, resources.size());
    }
    
    @Test
    public void testSubResources() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setEnableStaticResolution(true);
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStore.class);
        sf.create();
        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        assertEquals(1, resources.size());
        
        //Verify root ClassResourceInfo: BookStore
        ClassResourceInfo rootCri = resources.get(0);
        assertNotNull(rootCri.getURITemplate());
        URITemplate template = rootCri.getURITemplate();
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();
        assertTrue(template.match("/bookstore/books/123", values));     
        assertTrue(rootCri.hasSubResources());   
        MethodDispatcher md = rootCri.getMethodDispatcher();
        assertEquals(7, md.getOperationResourceInfos().size());  
        for (OperationResourceInfo ori : md.getOperationResourceInfos()) {
            if ("getDescription".equals(ori.getMethodToInvoke().getName())) {
                assertEquals("GET", ori.getHttpMethod());
                assertEquals("/path", ori.getURITemplate().getValue());
                assertEquals("text/bar", ori.getProduceTypes().get(0).toString());
                assertEquals("text/foo", ori.getConsumeTypes().get(0).toString());
                assertFalse(ori.isSubResourceLocator());
            } else if ("getAuthor".equals(ori.getMethodToInvoke().getName())) {
                assertEquals("GET", ori.getHttpMethod());
                assertEquals("/path2", ori.getURITemplate().getValue());
                assertEquals("text/bar2", ori.getProduceTypes().get(0).toString());
                assertEquals("text/foo2", ori.getConsumeTypes().get(0).toString());
                assertFalse(ori.isSubResourceLocator());
            } else if ("getBook".equals(ori.getMethodToInvoke().getName())) {
                assertNull(ori.getHttpMethod());
                assertNotNull(ori.getURITemplate());
                assertTrue(ori.isSubResourceLocator());
            }  else if ("getNewBook".equals(ori.getMethodToInvoke().getName())) {
                assertNull(ori.getHttpMethod());
                assertNotNull(ori.getURITemplate());
                assertTrue(ori.isSubResourceLocator());
            }  else if ("addBook".equals(ori.getMethodToInvoke().getName())) {
                assertEquals("POST", ori.getHttpMethod());
                assertNotNull(ori.getURITemplate());
                assertFalse(ori.isSubResourceLocator());
            } else if ("updateBook".equals(ori.getMethodToInvoke().getName())) {
                assertEquals("PUT", ori.getHttpMethod());
                assertNotNull(ori.getURITemplate());
                assertFalse(ori.isSubResourceLocator());
            } else if ("deleteBook".equals(ori.getMethodToInvoke().getName())) {
                assertEquals("DELETE", ori.getHttpMethod());
                assertNotNull(ori.getURITemplate());
                assertFalse(ori.isSubResourceLocator());
            } else {
                fail("unexpected OperationResourceInfo" + ori.getMethodToInvoke().getName());
            }
        }
        
        // Verify sub-resource ClassResourceInfo: Book
        assertEquals(1, rootCri.getSubResources().size());
        ClassResourceInfo subCri = rootCri.getSubResources().iterator().next();        
        assertNull(subCri.getURITemplate());
        assertEquals(org.apache.cxf.jaxrs.resources.Book.class, subCri.getResourceClass());
        MethodDispatcher subMd = subCri.getMethodDispatcher();
        assertEquals(2, subMd.getOperationResourceInfos().size());
        //getChapter method
        OperationResourceInfo subOri = subMd.getOperationResourceInfos().iterator().next();
        assertEquals("GET", subOri.getHttpMethod());
        assertNotNull(subOri.getURITemplate());
        
        //getState method
        OperationResourceInfo subOri2 = subMd.getOperationResourceInfos().iterator().next();
        assertEquals("GET", subOri2.getHttpMethod());
        assertNotNull(subOri2.getURITemplate());
    }

    private boolean verifyOp(Set<OperationResourceInfo> ops,
                             String opName, 
                             String httpMethod, 
                             boolean isSubresource) {
        for (OperationResourceInfo ori : ops) {
            if (opName.equals(ori.getMethodToInvoke().getName())) {
                assertEquals(httpMethod, ori.getHttpMethod());
                assertNotNull(ori.getURITemplate());
                assertEquals(isSubresource, ori.isSubResourceLocator());
                return true;
            }
        }
        return false;
    }
}
