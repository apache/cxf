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
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.cxf.jaxrs.ext.atom.AbstractEntryBuilder;
import org.apache.cxf.jaxrs.ext.atom.AbstractFeedBuilder;
import org.apache.cxf.jaxrs.ext.atom.AtomElementReader;
import org.apache.cxf.jaxrs.ext.atom.AtomElementWriter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AtomPojoProviderTest extends Assert {

    private ClassPathXmlApplicationContext ctx;
    
    @Before
    public void setUp() {
        ctx = 
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxrs/provider/servers.xml"});
    }
    
    @Test
    public void testWriteFeedWithBuilders() throws Exception {
        AtomPojoProvider provider = (AtomPojoProvider)ctx.getBean("atom");
        assertNotNull(provider);
        provider.setFormattedOutput(true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        Books books = new Books();
        List<Book> bs = new ArrayList<Book>();
        bs.add(new Book("a"));
        bs.add(new Book("b"));
        books.setBooks(bs);
        provider.writeTo(books, Books.class, Books.class, new Annotation[]{},
                         MediaType.valueOf("application/atom+xml"), null, bos);
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        Feed feed = new AtomFeedProvider().readFrom(Feed.class, null, null, null, null, bis);
        assertEquals("Books", feed.getTitle()); 
        List<Entry> entries = feed.getEntries();
        assertEquals(2, entries.size());
        verifyEntry(getEntry(entries, "a"), "a");
        verifyEntry(getEntry(entries, "b"), "b");
    }
    
    @Test
    public void testWriteEntryWithBuilders() throws Exception {
        AtomPojoProvider provider = (AtomPojoProvider)ctx.getBean("atom2");
        assertNotNull(provider);
        provider.setFormattedOutput(true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(new Book("a"), Book.class, Book.class, new Annotation[]{},
                         MediaType.valueOf("application/atom+xml;type=entry"), null, bos);
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        Entry entry = new AtomEntryProvider().readFrom(Entry.class, null, null, null, null, bis);
        verifyEntry(entry, "a");
        
    }
    
    @Test
    public void testReadEntryWithBuilders() throws Exception {
        AtomPojoProvider provider = (AtomPojoProvider)ctx.getBean("atom3");
        assertNotNull(provider);
        doTestReadEntry(provider);
    }
    
    @Test
    public void testReadEntryWithoutBuilders() throws Exception {
        doTestReadEntry(new AtomPojoProvider());
    }
    
    @SuppressWarnings("unchecked")
    private void doTestReadEntry(AtomPojoProvider provider) throws Exception {
        provider.setFormattedOutput(true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        MediaType mt = MediaType.valueOf("application/atom+xml;type=entry");
        provider.writeTo(new Book("a"), Book.class, Book.class, new Annotation[]{}, mt, null, bos);
        System.out.println(bos.toString());
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        Book book = (Book)provider.readFrom((Class)Book.class, Book.class, 
                                            new Annotation[]{}, mt, null, bis);
        assertEquals("a", book.getName());
    }
    
    
    @Test
    public void testReadFeedWithBuilders() throws Exception {
        AtomPojoProvider provider = (AtomPojoProvider)ctx.getBean("atom4");
        assertNotNull(provider);
        doTestReadFeed(provider);
    }
    
    @Test
    public void testReadFeedWithoutBuilders() throws Exception {
        AtomPojoProvider provider = new AtomPojoProvider();
        doTestReadFeed(provider);
    }

    @SuppressWarnings("unchecked")
    private void doTestReadFeed(AtomPojoProvider provider) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        MediaType mt = MediaType.valueOf("application/atom+xml;type=feed");
        Books books = new Books();
        List<Book> bs = new ArrayList<Book>();
        bs.add(new Book("a"));
        bs.add(new Book("b"));
        books.setBooks(bs);
        provider.writeTo(books, Books.class, Books.class, new Annotation[]{}, mt, null, bos);
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        Books books2 = (Books)provider.readFrom((Class)Books.class, Books.class, 
                                            new Annotation[]{}, mt, null, bis);
        List<Book> list = books2.getBooks();
        assertEquals(2, list.size());
        assertTrue("a".equals(list.get(0).getName()) || "a".equals(list.get(1).getName()));
        assertTrue("b".equals(list.get(0).getName()) || "b".equals(list.get(1).getName()));        
    }
        
    private Entry getEntry(List<Entry> entries, String title) {
        for (Entry e : entries) {
            if (title.equals(e.getTitle())) {
                return e;
            }
        }
        return null;
    }
    
    private void verifyEntry(Entry e, String title) {
        assertNotNull(e);
        assertEquals(title, e.getTitle());
    }
 
    public static class CustomFeedWriter implements AtomElementWriter<Feed, Books> {

        public void writeTo(Feed feed, Books pojoFeed) {
            feed.setTitle("Books");
        }
        
    }
    
    public static class CustomEntryWriter implements AtomElementWriter<Entry, Book> {

        public void writeTo(Entry entry, Book pojoEntry) {
            entry.setTitle(pojoEntry.getName());
        }
        
    }
    
    public static class CustomEntryReader implements AtomElementReader<Entry, Book> {

        public Book readFrom(Entry element) {
            try {
                String s = element.getContent();
                                
                Unmarshaller um = 
                    new JAXBElementProvider().getJAXBContext(Book.class, Book.class).createUnmarshaller();
                return (Book)um.unmarshal(new StringReader(s));
            } catch (Exception ex) {
                // ignore
            }
            return null;
        }
        
    }
    
    public static class CustomFeedReader implements AtomElementReader<Feed, Books> {

        public Books readFrom(Feed element) {
            Books books = new Books();
            List<Book> list = new ArrayList<Book>();
            CustomEntryReader entryReader = new CustomEntryReader();
            for (Entry e : element.getEntries()) {
                list.add(entryReader.readFrom(e));
            }
            books.setBooks(list);
            return books;
        }
        
    }
    
    public static class CustomFeedBuilder extends AbstractFeedBuilder<Books> {
        @Override
        public String getBaseUri(Books books) {
            return "http://books";
        }
    }
    
    public static class CustomEntryBuilder extends AbstractEntryBuilder<Book> {
        @Override
        public String getBaseUri(Book books) {
            return "http://book";
        }
    }
    
        
    @XmlRootElement
    public static class Book {
        private String name = "Book";

        public Book() {
            
        }
        
        public Book(String name) {
            this.name = name;
        }
        
        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
        
        
    }
    
    @XmlRootElement
    public static class Books {
        
        private List<Book> books;
        
        public Books() {
            
        }
        
        public List<Book> getBooks() {
            return books;
        }
        
        public void setBooks(List<Book> list) {
            books = list;
        }
    }
}
