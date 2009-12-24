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
import javax.ws.rs.Produces;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.cxf.jaxrs.ext.Description;

@XmlRootElement(name = "thechapter", namespace = "http://superbooks")
@XmlType(name = "chapter", namespace = "http://superbooks")
@Description("Chapter subresource")
public class Chapter {

    private int id;
    public Chapter() {
    }
    public Chapter(int id) {
        this.id = id;
    }
    
    @GET
    @Path("/id")
    @Produces({"application/xml", "application/json" })
    @Description("Get the chapter")
    public Chapter getIt() {
        return this;
    }
    
    public void setId(int ident) {
        id = ident;
    }
    
    public int getId() {
        return id;
    }

}
