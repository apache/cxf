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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeDataProvider;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeRegistration;
import org.apache.cxf.rs.security.oauth2.grants.code.ServerAuthorizationCodeGrant;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.security.LoginSecurityContext;


/**
 * This resource handles the End User authorising
 * or denying the Client to access its resources.
 * If End User approves the access this resource will
 * redirect End User back to the Client, supplying 
 * a request token verifier (aka authorization code)
 */
@Path("/authorize")
public class AuthorizationCodeGrantService extends AbstractOAuthService {

    private static final long DEFAULT_CODE_GRANT_LIFETIME = 3600L;
    
    private long grantLifetime = DEFAULT_CODE_GRANT_LIFETIME;
    
    @GET
    @Produces({"application/xhtml+xml", "text/html", "application/xml", "application/json" })
    public Response authorize() {
        MultivaluedMap<String, String> params = getQueryParameters();
        return startAuthorization(params);
    }
    
    @GET
    @Path("/decision")
    public Response authorizeDecision() {
        MultivaluedMap<String, String> params = getQueryParameters();
        return completeAuthorization(params);
    }
    
    @POST
    @Path("/decision")
    @Consumes("application/x-www-form-urlencoded")
    public Response authorizeDecisionForm(MultivaluedMap<String, String> params) {
        return completeAuthorization(params);
    }
    
    protected Response startAuthorization(MultivaluedMap<String, String> params) {
        Client client = getClient(params); 
        String redirectUri = validateRedirectUri(client, params.getFirst(OAuthConstants.REDIRECT_URI)); 
        if (!client.isConfidential()) {
            return createErrorResponse(params, redirectUri, OAuthConstants.UNAUTHORIZED_CLIENT);
        }
        String responseType = params.getFirst(OAuthConstants.RESPONSE_TYPE);
        if (responseType == null || !responseType.equals(OAuthConstants.CODE_RESPONSE_TYPE)) {
            return createErrorResponse(params, redirectUri, OAuthConstants.UNSUPPORTED_RESPONSE_TYPE);
        }
        
        List<OAuthPermission> permissions = null;
        try {
            List<String> list = parseScope(params.getFirst(OAuthConstants.SCOPE));
            permissions = ((AuthorizationCodeDataProvider)getDataProvider())
                .convertScopeToPermissions(list);
        } catch (OAuthServiceException ex) {
            return createErrorResponse(params, redirectUri, OAuthConstants.INVALID_SCOPE);
        }
        OAuthAuthorizationData data = 
            createAuthorizationData(client, params, permissions);
        return Response.ok(data).build();
        
    }
    
    public void setGrantLifetime(long lifetime) {
        this.grantLifetime = lifetime;
    }
    
    protected OAuthAuthorizationData createAuthorizationData(
        Client client, MultivaluedMap<String, String> params, List<OAuthPermission> perms) {
        
        OAuthAuthorizationData secData = new OAuthAuthorizationData();
        
        addAuthenticityTokenToSession(secData);
                
        secData.setPermissions(perms);
        
        StringBuilder sb = new StringBuilder();
        for (OAuthPermission perm : perms) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(perm.getPermission());
        }
        secData.setProposedScope(sb.toString());
        
        secData.setClientId(client.getClientId());
        secData.setRedirectUri(params.getFirst(OAuthConstants.REDIRECT_URI));
        secData.setState(params.getFirst(OAuthConstants.STATE));
        
        secData.setApplicationName(client.getApplicationName()); 
        secData.setApplicationWebUri(client.getApplicationWebUri());
        secData.setApplicationDescription(client.getApplicationDescription());
        secData.setApplicationLogoUri(client.getApplicationLogoUri());
        
        String replyTo = getMessageContext().getUriInfo()
            .getAbsolutePathBuilder().path("decision").build().toString();
        secData.setReplyTo(replyTo);
        
