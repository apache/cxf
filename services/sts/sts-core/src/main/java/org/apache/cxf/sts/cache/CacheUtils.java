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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.request.Renewing;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;

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

    public static void storeTokenInCache(
        SecurityToken securityToken,
        TokenStore cache,
        byte[] signatureValue
    ) {
        int hash = Arrays.hashCode(signatureValue);
        securityToken.setTokenHash(hash);
        String identifier = Integer.toString(hash);
        cache.add(identifier, securityToken);
    }
}
