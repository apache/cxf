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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import net.oauth.OAuth;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth.data.Client;
import org.apache.cxf.rs.security.oauth.data.RequestToken;
import org.apache.cxf.rs.security.oauth.data.RequestTokenRegistration;
import org.apache.cxf.rs.security.oauth.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth.utils.OAuthUtils;

public class RequestTokenHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(RequestTokenHandler.class);
    private static final String[] REQUIRED_PARAMETERS = 
        new String[] {
            OAuth.OAUTH_CONSUMER_KEY,
            OAuth.OAUTH_SIGNATURE_METHOD,
            OAuth.OAUTH_SIGNATURE,
            OAuth.OAUTH_TIMESTAMP,
            OAuth.OAUTH_NONCE,
            OAuth.OAUTH_CALLBACK
        };
    
    private long tokenLifetime = 3600L;
    private String defaultScope;
    
    public Response handle(MessageContext mc, OAuthDataProvider dataProvider) {
        try {
            OAuthMessage oAuthMessage = 
                OAuthUtils.getOAuthMessage(mc, mc.getHttpServletRequest(), REQUIRED_PARAMETERS);

            Client client = dataProvider
                .getClient(oAuthMessage.getParameter(OAuth.OAUTH_CONSUMER_KEY));
            //client credentials not found
            if (client == null) {
                throw new OAuthProblemException(OAuth.Problems.CONSUMER_KEY_UNKNOWN);
            }

            OAuthUtils.validateMessage(oAuthMessage, client, null, dataProvider);

            String callback = oAuthMessage.getParameter(OAuth.OAUTH_CALLBACK);
            validateCallbackURL(client, callback);

            List<String> scopes = OAuthUtils.parseParamValue(
                    oAuthMessage.getParameter(OAuthConstants.X_OAUTH_SCOPE), defaultScope);
            
            RequestTokenRegistration reg = new RequestTokenRegistration();
            reg.setClient(client);
            reg.setCallback(callback);
            reg.setState(oAuthMessage.getParameter("state"));
            reg.setScopes(scopes);
            reg.setLifetime(tokenLifetime);
            reg.setIssuedAt(System.currentTimeMillis() / 1000);
            
            RequestToken requestToken = dataProvider.createRequestToken(reg);

            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Preparing Temporary Credentials Endpoint correct response");
            }
            //create response
            Map<String, Object> responseParams = new HashMap<String, Object>();
            responseParams.put(OAuth.OAUTH_TOKEN, requestToken.getTokenKey());
            responseParams.put(OAuth.OAUTH_TOKEN_SECRET, requestToken.getTokenSecret());
            responseParams.put(OAuth.OAUTH_CALLBACK_CONFIRMED, Boolean.TRUE);

            String responseBody = OAuth.formEncode(responseParams.entrySet());

            return Response.ok(responseBody).build();
        } catch (OAuthProblemException e) {
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "An OAuth-related problem: {0}", new Object[] {e.fillInStackTrace()});
            }
            int code = e.getHttpStatusCode();
            if (code == 200) {
                code = HttpServletResponse.SC_UNAUTHORIZED; 
            }
            return OAuthUtils.handleException(e, code, String.valueOf(e.getParameters().get("realm")));
        } catch (Exception e) {
            if (LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Unexpected internal server exception: {0}",
                    new Object[] {e.fillInStackTrace()});
            }
            return OAuthUtils.handleException(e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected void validateCallbackURL(Client client,
                                       String oauthCallback) throws OAuthProblemException {

        if (!StringUtils.isEmpty(client.getApplicationURI())
                    && !oauthCallback.startsWith(client.getApplicationURI())) {
            OAuthProblemException problemEx = new OAuthProblemException(
                OAuth.Problems.PARAMETER_REJECTED + " - " + OAuth.OAUTH_CALLBACK);
            problemEx
                .setParameter(OAuthProblemException.HTTP_STATUS_CODE,
                    HttpServletResponse.SC_BAD_REQUEST);
            throw problemEx;
            
        }
        
    }

    public void setTokenLifetime(long tokenLifetime) {
        this.tokenLifetime = tokenLifetime;
    }

    public void setDefaultScope(String defaultScope) {
        this.defaultScope = defaultScope;
    }
            
}
