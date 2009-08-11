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

package org.apache.cxf.systest.jaxrs.sdo;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.cxf.systest.jaxrs.sdo.impl.StructureImpl;


@Path("/structures")
public class SDOResource {
    
    @SuppressWarnings("unchecked")
    @GET
    @Path("/structure")
    @Produces({"application/xml", "application/json" })
    public Structure getStructure() {
        Structure struct = new StructureImpl();
        struct.getTexts().add("text1");
        struct.setText("sdo");
        struct.setInt(3);
        struct.setDbl(123.5);
        return struct;
    }
        
    
}