        return secData;
    }
    
    protected Response completeAuthorization(MultivaluedMap<String, String> params) {
        
        if (!compareRequestAndSessionTokens(params.getFirst(OAuthConstants.SESSION_AUTHENTICITY_TOKEN))) {
            throw new WebApplicationException(400);     
        }
        
        Client client = getClient(params);
        String originalRedirectUri = params.getFirst(OAuthConstants.REDIRECT_URI);
        String actualRedirectUri = validateRedirectUri(client, originalRedirectUri);
        
        String decision = params.getFirst(OAuthConstants.AUTHORIZATION_DECISION_KEY);
        boolean allow = OAuthConstants.AUTHORIZATION_DECISION_ALLOW.equals(decision);

        if (!allow) {
            return createErrorResponse(params, actualRedirectUri, OAuthConstants.ACCESS_DENIED);
        }
        
        AuthorizationCodeRegistration codeReg = new AuthorizationCodeRegistration(); 
        
        codeReg.setClient(client);
        codeReg.setRedirectUri(originalRedirectUri);
        codeReg.setLifetime(grantLifetime);
        codeReg.setIssuedAt(System.currentTimeMillis() / 1000);
        
        List<String> requestedScope = parseScope(params.getFirst(OAuthConstants.SCOPE));
        codeReg.setRequestedScope(requestedScope);

        List<String> approvedScope = new LinkedList<String>(); 
        for (String rScope : requestedScope) {
            String param = params.getFirst(rScope + "_status");
            if (param != null && OAuthConstants.AUTHORIZATION_DECISION_ALLOW.equals(param)) {
                approvedScope.add(rScope);
            }
        }
        if (!requestedScope.containsAll(approvedScope)) {
            return createErrorResponse(params, actualRedirectUri, OAuthConstants.INVALID_SCOPE);
        }
        // the decision was allow but the approved scopes end up being empty
        // in this case we default to the requestedScope
        if (approvedScope.isEmpty()) {
            approvedScope = requestedScope;
        }
        codeReg.setApprovedScope(approvedScope);
        
        SecurityContext sc = getMessageContext().getSecurityContext();
        List<String> roleNames = Collections.emptyList();
        if (sc instanceof LoginSecurityContext) {
            roleNames = new ArrayList<String>();
            Set<Principal> roles = ((LoginSecurityContext)sc).getUserRoles();
            for (Principal p : roles) {
                roleNames.add(p.getName());
            }
        }
        codeReg.setSubject(new UserSubject(sc.getUserPrincipal() == null 
            ? null : sc.getUserPrincipal().getName(), roleNames));
        
        ServerAuthorizationCodeGrant grant = null;
        try {
            grant = ((AuthorizationCodeDataProvider)getDataProvider()).createCodeGrant(codeReg);
        } catch (OAuthServiceException ex) {
            return createErrorResponse(params, actualRedirectUri, OAuthConstants.ACCESS_DENIED);
        }
        
        UriBuilder ub = getRedirectUriBuilder(params.getFirst(OAuthConstants.STATE), actualRedirectUri);
        ub.queryParam("code", grant.getCode());
        return Response.seeOther(ub.build()).build();    
    }
    
    protected Response createErrorResponse(MultivaluedMap<String, String> params,
                                           String redirectUri,
                                           String error) {
        UriBuilder ub = getRedirectUriBuilder(params.getFirst(OAuthConstants.STATE), redirectUri);
        ub.queryParam("error", error);
        return Response.seeOther(ub.build()).build();
    }
    
    private List<String> parseScope(String requestedScope) {
        List<String> list = new LinkedList<String>();
        if (requestedScope != null) {
            String[] scopeValues = requestedScope.split(" ");
            for (String scope : scopeValues) {
                if (!StringUtils.isEmpty(scope)) {        
                    list.add(scope);
                }
            }
        }
        return list;
    }
    
    private UriBuilder getRedirectUriBuilder(String state, String redirectUri) {
        UriBuilder ub = UriBuilder.fromUri(redirectUri);
        if (state != null) { 
            ub.queryParam(OAuthConstants.STATE, state);
        }
        return ub;
    }
    
    protected String validateRedirectUri(Client client, String redirectUri) {
        
        List<String> uris = client.getRedirectUris();
        if (redirectUri != null) {
            String webUri = client.getApplicationWebUri();
            if (uris.size() > 0 && !uris.contains(redirectUri)
                || webUri != null && !redirectUri.startsWith(webUri)) {
                redirectUri = null;
            } 
        } else if (uris.size() == 1) {
            redirectUri = uris.get(0);
        }
        if (redirectUri == null) {
            reportInvalidRequestError("Client Redirect Uri is invalid");
        }
        return redirectUri;
    }
    
    private void addAuthenticityTokenToSession(OAuthAuthorizationData secData) {
        HttpSession session = getMessageContext().getHttpServletRequest().getSession();
        String value = UUID.randomUUID().toString();
        secData.setAuthenticityToken(value);
        session.setAttribute(OAuthConstants.SESSION_AUTHENTICITY_TOKEN, value);
    }
    
    private boolean compareRequestAndSessionTokens(String requestToken) {
        HttpSession session = getMessageContext().getHttpServletRequest().getSession();
        String sessionToken = (String)session.getAttribute(OAuthConstants.SESSION_AUTHENTICITY_TOKEN);
        
        if (StringUtils.isEmpty(sessionToken)) {
            return false;
        }
        
        session.removeAttribute(OAuthConstants.SESSION_AUTHENTICITY_TOKEN);
        return requestToken.equals(sessionToken);
    }
}
