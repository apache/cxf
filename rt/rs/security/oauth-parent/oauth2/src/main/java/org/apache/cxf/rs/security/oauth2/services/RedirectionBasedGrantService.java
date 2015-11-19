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
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.OAuthRedirectionState;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.provider.ResourceOwnerNameProvider;
import org.apache.cxf.rs.security.oauth2.provider.SessionAuthenticityTokenProvider;
import org.apache.cxf.rs.security.oauth2.provider.SubjectCreator;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.security.SecurityContext;


/**
 * The Base Redirection-Based Grant Service
 */
public abstract class RedirectionBasedGrantService extends AbstractOAuthService {
    private Set<String> supportedResponseTypes;
    private String supportedGrantType;
    private boolean partialMatchScopeValidation;
    private boolean useRegisteredRedirectUriIfPossible = true;
    private SessionAuthenticityTokenProvider sessionAuthenticityTokenProvider;
    private SubjectCreator subjectCreator;
    private ResourceOwnerNameProvider resourceOwnerNameProvider;
    private int maxDefaultSessionInterval;
    private boolean matchRedirectUriWithApplicationUri;
    
    protected RedirectionBasedGrantService(String supportedResponseType,
                                           String supportedGrantType) {
        this(Collections.singleton(supportedResponseType), supportedGrantType);
    }
    protected RedirectionBasedGrantService(Set<String> supportedResponseTypes,
                                           String supportedGrantType) {
        this.supportedResponseTypes = supportedResponseTypes;
        this.supportedGrantType = supportedGrantType;
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
        SecurityContext sc = getAndValidateSecurityContext(params);
        // Create a UserSubject representing the end user 
        UserSubject userSubject = createUserSubject(sc);
        Client client = getClient(params);
        return startAuthorization(params, userSubject, client);
    }
        
    protected Response startAuthorization(MultivaluedMap<String, String> params, 
                                          UserSubject userSubject,
                                          Client client) {    
        
        // Validate the provided request URI, if any, against the ones Client provided
        // during the registration
        String redirectUri = validateRedirectUri(client, params.getFirst(OAuthConstants.REDIRECT_URI)); 
        
        // Enforce the client confidentiality requirements
        if (!OAuthUtils.isGrantSupportedForClient(client, canSupportPublicClient(client), supportedGrantType)) {
            return createErrorResponse(params, redirectUri, OAuthConstants.UNAUTHORIZED_CLIENT);
        }
        
        // Check response_type
        String responseType = params.getFirst(OAuthConstants.RESPONSE_TYPE);
        if (responseType == null || !supportedResponseTypes.contains(responseType)) {
            return createErrorResponse(params, redirectUri, OAuthConstants.UNSUPPORTED_RESPONSE_TYPE);
        }
        // Get the requested scopes
        String providedScope = params.getFirst(OAuthConstants.SCOPE);
        List<String> requestedScope = null;
        try {
            requestedScope = OAuthUtils.getRequestedScopes(client, 
                                                           providedScope, 
                                                           partialMatchScopeValidation);
        } catch (OAuthServiceException ex) {
            return createErrorResponse(params, redirectUri, OAuthConstants.INVALID_SCOPE);
        }
        // Convert the requested scopes to OAuthPermission instances
        List<OAuthPermission> requestedPermissions = null;
        try {
            requestedPermissions = getDataProvider().convertScopeToPermissions(client, requestedScope);
        } catch (OAuthServiceException ex) {
            return createErrorResponse(params, redirectUri, OAuthConstants.INVALID_SCOPE);
        }
        // Validate the audience
        if (!OAuthUtils.validateAudience(params.getFirst(OAuthConstants.CLIENT_AUDIENCE), 
                                         client.getRegisteredAudiences())) {
            throw new OAuthServiceException(new OAuthError(OAuthConstants.INVALID_REQUEST));
        }
    
        // Request a new grant only if no pre-authorized token is available
        ServerAccessToken preAuthorizedToken = getDataProvider().getPreauthorizedToken(
            client, requestedScope, userSubject, supportedGrantType);
        final boolean authorizationCanBeSkipped = 
            preAuthorizedToken != null 
            || canAuthorizationBeSkipped(client, requestedScope, requestedPermissions);
        
        // Populate the authorization challenge data 
        OAuthAuthorizationData data = 
            createAuthorizationData(client, params, redirectUri, userSubject, requestedPermissions, 
                                    authorizationCanBeSkipped);
        
        if (authorizationCanBeSkipped) {
            List<OAuthPermission> approvedScopes = 
                preAuthorizedToken != null ? preAuthorizedToken.getScopes() : requestedPermissions; 
            return createGrant(data,
                               client, 
                               requestedScope,
                               OAuthUtils.convertPermissionsToScopeList(approvedScopes),
                               userSubject,
                               preAuthorizedToken);
        }
        
        return Response.ok(data).build();
        
    }
    
