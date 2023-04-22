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

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.interceptor.security.AbstractAuthorizingInInterceptor;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

@Priority(Priorities.AUTHORIZATION)
public class SimpleAuthorizingFilter implements ContainerRequestFilter {

    private AbstractAuthorizingInInterceptor interceptor;

    @Override
    public void filter(ContainerRequestContext context) {
        try {
            interceptor.handleMessage(JAXRSUtils.getCurrentMessage());
        } catch (AccessDeniedException ex) {
            context.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        }
    }

    public void setInterceptor(AbstractAuthorizingInInterceptor in) {
        interceptor = in;
    }
}
