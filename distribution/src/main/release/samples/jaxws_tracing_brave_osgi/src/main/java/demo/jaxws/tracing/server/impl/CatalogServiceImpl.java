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
package demo.jaxws.tracing.server.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.ws.AsyncHandler;

import com.github.kristofa.brave.Brave;

import org.apache.cxf.annotations.UseAsyncMethod;
import org.apache.cxf.jaxws.ServerAsyncResponse;

import demo.jaxws.tracing.server.Book;
import demo.jaxws.tracing.server.CatalogService;
import zipkin.Constants;

public class CatalogServiceImpl implements CatalogService {
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<String, Book> books = new ConcurrentHashMap<>();
    private final Brave brave;
    
    public CatalogServiceImpl(final Brave brave) {
        this.brave = brave;
    }
    
    @UseAsyncMethod
    public void addBook(Book book)  {
        throw new UnsupportedOperationException("Please use async version of the method");
    }
    
    public Future<?> addBookAsync(Book book, AsyncHandler<Book> handler) {
        final ServerAsyncResponse<Book> response = new ServerAsyncResponse<Book>();

        executor.submit(() -> {
            try {
                brave.localTracer().startNewSpan(Constants.LOCAL_COMPONENT, "Inserting New Book");
                books.put(book.getId(), book);
                handler.handleResponse(response);
            } finally {
                brave.localTracer().finishSpan();
            }
        });
        
        return response;
    }

    @Override
    public Book getBook(final String id) {
        final Book book = books.get(id);
        
        if (book == null) {
            throw new RuntimeException("Book with does not exists: " + id);
        }
        
        return book;
    }
    
    @Override
    public void delete(final String id) {
        if (books.remove(id) == null) {
            throw new RuntimeException("Book with does not exists: " + id);
        }
    }
}