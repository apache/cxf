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

package org.apache.cxf.systest.jaxrs.security;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.cxf.systest.jaxrs.Book;
import org.springframework.security.annotation.Secured;

public class SecureBook {
    private String name;
    private long id;
    
    public SecureBook() {
        name = "CXF in Action";
        id = 123L;
    }
    
    public SecureBook(String name, long id) {
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
    @Path("self")    
    @Produces("application/xml")
    @Secured("ROLE_ADMIN")
    @RolesAllowed("ROLE_ADMIN")
    public Book getBook() {
        return new Book(name, id);
    } 
    
}
