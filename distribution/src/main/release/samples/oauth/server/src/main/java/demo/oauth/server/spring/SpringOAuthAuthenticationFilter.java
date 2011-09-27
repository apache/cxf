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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;

public class SpringOAuthAuthenticationFilter implements Filter {
    public static final String OAUTH_AUTHORITIES = "oauth_authorities";

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse)response;

        List<String> authorities = (List<String>)request.getAttribute(OAUTH_AUTHORITIES);
        List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();

        if (authorities != null) {
            for (String authority : authorities) {
                grantedAuthorities.add(new GrantedAuthorityImpl(authority));
            }

            Authentication auth = new AnonymousAuthenticationToken(UUID.randomUUID().toString(),
                req.getUserPrincipal(), grantedAuthorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
        }


        chain.doFilter(req, resp);
    }

    public void destroy() {

    }
}
