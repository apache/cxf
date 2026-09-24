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

package org.apache.cxf.sts.cache;

import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.request.Renewing;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;

public final class CacheUtils {

    private CacheUtils() {
        // complete
    }

    public static SecurityToken createSecurityTokenForStorage(
        Element token,
        String tokenIdentifier,
        Instant expiry,
        Principal principal,
        String realm,
        Renewing renewing
    ) {
        SecurityToken securityToken = new SecurityToken(tokenIdentifier, null, expiry);
        securityToken.setToken(token);
        securityToken.setPrincipal(principal);

        Map<String, Object> props = new HashMap<>();
        securityToken.setProperties(props);
        if (realm != null) {
            props.put(STSConstants.TOKEN_REALM, realm);
        }

        // Handle Renewing logic
        if (renewing != null) {
            props.put(
                STSConstants.TOKEN_RENEWING_ALLOW,
                String.valueOf(renewing.isAllowRenewing())
            );
            props.put(
                STSConstants.TOKEN_RENEWING_ALLOW_AFTER_EXPIRY,
                String.valueOf(renewing.isAllowRenewingAfterExpiry())
            );
        } else {
            props.put(STSConstants.TOKEN_RENEWING_ALLOW, "true");
            props.put(STSConstants.TOKEN_RENEWING_ALLOW_AFTER_EXPIRY, "false");
        }

        return securityToken;
    }

    /**
     * Store the given SecurityToken in the cache, using a (SHA-256 based) key derived from the signature
     * of the given (signed) SAML Assertion - see TokenStoreUtils.getCacheKey(SamlAssertionWrapper).
     * Nothing is stored if the Assertion is not signed. The signature profile is not checked here, as the
     * Assertion might have just been signed by the STS - it is checked when a received token is looked up.
     */
    public static void storeTokenInCache(
        SecurityToken securityToken,
        TokenStore cache,
        SamlAssertionWrapper assertion
    ) throws WSSecurityException {
        String identifier = TokenStoreUtils.getCacheKey(assertion, false);
        if (identifier != null) {
            cache.add(identifier, securityToken);
        }
    }
}