    protected boolean canAuthorizationBeSkipped(Client client, 
                                                List<String> requestedScope, 
                                                List<OAuthPermission> permissions) {
        return false;
    }

    /**
     * Create the authorization challenge data 
     */
    protected OAuthAuthorizationData createAuthorizationData(Client client, 
                                                             MultivaluedMap<String, String> params,
                                                             String redirectUri, 
                                                             UserSubject subject,
                                                             List<OAuthPermission> perms,
                                                             boolean authorizationCanBeSkipped) {
        
        OAuthAuthorizationData secData = new OAuthAuthorizationData();
        
        secData.setState(params.getFirst(OAuthConstants.STATE));
        secData.setRedirectUri(redirectUri);
        secData.setAudience(params.getFirst(OAuthConstants.CLIENT_AUDIENCE));
        secData.setNonce(params.getFirst(OAuthConstants.NONCE));
        secData.setClientId(client.getClientId());
        secData.setProposedScope(params.getFirst(OAuthConstants.SCOPE));
        if (!authorizationCanBeSkipped) {
            secData.setPermissions(perms);
            secData.setApplicationName(client.getApplicationName()); 
            secData.setApplicationWebUri(client.getApplicationWebUri());
            secData.setApplicationDescription(client.getApplicationDescription());
            secData.setApplicationLogoUri(client.getApplicationLogoUri());
            secData.setApplicationCertificates(client.getApplicationCertificates());
            Map<String, String> extraProperties = client.getProperties();
            secData.setExtraApplicationProperties(extraProperties);
            String replyTo = getMessageContext().getUriInfo()
                .getAbsolutePathBuilder().path("decision").build().toString();
            secData.setReplyTo(replyTo);
            personalizeData(secData, subject);
            
            addAuthenticityTokenToSession(secData, params, subject);
        }
        
        return secData;
    }
    protected OAuthRedirectionState recreateRedirectionStateFromSession(
        UserSubject subject, MultivaluedMap<String, String> params, String sessionToken) {
        OAuthRedirectionState state = null; 
        if (sessionAuthenticityTokenProvider != null) {
            state = sessionAuthenticityTokenProvider.getSessionState(super.getMessageContext(), 
                                                                     sessionToken,
                                                                     subject);
        }
        if (state == null) {
            state = new OAuthRedirectionState();
            state.setClientId(params.getFirst(OAuthConstants.CLIENT_ID));
            state.setRedirectUri(params.getFirst(OAuthConstants.REDIRECT_URI));
            state.setAudience(params.getFirst(OAuthConstants.CLIENT_AUDIENCE));
            state.setProposedScope(params.getFirst(OAuthConstants.SCOPE));
            state.setState(params.getFirst(OAuthConstants.STATE));
        }
        return state;
    }
    
    protected void personalizeData(OAuthAuthorizationData data, UserSubject userSubject) {
        if (resourceOwnerNameProvider != null) {
            data.setEndUserName(resourceOwnerNameProvider.getName(userSubject));
        }
    }
    
