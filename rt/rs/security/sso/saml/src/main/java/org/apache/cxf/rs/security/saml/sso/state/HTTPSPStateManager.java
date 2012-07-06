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
package org.apache.cxf.rs.security.saml.sso.state;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("state")
public class HTTPSPStateManager implements SPStateManager {
    private MemorySPStateManager manager = new MemorySPStateManager();
    
    @POST
    @Path("/request/{relayState}")
    @Consumes("application/xml")
    public void setRequestState(@Encoded @PathParam("relayState") String relayState, 
                                RequestState state) {
        manager.setRequestState(relayState, state);
    }

    @DELETE
    @Path("/request/{relayState}")
    @Produces("application/xml")
    public RequestState removeRequestState(@Encoded @PathParam("relayState") String relayState) {
        return manager.removeRequestState(relayState);
    }

    @POST
    @Path("/response/{contextKey}")
    @Consumes("application/xml")
    public void setResponseState(@Encoded @PathParam("contextKey") String contextKey, 
                                 ResponseState state) {
        manager.setResponseState(contextKey, state);

    }

    @GET
    @Path("/response/{contextKey}")
    @Produces("application/xml")
    public ResponseState getResponseState(@Encoded @PathParam("contextKey") String contextKey) {
        return manager.getResponseState(contextKey);        
    }

    @DELETE
    @Path("/response/{contextKey}")
    @Produces("application/xml")
    public ResponseState removeResponseState(String contextKey) {
        return manager.getResponseState(contextKey);
    }

    @POST
    @Path("close")
    public void close() throws IOException {
        // TODO Auto-generated method stub

    }
}
