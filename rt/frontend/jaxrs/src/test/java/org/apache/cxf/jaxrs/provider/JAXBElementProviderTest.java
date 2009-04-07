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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.validation.Schema;

import org.w3c.dom.Node;

import org.xml.sax.ContentHandler;

import org.apache.cxf.jaxrs.impl.MetadataMap;

import org.junit.Assert;
import org.junit.Test;

public class JAXBElementProviderTest extends Assert {

    @Test
    public void testSetSchemasFromClasspath() {
        JAXBElementProvider provider = new JAXBElementProvider();
        List<String> locations = new ArrayList<String>();
        locations.add("classpath:/test.xsd");
        provider.setSchemas(locations);
        Schema s = provider.getSchema();
        assertNotNull("schema can not be read from classpath", s);
    }
    
    @Test
    public void testSetSchemasFromDisk() {
        JAXBElementProvider provider = new JAXBElementProvider();
        List<String> locations = new ArrayList<String>();
        String loc = getClass().getClassLoader().getResource("test.xsd").getFile();
        locations.add(loc);
        provider.setSchemas(locations);
        Schema s = provider.getSchema();
        assertNotNull("schema can not be read from disk", s);
    }
    
    @Test 
    public void testPackageContext() {
        JAXBElementProvider p = new JAXBElementProvider();
        try {
            JAXBContext context = p.getPackageContext(org.apache.cxf.jaxrs.fortest.jaxb.Book.class);
            JAXBContext context2 = p.getPackageContext(org.apache.cxf.jaxrs.fortest.jaxb.Book.class);
            assertNotNull(context);
            assertNotNull(context2);
            assertSame(context, context2);
        } finally {
            JAXBElementProvider.clearContexts();
        }
        
    }
    
    @Test
    public void testSetMarshallProperties() throws Exception {
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        props.put(Marshaller.JAXB_SCHEMA_LOCATION, "foo.xsd");
        
        final TestMarshaller m = new TestMarshaller();
        
        JAXBElementProvider provider = new JAXBElementProvider() {
            @Override
            protected Marshaller createMarshaller(Object obj, Class<?> cls, Type genericType, String enc)
                throws JAXBException {
                return m;    
            }
        };
        
        provider.setMarshallerProperties(props);
        provider.writeTo("123", String.class, (Type)String.class, new Annotation[]{}, 
                         MediaType.APPLICATION_XML_TYPE, new MetadataMap<String, Object>(), 
                         new ByteArrayOutputStream());
        
        assertEquals("Marshall properties have not been set", props, m.getProperties());
    }
    
    private static class TestMarshaller implements Marshaller {

        private Map<String, Object> props = new HashMap<String, Object>();
        
        public TestMarshaller() {
            
        }
        
        public Map getProperties() {
            return props;
        }
        
        public <A extends XmlAdapter> A getAdapter(Class<A> type) {
            // TODO Auto-generated method stub
            return null;
        }

        public AttachmentMarshaller getAttachmentMarshaller() {
            // TODO Auto-generated method stub
            return null;
        }

        public ValidationEventHandler getEventHandler() throws JAXBException {
            // TODO Auto-generated method stub
            return null;
        }

        public Listener getListener() {
            // TODO Auto-generated method stub
            return null;
        }

        public Node getNode(Object contentTree) throws JAXBException {
            // TODO Auto-generated method stub
            return null;
        }

        public Object getProperty(String name) throws PropertyException {
            // TODO Auto-generated method stub
            return null;
        }

        public Schema getSchema() {
            // TODO Auto-generated method stub
            return null;
        }

        public void marshal(Object jaxbElement, Result result) throws JAXBException {
            // TODO Auto-generated method stub
            
        }

        public void marshal(Object jaxbElement, OutputStream os) throws JAXBException {
        }

        public void marshal(Object jaxbElement, File output) throws JAXBException {
            // TODO Auto-generated method stub
            
        }

        public void marshal(Object jaxbElement, Writer writer) throws JAXBException {
            // TODO Auto-generated method stub
            
        }

        public void marshal(Object jaxbElement, ContentHandler handler) throws JAXBException {
            // TODO Auto-generated method stub
            
        }

        public void marshal(Object jaxbElement, Node node) throws JAXBException {
            // TODO Auto-generated method stub
            
        }

        public void marshal(Object jaxbElement, XMLStreamWriter writer) throws JAXBException {
            // TODO Auto-generated method stub
            
        }

        public void marshal(Object jaxbElement, XMLEventWriter writer) throws JAXBException {
            // TODO Auto-generated method stub
            
        }

        public void setAdapter(XmlAdapter adapter) {
            // TODO Auto-generated method stub
            
        }

        public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter) {
            // TODO Auto-generated method stub
            
        }

        public void setAttachmentMarshaller(AttachmentMarshaller am) {
            // TODO Auto-generated method stub
            
        }

        public void setEventHandler(ValidationEventHandler handler) throws JAXBException {
            // TODO Auto-generated method stub
            
        }

        public void setListener(Listener listener) {
            // TODO Auto-generated method stub
            
        }

        public void setProperty(String name, Object value) throws PropertyException {
            props.put(name, value);
            
        }

        public void setSchema(Schema schema) {
            // TODO Auto-generated method stub
            
        }
        
    }
    
}
