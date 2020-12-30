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

package org.apache.cxf.systest.jaxrs.provider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Book {
    private String name;
    private long id;
    private Map<Long, BookChapter> chapters = new HashMap<>();

    public Book() {
    }

    public Book(String name, long id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void addChapter(long chapterId, String title) {
        chapters.put(chapterId, new BookChapter(chapterId, title));
    }

    public Collection<BookChapter> getChapters() {
        return chapters.values();
    }
    
    public void setChapters(Collection<BookChapter> value) {
        value.forEach(c -> chapters.put(c.getId(), c));
    }
}