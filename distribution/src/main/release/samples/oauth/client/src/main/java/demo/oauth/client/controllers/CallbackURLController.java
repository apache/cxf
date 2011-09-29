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

import javax.servlet.http.HttpServletRequest;

import net.oauth.OAuth;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.server.OAuthServlet;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import demo.oauth.client.model.Common;
import demo.oauth.client.model.OAuthParams;



@Controller
public class CallbackURLController {

    @RequestMapping("/callback")
    protected ModelAndView handleRequest(@ModelAttribute("oAuthParams") OAuthParams oAuthParams,
                                         HttpServletRequest request) throws Exception {

        OAuthMessage message = OAuthServlet.getMessage(request, request.getRequestURL().toString());

        try {
            message.requireParameters(OAuth.OAUTH_TOKEN, OAuth.OAUTH_VERIFIER);
            oAuthParams.setOauthToken(message.getToken());
            oAuthParams.setOauthVerifier(message.getParameter(OAuth.OAUTH_VERIFIER));

            oAuthParams.setClientID(Common.findCookieValue(request, "clientID"));
            oAuthParams.setClientSecret(Common.findCookieValue(request, "clientSecret"));
        } catch (OAuthProblemException e) {
            oAuthParams.setErrorMessage("OAuth problem: " + e.getProblem() + e.getParameters().toString());
        }


        return new ModelAndView("tokenRequest");
    }
}
