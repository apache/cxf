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


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.abdera.model.Feed;

@Path("/")
public class AtomBookStore2 extends AtomBookStore {
    
    @GET
    @Path("/")
    @Produces({"application/atom+xml", "application/json" })
    public Feed getBooksAsFeed(@Context UriInfo uParam) {
        
        return super.getBooksAsFeed(uParam);
        
    }
    
    @Context
    public void setUriInfo(UriInfo ui) {
        super.uField = ui;
    }
}


