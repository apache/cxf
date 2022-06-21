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
package org.apache.cxf.systest.jaxrs.security.oidc;

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.provider.SubjectCreator;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.rs.security.oidc.idp.IdTokenProvider;
import org.apache.cxf.rs.security.oidc.idp.OidcUserSubject;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;
import org.apache.cxf.security.SecurityContext;


public class SubjectCreatorImpl implements SubjectCreator {

    private static final IdTokenProvider ID_TOKEN_PROVIDER = new IdTokenProviderImpl();

    @Override
    public OidcUserSubject createUserSubject(MessageContext mc, MultivaluedMap<String, String> params) {
        OidcUserSubject oidcSub = new OidcUserSubject(OAuthUtils.createSubject(mc,
            (SecurityContext)mc.get(SecurityContext.class.getName())));

        final List<String> scopes;
        String requestedScope = params.getFirst(OAuthConstants.SCOPE);
        if (requestedScope != null && !requestedScope.isEmpty()) {
            scopes = OidcUtils.getScopeClaims(requestedScope.split(" "));
        } else {
            scopes = Collections.emptyList();
        }

        oidcSub.setIdToken(ID_TOKEN_PROVIDER.getIdToken(null, oidcSub, scopes));

        return oidcSub;
    }

}
