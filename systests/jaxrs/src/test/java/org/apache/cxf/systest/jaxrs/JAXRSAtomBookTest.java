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

package org.apache.cxf.systest.jaxrs;

import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.codehaus.jettison.json.JSONObject;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSAtomBookTest extends AbstractBusClientServerTestBase {

    private Abdera abdera = new Abdera();
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(AtomBookServer.class));
    }
    
    @Test
    public void testGetBooks() throws Exception {
        String endpointAddress =
            "http://localhost:9080/bookstore/bookstore/books/feed"; 
        Feed feed = getFeed(endpointAddress, null);
        assertEquals("http://localhost:9080/bookstore/bookstore/books/feed", 
                     feed.getBaseUri().toString());
        assertEquals("Collection of Books", feed.getTitle());
        
        getAndCompareJson("http://localhost:9080/bookstore/bookstore/books/feed",
                               "resources/expected_atom_books_json.txt",
                               "application/json");
        
        getAndCompareJson("http://localhost:9080/bookstore/bookstore/books/jsonfeed",
                          "resources/expected_atom_books_jsonfeed.txt",
                          "application/json, text/html, application/xml;q=0.9,"
                          + " application/xhtml+xml, image/png, image/jpeg, image/gif,"
                          + " image/x-xbitmap, */*;q=0.1");
        
        Entry entry = addEntry(endpointAddress);
        entry = addEntry(endpointAddress + "/relative");
        
        endpointAddress =
            "http://localhost:9080/bookstore/bookstore/books/subresources/123"; 
        entry = getEntry(endpointAddress, null);
        assertEquals("CXF in Action", entry.getTitle());
        
        getAndCompareJson("http://localhost:9080/bookstore/bookstore/books/entries/123",
                               "resources/expected_atom_book_json.txt",
                               "application/json");
        
        getAndCompareJson("http://localhost:9080/bookstore/bookstore/books/entries/123?_type="
                               + "application/json",
                               "resources/expected_atom_book_json.txt",
                               "*/*");
        getAndCompareJson("http://localhost:9080/bookstore/bookstore/books/entries/123?_type="
                               + "json",
                               "resources/expected_atom_book_json.txt",
                               "*/*");
        
        // do the same using extension mappings
        getAndCompareJson("http://localhost:9080/bookstore/bookstore/books/entries/123.json",
                               "resources/expected_atom_book_json.txt",
                               "*/*");
        
        
    }
    
    private Entry addEntry(String endpointAddress) throws Exception {
        Entry e = createBookEntry(256, "AtomBook");
        StringWriter w = new StringWriter();
        e.writeTo(w);
        
        PostMethod post = new PostMethod(endpointAddress);
        post.setRequestEntity(
             new StringRequestEntity(w.toString(), "application/atom+xml", null));
        HttpClient httpclient = new HttpClient();
        
        String location = null;
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(201, result);
            location = post.getResponseHeader("Location").getValue();
            InputStream ins = post.getResponseBodyAsStream();
            Document<Entry> entryDoc = abdera.getParser().parse(copyIn(ins));
            assertEquals(entryDoc.getRoot().toString(), e.toString());
        } finally {
            post.releaseConnection();
        }         
        
        Entry entry = getEntry(location, null);
        assertEquals(location, entry.getBaseUri().toString());
        assertEquals("AtomBook", entry.getTitle());
        return entry;
    }
    
    @Test
    public void testGetBooks2() throws Exception {
        String endpointAddress =
            "http://localhost:9080/bookstore/sub/"; 
        Feed feed = getFeed(endpointAddress, null);
        assertEquals("http://localhost:9080/bookstore/sub/", 
                     feed.getBaseUri().toString());
        assertEquals("Collection of Books", feed.getTitle());
        
        getAndCompareJson("http://localhost:9080/bookstore/sub/books/entries/123.json",
                        "resources/expected_atom_book_json2.txt",
                        "*/*");
    }
    
    @Test
    public void testGetBooks3() throws Exception {
        getAndCompareJson("http://localhost:9080/atom/atom/books/entries/123.json",
                        "resources/expected_atom_book_json3.txt",
                        "*/*");
    }
    
    @Test
    public void testGetBooksWithCustomProvider() throws Exception {
        String endpointAddress =
            "http://localhost:9080/bookstore/bookstore4/books/feed"; 
        Feed feed = getFeed(endpointAddress, null);
        assertEquals("http://localhost:9080/bookstore/bookstore4/books/feed", 
                     feed.getBaseUri().toString());
        assertEquals("Collection of Books", feed.getTitle());
    }
    
    private void getAndCompareJson(String address, 
                                   String resourcePath,
                                   String type) throws Exception {
        GetMethod get = new GetMethod(address);
        get.setRequestHeader("Content-Type", "*/*");
        get.setRequestHeader("Accept", type);
        HttpClient httpClient = new HttpClient();
        try {
            httpClient.executeMethod(get);           
            String jsonContent = getStringFromInputStream(get.getResponseBodyAsStream());
            String expected = getStringFromInputStream(
                  getClass().getResourceAsStream(resourcePath));
            
            JSONObject obj1 = new JSONObject(jsonContent);
            JSONObject obj2 = new JSONObject(expected);
            
            assertEquals("Atom entry should've been formatted as json", 
                         obj1.toString(), obj2.toString());
        } finally {
            get.releaseConnection();
        }
    }
    
    private Entry createBookEntry(int id, String name) throws Exception {
        
        Book b = new Book();
        b.setId(id);
        b.setName(name);
        
        
        Factory factory = Abdera.getNewFactory();
        JAXBContext jc = JAXBContext.newInstance(Book.class);
        
        Entry e = factory.getAbdera().newEntry();
        e.setTitle(b.getName());
        e.setId(Long.toString(b.getId()));
        
        
        StringWriter writer = new StringWriter();
        jc.createMarshaller().marshal(b, writer);
        
        e.setContentElement(factory.newContent());
        e.getContentElement().setContentType(Content.Type.XML);
        e.getContentElement().setValue(writer.toString());
        
        return e;
    }   
    
    private Feed getFeed(String endpointAddress, String acceptType) throws Exception {
        GetMethod get = new GetMethod(endpointAddress);
        get.setRequestHeader("Content-Type", "*/*");
        if (acceptType != null) {
            get.setRequestHeader("Accept", acceptType);
        }
        HttpClient httpClient = new HttpClient();
        try {
            httpClient.executeMethod(get);           
            Document<Feed> doc = abdera.getParser().parse(copyIn(get.getResponseBodyAsStream()));
            return doc.getRoot();
        } finally {
            get.releaseConnection();
        }
    }
    
    private Entry getEntry(String endpointAddress, String acceptType) throws Exception {
        GetMethod get = new GetMethod(endpointAddress);
        get.setRequestHeader("Content-Type", "*/*");
        if (acceptType != null) {
            get.setRequestHeader("Accept", acceptType);
        }
        HttpClient httpClient = new HttpClient();
        try {
            httpClient.executeMethod(get);           
            Document<Entry> doc = abdera.getParser().parse(copyIn(get.getResponseBodyAsStream()));
            return doc.getRoot();
        } finally {
            get.releaseConnection();
        }
    }
    
    private InputStream copyIn(InputStream in) throws Exception {
        CachedOutputStream bos = new CachedOutputStream();
        IOUtils.copy(in, bos);
        in.close();
        in = bos.getInputStream();
        bos.close();
        return in;
    }
    private String getStringFromInputStream(InputStream in) throws Exception {
        return IOUtils.toString(in);
    }
}
