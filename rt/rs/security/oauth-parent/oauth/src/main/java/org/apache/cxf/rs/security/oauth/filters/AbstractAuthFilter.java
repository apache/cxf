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
    protected static final String USE_USER_SUBJECT = "org.apache.cxf.rs.security.oauth.use_user_subject";
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
        ALLOWED_OAUTH_PARAMETERS.add(OAuthConstants.X_OAUTH_SCOPE);
        ALLOWED_OAUTH_PARAMETERS.add(OAuthConstants.X_OAUTH_URI);
    }
    
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
    
    /**
     * Authenticates the third-party consumer and returns
     * {@link OAuthInfo} bean capturing the information about the request. 
     * @param req http request
     * @return OAuth info
     * @see OAuthInfo
     * @throws Exception
     * @throws OAuthProblemException
     */
    protected OAuthInfo handleOAuthRequest(HttpServletRequest req,
                                           boolean useUserSubject) throws
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
            
        } else {
            // TODO: the secret may not be included and only used to create a signature
            //       so the header will effectively be similar to the one used during 
            //       RequestToken requests; we'd need to handle this case too
            String consumerKey = oAuthMessage.getParameter(OAuth.OAUTH_CONSUMER_KEY);
            String consumerSecret = oAuthMessage.getParameter("oauth_consumer_secret");
            client = dataProvider.getClient(consumerKey);
            if (client == null || consumerSecret == null || !consumerSecret.equals(client.getSecretKey())) {
                LOG.warning("Client is invalid");
                throw new OAuthProblemException(OAuth.Problems.CONSUMER_KEY_UNKNOWN);
            }
        }

        OAuthUtils.validateMessage(oAuthMessage, client, accessToken, dataProvider);

        //check valid URI
        checkRequestURI(req, OAuthUtils.getAllUris(client, accessToken));
        
        List<OAuthPermission> permissions = dataProvider.getPermissionsInfo(
                OAuthUtils.getAllScopes(client, accessToken));
        
        for (OAuthPermission perm : permissions) {
            checkRequestURI(req, perm.getUris());
            if (!perm.getHttpVerbs().isEmpty() 
                && !perm.getHttpVerbs().contains(req.getMethod())) {
                String message = "Invalid http verb";
                LOG.warning(message);
                throw new OAuthProblemException(message);
            }
            checkNoAccessTokenIsAllowed(client, accessToken, perm);
        }
        
        return new OAuthInfo(client, accessToken, permissions, useUserSubject);
        
    }
    
    protected void checkNoAccessTokenIsAllowed(Client client, AccessToken token,
            OAuthPermission perm) throws OAuthProblemException {
        if (token == null && perm.isAuthorizationKeyRequired()) {
            throw new OAuthProblemException();
        }
    }
    
    protected void checkRequestURI(HttpServletRequest request, List<String> uris)
        throws OAuthProblemException {
        
        if (uris.isEmpty()) {
            return;
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
            LOG.warning(message);
            throw new OAuthProblemException(message);
        }
    }
    
    protected SecurityContext createSecurityContext(HttpServletRequest request, 
                                                    final OAuthInfo info) {
        request.setAttribute("oauth_authorities", info.getRoles());
        final UserSubject subject = info.getToken().getSubject();
        return new SecurityContext() {

            public Principal getUserPrincipal() {
                String login = info.useUserSubject() 
                    ? (subject != null ? subject.getLogin() : null)
                    : info.getClient().getLoginName();  
                return new SimplePrincipal(login);
            }

            public boolean isUserInRole(String role) {
                if (info.useUserSubject()) {
                    return subject != null
                        ? info.getToken().getSubject().getRoles().contains(role) : false;    
                } else {
                    List<String> roles = info.getRoles();
                    for (String authority : roles) {
                        if (authority.equals(role)) {
                            return true;
                        }
                    }
                }
                return false;
            }
             
        };
    }
    
    protected OAuthContext createOAuthContext(OAuthInfo info) {
        UserSubject subject = null;
        if (info.getToken() != null) {
            subject = info.getToken().getSubject();
        }
        return new OAuthContext(subject, info.getPermissions());
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
