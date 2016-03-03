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
package org.apache.cxf.rs.security.oidc.idp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthRedirectionState;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.services.AbstractImplicitGrantService;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;


public class OidcHybridService extends AbstractImplicitGrantService {
    public static final String CODE_AT_RESPONSE_TYPE = "code token";
    public static final String CODE_ID_TOKEN_RESPONSE_TYPE = "code id_token";
    public static final String CODE_ID_TOKEN_AT_RESPONSE_TYPE = "code id_token token";
    private static final Map<String, String> IMPLICIT_RESPONSE_TYPES;
    static {
        IMPLICIT_RESPONSE_TYPES = new HashMap<String, String>();
        IMPLICIT_RESPONSE_TYPES.put(CODE_AT_RESPONSE_TYPE, OAuthConstants.TOKEN_RESPONSE_TYPE);
        IMPLICIT_RESPONSE_TYPES.put(CODE_ID_TOKEN_RESPONSE_TYPE, OidcImplicitService.ID_TOKEN_RESPONSE_TYPE);
        IMPLICIT_RESPONSE_TYPES.put(CODE_ID_TOKEN_AT_RESPONSE_TYPE, OidcImplicitService.ID_TOKEN_AT_RESPONSE_TYPE);
    }
    private OidcAuthorizationCodeService codeService;
    private OidcImplicitService implicitService;
    
    public OidcHybridService() {
        super(new HashSet<String>(Arrays.asList(CODE_AT_RESPONSE_TYPE,
                                                CODE_ID_TOKEN_RESPONSE_TYPE,
                                                CODE_ID_TOKEN_AT_RESPONSE_TYPE)), 
                                  "Hybrid");
    }
    
    
    @Override
    public StringBuilder prepareGrant(OAuthRedirectionState state,
                                   Client client,
                                   List<String> requestedScope,
                                   List<String> approvedScope,
                                   UserSubject userSubject,
                                   ServerAccessToken preAuthorizedToken) {
        String actualResponseType = state.getResponseType();
        
        state.setResponseType(OAuthConstants.CODE_RESPONSE_TYPE);
        String code = codeService.getGrantCode(state, client, requestedScope,
                                               approvedScope, userSubject, preAuthorizedToken);
        state.setResponseType(IMPLICIT_RESPONSE_TYPES.get(actualResponseType)); 
        StringBuilder sb = implicitService.prepareGrant(state, client, requestedScope, 
                                                          approvedScope, userSubject, preAuthorizedToken);
   
        sb.append("&");
        sb.append(OAuthConstants.AUTHORIZATION_CODE_VALUE).append("=").append(code);
        return sb;
    }


    public void setCodeService(OidcAuthorizationCodeService codeService) {
        this.codeService = codeService;
    }


    public void setImplicitService(OidcImplicitService implicitService) {
        this.implicitService = implicitService;
    }
 }
