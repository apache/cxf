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

package org.apache.cxf.jaxrs.provider.jsrjsonb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.resources.CollectionsResource;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class JsrJsonbProviderTest {
    private JsrJsonbProvider provider;
    
    @Before
    public void setUp() {
        provider = new JsrJsonbProvider();
    }
    
    @Test
    public void testReadMalformedJson() throws Exception {
        final byte[] bytes = "junk".getBytes();
        final WebApplicationException ex = assertThrows(WebApplicationException.class, () -> read(Book.class, bytes));
        assertThat(ex.getResponse().getStatus(), equalTo(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void testReadListOfBooks() throws Exception {
        final String input = "["
           + "{"
           + "    \"name\":\"CXF 1\""
           + "},"
           + "{"
           + "    \"name\":\"CXF 2\""
           + "}"
           + "]";
        
        final Method m = CollectionsResource.class.getMethod("getBooks", new Class[]{});
        final List<Book> books = read(m.getReturnType(), m.getGenericReturnType(), input.getBytes());
        
        assertThat(books.size(), equalTo(2));
        assertThat(books.get(0).getName(), equalTo("CXF 1"));
        assertThat(books.get(1).getName(), equalTo("CXF 2"));
    }
    
    @Test
    public void testWriteBook() throws Exception {
        final Book book = new Book("CXF 1", 1);
        final String payload = write(book, Book.class);
        
        assertThat(payload, equalTo("{\"id\":1,\"name\":\"CXF 1\",\"state\":\"\"}"));
    }
    
    @Test
    public void testReadBook() throws Exception {
        String input = "{"
            +     "\"id\":1,"
            +     "\"name\":\"CXF 1\""
            + "}";
        
        final Book book = read(Book.class, input.getBytes());
        assertThat(book.getId(), equalTo(1L));
        assertThat(book.getName(), equalTo("CXF 1"));
    }
    
    @Test
    public void testWriteListOfBooks() throws Exception {
        final List<Book> books = new ArrayList<>();
        books.add(new Book("CXF 1", 1));
        books.add(new Book("CXF 2", 2));

        final Method m = CollectionsResource.class.getMethod("setBooksArray", new Class[]{Book[].class});
        final String payload = write(books, List.class, m.getGenericParameterTypes()[0]);
        
        assertThat(payload,
            equalTo("[{\"id\":1,\"name\":\"CXF 1\",\"state\":\"\"},{\"id\":2,\"name\":\"CXF 2\",\"state\":\"\"}]"));
    }
    
    private <T> T read(Class<?> clazz, byte[] bytes) throws IOException {
        return read(clazz, null, bytes);
    }
    
    @SuppressWarnings("unchecked")
    private <T> T read(Class<?> clazz, Type genericType, byte[] bytes) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            return (T)provider.readFrom((Class<Object>)clazz, genericType, null, null, null, in);
        }
    }
    
    private String write(Object value, Class<?> clazz) throws IOException {
        return write(value, clazz, null);
    }
    
    @SuppressWarnings("unchecked")
    private String write(Object value, Class<?> clazz, Type genericType) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            provider.writeTo(value, (Class<Object>)clazz, genericType, null, null, null, out);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
