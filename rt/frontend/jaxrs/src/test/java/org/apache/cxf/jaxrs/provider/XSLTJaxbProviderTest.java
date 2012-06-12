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

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.resources.SuperBook;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.staxutils.StaxUtils;
import org.easymock.EasyMock;

import org.junit.Assert;
import org.junit.Test;

public class XSLTJaxbProviderTest extends Assert {
    
    private static final String TEMPLATE_LOCATION = "classpath:/org/apache/cxf/jaxrs/provider/template.xsl";
    private static final String BOOK_XML = "<Book><id>123</id><name>TheBook</name></Book>";
    
    
    @Test
    public void testIsWriteable() throws Exception {
        XSLTJaxbProvider<Book> provider = new XSLTJaxbProvider<Book>();
        provider.setOutTemplate(TEMPLATE_LOCATION);
        provider.isWriteable(Book.class, Book.class, null, MediaType.APPLICATION_XML_TYPE);
    }
    
    @Test
    public void testIsWriteableWithSetClasses() throws Exception {
        XSLTJaxbProvider<Book> provider = new XSLTJaxbProvider<Book>();
        provider.setOutTemplate(TEMPLATE_LOCATION);
        List<String> names = new ArrayList<String>();
        names.add(Book.class.getName());
        provider.setOutClassNames(names);
        provider.isWriteable(Book.class, Book.class, null, MediaType.APPLICATION_XML_TYPE);
    }
    
    @Test
    public void testNotWriteableWithSetClasses() throws Exception {
        XSLTJaxbProvider<SuperBook> provider = new XSLTJaxbProvider<SuperBook>();
        provider.setOutTemplate(TEMPLATE_LOCATION);
        List<String> names = new ArrayList<String>();
        names.add(Book.class.getName());
        provider.setOutClassNames(names);
        provider.isWriteable(SuperBook.class, SuperBook.class, null, MediaType.APPLICATION_XML_TYPE);
    }
    
    @Test
    public void testIsWriteableWithSetClassesAndJaxbOnly() throws Exception {
        XSLTJaxbProvider<SuperBook> provider = new XSLTJaxbProvider<SuperBook>();
        provider.setSupportJaxbOnly(true);
        provider.setOutTemplate(TEMPLATE_LOCATION);
        List<String> names = new ArrayList<String>();
        names.add(Book.class.getName());
        provider.setOutClassNames(names);
        provider.isWriteable(SuperBook.class, SuperBook.class, null, MediaType.APPLICATION_XML_TYPE);
    }
    
    @Test
    public void testWrite() throws Exception {
        XSLTJaxbProvider<Book> provider = new XSLTJaxbProvider<Book>();
        provider.setOutTemplate(TEMPLATE_LOCATION);
        provider.setMessageContext(new MessageContextImpl(createMessage()));
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
        XSLTJaxbProvider<Book> provider = new XSLTJaxbProvider<Book>() {
            @Override
            protected XMLStreamWriter getStreamWriter(Object obj, OutputStream os, MediaType mt) {
                return StaxUtils.createXMLStreamWriter(os);
            }
        };
        provider.setOutTemplate(TEMPLATE_LOCATION);
        provider.setMessageContext(new MessageContextImpl(createMessage()));
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
        XSLTJaxbProvider<Book> provider = new XSLTJaxbProvider<Book>();
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
        XSLTJaxbProvider<Book> provider = new XSLTJaxbProvider<Book>();
        provider.setInTemplate(TEMPLATE_LOCATION);
        provider.isReadable(Book.class, Book.class, null, MediaType.APPLICATION_XML_TYPE);
    }
    
    @Test
    public void testIsReadableWithSetClasses() throws Exception {
        XSLTJaxbProvider<Book> provider = new XSLTJaxbProvider<Book>();
        provider.setInTemplate(TEMPLATE_LOCATION);
        List<String> names = new ArrayList<String>();
        names.add(Book.class.getName());
        provider.setInClassNames(names);
        provider.isReadable(Book.class, Book.class, null, MediaType.APPLICATION_XML_TYPE);
    }
    
