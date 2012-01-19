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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.resources.SuperBook;
import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Assert;
import org.junit.Test;

public class XSLTJaxbProviderTest extends Assert {
    
    private static final String TEMPLATE_LOCATION = "classpath:/org/apache/cxf/jaxrs/provider/template.xsl";
    private static final String BOOK_XML = "<Book><id>123</id><name>TheBook</name></Book>";
    
    
    @Test
    public void testIsWriteable() throws Exception {
        XSLTJaxbProvider provider = new XSLTJaxbProvider();
        provider.setOutTemplate(TEMPLATE_LOCATION);
        provider.isWriteable(Book.class, Book.class, null, MediaType.APPLICATION_XML_TYPE);
    }
    
    @Test
    public void testIsWriteableWithSetClasses() throws Exception {
        XSLTJaxbProvider provider = new XSLTJaxbProvider();
        provider.setOutTemplate(TEMPLATE_LOCATION);
        List<String> names = new ArrayList<String>();
        names.add(Book.class.getName());
        provider.setOutClassNames(names);
        provider.isWriteable(Book.class, Book.class, null, MediaType.APPLICATION_XML_TYPE);
    }
    
    @Test
    public void testNotWriteableWithSetClasses() throws Exception {
        XSLTJaxbProvider provider = new XSLTJaxbProvider();
        provider.setOutTemplate(TEMPLATE_LOCATION);
        List<String> names = new ArrayList<String>();
        names.add(Book.class.getName());
        provider.setOutClassNames(names);
        provider.isWriteable(SuperBook.class, SuperBook.class, null, MediaType.APPLICATION_XML_TYPE);
    }
    
    @Test
    public void testIsWriteableWithSetClassesAndJaxbOnly() throws Exception {
        XSLTJaxbProvider provider = new XSLTJaxbProvider();
        provider.setSupportJaxbOnly(true);
        provider.setOutTemplate(TEMPLATE_LOCATION);
        List<String> names = new ArrayList<String>();
        names.add(Book.class.getName());
        provider.setOutClassNames(names);
        provider.isWriteable(SuperBook.class, SuperBook.class, null, MediaType.APPLICATION_XML_TYPE);
    }
    
