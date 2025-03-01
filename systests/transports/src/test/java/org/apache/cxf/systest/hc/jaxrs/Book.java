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

package org.apache.cxf.systest.hc.jaxrs;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.xml.bind.annotation.XmlRootElement;

@JsonTypeInfo(use = Id.CLASS, include = As.PROPERTY, property = "class")
@XmlRootElement(name = "Book")
public class Book {
    private String name;
    private long id;
    private Map<Long, Chapter> chapters = new HashMap<>();

    public Book() {
        Chapter c1 = new Chapter();
        c1.setId(1L);
        c1.setTitle("chapter 1");
        chapters.put(c1.getId(), c1);
        Chapter c2 = new Chapter();
        c2.setId(2L);
        c2.setTitle("chapter 2");
        chapters.put(c2.getId(), c2);
    }

    public Book(String name, long id) {
        this.name = name;
        this.id = id;
    }

    public void setName(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }

    public void setId(long i) {
        id = i;
    }
    public long getId() {
        return id;
    }

    @PUT
    public void cloneState(Book book) {
        id = book.getId();
        name = book.getName();
    }

    @GET
    public Book retrieveState() {
        return this;
    }

    @GET
    @Path("chapters/{chapterid}/")
    @Produces("application/xml;charset=ISO-8859-1")
    public Chapter getChapter(@PathParam("chapterid") long chapterid) {
        return chapters.get(chapterid);
    }

    @GET
    @Path("chapters/acceptencoding/{chapterid}/")
    @Produces("application/xml")
    public Chapter getChapterAcceptEncoding(@PathParam("chapterid") long chapterid) {
        return chapters.get(chapterid);
    }

    @GET
    @Path("chapters/badencoding/{chapterid}/")
    @Produces("application/xml;charset=UTF-48")
    public Chapter getChapterBadEncoding(@PathParam("chapterid") long chapterid) {
        return chapters.get(chapterid);
    }

    @Path("chapters/sub/{chapterid}/")
    public Chapter getSubChapter(@PathParam("chapterid") long chapterid) {
        return chapters.get(chapterid);
    }

    @Path("chaptersobject/sub/{chapterid}/")
    public Object getSubChapterObject(@PathParam("chapterid") long chapterid) {
        return getSubChapter(chapterid);
    }

}
