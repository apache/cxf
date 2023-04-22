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

package org.apache.cxf.systest.microprofile.rest.client.regex;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("/bookstore/echoxmlbookregex/{id : [5-9]{3,4}}/{language:en|fr}/{format:pdf|epub|mobi}")
public interface BookStoreClientRoot {
    @POST
    @Path("/many")
    @Consumes("application/xml")
    @Produces("application/xml")
    Book echoXmlBookregexMany(Book book, @PathParam("id") String id, 
        @PathParam("language") String language, @PathParam("format") String format);

}