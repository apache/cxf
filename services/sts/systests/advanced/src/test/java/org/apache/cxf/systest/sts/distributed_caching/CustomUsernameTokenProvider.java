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

package org.apache.cxf.systest.sts.distributed_caching;

import org.w3c.dom.Document;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.dom.message.token.UsernameToken;

/**
 * A TokenProvider implementation that creates a UsernameToken.
 */
public class CustomUsernameTokenProvider implements TokenProvider {

    private static final String TOKEN_TYPE =
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#UsernameToken";

    public boolean canHandleToken(String tokenType) {
        return TOKEN_TYPE.equals(tokenType);
    }

    public boolean canHandleToken(String tokenType, String realm) {
        return canHandleToken(tokenType);
    }

    public TokenProviderResponse createToken(TokenProviderParameters tokenParameters) {
        try {
            Document doc = DOMUtils.getEmptyDocument();

            // Mock up a UsernameToken
            UsernameToken usernameToken = new UsernameToken(true, doc, WSS4JConstants.PASSWORD_TEXT);
            usernameToken.setName("alice");
            usernameToken.setPassword("password");
            String id = "UT-1234";
            usernameToken.addWSSENamespace();
            usernameToken.addWSUNamespace();
            usernameToken.setID(id);

            TokenProviderResponse response = new TokenProviderResponse();
            response.setToken(usernameToken.getElement());
            response.setTokenId(id);

            // Store the token in the cache
            if (tokenParameters.getTokenStore() != null) {
                SecurityToken securityToken = new SecurityToken(usernameToken.getID());
                securityToken.setToken(usernameToken.getElement());
                int hashCode = usernameToken.hashCode();
                String identifier = Integer.toString(hashCode);
                securityToken.setTokenHash(hashCode);
                tokenParameters.getTokenStore().add(identifier, securityToken);
            }

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            throw new STSException("Can't serialize SAML assertion", e, STSException.REQUEST_FAILED);
        }
    }

}
