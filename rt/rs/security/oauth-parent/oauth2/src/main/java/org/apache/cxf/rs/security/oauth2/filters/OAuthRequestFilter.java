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
package org.apache.cxf.rs.security.oauth2.filters;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenValidation;
import org.apache.cxf.rs.security.oauth2.common.OAuthContext;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.services.AbstractAccessTokenValidator;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.security.SecurityContext;

/**
 * JAX-RS OAuth2 filter which can be used to protect the end-user endpoints
 */
@Provider
@PreMatching
public class OAuthRequestFilter extends AbstractAccessTokenValidator 
    implements ContainerRequestFilter {
    private static final Logger LOG = LogUtils.getL7dLogger(OAuthRequestFilter.class);
    
    private boolean useUserSubject;
    private boolean audienceIsEndpointAddress;
    
    public void filter(ContainerRequestContext context) {
        Message m = JAXRSUtils.getCurrentMessage();
        if (isCorsRequest(m)) {
            return;
        }
        
        // Get the access token
        AccessTokenValidation accessTokenV = getAccessTokenValidation(); 
        
        // Find the scopes which match the current request
        
        List<OAuthPermission> permissions = accessTokenV.getTokenScopes();
        List<OAuthPermission> matchingPermissions = new ArrayList<OAuthPermission>();
        
        HttpServletRequest req = getMessageContext().getHttpServletRequest();
        for (OAuthPermission perm : permissions) {
            boolean uriOK = checkRequestURI(req, perm.getUris());
            boolean verbOK = checkHttpVerb(req, perm.getHttpVerbs());
            if (uriOK && verbOK) {
                matchingPermissions.add(perm);
            }
        }
        
        if (permissions.size() > 0 && matchingPermissions.isEmpty()) {
            String message = "Client has no valid permissions";
            LOG.warning(message);
            throw new WebApplicationException(403);
        }
      
        // Create the security context and make it available on the message
        SecurityContext sc = createSecurityContext(req, accessTokenV);
        m.put(SecurityContext.class, sc);
        
        // Also set the OAuthContext
        OAuthContext oauthContext = new OAuthContext(accessTokenV.getTokenSubject(),
                                                     accessTokenV.getClientSubject(),
                                                     matchingPermissions,
                                                     accessTokenV.getTokenGrantType());
        
        oauthContext.setClientId(accessTokenV.getClientId());
        oauthContext.setTokenKey(accessTokenV.getTokenKey());
        oauthContext.setTokenAudience(accessTokenV.getAudience());
        
        m.setContent(OAuthContext.class, oauthContext);
    }

    protected boolean checkHttpVerb(HttpServletRequest req, List<String> verbs) {
        if (!verbs.isEmpty() 
            && !verbs.contains(req.getMethod())) {
            String message = "Invalid http verb";
            LOG.fine(message);
            return false;
        }
        return true;
    }
    
    protected boolean checkRequestURI(HttpServletRequest request, List<String> uris) {
        
        if (uris.isEmpty()) {
            return true;
        }
        String servletPath = request.getPathInfo();
        boolean foundValidScope = false;
        for (String uri : uris) {
            if (OAuthUtils.checkRequestURI(servletPath, uri)) {
                foundValidScope = true;
                break;
            }
        }
        if (!foundValidScope) {
            String message = "Invalid request URI: " + request.getRequestURL().toString();
            LOG.warning(message);
        }
        return foundValidScope;
    }
    
    public void setUseUserSubject(boolean useUserSubject) {
        this.useUserSubject = useUserSubject;
    }
    
    
    protected SecurityContext createSecurityContext(HttpServletRequest request, 
                                                    AccessTokenValidation accessTokenV) {
        UserSubject resourceOwnerSubject = accessTokenV.getTokenSubject();
        UserSubject clientSubject = accessTokenV.getClientSubject();

        final UserSubject theSubject = 
            OAuthRequestFilter.this.useUserSubject ? resourceOwnerSubject : clientSubject;
                    
        return new SecurityContext() {

            public Principal getUserPrincipal() {
                return theSubject != null ? new SimplePrincipal(theSubject.getLogin()) : null;
            }

            public boolean isUserInRole(String role) {
                if (theSubject == null) {
                    return false;
                }
                return theSubject.getRoles().contains(role);
            }
        };
    }
    
    protected boolean isCorsRequest(Message m) {
        //Redirection-based flows (Implicit Grant Flow specifically) may have 
        //the browser issuing CORS preflight OPTIONS request. 
        //org.apache.cxf.rs.security.cors.CrossOriginResourceSharingFilter can be
        //used to handle preflights but local preflights (to be handled by the service code)
        // will be blocked by this filter unless CORS filter has done the initial validation
        // and set a message "local_preflight" property to true
        return MessageUtils.isTrue(m.get("local_preflight"));
    }

    protected boolean validateAudience(String audience) {
        if (audience == null) {
            return true;
        }
        
        boolean isValid = super.validateAudience(audience);
        if (isValid && audienceIsEndpointAddress) {
            String requestPath = (String)PhaseInterceptorChain.getCurrentMessage().get(Message.REQUEST_URL);
            isValid = requestPath.startsWith(audience);
        }
        return isValid;
    }
    
    public boolean isAudienceIsEndpointAddress() {
        return audienceIsEndpointAddress;
    }

    public void setAudienceIsEndpointAddress(boolean audienceIsEndpointAddress) {
        this.audienceIsEndpointAddress = audienceIsEndpointAddress;
    }
    
}
