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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.cxf.jaxrs.ext.Description;
import org.apache.cxf.jaxrs.ext.xml.XMLName;
import org.apache.cxf.jaxrs.model.wadl.FormInterface;

@XmlRootElement(name = "thebook", namespace = "http://superbooks")
@XmlType(name = "book", namespace = "http://superbooks")
@Description("Book subresource")
@XMLName(value = "{http://books}thesuperbook", prefix = "p1")
public class Book implements FormInterface {

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
    @Produces({"application/xml", "application/json" })
    @Description("Get the book")
    public Book getIt() {
        return this;
    }
    
    public void setId(int ident) {
        id = ident;
    }
    
    public int getId() {
        return id;
    }
    
    @Path("/chapter/{cid}")
    public Chapter getChapter(@PathParam("cid") int cid) {
        return chapter;
    }
    
    public void form1(MultivaluedMap map) {
    }
    
    public void form2(String f1, String f2) {
    }
    
}
