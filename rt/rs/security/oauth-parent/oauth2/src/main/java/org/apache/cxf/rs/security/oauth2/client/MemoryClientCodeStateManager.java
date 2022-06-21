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
package org.apache.cxf.rs.security.oauth2.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.rt.security.crypto.MessageDigestUtils;

public class MemoryClientCodeStateManager implements ClientCodeStateManager {
    private Map<String, MultivaluedMap<String, String>> map =
            new ConcurrentHashMap<>();

    private boolean generateNonce;

    @Override
    public MultivaluedMap<String, String> toRedirectState(MessageContext mc,
                                                          MultivaluedMap<String, String> requestState) {
        String stateParam = OAuthUtils.generateRandomTokenKey();
        MultivaluedMap<String, String> redirectMap = new MetadataMap<>();

        if (generateNonce) {
            String nonceParam = MessageDigestUtils.generate(CryptoUtils.generateSecureRandomBytes(32));
            requestState.putSingle(OAuthConstants.NONCE, nonceParam);
            redirectMap.putSingle(OAuthConstants.NONCE, nonceParam);
        }
        map.put(stateParam, requestState);
        OAuthUtils.setSessionToken(mc, stateParam, "state", 0);
        redirectMap.putSingle(OAuthConstants.STATE, stateParam);
        return redirectMap;
    }

    @Override
    public MultivaluedMap<String, String> fromRedirectState(MessageContext mc,
                                                            MultivaluedMap<String, String> redirectState) {
        String stateParam = redirectState.getFirst(OAuthConstants.STATE);
        String sessionToken = OAuthUtils.getSessionToken(mc, "state");
        if (sessionToken == null || !sessionToken.equals(stateParam)) {
            throw new OAuthServiceException("Invalid session token");
        }
        return map.remove(stateParam);
    }

    public void setGenerateNonce(boolean generateNonce) {
        this.generateNonce = generateNonce;
    }
}
