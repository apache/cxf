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
package org.apache.cxf.systests.cdi.base;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

@Named
public class BookStoreService {
    private Map< String, Book> books = new HashMap<>();

    public Book get(final String id) {
        return books.get(id);
    }

    public Collection< Book > all() {
        return books.values();
    }

    public Book store(final String id, final String name) {
        final Book book = new Book(name, id);
        books.put(id, book);
        return book;
    }

    @Produces
    @Dependent
    public String version() {
        return "1.0";
    }
}
