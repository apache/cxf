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

package org.apache.cxf.jaxrs.resources;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;


@XmlRootElement(name = "Book")
@XmlSeeAlso({SuperBook.class })
public class Book implements Comparable<Book> {
    private String name;
    private long id;
    private Map<Long, Chapter> chapters = new HashMap<Long, Chapter>();
    
    public Book() {
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
    
    @Path("chapters/{chapterid}/")
    @GET
    public Chapter getChapter(@PathParam("chapterid") int chapterid) {
        return chapters.get(new Long(chapterid));
    }   

    @GET
    public String getState() {
        return "";
    }
    
    
    public void setState(String s) {
    }
    
    public int hashCode() { 
        return name.hashCode() * 37 + new Long(id).hashCode();
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof Book)) {
            return false;
        }
        Book other = (Book)o;
        
        return other.name.equals(name) && other.id == id;
        
    }

    public int compareTo(Book b) {
        Long i1 = new Long(getId());
        Long i2 = new Long(b.getId());
        return i1.compareTo(i2);
    }
}
