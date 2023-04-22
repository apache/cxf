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
package org.apache.cxf.rs.security.oauth2.services;

import java.util.logging.Logger;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.SecurityContext;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenValidation;
import org.apache.cxf.rs.security.oauth2.utils.AuthorizationUtils;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

@Path("validate")
public class AccessTokenValidatorService extends AbstractAccessTokenValidator {
    private static final Logger LOG = LogUtils.getL7dLogger(AccessTokenValidatorService.class);
    private boolean blockUnsecureRequests;
    private boolean blockUnauthorizedRequests = true;
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public AccessTokenValidation getTokenValidationInfo(@Encoded MultivaluedMap<String, String> params) {
        checkSecurityContext();
        String authScheme = params.getFirst(OAuthConstants.AUTHORIZATION_SCHEME_TYPE);
        String authSchemeData = params.getFirst(OAuthConstants.AUTHORIZATION_SCHEME_DATA);
        try {
            return super.getAccessTokenValidation(authScheme, authSchemeData, params);
        } catch (NotAuthorizedException ex) {
            // at this point it does not mean that RS failed to authenticate but that the basic
            // local or chained token validation has failed
            AccessTokenValidation v = new AccessTokenValidation();
            v.setInitialValidationSuccessful(false);
            return v;
        }
    }

    private void checkSecurityContext() {
        SecurityContext sc = getMessageContext().getSecurityContext();
        if (!sc.isSecure() && blockUnsecureRequests) {
            LOG.warning("Unsecure HTTP, Transport Layer Security is recommended");
            AuthorizationUtils.throwAuthorizationFailure(supportedSchemes, realm);
        }
        if (sc.getUserPrincipal() == null && blockUnauthorizedRequests) {
            //TODO: check client certificates
            LOG.warning("Authenticated Principal is not available");
            AuthorizationUtils.throwAuthorizationFailure(supportedSchemes, realm);
        }

    }

    public void setBlockUnsecureRequests(boolean blockUnsecureRequests) {
        this.blockUnsecureRequests = blockUnsecureRequests;
    }

    public void setBlockUnauthorizedRequests(boolean blockUnauthorizedRequests) {
        this.blockUnauthorizedRequests = blockUnauthorizedRequests;
    }
}
