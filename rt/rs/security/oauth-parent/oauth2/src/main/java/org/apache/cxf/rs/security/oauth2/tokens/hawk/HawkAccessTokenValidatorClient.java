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
package org.apache.cxf.rs.security.oauth2.tokens.hawk;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenValidation;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenValidator;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public class HawkAccessTokenValidatorClient extends AbstractHawkAccessTokenValidator {
    private AccessTokenValidator validator;
        
    protected AccessTokenValidation getAccessTokenValidation(MessageContext mc,
                                                             String authScheme, 
                                                             String authSchemeData, 
                                                             MultivaluedMap<String, String> extraProps,
                                                             Map<String, String> schemeParams) {
        return validator.validateAccessToken(mc, authScheme, authSchemeData, extraProps);
    }

    public void setValidator(AccessTokenValidator validator) {
        List<String> schemes = validator.getSupportedAuthorizationSchemes();
        if (!schemes.contains("*") && !schemes.contains(OAuthConstants.HAWK_AUTHORIZATION_SCHEME)) { 
            throw new IllegalArgumentException();
        }
        this.validator = validator;
    }
    
}
