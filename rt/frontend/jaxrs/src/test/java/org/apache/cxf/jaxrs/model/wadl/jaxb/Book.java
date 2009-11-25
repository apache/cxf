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
package org.apache.cxf.jaxrs.model.wadl.jaxb;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "thebook", namespace = "http://superbooks")
@XmlType(name = "book", namespace = "http://superbooks")
public class Book {

    private int id;
    @XmlElement(name = "thechapter", namespace = "http://superbooks")
    private Chapter chapter;
    
    public Book() {
    }
    
    public Book(int id) {
        this.id = id;
    }
    
    @GET
    @Path("/book")
    public int getId() {
        return id;
    }
    
    public void setId(int ident) {
        id = ident;
    }
    
    @Path("/chapter/{cid}")
    public Chapter getChapter(@PathParam("cid") int cid) {
        return chapter;
    }
    
    @Path("/form1")
    @POST
    public void form1(MultivaluedMap map) {
    }
    
    @Path("/form2")
    @POST
    public void form2(@FormParam("field1") String f1, @FormParam("field2") String f2) {
    }
    
}
