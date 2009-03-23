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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/{a}/{b}/{c}/d")
public class TestResource {

    public TestResource() {
    }
    
    @Path("/resource")
    public TestResource subresource() {
        return this;
    }
    
    @Path("/resource")
    @GET
    @Produces("application/json")
    public String resourceMethod() {
        return "";
    }
    
    @GET
    @Produces("application/xml")
    @Path("/resource1")
    public TestResource xmlResource() {
        return this;
    }
    
    @Path("/resource1")
    @GET
    @Produces("application/json")
    public String jsonResource() {
        return "";
    }
    
    @GET
    @Path("/")
    @Produces("application/xml")
    public String listMethod() {
        return "This is a list method";
    }
    
    @GET
    @Path("/{e}")
    @Produces("application/xml")
    public String readMethod() {
        return "This is a list method";
    }
    
    @GET
    @Path("/{a}/{b}")
    public String limitedPath() {
        return "This is a list method";
    }
    
    @GET
    @Path(value = "/{e}")
    @Produces("application/json")
    public String unlimitedPath() {
        return "This is a list method";
    }
    
    @GET
    @Path("/{e}/bar/baz/baz")
    @Produces("application/json")
    public String readMethod2() {
        return "This is a list method";
    }
    
    @GET
    @Path("{id:custom}")
    @Produces("application/bar")
    public String readBar() {
        return "This is a bar method";
    }
    
    @GET
    @Path("{id:custom}")
    @Produces("application/foo")
    public String readFoo() {
        return "This is a foo method";
    }
}