    /**
     * Completes the authorization process
     */
    protected Response completeAuthorization(MultivaluedMap<String, String> params) {
        // Make sure the end user has authenticated, check if HTTPS is used
        SecurityContext securityContext = getAndValidateSecurityContext(params);
        UserSubject userSubject = createUserSubject(securityContext);
        
        // Make sure the session is valid
        String sessionTokenParamName = params.getFirst(OAuthConstants.SESSION_AUTHENTICITY_TOKEN_PARAM_NAME);
        if (sessionTokenParamName == null) {
            sessionTokenParamName = OAuthConstants.SESSION_AUTHENTICITY_TOKEN;
        }
        String sessionToken = params.getFirst(sessionTokenParamName);
        if (sessionToken == null || !compareRequestAndSessionTokens(sessionToken, params, userSubject)) {
            throw ExceptionUtils.toBadRequestException(null, null);     
        }
        
        OAuthRedirectionState state = 
            recreateRedirectionStateFromSession(userSubject, params, sessionToken);
        Client client = getClient(state.getClientId());
        String redirectUri = validateRedirectUri(client, state.getRedirectUri());
        
        // Get the end user decision value
        String decision = params.getFirst(OAuthConstants.AUTHORIZATION_DECISION_KEY);
        boolean allow = OAuthConstants.AUTHORIZATION_DECISION_ALLOW.equals(decision);
        
        // Return the error if denied
        if (!allow) {
            return createErrorResponse(params, redirectUri, OAuthConstants.ACCESS_DENIED);
        }
        
        // Check if the end user may have had a chance to down-scope the requested scopes
        List<String> requestedScope = OAuthUtils.parseScope(state.getProposedScope());
        List<String> approvedScope = new LinkedList<String>(); 
        for (String rScope : requestedScope) {
            String param = params.getFirst(rScope + "_status");
            if (param != null && OAuthConstants.AUTHORIZATION_DECISION_ALLOW.equals(param)) {
                approvedScope.add(rScope);
            }
        }
        if (!requestedScope.containsAll(approvedScope)
            || !OAuthUtils.validateScopes(requestedScope, client.getRegisteredScopes(), 
                                         partialMatchScopeValidation)) {
            return createErrorResponse(params, redirectUri, OAuthConstants.INVALID_SCOPE);
        }
        
        // Request a new grant
        return createGrant(state,
                           client, 
                           requestedScope,
                           approvedScope,
                           userSubject,
                           null);
        
    }
    
    public void setSessionAuthenticityTokenProvider(SessionAuthenticityTokenProvider sessionAuthenticityTokenProvider) {
        this.sessionAuthenticityTokenProvider = sessionAuthenticityTokenProvider;
    }
    
    public void setSubjectCreator(SubjectCreator creator) {
        this.subjectCreator = creator;
    }
    
    protected UserSubject createUserSubject(SecurityContext securityContext) {
        UserSubject subject = null;
        if (subjectCreator != null) {
            subject = subjectCreator.createUserSubject(getMessageContext());
            if (subject != null) {
                return subject; 
            }
        }
        return OAuthUtils.createSubject(getMessageContext(), securityContext);
    }
    
    protected Response createErrorResponse(MultivaluedMap<String, String> params,
                                           String redirectUri,
                                           String error) {
        return createErrorResponse(params.getFirst(OAuthConstants.STATE), redirectUri, error);
    }
    protected abstract Response createErrorResponse(String state,
                                                    String redirectUri,
                                                    String error);
    
    protected abstract Response createGrant(OAuthRedirectionState state,
                                            Client client,
                                            List<String> requestedScope,
                                            List<String> approvedScope,
                                            UserSubject userSubject,
                                            ServerAccessToken preAuthorizedToken);
    
    protected SecurityContext getAndValidateSecurityContext(MultivaluedMap<String, String> params) {
        SecurityContext securityContext =  
            (SecurityContext)getMessageContext().get(SecurityContext.class.getName());
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
        checkTransportSecurity();
        return securityContext;
    }
    
