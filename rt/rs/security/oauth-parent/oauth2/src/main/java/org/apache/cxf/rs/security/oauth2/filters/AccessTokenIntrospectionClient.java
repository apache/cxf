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
package org.apache.cxf.rs.security.oauth2.filters;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenValidation;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.TokenIntrospection;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenValidator;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

public class AccessTokenIntrospectionClient implements AccessTokenValidator {

    private WebClient tokenValidatorClient;
    public List<String> getSupportedAuthorizationSchemes() {
        return Collections.singletonList(OAuthConstants.BEARER_AUTHORIZATION_SCHEME);
    }

    public AccessTokenValidation validateAccessToken(MessageContext mc,
                                                     String authScheme,
                                                     String authSchemeData,
                                                     MultivaluedMap<String, String> extraProps)
        throws OAuthServiceException {
        WebClient client = WebClient.fromClient(tokenValidatorClient, true);
        MultivaluedMap<String, String> props = new MetadataMap<>();
        props.putSingle(OAuthConstants.TOKEN_ID, authSchemeData);
        try {
            TokenIntrospection response = client.post(props, TokenIntrospection.class);
            return convertIntrospectionToValidation(response);
        } catch (WebApplicationException ex) {
            throw new OAuthServiceException(ex);
        }
    }

    private AccessTokenValidation convertIntrospectionToValidation(TokenIntrospection response) {
        AccessTokenValidation atv = new AccessTokenValidation();
        atv.setInitialValidationSuccessful(response.isActive());
        if (response.getClientId() != null) {
            atv.setClientId(response.getClientId());
        }
        if (response.getIat() != null) {
            atv.setTokenIssuedAt(response.getIat());
        } else {
            atv.setTokenIssuedAt(OAuthUtils.getIssuedAt());
        }
        if (response.getExp() != null) {
            atv.setTokenLifetime(response.getExp() - atv.getTokenIssuedAt());
        }
        if (response.getNbf() != null) {
            atv.setTokenNotBefore(response.getNbf());
        }
        if (!StringUtils.isEmpty(response.getAud())) {
            atv.setAudiences(response.getAud());
        }
        if (response.getIss() != null) {
            atv.setTokenIssuer(response.getIss());
        }
        if (response.getScope() != null) {
            String[] scopes = response.getScope().split(" ");
            List<OAuthPermission> perms = new LinkedList<>();
            for (String s : scopes) {
                if (!StringUtils.isEmpty(s)) {
                    perms.add(new OAuthPermission(s.trim()));
                }
            }
            atv.setTokenScopes(perms);
        }
        if (response.getUsername() != null) {
            atv.setTokenSubject(new UserSubject(response.getUsername()));
        }
        atv.getExtraProps().putAll(response.getExtensions());

        return atv;
    }

    public void setTokenValidatorClient(WebClient tokenValidatorClient) {
        this.tokenValidatorClient = tokenValidatorClient;
    }


}
