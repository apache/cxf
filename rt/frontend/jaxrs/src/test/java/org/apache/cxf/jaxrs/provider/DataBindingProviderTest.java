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
import java.util.Collections;

import javax.ws.rs.GET;
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
import org.apache.cxf.jaxrs.resources.sdo.Structure;
import org.apache.cxf.jaxrs.resources.sdo.impl.StructureImpl;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.sdo.SDODataBinding;
import org.apache.cxf.service.Service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class DataBindingProviderTest extends Assert {

    private ClassResourceInfo c;
    private ClassResourceInfo c2;
    
    @Before
    public void setUp() {
        c = ResourceUtils.createClassResourceInfo(TheBooks.class, TheBooks.class, true, true);
        c2 = ResourceUtils.createClassResourceInfo(TheSDOBooks.class, TheSDOBooks.class, true, true);
    }
    
    @Test
    public void testAegisWrite() throws Exception {
        Service s = new JAXRSServiceImpl(Collections.singletonList(c));
        s.put("writeXsiType", true);
        AegisDatabinding binding = new AegisDatabinding();
        binding.initialize(s);
        DataBindingProvider p = new DataBindingProvider(binding);
        Book b = new Book("CXF", 127L);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(b, Book.class, Book.class,
            new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String data = "<ns1:Book xmlns:ns1=\"http://resources.jaxrs.cxf.apache.org\" "
            + "xmlns:ns2=\"http://www.w3.org/2001/XMLSchema-instance\" ns2:type=\"ns1:Book\">"
            + "<ns1:id>127</ns1:id><ns1:name>CXF</ns1:name><ns1:state></ns1:state></ns1:Book>";
        assertEquals(bos.toString(), data);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testAegisRead() throws Exception {
        String data = "<ns1:Book xmlns:ns1=\"http://resources.jaxrs.cxf.apache.org\" "
            + "xmlns:ns2=\"http://www.w3.org/2001/XMLSchema-instance\" ns2:type=\"ns1:Book\">"
            + "<ns1:id>127</ns1:id><ns1:name>CXF</ns1:name><ns1:state></ns1:state></ns1:Book>";
        Service s = new JAXRSServiceImpl(Collections.singletonList(c));
        s.put("readXsiType", true);
        AegisDatabinding binding = new AegisDatabinding();
        binding.initialize(s);
        DataBindingProvider p = new DataBindingProvider(binding);
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Book book = (Book)p.readFrom((Class)Book.class, Book.class,
                                      new Annotation[0], MediaType.APPLICATION_XML_TYPE, 
                                      new MetadataMap<String, String>(), is);
        assertEquals("CXF", book.getName());
        assertEquals(127L, book.getId());
    }
    
    @Test
    public void testJAXBWrite() throws Exception {
        Service s = new JAXRSServiceImpl(Collections.singletonList(c));
        DataBinding binding = new JAXBDataBinding();
        binding.initialize(s);
        DataBindingProvider p = new DataBindingProvider(binding);
        Book b = new Book("CXF", 127L);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(b, Book.class, Book.class,
            new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String data = "<Book><id>127</id><name>CXF</name><state></state></Book>";
        assertEquals(bos.toString(), data);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testJAXBRead() throws Exception {
        String data = "<Book><id>127</id><name>CXF</name><state></state></Book>";
        Service s = new JAXRSServiceImpl(Collections.singletonList(c));
        DataBinding binding = new JAXBDataBinding();
        binding.initialize(s);
        DataBindingProvider p = new DataBindingProvider(binding);
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Book book = (Book)p.readFrom((Class)Book.class, Book.class,
                                      new Annotation[0], MediaType.APPLICATION_XML_TYPE, 
                                      new MetadataMap<String, String>(), is);
        assertEquals("CXF", book.getName());
        assertEquals(127L, book.getId());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testSDOWrite() throws Exception {
        Service s = new JAXRSServiceImpl(Collections.singletonList(c2));
        DataBinding binding = new SDODataBinding();
        binding.initialize(s);
        DataBindingProvider p = new DataBindingProvider(binding);
        Structure struct = new StructureImpl();
        struct.getTexts().add("text1");
        struct.setText("sdo");
        struct.setInt(3);
        struct.setDbl(123.5);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(struct, Structure.class, Structure.class,
            new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String data = "<p0:Structure xmlns:p0=\"http://apache.org/structure/types\" " 
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " 
            + "xsi:type=\"p0:Structure\">"
            + "<p0:text>sdo</p0:text><p0:int>3</p0:int><p0:dbl>123.5</p0:dbl><p0:texts>text1</p0:texts>"
            + "</p0:Structure>";
        assertEquals(bos.toString(), data);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    @Ignore
    public void testSDORead() throws Exception {
        String data = "<p0:Structure xmlns:p0=\"http://apache.org/structure/types\" " 
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " 
            + "xsi:type=\"p0:Structure\">"
            + "<p0:text>sdo</p0:text><p0:int>3</p0:int><p0:dbl>123.5</p0:dbl><p0:texts>text1</p0:texts>"
            + "</p0:Structure>";
        Service s = new JAXRSServiceImpl(Collections.singletonList(c2));
        DataBinding binding = new SDODataBinding();
        binding.initialize(s);
        DataBindingProvider p = new DataBindingProvider(binding);
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Structure struct = (Structure)p.readFrom((Class)Structure.class, Structure.class,
                                      new Annotation[0], MediaType.APPLICATION_XML_TYPE, 
                                      new MetadataMap<String, String>(), is);
        assertEquals("sdo", struct.getText());
        assertEquals(123.5, struct.getDbl());
        assertEquals(3, struct.getInt());
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
        
        @POST
        public void setNewBook(Book b) {
        }
        
        @Path("/books/{bookId}/{new}")
        @POST
        public void setNewBook2(@PathParam("new") String id, Book b) {
        }
    }
    
    @Path("/")
    @Ignore
    public static class TheSDOBooks {

        @GET
        @Path("/books/{bookId}/{new}")
        public Structure getStructure() {
            return null;
        }
        
    }
}
