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

package org.apache.cxf.rs.security.oauth2.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

@Path("authorize")
public class AuthorizationService {
    
    private Map<String, RedirectionBasedGrantService> servicesMap = 
        new HashMap<String, RedirectionBasedGrantService>();
    
    @Context 
    public void setMessageContext(MessageContext context) {
        for (RedirectionBasedGrantService service : servicesMap.values()) {
            service.setMessageContext(context);
        }
    }
    @GET
    @Produces({"application/xhtml+xml", "text/html", "application/xml", "application/json" })
    public Response authorize(@QueryParam(OAuthConstants.RESPONSE_TYPE) String responseType) {
        RedirectionBasedGrantService service = getService(responseType);
        if (service != null) {
            return service.authorize();
        } else {
            return reportInvalidResponseType();
        }
    }
    
    @GET
    @Path("/decision")
    public Response authorizeDecision(@QueryParam(OAuthConstants.RESPONSE_TYPE) String responseType) {
        RedirectionBasedGrantService service = getService(responseType);
        if (service != null) {
            return service.authorizeDecision();
        } else {
            return reportInvalidResponseType();
        }
    }
    
    /**
     * Processes the end user decision
     * @return The grant value, authorization code or the token
     */
    @POST
    @Path("/decision")
    @Consumes("application/x-www-form-urlencoded")
    public Response authorizeDecisionForm(MultivaluedMap<String, String> params) {
        String responseType = params.getFirst(OAuthConstants.RESPONSE_TYPE);
        RedirectionBasedGrantService service = getService(responseType);
        if (service != null) {
            return service.authorizeDecisionForm(params);
        } else {
            return reportInvalidResponseType();
        }
    }
    
    private RedirectionBasedGrantService getService(String responseType) {
        return responseType == null ? null : servicesMap.get(responseType);
    }
    
    public void setServices(List<RedirectionBasedGrantService> services) {
        for (RedirectionBasedGrantService service : services) {
            for (String responseType : service.getSupportedResponseTypes()) {
                servicesMap.put(responseType, service);
            }
        }
        
    }
    
    protected Response reportInvalidResponseType() {
        return JAXRSUtils.toResponseBuilder(400)
            .type("application/json").entity(new OAuthError(OAuthConstants.UNSUPPORTED_RESPONSE_TYPE)).build();
    }
}