    @Test
    public void testWrite() throws Exception {
        XSLTJaxbProvider provider = new XSLTJaxbProvider();
        provider.setOutTemplate(TEMPLATE_LOCATION);
        
        Book b = new Book();
        b.setId(123L);
        b.setName("TheBook");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(b, Book.class, Book.class, b.getClass().getAnnotations(),
                         MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        Unmarshaller um = provider.getClassContext(Book.class).createUnmarshaller();
        Book b2 = (Book)um.unmarshal(new StringReader(bos.toString()));
        b.setName("TheBook2");
        assertEquals("Transformation is bad", b, b2);
    }
    
    @Test
    public void testWriteToStreamWriter() throws Exception {
        XSLTJaxbProvider provider = new XSLTJaxbProvider() {
            @Override
            protected XMLStreamWriter getStreamWriter(Object obj, OutputStream os, MediaType mt) {
                return StaxUtils.createXMLStreamWriter(os);
            }
        };
        provider.setOutTemplate(TEMPLATE_LOCATION);
        
        Book b = new Book();
        b.setId(123L);
        b.setName("TheBook");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(b, Book.class, Book.class, b.getClass().getAnnotations(),
                         MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        Unmarshaller um = provider.getClassContext(Book.class).createUnmarshaller();
        Book b2 = (Book)um.unmarshal(new StringReader(bos.toString()));
        b.setName("TheBook2");
        assertEquals("Transformation is bad", b, b2);
    }
    
    @Test
    public void testWriteWithoutTemplate() throws Exception {
        XSLTJaxbProvider provider = new XSLTJaxbProvider();
        provider.setSupportJaxbOnly(true);
        
        Book b = new Book();
        b.setId(123L);
        b.setName("TheBook");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(b, Book.class, Book.class, b.getClass().getAnnotations(),
                         MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        Unmarshaller um = provider.getClassContext(Book.class).createUnmarshaller();
        Book b2 = (Book)um.unmarshal(new StringReader(bos.toString()));
        assertEquals(b, b2);
    }
    
    @Test
    public void testIsReadable() throws Exception {
        XSLTJaxbProvider provider = new XSLTJaxbProvider();
        provider.setInTemplate(TEMPLATE_LOCATION);
        provider.isReadable(Book.class, Book.class, null, MediaType.APPLICATION_XML_TYPE);
    }
    
    @Test
    public void testIsReadableWithSetClasses() throws Exception {
        XSLTJaxbProvider provider = new XSLTJaxbProvider();
        provider.setInTemplate(TEMPLATE_LOCATION);
        List<String> names = new ArrayList<String>();
        names.add(Book.class.getName());
        provider.setInClassNames(names);
        provider.isReadable(Book.class, Book.class, null, MediaType.APPLICATION_XML_TYPE);
    }
    
    @Test
    public void testNotReadableWithSetClasses() throws Exception {
        XSLTJaxbProvider provider = new XSLTJaxbProvider();
        provider.setInTemplate(TEMPLATE_LOCATION);
        List<String> names = new ArrayList<String>();
        names.add(Book.class.getName());
        provider.setInClassNames(names);
        provider.isReadable(SuperBook.class, SuperBook.class, null, MediaType.APPLICATION_XML_TYPE);
    }
    
    @Test
    public void testIsReadableWithSetClassesAndJaxbOnly() throws Exception {
        XSLTJaxbProvider provider = new XSLTJaxbProvider();
        provider.setSupportJaxbOnly(true);
        provider.setInTemplate(TEMPLATE_LOCATION);
        List<String> names = new ArrayList<String>();
        names.add(Book.class.getName());
        provider.setInClassNames(names);
        provider.isReadable(SuperBook.class, SuperBook.class, null, MediaType.APPLICATION_XML_TYPE);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testRead() throws Exception {
        XSLTJaxbProvider provider = new XSLTJaxbProvider();
        provider.setInTemplate(TEMPLATE_LOCATION);
        
        Book b = new Book();
        b.setId(123L);
        b.setName("TheBook");
        Book b2 = (Book)provider.readFrom((Class)Book.class, Book.class, b.getClass().getAnnotations(),
                          MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(),
                          new ByteArrayInputStream(BOOK_XML.getBytes()));
        b.setName("TheBook2");
        assertEquals("Transformation is bad", b, b2);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testReadFromStreamReader() throws Exception {
        XSLTJaxbProvider provider = new XSLTJaxbProvider() {
            @Override
            protected XMLStreamReader getStreamReader(InputStream is, Class<?> type, MediaType mt) {
                return StaxUtils.createXMLStreamReader(is);
            }
        };
        provider.setInTemplate(TEMPLATE_LOCATION);
        
        Book b = new Book();
        b.setId(123L);
        b.setName("TheBook");
        Book b2 = (Book)provider.readFrom((Class)Book.class, Book.class, b.getClass().getAnnotations(),
                          MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(),
                          new ByteArrayInputStream(BOOK_XML.getBytes()));
        b.setName("TheBook2");
        assertEquals("Transformation is bad", b, b2);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testReadWithoutTemplate() throws Exception {
        XSLTJaxbProvider provider = new XSLTJaxbProvider();
        provider.setSupportJaxbOnly(true);
        
        Book b = new Book();
        b.setId(123L);
        b.setName("TheBook");
        Book b2 = (Book)provider.readFrom((Class)Book.class, Book.class, b.getClass().getAnnotations(),
                          MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(),
                          new ByteArrayInputStream(BOOK_XML.getBytes()));
        assertEquals("Transformation is bad", b, b2);
    }
    
}
