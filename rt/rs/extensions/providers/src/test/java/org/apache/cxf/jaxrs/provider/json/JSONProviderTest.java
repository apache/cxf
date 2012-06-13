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

package org.apache.cxf.jaxrs.provider.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.resources.CollectionsResource;
import org.apache.cxf.jaxrs.resources.ManyTags;
import org.apache.cxf.jaxrs.resources.SuperBook;
import org.apache.cxf.jaxrs.resources.TagVO;
import org.apache.cxf.jaxrs.resources.TagVO2;
import org.apache.cxf.jaxrs.resources.Tags;
import org.apache.cxf.jaxrs.resources.jaxb.Book2;
import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class JSONProviderTest extends Assert {

    @Test
    public void testReadMalformedJson() throws Exception {
        MessageBodyReader<Tags> p = new JSONProvider<Tags>();
        byte[] bytes = "junk".getBytes();
         
        try {
            p.readFrom(Tags.class, null, null, 
                                           null, null, new ByteArrayInputStream(bytes));
            fail("404 is expected");
        } catch (WebApplicationException ex) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
        }
    }
    
    @Test
    public void testReadEmbeddedArray() throws Exception {
        String input = 
                "{\"store\":"
               + "{\"books\":{"
               + "     \"book\":["
               + "         {           "
               + "           \"name\":\"CXF 1\""
               + "         },          "
               + "         {           "
               + "           \"name\":\"CXF 2\""
               + "         }           "
               + "      ]              " 
               + "   }                 "
               + " }                   "
               + "}                    ";
        
        Object storeObject = new JSONProvider<Store>().readFrom(Store.class, null, null, 
                                       null, null, new ByteArrayInputStream(input.getBytes()));
        Store store = (Store)storeObject;
        List<Book> books = store.getBooks();
        assertEquals(2, books.size());
        assertEquals("CXF 1", books.get(0).getName());
        assertEquals("CXF 2", books.get(1).getName());
    }
    
    @Test
    public void testReadEmbeddedArrayWithNamespaces() throws Exception {
        String input = 
                "{\"ns1.store\":"
               + "{\"ns1.books\":{"
               + "     \"ns1.thebook2\":["
               + "         {           "
               + "           \"name\":\"CXF 1\""
               + "         },          "
               + "         {           "
               + "           \"name\":\"CXF 2\""
               + "         }           "
               + "      ]              " 
               + "   }                 "
               + " }                   "
               + "}                    ";
        
        JSONProvider<QualifiedStore> p = new JSONProvider<QualifiedStore>();
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("http://superbooks", "ns1");
        p.setNamespaceMap(namespaceMap);
        
        Object storeObject = p.readFrom(QualifiedStore.class, null, null, 
                                       null, null, new ByteArrayInputStream(input.getBytes()));
        QualifiedStore store = (QualifiedStore)storeObject;
        List<Book2> books = store.getBooks();
        assertEquals(2, books.size());
        assertEquals("CXF 1", books.get(0).getName());
        assertEquals("CXF 2", books.get(1).getName());
    }
    
    
    @Test
    public void testWriteCollectionWithoutXmlRootElement() 
        throws Exception {
        JSONProvider<List<SuperBook>> provider 
            = new JSONProvider<List<SuperBook>>();
        provider.setCollectionWrapperName("{http://superbooks}SuperBooks");
        provider.setJaxbElementClassMap(Collections.singletonMap(
                SuperBook.class.getName(), 
                "{http://superbooks}SuperBook"));
        SuperBook b = 
            new SuperBook("CXF in Action", 123L, 124L);
        List<SuperBook> books = 
            Collections.singletonList(b);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(books, List.class, 
                         SuperBook.class,
                         new Annotation[0], MediaType.APPLICATION_JSON_TYPE, 
                         new MetadataMap<String, Object>(), bos);
        String expected = "{\"ns1.SuperBooks\":[{\"id\":123,\"name\":\"CXF in Action\","
            + "\"state\":\"\",\"superId\":124}]}";
        assertEquals(expected, bos.toString());
    }
    
    @Test
    public void testReadFromTags() throws Exception {
        MessageBodyReader<Tags> p = new JSONProvider<Tags>();
        byte[] bytes = 
            "{\"Tags\":{\"list\":[{\"group\":\"b\",\"name\":\"a\"},{\"group\":\"d\",\"name\":\"c\"}]}}"
            .getBytes();
        Object tagsObject = p.readFrom(Tags.class, null, null, 
                                          null, null, new ByteArrayInputStream(bytes));
        Tags tags = (Tags)tagsObject;
        List<TagVO> list = tags.getTags();
        assertEquals(2, list.size());
        assertEquals("a", list.get(0).getName());
        assertEquals("b", list.get(0).getGroup());
        assertEquals("c", list.get(1).getName());
        assertEquals("d", list.get(1).getGroup());
    }
    
    @Test
    public void testReadFromTag() throws Exception {
        MessageBodyReader<TagVO> p = new JSONProvider<TagVO>();
        byte[] bytes = "{\"tagVO\":{\"group\":\"b\",\"name\":\"a\"}}"
            .getBytes();
        Object tagsObject = p.readFrom(TagVO.class, null, null, 
                                          null, null, new ByteArrayInputStream(bytes));
        TagVO tag = (TagVO)tagsObject;
        assertEquals("a", tag.getName());
        assertEquals("b", tag.getGroup());
    }
    
    @Test
    public void testReadFromQualifiedTag() throws Exception {
        JSONProvider<TagVO2> p = new JSONProvider<TagVO2>();
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("http://tags", "ns1");
        p.setNamespaceMap(namespaceMap);
        byte[] bytes = "{\"ns1.thetag\":{\"group\":\"b\",\"name\":\"a\"}}"
            .getBytes();
        Object tagsObject = p.readFrom(TagVO2.class, null, null, 
                                       null, null, new ByteArrayInputStream(bytes));
        TagVO2 tag = (TagVO2)tagsObject;
        assertEquals("a", tag.getName());
        assertEquals("b", tag.getGroup());
    }
    
    @Test
    public void testReadFromUnwrappedTagWrapperName() throws Exception {
        JSONProvider<TagVO> p = new JSONProvider<TagVO>();
        p.setSupportUnwrapped(true);
        p.setWrapperName("tagVO");
        readUnwrapped(p);
    }
    
    @Test
    public void testReadFromUnwrappedTagMap() throws Exception {
        JSONProvider<TagVO> p = new JSONProvider<TagVO>();
        p.setSupportUnwrapped(true);
        
        Map<String, String> map = new HashMap<String, String>();
        map.put(TagVO.class.getName(), "tagVO");
        p.setWrapperMap(map);
        readUnwrapped(p);
    }
    
    @Test
    public void testReadFromUnwrappedTagRoot() throws Exception {
        JSONProvider<TagVO> p = new JSONProvider<TagVO>();
        p.setSupportUnwrapped(true);
        readUnwrapped(p);
    }
    
    @Test
    public void testReadFromUnwrappedQualifiedTagRoot() throws Exception {
        JSONProvider<TagVO2> p = new JSONProvider<TagVO2>();
        p.setSupportUnwrapped(true);
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("http://tags", "ns1");
        p.setNamespaceMap(namespaceMap);
        
        byte[] bytes = "{\"group\":\"b\",\"name\":\"a\"}"
            .getBytes();
        Object tagsObject = p.readFrom(TagVO2.class, null, null, 
                                          null, null, new ByteArrayInputStream(bytes));
        TagVO2 tag = (TagVO2)tagsObject;
        assertEquals("a", tag.getName());
        assertEquals("b", tag.getGroup());
    }
    
    private void readUnwrapped(JSONProvider<TagVO> p) throws Exception {
        byte[] bytes = "{\"group\":\"b\",\"name\":\"a\"}"
            .getBytes();
        Object tagsObject = p.readFrom(TagVO.class, null, null, 
                                          null, null, new ByteArrayInputStream(bytes));
        TagVO tag = (TagVO)tagsObject;
        assertEquals("a", tag.getName());
        assertEquals("b", tag.getGroup());
    }
    
    @Test
    public void testWriteToSingleTag() throws Exception {
        JSONProvider<TagVO> p = new JSONProvider<TagVO>();
        TagVO tag = createTag("a", "b");
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(tag, TagVO.class, TagVO.class, TagVO.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals("{\"tagVO\":{\"group\":\"b\",\"name\":\"a\"}}", s);
        
    }
    
    @Test
    public void testWriteToSingleTag2NoNs() throws Exception {
        JSONProvider<TagVO2> p = new JSONProvider<TagVO2>();
        p.setIgnoreNamespaces(true);
        TagVO2 tag = createTag2("a", "b");
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(tag, TagVO2.class, TagVO2.class, TagVO2.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals("{\"thetag\":{\"group\":\"b\",\"name\":\"a\"}}", s);
        
    }
    
    @Test
    public void testCopyReaderToDocument() throws Exception {
        String s = "{\"tagVO\":{\"group\":\"b\",\"name\":\"a\"}}";
        
        ByteArrayInputStream is = new ByteArrayInputStream(s.getBytes());
        
        Document doc = new JSONProvider<Document>().readFrom(Document.class, Document.class, 
                  new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE, 
                  new MetadataMap<String, String>(), is);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(os);
        StaxUtils.copy(doc, writer);
        writer.writeEndDocument();
        String s2 = os.toString();
        assertTrue(s2.contains("<group>b</group><name>a</name>"));
    }
    
    @Test
    public void testWriteDocumentToWriter() throws Exception {
        TagVO tag = createTag("a", "b");
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        new JAXBElementProvider<TagVO>().writeTo(tag, TagVO.class, TagVO.class, 
                  TagVO.class.getAnnotations(), MediaType.APPLICATION_XML_TYPE, 
                  new MetadataMap<String, Object>(), os);
        Document doc = DOMUtils.readXml(new StringReader(os.toString()));
         
        
        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        
        new JSONProvider<Document>().writeTo(
                  doc, Document.class, Document.class, 
                  new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE, 
                  new MetadataMap<String, Object>(), os2);
        String s = os2.toString();
        assertEquals("{\"tagVO\":{\"group\":\"b\",\"name\":\"a\"}}", s);
    }
    
    @Test
    public void testWriteBookWithStringConverter() throws Exception {
        JSONProvider<Book> p = new JSONProvider<Book>();
        p.setConvertTypesToStrings(true);
        Book book = new Book("CXF", 125);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(book, Book.class, Book.class, Book.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals("{\"Book\":{\"id\":\"125\",\"name\":\"CXF\",\"state\":\"\"}}", s);
        
    }
    
    @Test
    public void testWriteToSingleTagBadgerFish() throws Exception {
        JSONProvider<TagVO> p = new JSONProvider<TagVO>();
        p.setConvention("badgerfish");
        TagVO tag = createTag("a", "b");
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(tag, TagVO.class, TagVO.class, TagVO.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals("{\"tagVO\":{\"group\":{\"$\":\"b\"},\"name\":{\"$\":\"a\"}}}", s);
        
    }
    
    @Test
    public void testWriteToSingleQualifiedTag() throws Exception {
        JSONProvider<TagVO2> p = new JSONProvider<TagVO2>();
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("http://tags", "ns1");
        p.setNamespaceMap(namespaceMap);
        TagVO2 tag = createTag2("a", "b");
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(tag, TagVO2.class, TagVO2.class, TagVO2.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals("{\"ns1.thetag\":{\"group\":\"b\",\"name\":\"a\"}}", s);
    }
    
    @Test
    public void testWriteToSingleQualifiedTagBadgerFish() throws Exception {
        JSONProvider<TagVO2> p = new JSONProvider<TagVO2>();
        p.setConvention("badgerfish");
        TagVO2 tag = createTag2("a", "b");
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(tag, TagVO2.class, TagVO2.class, TagVO2.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals("{\"ns2:thetag\":{\"@xmlns\":{\"ns2\":\"http:\\/\\/tags\"},"
                     + "\"group\":{\"$\":\"b\"},\"name\":{\"$\":\"a\"}}}", s);
    }
    
    @Test
    public void testDropRootElement() throws Exception {
        JSONProvider<TagVO2> p = new JSONProvider<TagVO2>();
        p.setDropRootElement(true);
        p.setIgnoreNamespaces(true);
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("http://tags", "ns1");
        p.setNamespaceMap(namespaceMap);
        TagVO2 tag = createTag2("a", "b");
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(tag, TagVO2.class, TagVO2.class, TagVO2.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals("{\"group\":\"b\",\"name\":\"a\"}", s);
        
    }
    
    @Test
    public void testWriteQualifiedCollection() throws Exception {
        String data = "{\"ns1.tag\":[{\"group\":\"b\",\"name\":\"a\"}"
            + ",{\"group\":\"d\",\"name\":\"c\"}]}";
        doWriteQualifiedCollection(false, false, false, data);
    }
    
    @Test
    public void testWriteQualifiedCollectionDropNs() throws Exception {
        String data = "{\"tag\":[{\"group\":\"b\",\"name\":\"a\"}"
            + ",{\"group\":\"d\",\"name\":\"c\"}]}";
        doWriteQualifiedCollection(false, false, true, data);
    }
    
    @Test
    public void testWriteQualifiedCollection2() throws Exception {
        String data = "{{\"group\":\"b\",\"name\":\"a\"}"
            + ",{\"group\":\"d\",\"name\":\"c\"}}";
        doWriteQualifiedCollection(true, false, false, data);
    }
    
    @Test
    public void testWriteQualifiedCollection3() throws Exception {
        String data = "[{\"group\":\"b\",\"name\":\"a\"}"
            + ",{\"group\":\"d\",\"name\":\"c\"}]";
        doWriteQualifiedCollection(true, true, false, data);
    }
    
    public void doWriteQualifiedCollection(boolean drop, boolean serializeAsArray, 
                                           boolean ignoreNamespaces, String data) 
        throws Exception {
        JSONProvider<List<?>> p = new JSONProvider<List<?>>();
        p.setCollectionWrapperName("{http://tags}tag");
        p.setDropCollectionWrapperElement(drop);
        p.setSerializeAsArray(serializeAsArray);
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("http://tags", "ns1");
        p.setNamespaceMap(namespaceMap);
        p.setIgnoreNamespaces(ignoreNamespaces);
        
        List<TagVO2> tags = new ArrayList<TagVO2>();
        tags.add(createTag2("a", "b"));
        tags.add(createTag2("c", "d"));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Method m = CollectionsResource.class.getMethod("getTags", new Class[0]);
        p.writeTo(tags, m.getReturnType(), m.getGenericReturnType(), new Annotation[0], 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        String s = os.toString();
        assertEquals(s, data);
        
    }
    
    @Test
    public void testWriteUnqualifiedCollection() throws Exception {
        JSONProvider<List<Book>> p = new JSONProvider<List<Book>>();
        List<Book> books = new ArrayList<Book>();
        books.add(new Book("CXF", 123L));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Method m = CollectionsResource.class.getMethod("getBooks", new Class[0]);
        p.writeTo(books, m.getReturnType(), m.getGenericReturnType(), new Annotation[0], 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        assertEquals("{\"Book\":[{\"id\":123,\"name\":\"CXF\",\"state\":\"\"}]}",
                     os.toString());
        
    }
    
    @Test
    public void testReadUnqualifiedCollection() throws Exception {
        String data = "{\"Book\":[{\"id\":\"123\",\"name\":\"CXF in Action\"}"
            + ",{\"id\":\"124\",\"name\":\"CXF Rocks\"}]}";
        doReadUnqualifiedCollection(data, "setBooks", List.class);
    }
    
    @SuppressWarnings("unchecked")
    private <T> void doReadUnqualifiedCollection(String data, String mName, Class<T> type) throws Exception {
        JSONProvider<T> provider = new JSONProvider<T>();
        Method m = CollectionsResource.class.getMethod(mName, 
                                                       new Class[]{type});
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(
                       type, m.getGenericParameterTypes()[0],
                       new Annotation[0], MediaType.APPLICATION_JSON_TYPE, 
                       new MetadataMap<String, String>(), is);
        assertNotNull(o);
        Book b1 = null;
        Book b2 = null;
        if (type.isArray()) {
            assertEquals(2, ((Book[])o).length);
            b1 = ((Book[])o)[0];
            b2 = ((Book[])o)[1];
        } else if (type == Set.class) {
            Set<Book> set = CastUtils.cast((Set<?>)o);
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
        String data = "{\"ns1.thetag\":[{\"group\":\"b\",\"name\":\"a\"}"
            + ",{\"group\":\"d\",\"name\":\"c\"}]}";
        doReadQualifiedCollection(data, false);
    }
    
    @Test
    public void testReadQualifiedArray() throws Exception {
        String data = "{\"ns1.thetag\":[{\"group\":\"b\",\"name\":\"a\"}"
            + ",{\"group\":\"d\",\"name\":\"c\"}]}";
        doReadQualifiedCollection(data, true);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void doReadQualifiedCollection(String data, boolean isArray) throws Exception {
        
        JSONProvider provider = new JSONProvider();
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("http://tags", "ns1");
        provider.setNamespaceMap(namespaceMap);
        
        Method m = null;
        if (!isArray) {
            m = CollectionsResource.class.getMethod("setTags", new Class[]{List.class});
        } else {
            m = CollectionsResource.class.getMethod("setTagsArray", new Class[]{TagVO2[].class});
        }
        
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(
                       m.getParameterTypes()[0], m.getGenericParameterTypes()[0],
                       new Annotation[0], MediaType.APPLICATION_JSON_TYPE, 
                       new MetadataMap<String, String>(), is);
        assertNotNull(o);
        TagVO2 t1 = null;
        TagVO2 t2 = null;
        if (!isArray) {
            assertEquals(2, ((List<?>)o).size());
            t1 = (TagVO2)((List<?>)o).get(0);
            t2 = (TagVO2)((List<?>)o).get(1);
        } else {
            assertEquals(2, ((Object[])o).length);
            t1 = (TagVO2)((Object[])o)[0];
            t2 = (TagVO2)((Object[])o)[1];
        }
        assertEquals("a", t1.getName());
        assertEquals("b", t1.getGroup());
        
        assertEquals("c", t2.getName());
        assertEquals("d", t2.getGroup());
    }
    
    @Test
    public void testWriteToSingleQualifiedTag2() throws Exception {
        JSONProvider<TagVO2> p = new JSONProvider<TagVO2>();
        p.setNamespaceMap(Collections.singletonMap("http://tags", "ns1"));
        TagVO2 tag = createTag2("a", "b");
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(tag, TagVO2.class, TagVO2.class, TagVO2.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals("{\"ns1.thetag\":{\"group\":\"b\",\"name\":\"a\"}}", s);
        
    }
    
    @Test
    public void testWriteIgnoreMixedContent() throws Exception {
        doTestMixedContent("{\"Book\":{\"name\":\"CXF\",\"id\":125}}",
                           true, "book.xml");
    }
    
    @Test
    public void testWriteIgnoreMixedContent2() throws Exception {
        doTestMixedContent("{\"Book\":{\"name\":\"CXF\",\"id\":125,\"$\":\"books\"}}",
                           true, "book2.xml");
    }
    
    @Test
    public void testWriteMixedContent() throws Exception {
        doTestMixedContent("{\"Book\":{\"name\":\"CXF\",\"id\":125}}",
                           false, "book.xml");
    }
    
    private void doTestMixedContent(String data, boolean ignore, String fileName) throws Exception {
        InputStream is = getClass().getResourceAsStream(fileName);
        JAXBContext context = JAXBContext.newInstance(new Class[]{Books.class, Book.class});
        Unmarshaller um = context.createUnmarshaller();
        JAXBElement<?> jaxbEl = um.unmarshal(new StreamSource(is), Books.class);
        JSONProvider<JAXBElement<?>> p = new JSONProvider<JAXBElement<?>>();
        p.setIgnoreMixedContent(ignore);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(jaxbEl, JAXBElement.class, JAXBElement.class, JAXBElement.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        String s = os.toString();
        assertEquals(data, s);
    }
    
    @Test
    public void testWriteListOfDerivedTypes() throws Exception {
        JSONProvider<Books2> p = new JSONProvider<Books2>();
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("http://www.w3.org/2001/XMLSchema-instance", "xsins");
        p.setNamespaceMap(namespaceMap);
        Books2 books2 = new Books2();
        books2.setBooks(Collections.singletonList(
                            new SuperBook("CXF Rocks", 123L, 124L)));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(books2, Books2.class, Books2.class, Books2.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        String s = os.toString();
        String data = "{\"books2\":{\"books\":{\"@xsins.type\":\"superBook\",\"id\":123,"
            + "\"name\":\"CXF Rocks\",\"state\":\"\",\"superId\":124}}}";
        assertEquals(data, s);
    }
    
    @Test
    public void testReadListOfDerivedTypes() throws Exception {
        JSONProvider<Books2> p = new JSONProvider<Books2>();
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("http://www.w3.org/2001/XMLSchema-instance", "xsins");
        p.setNamespaceMap(namespaceMap);
        String data = "{\"books2\":{\"books\":{\"@xsins.type\":\"superBook\",\"id\":123,"
            + "\"name\":\"CXF Rocks\",\"superId\":124}}}";
        byte[] bytes = data.getBytes();
        Object books2Object = p.readFrom(Books2.class, null, null, 
                                          null, null, new ByteArrayInputStream(bytes));
        Books2 books = (Books2)books2Object;
        List<? extends Book> list = books.getBooks();
        assertEquals(1, list.size());
        SuperBook book = (SuperBook)list.get(0);
        assertEquals(124L, book.getSuperId());
        assertEquals(123L, book.getId());
        assertEquals("CXF Rocks", book.getName());
    }
    
    @Test
    public void testReadListOfDerivedTypesWithNullField() throws Exception {
        JSONProvider<Books2> p = new JSONProvider<Books2>();
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("http://www.w3.org/2001/XMLSchema-instance", "xsins");
        p.setNamespaceMap(namespaceMap);
        String data = "{\"books2\":{\"books\":{\"@xsins.type\":\"superBook\",\"id\":123,"
            + "\"name\":null,\"superId\":124}}}";
        byte[] bytes = data.getBytes();
        Object books2Object = p.readFrom(Books2.class, null, null, 
                                          null, null, new ByteArrayInputStream(bytes));
        Books2 books = (Books2)books2Object;
        List<? extends Book> list = books.getBooks();
        assertEquals(1, list.size());
        SuperBook book = (SuperBook)list.get(0);
        assertEquals(124L, book.getSuperId());
        assertEquals(0, book.getName().length());
    }
    
    @Test
    public void testWriteToListWithManyValues() throws Exception {
        JSONProvider<Tags> p = new JSONProvider<Tags>();
        Tags tags = new Tags();
        tags.addTag(createTag("a", "b"));
        tags.addTag(createTag("c", "d"));
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(tags, Tags.class, Tags.class, Tags.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals(
            "{\"Tags\":{\"list\":[{\"group\":\"b\",\"name\":\"a\"},{\"group\":\"d\",\"name\":\"c\"}]}}",
            s);
    }
    
    @Test
    public void testWriteToListWithSingleValue() throws Exception {
        JSONProvider<Tags> p = new JSONProvider<Tags>();
        p.setSerializeAsArray(true);
        p.setArrayKeys(Collections.singletonList("list"));
        Tags tags = new Tags();
        tags.addTag(createTag("a", "b"));
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(tags, Tags.class, Tags.class, Tags.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals(
            "{\"Tags\":{\"list\":[{\"group\":\"b\",\"name\":\"a\"}]}}",
            s);
    }
    
    @Test
    public void testWriteUsingNaturalNotation() throws Exception {
        JSONProvider<Post> p = new JSONProvider<Post>();
        p.setSerializeAsArray(true);
        p.setArrayKeys(Collections.singletonList("comments"));
        Post post = new Post();
        post.setTitle("post");
        Comment c1 = new Comment();
        c1.setTitle("comment1");
        Comment c2 = new Comment();
        c2.setTitle("comment2");
        post.getComments().add(c1);
        post.getComments().add(c2);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(post, Post.class, Post.class, Post.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals(
            "{\"post\":{\"title\":\"post\",\"comments\":[{\"title\":\"comment1\"},"
            + "{\"title\":\"comment2\"}]}}",
            s);
    }
    
    @Test
    public void testManyTags() throws Exception {
        JSONProvider<ManyTags> p = new JSONProvider<ManyTags>();
        p.setSerializeAsArray(true);
        p.setArrayKeys(Collections.singletonList("list"));
        
        Tags tags = new Tags();
        tags.addTag(createTag("a", "b"));
        ManyTags many = new ManyTags();
        many.setTags(tags);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(many, ManyTags.class, ManyTags.class, ManyTags.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals(
            "{\"ManyTags\":{\"tags\":{\"list\":[{\"group\":\"b\",\"name\":\"a\"}]}}}",
            s);
    }

    @Test
    public void testInDropElement() throws Exception {
        String data = "{\"Extra\":{\"ManyTags\":{\"tags\":{\"list\":[{\"group\":\"b\",\"name\":\"a\"}]}}}}";
        JSONProvider<ManyTags> provider = new JSONProvider<ManyTags>();
        provider.setInDropElements(Collections.singletonList("Extra"));
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(ManyTags.class, ManyTags.class,
                      new Annotation[0], MediaType.APPLICATION_JSON_TYPE, 
                      new MetadataMap<String, String>(), is);
        ManyTags holder = (ManyTags)o;
        assertNotNull(holder);    
        TagVO tag = holder.getTags().getTags().get(0);
        assertEquals("a", tag.getName());
        assertEquals("b", tag.getGroup());
    }
    
    @Test
    public void testInAppendElementNoNs() throws Exception {
        String data = "{\"tags\":{\"list\":[{\"group\":\"b\",\"name\":\"a\"}]}}";
        readAppendElementsNoNs(data, Collections.singletonMap("tags", "ManyTags"));
    }
    
    @Test
    public void testInAppendElementNoNs2() throws Exception {
        String data = "{\"ManyTags\":{\"list\":[{\"group\":\"b\",\"name\":\"a\"}]}}";
        readAppendElementsNoNs(data, Collections.singletonMap("list", "tags"));
    }
    
    private void readAppendElementsNoNs(String data, Map<String, String> appendMap) throws Exception {
        JSONProvider<ManyTags> provider = new JSONProvider<ManyTags>();
        provider.setInAppendElements(appendMap);
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(ManyTags.class, ManyTags.class,
                      new Annotation[0], MediaType.APPLICATION_JSON_TYPE, 
                      new MetadataMap<String, String>(), is);
        ManyTags holder = (ManyTags)o;
        assertNotNull(holder);    
        TagVO tag = holder.getTags().getTags().get(0);
        assertEquals("a", tag.getName());
        assertEquals("b", tag.getGroup());
    }
    
    @Test
    public void testInNsElementFromLocal() throws Exception {
        String data = "{thetag:{\"group\":\"B\",\"name\":\"A\"}}";
        readTagVO2AfterTransform(data, "thetag");
    }
    
    @Test
    public void testInNsElementFromNsElement() throws Exception {
        String data = "{t.thetag2:{\"group\":\"B\",\"name\":\"A\"}}";
        readTagVO2AfterTransform(data, "{http://bar}thetag2");
    }
    
    @Test
    public void testInLocalFromLocal() throws Exception {
        String data = "{thetag:{\"group\":\"B\",\"name\":\"A\"}}";
        readTagVOAfterTransform(data, "thetag");
    }
    
    @Test
    public void testInLocalFromNsElement() throws Exception {
        String data = "{t.thetag2:{\"group\":\"B\",\"name\":\"A\"}}";
        readTagVOAfterTransform(data, "{http://bar}thetag2");
    }
    
    private void readTagVO2AfterTransform(String data, String keyValue) throws Exception {
        JSONProvider<TagVO2> provider = new JSONProvider<TagVO2>();
        Map<String, String> map = new HashMap<String, String>();
        map.put(keyValue, "{http://tags}thetag");
        Map<String, String> nsmap = new HashMap<String, String>();
        nsmap.put("http://bar", "t");
        provider.setNamespaceMap(nsmap);
        provider.setInTransformElements(map);
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(TagVO2.class, TagVO2.class,
                      new Annotation[0], MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, String>(),
                      is);
        TagVO2 tag2 = (TagVO2)o;
        assertEquals("A", tag2.getName());
        assertEquals("B", tag2.getGroup());    
    }
    
    @Test
    public void testInNsElementsFromLocals() throws Exception {
        String data = "{tagholder:{thetag:{\"group\":\"B\",\"name\":\"A\"}}}";
        JSONProvider<TagVO2Holder> provider = new JSONProvider<TagVO2Holder>();
        Map<String, String> map = new HashMap<String, String>();
        map.put("tagholder", "{http://tags}tagholder");
        map.put("thetag", "{http://tags}thetag");
        provider.setInTransformElements(map);
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(TagVO2Holder.class, TagVO2Holder.class,
                      new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        TagVO2Holder holder = (TagVO2Holder)o;
        TagVO2 tag2 = holder.getTagValue();
        assertEquals("A", tag2.getName());
        assertEquals("B", tag2.getGroup());    
    }
    
    @Test
    public void testOutAttributesAsElements() throws Exception {
        JSONProvider<TagVO2Holder> provider = new JSONProvider<TagVO2Holder>();
        Map<String, String> map = new HashMap<String, String>();
        map.put("{http://tags}thetag", "thetag");
        map.put("{http://tags}tagholder", "tagholder");
        provider.setOutTransformElements(map);
        provider.setAttributesToElements(true);
        TagVO2 tag = new TagVO2("A", "B");
        TagVO2Holder holder = new TagVO2Holder();
        holder.setTag(tag);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(holder, TagVO2Holder.class, TagVO2Holder.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = 
            "{\"tagholder\":{\"attr\":\"attribute\",\"thetag\":{\"group\":\"B\",\"name\":\"A\"}}}";
        assertEquals(expected, bos.toString());
    }
    
    @Test
    public void testOutAttributesAsElementsForList() throws Exception {

        //Provider
        JSONProvider<List<?>> provider = new JSONProvider<List<?>>();
        provider.setCollectionWrapperName("tagholders");
        Map<String, String> map = new HashMap<String, String>();
        map.put("{http://tags}*", "*");
        provider.setOutTransformElements(map);
        provider.setAttributesToElements(true);
    
        //data setup
        TagVO2 tag = new TagVO2("A", "B");
        TagVO2Holder holder = new TagVO2Holder();
        holder.setTag(tag);
        List<TagVO2Holder> list = new ArrayList<TagVO2Holder>();
        list.add(holder);
    
        //ParameterizedType required for Lists of Objects
        ParameterizedType type = new ParameterizedType() {
            public Type getRawType() { return List.class; }
            public Type getOwnerType() { return null; }
            public Type[] getActualTypeArguments() {
                return new Type[] {TagVO2Holder.class};
            }
        };
    
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(list, ArrayList.class, type,
            new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = 
            "{\"tagholders\":["
            + "{\"tagholder\":{\"attr\":\"attribute\",\"thetag\":{\"group\":\"B\",\"name\":\"A\"}}}"
            + "]}";
        assertEquals(expected, bos.toString());
    }
    
    private void readTagVOAfterTransform(String data, String keyValue) throws Exception {
        JSONProvider<TagVO> provider = new JSONProvider<TagVO>();
        Map<String, String> map = new HashMap<String, String>();
        map.put(keyValue, "tagVO");
        provider.setInTransformElements(map);
        Map<String, String> nsmap = new HashMap<String, String>();
        nsmap.put("http://bar", "t");
        provider.setNamespaceMap(nsmap);
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(TagVO.class, TagVO.class,
                      new Annotation[0], MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, String>(),
                      is);
        TagVO tag2 = (TagVO)o;
        assertEquals("A", tag2.getName());
        assertEquals("B", tag2.getGroup());    
    }
    
    
    @Test
    @Ignore
    // name:A is lost
    public void testDropElementsIgnored() throws Exception {
        JSONProvider<ManyTags> provider = new JSONProvider<ManyTags>();
        List<String> list = new ArrayList<String>();
        list.add("ManyTags");
        list.add("list");
        list.add("tags");
        provider.setOutDropElements(list);
        ManyTags many = new ManyTags();
        Tags tags = new Tags();
        tags.addTag(new TagVO("A", "B"));
        tags.addTag(new TagVO("C", "D"));
        many.setTags(tags);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(many, ManyTags.class, ManyTags.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        System.out.println(bos.toString());
    }
 
    @Test
    public void testDropElements() throws Exception {
        JSONProvider<ManyTags> provider = new JSONProvider<ManyTags>();
        List<String> list = new ArrayList<String>();
        list.add("ManyTags");
        list.add("tags");
        provider.setOutDropElements(list);
        ManyTags many = new ManyTags();
        Tags tags = new Tags();
        tags.addTag(new TagVO("A", "B"));
        tags.addTag(new TagVO("C", "D"));
        many.setTags(tags);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(many, ManyTags.class, ManyTags.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "{\"list\":[{\"group\":\"B\",\"name\":\"A\"},"
            + "{\"group\":\"D\",\"name\":\"C\"}]}";
        assertEquals(expected, bos.toString());
    }
    
    
    @Test
    public void testWriteWithXmlRootElementAndPackageInfo() throws Exception {
        JSONProvider<org.apache.cxf.jaxrs.resources.jaxb.Book2> provider = 
            new JSONProvider<org.apache.cxf.jaxrs.resources.jaxb.Book2>();
        org.apache.cxf.jaxrs.resources.jaxb.Book2 book = 
            new org.apache.cxf.jaxrs.resources.jaxb.Book2(333);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(book, org.apache.cxf.jaxrs.resources.jaxb.Book2.class, 
                         org.apache.cxf.jaxrs.resources.jaxb.Book2.class,
                         new Annotation[0], MediaType.APPLICATION_JSON_TYPE, 
                         new MetadataMap<String, Object>(), bos);
        assertEquals("{\"os.thebook2\":{\"id\":333}}", bos.toString());
    }
    
    @Test
    public void testDropQualifiedElements() throws Exception {
        JSONProvider<TagVO2> provider = new JSONProvider<TagVO2>();
        List<String> list = new ArrayList<String>();
        list.add("{http://tags}thetag");
        provider.setOutDropElements(list);
        Map<String, String> map = new HashMap<String, String>();
        map.put("name", "");
        provider.setOutTransformElements(map);
        TagVO2 tag = new TagVO2("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO2.class, TagVO2.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "{\"group\":\"B\"}";
        assertEquals(expected, bos.toString());
    }
    
    @Test
    public void testOutAppendNsElementBeforeLocal() throws Exception {
        JSONProvider<TagVO> provider = new JSONProvider<TagVO>();
        Map<String, String> map = new HashMap<String, String>();
        map.put("tagVO", "{http://tagsvo2}t");
        provider.setOutAppendElements(map);
        TagVO tag = new TagVO("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO.class, TagVO.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "{\"ps1.t\":{\"tagVO\":{\"group\":\"B\",\"name\":\"A\"}}}";
        assertEquals(expected, bos.toString());
    }
    
    @Test
    public void testOutAppendLocalBeforeLocal() throws Exception {
        JSONProvider<TagVO> provider = new JSONProvider<TagVO>();
        Map<String, String> map = new HashMap<String, String>();
        map.put("tagVO", "supertag");
        provider.setOutAppendElements(map);
        TagVO tag = new TagVO("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO.class, TagVO.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "{\"supertag\":{\"tagVO\":{\"group\":\"B\",\"name\":\"A\"}}}";
        assertEquals(expected, bos.toString());
    }
    
    @Test
    public void testOutAppendElementsSameNs() throws Exception {
        JSONProvider<TagVO2> provider = new JSONProvider<TagVO2>();
        Map<String, String> map = new HashMap<String, String>();
        map.put("{http://tags}thetag", "{http://tags}t");
        provider.setOutAppendElements(map);
        TagVO2 tag = new TagVO2("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO2.class, TagVO2.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "{\"ns2.t\":{\"ns2.thetag\":{\"group\":\"B\",\"name\":\"A\"}}}";
        assertEquals(expected, bos.toString());
    }
    
    @Test
    public void testOutAppendElementsDiffNs() throws Exception {
        JSONProvider<TagVO2> provider = new JSONProvider<TagVO2>();
        Map<String, String> map = new HashMap<String, String>();
        map.put("{http://tags}thetag", "{http://tagsvo2}t");
        provider.setOutAppendElements(map);
        TagVO2 tag = new TagVO2("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO2.class, TagVO2.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "{\"ps1.t\":{\"ns2.thetag\":{\"group\":\"B\",\"name\":\"A\"}}}";
        assertEquals(expected, bos.toString());
    }
    
    @Test
    public void testOutElementsMapLocalNsToLocalNs() throws Exception {
        JSONProvider<TagVO2> provider = new JSONProvider<TagVO2>();
        Map<String, String> map = new HashMap<String, String>();
        map.put("{http://tags}thetag", "{http://tagsvo2}t");
        provider.setOutTransformElements(map);
        TagVO2 tag = new TagVO2("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO2.class, TagVO2.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "{\"ns2.t\":{\"group\":\"B\",\"name\":\"A\"}}";
        assertEquals(expected, bos.toString());
    }
    

    @Test
    public void testOutElementsMapLocalNsToLocal() throws Exception {
        JSONProvider<TagVO2> provider = new JSONProvider<TagVO2>();
        Map<String, String> map = new HashMap<String, String>();
        map.put("{http://tags}thetag", "t");
        provider.setOutTransformElements(map);
        TagVO2 tag = new TagVO2("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO2.class, TagVO2.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "{\"t\":{\"group\":\"B\",\"name\":\"A\"}}";
        assertEquals(expected, bos.toString());
    }
    
    @Test
    public void testOutElementsMapLocalToLocalNs() throws Exception {
        JSONProvider<TagVO> provider = new JSONProvider<TagVO>();
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("http://tags", "ns1");
        Map<String, String> map = new HashMap<String, String>();
        map.put("tagVO", "{http://tags}thetag");
        provider.setOutTransformElements(map);
        TagVO tag = new TagVO("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO.class, TagVO.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "{\"ps1.thetag\":{\"group\":\"B\",\"name\":\"A\"}}";
        assertEquals(expected, bos.toString());
    }
    
    @Test
    public void testOutElementsMapLocalToLocal() throws Exception {
        JSONProvider<TagVO> provider = new JSONProvider<TagVO>();
        Map<String, String> map = new HashMap<String, String>();
        map.put("tagVO", "supertag");
        map.put("group", "group2");
        provider.setOutTransformElements(map);
        TagVO tag = new TagVO("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO.class, TagVO.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "{\"supertag\":{\"group2\":\"B\",\"name\":\"A\"}}";
        assertEquals(expected, bos.toString());
    }
    
    private TagVO createTag(String name, String group) {
        return new TagVO(name, group);
    }
    
    private TagVO2 createTag2(String name, String group) {
        return new TagVO2(name, group);
    }
    
    @Test
    public void testWriteReadDerivedNamespace() throws Exception {
        JSONProvider<Base1> provider = new JSONProvider<Base1>();
        provider.setMarshallAsJaxbElement(true);
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("http://derivedtest", "derivedtestnamespace");
        provider.setNamespaceMap(namespaceMap);

        Base1 b = new Derived1("base", "derived");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(b, Base1.class, Base1.class,
                        new Annotation[0], MediaType.APPLICATION_JSON_TYPE,
                        new MetadataMap<String, Object>(), bos);
        
        readBase(bos.toString());
    }

    private void readBase(String data) throws Exception {
        JSONProvider<Base1> provider = new JSONProvider<Base1>();
        provider.setUnmarshallAsJaxbElement(true);
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("http://derivedtest", "derivedtestnamespace");
        provider.setNamespaceMap(namespaceMap);

        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());

        Base1 base = provider.readFrom(
                        Base1.class, Base1.class,
                        new Annotation[0], MediaType.APPLICATION_JSON_TYPE,
                        new MetadataMap<String, String>(), is);
        assertEquals("base", base.getBase1Field());
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "Base1", namespace = "http://derivedtest")
    @XmlSeeAlso({Derived1.class })
    public static class Base1 {
    
        protected String base1Field;
    
        Base1() { }
    
        Base1(String base) {
            base1Field = base;
        }
    
        public String getBase1Field() {
            return base1Field;
        }
    
        public void setBase1Field(String value) {
            this.base1Field = value;
        }
    }


    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "Derived1", namespace = "http://derivedtest")
    public static class Derived1 extends Base1 {
    
        protected String derived1Field;
    
        Derived1() { }
        public Derived1(String base, String derived) {
            super(base);
            derived1Field = derived;
        }
    
        public String getDerived1Field() {
            return derived1Field;
        }
    
        public void setDerived1Field(String value) {
            this.derived1Field = value;
        }
    
    }

    
    @XmlRootElement()
    public static class Books {
        @XmlMixed
        @XmlAnyElement(lax = true)
        protected List<Object> books;
    }
    
    @XmlRootElement()
    public static class Books2 {
        protected List<? extends Book> books;
        
        public void setBooks(List<? extends Book> list) {
            books = list;
        }
        
        public List<? extends Book> getBooks() {
            return books;
        }
    }
    
    @XmlRootElement()
    @XmlType(name = "", propOrder = {"title", "comments" })
    public static class Post {
        private String title;
        private List<Comment> comments = new ArrayList<Comment>();
        public void setTitle(String title) {
            this.title = title;
        }
        public String getTitle() {
            return title;
        }
        public void setComments(List<Comment> comments) {
            this.comments = comments;
        }
        public List<Comment> getComments() {
            return comments;
        }
    }
    
    public static class Comment {
        private String title;

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
    @XmlRootElement(name = "store")
    public static class Store {
        private List<Book> books = new LinkedList<Book>();
       
        @XmlElement(name = "book")
        @XmlElementWrapper(name = "books")
        public List<Book> getBooks() {
            return books;
        }
    }
    
    @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
    @XmlRootElement(name = "store", namespace = "http://superbooks")
    public static class QualifiedStore {
        private List<Book2> books = new LinkedList<Book2>();
       
        @XmlElement(name = "thebook2", namespace = "http://superbooks")
        @XmlElementWrapper(name = "books", namespace = "http://superbooks")
        public List<Book2> getBooks() {
            return books;
        }
    }
    
    @XmlRootElement(name = "tagholder", namespace = "http://tags")
    public static class TagVO2Holder {
        @XmlElement(name = "thetag", namespace = "http://tags")
        private TagVO2 t;
        @XmlAttribute
        private String attr = "attribute"; 
        
        public void setTag(TagVO2 tag) {
            this.t = tag;
        }

        public TagVO2 getTagValue() {
            return t;
        }

        public String getAttribute() {
            return attr;
        }
        
    }
}
