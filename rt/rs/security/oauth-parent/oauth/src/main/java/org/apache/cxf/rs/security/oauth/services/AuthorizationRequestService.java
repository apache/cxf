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

package org.apache.cxf.rs.security.oauth.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.cxf.rs.security.oauth.data.OAuthAuthorizationData;


/**
 * This resource handles the End User authorising
 * or denying the Client to access its resources.
 * If End User approves the access this resource will
 * redirect End User back to the Client, supplying
 * a request token verifier (aka authorization code)
 */
@Path("/authorize")
public class AuthorizationRequestService extends AbstractOAuthService {

    private AuthorizationRequestHandler handler = new AuthorizationRequestHandler();

    public void setAuthorizationRequestHandler(AuthorizationRequestHandler h) {
        this.handler = h;
    }

    @GET
    @Produces({"application/xhtml+xml", "text/html", "application/xml", "application/json" })
    public Response authorize() {
        Response response = handler.handle(getMessageContext(), getDataProvider());
        if (response.getEntity() instanceof OAuthAuthorizationData) {
            String replyTo = getMessageContext().getUriInfo()
                .getAbsolutePathBuilder().path("decision").build().toString();
            ((OAuthAuthorizationData)response.getEntity()).setReplyTo(replyTo);
        }
        return response;
    }

    @GET
    @Path("/decision")
    @Produces({"application/xhtml+xml",
               "text/html",
               "application/xml;qs=0.9",
               "application/json;qs=0.9",
               "application/x-www-form-urlencoded" })
    public Response authorizeDecision() {
        return authorize();
    }

    @POST
    @Path("/decision")
    @Consumes("application/x-www-form-urlencoded")
    @Produces({"application/xhtml+xml",
               "text/html",
               "application/xml;qs=0.9",
               "application/json;qs=0.9",
               "application/x-www-form-urlencoded" })
    public Response authorizeDecisionForm() {
        return authorizeDecision();
    }
}
