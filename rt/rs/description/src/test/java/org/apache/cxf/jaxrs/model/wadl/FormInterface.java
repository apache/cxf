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
package org.apache.cxf.jaxrs.model.wadl;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

public interface FormInterface {
    @Path("/form1")
    @POST
    void form1(MultivaluedMap<String, String> map);

    @Path("/form2")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    String form2(@FormParam("field1") String f1, @FormParam("field2") String f2);

    @Path("/form3/{id}")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    String form3(@HeaderParam("headerId") String headerId,
                 @PathParam("id") String id,
                 @FormParam("field1") String f1, @FormParam("field2") String f2);

    @Path("/form4/{id}")
    @POST
    @Consumes("multipart/form-data")
    @Produces(MediaType.TEXT_PLAIN)
    String form4(@PathParam("id") String id,
                 @Multipart("field1") String f1, @Multipart("field2") String f2);
}
