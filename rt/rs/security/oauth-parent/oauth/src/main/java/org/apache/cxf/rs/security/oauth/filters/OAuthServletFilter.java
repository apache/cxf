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
package org.apache.cxf.rs.security.oauth.filters;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuthProblemException;
import net.oauth.server.OAuthServlet;

import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.oauth.data.OAuthContext;
import org.apache.cxf.rs.security.oauth.utils.OAuthUtils;
import org.apache.cxf.security.SecurityContext;

/**
 * HTTP Servlet filter which can be used to protect end user endpoints
 */
public class OAuthServletFilter extends AbstractAuthFilter implements javax.servlet.Filter {
    protected static final String USE_USER_SUBJECT = "org.apache.cxf.rs.security.oauth.use_user_subject";
    
    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        super.setDataProvider(OAuthUtils.getOAuthDataProvider(servletContext));
        super.setValidator(OAuthUtils.getOAuthValidator(servletContext));
        super.setUseUserSubject(MessageUtils.isTrue(servletContext.getInitParameter(USE_USER_SUBJECT)));
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws
        IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse)response;

        try {
            OAuthInfo info = handleOAuthRequest(req);
            req = setSecurityContext(req, info);
            chain.doFilter(req, resp);
        } catch (OAuthProblemException e) {
            OAuthServlet.handleException(resp, e, "");
        } catch (Exception e) {
            OAuthServlet.handleException(resp, e, "");
        }
    }

    protected HttpServletRequest setSecurityContext(HttpServletRequest request, 
                                                    OAuthInfo info) {
        final SecurityContext sc = createSecurityContext(request, info);
        HttpServletRequest newRequest = new HttpServletRequestWrapper(request) {
        
            @Override
            public Principal getUserPrincipal() {
                return sc.getUserPrincipal();
            }
            
            @Override
            public boolean isUserInRole(String role) {
                return sc.isUserInRole(role);
            }
            
            @Override
            public String getAuthType() {
                return "OAuth";
            }
        };
        newRequest.setAttribute(OAuthContext.class.getName(), createOAuthContext(info));
        return newRequest;
    }
    
    public void destroy() {
    }
}
