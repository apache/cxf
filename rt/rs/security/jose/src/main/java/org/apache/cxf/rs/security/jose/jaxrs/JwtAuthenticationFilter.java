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
package org.apache.cxf.rs.security.jose.jaxrs;

import java.io.IOException;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.JoseException;
import org.apache.cxf.rs.security.jose.JoseUtils;
import org.apache.cxf.rs.security.jose.jwt.AbstractJoseJwtConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.security.SecurityContext;

@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter extends AbstractJoseJwtConsumer implements ContainerRequestFilter {
    protected static final Logger LOG = LogUtils.getL7dLogger(JwtAuthenticationFilter.class);
    
    private static final String DEFAULT_AUTH_SCHEME = "JWT";
    private String expectedAuthScheme = DEFAULT_AUTH_SCHEME;
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        String[] parts = auth == null ? null : auth.split(" ");
        if (parts == null || !expectedAuthScheme.equals(parts[0]) || parts.length != 2) {
            throw new JoseException(expectedAuthScheme + " scheme is expected");
        }
        JwtToken token = super.getJwtToken(parts[1]);
        JoseUtils.setMessageContextProperty(token.getHeaders());
        JAXRSUtils.getCurrentMessage().put(SecurityContext.class, new JwtTokenSecurityContext(token));
    }
    
    public void setExpectedAuthScheme(String expectedAuthScheme) {
        this.expectedAuthScheme = expectedAuthScheme;
    }
    
}
