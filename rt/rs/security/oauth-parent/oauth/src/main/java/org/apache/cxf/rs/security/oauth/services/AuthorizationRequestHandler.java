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

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import net.oauth.OAuth;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth.data.AuthorizationInput;
import org.apache.cxf.rs.security.oauth.data.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth.data.OAuthPermission;
import org.apache.cxf.rs.security.oauth.data.RequestToken;
import org.apache.cxf.rs.security.oauth.data.UserSubject;
import org.apache.cxf.rs.security.oauth.provider.DefaultOAuthValidator;
import org.apache.cxf.rs.security.oauth.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth.utils.OAuthUtils;
import org.apache.cxf.security.LoginSecurityContext;
import org.apache.cxf.security.SecurityContext;


public class AuthorizationRequestHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(AuthorizationRequestHandler.class);
    private static final String[] REQUIRED_PARAMETERS = 
        new String[] {
            OAuth.OAUTH_TOKEN
        };
    
    public Response handle(MessageContext mc, OAuthDataProvider dataProvider) {
        HttpServletRequest request = mc.getHttpServletRequest();
        try {
            OAuthMessage oAuthMessage = 
                OAuthUtils.getOAuthMessage(mc, request, REQUIRED_PARAMETERS);
            new DefaultOAuthValidator().checkSingleParameter(oAuthMessage);

            RequestToken token = dataProvider.getRequestToken(oAuthMessage.getToken());
            if (token == null) {
                throw new OAuthProblemException(OAuth.Problems.TOKEN_REJECTED);
            }
            
            OAuthAuthorizationData secData = new OAuthAuthorizationData();
            if (!compareRequestSessionTokens(request, oAuthMessage)) {
                addAuthenticityTokenToSession(secData, request);
                return Response.ok(
                        addAdditionalParams(secData, dataProvider, token)).build();
            }
            
            String decision = oAuthMessage.getParameter(OAuthConstants.AUTHORIZATION_DECISION_KEY);
            boolean allow = OAuthConstants.AUTHORIZATION_DECISION_ALLOW.equals(decision);

            Map<String, String> queryParams = new HashMap<String, String>();
            if (allow) {
                SecurityContext sc = (SecurityContext)mc.get(SecurityContext.class.getName());
                List<String> roleNames = Collections.emptyList();
                if (sc instanceof LoginSecurityContext) {
                    roleNames = new ArrayList<String>();
                    Set<Principal> roles = ((LoginSecurityContext)sc).getUserRoles();
                    for (Principal p : roles) {
                        roleNames.add(p.getName());
                    }
                }
                token.setSubject(new UserSubject(sc.getUserPrincipal() == null 
                    ? null : sc.getUserPrincipal().getName(), roleNames));
                
                AuthorizationInput input = new AuthorizationInput();
                input.setToken(token);
                 
                Set<OAuthPermission> approvedScopesSet = new HashSet<OAuthPermission>();
                
                List<OAuthPermission> originalScopes = token.getScopes(); 
                for (OAuthPermission perm : originalScopes) {
                    String param = oAuthMessage.getParameter(perm.getPermission() + "_status");
                    if (param != null && OAuthConstants.AUTHORIZATION_DECISION_ALLOW.equals(param)) {
                        approvedScopesSet.add(perm);
                    }
                }
                List<OAuthPermission> approvedScopes = new LinkedList<OAuthPermission>(approvedScopesSet);
                if (approvedScopes.isEmpty()) {
                    approvedScopes = originalScopes;
                } else if (approvedScopes.size() < originalScopes.size()) {
                    for (OAuthPermission perm : originalScopes) {
                        if (perm.isDefault() && !approvedScopes.contains(perm)) {
                            approvedScopes.add(perm);    
                        }
                    }
                }
                
                input.setApprovedScopes(approvedScopes);
                
                String verifier = dataProvider.finalizeAuthorization(input);
                queryParams.put(OAuth.OAUTH_VERIFIER, verifier);
            } else {
                dataProvider.removeToken(token);
            }
            queryParams.put(OAuth.OAUTH_TOKEN, token.getTokenKey());
            if (token.getState() != null) {
                queryParams.put("state", token.getState());
            }
            String callbackValue = getCallbackValue(token);
            if (OAuthConstants.OAUTH_CALLBACK_OOB.equals(callbackValue)) {
                OOBAuthorizationResponse bean = convertQueryParamsToOOB(queryParams);
                return Response.ok().type(MediaType.TEXT_HTML).entity(bean).build();
            } else {
                URI callbackURI = buildCallbackURI(callbackValue, queryParams);
                return Response.seeOther(callbackURI).build();
            }
            
        } catch (OAuthProblemException e) {
            LOG.log(Level.WARNING, "An OAuth related problem: {0}", new Object[]{e.fillInStackTrace()});
            int code = e.getHttpStatusCode();
            if (code == HttpServletResponse.SC_OK) {
                code = e.getProblem() == OAuth.Problems.CONSUMER_KEY_UNKNOWN
                    ? 401 : 400;
            }
            return OAuthUtils.handleException(mc, e, code);
        } catch (OAuthServiceException e) {
            return OAuthUtils.handleException(mc, e, HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unexpected internal server exception: {0}",
                new Object[] {e.fillInStackTrace()});
            return OAuthUtils.handleException(mc, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected String getCallbackValue(RequestToken token) throws OAuthProblemException {
        String callback = token.getCallback();
        if (callback == null) {
            callback = token.getClient().getApplicationURI();
        }
        if (callback == null) {
            throw new OAuthProblemException(OAuth.Problems.TOKEN_REJECTED);
        }
        return callback;
    }
    
    private URI buildCallbackURI(String callback, final Map<String, String> queryParams) {

        UriBuilder builder = UriBuilder.fromUri(callback);
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }

        return builder.build(); 
    }
    
    private OOBAuthorizationResponse convertQueryParamsToOOB(Map<String, String> queryParams) {

        OOBAuthorizationResponse oob = new OOBAuthorizationResponse();
        oob.setRequestToken(queryParams.get(OAuth.OAUTH_TOKEN));
        oob.setVerifier(queryParams.get(OAuth.OAUTH_VERIFIER));
        oob.setState(queryParams.get("state"));
        return oob; 
    }
    
    protected OAuthAuthorizationData addAdditionalParams(OAuthAuthorizationData secData,
                                                         OAuthDataProvider dataProvider,
                                                         RequestToken token) throws OAuthProblemException {
        secData.setOauthToken(token.getTokenKey());
        secData.setApplicationName(token.getClient().getApplicationName()); 
        secData.setApplicationURI(token.getClient().getApplicationURI());
        secData.setCallbackURI(getCallbackValue(token));
        secData.setApplicationDescription(token.getClient().getApplicationDescription());
        secData.setLogoUri(token.getClient().getLogoUri());
        secData.setPermissions(token.getScopes());
        
        return secData;
    }
    
    private void addAuthenticityTokenToSession(OAuthAuthorizationData secData,
            HttpServletRequest request) {
        HttpSession session = request.getSession();
        String value = UUID.randomUUID().toString();
        
        secData.setAuthenticityToken(value);
        session.setAttribute(OAuthConstants.AUTHENTICITY_TOKEN, value);
    }
    
    private boolean compareRequestSessionTokens(HttpServletRequest request,
            OAuthMessage oAuthMessage) {
        HttpSession session = request.getSession();
        String requestToken = null; 
        try {
            requestToken = oAuthMessage.getParameter(OAuthConstants.AUTHENTICITY_TOKEN);
        } catch (IOException ex) {
            return false;
        }
        String sessionToken = (String) session.getAttribute(OAuthConstants.AUTHENTICITY_TOKEN);
        
        if (StringUtils.isEmpty(requestToken) || StringUtils.isEmpty(sessionToken)) {
            return false;
        }
        
        boolean b = requestToken.equals(sessionToken);
        session.removeAttribute(OAuthConstants.AUTHENTICITY_TOKEN);
        return b;
    }

    
}
