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

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import org.apache.cxf.rs.security.jose.common.JoseException;

public class JwtCookieAuthenticationFilter extends AbstractJwtAuthenticationFilter {
    private static final String DEFAULT_COOKIE_NAME = "access_token";
    private String cookieName = DEFAULT_COOKIE_NAME;

    protected String getEncodedJwtToken(ContainerRequestContext requestContext) {
        Cookie cookie = requestContext.getCookies().get(cookieName);
        if (cookie == null || cookie.getValue() == null) {
            throw new JoseException("JWT cookie is not available");
        }
        return cookie.getValue();
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }
}
