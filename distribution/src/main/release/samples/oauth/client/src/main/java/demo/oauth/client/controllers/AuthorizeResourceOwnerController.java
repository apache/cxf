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

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import demo.oauth.client.model.OAuthParams;


@Controller
public class AuthorizeResourceOwnerController {

    @RequestMapping("/authorizeResourceOwner")
    public void handleRequest(@ModelAttribute(value = "oAuthParams") OAuthParams oAuthParams,
                              HttpServletResponse response) throws Exception {

        String oauthToken = oAuthParams.getOauthToken();
        String resourceOwnerAuthorizationEndpoint = oAuthParams.getResourceOwnerAuthorizationEndpoint();
        if (resourceOwnerAuthorizationEndpoint == null || "".equals(resourceOwnerAuthorizationEndpoint)) {
            oAuthParams.setErrorMessage("Missing resource owner authorization URI");
        }

        if (oauthToken == null || "".equals(oauthToken)) {
            oAuthParams.setErrorMessage("Missing oauth token");
        }

        response
            .sendRedirect(
                new StringBuilder().append(resourceOwnerAuthorizationEndpoint).
                    append("?oauth_token=").append(oauthToken).toString());
    }
}
