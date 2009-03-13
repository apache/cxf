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

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/bookstore/{id}/{id2}/{id3}")
public class BookStoreSubresourcesOnly {

    @Path("/sub1")
    public BookStoreSubresourcesOnly getItself() { 
        return this;
    }
    
    @Path("/sub2")
    public BookStoreSubresourcesOnly getItself2(@PathParam("id") String id1, @PathParam("id3") String id3) { 
        return this;
    }
    
    @Path("/{id4}/sub3")
    public BookStoreSubresourcesOnly getItself3(@PathParam("id4") String id4) { 
        return this;
    }
}