    protected String validateRedirectUri(Client client, String redirectUri) {
        
        List<String> uris = client.getRedirectUris();
        if (redirectUri != null) {
            if (!uris.contains(redirectUri)) {
                reportInvalidRequestError("Client Redirect Uri is invalid");
            } 
        } else if (uris.size() == 1 && useRegisteredRedirectUriIfPossible) {
            redirectUri = uris.get(0);
        }
        if (redirectUri == null && uris.size() == 0 && !canRedirectUriBeEmpty(client)) {
            reportInvalidRequestError("Client Redirect Uri is invalid");    
        }
        if (redirectUri != null && matchRedirectUriWithApplicationUri
            && client.getApplicationWebUri() != null
            && !redirectUri.startsWith(client.getApplicationWebUri())) {
            reportInvalidRequestError("Client Redirect Uri is invalid");
        }
        return redirectUri;
    }
    
    private void addAuthenticityTokenToSession(OAuthAuthorizationData secData,
                                               MultivaluedMap<String, String> params,
                                               UserSubject subject) {
        final String sessionToken;
        if (this.sessionAuthenticityTokenProvider != null) {
            sessionToken = sessionAuthenticityTokenProvider.createSessionToken(getMessageContext(),
                                                                               params,
                                                                               subject,
                                                                               secData);
        } else {
            sessionToken = OAuthUtils.setSessionToken(getMessageContext(), maxDefaultSessionInterval);
        }
        secData.setAuthenticityToken(sessionToken);
    }
    
    private boolean compareRequestAndSessionTokens(String requestToken,
                                                   MultivaluedMap<String, String> params,
                                                   UserSubject subject) {
        final String sessionToken;
        if (this.sessionAuthenticityTokenProvider != null) {
            sessionToken = sessionAuthenticityTokenProvider.removeSessionToken(getMessageContext(),
                                                                               params,
                                                                               subject);
        } else {
            sessionToken = OAuthUtils.getSessionToken(getMessageContext());
        }
        if (StringUtils.isEmpty(sessionToken)) {
            return false;
        } else {
            return requestToken.equals(sessionToken);
        }
    }
    
    /**
     * Get the {@link Client} reference
     * @param params request parameters
     * @return Client the client reference 
     * @throws {@link javax.ws.rs.WebApplicationException} if no matching Client is found, 
     *         the error is returned directly to the end user without 
     *         following the redirect URI if any
     */
    protected Client getClient(String clientId) {
        Client client = null;
        
        try {
            client = getValidClient(clientId);
        } catch (OAuthServiceException ex) {
            if (ex.getError() != null) {
                reportInvalidRequestError(ex.getError(), null);
            }
        }
        
        if (client == null) {
            reportInvalidRequestError("Client ID is invalid", null);
        }
        return client;
        
    }
    protected Client getClient(MultivaluedMap<String, String> params) {
        return this.getClient(params.getFirst(OAuthConstants.CLIENT_ID));
    }
    protected String getSupportedGrantType() {
        return this.supportedGrantType;
    }
    public void setResourceOwnerNameProvider(ResourceOwnerNameProvider resourceOwnerNameProvider) {
        this.resourceOwnerNameProvider = resourceOwnerNameProvider;
    }

    public void setPartialMatchScopeValidation(boolean partialMatchScopeValidation) {
        this.partialMatchScopeValidation = partialMatchScopeValidation;
    }
    /**
     * If a client does not include a redirect_uri parameter but has an exactly one
     * pre-registered redirect_uri then use that redirect_uri
     * @param use allows to use a single registered redirect_uri if set to true (default)
     */
    public void setUseRegisteredRedirectUriIfPossible(boolean use) {
        this.useRegisteredRedirectUriIfPossible = use;
    }
    
    protected abstract boolean canSupportPublicClient(Client c);
    
    protected abstract boolean canRedirectUriBeEmpty(Client c);

    public void setMaxDefaultSessionInterval(int maxDefaultSessionInterval) {
        this.maxDefaultSessionInterval = maxDefaultSessionInterval;
    }

    public void setMatchRedirectUriWithApplicationUri(boolean matchRedirectUriWithApplicationUri) {
        this.matchRedirectUriWithApplicationUri = matchRedirectUriWithApplicationUri;
    }
}
