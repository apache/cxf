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

package org.apache.cxf.jaxrs.provider;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.service.Service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class DataBindingJSONProviderTest extends Assert {

    private ClassResourceInfo c;
    
    @Before
    public void setUp() {
        c = ResourceUtils.createClassResourceInfo(TheBooks.class, TheBooks.class, true, true);
    }
    
    public void testJAXBWrite() throws Exception {
        Service s = new JAXRSServiceImpl(Collections.singletonList(c), true);
        DataBinding binding = new JAXBDataBinding();
        binding.initialize(s);
        DataBindingJSONProvider p = new DataBindingJSONProvider();
        p.setDataBinding(binding);
        Book b = new Book("CXF", 127L);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(b, Book.class, Book.class,
            new Annotation[0], MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), bos);
        String data = "{\"Book\":{\"id\":127,\"name\":\"CXF\",\"state\":\"\"}}";
        assertEquals(bos.toString(), data);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testJAXBRead() throws Exception {
        String data = "{\"Book\":{\"id\":127,\"name\":\"CXF\",\"state\":\"\"}}";
        Service s = new JAXRSServiceImpl(Collections.singletonList(c), true);
        DataBinding binding = new JAXBDataBinding();
        binding.initialize(s);
        DataBindingJSONProvider p = new DataBindingJSONProvider();
        p.setDataBinding(binding);
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Book book = (Book)p.readFrom((Class)Book.class, Book.class,
                                      new Annotation[0], MediaType.APPLICATION_JSON_TYPE, 
                                      new MetadataMap<String, String>(), is);
        assertEquals("CXF", book.getName());
        assertEquals(127L, book.getId());
    }
    
    @Test
    public void testAegisWrite() throws Exception {
        Service s = new JAXRSServiceImpl(Collections.singletonList(c), true);
        s.put("writeXsiType", true);
        AegisDatabinding binding = new AegisDatabinding();
        binding.initialize(s);
        DataBindingJSONProvider p = new DataBindingJSONProvider();
        p.setDataBinding(binding);
        Book b = new Book("CXF", 127L);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(b, Book.class, Book.class,
            new Annotation[0], MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), bos);
        doTestAegisRead(bos.toString());
    }
    
    @Test
    @Ignore
    public void testAegisCollectionWrite() throws Exception {
        Service s = new JAXRSServiceImpl(Collections.singletonList(c), true);
        s.put("writeXsiType", true);
        AegisDatabinding binding = new AegisDatabinding();
        binding.initialize(s);
        DataBindingJSONProvider p = new DataBindingJSONProvider();
        p.setDataBinding(binding);
        
        Book b = new Book("CXF", 127L);
        List<Book> books = new ArrayList<Book>();
        books.add(b);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(books, List.class, Book.class,
            new Annotation[0], MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), bos);
        
        System.out.println(bos.toString());         
    }
    
    @Test
    public void testAegisRead() throws Exception {
        String data = "{\"ns1.Book\":{\"@xsi.type\":\"ns1:Book\",\"ns1.id\":127,"
            + "\"ns1.name\":\"CXF\",\"ns1.state\":\"\"}}";
        doTestAegisRead(data);
    }
    
    @SuppressWarnings("unchecked")
    public void doTestAegisRead(String data) throws Exception {
        Service s = new JAXRSServiceImpl(Collections.singletonList(c), true);
        s.put("readXsiType", true);
        AegisDatabinding binding = new AegisDatabinding();
        binding.initialize(s);
        DataBindingJSONProvider p = new DataBindingJSONProvider();
        p.setDataBinding(binding);
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Book book = (Book)p.readFrom((Class)Book.class, Book.class,
                                      new Annotation[0], MediaType.APPLICATION_XML_TYPE, 
                                      new MetadataMap<String, String>(), is);
        assertEquals("CXF", book.getName());
        assertEquals(127L, book.getId());
    }
    
    @Path("/")
    @Ignore
    public static class TheBooks {

        @Path("/books/{bookId}/{new}")
        public Book getNewBook(Book b) {
            return new Book();
        }
        
        @Path("/books/{bookId}/{new}")
        public Book getNewBook2() {
            return new Book();
        }
        
//        @Path("/books/{bookId}/{new}")
//        public List<Book> getNewBook3() {
//            return null;
//        }
        
        @POST
        public void setNewBook(Book b) {
        }
        
        @Path("/books/{bookId}/{new}")
        @POST
        public void setNewBook2(@PathParam("new") String id, Book b) {
        }
    }
    
}
