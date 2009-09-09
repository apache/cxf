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
import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.validation.Schema;

import org.w3c.dom.Node;

import org.xml.sax.ContentHandler;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.resources.CollectionsResource;
import org.apache.cxf.jaxrs.resources.SuperBook;
import org.apache.cxf.jaxrs.resources.TagVO2;

import org.junit.Assert;
import org.junit.Test;

public class JAXBElementProviderTest extends Assert {

    @Test
    public void testIsWriteableList() throws Exception {
        testIsWriteableCollection("getBooks");
    }
    
    @Test
    public void testIsWriteableCollection() throws Exception {
        testIsWriteableCollection("getBookCollection");
    }
    
    @Test
    public void testIsWriteableSet() throws Exception {
        testIsWriteableCollection("getBookSet");
    }
    
    @Test
    public void testIsWriteableJAXBElements() throws Exception {
        testIsWriteableCollection("getBookElements");
    }
    
    private void testIsWriteableCollection(String mName) throws Exception {
        JAXBElementProvider provider = new JAXBElementProvider();
        provider.setCollectionWrapperName("foo");
        Method m = CollectionsResource.class.getMethod(mName, new Class[0]);
        assertTrue(provider.isWriteable(m.getReturnType(), m.getGenericReturnType(),
                             new Annotation[0], MediaType.TEXT_XML_TYPE));
    }
    
    @Test
    public void testWriteCollection() throws Exception {
        doWriteUnqualifiedCollection(true, "getBookCollection", "setBookCollection", 
                                     Collection.class);
    }
    
    @Test
    public void testWriteList() throws Exception {
        doWriteUnqualifiedCollection(true, "getBooks", "setBooks", List.class);
    }
    
    @Test
    public void testWriteSet() throws Exception {
        doWriteUnqualifiedCollection(true, "getBookSet", "setBookSet", Set.class);
    }
    
    @Test
    public void testWriteCollectionJaxbName() throws Exception {
        doWriteUnqualifiedCollection(false, "getBooks", "setBooks", List.class);
    }
    
    @Test
    public void testWriteArray() throws Exception {
        doWriteUnqualifiedCollection(true, "getBooksArray", "setBooksArray", Book[].class);
    }
    
