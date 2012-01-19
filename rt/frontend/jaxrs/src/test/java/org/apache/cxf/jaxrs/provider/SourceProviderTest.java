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
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.util.Collections;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.staxutils.StaxSource;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.transform.InTransformReader;

import org.junit.Assert;
import org.junit.Test;

public class SourceProviderTest extends Assert {
    
       
    @Test
    public void testIsWriteable() {
        SourceProvider p = new SourceProvider();
        assertTrue(p.isWriteable(StreamSource.class, null, null, null)
                   && p.isWriteable(DOMSource.class, null, null, null)
                   && p.isWriteable(Source.class, null, null, null));
    }
    
    @Test
    public void testIsReadable() {
        SourceProvider p = new SourceProvider();
        assertTrue(p.isReadable(StreamSource.class, null, null, null)
                   && p.isReadable(DOMSource.class, null, null, null)
                   && p.isReadable(Source.class, null, null, null));
    }

    @Test
    public void testReadFrom() throws Exception {
        SourceProvider p = new TestSourceProvider();
        assertSame(StreamSource.class, verifyRead(p, StreamSource.class).getClass());
        assertSame(StreamSource.class, verifyRead(p, Source.class).getClass());
        assertSame(StaxSource.class, verifyRead(p, SAXSource.class).getClass());
        assertSame(StaxSource.class, verifyRead(p, StaxSource.class).getClass());
        assertSame(DOMSource.class, verifyRead(p, DOMSource.class).getClass());
        assertTrue(Document.class.isAssignableFrom(verifyRead(p, Document.class).getClass()));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testReadFromStreamReader() throws Exception {
        TestSourceProvider p = new TestSourceProvider();
        
        InputStream is = new ByteArrayInputStream("<test xmlns=\"http://bar\"/>".getBytes());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        reader = new InTransformReader(reader, 
                                       Collections.singletonMap("{http://bar}test", "test2"),
                                       null,
                                       null,
                                       null,
                                       false);
        
        p.getMessage().setContent(XMLStreamReader.class, reader);
        
        Source source = (Source)p.readFrom((Class)Source.class,
                   null, null, null, null, is);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        TransformerFactory.newInstance().newTransformer()
            .transform(source, new StreamResult(bos));
        assertTrue(bos.toString().contains("test2"));
    }
    
    @Test
    public void testWriteToDocument() throws Exception {
        SourceProvider p = new SourceProvider();
        
        Document doc = DOMUtils.readXml(new StringReader("<test/>"));
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(doc, Document.class, Document.class, 
                  new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE, 
                  new MetadataMap<String, Object>(), os);
        String s = os.toString();
        assertEquals("<test/>", s);
           
    }
    
    @Test
    public void testReadFromWithPreferredFormat() throws Exception {
        TestSourceProvider p = new TestSourceProvider();
        p.getMessage().put("source-preferred-format", "sax");        
        assertSame(StaxSource.class, verifyRead(p, Source.class).getClass());
    }
    
    @Test
    public void testWriteTo() throws Exception {
        SourceProvider p = new TestSourceProvider();
        StreamSource s = new StreamSource(new ByteArrayInputStream("<test/>".getBytes()));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        p.writeTo(s, null, null, null, MediaType.APPLICATION_XML_TYPE, 
                  new MetadataMap<String, Object>(), os);
        assertTrue(os.toString().contains("<test/>"));
        os = new ByteArrayOutputStream();
        p.writeTo(createDomSource(), null, null, null, MediaType.APPLICATION_XML_TYPE, 
                  new MetadataMap<String, Object>(), os);
        assertTrue(os.toString().contains("<test/>"));
    }
    
    @SuppressWarnings("unchecked")
    private <T> T verifyRead(MessageBodyReader p, Class<T> type) throws Exception {
        return (T)p.readFrom(type,
                   null, null, null, null,
                   new ByteArrayInputStream("<test/>".getBytes()));
    }
    
    private DOMSource createDomSource() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        builder = factory.newDocumentBuilder();
        return new DOMSource(builder.parse(new ByteArrayInputStream("<test/>".getBytes())));
    }
    
    private static class TestSourceProvider extends SourceProvider {
        
        private Message m = new MessageImpl();
        
        public TestSourceProvider() {
        }
        
        public Message getMessage() {
            return m;
        }
        
        protected MessageContext getContext() {
            return new MessageContextImpl(m);
        };
    }
}
