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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import net.oauth.OAuth;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.oauth.data.Client;
import org.apache.cxf.rs.security.oauth.data.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth.data.RequestToken;
import org.apache.cxf.rs.security.oauth.provider.DefaultOAuthValidator;
import org.apache.cxf.rs.security.oauth.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth.utils.OAuthUtils;


public class AuthorizationRequestHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(AuthorizationRequestHandler.class);
    private static final String[] REQUIRED_PARAMETERS = 
        new String[] {
            OAuth.OAUTH_TOKEN
        };
    
    public Response handle(HttpServletRequest request, OAuthDataProvider dataProvider) {

        try {
            OAuthMessage oAuthMessage = 
                OAuthUtils.getOAuthMessage(request, REQUIRED_PARAMETERS);
            new DefaultOAuthValidator().checkSingleParameter(oAuthMessage);

            RequestToken token = dataProvider.getRequestToken(oAuthMessage.getToken());
            if (token == null) {
                throw new OAuthProblemException(OAuth.Problems.TOKEN_REJECTED);
            }
            
            OAuthAuthorizationData secData = new OAuthAuthorizationData();
            if (!compareRequestSessionTokens(request)) {
                secData.setPermissions(
                        dataProvider.getPermissionsInfo(token.getPermissions()));
                secData.setScopes(token.getScopes());
                addAuthenticityTokenToSession(secData, request);
                return Response.ok(addAdditionalParams(secData, token)).build();
            }
            
            String decision = request.getParameter(OAuthConstants.AUTHORIZATION_DECISION_KEY);
            Client clientInfo = token.getClient();
            if (!OAuthConstants.AUTHORIZATION_DECISION_ALLOW.equals(decision)) {
                //user not authorized client
                secData.setCallback(clientInfo.getCallbackURL());
                return Response.ok(addAdditionalParams(secData, token)).build();
            }

            String verifier = dataProvider.createRequestTokenVerifier(token);
            

            String callbackURL = clientInfo.getCallbackURL();

            Map<String, String> queryParams = new HashMap<String, String>();
            queryParams.put(OAuth.OAUTH_VERIFIER, verifier);
            queryParams.put(OAuth.OAUTH_TOKEN, token.getTokenString());
            if (token.getState() != null) {
                queryParams.put("state", token.getState());
            }
            callbackURL = buildCallbackUrl(callbackURL, queryParams);


            return Response.seeOther(URI.create(callbackURL))
                    .build();
        } catch (OAuthProblemException e) {
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "An OAuth related problem: {0}", new Object[]{e.fillInStackTrace()});
            }
            return OAuthUtils.handleException(e, e.getHttpStatusCode(),
                    String.valueOf(e.getParameters().get("realm")));
        } catch (Exception e) {
            if (LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Server exception: {0}", new Object[]{e.fillInStackTrace()});
            }
            return OAuthUtils.handleException(e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected String buildCallbackUrl(String callbackURL, final Map<String, String> queryParams) {

        boolean containsQuestionMark = callbackURL.contains("?");


        StringBuffer query = new StringBuffer(OAuthUtils.format(queryParams.entrySet(), "UTF-8"));
        StringBuffer url = new StringBuffer(callbackURL);

        if (!StringUtils.isEmpty(url.toString())) {
            if (containsQuestionMark) {
                url.append("&").append(query);
            } else {
                url.append("?").append(query);
            }
        }

        return url.toString();
    }
    
    protected OAuthAuthorizationData addAdditionalParams(OAuthAuthorizationData secData,
                                                         RequestToken token) {
        secData.setOauthToken(token.getTokenString());
        secData.setApplicationName(token.getClient().getApplicationName()); 
        secData.setUserName(token.getClient().getLoginName());
      
        return secData;
    }
    
    private void addAuthenticityTokenToSession(OAuthAuthorizationData secData,
            HttpServletRequest request) {
        HttpSession session = request.getSession();
        String value = UUID.randomUUID().toString();
        
        secData.setAuthenticityToken(value);
        session.setAttribute(OAuthConstants.AUTHENTICITY_TOKEN, value);
    }
    
    private boolean compareRequestSessionTokens(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String requestToken = request.getParameter(OAuthConstants.AUTHENTICITY_TOKEN);
        String sessionToken = (String) session.getAttribute(OAuthConstants.AUTHENTICITY_TOKEN);
        
        if (StringUtils.isEmpty(requestToken) || StringUtils.isEmpty(sessionToken)) {
            return false;
        }
        
        boolean b = requestToken.equals(sessionToken);
        session.removeAttribute(OAuthConstants.AUTHENTICITY_TOKEN);
        return b;
    }
}
