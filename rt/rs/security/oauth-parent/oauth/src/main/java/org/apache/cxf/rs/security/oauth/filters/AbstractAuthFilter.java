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
package org.apache.cxf.rs.security.oauth.filters;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import net.oauth.OAuth;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.server.OAuthServlet;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.oauth.data.AccessToken;
import org.apache.cxf.rs.security.oauth.data.Client;
import org.apache.cxf.rs.security.oauth.data.OAuthContext;
import org.apache.cxf.rs.security.oauth.data.OAuthPermission;
import org.apache.cxf.rs.security.oauth.data.UserSubject;
import org.apache.cxf.rs.security.oauth.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth.utils.OAuthUtils;
import org.apache.cxf.security.SecurityContext;

/**
 * Base OAuth filter which can be used to protect end-user endpoints
 */
public class AbstractAuthFilter {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractAuthFilter.class);
    private static final String[] REQUIRED_PARAMETERS = 
        new String[] {
            OAuth.OAUTH_CONSUMER_KEY,
            OAuth.OAUTH_TOKEN,
            OAuth.OAUTH_SIGNATURE_METHOD,
            OAuth.OAUTH_SIGNATURE,
            OAuth.OAUTH_TIMESTAMP,
            OAuth.OAUTH_NONCE
        };
    
    private static final Set<String> ALLOWED_OAUTH_PARAMETERS;
    static {
        ALLOWED_OAUTH_PARAMETERS = new HashSet<String>();
        ALLOWED_OAUTH_PARAMETERS.addAll(Arrays.asList(REQUIRED_PARAMETERS));
        ALLOWED_OAUTH_PARAMETERS.add(OAuth.OAUTH_VERSION);
        ALLOWED_OAUTH_PARAMETERS.add(OAuthConstants.X_OAUTH_SCOPE);
        ALLOWED_OAUTH_PARAMETERS.add(OAuthConstants.OAUTH_CONSUMER_SECRET);
    }
    
    private boolean useUserSubject;
    private OAuthDataProvider dataProvider;

    protected AbstractAuthFilter() {
        
    }
    
    /**
     * Sets {@link OAuthDataProvider} provider.
     * @param provider the provider
     */
    public void setDataProvider(OAuthDataProvider provider) {
        dataProvider = provider;
    }
    
    public void setUseUserSubject(boolean useUserSubject) {
        this.useUserSubject = useUserSubject;
    }

    public boolean isUseUserSubject() {
        return useUserSubject;
    }

    /**
     * Authenticates the third-party consumer and returns
     * {@link OAuthInfo} bean capturing the information about the request. 
     * @param req http request
     * @return OAuth info
     * @see OAuthInfo
     * @throws Exception
     * @throws OAuthProblemException
     */
    protected OAuthInfo handleOAuthRequest(HttpServletRequest req) throws
        Exception, OAuthProblemException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "OAuth security filter for url: {0}", req.getRequestURL());
        }
        
        AccessToken accessToken = null;
        Client client = null;
        
        OAuthMessage oAuthMessage = OAuthServlet.getMessage(new CustomHttpServletWrapper(req), 
                                                            OAuthServlet.getRequestURL(req));
        if (oAuthMessage.getParameter(OAuth.OAUTH_TOKEN) != null) {
            oAuthMessage.requireParameters(REQUIRED_PARAMETERS);

            accessToken = dataProvider.getAccessToken(oAuthMessage.getToken());

            //check if access token is not null
            if (accessToken == null) {
                LOG.warning("Access token is unavailable");
                throw new OAuthProblemException(OAuth.Problems.TOKEN_REJECTED);
            }
            client = accessToken.getClient(); 
            
            OAuthUtils.validateMessage(oAuthMessage, client, accessToken, dataProvider);    
        } else {
            String consumerKey = null;
            String consumerSecret = null;
            
            String authHeader = oAuthMessage.getHeader("Authorization");
            if (authHeader != null) {
                if (authHeader.startsWith("OAuth")) {
                    consumerKey = oAuthMessage.getParameter(OAuth.OAUTH_CONSUMER_KEY);
                    consumerSecret = oAuthMessage.getParameter(OAuthConstants.OAUTH_CONSUMER_SECRET);
                } else if (authHeader.startsWith("Basic")) {
                    AuthorizationPolicy policy = getAuthorizationPolicy(authHeader);
                    if (policy != null) {
                        consumerKey = policy.getUserName();
                        consumerSecret = policy.getPassword();
                    }
                }
            }
            
            if (consumerKey != null) {
                client = dataProvider.getClient(consumerKey);
            }
            if (client == null) {
                LOG.warning("Client is invalid");
                throw new OAuthProblemException(OAuth.Problems.CONSUMER_KEY_UNKNOWN);
            }
            
            if (consumerSecret != null && !consumerSecret.equals(client.getSecretKey())) {
                LOG.warning("Client secret is invalid");
                throw new OAuthProblemException(OAuth.Problems.CONSUMER_KEY_UNKNOWN);
            } else {
                OAuthUtils.validateMessage(oAuthMessage, client, null, dataProvider);
            }
            accessToken = client.getPreAuthorizedToken();
            if (accessToken == null || !accessToken.isPreAuthorized()) {
                LOG.warning("Preauthorized access token is unavailable");
                throw new OAuthProblemException(OAuth.Problems.TOKEN_REJECTED);
            }
        }

        List<OAuthPermission> permissions = accessToken.getScopes();
        List<OAuthPermission> matchingPermissions = new ArrayList<OAuthPermission>();
        
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
            throw new OAuthProblemException(message);
        }
        return new OAuthInfo(accessToken, matchingPermissions);
        
    }
    
    protected AuthorizationPolicy getAuthorizationPolicy(String authorizationHeader) {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        return m != null ? (AuthorizationPolicy)m.get(AuthorizationPolicy.class) : null;
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
            String message = "Invalid request URI";
            LOG.fine(message);
        }
        return foundValidScope;
    }
    
    protected SecurityContext createSecurityContext(HttpServletRequest request, 
                                                    final OAuthInfo info) {
        // TODO: 
        // This custom parameter is only needed by the "oauth" 
        // demo shipped in the distribution; needs to be removed.
        request.setAttribute("oauth_authorities", info.getRoles());
        
        UserSubject subject = info.getToken().getSubject();

        final UserSubject theSubject = subject;
        return new SecurityContext() {

            public Principal getUserPrincipal() {
                String login = AbstractAuthFilter.this.useUserSubject 
                    ? (theSubject != null ? theSubject.getLogin() : null)
                    : info.getToken().getClient().getLoginName();  
                return new SimplePrincipal(login);
            }

            public boolean isUserInRole(String role) {
                List<String> roles = null;
                if (AbstractAuthFilter.this.useUserSubject && theSubject != null) {
                    roles = theSubject.getRoles();    
                } else {
                    roles = info.getRoles();
                }
                return roles == null ? false : roles.contains(role);
            }
             
        };
    }
    
    protected OAuthContext createOAuthContext(OAuthInfo info) {
        UserSubject subject = null;
        if (info.getToken() != null) {
            subject = info.getToken().getSubject();
        }
        return new OAuthContext(subject, info.getMatchedPermissions());
    }
    
    private static class CustomHttpServletWrapper extends HttpServletRequestWrapper {
        public CustomHttpServletWrapper(HttpServletRequest req) {
            super(req);
        }
        
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> params = super.getParameterMap();
            
            if (ALLOWED_OAUTH_PARAMETERS.containsAll(params.keySet())) {
                return params;
            }
            
            Map<String, String[]> newParams = new HashMap<String, String[]>();
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                if (ALLOWED_OAUTH_PARAMETERS.contains(entry.getKey())) {    
                    newParams.put(entry.getKey(), entry.getValue());
                }
            }
            return newParams;
        }
    }
}
