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
import java.io.StringReader;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.Unmarshaller;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.resources.Book;

import org.junit.Assert;
import org.junit.Test;

public class XSLTJaxbProviderTest extends Assert {
    
    private static final String TEMPLATE_LOCATION = "classpath:/org/apache/cxf/jaxrs/provider/template.xsl";
    private static final String BOOK_XML = "<Book><id>123</id><name>TheBook</name></Book>";
    
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
    
    
    
}
