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
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class AtomClientBookTest extends AbstractBusClientServerTestBase {

    private Abdera abdera = new Abdera();
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(AtomBookServer.class));
    }
    
    @Test
    @Ignore("this test fails on different JDK's due to the"
            + "maps abdera uses not being ordered so the"
            + "strict string compares fail")
    public void testGetBooks() throws Exception {
        String endpointAddress =
            "http://localhost:9080/bookstore/books/feed"; 
        Feed feed = getFeed(endpointAddress, null);
        assertEquals(endpointAddress, feed.getBaseUri().toString());
        assertEquals("Collection of Books", feed.getTitle());
        
        getAndCompareAsStrings("http://localhost:9080/bookstore/books/feed",
                               "resources/expected_atom_books_json.txt",
                               "application/json");
        
        // add new book
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
            Document<Entry> entryDoc = abdera.getParser().parse(post.getResponseBodyAsStream());
            assertEquals(entryDoc.getRoot().toString(), e.toString());
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }         
        
        Entry entry = getEntry(location, null);
        assertEquals(location, entry.getBaseUri().toString());
        assertEquals("AtomBook", entry.getTitle());
                
        // get existing book
        
        endpointAddress =
            "http://localhost:9080/bookstore/books/subresources/123"; 
        entry = getEntry(endpointAddress, null);
        assertEquals("CXF in Action", entry.getTitle());
        
        // now json
        getAndCompareAsStrings("http://localhost:9080/bookstore/books/entries/123",
                               "resources/expected_atom_book_json.txt",
                               "application/json");
        
        // do the same using a system query
        getAndCompareAsStrings("http://localhost:9080/bookstore/books/entries/123?_contentType="
                               + "application/json",
                               "resources/expected_atom_book_json.txt",
                               "*/*");
//      do the same using a system query shortcut
        getAndCompareAsStrings("http://localhost:9080/bookstore/books/entries/123?_contentType="
                               + "json",
                               "resources/expected_atom_book_json.txt",
                               "*/*");
        
        
    }
    
    private void getAndCompareAsStrings(String address, 
                                        String resourcePath,
                                        String type) throws Exception {
        GetMethod get = new GetMethod(address);
        get.setRequestHeader("Accept", type);
        HttpClient httpClient = new HttpClient();
        try {
            httpClient.executeMethod(get);           
            String jsonContent = getStringFromInputStream(get.getResponseBodyAsStream());
            String expected = getStringFromInputStream(
                  getClass().getResourceAsStream(resourcePath));
            assertEquals("Atom entry should've been formatted as json", expected, jsonContent);
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
        if (acceptType != null) {
            get.setRequestHeader("Accept", acceptType);
        }
        HttpClient httpClient = new HttpClient();
        try {
            httpClient.executeMethod(get);           
            Document<Feed> doc = abdera.getParser().parse(get.getResponseBodyAsStream());
            return doc.getRoot();
        } finally {
            get.releaseConnection();
        }
    }
    
    private Entry getEntry(String endpointAddress, String acceptType) throws Exception {
        GetMethod get = new GetMethod(endpointAddress);
        if (acceptType != null) {
            get.setRequestHeader("Accept", acceptType);
        }
        HttpClient httpClient = new HttpClient();
        try {
            httpClient.executeMethod(get);           
            Document<Entry> doc = abdera.getParser().parse(get.getResponseBodyAsStream());
            return doc.getRoot();
        } finally {
            get.releaseConnection();
        }
    }
    
    private String getStringFromInputStream(InputStream in) throws Exception {        
        CachedOutputStream bos = new CachedOutputStream();
        IOUtils.copy(in, bos);
        in.close();
        bos.close();
        return bos.getOut().toString();        
    }
}
