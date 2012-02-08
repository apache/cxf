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
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.ParameterStyle;
import net.oauth.client.OAuthClient;
import net.oauth.client.URLConnectionClient;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import demo.oauth.client.model.OAuthParams;

@Controller
public class TemporaryCredentialsController {

    @RequestMapping("/handleTemporaryCredentials")
    public ModelAndView handleRequest(@ModelAttribute(value = "oAuthParams") OAuthParams oAuthParams,
                                      HttpServletResponse response) {

        OAuthServiceProvider provider;
        OAuthConsumer consumer;
        OAuthAccessor accessor;

        OAuthClient client = new OAuthClient(new URLConnectionClient());

        oAuthParams.setErrorMessage(null);
        String temporaryCredentialsEndpointUrl = oAuthParams.getTemporaryCredentialsEndpoint();
        if (temporaryCredentialsEndpointUrl == null || "".equals(temporaryCredentialsEndpointUrl)) {
            oAuthParams.setErrorMessage("Missing temporary credentials endpoint url");
        }
        String clientId = oAuthParams.getClientID();
        if (clientId == null || "".equals(clientId)) {
            oAuthParams.setErrorMessage("Missing client identifier");
        }
        String secret = oAuthParams.getClientSecret();
        if (secret == null || "".equals(secret)) {
            oAuthParams.setErrorMessage("Missing client shared-secret");
        }

        if (oAuthParams.getErrorMessage() == null) {
            provider = new OAuthServiceProvider(temporaryCredentialsEndpointUrl,
                oAuthParams.getResourceOwnerAuthorizationEndpoint(), oAuthParams.getTokenRequestEndpoint());
            consumer = new OAuthConsumer(null, clientId,
                secret,
                provider);
            accessor = new OAuthAccessor(consumer);

            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put(OAuth.OAUTH_SIGNATURE_METHOD, oAuthParams.getSignatureMethod());
            parameters.put(OAuth.OAUTH_NONCE, UUID.randomUUID().toString());
            parameters.put(OAuth.OAUTH_TIMESTAMP, String.valueOf(System.currentTimeMillis() / 1000));
            parameters.put(OAuth.OAUTH_CALLBACK, oAuthParams.getCallbackURL());
            parameters.put("realm", "private");
            parameters.put("scope", "read_info modify_info");


            try {
                accessor.consumer
                    .setProperty(OAuthClient.PARAMETER_STYLE, ParameterStyle.AUTHORIZATION_HEADER);
                client.getRequestToken(accessor, OAuthMessage.POST, parameters.entrySet());
            } catch (Exception e) {
                oAuthParams.setErrorMessage(e.toString());
            }

            oAuthParams.setOauthToken(accessor.requestToken);
            oAuthParams.setOauthTokenSecret(accessor.tokenSecret);
            Cookie cId = new Cookie("clientID", oAuthParams.getClientID());
            Cookie cSec = new Cookie("clientSecret", oAuthParams.getClientSecret());
            Cookie tokenSec = new Cookie("tokenSec", accessor.tokenSecret); 
            response.addCookie(cId);
            response.addCookie(cSec);
            response.addCookie(tokenSec);
        }

        ModelAndView modelAndView = new ModelAndView();
        if (oAuthParams.getErrorMessage() != null) {
            modelAndView.setViewName("temporaryCredentials");
        } else {
            modelAndView.setViewName("authorizeResourceOwner");
        }

        return modelAndView;
    }

    @RequestMapping("/temporaryCredentials")
    public ModelAndView handleInternalRequest(
        @ModelAttribute(value = "oAuthParams") OAuthParams oAuthParams) {
        return new ModelAndView("temporaryCredentials");
    }

}
