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
package org.apache.cxf.rs.security.oauth2.grants.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

/**
 * The "JWT Bearer" grant handler
 */
public class JwtBearerGrantHandler extends AbstractJwtHandler {
    private static final String ENCODED_JWT_BEARER_GRANT;
    static {
        //  AccessTokenService may be configured with the form provider
        // which will not decode by default - so listing both the actual
        // and encoded grant type value will help
        ENCODED_JWT_BEARER_GRANT = HttpUtils.urlEncode(Constants.JWT_BEARER_GRANT, StandardCharsets.UTF_8.name());
    }
    public JwtBearerGrantHandler() {
        super(Arrays.asList(Constants.JWT_BEARER_GRANT, ENCODED_JWT_BEARER_GRANT));
    }

    @Override
    public ServerAccessToken createAccessToken(Client client, MultivaluedMap<String, String> params)
        throws OAuthServiceException {
        String assertion = params.getFirst(Constants.CLIENT_GRANT_ASSERTION_PARAM);
        if (assertion == null) {
            throw new OAuthServiceException(OAuthConstants.INVALID_GRANT);
        }
        try {
            JwsJwtCompactConsumer jwsReader = getJwsReader(assertion);
            JwtToken jwtToken = jwsReader.getJwtToken();
            validateSignature(new JwsHeaders(jwtToken.getJwsHeaders()),
                                  jwsReader.getUnsignedEncodedSequence(),
                                  jwsReader.getDecodedSignature());


            validateClaims(client, jwtToken.getClaims());
            UserSubject grantSubject = new UserSubject(jwtToken.getClaims().getSubject());

            return doCreateAccessToken(client,
                                       grantSubject,
                                       Constants.JWT_BEARER_GRANT,
                                       OAuthUtils.parseScope(params.getFirst(OAuthConstants.SCOPE)));
        } catch (OAuthServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OAuthServiceException(OAuthConstants.INVALID_GRANT, ex);
        }

    }

    protected JwsJwtCompactConsumer getJwsReader(String assertion) {
        return new JwsJwtCompactConsumer(assertion);
    }

}
