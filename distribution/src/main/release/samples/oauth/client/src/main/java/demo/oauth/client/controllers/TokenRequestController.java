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
package demo.oauth.client.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.client.OAuthClient;
import net.oauth.client.URLConnectionClient;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import demo.oauth.client.model.Common;
import demo.oauth.client.model.OAuthParams;


@Controller
public class TokenRequestController {

    @RequestMapping("/tokenRequest")
    protected ModelAndView handleRequest(@ModelAttribute("oAuthParams") OAuthParams oAuthParams,
                                         HttpServletRequest request)
        throws Exception {

        String oauthToken = oAuthParams.getOauthToken();

        String tokenRequestEndpoint = oAuthParams.getTokenRequestEndpoint();
        String clientID = oAuthParams.getClientID();

        if (tokenRequestEndpoint == null || "".equals(tokenRequestEndpoint)) {
            oAuthParams.setErrorMessage("Missing token request URI");
        }

        if (clientID == null || "".equals(clientID)) {
            oAuthParams.setErrorMessage("Missing consumer key");
        }

        if (oauthToken == null || "".equals(oauthToken)) {
            oAuthParams.setErrorMessage("Missing oauth token");
        }

        String verifier = oAuthParams.getOauthVerifier();
        if (verifier == null || "".equals(verifier)) {
            oAuthParams.setErrorMessage("Missing oauth verifier");
        }

        if (oAuthParams.getErrorMessage() == null) {
            OAuthClient client = new OAuthClient(new URLConnectionClient());
            OAuthServiceProvider provider = new OAuthServiceProvider(
                oAuthParams.getTemporaryCredentialsEndpoint(),
                oAuthParams.getResourceOwnerAuthorizationEndpoint(), tokenRequestEndpoint);

            OAuthConsumer consumer = new OAuthConsumer(null, clientID,
                oAuthParams.getClientSecret(),
                provider);
            OAuthAccessor accessor = new OAuthAccessor(consumer);
            accessor.requestToken = oauthToken;
            accessor.tokenSecret = Common.findCookieValue(request, "tokenSec");
            
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put(OAuth.OAUTH_SIGNATURE_METHOD, oAuthParams.getSignatureMethod());
            parameters.put(OAuth.OAUTH_NONCE, UUID.randomUUID().toString());
            parameters.put(OAuth.OAUTH_TIMESTAMP, String.valueOf(System.currentTimeMillis() / 1000));
            parameters.put(OAuth.OAUTH_TOKEN, oauthToken);
            parameters.put(OAuth.OAUTH_VERIFIER, oAuthParams.getOauthVerifier());


            try {
                client.getAccessToken(accessor, OAuthMessage.GET, parameters.entrySet());
                oAuthParams.setOauthToken(accessor.accessToken);
            } catch (Exception e) {
                oAuthParams.setErrorMessage(e.toString());
                oAuthParams.setOauthToken(oauthToken);
                return new ModelAndView("tokenRequest");
            }
            oAuthParams.setOauthTokenSecret(accessor.tokenSecret);
        }

        oAuthParams.setClientID(Common.findCookieValue(request, "clientID"));
        oAuthParams.setClientSecret(Common.findCookieValue(request, "clientSecret"));

        return new ModelAndView("accessToken");
    }

}
