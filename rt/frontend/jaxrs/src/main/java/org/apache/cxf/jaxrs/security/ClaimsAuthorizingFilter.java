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
package org.apache.cxf.jaxrs.security;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.claims.ClaimBean;
import org.apache.cxf.rt.security.claims.interceptor.ClaimsAuthorizingInterceptor;

public class ClaimsAuthorizingFilter implements ContainerRequestFilter {

    private ClaimsAuthorizingInterceptor interceptor;

    public ClaimsAuthorizingFilter() {
        this.interceptor = new ClaimsAuthorizingInterceptor();
    }

    public ClaimsAuthorizingFilter(ClaimsAuthorizingInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void filter(ContainerRequestContext context) {
        Message message = JAXRSUtils.getCurrentMessage();
        try {
            interceptor.handleMessage(message);
        } catch (AccessDeniedException ex) {
            context.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        }
    }

    @Deprecated()
    public void setClaims(Map<String, List<ClaimBean>> claimsMap) {
        interceptor.setClaims(claimsMap);
    }

    @Deprecated()
    public void setSecuredObject(Object securedObject) {
        interceptor.setSecuredObject(securedObject);
    }

}
