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
package org.apache.cxf.rs.security.oauth2.tokens.jwt;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.jose.jwt.JoseJwtConsumer;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenValidation;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenValidator;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;

public abstract class AbstactJwtAccessTokenValidator extends JoseJwtConsumer 
    implements AccessTokenValidator {
    private OAuthDataProvider dataProvider;
    
    @Override
    public List<String> getSupportedAuthorizationSchemes() {
        return Collections.singletonList("*");
    }

    @Override
    public AccessTokenValidation validateAccessToken(MessageContext mc, 
                                                     String authScheme,
                                                     String authSchemeData,
                                                     MultivaluedMap<String, String> extraProps)
        throws OAuthServiceException {
        ServerAccessToken at = dataProvider.getAccessToken(authSchemeData);
        super.getJwtToken(at.getTokenKey());
        return new AccessTokenValidation(at);
    }

    public void setDataProvider(OAuthDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    
}
