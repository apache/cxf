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
package org.apache.cxf.rs.security.oidc.rp;

import jakarta.ws.rs.core.SecurityContext;
import org.apache.cxf.common.security.SimpleSecurityContext;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oidc.common.AbstractUserInfo;
import org.apache.cxf.rs.security.oidc.common.IdToken;

public class OidcSecurityContext extends SimpleSecurityContext implements SecurityContext {
    private OidcClientTokenContext oidcContext;
    private String roleClaim;

    public OidcSecurityContext(IdToken token) {
        this(new OidcClientTokenContextImpl(token));
    }

    public OidcSecurityContext(OidcClientTokenContext oidcContext) {
        super(getPrincipalName(oidcContext));
        this.oidcContext = oidcContext;
    }

    public OidcClientTokenContext getOidcContext() {
        return oidcContext;
    }

    protected static String getPrincipalName(OidcClientTokenContext oidcContext) {
        String name = null;
        if (oidcContext.getUserInfo() != null) {
            name = getPrincipalName(oidcContext.getUserInfo());
        }
        if (name == null && oidcContext.getIdToken() != null) {
            name = getPrincipalName(oidcContext.getIdToken());
        }
        return name;
    }

    protected static String getPrincipalName(AbstractUserInfo info) {

        String name = info.getPreferredUserName();
        if (name == null) {
            name = info.getGivenName();
        }
        if (name == null) {
            name = info.getNickName();
        }
        if (name == null) {
            name = info.getName();
        }
        if (name == null) {
            name = info.getSubject();
        }
        return name;

    }

    @Override
    public boolean isSecure() {
        String value = HttpUtils.getEndpointAddress(JAXRSUtils.getCurrentMessage());
        return value.startsWith("https://");
    }

    @Override
    public String getAuthenticationScheme() {
        return "OIDC";
    }

    @Override
    public boolean isUserInRole(String role) {

        return roleClaim != null && role != null
            && (containsClaim(oidcContext.getIdToken(), roleClaim, role)
                || containsClaim(oidcContext.getUserInfo(), roleClaim, role));
    }

    private boolean containsClaim(AbstractUserInfo userInfo, String claim, String claimValue) {
        return userInfo != null && userInfo.containsProperty(claim)
            && claimValue.equals(userInfo.getProperty(claim));
    }

    /**
     * Set the claim name that corresponds to the "role" of the Subject of the IdToken.
     */
    public void setRoleClaim(String roleClaim) {
        this.roleClaim = roleClaim;
    }
}
