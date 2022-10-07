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
package odata.server;

import java.util.ArrayList;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import odata.jakarta.ODataHttpHandlerImpl;

import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;

@Path("/DemoService.svc")
public class JaxrsODataService {

    @GET
    @Path("{id:.*}")
    public Response service(@Context HttpServletRequest req, @Context HttpServletResponse resp) {

        String requestMapping = req.getContextPath() + req.getServletPath() + "/DemoService.svc";
        req.setAttribute("requestMapping", requestMapping);
        // create odata handler and configure it with EdmProvider and Processor
        OData odata = OData.newInstance();
        ServiceMetadata edm = odata.createServiceMetadata(new DemoEdmProvider(),
                                                          new ArrayList<EdmxReference>());
        ODataHttpHandlerImpl handler = new ODataHttpHandlerImpl(odata, edm);
        handler.register(new DemoEntityCollectionProcessor());

        // let the handler do the work
        handler.process(req, resp);
        return Response.ok().build();
    }
    
}
