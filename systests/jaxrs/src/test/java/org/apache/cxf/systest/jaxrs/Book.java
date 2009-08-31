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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "Book")
public class Book {
    private String name;
    private long id;
    private Map<Long, Chapter> chapters = new HashMap<Long, Chapter>();
    
    public Book() {
        init();
        //System.out.println("----chapters: " + chapters.size());
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
    
    @GET
    @Path("chapters/{chapterid}/")    
    @Produces("application/xml;charset=ISO-8859-1")
    public Chapter getChapter(@PathParam("chapterid")int chapterid) {
        return chapters.get(new Long(chapterid));
    } 

    @GET
    @Path("chapters/badencoding/{chapterid}/")    
    @Produces("application/xml;charset=UTF-48")
    public Chapter getChapterBadEncoding(@PathParam("chapterid")int chapterid) {
        return chapters.get(new Long(chapterid));
    }
    
    @Path("chapters/sub/{chapterid}/")    
    public Chapter getSubChapter(@PathParam("chapterid")int chapterid) {
        return chapters.get(new Long(chapterid));
    }
    
    @Path("chaptersobject/sub/{chapterid}/")    
    public Object getSubChapterObject(@PathParam("chapterid")int chapterid) {
        return getSubChapter(chapterid);
    }
    
    
    final void init() {
        Chapter c1 = new Chapter();
        c1.setId(1);
        c1.setTitle("chapter 1");
        chapters.put(c1.getId(), c1);
        Chapter c2 = new Chapter();
        c2.setId(2);
        c2.setTitle("chapter 2");
        chapters.put(c2.getId(), c2);
    }

}
