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
import java.util.List;

import javax.ws.rs.core.HttpHeaders;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenValidation;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenValidator;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public class AccessTokenValidatorClient implements AccessTokenValidator {

    private WebClient tokenValidatorClient;
    
    public List<String> getSupportedAuthorizationSchemes() {
        return Collections.singletonList(OAuthConstants.ALL_AUTH_SCHEMES);
    }

    public AccessTokenValidation validateAccessToken(MessageContext mc,
                                                     String authScheme, 
                                                     String authSchemeData) 
        throws OAuthServiceException {
        WebClient client = WebClient.fromClient(tokenValidatorClient, true);
        client.header(HttpHeaders.AUTHORIZATION, authScheme + " " + authSchemeData);
        return client.get(AccessTokenValidation.class);
    }

    public void setTokenValidatorClient(WebClient tokenValidatorClient) {
        this.tokenValidatorClient = tokenValidatorClient;
    }

}
