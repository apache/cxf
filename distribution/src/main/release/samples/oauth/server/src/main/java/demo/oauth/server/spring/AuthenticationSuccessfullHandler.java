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
package demo.oauth.server.spring;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuth;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.oauth.utils.OAuthConstants;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

public class AuthenticationSuccessfullHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private String confirmationUrl;

    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException,
            ServletException {
        super.onAuthenticationSuccess(request, response, authentication);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {

        String oauthToken = request.getParameter(OAuth.OAUTH_TOKEN);
        String authToken = request.getParameter(OAuthConstants.AUTHENTICITY_TOKEN);
        String decision = request.getParameter(OAuthConstants.AUTHORIZATION_DECISION_KEY);
        String xScope = request.getParameter(OAuthConstants.X_OAUTH_SCOPE);

        if (StringUtils.isEmpty(oauthToken)) {
            return super.determineTargetUrl(request, response);
        }

        StringBuilder url = new StringBuilder(confirmationUrl).append("?").append(OAuth.OAUTH_TOKEN).append("=")
                .append(oauthToken).append("&").append(OAuthConstants.AUTHENTICITY_TOKEN)
                .append("=")
                .append(authToken);

        if (!StringUtils.isEmpty(decision)) {
            url.append("&").append(OAuthConstants.AUTHORIZATION_DECISION_KEY).append("=")
                    .append(decision);
        }

        if (!StringUtils.isEmpty(xScope)) {
            url.append("&").append(OAuthConstants.X_OAUTH_SCOPE).append("=").append(xScope);
        }

        return url.toString();
    }

    public void setConfirmationUrl(String confirmationUrl) {
        this.confirmationUrl = confirmationUrl;
    }
}
