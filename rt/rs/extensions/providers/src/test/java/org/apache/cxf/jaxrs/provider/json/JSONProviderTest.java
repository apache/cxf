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
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlMixed;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
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
import org.apache.cxf.jaxrs.resources.jaxb.Book2NoRootElement;
import org.apache.cxf.staxutils.DelegatingXMLStreamWriter;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.transform.TransformUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JSONProviderTest {

    @Test
    public void testReadMalformedJson() throws Exception {
        MessageBodyReader<Tags> p = new JSONProvider<>();
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
    public void testReadListOfProperties() throws Exception {

        String input =
            "{\"theBook\":"
            + "{"
            + "\"Names\":[{\"Name\":\"1\"}, {\"Name\":\"2\"}]"
            + " }                   "
            + "}                    ";

        JSONProvider<TheBook> provider = new JSONProvider<>();
        provider.setPrimitiveArrayKeys(Collections.singletonList("Names"));
        TheBook theBook = provider.readFrom(TheBook.class, null, null,
                                   null, null, new ByteArrayInputStream(input.getBytes()));
        List<String> names = theBook.getName();
        assertNotNull(names);
        assertEquals("1", names.get(0));
        assertEquals("2", names.get(1));
    }

    @Test
    public void testReadNullStringAsNull() throws Exception {

        String input = "{\"Book\":{\"id\":123,\"name\":\"null\"}}";

        JSONProvider<Book> provider = new JSONProvider<>();
        Book theBook = provider.readFrom(Book.class, null, null,
                                   null, null, new ByteArrayInputStream(input.getBytes()));
        assertEquals(123L, theBook.getId());
        assertEquals("", theBook.getName());
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

        JSONProvider<QualifiedStore> p = new JSONProvider<>();
        Map<String, String> namespaceMap = new HashMap<>();
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
        JSONProvider<List<SuperBook>> provider = new JSONProvider<>();
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
    @org.junit.Ignore
    public void testWriteCollectionAsPureArray() throws Exception {
        JSONProvider<ReportDefinition> provider
            = new JSONProvider<>();
        provider.setSerializeAsArray(true);
        provider.setDropRootElement(true);
        provider.setOutDropElements(Arrays.asList("parameterList"));
        provider.setDropElementsInXmlStream(false);
        ReportDefinition r = new ReportDefinition();
        r.setReportName("report");
        r.addParameterDefinition(new ParameterDefinition("param"));

        Method m = ReportService.class.getMethod("findReport", new Class<?>[]{});
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(r, m.getReturnType(), m.getGenericReturnType(),
                         new Annotation[0], MediaType.APPLICATION_JSON_TYPE,
                         new MetadataMap<String, Object>(), bos);
        assertTrue(bos.toString().startsWith("[{\"parameterList\":"));
    }

    @Test
    public void testWriteCollectionAsPureArray2() throws Exception {
        JSONProvider<ReportDefinition> provider
            = new JSONProvider<>();
        provider.setSerializeAsArray(true);
        provider.setOutDropElements(Collections.singletonList("reportDefinition"));
        provider.setDropElementsInXmlStream(false);
        ReportDefinition r = new ReportDefinition();
        r.setReportName("report");
        r.addParameterDefinition(new ParameterDefinition("param"));

        Method m = ReportService.class.getMethod("findReport", new Class<?>[]{});
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(r, m.getReturnType(), m.getGenericReturnType(),
                         new Annotation[0], MediaType.APPLICATION_JSON_TYPE,
                         new MetadataMap<String, Object>(), bos);
        assertTrue(bos.toString().startsWith("[{\"parameterList\":"));
    }

    @Test
    public void testWriteCollectionAsPureArray3() throws Exception {

        JSONProvider<ReportDefinition> provider
            = new JSONProvider<>();
        provider.setIgnoreNamespaces(true);
        provider.setSerializeAsArray(true);
        provider.setArrayKeys(Arrays.asList("parameterList".split(" ")));
        provider.setDropRootElement(true);
        provider.setDropElementsInXmlStream(false);
        ReportDefinition r = new ReportDefinition();
        r.setReportName("report");
        ParameterDefinition paramDef = new ParameterDefinition("param");
        r.addParameterDefinition(paramDef);

        Method m = ReportService.class.getMethod("findReport", new Class<?>[]{});
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(r, m.getReturnType(), m.getGenericReturnType(),
                         new Annotation[0], MediaType.APPLICATION_JSON_TYPE,
                         new MetadataMap<String, Object>(), bos);
        assertTrue(bos.toString().startsWith("[{\"parameterList\":["));

    }

    @Test
    public void testWriteBeanNoRootAtJsonLevel() throws Exception {
        JSONProvider<Book> provider = new JSONProvider<>();
        provider.setDropRootElement(true);
        provider.setDropElementsInXmlStream(false);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(new Book("cxf", 123), Book.class, Book.class,
                         new Annotation[0], MediaType.APPLICATION_JSON_TYPE,
                         new MetadataMap<String, Object>(), bos);
        assertTrue(bos.toString().contains("\"name\":\"cxf\""));
        assertTrue(bos.toString().contains("\"id\":123"));
        assertFalse(bos.toString().startsWith("{\"Book\":"));
    }

    @Test
    public void testWriteBeanIgnorePropertyAtJsonLevel() throws Exception {
        JSONProvider<Book> provider = new JSONProvider<>();
        provider.setOutDropElements(Collections.singletonList("id"));
        provider.setDropElementsInXmlStream(false);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(new Book("cxf", 123), Book.class, Book.class,
                         new Annotation[0], MediaType.APPLICATION_JSON_TYPE,
                         new MetadataMap<String, Object>(), bos);
        assertTrue(bos.toString().contains("\"name\":\"cxf\""));
        assertFalse(bos.toString().contains("\"id\":123"));
        assertTrue(bos.toString().startsWith("{\"Book\":"));
    }
    @Test
    public void testDoNotEscapeForwardSlashes() throws Exception {
        JSONProvider<Book> provider = new JSONProvider<>();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(new Book("http://cxf", 123), Book.class, Book.class,
                         new Annotation[0], MediaType.APPLICATION_JSON_TYPE,
                         new MetadataMap<String, Object>(), bos);
        assertTrue(bos.toString().contains("\"name\":\"http://cxf\""));
    }
    @Test
    public void testEscapeForwardSlashesAlways() throws Exception {
        JSONProvider<Book> provider = new JSONProvider<>();
        provider.setEscapeForwardSlashesAlways(true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(new Book("http://cxf", 123), Book.class, Book.class,
                         new Annotation[0], MediaType.APPLICATION_JSON_TYPE,
                         new MetadataMap<String, Object>(), bos);
        assertTrue(bos.toString().contains("\"name\":\"http:\\/\\/cxf\""));
    }

    @Test
    public void testWriteNullValueAsString() throws Exception {
        doTestWriteNullValue(true);
    }
    @Test
    public void testWriteNullValueAsNull() throws Exception {
        doTestWriteNullValue(false);
    }

    private void doTestWriteNullValue(boolean nullAsString) throws Exception {
        JSONProvider<Book> provider = new JSONProvider<Book>() {
            protected XMLStreamWriter createWriter(Object actualObject, Class<?> actualClass,
                Type genericType, String enc, OutputStream os, boolean isCollection) throws Exception {
                return new NullWriter(
                    super.createWriter(actualObject, actualClass, genericType, enc, os, isCollection));
            }
        };
        provider.setWriteNullAsString(nullAsString);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(new Book("cxf", 123), Book.class, Book.class,
                         new Annotation[0], MediaType.APPLICATION_JSON_TYPE,
                         new MetadataMap<String, Object>(), bos);
        if (nullAsString) {
            assertTrue(bos.toString().contains("\"state\":\"null\""));
        } else {
            assertTrue(bos.toString().contains("\"state\":null"));
        }
    }

    @Test
    public void testWriteCollectionParameterDef()
        throws Exception {
        doTestWriteCollectionParameterDef(false);
    }

    @Test
    public void testWriteCollectionParameterDefAsJaxbElement()
        throws Exception {
        doTestWriteCollectionParameterDef(true);
    }

    private void doTestWriteCollectionParameterDef(boolean asJaxbElement)
        throws Exception {
        JSONProvider<List<ReportDefinition>> provider = new JSONProvider<>();
        provider.setMarshallAsJaxbElement(asJaxbElement);
        provider.setUnmarshallAsJaxbElement(asJaxbElement);
        ReportDefinition r = new ReportDefinition();
        //r.setReportName("report");
        r.addParameterDefinition(new ParameterDefinition("param"));
        List<ReportDefinition> reports = Collections.singletonList(r);

        Method m = ReportService.class.getMethod("findAllReports", new Class<?>[]{});
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(reports, m.getReturnType(), m.getGenericReturnType(),
                         new Annotation[0], MediaType.APPLICATION_JSON_TYPE,
                         new MetadataMap<String, Object>(), bos);
        @SuppressWarnings({
            "unchecked", "rawtypes"
        })
        List<ReportDefinition> reports2 = provider.readFrom((Class)m.getReturnType(), m.getGenericReturnType(),
                          new Annotation[0], MediaType.APPLICATION_JSON_TYPE,
                          new MetadataMap<String, String>(), new ByteArrayInputStream(bos.toString().getBytes()));
        assertNotNull(reports2);
        assertEquals(1, reports2.size());
        ReportDefinition rd = reports2.get(0);
        //assertEquals("report", rd.getReportName());

        List<ParameterDefinition> params = rd.getParameterList();
        assertNotNull(params);
        assertEquals(1, params.size());
        ParameterDefinition pd = params.get(0);
        assertEquals("param", pd.getName());

    }




    @Test
    public void testReadFromTags() throws Exception {
        MessageBodyReader<Tags> p = new JSONProvider<>();
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
        MessageBodyReader<TagVO> p = new JSONProvider<>();
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
        doTestReadFromQualifiedTag(".");
    }
    @Test
    public void testReadFromQualifiedTagCustomNsSep() throws Exception {
        doTestReadFromQualifiedTag("__");
    }
    private void doTestReadFromQualifiedTag(String nsSep) throws Exception {
        JSONProvider<TagVO2> p = new JSONProvider<>();
        Map<String, String> namespaceMap = new HashMap<>();
        namespaceMap.put("http://tags", "ns1");
        p.setNamespaceMap(namespaceMap);
        if (!".".equals(nsSep)) {
            p.setNamespaceSeparator(nsSep);
        }
        byte[] bytes = ("{\"ns1" + nsSep + "thetag\":{\"group\":\"b\",\"name\":\"a\"}}")
            .getBytes();
        Object tagsObject = p.readFrom(TagVO2.class, null, null,
                                       null, null, new ByteArrayInputStream(bytes));
        TagVO2 tag = (TagVO2)tagsObject;
        assertEquals("a", tag.getName());
        assertEquals("b", tag.getGroup());
    }

    @Test
    public void testReadFromUnwrappedTagWrapperName() throws Exception {
        JSONProvider<TagVO> p = new JSONProvider<>();
        p.setSupportUnwrapped(true);
        p.setWrapperName("tagVO");
        readUnwrapped(p);
    }

    @Test
    public void testReadFromUnwrappedTagMap() throws Exception {
        JSONProvider<TagVO> p = new JSONProvider<>();
        p.setSupportUnwrapped(true);

        Map<String, String> map = new HashMap<>();
        map.put(TagVO.class.getName(), "tagVO");
        p.setWrapperMap(map);
        readUnwrapped(p);
    }

    @Test
    public void testReadFromUnwrappedTagRoot() throws Exception {
        JSONProvider<TagVO> p = new JSONProvider<>();
        p.setSupportUnwrapped(true);
        readUnwrapped(p);
    }

    @Test
    public void testReadFromUnwrappedQualifiedTagRoot() throws Exception {
        JSONProvider<TagVO2> p = new JSONProvider<>();
        p.setSupportUnwrapped(true);
        Map<String, String> namespaceMap = new HashMap<>();
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
        JSONProvider<TagVO> p = new JSONProvider<>();
        TagVO tag = createTag("a", "b");

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        p.writeTo(tag, TagVO.class, TagVO.class, TagVO.class.getAnnotations(),
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);

        String s = os.toString();
        assertEquals("{\"tagVO\":{\"group\":\"b\",\"name\":\"a\"}}", s);

    }

    @Test
    public void testWriteToSingleTag2NoNs() throws Exception {
        JSONProvider<TagVO2> p = new JSONProvider<>();
        p.setIgnoreNamespaces(true);
        TagVO2 tag = createTag2("a", "b");

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        p.writeTo(tag, TagVO2.class, TagVO2.class, TagVO2.class.getAnnotations(),
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);

        String s = os.toString();
        assertEquals("{\"thetag\":{\"group\":\"b\",\"name\":\"a\"}}", s);

    }

    @Test(expected = BadRequestException.class)
    public void testIgnoreNamespacesPackageInfo() throws Exception {
        JSONProvider<Book2> p = new JSONProvider<>();
        p.setIgnoreNamespaces(true);
        Book2 book = new Book2(123);
        book.setName("CXF");

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        p.writeTo(book, Book2.class, Book2.class, Book2.class.getAnnotations(),
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);

        String s = os.toString();
        assertEquals("{\"thebook2\":{\"id\":123,\"name\":\"CXF\"}}", s);

        p.readFrom(Book2.class, null, Book2.class.getAnnotations(),
                   MediaType.APPLICATION_JSON_TYPE, null, new ByteArrayInputStream(s.getBytes()));

    }

    @Test
    public void testIgnoreNamespacesPackageInfo2() throws Exception {
        JSONProvider<Book2NoRootElement> p = new JSONProvider<>();
        p.setMarshallAsJaxbElement(true);
        p.setIgnoreNamespaces(true);
        Book2NoRootElement book = new Book2NoRootElement(123);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        p.writeTo(book, Book2NoRootElement.class, Book2NoRootElement.class,
                  Book2NoRootElement.class.getAnnotations(),
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);

        String s = os.toString();
        assertEquals("{\"book2\":{\"id\":123}}", s);

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
        Document doc = StaxUtils.read(new StringReader(os.toString()));


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
        JSONProvider<Book> p = new JSONProvider<>();
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
        JSONProvider<TagVO> p = new JSONProvider<>();
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
        doTestWriteToSingleQualifiedTag(".");
    }
    @Test
    public void testWriteToSingleQualifiedTagCustomNsSep() throws Exception {
        doTestWriteToSingleQualifiedTag("__");
    }
    private void doTestWriteToSingleQualifiedTag(String nsSep) throws Exception {
        JSONProvider<TagVO2> p = new JSONProvider<>();
        Map<String, String> namespaceMap = new HashMap<>();
        namespaceMap.put("http://tags", "ns1");
        p.setNamespaceMap(namespaceMap);
        if (!".".equals(nsSep)) {
            p.setNamespaceSeparator(nsSep);
        }
        TagVO2 tag = createTag2("a", "b");

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        p.writeTo(tag, TagVO2.class, TagVO2.class, TagVO2.class.getAnnotations(),
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);

        String s = os.toString();
        assertEquals("{\"ns1" + nsSep + "thetag\":{\"group\":\"b\",\"name\":\"a\"}}", s);
    }

    @Test
    public void testWriteToSingleQualifiedTagBadgerFish() throws Exception {
        JSONProvider<TagVO2> p = new JSONProvider<>();
        Map<String, String> namespaceMap = new HashMap<>();
        namespaceMap.put("http://tags", "ns2");
        p.setNamespaceMap(namespaceMap);

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
        JSONProvider<TagVO2> p = new JSONProvider<>();
        p.setDropRootElement(true);
        p.setIgnoreNamespaces(true);
        Map<String, String> namespaceMap = new HashMap<>();
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
    public void testDropRootElementFromDocument() throws Exception {
        JSONProvider<Document> p = new JSONProvider<>();
        Document doc = StaxUtils.read(new StringReader("<a><b>2</b></a>"));
        p.setDropRootElement(true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        p.writeTo(doc, Document.class, Document.class, new Annotation[]{},
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);

        String s = os.toString();
        assertEquals("{\"b\":2}", s);

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
        JSONProvider<List<?>> p = new JSONProvider<>();
        p.setCollectionWrapperName("{http://tags}tag");
        p.setDropCollectionWrapperElement(drop);
        p.setSerializeAsArray(serializeAsArray);
        Map<String, String> namespaceMap = new HashMap<>();
        namespaceMap.put("http://tags", "ns1");
        p.setNamespaceMap(namespaceMap);
        p.setIgnoreNamespaces(ignoreNamespaces);

        List<TagVO2> tags = new ArrayList<>();
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
    public void testIgnoreNamespaces() throws Exception {
        JSONProvider<TestBean> p = new JSONProvider<>();
        p.setIgnoreNamespaces(true);
        TestBean bean = new TestBean();
        bean.setName("a");
        bean.setId("b");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        p.writeTo(bean, TestBean.class, TestBean.class, new Annotation[0],
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        String s = os.toString();
        assertEquals("{\"testBean\":{\"@id\":\"b\",\"name\":\"a\"}}", s);

    }

    @Test
    public void testWriteUnqualifiedCollection() throws Exception {
        JSONProvider<List<Book>> p = new JSONProvider<>();
        List<Book> books = new ArrayList<>();
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
        JSONProvider<T> provider = new JSONProvider<>();
        Method m = CollectionsResource.class.getMethod(mName,
                                                       new Class[]{type});
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(
                       type, m.getGenericParameterTypes()[0],
                       new Annotation[0], MediaType.APPLICATION_JSON_TYPE,
                       new MetadataMap<String, String>(), is);
        assertNotNull(o);
        final Book b1;
        final Book b2;
        if (type.isArray()) {
            assertEquals(2, ((Book[])o).length);
            b1 = ((Book[])o)[0];
            b2 = ((Book[])o)[1];
        } else if (type == Set.class) {
            Set<Book> set = CastUtils.cast((Set<?>)o);
            List<Book> books = new ArrayList<>(new TreeSet<Book>(set));
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
        Map<String, String> namespaceMap = new HashMap<>();
        namespaceMap.put("http://tags", "ns1");
        provider.setNamespaceMap(namespaceMap);

        final Method m;
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
        final TagVO2 t1;
        final TagVO2 t2;
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
        JSONProvider<TagVO2> p = new JSONProvider<>();
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
        JAXBContext context = JAXBContext.newInstance(Books.class);
        Unmarshaller um = context.createUnmarshaller();
        JAXBElement<?> jaxbEl = um.unmarshal(new StreamSource(is), Books.class);

        JSONProvider<JAXBElement<?>> p = new JSONProvider<>();
        p.setIgnoreMixedContent(ignore);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        p.writeTo(jaxbEl, JAXBElement.class, JAXBElement.class, JAXBElement.class.getAnnotations(),
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        String s = os.toString();
        assertEquals(data, s);
    }

    @Test
    public void testWriteListOfDerivedTypes() throws Exception {
        JSONProvider<Books2> p = new JSONProvider<>();
        Map<String, String> namespaceMap = new HashMap<>();
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
        JSONProvider<Books2> p = new JSONProvider<>();
        Map<String, String> namespaceMap = new HashMap<>();
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
        JSONProvider<Books2> p = new JSONProvider<>();
        Map<String, String> namespaceMap = new HashMap<>();
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
        JSONProvider<Tags> p = new JSONProvider<>();
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
        JSONProvider<Tags> p = new JSONProvider<>();
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
    public void testWriteArrayWithStructuredArrayKeyName() throws Exception {
        JSONProvider<BeanA> p = new JSONProvider<>();
        p.setSerializeAsArray(true);
        p.setArrayKeys(Collections.singletonList("beana/beanb/name"));
        BeanA beanA = new BeanA();
        beanA.setName("beana");
        BeanB beanB = new BeanB();
        beanB.setName(Collections.singletonList("beanbArray"));
        beanA.setBeanB(beanB);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        p.writeTo(beanA, BeanA.class, BeanA.class, BeanA.class.getAnnotations(),
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);

        String s = os.toString();
        assertTrue(s.contains("\"name\":[\"beanbArray\"]"));
        assertTrue(s.contains("\"name\":\"beana\""));
    }
    @Test
    public void testWriteArrayWithStructuredArrayKeyName2() throws Exception {
        JSONProvider<BeanC> p = new JSONProvider<>();
        p.setSerializeAsArray(true);
        p.setDropRootElement(true);
        List<String> keys = new LinkedList<>();
        keys.add("beanblist");
        keys.add("beanblist/name");
        p.setArrayKeys(keys);
        BeanC beanC = new BeanC();
        beanC.setName("beanc");
        BeanB beanB = new BeanB();
        beanB.setName(Collections.singletonList("beanbArray"));
        beanC.setBeanBList(Collections.singletonList(beanB));
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        p.writeTo(beanC, BeanC.class, BeanC.class, BeanC.class.getAnnotations(),
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);

        String s = os.toString();
        assertTrue(s.contains("\"beanblist\":[{\"name\":[\"beanbArray\"]}]"));
        assertTrue(s.contains("\"name\":\"beanc\""));
    }

    @Test
    public void testWriteArrayWithStructuredArrayKeyNameDropRoot() throws Exception {
        JSONProvider<BeanA> p = new JSONProvider<>();
        p.setSerializeAsArray(true);
        p.setDropRootElement(true);
        p.setArrayKeys(Collections.singletonList("beanb/name"));
        BeanA beanA = new BeanA();
        beanA.setName("beana");
        BeanB beanB = new BeanB();
        beanB.setName(Collections.singletonList("beanbArray"));
        beanA.setBeanB(beanB);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        p.writeTo(beanA, BeanA.class, BeanA.class, BeanA.class.getAnnotations(),
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);

        String s = os.toString();
        assertTrue(s.contains("\"name\":[\"beanbArray\"]"));
        assertTrue(s.contains("\"name\":\"beana\""));
    }
    @Test
    public void testWriteArrayWithStructuredArrayKeyNameSingleBean() throws Exception {
        JSONProvider<BeanB> p = new JSONProvider<>();
        p.setSerializeAsArray(true);
        p.setArrayKeys(Collections.singletonList("beanb/name"));
        BeanB beanB = new BeanB();
        beanB.setName(Collections.singletonList("beanbArray"));
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        p.writeTo(beanB, BeanB.class, BeanB.class, BeanB.class.getAnnotations(),
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);

        String s = os.toString();
        assertTrue(s.contains("\"name\":[\"beanbArray\"]"));
    }

    @Test
    public void testWriteArrayAndNamespaceOnObject() throws Exception {
        JSONProvider<TagVO2> p = new JSONProvider<>();
        p.setIgnoreNamespaces(true);
        p.setSerializeAsArray(true);
        TagVO2 tag = new TagVO2("a", "b");
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        p.writeTo(tag, TagVO2.class, TagVO2.class, TagVO2.class.getAnnotations(),
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);

        String s = os.toString();
        assertEquals("{\"thetag\":[{\"group\":\"b\",\"name\":\"a\"}]}", s);
    }

    @Test
    public void testWriteUsingNaturalNotation() throws Exception {
        JSONProvider<Post> p = new JSONProvider<>();
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
        JSONProvider<ManyTags> p = new JSONProvider<>();
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
    public void testManyTagsEmptyArray() throws Exception {
        JSONProvider<ManyTags> p = new JSONProvider<ManyTags>() {
            protected XMLStreamWriter createWriter(Object actualObject, Class<?> actualClass,
                Type genericType, String enc, OutputStream os, boolean isCollection) throws Exception {
                return new EmptyListWriter(
                    super.createWriter(actualObject, actualClass, genericType, enc, os, isCollection));
            }
        };
        p.setSerializeAsArray(true);
        p.setArrayKeys(Collections.singletonList("list"));
        p.setIgnoreEmptyArrayValues(true);
        Tags tags = new Tags();
        tags.addTag(createTag("a", "b"));
        ManyTags many = new ManyTags();
        many.setTags(tags);

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        p.writeTo(many, ManyTags.class, ManyTags.class, ManyTags.class.getAnnotations(),
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);

        String s = os.toString();
        assertEquals("{\"ManyTags\":{\"tags\":{\"list\":[]}}}", s);
    }

    @Test
    public void testInDropElement() throws Exception {
        String data = "{\"Extra\":{\"ManyTags\":{\"tags\":{\"list\":[{\"group\":\"b\",\"name\":\"a\"}]}}}}";
        JSONProvider<ManyTags> provider = new JSONProvider<>();
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
        JSONProvider<ManyTags> provider = new JSONProvider<>();
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
        JSONProvider<TagVO2> provider = new JSONProvider<>();
        Map<String, String> map = new HashMap<>();
        map.put(keyValue, "{http://tags}thetag");
        Map<String, String> nsmap = new HashMap<>();
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
        JSONProvider<TagVO2Holder> provider = new JSONProvider<>();
        Map<String, String> map = new HashMap<>();
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
        JSONProvider<TagVO2Holder> provider = new JSONProvider<>();
        Map<String, String> map = new HashMap<>();
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
    public void testAttributesAsElementsWithTransform() throws Exception {
        JSONProvider<TagVO2Holder> provider = new JSONProvider<TagVO2Holder>() {
            protected XMLStreamWriter createTransformWriterIfNeeded(XMLStreamWriter writer,
                                                                    OutputStream os,
                                                                    boolean dropAtXmlLevel) {
                return TransformUtils.createTransformWriterIfNeeded(writer, os,
                                                                    Collections.emptyMap(),
                                                                    null,
                                                                    Collections.emptyMap(),
                                                                    true,
                                                                    null);
            }
        };
        provider.setIgnoreNamespaces(true);
        TagVO2 tag = new TagVO2("A", "B");
        tag.setAttrInt(123);
        TagVO2Holder holder = new TagVO2Holder();
        holder.setTag(tag);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(holder, TagVO2Holder.class, TagVO2Holder.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected =
            "{\"tagholder\":{\"attr\":\"attribute\",\"thetag\":{\"attrInt\":123,\"group\":\"B\",\"name\":\"A\"}}}";
        assertEquals(expected, bos.toString());
    }

    @Test
    public void testAttributesAsElementsWithInteger() throws Exception {
        JSONProvider<TagVO2Holder> provider = new JSONProvider<>();
        provider.setAttributesToElements(true);
        provider.setIgnoreNamespaces(true);
        TagVO2 tag = new TagVO2("A", "B");
        tag.setAttrInt(123);
        TagVO2Holder holder = new TagVO2Holder();
        holder.setTag(tag);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(holder, TagVO2Holder.class, TagVO2Holder.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected =
            "{\"tagholder\":{\"attr\":\"attribute\",\"thetag\":{\"attrInt\":123,\"group\":\"B\",\"name\":\"A\"}}}";
        assertEquals(expected, bos.toString());
    }

    @Test
    public void testOutAttributesAsElementsForList() throws Exception {

        //Provider
        JSONProvider<List<?>> provider = new JSONProvider<>();
        provider.setCollectionWrapperName("tagholders");
        Map<String, String> map = new HashMap<>();
        map.put("{http://tags}*", "*");
        provider.setOutTransformElements(map);
        provider.setAttributesToElements(true);

        //data setup
        TagVO2 tag = new TagVO2("A", "B");
        TagVO2Holder holder = new TagVO2Holder();
        holder.setTag(tag);
        List<TagVO2Holder> list = new ArrayList<>();
        list.add(holder);

        //ParameterizedType required for Lists of Objects
        ParameterizedType type = new ParameterizedType() {
            public Type getRawType() {
                return List.class;
            }
            public Type getOwnerType() {
                return null;
            }
            public Type[] getActualTypeArguments() {
                return new Type[] {TagVO2Holder.class};
            }
        };

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(list, ArrayList.class, type,
            new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected =
            "{\"tagholders\":["
            + "{\"attr\":\"attribute\",\"thetag\":{\"group\":\"B\",\"name\":\"A\"}}"
            + "]}";
        assertEquals(expected, bos.toString());
    }

    private void readTagVOAfterTransform(String data, String keyValue) throws Exception {
        JSONProvider<TagVO> provider = new JSONProvider<>();
        Map<String, String> map = new HashMap<>();
        map.put(keyValue, "tagVO");
        provider.setInTransformElements(map);
        Map<String, String> nsmap = new HashMap<>();
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
    public void testDropElementsIgnored() throws Exception {
        JSONProvider<ManyTags> provider = new JSONProvider<>();
        List<String> list = new ArrayList<>();
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
    }

    @Test
    public void testDropElements() throws Exception {
        JSONProvider<ManyTags> provider = new JSONProvider<>();
        List<String> list = new ArrayList<>();
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
            new JSONProvider<>();
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
        JSONProvider<TagVO2> provider = new JSONProvider<>();
        List<String> list = new ArrayList<>();
        list.add("{http://tags}thetag");
        provider.setOutDropElements(list);
        Map<String, String> map = new HashMap<>();
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
        JSONProvider<TagVO> provider = new JSONProvider<>();
        Map<String, String> map = new HashMap<>();
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
        JSONProvider<TagVO> provider = new JSONProvider<>();
        Map<String, String> map = new HashMap<>();
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
        JSONProvider<TagVO2> provider = new JSONProvider<>();

        Map<String, String> namespaceMap = new HashMap<>();
        namespaceMap.put("http://tags", "ns2");
        provider.setNamespaceMap(namespaceMap);

        Map<String, String> map = new HashMap<>();
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
        JSONProvider<TagVO2> provider = new JSONProvider<>();

        Map<String, String> namespaceMap = new HashMap<>();
        namespaceMap.put("http://tags", "ns2");
        provider.setNamespaceMap(namespaceMap);

        Map<String, String> map = new HashMap<>();
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
        JSONProvider<TagVO2> provider = new JSONProvider<>();
        Map<String, String> namespaceMap = new HashMap<>();
        namespaceMap.put("http://tags", "ns2");
        provider.setNamespaceMap(namespaceMap);
        Map<String, String> map = new HashMap<>();
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
        JSONProvider<TagVO2> provider = new JSONProvider<>();
        Map<String, String> map = new HashMap<>();
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
        JSONProvider<TagVO> provider = new JSONProvider<>();
        Map<String, String> namespaceMap = new HashMap<>();
        namespaceMap.put("http://tags", "ns1");
        Map<String, String> map = new HashMap<>();
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
        JSONProvider<TagVO> provider = new JSONProvider<>();
        Map<String, String> map = new HashMap<>();
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
        JSONProvider<Base1> provider = new JSONProvider<>();
        provider.setMarshallAsJaxbElement(true);
        Map<String, String> namespaceMap = new HashMap<>();
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
        JSONProvider<Base1> provider = new JSONProvider<>();
        provider.setUnmarshallAsJaxbElement(true);
        Map<String, String> namespaceMap = new HashMap<>();
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
        private List<Comment> comments = new ArrayList<>();
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
        private List<Book> books = new LinkedList<>();

        @XmlElement(name = "book")
        @XmlElementWrapper(name = "books")
        public List<Book> getBooks() {
            return books;
        }
    }

    @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
    @XmlRootElement(name = "theBook")
    public static class TheBook {
        private List<String> name = new LinkedList<>();

        @XmlElement(name = "Name")
        @XmlElementWrapper(name = "Names")
        public List<String> getName() {
            return name;
        }
    }

    @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
    @XmlRootElement(name = "store", namespace = "http://superbooks")
    public static class QualifiedStore {
        private List<Book2> books = new LinkedList<>();

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

    interface ReportService {

        List<ReportDefinition> findAllReports();
        ReportDefinition findReport();

    }

    public static class ParameterDefinition {
        private String name;
        public ParameterDefinition() {
        }
        public ParameterDefinition(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    @XmlRootElement
    public static class ReportDefinition {
        private String reportName;

        private List<ParameterDefinition> parameterList;

        public ReportDefinition() {

        }

        public ReportDefinition(String reportName) {
            this.reportName = reportName;
        }

        public String getReportName() {
            return reportName;
        }

        public void setReportName(String reportName) {
            this.reportName = reportName;
        }

        public List<ParameterDefinition> getParameterList() {
            return parameterList;
        }

        public void setParameterList(List<ParameterDefinition> parameterList) {
            this.parameterList = parameterList;
        }

        public void addParameterDefinition(ParameterDefinition parameterDefinition) {
            if (parameterList == null) {
                parameterList = new ArrayList<>();
            }
            parameterList.add(parameterDefinition);
        }
    }

    private static class EmptyListWriter extends DelegatingXMLStreamWriter {
        private int count;
        EmptyListWriter(XMLStreamWriter writer) {
            super(writer);
        }

        public void writeCharacters(String text) throws XMLStreamException {
        }

        public void writeCharacters(char[] text, int arg1, int arg2) throws XMLStreamException {
        }

        public void writeStartElement(String p, String local, String uri) throws XMLStreamException {
            if ("group".equals(local) || "name".equals(local)) {
                count++;
            } else {
                super.writeStartElement(p, local, uri);
            }
        }



        public void writeEndElement() throws XMLStreamException {
            if (count == 0) {
                super.writeEndElement();
            } else {
                count--;
            }
        }
    }

    private static class NullWriter extends DelegatingXMLStreamWriter {
        NullWriter(XMLStreamWriter writer) {
            super(writer);
        }

        public void writeCharacters(String text) throws XMLStreamException {
            if (StringUtils.isEmpty(text.trim())) {
                super.writeCharacters(null);
            } else {
                super.writeCharacters(text);
            }
        }

        public void writeCharacters(char[] text, int arg1, int arg2) throws XMLStreamException {
            String str = new String(text);
            if (StringUtils.isEmpty(str.trim())) {
                super.writeCharacters(null);
            } else {
                super.writeCharacters(text, arg1, arg2);
            }
        }
    }

    @XmlRootElement(namespace = "http://testbean")
    public static class TestBean {
        private String name;
        private String id;
        public TestBean() {

        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getId() {
            return id;
        }
        @XmlAttribute(namespace = "http://testbean")
        public void setId(String id) {
            this.id = id;
        }
    }
    @XmlRootElement(name = "beana")
    public static class BeanA {
        private String name;
        private BeanB beanB;
        public BeanA() {

        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public BeanB getBeanB() {
            return beanB;
        }
        @XmlElement(name = "beanb")
        public void setBeanB(BeanB beanB) {
            this.beanB = beanB;
        }
    }
    @XmlRootElement(name = "beanb")
    public static class BeanB {
        private List<String> name;
        public BeanB() {

        }
        public List<String> getName() {
            return name;
        }
        public void setName(List<String> name) {
            this.name = name;
        }
    }
    @XmlRootElement(name = "beanc")
    public static class BeanC {
        private String name;
        private List<BeanB> beanBList;
        public BeanC() {

        }
        public List<BeanB> getBeanBList() {
            return beanBList;
        }
        @XmlElement(name = "beanblist")
        public void setBeanBList(List<BeanB> beanBList) {
            this.beanBList = beanBList;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }

    }
}