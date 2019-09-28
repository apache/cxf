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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.validation.Schema;

import org.w3c.dom.Node;

import org.xml.sax.ContentHandler;

import org.apache.cxf.annotations.SchemaValidation;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.jaxrs.fortest.jaxb.jaxbelement.ParamJAXBElement;
import org.apache.cxf.jaxrs.fortest.jaxb.jaxbelement.ParamType;
import org.apache.cxf.jaxrs.fortest.jaxb.packageinfo.Book2;
import org.apache.cxf.jaxrs.fortest.jaxb.packageinfo.Book2NoRootElement;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.index.TestBean;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.resources.BookNoRootElement;
import org.apache.cxf.jaxrs.resources.BookStore;
import org.apache.cxf.jaxrs.resources.CollectionsResource;
import org.apache.cxf.jaxrs.resources.ManyTags;
import org.apache.cxf.jaxrs.resources.SuperBook;
import org.apache.cxf.jaxrs.resources.TagVO;
import org.apache.cxf.jaxrs.resources.TagVO2;
import org.apache.cxf.jaxrs.resources.Tags;
import org.apache.cxf.jaxrs.utils.ParameterizedCollectionType;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.easymock.EasyMock;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JAXBElementProviderTest {

    @Test
    public void testReadFromISO() throws Exception {

        String eWithAcute = "\u00E9";
        String nameStringUTF16 = "F" + eWithAcute + "lix";
        String bookStringUTF16 = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"
                                 + "<Book><name>" + nameStringUTF16 + "</name></Book>";

        byte[] iso88591bytes = bookStringUTF16.getBytes("ISO-8859-1");

        JAXBElementProvider<Book> p = new JAXBElementProvider<>();
        Book book = p.readFrom(Book.class, null,
                new Annotation[]{},
                MediaType.valueOf(MediaType.APPLICATION_XML), null,
                new ByteArrayInputStream(iso88591bytes));
        assertEquals(book.getName(), nameStringUTF16);
    }

    @Test
    public void testReadChineeseChars() throws Exception {

        String nameStringUTF16 = "中文";

        String bookStringUTF16 = "<Book><name>" + nameStringUTF16 + "</name></Book>";
        JAXBElementProvider<Book> p = new JAXBElementProvider<>();
        Book book = p.readFrom(Book.class, null,
                new Annotation[]{},
                MediaType.valueOf(MediaType.APPLICATION_XML + ";charset=UTF-8"), null,
                new ByteArrayInputStream(bookStringUTF16.getBytes(StandardCharsets.UTF_8)));
        assertEquals(book.getName(), nameStringUTF16);
    }

    @Test
    public void testSingleJAXBContext() throws Exception {
        ClassResourceInfo cri =
            ResourceUtils.createClassResourceInfo(JAXBResource.class, JAXBResource.class, true, true);
        JAXBElementProvider<Book> provider = new JAXBElementProvider<>();
        provider.setSingleJaxbContext(true);
        provider.init(Collections.singletonList(cri));
        JAXBContext bookContext = provider.getJAXBContext(Book.class, Book.class);
        assertNotNull(bookContext);
        JAXBContext superBookContext = provider.getJAXBContext(SuperBook.class, SuperBook.class);
        assertSame(bookContext, superBookContext);
        JAXBContext book2Context = provider.getJAXBContext(Book2.class, Book2.class);
        assertSame(bookContext, book2Context);
    }

    @Test
    public void testExtraClass() throws Exception {
        ClassResourceInfo cri =
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        JAXBElementProvider<SuperBook> provider = new JAXBElementProvider<>();
        provider.setSingleJaxbContext(true);
        provider.setExtraClass(new Class[]{SuperBook.class});
        provider.init(Collections.singletonList(cri));
        JAXBContext bookContext = provider.getJAXBContext(Book.class, Book.class);
        assertNotNull(bookContext);
        JAXBContext superBookContext = provider.getJAXBContext(SuperBook.class, SuperBook.class);
        assertSame(bookContext, superBookContext);
    }

    @Test
    public void testExtraClassWithoutSingleContext() throws Exception {
        ClassResourceInfo cri =
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        JAXBElementProvider<SuperBook> provider = new JAXBElementProvider<>();
        provider.setExtraClass(new Class[]{SuperBook.class});
        provider.init(Collections.singletonList(cri));
        JAXBContext bookContext = provider.getJAXBContext(Book.class, Book.class);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bookContext.createMarshaller().marshal(new SuperBook("name", 1L, 2L), os);
        SuperBook book = (SuperBook)bookContext.createUnmarshaller()
                             .unmarshal(new ByteArrayInputStream(os.toByteArray()));
        assertEquals(2L, book.getSuperId());
    }

    @Test
    public void testExtraClassWithGenerics() throws Exception {
        JAXBElementProvider<?> provider = new JAXBElementProvider<>();
        provider.setExtraClass(new Class[]{XmlObject.class});
        testXmlList(provider);
    }

    @Test
    public void testExtraClassWithGenericsAndSingleContext() throws Exception {
        ClassResourceInfo cri =
            ResourceUtils.createClassResourceInfo(XmlListResource.class, XmlListResource.class, true, true);
        JAXBElementProvider<?> provider = new JAXBElementProvider<>();
        provider.setSingleJaxbContext(true);
        provider.setExtraClass(new Class[]{XmlObject.class});
        provider.init(Collections.singletonList(cri));
        testXmlList(provider);

    }

    @Test
    public void testGenericsAndSingleContext() throws Exception {
        ClassResourceInfo cri =
            ResourceUtils.createClassResourceInfo(XmlListResource.class, XmlListResource.class, true, true);
        JAXBElementProvider<?> provider = new JAXBElementProvider<>();
        provider.setSingleJaxbContext(true);
        provider.init(Collections.singletonList(cri));
        testXmlList(provider);
    }

    @Test
    public void testGenericsAndSingleContext2() throws Exception {
        ClassResourceInfo cri =
            ResourceUtils.createClassResourceInfo(XmlListResource.class, XmlListResource.class, true, true);
        JAXBElementProvider<?> provider = new JAXBElementProvider<>();
        provider.setSingleJaxbContext(true);
        provider.init(Collections.singletonList(cri));

        List<org.apache.cxf.jaxrs.fortest.jaxb.SuperBook> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            org.apache.cxf.jaxrs.fortest.jaxb.SuperBook o =
                new org.apache.cxf.jaxrs.fortest.jaxb.SuperBook();
            o.setName("name #" + i);
            list.add(o);
        }
        XmlList<org.apache.cxf.jaxrs.fortest.jaxb.SuperBook> xmlList = new XmlList<>(list);

        Method m = XmlListResource.class.getMethod("testJaxb2", new Class[]{});
        JAXBContext context = provider.getJAXBContext(m.getReturnType(), m.getGenericReturnType());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        context.createMarshaller().marshal(xmlList, os);
        @SuppressWarnings("unchecked")
        XmlList<org.apache.cxf.jaxrs.fortest.jaxb.SuperBook> list2 =
            (XmlList<org.apache.cxf.jaxrs.fortest.jaxb.SuperBook>)context.createUnmarshaller()
                .unmarshal(new ByteArrayInputStream(os.toByteArray()));

        List<org.apache.cxf.jaxrs.fortest.jaxb.SuperBook> actualList = list2.getList();
        assertEquals(10, actualList.size());
        for (int i = 0; i < 10; i++) {
            org.apache.cxf.jaxrs.fortest.jaxb.SuperBook object = actualList.get(i);
            assertEquals("name #" + i, object.getName());
        }
    }

    @SuppressWarnings("unchecked")
    private void testXmlList(JAXBElementProvider<?> provider) throws Exception {

        List<XmlObject> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            MyObject o = new MyObject();
            o.setName("name #" + i);
            list.add(new XmlObject(o));
        }
        XmlList<XmlObject> xmlList = new XmlList<>(list);

        Method m = XmlListResource.class.getMethod("testJaxb", new Class[]{});
        JAXBContext context = provider.getJAXBContext(m.getReturnType(), m.getGenericReturnType());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        context.createMarshaller().marshal(xmlList, os);
        XmlList<XmlObject> list2 = (XmlList<XmlObject>)context.createUnmarshaller()
            .unmarshal(new ByteArrayInputStream(os.toByteArray()));

        List<XmlObject> actualList = list2.getList();
        assertEquals(10, actualList.size());
        for (int i = 0; i < 10; i++) {
            XmlObject object = actualList.get(i);
            assertEquals("name #" + i, object.getAttribute().getName());
        }
    }

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
    public void testIsSupportedWithJaxbIndex() {
        JAXBElementProvider<TestBean> provider = new JAXBElementProvider<>();
        assertTrue(provider.isSupported(TestBean.class, TestBean.class, new Annotation[]{}));
    }

    @Test
    public void testIsWriteableJAXBElements() throws Exception {
        testIsWriteableCollection("getBookElements");
    }

    private void testIsWriteableCollection(String mName) throws Exception {
        JAXBElementProvider<?> provider = new JAXBElementProvider<>();
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

    public <T> void doWriteUnqualifiedCollection(boolean setName, String mName,
                                                 String setterName,
                                                 Class<T> type) throws Exception {
        JAXBElementProvider<T> provider = new JAXBElementProvider<>();
        if (setName) {
            provider.setCollectionWrapperName("Books");
        }
        List<Book> books = new ArrayList<>();
        books.add(new Book("CXF in Action", 123L));
        books.add(new Book("CXF Rocks", 124L));
        @SuppressWarnings("unchecked")
        T o = (T)(type.isArray() ? books.toArray() : type == Set.class
            ? new HashSet<>(books) : books);

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
        JAXBElementProvider<Book> provider = new JAXBElementProvider<>();
        provider.setJaxbElementClassNames(Collections.singletonList(Book.class.getName()));
        Book b = new SuperBook("CXF in Action", 123L, 124L);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(b, Book.class, Book.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        readSuperBook(bos.toString(), true);
    }

    @Test
    public void testWriteDerivedType2() throws Exception {
        JAXBElementProvider<Book> provider = new JAXBElementProvider<>();
        Book b = new SuperBook("CXF in Action", 123L, 124L);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(b, Book.class, Book.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);

        readSuperBook(bos.toString(), false);
    }

    @Test
    public void testWriteWithCustomPrefixes() throws Exception {
        JAXBElementProvider<TagVO2> provider = new JAXBElementProvider<>();
        provider.setNamespacePrefixes(
            Collections.singletonMap("http://tags", "prefix"));
        TagVO2 tag = new TagVO2("a", "b");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO2.class, TagVO2.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        assertTrue(bos.toString().contains("prefix:thetag"));
    }

    @Test
    public void testWriteCollectionWithCustomPrefixes() throws Exception {
        JAXBElementProvider<List<TagVO2>> provider = new JAXBElementProvider<>();
        provider.setNamespacePrefixes(
            Collections.singletonMap("http://tags", "prefix"));
        TagVO2 tag = new TagVO2("a", "b");
        List<TagVO2> tags = Collections.singletonList(tag);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tags, List.class, new ParameterizedCollectionType(TagVO2.class),
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        assertTrue(bos.toString().contains("prefix:thetag"));
        assertFalse(bos.toString().contains("ns1:thetag"));
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
        JAXBElementProvider<org.apache.cxf.jaxrs.fortest.jaxb.SuperBook> provider
            = new JAXBElementProvider<>();
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
        JAXBElementProvider<List<org.apache.cxf.jaxrs.fortest.jaxb.SuperBook>> provider
            = new JAXBElementProvider<>();

        Map<String, String> prefixes = new HashMap<>();
        prefixes.put("http://superbooks", "ns1");
        prefixes.put("http://books", "ns2");
        provider.setNamespacePrefixes(prefixes);

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
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<ns1:SuperBooks xmlns:ns1=\"http://superbooks\">"
            + "<ns1:SuperBook xmlns:ns2=\"http://books\" xmlns:ns1=\"http://superbooks\"><id>123</id>"
            + "<name>CXF in Action</name><superId>124</superId></ns1:SuperBook></ns1:SuperBooks>";
        assertEquals(expected, bos.toString());
    }


    @Test
    public void testWriteWithoutXmlRootElementDerived() throws Exception {
        JAXBElementProvider<org.apache.cxf.jaxrs.fortest.jaxb.Book> provider
            = new JAXBElementProvider<>();
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
    public void testWriteWithoutXmlRootElementWithPackageInfo() throws Exception {
        JAXBElementProvider<Book2NoRootElement> provider = new JAXBElementProvider<>();
        provider.setMarshallAsJaxbElement(true);
        Book2NoRootElement book = new Book2NoRootElement(333);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(book, Book2NoRootElement.class,
                         Book2NoRootElement.class,
                         new Annotation[0], MediaType.TEXT_XML_TYPE,
                         new MetadataMap<String, Object>(), bos);
        assertTrue(bos.toString().contains("book2"));
        assertTrue(bos.toString().contains("http://superbooks"));
        provider.setUnmarshallAsJaxbElement(true);

        ByteArrayInputStream is = new ByteArrayInputStream(bos.toByteArray());
        Book2NoRootElement book2 = provider.readFrom(
                       Book2NoRootElement.class,
                       Book2NoRootElement.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        assertEquals(book2.getId(), book.getId());
    }

    @Test
    public void testWriteWithXmlRootElementAndPackageInfo() throws Exception {
        JAXBElementProvider<Book2> provider = new JAXBElementProvider<>();
        Book2 book = new Book2(333);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(book, Book2.class,
                         Book2.class,
                         new Annotation[0], MediaType.TEXT_XML_TYPE,
                         new MetadataMap<String, Object>(), bos);
        assertTrue(bos.toString().contains("thebook2"));
        assertTrue(bos.toString().contains("http://superbooks"));
        ByteArrayInputStream is = new ByteArrayInputStream(bos.toByteArray());
        Book2 book2 = provider.readFrom(
                       Book2.class,
                       Book2.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        assertEquals(book2.getId(), book.getId());
    }

    @Test
    public void testWriteWithoutXmlRootElementObjectFactory() throws Exception {
        JAXBElementProvider<org.apache.cxf.jaxrs.fortest.jaxb.SuperBook2> provider
            = new JAXBElementProvider<>();
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
        JAXBElementProvider<org.apache.cxf.jaxrs.fortest.jaxb.SuperBook2> provider2
            = new JAXBElementProvider<>();
        ByteArrayInputStream is = new ByteArrayInputStream(bos.toByteArray());
        org.apache.cxf.jaxrs.fortest.jaxb.SuperBook2 book = provider2.readFrom(
                       org.apache.cxf.jaxrs.fortest.jaxb.SuperBook2.class,
                       org.apache.cxf.jaxrs.fortest.jaxb.SuperBook2.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        assertEquals(124L, book.getSuperId());
    }

    @Test
    public void testObjectFactoryExtraClass() throws Exception {
        JAXBElementProvider<org.apache.cxf.jaxrs.fortest.jaxb.Book> provider
            = new JAXBElementProvider<>();
        provider.setExtraClass(new Class[]{org.apache.cxf.jaxrs.fortest.jaxb.index.SuperBook3.class});

        doTestObjectFactoryExtraClass(provider);
    }

    @Test
    public void testObjectFactoryExtraClass2() throws Exception {
        JAXBElementProvider<org.apache.cxf.jaxrs.fortest.jaxb.Book> provider
            = new JAXBElementProvider<>();
        provider.setExtraClass(new Class[] {
            org.apache.cxf.jaxrs.fortest.jaxb.Book.class,
            org.apache.cxf.jaxrs.fortest.jaxb.index.SuperBook3.class});
        provider.setSingleJaxbContext(true);
        provider.setUseSingleContextForPackages(true);
        provider.init(null);

        doTestObjectFactoryExtraClass(provider);
    }

    private void doTestObjectFactoryExtraClass(JAXBElementProvider<org.apache.cxf.jaxrs.fortest.jaxb.Book> provider)
        throws Exception {

        org.apache.cxf.jaxrs.fortest.jaxb.index.SuperBook3 b =
            new org.apache.cxf.jaxrs.fortest.jaxb.index.SuperBook3("CXF in Action", 123L, 124L);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(b, org.apache.cxf.jaxrs.fortest.jaxb.Book.class,
                         org.apache.cxf.jaxrs.fortest.jaxb.Book.class,
                         new Annotation[0], MediaType.TEXT_XML_TYPE,
                         new MetadataMap<String, Object>(), bos);
        JAXBElementProvider<org.apache.cxf.jaxrs.fortest.jaxb.Book> provider2
            = new JAXBElementProvider<>();
        provider2.setExtraClass(new Class[]{org.apache.cxf.jaxrs.fortest.jaxb.index.SuperBook3.class});

        ByteArrayInputStream is = new ByteArrayInputStream(bos.toByteArray());
        org.apache.cxf.jaxrs.fortest.jaxb.index.SuperBook3 book =
            (org.apache.cxf.jaxrs.fortest.jaxb.index.SuperBook3)provider2.readFrom(
                       org.apache.cxf.jaxrs.fortest.jaxb.Book.class,
                       org.apache.cxf.jaxrs.fortest.jaxb.Book.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        assertEquals(124L, book.getSuperId());
    }


    private void readSuperBook(String data, boolean xsiTypeExpected) throws Exception {
        if (xsiTypeExpected) {
            assertTrue(data.contains("xsi:type"));
        }
        JAXBElementProvider<SuperBook> provider = new JAXBElementProvider<>();
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        SuperBook book = provider.readFrom(
                       SuperBook.class, SuperBook.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        assertEquals(124L, book.getSuperId());
    }

    @Test
    public void testReadSuperBookWithJaxbElement() throws Exception {
        final String data = "<BookNoRootElement>"
            + "<name>superbook</name><id>111</id>"
            + "</BookNoRootElement>";
        JAXBElementProvider<BookNoRootElement> provider
            = new JAXBElementProvider<>();
        provider.setUnmarshallAsJaxbElement(true);
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        BookNoRootElement book = provider.readFrom(
                       BookNoRootElement.class, BookNoRootElement.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        assertEquals(111L, book.getId());
        assertEquals("superbook", book.getName());
    }

    @Test
    public void testReadSuperBookWithJaxbElementAndTransform() throws Exception {
        final String data = "<BookNoRootElement xmlns=\"http://books\">"
            + "<name>superbook</name><id>111</id>"
            + "</BookNoRootElement>";
        JAXBElementProvider<BookNoRootElement> provider = new JAXBElementProvider<>();
        provider.setUnmarshallAsJaxbElement(true);
        provider.setInTransformElements(Collections.singletonMap(
             "{http://books}*", ""));
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        BookNoRootElement book = provider.readFrom(
                       BookNoRootElement.class, BookNoRootElement.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        assertEquals(111L, book.getId());
        assertEquals("superbook", book.getName());
    }

    private void readSuperBook2(String data, boolean unmarshalAsJaxbElement) throws Exception {
        JAXBElementProvider<org.apache.cxf.jaxrs.fortest.jaxb.SuperBook> provider
            = new JAXBElementProvider<>();
        if (!unmarshalAsJaxbElement) {
            provider.setJaxbElementClassMap(Collections.singletonMap(
                org.apache.cxf.jaxrs.fortest.jaxb.SuperBook.class.getName(), "SuperBook"));
        } else {
            provider.setUnmarshallAsJaxbElement(true);
        }
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        org.apache.cxf.jaxrs.fortest.jaxb.SuperBook book = provider.readFrom(
                       org.apache.cxf.jaxrs.fortest.jaxb.SuperBook.class,
                       org.apache.cxf.jaxrs.fortest.jaxb.SuperBook.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        assertEquals(124L, book.getSuperId());
    }

    private void doTestWriteJAXBCollection(String mName) throws Exception {
        JAXBElementProvider<List<?>> provider = new JAXBElementProvider<>();
        List<JAXBElement<Book>> books = new ArrayList<>();
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

    @SuppressWarnings({"rawtypes", "unchecked" })
    @Test
    public void testReadJAXBElement() throws Exception {
        String xml = "<Book><id>123</id><name>CXF in Action</name></Book>";
        JAXBElementProvider<JAXBElement> provider = new JAXBElementProvider<>();
        JAXBElement<Book> jaxbElement = provider.readFrom(JAXBElement.class, Book.class,
             new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(),
             new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        Book book = jaxbElement.getValue();
        assertEquals(123L, book.getId());
        assertEquals("CXF in Action", book.getName());

    }

    @Test
    public void testReadParamJAXBElement() throws Exception {
        String xml = "<param xmlns=\"http://jaxbelement/10\">"
            + "<filter name=\"foo\"/><comment>a</comment></param>";
        JAXBElementProvider<ParamJAXBElement> provider = new JAXBElementProvider<>();
        ParamJAXBElement jaxbElement = provider.readFrom(ParamJAXBElement.class, ParamJAXBElement.class,
             new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(),
             new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        ParamType param = jaxbElement.getValue();
        assertEquals("a", param.getComment());
    }

    @Test
    public void testWriteQualifiedCollection() throws Exception {
        JAXBElementProvider<List<TagVO2>> provider = new JAXBElementProvider<>();
        provider.setCollectionWrapperName("{http://tags}tags");
        List<TagVO2> tags = new ArrayList<>();
        tags.add(new TagVO2("A", "B"));
        tags.add(new TagVO2("C", "D"));
        Method m = CollectionsResource.class.getMethod("getTags", new Class[0]);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tags, m.getReturnType(), m.getGenericReturnType(),
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        doReadQualifiedCollection(bos.toString(), false);
    }

    @Test
    public void testInNsElementFromLocal() throws Exception {
        String data = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<thetag><group>B</group><name>A</name></thetag>";
        readTagVO2AfterTransform(data, "thetag");
    }

    @Test
    public void testInNsElementFromNsElement() throws Exception {
        String data = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<t:thetag2 xmlns:t=\"http://bar\"><group>B</group><name>A</name></t:thetag2>";
        readTagVO2AfterTransform(data, "{http://bar}thetag2");
    }

    @Test
    public void testInAppendElementNoNs() throws Exception {
        String data = "<tags><list><group>b</group><name>a</name></list></tags>";
        readAppendElementsNoNs(data, Collections.singletonMap("tags", "ManyTags"));
    }

    @Test
    public void testInAppendElementNoNs2() throws Exception {
        String data = "<ManyTags><list><group>b</group><name>a</name></list></ManyTags>";
        readAppendElementsNoNs(data, Collections.singletonMap("list", "tags"));
    }

    private void readAppendElementsNoNs(String data, Map<String, String> appendMap) throws Exception {
        JAXBElementProvider<ManyTags> provider = new JAXBElementProvider<>();
        provider.setInAppendElements(appendMap);
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(ManyTags.class, ManyTags.class,
                      new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        ManyTags holder = (ManyTags)o;
        assertNotNull(holder);
        TagVO tag = holder.getTags().getTags().get(0);
        assertEquals("a", tag.getName());
        assertEquals("b", tag.getGroup());
    }


    @Test
    public void testInDropElement() throws Exception {
        String data = "<Extra><ManyTags><tags><list><group>b</group><name>a</name></list></tags>"
            + "</ManyTags></Extra>";
        JAXBElementProvider<ManyTags> provider = new JAXBElementProvider<>();
        provider.setInDropElements(Collections.singletonList("Extra"));
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(ManyTags.class, ManyTags.class,
                      new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        ManyTags holder = (ManyTags)o;
        assertNotNull(holder);
        TagVO tag = holder.getTags().getTags().get(0);
        assertEquals("a", tag.getName());
        assertEquals("b", tag.getGroup());
    }

    @Test
    public void testInLocalFromLocal() throws Exception {
        String data = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<thetag><group>B</group><name>A</name></thetag>";
        readTagVOAfterTransform(data, "thetag");
    }


    @Test
    public void testInLocalFromNsElement() throws Exception {
        String data = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<t:thetag2 xmlns:t=\"http://bar\"><group>B</group><name>A</name></t:thetag2>";
        readTagVOAfterTransform(data, "{http://bar}thetag2");
    }


    private void readTagVO2AfterTransform(String data, String keyValue) throws Exception {
        JAXBElementProvider<TagVO2> provider = new JAXBElementProvider<>();
        Map<String, String> map = new HashMap<>();
        map.put(keyValue, "{http://tags}thetag");
        provider.setInTransformElements(map);
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(TagVO2.class, TagVO2.class,
                      new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        TagVO2 tag2 = (TagVO2)o;
        assertEquals("A", tag2.getName());
        assertEquals("B", tag2.getGroup());
    }

    @Test
    public void testInNsElementsFromLocals() throws Exception {
        String data = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<tagholder><thetag><group>B</group><name>A</name></thetag></tagholder>";
        JAXBElementProvider<TagVO2Holder> provider = new JAXBElementProvider<>();
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
    public void testInNsElementsFromLocalsWildcard() throws Exception {
        String data = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<tagholder><thetag><group>B</group><name>A</name></thetag></tagholder>";
        JAXBElementProvider<TagVO2Holder> provider = new JAXBElementProvider<>();
        Map<String, String> map = new LinkedHashMap<>();
        map.put("group", "group");
        map.put("name", "name");
        map.put("*", "{http://tags}*");
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
    public void testInNsElementsFromLocalsWildcard2() throws Exception {
        String data = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<ns2:tagholder xmlns:ns2=\"http://tags2\" attr=\"attribute\"><ns2:thetag><group>B</group>"
            + "<name>A</name></ns2:thetag></ns2:tagholder>";
        JAXBElementProvider<TagVO2Holder> provider = new JAXBElementProvider<>();
        Map<String, String> map = new LinkedHashMap<>();
        map.put("group", "group");
        map.put("name", "name");
        map.put("{http://tags2}*", "{http://tags}*");
        provider.setInTransformElements(map);
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(TagVO2Holder.class, TagVO2Holder.class,
                      new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        TagVO2Holder holder = (TagVO2Holder)o;
        TagVO2 tag2 = holder.getTagValue();
        assertEquals("A", tag2.getName());
        assertEquals("B", tag2.getGroup());
    }

    private void readTagVOAfterTransform(String data, String keyValue) throws Exception {
        JAXBElementProvider<TagVO> provider = new JAXBElementProvider<>();
        Map<String, String> map = new HashMap<>();
        map.put(keyValue, "tagVO");
        provider.setInTransformElements(map);
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(TagVO.class, TagVO.class,
                      new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        TagVO tag2 = (TagVO)o;
        assertEquals("A", tag2.getName());
        assertEquals("B", tag2.getGroup());
    }

    @Test
    public void testOutAttributesAsElements() throws Exception {
        JAXBElementProvider<TagVO2Holder> provider = new JAXBElementProvider<>();
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
        String expected = "<?xml version='1.0' encoding='UTF-8'?><tagholder><attr>attribute</attr>"
            + "<thetag><group>B</group><name>A</name></thetag></tagholder>";
        assertEquals(expected, bos.toString());
    }
    @Test
    public void testOutAttributesAsElementsForList() throws Exception {

        //Provider
        JAXBElementProvider<List<?>> provider = new JAXBElementProvider<>();
        provider.setMessageContext(new MessageContextImpl(createMessage()));
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
        provider.writeTo(list, ArrayList.class, type, //NOPMD
            new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);

        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<tagholders><tagholder><attr>attribute</attr>"
            + "<thetag><group>B</group><name>A</name></thetag></tagholder></tagholders>";
        assertEquals(expected, bos.toString());
    }


    @Test
    public void testOutAppendElementsDiffNs() throws Exception {
        JAXBElementProvider<TagVO2> provider = new JAXBElementProvider<>();

        Map<String, String> prefixes = new HashMap<>();
        prefixes.put("http://tags", "ns2");
        provider.setNamespacePrefixes(prefixes);

        Map<String, String> map = new HashMap<>();
        map.put("{http://tags}thetag", "{http://tagsvo2}t");
        provider.setOutAppendElements(map);
        TagVO2 tag = new TagVO2("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO2.class, TagVO2.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "<?xml version='1.0' encoding='UTF-8'?><ps1:t xmlns:ps1=\"http://tagsvo2\">"
            + "<ns2:thetag xmlns:ns2=\"http://tags\"><group>B</group><name>A</name></ns2:thetag></ps1:t>";
        assertEquals(expected, bos.toString());
    }

    @Test
    public void testOutAppendNsElementBeforeLocal() throws Exception {
        JAXBElementProvider<TagVO> provider = new JAXBElementProvider<>();
        Map<String, String> map = new HashMap<>();
        map.put("tagVO", "{http://tagsvo2}t");
        provider.setOutAppendElements(map);
        TagVO tag = new TagVO("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO.class, TagVO.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "<?xml version='1.0' encoding='UTF-8'?><ps1:t xmlns:ps1=\"http://tagsvo2\">"
            + "<tagVO><group>B</group><name>A</name></tagVO></ps1:t>";
        assertEquals(expected, bos.toString());
    }

    @Test
    public void testOutAppendLocalBeforeLocal() throws Exception {
        JAXBElementProvider<TagVO> provider = new JAXBElementProvider<>();
        Map<String, String> map = new HashMap<>();
        map.put("tagVO", "supertag");
        provider.setOutAppendElements(map);
        TagVO tag = new TagVO("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO.class, TagVO.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<supertag><tagVO><group>B</group><name>A</name></tagVO></supertag>";
        assertEquals(expected, bos.toString());
    }

    @Test
    public void testOutAppendElementsSameNs() throws Exception {
        JAXBElementProvider<TagVO2> provider = new JAXBElementProvider<>();

        Map<String, String> prefixes = new HashMap<>();
        prefixes.put("http://tags", "ns2");
        provider.setNamespacePrefixes(prefixes);

        Map<String, String> map = new HashMap<>();
        map.put("{http://tags}thetag", "{http://tags}t");
        provider.setOutAppendElements(map);
        TagVO2 tag = new TagVO2("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO2.class, TagVO2.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<ns2:t xmlns:ns2=\"http://tags\"><ns2:thetag><group>B</group><name>A</name></ns2:thetag>"
            + "</ns2:t>";
        assertEquals(expected, bos.toString());
    }


    @Test
    public void testOutElementsMapLocalNsToLocalNs() throws Exception {
        JAXBElementProvider<TagVO2> provider = new JAXBElementProvider<>();
        Map<String, String> prefixes = new HashMap<>();
        prefixes.put("http://tags", "ns2");
        provider.setNamespacePrefixes(prefixes);
        Map<String, String> map = new HashMap<>();
        map.put("{http://tags}thetag", "{http://tagsvo2}t");
        provider.setOutTransformElements(map);
        TagVO2 tag = new TagVO2("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO2.class, TagVO2.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<ns2:t xmlns:ns2=\"http://tagsvo2\"><group>B</group><name>A</name></ns2:t>";
        assertEquals(expected, bos.toString());

    }

    @Test
    public void testOutElementsMapLocalNsToLocal() throws Exception {
        JAXBElementProvider<TagVO2> provider = new JAXBElementProvider<>();
        Map<String, String> map = new HashMap<>();
        map.put("{http://tags}thetag", "t");
        provider.setOutTransformElements(map);
        TagVO2 tag = new TagVO2("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO2.class, TagVO2.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<t><group>B</group><name>A</name></t>";
        assertEquals(expected, bos.toString());
    }

    @Test
    public void testOutElementsMapLocalNsToLocalWildcard() throws Exception {
        JAXBElementProvider<TagVO2Holder> provider = new JAXBElementProvider<>();
        Map<String, String> map = new HashMap<>();
        map.put("{http://tags}*", "*");
        provider.setOutTransformElements(map);
        TagVO2 tag = new TagVO2("A", "B");
        TagVO2Holder holder = new TagVO2Holder();
        holder.setTag(tag);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(holder, TagVO2Holder.class, TagVO2Holder.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<tagholder attr=\"attribute\"><thetag><group>B</group><name>A</name></thetag></tagholder>";
        assertEquals(expected, bos.toString());
    }

    @Test
    public void testOutElementsMapLocalNsToLocalWildcard2() throws Exception {
        JAXBElementProvider<TagVO2Holder> provider = new JAXBElementProvider<>();

        Map<String, String> prefixes = new HashMap<>();
        prefixes.put("http://tags", "ns2");
        provider.setNamespacePrefixes(prefixes);

        Map<String, String> map = new HashMap<>();
        map.put("{http://tags}*", "{http://tags2}*");
        provider.setOutTransformElements(map);
        TagVO2 tag = new TagVO2("A", "B");
        TagVO2Holder holder = new TagVO2Holder();
        holder.setTag(tag);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(holder, TagVO2Holder.class, TagVO2Holder.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);

        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<ns2:tagholder xmlns:ns2=\"http://tags2\" attr=\"attribute\"><ns2:thetag><group>B</group>"
            + "<name>A</name></ns2:thetag></ns2:tagholder>";
        assertEquals(expected, bos.toString());
    }

    @Test
    public void testOutElementsMapLocalToLocalNs() throws Exception {
        JAXBElementProvider<TagVO> provider = new JAXBElementProvider<>();
        Map<String, String> map = new HashMap<>();
        map.put("tagVO", "{http://tags}thetag");
        provider.setOutTransformElements(map);
        TagVO tag = new TagVO("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO.class, TagVO.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<ps1:thetag xmlns:ps1=\"http://tags\"><group>B</group><name>A</name></ps1:thetag>";
        assertEquals(expected, bos.toString());
    }

    @Test
    public void testOutElementsMapLocalToLocal() throws Exception {
        JAXBElementProvider<TagVO> provider = new JAXBElementProvider<>();
        Map<String, String> map = new HashMap<>();
        map.put("tagVO", "thetag");
        map.put("group", "group2");
        provider.setOutTransformElements(map);
        TagVO tag = new TagVO("A", "B");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(tag, TagVO.class, TagVO.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<thetag><group2>B</group2><name>A</name></thetag>";
        assertEquals(expected, bos.toString());
    }

    @Test
    public void testDropElements() throws Exception {
        JAXBElementProvider<ManyTags> provider = new JAXBElementProvider<>();
        List<String> list = new ArrayList<>();
        list.add("tagVO");
        list.add("ManyTags");
        list.add("list");
        provider.setOutDropElements(list);
        ManyTags many = new ManyTags();
        Tags tags = new Tags();
        tags.addTag(new TagVO("A", "B"));
        tags.addTag(new TagVO("C", "D"));
        many.setTags(tags);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(many, ManyTags.class, ManyTags.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<tags><group>B</group><name>A</name><group>D</group><name>C</name></tags>";
        assertEquals(expected, bos.toString());
    }

    @Test
    public void testDropQualifiedElements() throws Exception {
        JAXBElementProvider<TagVO2> provider = new JAXBElementProvider<>();
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
        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<group>B</group>";
        assertEquals(expected, bos.toString());

    }

    @Test
    public void testReadUnqualifiedCollection() throws Exception {
        String data = "<Books><Book><id>123</id><name>CXF in Action</name>"
            + "</Book><Book><id>124</id><name>CXF Rocks</name></Book></Books>";
        doReadUnqualifiedCollection(data, "setBooks", List.class);
    }

    @Test
    public void testReadMalformedXML() throws Exception {
        JAXBElementProvider<Book> provider = new JAXBElementProvider<>();
        try {
            provider.readFrom(Book.class, Book.class,
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(),
                       new ByteArrayInputStream("<Book>".getBytes()));
            fail("404 is expected");
        } catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void doReadUnqualifiedCollection(String data, String mName, Class<T> type) throws Exception {
        JAXBElementProvider<T> provider = new JAXBElementProvider<>();
        Method m = CollectionsResource.class.getMethod(mName,
                                                       new Class[]{type});
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(
                       type, m.getGenericParameterTypes()[0],
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
        assertNotNull(o);
        Book b1 = null;
        Book b2 = null;
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
    public void doReadQualifiedCollection(String data,
                                          boolean isArray) throws Exception {
        @SuppressWarnings("rawtypes")
        JAXBElementProvider provider = new JAXBElementProvider();
        Method m = null;
        if (!isArray) {
            m = CollectionsResource.class.getMethod("setTags", new Class[]{List.class});
        } else {
            m = CollectionsResource.class.getMethod("setTagsArray", new Class[]{TagVO2[].class});
        }

        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Object o = provider.readFrom(
                       m.getParameterTypes()[0], m.getGenericParameterTypes()[0],
                       new Annotation[0], MediaType.TEXT_XML_TYPE, new MetadataMap<String, String>(), is);
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
        assertEquals("A", t1.getName());
        assertEquals("B", t1.getGroup());

        assertEquals("C", t2.getName());
        assertEquals("D", t2.getGroup());
    }

    @Test
    public void testSetSchemasFromClasspath() {
        JAXBElementProvider<?> provider = new JAXBElementProvider<>();
        List<String> locations = new ArrayList<>();
        locations.add("classpath:/test.xsd");
        provider.setSchemaLocations(locations);
        Schema s = provider.getSchema();
        assertNotNull("schema can not be read from classpath", s);
    }

    @Test
    public void testSetSchemasFromAnnotation() {
        JAXBElementProvider<?> provider = new JAXBElementProvider<>();
        ClassResourceInfo cri =
            ResourceUtils.createClassResourceInfo(JAXBResource.class, JAXBResource.class, true, true);
        provider.init(Collections.singletonList(cri));
        Schema s = provider.getSchema();
        assertNotNull("schema can not be read from classpath", s);
    }

    @Test
    public void testSetSchemasFromDisk() throws Exception {
        JAXBElementProvider<?> provider = new JAXBElementProvider<>();
        List<String> locations = new ArrayList<>();
        String loc = getClass().getClassLoader().getResource("test.xsd").toURI().getPath();

        locations.add("file:" + loc);
        provider.setSchemaLocations(locations);
        Schema s = provider.getSchema();
        assertNotNull("schema can not be read from disk", s);
    }

    @Test
    public void testWriteWithValidation() throws Exception {
        JAXBElementProvider<Book2> provider = new JAXBElementProvider<>();
        List<String> locations = new ArrayList<>();
        String loc = getClass().getClassLoader().getResource("book1.xsd").toURI().getPath();
        locations.add(loc);
        provider.setSchemaLocations(locations);
        Schema s = provider.getSchema();
        assertNotNull("schema can not be read from disk", s);

        provider.setValidateOutput(true);
        provider.setValidateBeforeWrite(true);

        Book2 book2 = new Book2();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(book2, Book2.class, Book2.class, new Annotation[]{}, MediaType.TEXT_XML_TYPE,
                         new MetadataMap<String, Object>(), bos);

        assertTrue(bos.toString().contains("http://superbooks"));
    }

    @Test
    public void testWriteWithFailedValidation() throws Exception {
        JAXBElementProvider<Book2> provider = new JAXBElementProvider<>();
        List<String> locations = new ArrayList<>();
        String loc = getClass().getClassLoader().getResource("test.xsd").toURI().getPath();
        locations.add("file:" + loc);
        provider.setSchemaLocations(locations);
        Schema s = provider.getSchema();
        assertNotNull("schema can not be read from disk", s);

        provider.setValidateOutput(true);
        provider.setValidateBeforeWrite(true);

        try {
            provider.writeTo(new Book2(), Book2.class, Book2.class, new Annotation[]{},
                             MediaType.TEXT_XML_TYPE,
                             new MetadataMap<String, Object>(), new ByteArrayOutputStream());
            fail("Validation exception expected");
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            boolean english = "en".equals(java.util.Locale.getDefault().getLanguage());
            if (english) {
                assertTrue(cause.getMessage().contains("Cannot find the declaration of element"));
            }
        }

    }

    @Test
    public void testIsReadableWithJaxbIndex() {
        JAXBElementProvider<TestBean> p = new JAXBElementProvider<>();
        assertTrue(p.isReadable(TestBean.class,
                                TestBean.class,
                                new Annotation[]{}, MediaType.APPLICATION_XML_TYPE));
    }

    @Test
    public void testResponseIsNotReadable() {
        JAXBElementProvider<Response> p = new JAXBElementProvider<>();
        assertFalse(p.isReadable(Response.class,
                                 Response.class,
                                 new Annotation[]{}, MediaType.APPLICATION_XML_TYPE));
    }

    @Test
    public void testResponseIsNotReadable2() {
        JAXBElementProvider<Response> p = new JAXBElementProvider<>();
        p.setUnmarshallAsJaxbElement(true);
        assertFalse(p.isReadable(Response.class,
                                 Response.class,
                                 new Annotation[]{}, MediaType.APPLICATION_XML_TYPE));
    }


    @Test
    public void testXMLSourceIsNotReadable() {
        JAXBElementProvider<XMLSource> p = new JAXBElementProvider<>();
        assertFalse(p.isReadable(XMLSource.class,
                                 XMLSource.class,
                                 new Annotation[]{}, MediaType.APPLICATION_XML_TYPE));
    }
    @Test
    public void testPackageContextObjectFactory() {
        JAXBElementProvider<org.apache.cxf.jaxrs.fortest.jaxb.Book> p
            = new JAXBElementProvider<>();
        assertTrue(p.isReadable(org.apache.cxf.jaxrs.fortest.jaxb.Book.class,
                                org.apache.cxf.jaxrs.fortest.jaxb.Book.class,
                                new Annotation[]{}, MediaType.APPLICATION_XML_TYPE));
        try {
            JAXBContext context = p.getPackageContext(org.apache.cxf.jaxrs.fortest.jaxb.Book.class);
            JAXBContext context2 = p.getPackageContext(org.apache.cxf.jaxrs.fortest.jaxb.Book.class);
            assertNotNull(context);
            assertNotNull(context2);
            assertSame(context, context2);
        } finally {
            p.clearContexts();
        }

    }

    @Test
    public void testSetMarshallProperties() throws Exception {

        Map<String, Object> props = new HashMap<>();
        props.put(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        props.put(Marshaller.JAXB_SCHEMA_LOCATION, "foo.xsd");

        final TestMarshaller m = new TestMarshaller();

        JAXBElementProvider<Object> provider = new JAXBElementProvider<Object>() {
            @Override
            protected Marshaller createMarshaller(Object obj, Class<?> cls, Type genericType, String enc)
                throws JAXBException {
                return m;
            }
        };

        provider.setMarshallerProperties(props);
        provider.writeTo("123", String.class, String.class, new Annotation[]{},
                         MediaType.APPLICATION_XML_TYPE, new MetadataMap<String, Object>(),
                         new ByteArrayOutputStream());

        assertEquals("Marshall properties have not been set", props, m.getProperties());
    }

    private static class TestMarshaller implements Marshaller {

        private Map<String, Object> props = new HashMap<>();

        TestMarshaller() {

        }

        public Map<String, Object> getProperties() {
            return props;
        }

        @SuppressWarnings("rawtypes")
        public <A extends XmlAdapter> A getAdapter(Class<A> type) {
            return null;
        }

        public AttachmentMarshaller getAttachmentMarshaller() {
            return null;
        }

        public ValidationEventHandler getEventHandler() throws JAXBException {
            return null;
        }

        public Listener getListener() {
            return null;
        }

        public Node getNode(Object contentTree) throws JAXBException {
            return null;
        }

        public Object getProperty(String name) throws PropertyException {
            return null;
        }

        public Schema getSchema() {
            return null;
        }

        public void marshal(Object jaxbElement, Result result) throws JAXBException {

        }

        public void marshal(Object jaxbElement, OutputStream os) throws JAXBException {
        }

        public void marshal(Object jaxbElement, File output) throws JAXBException {

        }

        public void marshal(Object jaxbElement, Writer writer) throws JAXBException {

        }

        public void marshal(Object jaxbElement, ContentHandler handler) throws JAXBException {

        }

        public void marshal(Object jaxbElement, Node node) throws JAXBException {

        }

        public void marshal(Object jaxbElement, XMLStreamWriter writer) throws JAXBException {

        }

        public void marshal(Object jaxbElement, XMLEventWriter writer) throws JAXBException {

        }

        @SuppressWarnings("rawtypes")
        public void setAdapter(XmlAdapter adapter) {

        }

        @SuppressWarnings("rawtypes")
        public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter) {

        }

        public void setAttachmentMarshaller(AttachmentMarshaller am) {

        }

        public void setEventHandler(ValidationEventHandler handler) throws JAXBException {

        }

        public void setListener(Listener listener) {

        }

        public void setProperty(String name, Object value) throws PropertyException {
            props.put(name, value);

        }

        public void setSchema(Schema schema) {

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

    @Path("/")
    @SchemaValidation(schemas = "/test.xsd")
    public static class JAXBResource {

        @GET
        public Book getBook() {
            return null;
        }

        @GET
        public SuperBook getSuperBook() {
            return null;
        }

        @GET
        public List<Book2> getBook2() {
            return null;
        }

    }

    @XmlRootElement(name = "list")
    public static class XmlList<A> {
        private List<A> list;

        public XmlList() {
            // no-op
        }

        public XmlList(List<A> l) {
            list = l;
        }

        public List<A> getList() {
            return list;
        }

        public void setList(List<A> l) {
            list = l;
        }
    }

    @XmlType
    public static class XmlObject {
        private MyObject attribute;

        public XmlObject() {
            // no-op
        }

        public XmlObject(MyObject a) {
            attribute = a;
        }

        @XmlElement(name = "attribute")
        public MyObject getAttribute() {
            return attribute;
        }

        public void setAttribute(MyObject a) {
            attribute = a;
        }
    }

    public static class MyObject {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String n) {
            name = n;
        }
    }


    @Path("/test")
    public class XmlListResource {
        @GET
        @Path("/jaxb")
        public XmlList<XmlObject> testJaxb() {
            return null;
        }

        @GET
        @Path("/jaxb2")
        public XmlList<org.apache.cxf.jaxrs.fortest.jaxb.SuperBook> testJaxb2() {
            return null;
        }
    }

    private Message createMessage() {
        ServerProviderFactory factory = ServerProviderFactory.getInstance();
        Message m = new MessageImpl();
        m.put(Message.ENDPOINT_ADDRESS, "http://localhost:8080/bar");
        m.put("org.apache.cxf.http.case_insensitive_queries", false);
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        Endpoint endpoint = EasyMock.mock(Endpoint.class);
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(null).anyTimes();
        EasyMock.expect(endpoint.get(Application.class.getName())).andReturn(null);
        EasyMock.expect(endpoint.size()).andReturn(0).anyTimes();
        EasyMock.expect(endpoint.isEmpty()).andReturn(true).anyTimes();
        EasyMock.expect(endpoint.get(ServerProviderFactory.class.getName())).andReturn(factory).anyTimes();
        EasyMock.replay(endpoint);
        e.put(Endpoint.class, endpoint);
        return m;
    }
}