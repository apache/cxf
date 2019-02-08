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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.xml.bind.JAXBElement;

public class CollectionsResource {


    @GET
    public List<Book> getBooks() {
        return null;
    }

    @GET
    public Collection<Book> getBookCollection() {
        return null;
    }

    @GET
    public List<JAXBElement<Book>> getBookElements() {
        return null;
    }

    @GET
    public List<JAXBElement<?>> getBookElements2() {
        return null;
    }

    @GET
    public Set<Book> getBookSet() {
        return null;
    }

    @GET
    public List<TagVO2> getTags() {
        return null;
    }

    @GET
    public Book[] getBooksArray() {
        return null;
    }

    @POST
    public void setBooks(List<Book> books) {
    }

    @POST
    public void setBooksArray(Book[] books) {
    }

    @POST
    public void setTags(List<TagVO2> tags) {
    }

    @POST
    public void setTagsArray(TagVO2[] tags) {
    }

    @POST
    public void setBookCollection(Collection<Book> books) {
    }

    @POST
    public void setBookSet(Set<Book> books) {
    }
}
