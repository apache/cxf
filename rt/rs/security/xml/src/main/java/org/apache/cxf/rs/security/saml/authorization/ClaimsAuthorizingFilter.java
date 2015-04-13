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
package org.apache.cxf.rs.security.saml.authorization;

import java.util.List;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.saml.claims.ClaimBean;
import org.apache.cxf.rt.security.saml.interceptor.ClaimsAuthorizingInterceptor;

public class ClaimsAuthorizingFilter implements ContainerRequestFilter {

    private ClaimsAuthorizingInterceptor interceptor = new ClaimsAuthorizingInterceptor();
    
    public void filter(ContainerRequestContext context) {
        Message message = JAXRSUtils.getCurrentMessage();
        try {
            interceptor.handleMessage(message);
        } catch (AccessDeniedException ex) {
            context.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        }
    }

    public void setClaims(Map<String, List<ClaimBean>> claimsMap) {
        interceptor.setClaims(claimsMap);
    }
    
    public void setSecuredObject(Object securedObject) {
        interceptor.setSecuredObject(securedObject);
    }
    
    
}
