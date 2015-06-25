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

import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.common.security.SimpleSecurityContext;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oidc.common.IdToken;

public class OidcSecurityContext extends SimpleSecurityContext implements SecurityContext {
    private OidcClientTokenContext oidcContext;
    public OidcSecurityContext(IdToken token) {
        this(new OidcClientTokenContextImpl());
    }
    public OidcSecurityContext(OidcClientTokenContext oidcContext) {
        super(getUserName(oidcContext));
        this.oidcContext = oidcContext;
    }
    public OidcClientTokenContext getOidcContext() {
        return oidcContext;
    }
    private static String getUserName(OidcClientTokenContext oidcContext) {
        if (oidcContext.getUserInfo() != null) {
            return oidcContext.getUserInfo().getEmail();
        } else {
            return oidcContext.getIdToken().getSubject();
        }
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
}
