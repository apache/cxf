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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
import org.apache.cxf.security.SecurityContext;


/**
 * The Base Redirection-Based Grant Service
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
    
    /**
     * Handles the initial authorization request by preparing 
     * the authorization challenge data and returning it to the user.
     * Typically the data are expected to be presented in the HTML form 
     * @return the authorization data
     */
    @GET
    @Produces({"application/xhtml+xml", "text/html", "application/xml", "application/json" })
    public Response authorize() {
        MultivaluedMap<String, String> params = getQueryParameters();
        return startAuthorization(params);
    }
    
    /**
     * Processes the end user decision
     * @return The grant value, authorization code or the token
     */
    @GET
    @Path("/decision")
    public Response authorizeDecision() {
        MultivaluedMap<String, String> params = getQueryParameters();
        return completeAuthorization(params);
    }
    
    /**
     * Processes the end user decision
     * @return The grant value, authorization code or the token
     */
    @POST
    @Path("/decision")
    @Consumes("application/x-www-form-urlencoded")
    public Response authorizeDecisionForm(MultivaluedMap<String, String> params) {
        return completeAuthorization(params);
    }
    
    /**
     * Starts the authorization process
     */
    protected Response startAuthorization(MultivaluedMap<String, String> params) {
        // Make sure the end user has authenticated, check if HTTPS is used
        SecurityContext sc = getAndValidateSecurityContext();
        
        Client client = getClient(params);
        
        // Validate the provided request URI, if any, against the ones Client provided
        // during the registration
        String redirectUri = validateRedirectUri(client, params.getFirst(OAuthConstants.REDIRECT_URI)); 
        
        // Enforce the client confidentiality requirements
        if (!OAuthUtils.isGrantSupportedForClient(client, isClientConfidential, supportedGrantType)) {
            return createErrorResponse(params, redirectUri, OAuthConstants.UNAUTHORIZED_CLIENT);
        }
        
        // Check response_type
        String responseType = params.getFirst(OAuthConstants.RESPONSE_TYPE);
        if (responseType == null || !responseType.equals(supportedResponseType)) {
            return createErrorResponse(params, redirectUri, OAuthConstants.UNSUPPORTED_RESPONSE_TYPE);
        }
        
        // Get the requested scopes
        List<String> requestedScope = OAuthUtils.parseScope(params.getFirst(OAuthConstants.SCOPE));
        
        // Create a UserSubject representing the end user 
        UserSubject userSubject = createUserSubject(sc);
        
        // Request a new grant only if no pre-authorized token is available
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
        
        // Convert the requested scopes to OAuthPermission instances
        List<OAuthPermission> permissions = null;
        try {
            permissions = getDataProvider().convertScopeToPermissions(client, requestedScope);
        } catch (OAuthServiceException ex) {
            return createErrorResponse(params, redirectUri, OAuthConstants.INVALID_SCOPE);
        }
    
        // Return the authorization challenge data to the end user 
        OAuthAuthorizationData data = 
            createAuthorizationData(client, params, permissions);
        return Response.ok(data).build();
        
    }
    
    /**
     * Create the authorization challenge data 
     */
    protected OAuthAuthorizationData createAuthorizationData(
        Client client, MultivaluedMap<String, String> params, List<OAuthPermission> perms) {
        
        OAuthAuthorizationData secData = new OAuthAuthorizationData();
        
        addAuthenticityTokenToSession(secData);
                
        secData.setPermissions(perms);
        secData.setProposedScope(OAuthUtils.convertPermissionsToScope(perms));
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
    
    /**
     * Completes the authorization process
     */
    protected Response completeAuthorization(MultivaluedMap<String, String> params) {
        // Make sure the end user has authenticated, check if HTTPS is used
        SecurityContext securityContext = getAndValidateSecurityContext();
        
        // Make sure the session is valid
        if (!compareRequestAndSessionTokens(params.getFirst(OAuthConstants.SESSION_AUTHENTICITY_TOKEN))) {
            throw new WebApplicationException(400);     
        }
        //TODO: additionally we can check that the Principal that got authenticated
        // in startAuthorization is the same that got authenticated in completeAuthorization
        
        Client client = getClient(params);
        String redirectUri = validateRedirectUri(client, params.getFirst(OAuthConstants.REDIRECT_URI));
        
        // Get the end user decision value
        String decision = params.getFirst(OAuthConstants.AUTHORIZATION_DECISION_KEY);
        boolean allow = OAuthConstants.AUTHORIZATION_DECISION_ALLOW.equals(decision);
        
        // Return the error if denied
        if (!allow) {
            return createErrorResponse(params, redirectUri, OAuthConstants.ACCESS_DENIED);
        }
        
        // Check if the end user may have had a chance to down-scope the requested scopes
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
        
        // Request a new grant
        return createGrant(params,
                           client, 
                           redirectUri,
                           requestedScope,
                           approvedScope,
                           userSubject,
                           null);
        
    }
    
    private UserSubject createUserSubject(SecurityContext securityContext) {
        return OAuthUtils.createSubject(securityContext);
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
