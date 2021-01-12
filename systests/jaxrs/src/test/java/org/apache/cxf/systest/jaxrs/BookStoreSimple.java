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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/simplebooks/{id}")
public class BookStoreSimple {
    public static class BookBean {
        private long id;
        public BookBean() {

        }
        public BookBean(long id) {
            this.id = id;
        }
        public long getId() {
            return id;
        }
        @PathParam("id")
        public void setId(long id) {
            this.id = id;
        }
    }
    @Resource
    private Book injectedBook;


    @GET
    public Book getBook(@PathParam("id") long id) {
        return new Book("Simple", id);
    }

    @GET
    @Path("/book")
    public Book getBook2(@PathParam("id") long id) {
        return getBook(id);
    }
    @PostConstruct
    public void postConstruct() {
        if (injectedBook == null) {
            throw new IllegalStateException("Book resource has not been injected");
        }
    }
    @GET
    @Path("/beanparam")
    public Book getBookBeanParam(@BeanParam BookBean bookBean) {
        return getBook(bookBean.getId());
    }
}
