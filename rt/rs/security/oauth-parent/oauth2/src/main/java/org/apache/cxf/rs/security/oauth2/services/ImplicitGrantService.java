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

import java.net.URI;
import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;


/**
 * Redirection-based Implicit Grant Service
 * 
 * This resource handles the End User authorising
 * or denying the Client embedded in the Web agent.
 * 
 * We can consider having a single authorization service dealing with either
 * authorization code or implicit grant.
 */
@Path("/authorize-implicit")
public class ImplicitGrantService extends RedirectionBasedGrantService {
    // For a client to validate that this client is a targeted recipient.
    private boolean reportClientId;
    
    public ImplicitGrantService() {
        super(OAuthConstants.TOKEN_RESPONSE_TYPE, OAuthConstants.IMPLICIT_GRANT);
    }
    
    protected Response createGrant(MultivaluedMap<String, String> params,
                                   Client client,
                                   String redirectUri,
                                   List<String> requestedScope,
                                   List<String> approvedScope,
                                   UserSubject userSubject,
                                   ServerAccessToken preAuthorizedToken) {
        ServerAccessToken token = null;
        if (preAuthorizedToken == null) {
            AccessTokenRegistration reg = new AccessTokenRegistration();
            reg.setClient(client);
            reg.setGrantType(OAuthConstants.IMPLICIT_GRANT);
            reg.setSubject(userSubject);
            reg.setRequestedScope(requestedScope);        
            reg.setApprovedScope(approvedScope);
            token = getDataProvider().createAccessToken(reg);
        } else {
            token = preAuthorizedToken;
        }

   
       // return the code by appending it as a fragment parameter to the redirect URI
        
        String state = params.getFirst(OAuthConstants.STATE);
        StringBuilder sb = getUriWithFragment(state, redirectUri);
        if (state != null) {
            sb.append("&");
        }
        sb.append(OAuthConstants.ACCESS_TOKEN).append("=").append(token.getTokenKey());
        sb.append("&")
            .append(OAuthConstants.ACCESS_TOKEN_TYPE).append("=").append(token.getTokenType());
        if (reportClientId) {
            sb.append("&")
                .append(OAuthConstants.CLIENT_ID).append("=").append(client.getClientId());
        }
        if (isWriteOptionalParameters()) {
            sb.append("&").append(OAuthConstants.ACCESS_TOKEN_EXPIRES_IN)
                .append("=").append(token.getExpiresIn());
            // Reporting scope is required if the approved scope is different and
            // optional - otherwise; lets always report it for now if it is non-empty 
            List<OAuthPermission> perms = token.getScopes();
            if (!perms.isEmpty()) {
                String scope = OAuthUtils.convertPermissionsToScope(perms);
                sb.append("&").append(OAuthConstants.SCOPE).append("=")
                    .append(HttpUtils.queryEncode(scope));
            }
            //TODO: also report other token parameters if any if needed  
        }
        
        return Response.seeOther(URI.create(sb.toString())).build();
    }
    
    protected Response createErrorResponse(MultivaluedMap<String, String> params,
                                           String redirectUri,
                                           String error) {
        String state = params.getFirst(OAuthConstants.STATE);
        StringBuilder sb = getUriWithFragment(state, redirectUri);
        if (state != null) {
            sb.append("&");
        }
        sb.append(OAuthConstants.ERROR_KEY).append("=").append(error);
        return Response.seeOther(URI.create(sb.toString())).build();
    }
    
    private StringBuilder getUriWithFragment(String state, String redirectUri) {
        StringBuilder sb = new StringBuilder();
        sb.append(redirectUri);
        sb.append("#");
        if (state != null) {
            sb.append(OAuthConstants.STATE).append("=").append(state);   
        }
        return sb;
    }

    public void setReportClientId(boolean reportClientId) {
        this.reportClientId = reportClientId;
    }

    @Override
    protected boolean canSupportPublicClient(Client c) {
        return true;
    }
    
    @Override
    protected boolean canRedirectUriBeEmpty(Client c) {
        return false;
    }
    
}


