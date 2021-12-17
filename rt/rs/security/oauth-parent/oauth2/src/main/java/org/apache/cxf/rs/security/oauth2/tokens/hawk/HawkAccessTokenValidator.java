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

import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenValidation;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public class HawkAccessTokenValidator extends AbstractHawkAccessTokenValidator {
    private OAuthDataProvider dataProvider;

    protected AccessTokenValidation getAccessTokenValidation(MessageContext mc,
                                                             String authScheme,
                                                             String authSchemeData,
                                                             MultivaluedMap<String, String> extraProps,
                                                             Map<String, String> schemeParams) {
        String macKey = schemeParams.get(OAuthConstants.HAWK_TOKEN_ID);
        ServerAccessToken accessToken = dataProvider.getAccessToken(macKey);
        if (!(accessToken instanceof HawkAccessToken)) {
            throw new OAuthServiceException(OAuthConstants.SERVER_ERROR);
        }
        HawkAccessToken macAccessToken = (HawkAccessToken)accessToken;
        AccessTokenValidation atv = new AccessTokenValidation(macAccessToken);

        // OAuth2 Pop token introspection will likely support returning a JWE-encrypted key
        if (!isRemoteSignatureValidation() || mc.getSecurityContext().isSecure()) {
            atv.getExtraProps().put(OAuthConstants.HAWK_TOKEN_KEY, macAccessToken.getMacKey());
            atv.getExtraProps().put(OAuthConstants.HAWK_TOKEN_ALGORITHM, macAccessToken.getMacAlgorithm());
        }

        return atv;
    }

    public void setDataProvider(OAuthDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

}
