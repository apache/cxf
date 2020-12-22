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
package org.apache.cxf.systest.jaxrs;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class XForwardedServletFilter implements Filter {

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest)req;
        if (httpReq.getHeader("USE_XFORWARDED") != null) {
            httpReq = new HttpServletRequestXForwardedFilter(httpReq);
        } else if (httpReq.getHeader("USE_XFORWARDED_MANY_HOSTS") != null) {
            httpReq = new HttpServletRequestXForwardedFilter(httpReq, true);
        }
        chain.doFilter(httpReq, resp);
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub
        
    }

    private static class HttpServletRequestXForwardedFilter extends HttpServletRequestWrapper {
        private final boolean multihost;

        HttpServletRequestXForwardedFilter(HttpServletRequest request) {
            this(request, false);
        }

        HttpServletRequestXForwardedFilter(HttpServletRequest request, boolean multihost) {
            super(request);
            this.multihost = multihost;
        }

        @Override
        public String getHeader(String name) {
            if ("X-Forwarded-For".equals(name)) {
                return "199.0.0.1";
            } else if ("X-Forwarded-Proto".equals(name)) {
                return "https";
            } else if ("X-Forwarded-Prefix".equals(name)) {
                return "/reverse";
            } else if ("X-Forwarded-Port".equals(name)) {
                return "8090";
            } else if ("X-Forwarded-Host".equals(name)) {
                return !multihost ? "external" : "external1, external2, external3";
            } else { 
                return super.getHeader(name);
            }
        }
        

    }
}
