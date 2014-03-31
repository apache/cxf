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
package org.apache.cxf.rs.security.saml.sso;

import java.net.URI;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.rs.security.saml.sso.state.RequestState;

@Path("sso")
public class RequestAssertionConsumerService extends AbstractRequestAssertionConsumerHandler {
    @POST
    @Produces(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processSamlResponse(@FormParam(SSOConstants.SAML_RESPONSE) String encodedSamlResponse,
                                        @FormParam(SSOConstants.RELAY_STATE) String relayState) {
        return doProcessSamlResponse(encodedSamlResponse, relayState, true);
        
    }
    
    @GET
    public Response getSamlResponse(@QueryParam(SSOConstants.SAML_RESPONSE) String encodedSamlResponse,
                                    @QueryParam(SSOConstants.RELAY_STATE) String relayState) {
        return doProcessSamlResponse(encodedSamlResponse, relayState, false);
    }
    
    protected Response doProcessSamlResponse(String encodedSamlResponse,
                                             String relayState,
                                             boolean postBinding) {
        RequestState requestState = processRelayState(relayState);
       
        String contextCookie = createSecurityContext(requestState,
                                                    encodedSamlResponse,
                                                   relayState,
                                                   postBinding);
       
        // Finally, redirect to the service provider endpoint
        URI targetURI = getTargetURI(requestState.getTargetAddress());
        return Response.seeOther(targetURI).header("Set-Cookie", contextCookie).build();
    }
    
    private URI getTargetURI(String targetAddress) {
        if (targetAddress != null) {
            try {
                return URI.create(targetAddress);
            } catch (IllegalArgumentException ex) {
                reportError("INVALID_TARGET_URI");
            }
        } else {
            reportError("MISSING_TARGET_URI");
        }
        throw ExceptionUtils.toBadRequestException(null, null);
    }
}
