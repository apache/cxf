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

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.security.LoginSecurityContext;
import org.apache.cxf.security.SecurityContext;


/**
 * This resource handles the End User authorising
 * or denying the Client to access its resources.
 * If End User approves the access this resource will
 * redirect End User back to the Client, supplying 
 * a request token verifier (aka authorization code)
 */
public abstract class RedirectionBasedGrantService extends AbstractOAuthService {
    private String supportedResponseType;
    private String supportedGrantType;
    private boolean isClientConfidential;
    protected RedirectionBasedGrantService(String supportedResponseType,
                                           String supportedGrantType,
                                           boolean isConfidential) {
        this.supportedResponseType = supportedResponseType;
        this.supportedGrantType = supportedGrantType;
        this.isClientConfidential = isConfidential;
    }
    
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
        SecurityContext sc = getAndValidateSecurityContext();
        
        Client client = getClient(params); 
        String redirectUri = validateRedirectUri(client, params.getFirst(OAuthConstants.REDIRECT_URI)); 
        
        if (!OAuthUtils.isGrantSupportedForClient(client, isClientConfidential, supportedGrantType)) {
            return createErrorResponse(params, redirectUri, OAuthConstants.UNAUTHORIZED_CLIENT);
        }
        String responseType = params.getFirst(OAuthConstants.RESPONSE_TYPE);
        if (responseType == null || !responseType.equals(supportedResponseType)) {
            return createErrorResponse(params, redirectUri, OAuthConstants.UNSUPPORTED_RESPONSE_TYPE);
        }
        List<String> requestedScope = OAuthUtils.parseScope(params.getFirst(OAuthConstants.SCOPE));
        
        UserSubject userSubject = createUserSubject(sc);
        ServerAccessToken preauthorizedToken = getDataProvider().getPreauthorizedToken(
            client, userSubject, supportedGrantType);
        if (preauthorizedToken != null) {
            return createGrant(params,
                               client, 
                               redirectUri,
                               requestedScope,
                               Collections.<String>emptyList(),
                               userSubject,
                               preauthorizedToken);
        }
        
        List<OAuthPermission> permissions = null;
        try {
            permissions = getDataProvider().convertScopeToPermissions(client, requestedScope);
        } catch (OAuthServiceException ex) {
            return createErrorResponse(params, redirectUri, OAuthConstants.INVALID_SCOPE);
        }
    
        OAuthAuthorizationData data = 
            createAuthorizationData(client, params, permissions);
        return Response.ok(data).build();
        
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
        SecurityContext securityContext = getAndValidateSecurityContext();
        
        if (!compareRequestAndSessionTokens(params.getFirst(OAuthConstants.SESSION_AUTHENTICITY_TOKEN))) {
            throw new WebApplicationException(400);     
        }
        //TODO: additionally we can check that the Principal that got authenticated
        // in startAuthorization is the same that got authenticated in completeAuthorization
        
        Client client = getClient(params);
        String redirectUri = validateRedirectUri(client, params.getFirst(OAuthConstants.REDIRECT_URI));
        
        String decision = params.getFirst(OAuthConstants.AUTHORIZATION_DECISION_KEY);
        boolean allow = OAuthConstants.AUTHORIZATION_DECISION_ALLOW.equals(decision);

        if (!allow) {
            return createErrorResponse(params, redirectUri, OAuthConstants.ACCESS_DENIED);
        }
        
        List<String> requestedScope = OAuthUtils.parseScope(params.getFirst(OAuthConstants.SCOPE));
        List<String> approvedScope = new LinkedList<String>(); 
        for (String rScope : requestedScope) {
            String param = params.getFirst(rScope + "_status");
            if (param != null && OAuthConstants.AUTHORIZATION_DECISION_ALLOW.equals(param)) {
                approvedScope.add(rScope);
            }
        }
        if (!requestedScope.containsAll(approvedScope)) {
            return createErrorResponse(params, redirectUri, OAuthConstants.INVALID_SCOPE);
        }
        
        UserSubject userSubject = createUserSubject(securityContext);
        
        return createGrant(params,
                           client, 
                           redirectUri,
                           requestedScope,
                           approvedScope,
                           userSubject,
                           null);
        
    }
    
    private UserSubject createUserSubject(SecurityContext securityContext) {
        List<String> roleNames = Collections.emptyList();
        if (securityContext instanceof LoginSecurityContext) {
            roleNames = new ArrayList<String>();
            Set<Principal> roles = ((LoginSecurityContext)securityContext).getUserRoles();
            for (Principal p : roles) {
                roleNames.add(p.getName());
            }
        }
        return 
            new UserSubject(securityContext.getUserPrincipal().getName(), roleNames);
    }
    
    protected abstract Response createErrorResponse(MultivaluedMap<String, String> params,
                                                    String redirectUri,
                                                    String error);
    
    protected abstract Response createGrant(MultivaluedMap<String, String> params,
                                            Client client,
                                            String redirectUri,
                                            List<String> requestedScope,
                                            List<String> approvedScope,
                                            UserSubject userSubject,
                                            ServerAccessToken preAuthorizedToken);
    
    private SecurityContext getAndValidateSecurityContext() {
        SecurityContext securityContext =  
            (SecurityContext)getMessageContext().get(SecurityContext.class.getName());
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            throw new WebApplicationException(401);
        }
        checkTransportSecurity();
        return securityContext;
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
