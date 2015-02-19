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

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

public class AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private String authorizeUrl;

    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception)
        throws IOException, ServletException {
        String oauthToken = request.getParameter(OAuth.OAUTH_TOKEN);
        String xScope = request.getParameter(OAuthConstants.X_OAUTH_SCOPE);

        StringBuilder url = new StringBuilder(authorizeUrl).append("?").append(OAuth.OAUTH_TOKEN).append("=")
            .append(oauthToken);

        if (!StringUtils.isEmpty(xScope)) {
            url.append("&").append(OAuthConstants.X_OAUTH_SCOPE).append("=").append(xScope);
        }

        setDefaultFailureUrl(url.toString());
        super.onAuthenticationFailure(request, response,
            exception);
    }

    public void setAuthorizeUrl(String authorizeUrl) {
        this.authorizeUrl = authorizeUrl;
    }
}
