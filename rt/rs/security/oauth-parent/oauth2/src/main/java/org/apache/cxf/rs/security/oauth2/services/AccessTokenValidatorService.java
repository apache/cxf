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

import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.rs.security.oauth2.common.AccessTokenValidation;
import org.apache.cxf.rs.security.oauth2.utils.AuthorizationUtils;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

@Path("validate")
public class AccessTokenValidatorService extends AbstractAccessTokenValidator {
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public AccessTokenValidation getTokenValidationInfo(@Encoded MultivaluedMap<String, String> params) {
        if (getMessageContext().getSecurityContext().getUserPrincipal() == null) {
            AuthorizationUtils.throwAuthorizationFailure(supportedSchemes, realm);
        }
        String authScheme = params.getFirst(OAuthConstants.AUTHORIZATION_SCHEME_TYPE);
        String authSchemeData  = params.getFirst(OAuthConstants.AUTHORIZATION_SCHEME_DATA);
        return super.getAccessTokenValidation(authScheme, authSchemeData, params);
    }
}
