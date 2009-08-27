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

package org.apache.cxf.jaxrs.model;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.utils.ResourceUtils;

import org.junit.Assert;
import org.junit.Test;

public class ClassResourceInfoTest extends Assert {
    
    @Path("/bar")
    @Produces("test/bar")
    @Consumes("test/foo")
    static class TestClass {
        @Context UriInfo u;
        @Context HttpHeaders h;
        @Resource HttpServletRequest req;
        @Resource HttpServletResponse res;
        @Resource ServletContext c;
        int i;
        
        @GET
        public void getIt() { 
            
        }
    }
    
    static class TestClass1 extends TestClass {
        @GET
        public void getIt() { 
            
        }
    }
    
    static class TestClass2 extends TestClass1 {
        @GET
        public void getIt() { 
            
        }
    }
    
    static class TestClass3 {
        @Resource HttpServletRequest req;
        @Resource HttpServletResponse res;
        @Resource ServletContext c;
        
        @GET
        public void getIt() { 
            
        }
        
        @HEAD
        public void head() { 
            
        }
    }
    
    @Test
    public void testGetHttpContexts() {
        ClassResourceInfo c = new ClassResourceInfo(TestClass.class);
        List<Field> fields = c.getContextFields();
        assertEquals("Only root classes should check these fields", 0, fields.size());
        
        c = new ClassResourceInfo(TestClass.class, true);
        fields = c.getContextFields();
        assertEquals("2 http context fields available", 2, fields.size());
        assertTrue("Wrong fields selected", 
                   (fields.get(0).getType() == UriInfo.class
                   || fields.get(1).getType() == UriInfo.class)
                   && (fields.get(0).getType() == HttpHeaders.class
                   || fields.get(1).getType() == HttpHeaders.class));
    }

    @Test
    public void testGetResources() {
        ClassResourceInfo c = new ClassResourceInfo(TestClass3.class);
        List<Field> fields = c.getResourceFields();
        assertEquals("Only root classes should check these fields", 0, fields.size());
        c = new ClassResourceInfo(TestClass3.class, true);
        fields = c.getResourceFields();
        
        Set<Class<?>> clses = new HashSet<Class<?>>(); 
        for (Field f : fields) {
            clses.add(f.getType());
        }
        assertEquals("3 resources fields available", 3, fields.size());
        assertTrue("Wrong fields selected",
                   clses.contains(HttpServletRequest.class)
                   && clses.contains(HttpServletResponse.class)
                   && clses.contains(ServletContext.class)); 
    }
    
    @Test
    public void testGetPath() {
        ClassResourceInfo c = new ClassResourceInfo(TestClass.class);
        assertEquals("/bar", c.getPath().value());
        
        c = new ClassResourceInfo(TestClass1.class);
        assertEquals("/bar", c.getPath().value());
        
        c = new ClassResourceInfo(TestClass2.class);
        assertEquals("/bar", c.getPath().value());
    }
    
    @Test
    public void testGetProduce() {
        ClassResourceInfo c = new ClassResourceInfo(TestClass.class);
        assertEquals("test/bar", c.getProduceMime().get(0).toString());
        
        c = new ClassResourceInfo(TestClass1.class);
        assertEquals("test/bar", c.getProduceMime().get(0).toString());
        
        c = new ClassResourceInfo(TestClass2.class);
        assertEquals("test/bar", c.getProduceMime().get(0).toString());
    }
    
    @Test
    public void testGetConsume() {
        ClassResourceInfo c = new ClassResourceInfo(TestClass.class);
        assertEquals("test/foo", c.getConsumeMime().get(0).toString());
        
        c = new ClassResourceInfo(TestClass1.class);
        assertEquals("test/foo", c.getConsumeMime().get(0).toString());
        
        c = new ClassResourceInfo(TestClass2.class);
        assertEquals("test/foo", c.getConsumeMime().get(0).toString());
    }
    
    @Test
    public void testGetSameSubresource() {
        ClassResourceInfo c = new ClassResourceInfo(TestClass.class);
        assertEquals("No subresources expected", 0, c.getSubResources().size());
        assertNull(c.findResource(TestClass.class, TestClass.class));
        ClassResourceInfo c1 = c.getSubResource(TestClass.class, TestClass.class);
        assertNotNull(c1);
        assertSame(c1, c.findResource(TestClass.class, TestClass.class));
        assertSame(c1, c.getSubResource(TestClass.class, TestClass.class));
        assertEquals("Single subresources expected", 1, c.getSubResources().size());
    }
    
    @Test
    public void testGetSubresourceSubclass() {
        ClassResourceInfo c = new ClassResourceInfo(TestClass.class);
        assertEquals("No subresources expected", 0, c.getSubResources().size());
        assertNull(c.findResource(TestClass.class, TestClass1.class));
        ClassResourceInfo c1 = c.getSubResource(TestClass.class, TestClass1.class);
        assertNotNull(c1);
        assertSame(c1, c.findResource(TestClass.class, TestClass1.class));
        assertNull(c.findResource(TestClass.class, TestClass2.class));
        ClassResourceInfo c2 = c.getSubResource(TestClass.class, TestClass2.class);
        assertNotNull(c2);
        assertSame(c2, c.findResource(TestClass.class, TestClass2.class));
        assertSame(c2, c.getSubResource(TestClass.class, TestClass2.class));
        assertNotSame(c1, c2);
        
    }
    
    @Test
    public void testAllowedMethods() {
        ClassResourceInfo c = ResourceUtils.createClassResourceInfo(
                                  TestClass3.class, TestClass3.class, false, false);
        Set<String> methods = c.getAllowedMethods();
        assertEquals(2, methods.size());
        assertTrue(methods.contains("HEAD") && methods.contains("GET"));
    }
}