    public void doWriteUnqualifiedCollection(boolean setName, String mName, String setterName, 
                                             Class<?> type) throws Exception {
        JAXBElementProvider provider = new JAXBElementProvider();
        if (setName) {
            provider.setCollectionWrapperName("Books");
        }
        List<Book> books = new ArrayList<Book>();
        books.add(new Book("CXF in Action", 123L));
        books.add(new Book("CXF Rocks", 124L));
        Object o = type.isArray() ? books.toArray() : type == Set.class 
            ? new HashSet<Book>(books) : books;
        
        Method m = CollectionsResource.class.getMethod(mName, new Class[0]);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(o, m.getReturnType(), m.getGenericReturnType(),
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        doReadUnqualifiedCollection(bos.toString(), setterName, type);
    }
    
    @Test
    public void testWriteJAXBElementCollection() throws Exception {
        doTestWriteJAXBCollection("getBookElements");
    }
    
    @Test
    public void testWriteJAXBElementCollection2() throws Exception {
        doTestWriteJAXBCollection("getBookElements2");
    }
    
    @Test
    public void testWriteDerivedType() throws Exception {
        JAXBElementProvider provider = new JAXBElementProvider();
        provider.setJaxbElementClassNames(Collections.singletonList(Book.class.getName()));
        Book b = new SuperBook("CXF in Action", 123L, 124L);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(b, Book.class, Book.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        readSuperBook(bos.toString(), true);
    }
    
    @Test
    public void testWriteDerivedType2() throws Exception {
        JAXBElementProvider provider = new JAXBElementProvider();
        Book b = new SuperBook("CXF in Action", 123L, 124L);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(b, Book.class, Book.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        
        readSuperBook(bos.toString(), false);
    }
    
    @Test
    public void testWriteWithoutXmlRootElement() throws Exception {
        doTestWriteWithoutXmlRootElement("SuperBook", false, false);
    }
    
    @Test
    public void testWriteWithoutXmlRootElement2() throws Exception {
        doTestWriteWithoutXmlRootElement("SuperBook", true, false);
    }
    
    @Test
    public void testWriteWithoutXmlRootElement3() throws Exception {
        doTestWriteWithoutXmlRootElement("{http://books}SuperBook", false, false);
    }
    
    @Test
    public void testWriteWithoutXmlRootElement4() throws Exception {
        doTestWriteWithoutXmlRootElement("SuperBook", true, true);
    }
    
    public void doTestWriteWithoutXmlRootElement(String name, boolean unmarshalAsJaxbElement,
                                                 boolean marshalAsJaxbElement) 
        throws Exception {
        JAXBElementProvider provider = new JAXBElementProvider();
        if (!marshalAsJaxbElement) {
            provider.setJaxbElementClassMap(Collections.singletonMap(
                org.apache.cxf.jaxrs.fortest.jaxb.SuperBook.class.getName(), 
                name));
        } else {
            provider.setMarshallAsJaxbElement(marshalAsJaxbElement);
        }
        org.apache.cxf.jaxrs.fortest.jaxb.SuperBook b = 
            new org.apache.cxf.jaxrs.fortest.jaxb.SuperBook("CXF in Action", 123L, 124L);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(b, org.apache.cxf.jaxrs.fortest.jaxb.SuperBook.class, 
                         org.apache.cxf.jaxrs.fortest.jaxb.SuperBook.class,
                         new Annotation[0], MediaType.TEXT_XML_TYPE, 
                         new MetadataMap<String, Object>(), bos);
        readSuperBook2(bos.toString(), unmarshalAsJaxbElement);
    }
    
    @Test
    public void testWriteCollectionWithoutXmlRootElement() 
        throws Exception {
        JAXBElementProvider provider = new JAXBElementProvider();
        provider.setCollectionWrapperName("{http://superbooks}SuperBooks");
        provider.setJaxbElementClassMap(Collections.singletonMap(
                org.apache.cxf.jaxrs.fortest.jaxb.SuperBook.class.getName(), 
                "{http://superbooks}SuperBook"));
        org.apache.cxf.jaxrs.fortest.jaxb.SuperBook b = 
            new org.apache.cxf.jaxrs.fortest.jaxb.SuperBook("CXF in Action", 123L, 124L);
        List<org.apache.cxf.jaxrs.fortest.jaxb.SuperBook> books = 
            Collections.singletonList(b);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(books, List.class, 
                         org.apache.cxf.jaxrs.fortest.jaxb.SuperBook.class,
                         new Annotation[0], MediaType.TEXT_XML_TYPE, 
                         new MetadataMap<String, Object>(), bos);
        String expected = "<ns1:SuperBooks xmlns:ns1=\"http://superbooks\">"
            + "<ns1:SuperBook xmlns:ns2=\"http://books\" xmlns:ns1=\"http://superbooks\"><id>123</id>"
            + "<name>CXF in Action</name><superId>124</superId></ns1:SuperBook></ns1:SuperBooks>";
        assertEquals(expected, bos.toString());
    }
    
    
    @Test
    public void testWriteWithoutXmlRootElementDerived() throws Exception {
        JAXBElementProvider provider = new JAXBElementProvider();
        provider.setJaxbElementClassMap(Collections.singletonMap(
            org.apache.cxf.jaxrs.fortest.jaxb.Book.class.getName(), "Book"));
        org.apache.cxf.jaxrs.fortest.jaxb.Book b = 
            new org.apache.cxf.jaxrs.fortest.jaxb.SuperBook("CXF in Action", 123L, 124L);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(b, org.apache.cxf.jaxrs.fortest.jaxb.Book.class, 
                         org.apache.cxf.jaxrs.fortest.jaxb.Book.class,
                         new Annotation[0], MediaType.TEXT_XML_TYPE, 
                         new MetadataMap<String, Object>(), bos);
        readSuperBook2(bos.toString(), false);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testWriteWithoutXmlRootElementObjectFactory() throws Exception {
        JAXBElementProvider provider = new JAXBElementProvider();
        provider.setJaxbElementClassMap(Collections.singletonMap(
            org.apache.cxf.jaxrs.fortest.jaxb.SuperBook2.class.getName(), 
            "{http://books}SuperBook2"));
        org.apache.cxf.jaxrs.fortest.jaxb.SuperBook2 b = 
            new org.apache.cxf.jaxrs.fortest.jaxb.SuperBook2("CXF in Action", 123L, 124L);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(b, org.apache.cxf.jaxrs.fortest.jaxb.SuperBook2.class, 
                         org.apache.cxf.jaxrs.fortest.jaxb.SuperBook2.class,
                         new Annotation[0], MediaType.TEXT_XML_TYPE, 
                         new MetadataMap<String, Object>(), bos);
        JAXBElementProvider provider2 = new JAXBElementProvider();
        ByteArrayInputStream is = new ByteArrayInputStream(bos.toByteArray());
        org.apache.cxf.jaxrs.fortest.jaxb.SuperBook2 book = 
            (org.apache.cxf.jaxrs.fortest.jaxb.SuperBook2)provider2.readFrom(
                       (Class)org.apache.cxf.jaxrs.fortest.jaxb.SuperBook2.class, 
                       org.apache.cxf.jaxrs.fortest.jaxb.SuperBook2.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        assertEquals(124L, book.getSuperId());
    }
    
    @SuppressWarnings("unchecked")
    private void readSuperBook(String data, boolean xsiTypeExpected) throws Exception {
        if (xsiTypeExpected) {
            assertTrue(data.contains("xsi:type"));
        }
        JAXBElementProvider provider = new JAXBElementProvider();
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        SuperBook book = (SuperBook)provider.readFrom(
                       (Class)SuperBook.class, SuperBook.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        assertEquals(124L, book.getSuperId());
    }
    
    @SuppressWarnings("unchecked")
    private void readSuperBook2(String data, boolean unmarshalAsJaxbElement) throws Exception {
        JAXBElementProvider provider = new JAXBElementProvider();
        if (!unmarshalAsJaxbElement) {
            provider.setJaxbElementClassMap(Collections.singletonMap(
                org.apache.cxf.jaxrs.fortest.jaxb.SuperBook.class.getName(), "SuperBook"));
        } else {
            provider.setUnmarshallAsJaxbElement(true);
        }
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        org.apache.cxf.jaxrs.fortest.jaxb.SuperBook book = 
            (org.apache.cxf.jaxrs.fortest.jaxb.SuperBook)provider.readFrom(
                       (Class)org.apache.cxf.jaxrs.fortest.jaxb.SuperBook.class, 
                       org.apache.cxf.jaxrs.fortest.jaxb.SuperBook.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        assertEquals(124L, book.getSuperId());
    }
    
    private void doTestWriteJAXBCollection(String mName) throws Exception {
        JAXBElementProvider provider = new JAXBElementProvider();
        List<JAXBElement<Book>> books = new ArrayList<JAXBElement<Book>>();
        books.add(new JAXBElement<Book>(new QName("Books"), Book.class, null, 
            new Book("CXF in Action", 123L)));
        books.add(new JAXBElement<Book>(new QName("Books"), Book.class, null, 
            new Book("CXF Rocks", 124L)));
        
        Method m = CollectionsResource.class.getMethod(mName, new Class[0]);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(books, m.getReturnType(), m.getGenericReturnType(),
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        doReadUnqualifiedCollection(bos.toString(), "setBooks", List.class);
    }
    
    @Test
    public void testWriteQualifiedCollection() throws Exception {
        JAXBElementProvider provider = new JAXBElementProvider();
        provider.setCollectionWrapperName("{http://tags}tags");
        List<TagVO2> tags = new ArrayList<TagVO2>();
        tags.add(new TagVO2("A", "B"));
        tags.add(new TagVO2("C", "D"));
        Method m = CollectionsResource.class.getMethod("getTags", new Class[0]);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tags, m.getReturnType(), m.getGenericReturnType(),
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        doReadQualifiedCollection(bos.toString(), false);
    }
    
    @Test
    public void testReadUnqualifiedCollection() throws Exception {
        String data = "<Books><Book><id>123</id><name>CXF in Action</name>"
            + "</Book><Book><id>124</id><name>CXF Rocks</name></Book></Books>";
        doReadUnqualifiedCollection(data, "setBooks", List.class);
    }
    
    @SuppressWarnings("unchecked")
    private void doReadUnqualifiedCollection(String data, String mName, Class<?> type) throws Exception {
        JAXBElementProvider provider = new JAXBElementProvider();
        Method m = CollectionsResource.class.getMethod(mName, 
                                                       new Class[]{type});
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(
                      (Class)m.getParameterTypes()[0], m.getGenericParameterTypes()[0],
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        assertNotNull(o);
        Book b1 = null;
        Book b2 = null;
        if (type.isArray()) {
            assertEquals(2, ((Book[])o).length);
            b1 = ((Book[])o)[0];
            b2 = ((Book[])o)[1];
        } else if (type == Set.class) {
            Set<Book> set = (Set)o;
            List<Book> books = new ArrayList<Book>(new TreeSet<Book>(set));
            b1 = books.get(0);
            b2 = books.get(1);
        } else {
            List<Book> books = (List<Book>)o;
            b1 = books.get(0);
            b2 = books.get(1);
        }
        
        assertEquals(123, b1.getId());
        assertEquals("CXF in Action", b1.getName());
        
        assertEquals(124, b2.getId());
        assertEquals("CXF Rocks", b2.getName());    
    }
    
    
    @Test
    public void testReadQualifiedCollection() throws Exception {
        String data = "<ns1:tags xmlns:ns1=\"http://tags\"><ns1:thetag><group>B</group><name>A</name>"
            + "</ns1:thetag><ns1:thetag><group>D</group><name>C</name></ns1:thetag></ns1:tags>";
        doReadQualifiedCollection(data, false);
    }
    
    @Test
    public void testReadQualifiedArray() throws Exception {
        String data = "<ns1:tags xmlns:ns1=\"http://tags\"><ns1:thetag><group>B</group><name>A</name>"
            + "</ns1:thetag><ns1:thetag><group>D</group><name>C</name></ns1:thetag></ns1:tags>";
        doReadQualifiedCollection(data, true);
    }
    
    @SuppressWarnings("unchecked")
    public void doReadQualifiedCollection(String data, boolean isArray) throws Exception {
        JAXBElementProvider provider = new JAXBElementProvider();
        Method m = null;
        if (!isArray) {
            m = CollectionsResource.class.getMethod("setTags", new Class[]{List.class});
        } else {
            m = CollectionsResource.class.getMethod("setTagsArray", new Class[]{TagVO2[].class});
        }
        
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(
                      (Class)m.getParameterTypes()[0], m.getGenericParameterTypes()[0],
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        assertNotNull(o);
        TagVO2 t1 = null;
        TagVO2 t2 = null;
        if (!isArray) {
            assertEquals(2, ((List)o).size());
            t1 = (TagVO2)((List)o).get(0);
            t2 = (TagVO2)((List)o).get(1);
        } else {
            assertEquals(2, ((Object[])o).length);
            t1 = (TagVO2)((Object[])o)[0];
            t2 = (TagVO2)((Object[])o)[1];
        }
        assertEquals("A", t1.getName());
        assertEquals("B", t1.getGroup());
        
        assertEquals("C", t2.getName());
        assertEquals("D", t2.getGroup());
    }
    
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
