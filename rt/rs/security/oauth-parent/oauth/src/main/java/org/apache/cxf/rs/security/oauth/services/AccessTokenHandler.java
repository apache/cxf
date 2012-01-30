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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import net.oauth.OAuth;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth.data.AccessToken;
import org.apache.cxf.rs.security.oauth.data.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth.data.RequestToken;
import org.apache.cxf.rs.security.oauth.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth.utils.OAuthUtils;


public class AccessTokenHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(AccessTokenHandler.class);
    private static final String[] REQUIRED_PARAMETERS = 
        new String[] {
            OAuth.OAUTH_CONSUMER_KEY,
            OAuth.OAUTH_TOKEN,
            OAuth.OAUTH_SIGNATURE_METHOD,
            OAuth.OAUTH_SIGNATURE,
            OAuth.OAUTH_TIMESTAMP,
            OAuth.OAUTH_NONCE
        };
    
    public Response handle(MessageContext mc, OAuthDataProvider dataProvider) {
        try {
            OAuthMessage oAuthMessage = 
                OAuthUtils.getOAuthMessage(mc, mc.getHttpServletRequest(), REQUIRED_PARAMETERS);

            RequestToken requestToken = dataProvider.getRequestToken(oAuthMessage.getToken());
            if (requestToken == null) {
                throw new OAuthProblemException(OAuth.Problems.TOKEN_REJECTED);
            }
            
            String oauthVerifier = oAuthMessage.getParameter(OAuth.OAUTH_VERIFIER);
            if (oauthVerifier == null) {
                if (requestToken.getSubject() != null && requestToken.isPreAuthorized()) {
                    LOG.fine("Preauthorized request token");
                } else {
                    throw new OAuthProblemException(OAuthConstants.VERIFIER_INVALID);
                }
            } else if (!oauthVerifier.equals(requestToken.getVerifier())) {
                throw new OAuthProblemException(OAuthConstants.VERIFIER_INVALID);
            }
            
            OAuthUtils.validateMessage(oAuthMessage, requestToken.getClient(), requestToken,
                                       dataProvider);

            AccessTokenRegistration reg = new AccessTokenRegistration();
            reg.setRequestToken(requestToken);
            AccessToken accessToken = dataProvider.createAccessToken(reg);

            //create response
            Map<String, Object> responseParams = new HashMap<String, Object>();
            responseParams.put(OAuth.OAUTH_TOKEN, accessToken.getTokenKey());
            responseParams.put(OAuth.OAUTH_TOKEN_SECRET, accessToken.getTokenSecret());

            String responseString = OAuth.formEncode(responseParams.entrySet());
            return Response.ok(responseString).build();

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
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Server Exception: {0}", new Object[] {e.fillInStackTrace()});
            }
            return OAuthUtils.handleException(e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