    @Test
    public void testNotReadableWithSetClasses() throws Exception {
        XSLTJaxbProvider<SuperBook> provider = new XSLTJaxbProvider<SuperBook>();
        provider.setInTemplate(TEMPLATE_LOCATION);
        List<String> names = new ArrayList<String>();
        names.add(Book.class.getName());
        provider.setInClassNames(names);
        provider.isReadable(SuperBook.class, SuperBook.class, null, MediaType.APPLICATION_XML_TYPE);
    }
    
    @Test
    public void testIsReadableWithSetClassesAndJaxbOnly() throws Exception {
        XSLTJaxbProvider<SuperBook> provider = new XSLTJaxbProvider<SuperBook>();
        provider.setSupportJaxbOnly(true);
        provider.setInTemplate(TEMPLATE_LOCATION);
        List<String> names = new ArrayList<String>();
        names.add(Book.class.getName());
        provider.setInClassNames(names);
        provider.isReadable(SuperBook.class, SuperBook.class, null, MediaType.APPLICATION_XML_TYPE);
    }
    
    @Test
    public void testRead() throws Exception {
        XSLTJaxbProvider<Book> provider = new XSLTJaxbProvider<Book>();
        provider.setInTemplate(TEMPLATE_LOCATION);
        
        Book b = new Book();
        b.setId(123L);
        b.setName("TheBook");
        Book b2 = provider.readFrom(Book.class, Book.class, b.getClass().getAnnotations(),
                          MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(),
                          new ByteArrayInputStream(BOOK_XML.getBytes()));
        b.setName("TheBook2");
        assertEquals("Transformation is bad", b, b2);
    }
    
    @Test
    public void testReadFromStreamReader() throws Exception {
        XSLTJaxbProvider<Book> provider = new XSLTJaxbProvider<Book>() {
            @Override
            protected XMLStreamReader getStreamReader(InputStream is, Class<?> type, MediaType mt) {
                return StaxUtils.createXMLStreamReader(is);
            }
        };
        provider.setInTemplate(TEMPLATE_LOCATION);
        
        Book b = new Book();
        b.setId(123L);
        b.setName("TheBook");
        Book b2 = provider.readFrom(Book.class, Book.class, b.getClass().getAnnotations(),
                          MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(),
                          new ByteArrayInputStream(BOOK_XML.getBytes()));
        b.setName("TheBook2");
        assertEquals("Transformation is bad", b, b2);
    }
    
    @Test
    public void testReadWithoutTemplate() throws Exception {
        XSLTJaxbProvider<Book> provider = new XSLTJaxbProvider<Book>();
        provider.setSupportJaxbOnly(true);
        
        Book b = new Book();
        b.setId(123L);
        b.setName("TheBook");
        Book b2 = provider.readFrom(Book.class, Book.class, b.getClass().getAnnotations(),
                          MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(),
                          new ByteArrayInputStream(BOOK_XML.getBytes()));
        assertEquals("Transformation is bad", b, b2);
    }
    
    private Message createMessage() {
        ProviderFactory factory = ProviderFactory.getInstance();
        Message m = new MessageImpl();
        m.put(Message.ENDPOINT_ADDRESS, "http://localhost:8080/bar");
        m.put("org.apache.cxf.http.case_insensitive_queries", false);
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        Endpoint endpoint = EasyMock.createMock(Endpoint.class);
        endpoint.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(null).anyTimes();
        endpoint.get(Application.class.getName());
        EasyMock.expectLastCall().andReturn(null);
        endpoint.size();
        EasyMock.expectLastCall().andReturn(0).anyTimes();
        endpoint.isEmpty();
        EasyMock.expectLastCall().andReturn(true).anyTimes();
        endpoint.get(ProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(factory).anyTimes();
        EasyMock.replay(endpoint);
        e.put(Endpoint.class, endpoint);
        return m;
    }
}
